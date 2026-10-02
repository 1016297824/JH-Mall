package com.mall.payment.infrastructure.channel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 渠道退款回调解析结果
 *
 * <p>由 {@link PaymentChannelAdapter#parseRefundCallback} 返回。</p>
 *
 * <p>⚠️ <b>为何用渠道退款单号定位</b>：渠道回调只携带它自己的退款单号，
 * 因此本地退款单必须在<b>渠道受理时</b>就把 {@code channel_refund_no} 落库
 * （见 {@code MallRefundMapper.updateChannelRefundNo}），否则此处无法定位。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundCallbackResult {

    /** 验签是否通过；为 false 时其余字段无意义 */
    private boolean verified;

    /** 渠道侧退款单号，用于定位本地退款单 */
    private String channelRefundNo;

    /** 渠道侧退款金额（单位：分） */
    private Long refundAmount;

    /** 退款状态，适配器负责把渠道状态原文映射为 {@code RefundStatusEnum} 的码值 */
    private Integer refundStatus;

    /** 渠道侧退款状态原文 */
    private String channelRefundStatus;

    /** 回调唯一标识，用于防重放 */
    private String nonce;

    /** 失败原因（验签失败或报文无法解析） */
    private String failReason;
}
