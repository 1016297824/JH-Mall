package com.mall.product.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

/**
 * {@link HotProductServiceImpl} 单元测试
 *
 * <p>覆盖热点排行查询、排名增量更新等核心逻辑</p>
 *
 * @author JH-Mall
 * @date 2026/06/01
 */
@ExtendWith(MockitoExtension.class)
class HotProductServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private MallProductSpuMapper spuMapper;

    /** Caffeine 手动构建，受 mock 隔离 */
    private Cache<Long, SpuVO> hotProductCache;
    /** 默认配置（rankMaxSize=200, salesWeight=0.6, uvWeight=0.4） */
    private MallProductConfigProperties configProps;
    private HotProductServiceImpl service;

    /**
     * 初始化 Caffeine 缓存和配置，通过人工构造注入依赖
     */
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

    /**
     * ZSet 为空时应返回空列表（MySQL 降级也会受 mock 影响）
     */
    @Test
    void hotListShouldReturnEmptyWhenZSetEmpty() {
        @SuppressWarnings("unchecked")
        BoundZSetOperations<String, Object> boundZSet = mock(BoundZSetOperations.class);
        when(redisTemplate.boundZSetOps(CacheConstants.Product.HOT_RANK)).thenReturn(boundZSet);
        when(boundZSet.reverseRange(0, 19)).thenReturn(Collections.emptySet());
        when(spuMapper.selectPage(any(Page.class), any())).thenReturn(new Page<>(1, 20));

        List<SpuVO> result = service.hotList(20);

        assertThat(result).isEmpty();
    }
}
