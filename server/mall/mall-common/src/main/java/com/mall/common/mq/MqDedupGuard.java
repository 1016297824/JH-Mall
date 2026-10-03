package com.mall.common.mq;

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
 * <p><b>放在 mall-common</b>：本类原在 mall-product / mall-marketing / mall-order
 * 各有一份<b>逐字相同</b>的副本，新增消费者时极容易漏掉或改歪其中一份
 * （mall-user 就因此完全没有去重，MQ 重投会重复发放积分）。7 个 mall 模块的
 * {@code scanBasePackages} 都含 {@code com.mall.common}，故 {@code @Component}
 * 直接生效，各模块无需额外装配。</p>
 *
 * <p>去重标记默认保留至 TTL 自然过期；仅在<b>消费失败</b>时由调用方显式调用
 * {@link #release(String, String)} 删除，否则 MQ 重投会被去重拦截、消息被永久放弃
 * （例如库存永不回补、积分永不发放）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqDedupGuard {

    /** 去重标记存活时间（小时） */
    private static final long DEDUP_TTL_HOURS = 24L;

    /** 占位值，仅表示「该消息已被某消费组处理过」 */
    private static final String PLACEHOLDER = "1";

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
        String key = buildKey(messageId, consumerGroup);
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(key, PLACEHOLDER, DEDUP_TTL_HOURS, TimeUnit.HOURS);
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
        redisTemplate.delete(buildKey(messageId, consumerGroup));
        log.warn("去重标记已释放，允许重投: messageId={}, group={}", messageId, consumerGroup);
    }

    /**
     * 拼装去重 key
     *
     * <p>{@link #tryDedup} 与 {@link #release} <b>必须</b>走同一个方法：
     * 两者拼法一旦分叉，失败后释放的就不是自己占的那个位，重投依旧会被拦截。</p>
     *
     * @param messageId     消息全局唯一 ID
     * @param consumerGroup 消费组
     * @return Redis key
     */
    private String buildKey(String messageId, String consumerGroup) {
        return CacheConstants.MQ.DEDUP + messageId + ":" + consumerGroup;
    }
}
