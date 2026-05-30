package com.mall.product.service.impl;

import com.mall.product.VO.SkuVO;
import com.mall.product.service.ISkuService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkuCacheServiceImplTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private ISkuService skuService;
    @InjectMocks private SkuCacheServiceImpl cacheService;

    @Test
    void getBySkuIdShouldReturnFromCacheWhenHit() {
        SkuVO cached = new SkuVO(); cached.setSkuId("101");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("mall:product:sku:101")).thenReturn(cached);

        SkuVO result = cacheService.getBySkuId(101L);

        assertThat(result).isSameAs(cached);
    }

    @Test
    void getBySkuIdShouldFallbackWhenCacheMiss() {
        SkuVO dbResult = new SkuVO(); dbResult.setSkuId("101");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("mall:product:sku:101")).thenReturn(null);
        when(skuService.getBySkuId(101L)).thenReturn(dbResult);

        SkuVO result = cacheService.getBySkuId(101L);

        assertThat(result).isSameAs(dbResult);
        verify(valueOperations).set(eq("mall:product:sku:101"), eq(dbResult), eq(600L), eq(TimeUnit.SECONDS));
    }
}
