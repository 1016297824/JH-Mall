package com.mall.order.infrastructure.mq;

import com.mall.common.constant.CacheConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * MQ 消费幂等去重守卫
 *
 * <p>Redis SETNX 实现，key 模式 {@code mall:mq:dedup:{messageId}:{consumerGroup}}，
 * TTL 24h（设计文档 §7.4）。</p>
 *
 * <p>去重标记默认保留至 TTL 自然过期；仅在<b>消费失败</b>时由调用方显式调用
 * {@link #release(String, String)} 删除，否则 MQ 重投会被去重拦截、消息被永久放弃。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqDedupGuard {

    /** 去重标记存活时间 */
    private static final long DEDUP_TTL_HOURS = 24L;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 尝试占位去重标记
     *
     * @param messageId     消息全局唯一 ID
     * @param consumerGroup 消费组
     * @return true=首次消费，应处理；false=重复投递，应跳过
     */
    public boolean tryDedup(String messageId, String consumerGroup) {
        if (messageId == null || messageId.isBlank()) {
            // 无消息 ID 时不做去重，宁可重复处理也不能漏处理
            log.warn("消息缺少 messageId，跳过去重");
            return true;
        }
        String key = CacheConstants.MQ.DEDUP + messageId + ":" + consumerGroup;
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", DEDUP_TTL_HOURS, TimeUnit.HOURS);
        if (!Boolean.TRUE.equals(first)) {
            log.info("重复投递已跳过: messageId={}, group={}", messageId, consumerGroup);
            return false;
        }
        return true;
    }

    /**
     * 释放去重标记，允许 MQ 重投时重新处理
     *
     * <p>仅在处理失败时调用。若不释放，重投会被 {@link #tryDedup} 拦截，
     * 等于永久放弃该消息。</p>
     *
     * @param messageId     消息全局唯一 ID
     * @param consumerGroup 消费组
     */
    public void release(String messageId, String consumerGroup) {
        if (messageId == null || messageId.isBlank()) {
            return;
        }
        redisTemplate.delete(CacheConstants.MQ.DEDUP + messageId + ":" + consumerGroup);
        log.warn("去重标记已释放，允许重投: messageId={}, group={}", messageId, consumerGroup);
    }
}