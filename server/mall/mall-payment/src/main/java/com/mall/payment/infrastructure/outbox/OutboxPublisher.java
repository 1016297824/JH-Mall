package com.mall.payment.infrastructure.outbox;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.enums.OutboxStatusEnum;
import com.mall.payment.DO.MallOutboxDO;
import com.mall.payment.mapper.MallOutboxMapper;
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
 * （设计文档 {@code docs/design/13_mall-payment详细设计.md} §8.3）。</p>
 *
 * <p>Payload 必须是精简 DTO，禁止直接序列化 DO（同 §8.1 的字段约束）。
 * 直接序列化 DO 会把 {@code version}、{@code isDeleted} 等内部字段泄露给消费方，
 * 且 DO 字段变更会静默改变消息契约。</p>
 *
 * <p><b>本模块投递三类事件</b>：{@code mall:payment:paid}、
 * {@code mall:refund:succeeded}、{@code mall:refund:failed}（§8.1）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    /** 聚合类型：支付单 */
    public static final String AGGREGATE_PAYMENT = "PAYMENT";

    /** 聚合类型：退款单 */
    public static final String AGGREGATE_REFUND = "REFUND";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final MallOutboxMapper outboxMapper;

    /**
     * 发布消息（事务内落库，异步投递）
     *
     * @param topic         消息主题，取自 {@code MqTopicConstants.Payment}
     * @param eventType     事件类型，用于消费方区分语义
     * @param aggregateType 聚合类型，见 {@link #AGGREGATE_PAYMENT} / {@link #AGGREGATE_REFUND}
     * @param aggregateId   聚合 ID（支付单号或退款单号）
     * @param payload       消息体，必须是精简 DTO
     */
    public void publish(String topic, String eventType, String aggregateType,
                        String aggregateId, Object payload) {
        MallOutboxDO message = new MallOutboxDO();
        // id 非自增（见 mall_outbox DDL），用 MyBatis-Plus 雪花 ID 生成
        message.setId(IdWorker.getId());
        message.setMessageId(UUID.randomUUID().toString().replace("-", ""));
        message.setTopic(topic);
        message.setEventType(eventType);
        message.setAggregateType(aggregateType);
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
