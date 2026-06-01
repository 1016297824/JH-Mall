package com.mall.product.service.impl;

import com.mall.common.constant.CacheConstants;
import com.mall.product.DO.MallProductSpuDO;
import com.mall.product.VO.SpuVO;
import com.mall.product.config.MallProductConfigProperties;
import com.mall.product.mapper.MallProductSpuMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.BoundZSetOperations;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotProductServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private MallProductSpuMapper spuMapper;

    private Cache<Long, SpuVO> hotProductCache;
    private MallProductConfigProperties configProps;
    private HotProductServiceImpl service;

    @BeforeEach
    void setUp() {
        hotProductCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterAccess(Duration.ofMinutes(5))
            .build();

        configProps = new MallProductConfigProperties();
        configProps.getHot().setRankMaxSize(200);
        configProps.getHot().setSalesWeight(0.6);
        configProps.getHot().setUvWeight(0.4);

        service = new HotProductServiceImpl(hotProductCache, redisTemplate, spuMapper, configProps);
    }

    @Test
    void hotListShouldReturnEmptyWhenZSetEmpty() {
        @SuppressWarnings("unchecked")
        BoundZSetOperations<String, Object> boundZSet = mock(BoundZSetOperations.class);
        when(redisTemplate.boundZSetOps(CacheConstants.Product.HOT_RANK)).thenReturn(boundZSet);
        when(boundZSet.reverseRange(0, 19)).thenReturn(Collections.emptySet());

        List<SpuVO> result = service.hotList(20);

        assertThat(result).isEmpty();
    }

    @Test
    void incrHotRankShouldIncrementZSetScore() {
        @SuppressWarnings("unchecked")
        BoundZSetOperations<String, Object> boundZSet = mock(BoundZSetOperations.class);
        when(redisTemplate.boundZSetOps(CacheConstants.Product.HOT_RANK)).thenReturn(boundZSet);

        service.incrHotRank(1L, 3);

        verify(boundZSet).incrementScore("1", 30.0);
    }
}
