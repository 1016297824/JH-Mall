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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.BoundZSetOperations;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
        configProps.getHot().setUvWindowDays(7);

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

    /**
     * incrUv 应写入日分片键并设置 TTL
     */
    @Test
    void incrUvShouldWriteToDailyShardedKey() {
        @SuppressWarnings("unchecked")
        HyperLogLogOperations<String, Object> hllOps = mock(HyperLogLogOperations.class);
        when(redisTemplate.opsForHyperLogLog()).thenReturn(hllOps);

        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String expectedKey = CacheConstants.Product.UV + "1001:" + today;

        service.incrUv(1001L, 888L);

        // 验证 PFADD 使用了日分片键
        verify(hllOps).add(eq(expectedKey), eq("888"));

        // 验证 TTL 设置为 uvWindowDays + 2 = 9 天
        verify(redisTemplate).expire(eq(expectedKey), eq(Duration.ofDays(9)));
    }

    /**
     * refreshHotRank 应使用多键 PFCOUNT 做滑动窗口 UV 联合估算
     */
    @Test
    @SuppressWarnings("unchecked")
    void refreshHotRankShouldUseSlidingWindowMultiKeyPfcount() {
        // 1. 分布式锁
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq(CacheConstants.Job.LOCK_HOT_RANK), eq("1"), any(Duration.class)))
                .thenReturn(true);

        HyperLogLogOperations<String, Object> hllOps = mock(HyperLogLogOperations.class);
        when(redisTemplate.opsForHyperLogLog()).thenReturn(hllOps);

        // 2. MySQL 返回 1 条 SPU
        MallProductSpuDO spu = new MallProductSpuDO();
        spu.setId(1001L);
        spu.setSalesCount(100);
        Page<MallProductSpuDO> page = new Page<>(1, 200);
        page.setRecords(Collections.singletonList(spu));
        when(spuMapper.selectPage(any(Page.class), any())).thenReturn(page);

        // 3. ZSet 操作
        BoundZSetOperations<String, Object> boundZSet = mock(BoundZSetOperations.class);
        when(redisTemplate.boundZSetOps(CacheConstants.Product.HOT_RANK)).thenReturn(boundZSet);

        // 4. refreshHotRank 会清空 ZSet 和释放锁
        when(redisTemplate.delete(CacheConstants.Product.HOT_RANK)).thenReturn(true);
        when(redisTemplate.delete(CacheConstants.Job.LOCK_HOT_RANK)).thenReturn(true);

        service.refreshHotRank();

        // 5. 验证 PFCOUNT 用日分片多键调用
        //    应传 7 个键（最近 7 天）：mall:product:uv:1001:{today} ... mall:product:uv:1001:{today-6}
        ArgumentCaptor<String[]> keysCaptor = ArgumentCaptor.forClass(String[].class);
        verify(hllOps).size(keysCaptor.capture());
        String[] capturedKeys = keysCaptor.getValue();

        assertThat(capturedKeys).hasSize(7);
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            String expectedKey = CacheConstants.Product.UV + "1001:" + today.minusDays(i).format(DateTimeFormatter.BASIC_ISO_DATE);
            assertThat(capturedKeys[i]).isEqualTo(expectedKey);
        }

        // 6. 验证 ZSet 写入了综合热度分
        //    热度分 = 100 * 10 * 0.6 + uv * 10 * 0.4（uv 是 mock 返回值，未设 stubbing 则为 null → 0）
        verify(boundZSet).add(eq("1001"), anyDouble());
    }
}
