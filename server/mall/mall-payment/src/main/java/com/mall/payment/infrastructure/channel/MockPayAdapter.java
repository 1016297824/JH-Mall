package com.mall.payment.infrastructure.channel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.enums.payment.RefundStatusEnum;
import com.mall.payment.DO.MallPaymentChannelDO;
import com.mall.payment.DO.MallPaymentDO;
import com.mall.payment.DO.MallRefundDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模拟支付渠道适配器
 *
 * <p>本地 / 联调环境的渠道实现，使「发起支付 → 回调推进 → 订单流转」全链路可完整跑通，
 * 而无需真实商户资质（AppId / 商户号 / 私钥 / 平台证书）。</p>
 *
 * <p>行为约定：</p>
 * <ul>
 *   <li>{@code invokePay}：成功返回<b>可预期</b>的渠道支付单号与调起参数，
 *       便于本地手动构造回调报文</li>
 *   <li>{@code invokeRefund}：受理成功但返回 {@code PROCESSING}，
 *       终态由回调推进（与真实渠道的异步语义一致）</li>
 *   <li>{@code queryBill}：模拟渠道无真实账单，返回查询成功但状态未知</li>
 * </ul>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Slf4j
@Component
public class MockPayAdapter implements PaymentChannelAdapter {

    /** 模拟渠道支付单号前缀 */
    public static final String CHANNEL_PAY_NO_PREFIX = "MOCKPAY";

    /** 模拟渠道退款单号前缀 */
    public static final String CHANNEL_REFUND_NO_PREFIX = "MOCKREF";

    /** 模拟渠道在回调中使用的固定签名（便于本地联调构造报文） */
    public static final String MOCK_SIGN = "MOCK_SIGN";

    /** JSON 报文解析器（模拟渠道回调报文为 JSON，线程安全可复用） */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 向渠道发起支付（模拟实现）
     *
     * <p>渠道支付单号由「固定前缀 + 支付单号」确定性推导，便于本地手动构造回调报文。</p>
     *
     * @param payment 支付单
     * @param channel 渠道配置
     * @param openid  用户在该渠道的标识（模拟实现不使用）
     * @return 发起结果，含可预期的渠道支付单号与前端调起参数
     */
    @Override
    public PayResult invokePay(MallPaymentDO payment, MallPaymentChannelDO channel, String openid) {
        String channelPaymentNo = CHANNEL_PAY_NO_PREFIX + payment.getPaymentNo();
        Map<String, String> payParams = new LinkedHashMap<>(8);
        payParams.put("appId", "mockAppId");
        payParams.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        payParams.put("nonceStr", payment.getPaymentNo());
        payParams.put("package", "prepay_id=" + channelPaymentNo);
        payParams.put("signType", "RSA");
        payParams.put("paySign", MOCK_SIGN);

        PayResult result = new PayResult();
        result.setSuccess(true);
        result.setChannelPaymentNo(channelPaymentNo);
        result.setPayParams(payParams);
        result.setChannelPayStatus("NOTPAY");
        log.info("[MockPay] 发起支付成功，paymentNo={}, channelPaymentNo={}",
                payment.getPaymentNo(), channelPaymentNo);
        return result;
    }

    /**
     * 向渠道发起退款（模拟实现）
     *
     * <p>渠道受理成功，但退款状态返回非终态 {@code PROCESSING}，终态由后续异步回调推进。</p>
     *
     * @param payment 原支付单
     * @param refund  退款单
     * @param channel 渠道配置
     * @return 退款结果，含可预期的渠道退款单号与非终态退款状态
     */
    @Override
    public RefundResult invokeRefund(MallPaymentDO payment, MallRefundDO refund, MallPaymentChannelDO channel) {
        String channelRefundNo = CHANNEL_REFUND_NO_PREFIX + refund.getRefundNo();

        RefundResult result = new RefundResult();
        result.setSuccess(true);
        result.setChannelRefundNo(channelRefundNo);
        result.setRefundStatus(RefundStatusEnum.PROCESSING.getCode());
        result.setChannelRefundStatus(RefundStatusEnum.PROCESSING.name());
        log.info("[MockPay] 发起退款已受理，refundNo={}, channelRefundNo={}",
                refund.getRefundNo(), channelRefundNo);
        return result;
    }

    /**
     * 查询渠道侧交易状态（模拟实现）
     *
     * <p>模拟渠道无真实账单，固定返回查询成功但交易状态未知。</p>
     *
     * @param channelPaymentNo 渠道侧支付单号
     * @param channel          渠道配置
     * @return 账单查询结果，交易状态固定为 {@code UNKNOWN}
     */
    @Override
    public ChannelBillResult queryBill(String channelPaymentNo, MallPaymentChannelDO channel) {
        ChannelBillResult result = new ChannelBillResult();
        result.setSuccess(true);
        result.setChannelTradeStatus("UNKNOWN");
        result.setChannelPaymentNo(channelPaymentNo);
        log.info("[MockPay] 查询账单，channelPaymentNo={}，模拟渠道返回状态未知", channelPaymentNo);
        return result;
    }

