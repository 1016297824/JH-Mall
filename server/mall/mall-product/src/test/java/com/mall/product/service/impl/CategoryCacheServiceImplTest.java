package com.mall.product.service.impl;

import com.mall.product.VO.CategoryVO;
import com.mall.product.service.ICategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryCacheServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ICategoryService categoryService;

    @InjectMocks
    private CategoryCacheServiceImpl cacheService;

    @Test
    void getTreeShouldReturnFromCacheWhenHit() {
        List<CategoryVO> cached = List.of(new CategoryVO());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("mall:product:category:tree")).thenReturn(cached);

        List<CategoryVO> result = cacheService.getTree();

        assertThat(result).isSameAs(cached);
    }

    @Test
    void getTreeShouldFallbackToDBWhenCacheMiss() {
        List<CategoryVO> dbResult = List.of(new CategoryVO());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("mall:product:category:tree")).thenReturn(null);
        when(categoryService.tree()).thenReturn(dbResult);

        List<CategoryVO> result = cacheService.getTree();

        assertThat(result).isSameAs(dbResult);
        verify(valueOperations).set(eq("mall:product:category:tree"), eq(dbResult), eq(1800L), eq(TimeUnit.SECONDS));
    }
}
