package com.mall.product.service.impl;

import com.mall.api.feign.RemoteProductService.ReserveStockItemRequest;
import com.mall.common.exception.BusinessException;
import com.mall.product.DO.MallSkuStockDO;
import com.mall.product.mapper.MallSkuStockMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    @Mock private MallSkuStockMapper mallSkuStockMapper;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @InjectMocks private StockServiceImpl stockService;

    @Test
    void reserveStockShouldSucceed() {
        MallSkuStockDO stock = new MallSkuStockDO();
        stock.setSkuId(101L); stock.setAvailableStock(100); stock.setVersion(1);
        when(mallSkuStockMapper.selectBySkuId(101L)).thenReturn(stock);
        when(mallSkuStockMapper.reserveStock(101L, 1, 1)).thenReturn(1);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ReserveStockItemRequest item = new ReserveStockItemRequest(101L, 1);
        boolean result = stockService.reserveStock("ORD001", List.of(item));

        assertThat(result).isTrue();
    }

    @Test
    void reserveStockShouldThrowWhenInsufficient() {
        MallSkuStockDO stock = new MallSkuStockDO();
        stock.setSkuId(101L); stock.setAvailableStock(0); stock.setVersion(1);
        when(mallSkuStockMapper.selectBySkuId(101L)).thenReturn(stock);
        when(mallSkuStockMapper.reserveStock(101L, 10, 1)).thenReturn(0);

        ReserveStockItemRequest item = new ReserveStockItemRequest(101L, 10);
        assertThatThrownBy(() -> stockService.reserveStock("ORD001", List.of(item)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void restockShouldSucceed() {
        MallSkuStockDO stock = new MallSkuStockDO();
        stock.setSkuId(101L); stock.setVersion(1);
        when(mallSkuStockMapper.selectBySkuId(101L)).thenReturn(stock);
        when(mallSkuStockMapper.restock(101L, 5, 1)).thenReturn(1);

        stockService.restock(101L, 5);
    }
}
