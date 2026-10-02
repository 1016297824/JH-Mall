package com.mall.marketing.DO;

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
 * <p>对应表 {@code mall_outbox}。与业务写操作同事务落库，由调度器异步投递 RocketMQ，
 * 实现「本地事务 + 消息」最终一致。</p>
 *
 * <p>本模块用它投递 {@code mall:coupon:used} 核销事件。</p>
 *
 * <p><b>注意</b>：{@code id} 为 {@code bigint NOT NULL} 而非自增，需应用侧生成。
 * 表无 is_deleted / version 列。</p>
 *
 * <p>{@code status} 取值见 {@code mall-common} 的 {@code OutboxStatusEnum}
 * （NEW / PENDING / SENT / FAILED / CANCELLED）。</p>
 *
 * <p><b>与 mall-order 的关系</b>：两者共用同一张 {@code mall_outbox} 表，
 * 但各自持有独立的 DO 与 Mapper（沿用现有模块私有模式），
 * 互不感知对方的 topic 与聚合类型。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Data
@NoArgsConstructor
@TableName("mall_outbox")
public class MallOutboxDO {

    /** 主键，非自增，需应用侧生成 */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /** 消息全局唯一 ID，消费端幂等去重用 */
    @TableField("message_id")
    private String messageId;

    /** 消息主题，如 mall:coupon:used */
    @TableField("topic")
    private String topic;

    /** 事件类型，如 CouponUsed */
    @TableField("event_type")
    private String eventType;

    /** 聚合类型，本模块固定为 COUPON */
    @TableField("aggregate_type")
    private String aggregateType;

    /** 聚合 ID，通常是券记录 ID 或订单号 */
    @TableField("aggregate_id")
    private String aggregateId;

    /** 消息体 JSON，禁止直接序列化 DO，须用精简 DTO */
    @TableField("payload")
    private String payload;

    /** 投递状态，取值见 OutboxStatusEnum */
    @TableField("status")
    private String status;

    /** 已重试次数，上限 3 次后置 FAILED */
    @TableField("retry_count")
    private Integer retryCount;

    /** 下次重试时间（指数退避：10s / 30s / 60s + 抖动） */
    @TableField("next_retry_time")
    private LocalDateTime nextRetryTime;

    /** 预约投递时间，NULL=立即投递 */
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
