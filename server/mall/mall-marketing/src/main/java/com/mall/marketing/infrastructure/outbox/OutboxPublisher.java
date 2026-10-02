package com.mall.marketing.infrastructure.outbox;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.enums.OutboxStatusEnum;
import com.mall.marketing.DO.MallOutboxDO;
import com.mall.marketing.mapper.MallOutboxMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbox 消息发布器（业务侧）
 *
 * <p>与业务写操作<b>在同一本地事务</b>内落库 {@code mall_outbox}，
 * 由 {@link OutboxScheduler} 异步投递 RocketMQ，实现最终一致
 * （设计文档 §7.1「可靠消息投递」）。</p>
 *
 * <p>Payload 必须是精简 DTO，禁止直接序列化 DO（设计文档 §7.1 Payload 约束）。</p>
 *
 * <p>本模块目前只投递一种事件：{@code mall:coupon:used}（券核销事实）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    /** 聚合类型：券记录 */
    public static final String AGGREGATE_COUPON = "COUPON";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final MallOutboxMapper outboxMapper;

    /**
     * 发布消息（事务内落库，异步投递）
     *
     * @param topic       消息主题
     * @param eventType   事件类型
     * @param aggregateId 聚合 ID（券记录 ID）
     * @param payload     消息体，必须是精简 DTO
     */
    public void publish(String topic, String eventType, String aggregateId, Object payload) {
        MallOutboxDO message = new MallOutboxDO();
        // id 非自增（见 mall_outbox DDL），用 MyBatis-Plus 雪花 ID 生成
        message.setId(IdWorker.getId());
        message.setMessageId(UUID.randomUUID().toString().replace("-", ""));
        message.setTopic(topic);
        message.setEventType(eventType);
        message.setAggregateType(AGGREGATE_COUPON);
        message.setAggregateId(aggregateId);
        try {
            message.setPayload(OBJECT_MAPPER.writeValueAsString(payload));
        } catch (Exception e) {
            // 序列化失败属于编程错误，直接抛出让业务事务回滚
            throw new IllegalStateException("Outbox payload 序列化失败, topic=" + topic, e);
        }
        message.setStatus(OutboxStatusEnum.NEW.getCode());
        message.setRetryCount(0);
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());
        outboxMapper.insert(message);

        log.debug("Outbox 已落库: topic={}, eventType={}, aggregateId={}", topic, eventType, aggregateId);
    }
}
