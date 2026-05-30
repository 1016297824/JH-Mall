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

@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl implements IStockService {

    private final MallSkuStockMapper mallSkuStockMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reserveStock(String orderNo, List<ReserveStockItemRequest> items) {
        for (ReserveStockItemRequest item : items) {
            MallSkuStockDO stock = mallSkuStockMapper.selectBySkuId(item.getSkuId());
            if (stock == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            int affected = mallSkuStockMapper.reserveStock(item.getSkuId(), item.getQty(), stock.getVersion());
            if (affected == 0) {
                throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT);
            }
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
        mallSkuStockMapper.restock(skuId, qty, stock.getVersion());
    }
}
