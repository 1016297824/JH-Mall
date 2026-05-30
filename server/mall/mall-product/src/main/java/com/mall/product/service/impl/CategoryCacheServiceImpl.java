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

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryCacheServiceImpl implements ICategoryCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ICategoryService categoryService;

    private static final long TTL_SECONDS = 1800;

    @Override
    public List<CategoryVO> getTree() {
        String key = CacheConstants.Product.CATEGORY_TREE;
        @SuppressWarnings("unchecked")
        List<CategoryVO> cached = (List<CategoryVO>) redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }
        List<CategoryVO> tree = categoryService.tree();
        redisTemplate.opsForValue().set(key, tree, TTL_SECONDS, TimeUnit.SECONDS);
        return tree;
    }

    @Override
    public void refreshCache() {
        redisTemplate.delete(CacheConstants.Product.CATEGORY_TREE);
        log.debug("Category tree cache refreshed");
    }
}
