package com.mall.payment.infrastructure.channel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 渠道支付回调解析结果
 *
 * <p>由 {@link PaymentChannelAdapter#parsePayCallback} 返回，是「验签 + 解析」的合并产物：
 * 适配器负责用各平台 SDK 完成验签，并把异构的回调报文归一成如下字段。</p>
 *
 * <p>金额单位为<strong>分</strong>。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayCallbackResult {

    /** 验签是否通过；为 false 时其余字段无意义 */
    private boolean verified;

    /** 渠道侧支付单号，用于定位本地支付单 */
    private String channelPaymentNo;

    /** 渠道侧交易金额（单位：分），用于与本地支付单比对，防篡改 */
    private Long payAmount;

    /** 渠道侧交易状态原文 */
    private String channelPayStatus;

    /** 回调唯一标识，用于防重放（优先取渠道通知 ID，缺省用渠道交易号） */
    private String nonce;

    /** 失败原因（验签失败或报文无法解析） */
    private String failReason;
}
