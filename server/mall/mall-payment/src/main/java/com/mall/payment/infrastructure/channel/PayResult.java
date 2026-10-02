package com.mall.payment.infrastructure.channel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 渠道发起支付结果
 *
 * <p>由 {@link PaymentChannelAdapter#invokePay} 返回，只在适配层与 Service 之间传递，
 * <b>不</b>直接作为接口响应。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayResult {

    /** 是否成功向渠道发起支付（网关/网络级成功，非「用户已付款」） */
    private boolean success;

    /** 渠道侧支付单号（微信 prepay_id / 支付宝 trade_no），用于后续对账与关单 */
    private String channelPaymentNo;

    /** 前端调起支付 SDK 所需的参数（appId / timeStamp / nonceStr / package / signType / paySign） */
    private Map<String, String> payParams;

    /** 渠道侧状态原文，仅作留痕 */
    private String channelPayStatus;

    /** 失败原因，{@code success=false} 时非空 */
    private String failReason;
}
