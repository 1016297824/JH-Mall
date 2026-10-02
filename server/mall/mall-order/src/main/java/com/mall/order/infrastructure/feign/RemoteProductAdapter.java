package com.mall.order.infrastructure.feign;

import com.mall.api.feign.RemoteProductService;
import com.mall.api.feign.RemoteProductService.ReserveStockItemRequest;
import com.mall.common.DTO.product.ProductSkuDTO;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * mall-product Feign 适配器
 *
 * <p>负责商品域远程调用的容错降级：批量查询 SKU 失败时返回空 Map
 * 而非抛异常，由调用方回退到购物车表冗余字段（设计文档 §4.5 第 4 点）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteProductAdapter {

    private final RemoteProductService remoteProductService;

    /**
     * 批量查询 SKU，失败降级为空 Map
     *
     * @param skuIds SKU ID 列表
     * @return skuId → SKU 信息；调用失败时返回空 Map
     */
    public Map<Long, ProductSkuDTO> batchGetSkuSafely(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<ProductSkuDTO> skuList = remoteProductService.batchGetSku(skuIds);
            if (skuList == null || skuList.isEmpty()) {
                return Collections.emptyMap();
            }
            return skuList.stream()
                    .filter(sku -> sku.getSkuId() != null)
                    .collect(Collectors.toMap(ProductSkuDTO::getSkuId, Function.identity(), (a, b) -> a));
        } catch (Exception e) {
            // 降级：不抛异常，由调用方使用冗余字段兜底
            log.warn("batchGetSku 调用失败，降级使用冗余字段, skuIds={}, reason={}", skuIds, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 查询单个 SKU，失败降级为 null
     *
     * @param skuId SKU ID
     * @return SKU 信息，不存在或调用失败返回 null
     */
    public ProductSkuDTO getSkuSafely(Long skuId) {
        Map<Long, ProductSkuDTO> map = batchGetSkuSafely(List.of(skuId));
        return map.get(skuId);
    }

    // ═══════════════════════════════════════════════════════════
    // 库存操作 —— 刻意不降级
    // ═══════════════════════════════════════════════════════════
    // 查询类可以降级（展示问题），但库存扣减/释放属于**写操作**：
    // 静默失败会导致「C 端以为锁了库存、实际没锁」或「补偿漏掉」，
    // 因此这里让异常向上抛，由 OrderServiceImpl 捕获并触发补偿（设计文档 §5.7）。

    /**
     * 锁定库存（下单时）
     *
     * @param orderNo 订单号
     * @param items   预扣项
     * @return mall-product 返回是否全部成功
     * @throws RuntimeException Feign 调用失败或返回 false 时抛出
     */
    public boolean reserveStock(String orderNo, List<ReserveStockItemRequest> items) {
        boolean ok = remoteProductService.reserveStock(orderNo, items);
        if (!ok) {
            log.warn("reserveStock 返回失败, orderNo={}, items={}", orderNo, items);
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT);
        }
        return true;
    }

    /**
     * 释放已锁库存（取消订单 / 下单失败补偿）
     *
     * @param orderNo 订单号
     */
    public void releaseStock(String orderNo) {
        remoteProductService.releaseStock(orderNo);
        log.info("releaseStock 已调用, orderNo={}", orderNo);
    }

    /**
     * 回补库存（售后退货退款，§8.4）
     *
     * <p>同为写操作，不降级。</p>
     *
     * @param skuId SKU ID
     * @param qty   回补数量
     */
    public void restock(Long skuId, Integer qty) {
        remoteProductService.restock(skuId, qty);
        log.info("restock 已调用, skuId={}, qty={}", skuId, qty);
    }
}