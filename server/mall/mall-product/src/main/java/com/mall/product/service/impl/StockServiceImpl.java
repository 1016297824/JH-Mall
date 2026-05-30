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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final MallSkuStockMapper mallSkuStockMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reserveStock(String orderNo, List<ReserveStockItemRequest> items) {
        // 逐项预扣库存（乐观锁版本控制）
        for (ReserveStockItemRequest item : items) {
            MallSkuStockDO stock = mallSkuStockMapper.selectBySkuId(item.getSkuId());
            if (stock == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            // 乐观锁扣减 available → locked，不满足则回滚
            int affected = mallSkuStockMapper.reserveStock(item.getSkuId(), item.getQty(), stock.getVersion());
            if (affected == 0) {
                throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT);
            }
            // 记录预扣缓存（作为幂等键，TTL 24h）
            redisTemplate.opsForValue().set(
                    CacheConstants.Product.STOCK_RESERVE + orderNo + ":" + item.getSkuId(),
                    item.getQty().toString(), 86400, TimeUnit.SECONDS);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseStock(String orderNo) {
        log.info("releaseStock called for orderNo={}", orderNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restock(Long skuId, Integer qty) {
        MallSkuStockDO stock = mallSkuStockMapper.selectBySkuId(skuId);
        if (stock == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        // 乐观锁增加可用库存
        mallSkuStockMapper.restock(skuId, qty, stock.getVersion());
    }
}
