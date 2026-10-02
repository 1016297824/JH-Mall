package com.mall.payment.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退款失败事件 Payload
 *
 * <p>对应 Topic {@code mall:refund:failed}（设计文档 §8.1）。
 * mall-order 消费后通知用户退款失败，并允许重新发起售后。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundFailedEvent {

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

    /** 失败原因 */
    private String failReason;

    /** 渠道侧退款单号 */
    private String channelRefundNo;
}
