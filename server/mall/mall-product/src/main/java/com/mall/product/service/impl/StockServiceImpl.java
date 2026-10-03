package com.mall.product.service.impl;

import com.mall.api.feign.RemoteProductService.ReserveStockItemRequest;
import com.mall.common.constant.CacheConstants;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.product.DO.MallSkuStockDO;
import com.mall.product.mapper.MallSkuStockMapper;
import com.mall.product.service.IStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 库存服务实现
 *
 * <p>基于乐观锁实现并发安全库存扣减，支持预扣、释放、补货</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl implements IStockService {

    /** 预扣记录在 Redis 中的存活时间，与 reserveStock 写入保持一致（24h） */
    private static final long RESERVE_KEY_TTL_HOURS = 24L;

    /** 回补幂等键在 Redis 中的存活时间（24h） */
    private static final long RESTOCK_KEY_TTL_HOURS = 24L;

    /** 库存释放遇乐观锁冲突时的最大重试次数 */
    private static final int RELEASE_MAX_RETRY = 3;

    /** Redis SCAN 每批拉取的 key 数量 */
    private static final int SCAN_BATCH_SIZE = 1000;

    private final MallSkuStockMapper mallSkuStockMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reserveStock(String orderNo, List<ReserveStockItemRequest> items) {
        // 逐项预扣库存（乐观锁版本控制，任一失败整体回滚）
        for (ReserveStockItemRequest item : items) {
            // 先查询当前库存记录，获取乐观锁版本号
            MallSkuStockDO stock = mallSkuStockMapper.selectBySkuId(item.getSkuId());
            if (stock == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            // 乐观锁扣减：available_stock -= qty, locked_stock += qty
            // WHERE 条件附带 version 校验，防止并发覆盖
            // 影响行数为 0 说明版本冲突或可用库存不足
            int affected = mallSkuStockMapper.reserveStock(item.getSkuId(), item.getQty(), stock.getVersion());
            if (affected == 0) {
                throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT);
            }
            // 预扣成功后在 Redis 记录幂等键：orderNo:skuId → qty
            // 用于订单超时取消/取消时释放库存，或防止重复扣减
            redisTemplate.opsForValue().set(
                    CacheConstants.Product.STOCK_RESERVE + orderNo + ":" + item.getSkuId(),
                    item.getQty().toString(), RESERVE_KEY_TTL_HOURS, TimeUnit.HOURS);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean releaseStock(String orderNo) {
        String keyPrefix = CacheConstants.Product.STOCK_RESERVE + orderNo + ":";
        List<String> reserveKeys = scanReserveKeys(keyPrefix);
        if (reserveKeys.isEmpty()) {
            log.warn("releaseStock 未找到预扣记录, orderNo={}", orderNo);
            return true;
        }

        int released = 0;
        int failed = 0;
        for (String reserveKey : reserveKeys) {
            Object qtyObj = redisTemplate.opsForValue().get(reserveKey);
            if (qtyObj == null) {
                // 记录已过期或已被并发消费：视为「已无需释放」的幂等成功，不计入 failed。
                // 若计入 failed 会触发 MQ 重投，而这些记录不会回来，只会耗尽重试次数进死信
                continue;
            }

            long skuId;
            int qty;
            try {
                skuId = Long.parseLong(reserveKey.substring(keyPrefix.length()));
                qty = Integer.parseInt(String.valueOf(qtyObj));
            } catch (NumberFormatException e) {
                log.error("releaseStock 预扣记录格式非法, key={}, value={}", reserveKey, qtyObj, e);
                failed++;
                continue;
            }

            // 乐观锁重试：并发重复调用时，后到者 version 已变，update 影响 0 行而被拦截
            boolean ok = false;
            for (int attempt = 1; attempt <= RELEASE_MAX_RETRY; attempt++) {
                MallSkuStockDO stock = mallSkuStockMapper.selectBySkuId(skuId);
                if (stock == null) {
                    log.error("releaseStock 库存记录不存在, skuId={}, orderNo={}", skuId, orderNo);
                    break;
                }
                if (mallSkuStockMapper.releaseStock(skuId, qty, stock.getVersion()) > 0) {
                    ok = true;
                    break;
                }
                log.warn("releaseStock 版本冲突，重试 {}/{}, skuId={}", attempt, RELEASE_MAX_RETRY, skuId);
            }

            if (ok) {
                // 仅在数据库更新成功后删除预扣记录，保证可重试
                redisTemplate.delete(reserveKey);
                released++;
            } else {
                failed++;
                log.error("releaseStock 释放失败，保留预扣记录待重试, orderNo={}, skuId={}, qty={}",
                        orderNo, skuId, qty);
            }
        }
        log.info("releaseStock 完成, orderNo={}, 预扣项={}, 成功释放={}, 失败={}",
                orderNo, reserveKeys.size(), released, failed);
        // 有失败项时返回 false，由调用方决定是否重试；单个预扣项失败不抛异常（见接口 Javadoc）
        return failed == 0;
    }

    /**
     * 按前缀扫描预扣记录 key。
     *
     * <p>mall-product 无法直接查询 mall-order 的订单项表（跨服务），
     * 因此以 reserveStock 写入的 Redis 预扣记录作为唯一事实来源。</p>
     *
     * @param keyPrefix key 前缀（含订单号）
     * @return 命中的 key 列表
     * @throws BusinessException 扫描失败时抛出——不能返回空列表让调用方误判为「无预扣记录」
     */
    private List<String> scanReserveKeys(String keyPrefix) {
        List<String> keys = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(keyPrefix + "*")
                .count(SCAN_BATCH_SIZE)
                .build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        } catch (Exception e) {
            // 返回空列表会让 releaseStock 走「无预扣记录」分支并返回 true，
            // 调用方据此 ACK，而实际一条预扣记录都没释放 → 库存永久占用。
            // 此处尚未产生任何 DB/Redis 副作用，上抛是安全的。
            log.error("releaseStock 扫描预扣记录失败, keyPrefix={}", keyPrefix, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return keys;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restock(Long skuId, Integer qty, String bizNo) {
        // 幂等：同一业务单 + SKU 只回补一次。调用方是跨服务（Feign）且可被 MQ 重投——
        // 无幂等键时「restock 成功但本地事务回滚」会导致重投重复回补
        String idempotentKey = CacheConstants.Product.STOCK_RESTOCK + bizNo + ":" + skuId;
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, String.valueOf(qty), RESTOCK_KEY_TTL_HOURS, TimeUnit.HOURS);
        if (!Boolean.TRUE.equals(first)) {
            log.info("restock 幂等命中，跳过重复回补: bizNo={}, skuId={}", bizNo, skuId);
            return;
        }

        try {
            MallSkuStockDO stock = mallSkuStockMapper.selectBySkuId(skuId);
            if (stock == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            // 乐观锁增加可用库存：影响 0 行即版本冲突（并发改动同一行，热销 SKU 上是常态），
            // 必须上抛以释放幂等键并交由上层重试——否则幂等键留存会造成静默未回补、日志谎报成功
            if (mallSkuStockMapper.restock(skuId, qty, stock.getVersion()) == 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        } catch (RuntimeException e) {
            // 回补失败必须释放幂等标记，否则重试会被幂等拦截、库存永不回补
            log.error("restock 回补失败，释放幂等标记待重试: bizNo={}, skuId={}", bizNo, skuId, e);
            try {
                redisTemplate.delete(idempotentKey);
            } catch (RuntimeException releaseError) {
                log.error("【需人工介入】释放 restock 幂等标记失败: bizNo={}, skuId={}",
                        bizNo, skuId, releaseError);
            }
            throw e;
        }
    }
}
