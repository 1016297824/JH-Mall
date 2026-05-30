package com.mall.product.service;

import com.mall.api.feign.RemoteProductService;
import java.util.List;

/**
 * 库存服务接口
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
public interface IStockService {

    /**
     * 预扣库存
     *
     * @param orderNo 订单号（幂等键）
     * @param items   预扣项列表
     * @return 是否全部扣减成功
     */
    boolean reserveStock(String orderNo, List<RemoteProductService.ReserveStockItemRequest> items);

    /**
     * 释放预扣库存（订单取消时调用）
     *
     * @param orderNo 订单号
     */
    void releaseStock(String orderNo);

    /**
     * 补货（增加可用库存）
     *
     * @param skuId SKU ID
     * @param qty   补货数量
     */
    void restock(Long skuId, Integer qty);
}
