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

/**
 * SKU 缓存服务实现
 *
 * <p>缓存 SKU 详情，TTL 10 分钟</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkuCacheServiceImpl implements ISkuCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ISkuService skuService;

    /** SKU 缓存 TTL（秒） */
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
