package com.mall.marketing.infrastructure.mq;

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
 * MQ 幂等去重守卫单元测试
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@ExtendWith(MockitoExtension.class)
class MqDedupGuardTest {

    private static final String MESSAGE_ID = "MSG_001";
    private static final String CONSUMER_GROUP = "mall-marketing-order-paid";

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @InjectMocks private MqDedupGuard dedupGuard;

    @Test
    @DisplayName("首次消费：SETNX 成功 → 返回 true 表示应处理")
    void firstConsumeShouldReturnTrue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(CacheConstants.MQ.DEDUP + MESSAGE_ID + ":" + CONSUMER_GROUP),
                eq("1"), eq(24L), eq(TimeUnit.HOURS))).thenReturn(true);

        assertThat(dedupGuard.tryDedup(MESSAGE_ID, CONSUMER_GROUP)).isTrue();
    }

    @Test
    @DisplayName("重复投递：SETNX 失败 → 返回 false 表示应跳过")
    void duplicateConsumeShouldReturnFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(CacheConstants.MQ.DEDUP + MESSAGE_ID + ":" + CONSUMER_GROUP),
                eq("1"), eq(24L), eq(TimeUnit.HOURS))).thenReturn(false);

        assertThat(dedupGuard.tryDedup(MESSAGE_ID, CONSUMER_GROUP)).isFalse();
    }

    @Test
    @DisplayName("缺少 messageId 时不做去重，返回 true（宁可重复也不能漏处理）")
    void missingMessageIdShouldSkipDedup() {
        assertThat(dedupGuard.tryDedup(null, CONSUMER_GROUP)).isTrue();
        assertThat(dedupGuard.tryDedup("  ", CONSUMER_GROUP)).isTrue();
    }

    @Test
    @DisplayName("去重标记 TTL 为 24 小时")
    void dedupMarkerShouldHaveTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(CacheConstants.MQ.DEDUP + MESSAGE_ID + ":" + CONSUMER_GROUP),
                eq("1"), eq(24L), eq(TimeUnit.HOURS))).thenReturn(true);

        dedupGuard.tryDedup(MESSAGE_ID, CONSUMER_GROUP);

        verify(valueOperations).setIfAbsent(CacheConstants.MQ.DEDUP + MESSAGE_ID + ":" + CONSUMER_GROUP,
                "1", 24L, TimeUnit.HOURS);
    }

    @Test
    @DisplayName("release：删除与 tryDedup 完全相同的 key（拼法不一致会导致重投仍被拦截）")
    void releaseShouldDeleteTheSameKey() {
        dedupGuard.release(MESSAGE_ID, CONSUMER_GROUP);

        verify(redisTemplate).delete(CacheConstants.MQ.DEDUP + MESSAGE_ID + ":" + CONSUMER_GROUP);
    }

    @Test
    @DisplayName("release 缺少 messageId：不触碰 Redis")
    void releaseShouldDoNothingWhenMessageIdMissing() {
        dedupGuard.release(null, CONSUMER_GROUP);

        verify(redisTemplate, never()).delete(anyString());
    }
}
