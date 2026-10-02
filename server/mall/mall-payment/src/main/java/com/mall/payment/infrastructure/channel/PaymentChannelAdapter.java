package com.mall.payment.infrastructure.channel;

import com.mall.payment.DO.MallPaymentChannelDO;
import com.mall.payment.DO.MallPaymentDO;
import com.mall.payment.DO.MallRefundDO;

import java.util.Map;

/**
 * 支付渠道适配器
 *
 * <p>屏蔽各支付平台（微信 / 支付宝 / 模拟渠道）的 SDK 差异，向上只暴露三个动作：
 * 发起支付、发起退款、账单查询。对应设计文档
 * {@code docs/design/13_mall-payment详细设计.md} §4.3 与 §9.1。</p>
 *
 * <p><b>验签职责</b>：渠道回调的验签由具体适配器实现（使用各平台 SDK），
 * <b>不</b>放在网关 —— 网关无法实现微信 SHA256-RSA / 支付宝 RSA2 验签
 * （设计 §3.5/§5.1 的「网关验签」描述在技术上不成立，见实施计划 §0.2 ②）。</p>
 *
 * <p>金额字段一律 {@code Long}，单位<strong>分</strong>。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
public interface PaymentChannelAdapter {

    /**
     * 向渠道发起支付
     *
     * @param payment 支付单
     * @param channel 渠道配置
     * @param openid  用户在该渠道的标识（微信 JSAPI/小程序必需，H5/Native 可为 null）
     * @return 发起结果，含渠道支付单号与前端调起参数
     */
    PayResult invokePay(MallPaymentDO payment, MallPaymentChannelDO channel, String openid);

    /**
     * 向渠道发起退款
     *
     * <p>渠道编码必须与原始支付一致，由调用方保证。</p>
     *
     * @param payment 原支付单
     * @param refund  退款单
     * @param channel 渠道配置
     * @return 退款结果，含渠道退款单号与退款状态
     */
    RefundResult invokeRefund(MallPaymentDO payment, MallRefundDO refund, MallPaymentChannelDO channel);

    /**
     * 查询渠道侧交易状态
     *
     * <p>用于回调丢失时的主动对账。</p>
     *
     * @param channelPaymentNo 渠道侧支付单号
     * @param channel          渠道配置
     * @return 账单查询结果
     */
    ChannelBillResult queryBill(String channelPaymentNo, MallPaymentChannelDO channel);

    /**
     * 验签并解析支付回调
     *
     * <p>把各平台异构的回调报文（微信 XML/JSON + SHA256-RSA 签名、支付宝表单 + RSA2 签名）
     * 归一为 {@link PayCallbackResult}。验签是<b>适配器职责</b>：网关不具备各渠道的私钥与平台证书，
     * 无法完成渠道验签。</p>
     *
     * @param rawBody 回调原始报文
     * @param headers 回调请求头（部分渠道的签名在头部，如微信 APIv3）
     * @return 解析结果，验签失败时 {@code verified=false}
     */
    PayCallbackResult parsePayCallback(String rawBody, Map<String, String> headers);

    /**
     * 验签并解析退款回调
     *
     * @param rawBody 回调原始报文
     * @param headers 回调请求头
     * @return 解析结果，验签失败时 {@code verified=false}
     */
    RefundCallbackResult parseRefundCallback(String rawBody, Map<String, String> headers);
}
