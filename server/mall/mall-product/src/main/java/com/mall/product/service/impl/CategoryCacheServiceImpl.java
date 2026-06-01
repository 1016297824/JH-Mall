package com.mall.product.service.impl;

import com.mall.common.constant.CacheConstants;
import com.mall.product.VO.CategoryVO;
import com.mall.product.service.ICategoryCacheService;
import com.mall.product.service.ICategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 类目缓存服务实现
 *
 * <p>缓存类目树，TTL 30 分钟</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryCacheServiceImpl implements ICategoryCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ICategoryService categoryService;

    /** 类目树缓存 TTL（秒） */
    private static final long TTL_SECONDS = 1800;

    @Override
    public List<CategoryVO> getTree() {
        // 先从 Redis 读取缓存的类目树，避免频繁穿透 DB
        String key = CacheConstants.Product.CATEGORY_TREE;
        @SuppressWarnings("unchecked")
        List<CategoryVO> cached = (List<CategoryVO>) redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }
        // 缓存未命中，查 DB 构建类目树后回填 Redis，TTL 30 分钟
        List<CategoryVO> tree = categoryService.tree();
        redisTemplate.opsForValue().set(key, tree, TTL_SECONDS, TimeUnit.SECONDS);
        return tree;
    }

    @Override
    public void refreshCache() {
        // 直接删除缓存键，下次 getTree 时自动重新加载
        redisTemplate.delete(CacheConstants.Product.CATEGORY_TREE);
        log.debug("Category tree cache refreshed");
    }
}
