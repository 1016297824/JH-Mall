package com.mall.payment.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 退款成功事件 Payload
 *
 * <p>对应 Topic {@code mall:refund:succeeded}（设计文档 §8.1）。
 * mall-order 消费后推进售后单状态并通知用户。</p>
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

    /** 退款成功时间 */
    private LocalDateTime refundTime;

    /** 渠道侧退款单号（对账用） */
    private String channelRefundNo;
}
