package com.mall.product.service.impl;

import com.mall.common.constant.CacheConstants;
import com.mall.product.VO.SkuVO;
import com.mall.product.service.ISkuCacheService;
import com.mall.product.service.ISkuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkuCacheServiceImpl implements ISkuCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ISkuService skuService;

    private static final long TTL_SECONDS = 600;

    @Override
    public SkuVO getBySkuId(Long skuId) {
        String key = CacheConstants.Product.SKU + skuId;
        SkuVO cached = (SkuVO) redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }
        SkuVO skuVO = skuService.getBySkuId(skuId);
        redisTemplate.opsForValue().set(key, skuVO, TTL_SECONDS, TimeUnit.SECONDS);
        return skuVO;
    }

    @Override
    public void evictCache(Long skuId) {
        redisTemplate.delete(CacheConstants.Product.SKU + skuId);
    }
}
