package com.mall.product.DO;

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
 * Outbox 消息 DO（可靠消息表）
 *
 * <p>用于搜索同步等场景的可靠投递，消息先写 Outbox 再异步发送</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Data
@NoArgsConstructor
@TableName("mall_outbox")
public class OutboxMessageDO {

    /** 主键 ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 消息唯一标识 */
    @TableField("message_id")
    private String messageId;

    /** 消息主题 */
    @TableField("topic")
    private String topic;

    /** 事件类型 */
    @TableField("event_type")
    private String eventType;

    /** 聚合类型 */
    @TableField("aggregate_type")
    private String aggregateType;

    /** 聚合 ID */
    @TableField("aggregate_id")
    private String aggregateId;

    /** 消息体 JSON */
    @TableField("payload")
    private String payload;

    /** 状态（NEW/SENT/FAILED） */
    @TableField("status")
    private String status;

    /** 重试次数 */
    @TableField("retry_count")
    private Integer retryCount;

    /** 下次重试时间 */
    @TableField("next_retry_time")
    private LocalDateTime nextRetryTime;

    /** 预约发送时间 */
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
