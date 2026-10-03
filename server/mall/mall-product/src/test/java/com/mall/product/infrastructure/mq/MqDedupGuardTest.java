package com.mall.product.infrastructure.mq;

import com.mall.common.constant.CacheConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MQ 消费幂等去重守卫单元测试
 *
 * <p>重点锁定 {@code tryDedup} 与 {@code release} 必须使用<b>完全相同</b>的 key：
 * 若两者拼法不一致，失败后释放的就不是自己占的位，重投依然会被拦截。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class MqDedupGuardTest {

    private static final String MSG_ID = "MSG_20261003000001";

    private static final String GROUP = "mall-product-order-cancelled";

    /** 期望的 key 拼法：前缀 + messageId + ":" + consumerGroup */
    private static final String KEY = CacheConstants.MQ.DEDUP + MSG_ID + ":" + GROUP;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private MqDedupGuard dedupGuard;

    @Test
    @DisplayName("首次消费：SETNX 占位成功返回 true")
    void shouldReturnTrueOnFirstConsume() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(KEY), eq("1"), eq(24L), eq(TimeUnit.HOURS))).thenReturn(true);

        assertThat(dedupGuard.tryDedup(MSG_ID, GROUP)).isTrue();
    }

    @Test
    @DisplayName("重复投递：SETNX 占位失败返回 false")
    void shouldReturnFalseOnDuplicateDelivery() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(KEY), eq("1"), eq(24L), eq(TimeUnit.HOURS))).thenReturn(false);

        assertThat(dedupGuard.tryDedup(MSG_ID, GROUP)).isFalse();
    }

    @Test
    @DisplayName("缺少 messageId：不做去重，宁可重复处理也不漏处理")
    void shouldSkipDedupWhenMessageIdMissing() {
        assertThat(dedupGuard.tryDedup(null, GROUP)).isTrue();
        assertThat(dedupGuard.tryDedup("  ", GROUP)).isTrue();
    }

    @Test
    @DisplayName("release：删除与 tryDedup 完全相同的 key")
    void releaseShouldDeleteTheSameKey() {
        dedupGuard.release(MSG_ID, GROUP);

        verify(redisTemplate).delete(KEY);
    }

    @Test
    @DisplayName("release 缺少 messageId：不触碰 Redis")
    void releaseShouldDoNothingWhenMessageIdMissing() {
        dedupGuard.release(null, GROUP);

        verify(redisTemplate, never()).delete(anyString());
    }
}
