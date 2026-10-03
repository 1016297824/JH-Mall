package com.mall.payment.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退款成功事件 Payload
 *
 * <p>对应 Topic {@code mall:refund:succeeded}（设计文档 §8.1）。
 * mall-order 消费后推进售后单状态并通知用户。</p>
 *
 * <p><b>时间字段为 ISO-8601 字符串</b>：Outbox 用裸 {@code ObjectMapper} 序列化 payload，
 * 未注册 JavaTimeModule，放 {@code LocalDateTime} 会在写 Outbox 时抛
 * {@code InvalidDefinitionException}，导致退款回调整体失败。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundSucceededEvent {

    /** 退款单号 */
    private String refundNo;

    /** 支付单号 */
    private String paymentNo;

    /** 订单号 */
    private String orderNo;

    /** 售后单号 */
    private String afterSaleNo;

    /** 用户 ID */
    private Long userId;

    /** 退款金额（单位：分） */
    private Long refundAmount;

    /** 退款成功时间，ISO-8601 字符串（如 2026-05-17T10:30:00） */
    private String refundTime;

    /** 渠道侧退款单号（对账用） */
    private String channelRefundNo;
}
