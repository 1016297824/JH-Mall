package com.mall.payment.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

/**
 * Outbox 消息 DO
 *
 * <p>对应表 {@code mall_outbox}，用于「本地事务内写事件 + 异步投递 MQ」的可靠性保障。
 * 该表由多个模块<b>共用</b>（mall-order / mall-marketing / mall-payment），
 * 结构在各模块 DDL 中均有 {@code CREATE TABLE IF NOT EXISTS} 定义，
 * 通过 {@code aggregate_type} + {@code message_id} 区分来源。</p>
 *
 * <p>{@code status}：{@code NEW} 待投递 / {@code SENT} 已投递 / {@code FAILED} 已死信。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Data
@NoArgsConstructor
@TableName("mall_outbox")
public class MallOutboxDO {

    /** 主键（雪花 ID，非自增） */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /** 消息全局唯一 ID，投递去重用 */
    @TableField("message_id")
    private String messageId;

    /** 消息主题 */
    @TableField("topic")
    private String topic;

    /** 事件类型 */
    @TableField("event_type")
    private String eventType;

    /** 聚合类型，如 payment / refund */
    @TableField("aggregate_type")
    private String aggregateType;

    /** 聚合 ID，如 paymentNo / refundNo */
    @TableField("aggregate_id")
    private String aggregateId;

    /** 消息体 JSON */
    @TableField("payload")
    private String payload;

    /** 投递状态：NEW / SENT / FAILED */
    @TableField("status")
    private String status;

    /** 已重试次数 */
    @TableField("retry_count")
    private Integer retryCount;

    /** 下次重试时间 */
    @TableField("next_retry_time")
    private LocalDateTime nextRetryTime;

    /** 预约投递时间，为 null 表示立即投递 */
    @TableField("scheduled_time")
    private LocalDateTime scheduledTime;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
