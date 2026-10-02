package com.mall.payment.infrastructure.channel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 渠道发起退款结果
 *
 * <p>由 {@link PaymentChannelAdapter#invokeRefund} 返回。注意区分两层语义：</p>
 * <ul>
 *   <li>{@code success}：「是否成功把退款请求送达渠道」</li>
 *   <li>{@code refundStatus}：「退款本身的最终结果」，取值见 {@code RefundStatusEnum}</li>
 * </ul>
 *
 * <p>渠道受理后常见的是 {@code PROCESSING}（等渠道异步回调推进终态），
 * 只有部分渠道会同步返回 {@code SUCCESS} 或明确 {@code FAILED}。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundResult {

    /** 是否成功向渠道发起退款请求 */
    private boolean success;

    /** 渠道侧退款单号 */
    private String channelRefundNo;

    /** 退款状态，取值见 {@code RefundStatusEnum} */
    private Integer refundStatus;

    /** 渠道侧状态原文，仅作留痕 */
    private String channelRefundStatus;

    /** 失败原因，{@code success=false} 时非空 */
    private String failReason;
}