    /**
     * 验签并解析支付回调（模拟实现）
     *
     * <p>模拟渠道回调报文为 JSON，签名字段 {@code sign} 必须等于 {@link #MOCK_SIGN}。
     * 报文无法解析或签名不符一律返回 {@code verified=false}，<b>不抛异常</b>，
     * 由上层据此返回 HTTP 400。</p>
     *
     * @param rawBody 回调原始报文（JSON）
     * @param headers 回调请求头（模拟实现不使用）
     * @return 解析结果，验签失败时 {@code verified=false} 且携带失败原因
     */
    @Override
    public PayCallbackResult parsePayCallback(String rawBody, Map<String, String> headers) {
        PayCallbackResult result = new PayCallbackResult();
        JsonNode node;
        try {
            node = OBJECT_MAPPER.readTree(rawBody);
        } catch (Exception e) {
            result.setVerified(false);
            result.setFailReason("回调报文无法解析");
            log.warn("[MockPay] 支付回调报文解析失败，rawBody={}", rawBody, e);
            return result;
        }

        if (!isSignValid(node)) {
            result.setVerified(false);
            result.setFailReason("验签失败：签名不符");
            log.warn("[MockPay] 支付回调验签失败，sign={}", text(node, "sign"));
            return result;
        }

        result.setVerified(true);
        result.setChannelPaymentNo(text(node, "channelPaymentNo"));
        result.setPayAmount(number(node, "payAmount"));
        result.setChannelPayStatus(text(node, "channelPayStatus"));
        result.setNonce(text(node, "nonce"));
        log.info("[MockPay] 支付回调验签通过，channelPaymentNo={}", result.getChannelPaymentNo());
        return result;
    }

    @Override
    public RefundCallbackResult parseRefundCallback(String rawBody, Map<String, String> headers) {
        RefundCallbackResult result = new RefundCallbackResult();
        JsonNode node;
        try {
            node = OBJECT_MAPPER.readTree(rawBody);
        } catch (Exception e) {
            result.setVerified(false);
            result.setFailReason("回调报文无法解析");
            log.warn("[MockPay] 退款回调报文解析失败，rawBody={}", rawBody, e);
            return result;
        }

        if (!isSignValid(node)) {
            result.setVerified(false);
            result.setFailReason("验签失败：签名不符");
            log.warn("[MockPay] 退款回调验签失败，sign={}", text(node, "sign"));
            return result;
        }

        String channelRefundStatus = text(node, "channelRefundStatus");
        result.setVerified(true);
        result.setChannelRefundNo(text(node, "channelRefundNo"));
        result.setRefundAmount(number(node, "refundAmount"));
        result.setChannelRefundStatus(channelRefundStatus);
        result.setRefundStatus(mapRefundStatus(channelRefundStatus));
        result.setNonce(text(node, "nonce"));
        log.info("[MockPay] 退款回调验签通过，channelRefundNo={}, refundStatus={}",
                result.getChannelRefundNo(), result.getRefundStatus());
        return result;
    }

    /**
     * 校验模拟回调签名
     *
     * <p>报文 {@code sign} 字段必须严格等于 {@link #MOCK_SIGN}；缺省或不等均视为验签失败。</p>
     *
     * @param node 已解析的 JSON 根节点
     * @return 验签通过返回 true
     */
    private boolean isSignValid(JsonNode node) {
        return MOCK_SIGN.equals(text(node, "sign"));
    }

    /**
     * 把渠道退款状态原文映射为 {@link RefundStatusEnum} 的码值
     *
     * <p>按枚举名忽略大小写精确匹配；<b>未识别的状态一律映射为「处理中」</b>，
     * 避免把未知状态误判为终态。</p>
     *
     * @param channelRefundStatus 渠道退款状态原文
     * @return 退款状态码
     */
    private Integer mapRefundStatus(String channelRefundStatus) {
        if (channelRefundStatus == null) {
            return RefundStatusEnum.PROCESSING.getCode();
        }
        try {
            return RefundStatusEnum.valueOf(channelRefundStatus.trim().toUpperCase()).getCode();
        } catch (IllegalArgumentException e) {
            log.warn("[MockPay] 未识别的渠道退款状态，按处理中处理，channelRefundStatus={}", channelRefundStatus);
            return RefundStatusEnum.PROCESSING.getCode();
        }
    }

    /**
     * 读取 JSON 文本字段
     *
     * @param node  JSON 根节点
     * @param field 字段名
     * @return 字段值，缺省或为 null 时返回 null
     */
    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    /**
     * 读取 JSON 数值字段（金额，单位：分）
     *
     * @param node  JSON 根节点
     * @param field 字段名
     * @return 字段值（Long），缺省或为 null 时返回 null
     */
    private Long number(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asLong();
    }
}
