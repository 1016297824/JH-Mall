package com.mall.product.service;

import com.mall.api.feign.RemoteProductService;
import java.util.List;

public interface IStockService {

    boolean reserveStock(String orderNo, List<RemoteProductService.ReserveStockItemRequest> items);

    void releaseStock(String orderNo);

    void restock(Long skuId, Integer qty);
}
