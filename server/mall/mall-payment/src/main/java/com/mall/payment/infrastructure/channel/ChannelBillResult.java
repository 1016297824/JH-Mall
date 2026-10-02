package com.mall.payment.infrastructure.channel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 渠道账单查询结果
 *
 * <p>由 {@link PaymentChannelAdapter#queryBill} 返回，用于「回调丢失补偿」场景：
 * 定时任务对长时间处于 UNPAID 的支付单主动向渠道查询真实交易状态。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChannelBillResult {

    /** 是否查询成功（网络/鉴权层面） */
    private boolean success;

    /** 渠道侧交易状态原文（如微信 SUCCESS / NOTPAY / CLOSED） */
    private String channelTradeStatus;

    /** 渠道侧交易金额（单位：分），查不到为 null */
    private Long tradeAmount;

    /** 渠道侧交易号 */
    private String channelPaymentNo;

    /** 失败原因，{@code success=false} 时非空 */
    private String failReason;
}
