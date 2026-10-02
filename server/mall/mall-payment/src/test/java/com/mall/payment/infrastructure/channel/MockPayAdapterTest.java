package com.mall.payment.infrastructure.channel;

import com.mall.common.enums.payment.RefundStatusEnum;
import com.mall.payment.DO.MallPaymentChannelDO;
import com.mall.payment.DO.MallPaymentDO;
import com.mall.payment.DO.MallRefundDO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MockPayAdapter 单元测试
 *
 * <p>模拟渠道的核心价值是「可预期的行为」—— 本地联调时能凭固定规则构造出回调报文，
 * 故此处重点断言渠道单号等标识的<b>可预测性</b>。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@DisplayName("MockPayAdapter 模拟渠道适配器")
class MockPayAdapterTest {

    private static final String PAYMENT_NO = "PAY20261003000001";

    private static final String REFUND_NO = "REF20261003000001";

    private final MockPayAdapter adapter = new MockPayAdapter();

    @Test
    @DisplayName("invokePay 成功，且渠道支付单号可预期（固定前缀 + 支付单号）")
    void invokePayProducesPredictableChannelNo() {
        PayResult result = adapter.invokePay(payment(), channel(), "openid-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getChannelPaymentNo())
                .isEqualTo(MockPayAdapter.CHANNEL_PAY_NO_PREFIX + PAYMENT_NO);
        assertThat(result.getFailReason()).isNull();
    }

    @Test
    @DisplayName("invokePay 返回前端调起支付所需的全部参数")
    void invokePayReturnsSdkParams() {
        PayResult result = adapter.invokePay(payment(), channel(), "openid-1");

        assertThat(result.getPayParams())
                .containsKeys("appId", "timeStamp", "nonceStr", "package", "signType", "paySign");
        // package 需携带 prepay_id，否则前端无法调起
        assertThat(result.getPayParams().get("package"))
                .contains(MockPayAdapter.CHANNEL_PAY_NO_PREFIX + PAYMENT_NO);
    }

    @Test
    @DisplayName("invokePay 的渠道侧状态为未支付 NOTPAY")
    void invokePayChannelStatusNotPay() {
        PayResult result = adapter.invokePay(payment(), channel(), null);

        assertThat(result.getChannelPayStatus()).isEqualTo("NOTPAY");
    }

    @Test
    @DisplayName("invokeRefund 受理成功但返回 PROCESSING（终态由渠道回调推进）")
    void invokeRefundReturnsProcessing() {
        RefundResult result = adapter.invokeRefund(payment(), refund(), channel());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getRefundStatus()).isEqualTo(RefundStatusEnum.PROCESSING.getCode());
        assertThat(result.getChannelRefundNo())
                .isEqualTo(MockPayAdapter.CHANNEL_REFUND_NO_PREFIX + REFUND_NO);
    }

    @Test
    @DisplayName("queryBill 返回查询成功但交易状态未知（模拟渠道无真实账单）")
    void queryBillReturnsSuccess() {
        ChannelBillResult result = adapter.queryBill("MOCKPAY123", channel());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getChannelTradeStatus()).isEqualTo("UNKNOWN");
        assertThat(result.getChannelPaymentNo()).isEqualTo("MOCKPAY123");
    }

    // ======================== 回调解析 ========================

    @Test
    @DisplayName("parsePayCallback 验签通过时解析出渠道单号、金额与状态")
    void parsePayCallbackVerified() {
        String body = """
                {"channelPaymentNo":"MOCKPAY123","payAmount":89900,
                 "channelPayStatus":"SUCCESS","nonce":"NONCE-1","sign":"MOCK_SIGN"}""";

        PayCallbackResult result = adapter.parsePayCallback(body, Map.of());

        assertThat(result.isVerified()).isTrue();
        assertThat(result.getChannelPaymentNo()).isEqualTo("MOCKPAY123");
        assertThat(result.getPayAmount()).isEqualTo(89900L);
        assertThat(result.getChannelPayStatus()).isEqualTo("SUCCESS");
        assertThat(result.getNonce()).isEqualTo("NONCE-1");
    }

    @Test
    @DisplayName("parsePayCallback 签名不符时不通过验签")
    void parsePayCallbackBadSign() {
        String body = """
                {"channelPaymentNo":"MOCKPAY123","payAmount":89900,
                 "channelPayStatus":"SUCCESS","nonce":"NONCE-1","sign":"WRONG"}""";

        PayCallbackResult result = adapter.parsePayCallback(body, Map.of());

        assertThat(result.isVerified()).isFalse();
        assertThat(result.getFailReason()).isNotBlank();
    }

    @Test
    @DisplayName("parsePayCallback 报文非法时不通过验签（不抛异常）")
    void parsePayCallbackMalformed() {
        PayCallbackResult result = adapter.parsePayCallback("not-a-json", Map.of());

        assertThat(result.isVerified()).isFalse();
    }

    @Test
    @DisplayName("parseRefundCallback 解析并把渠道状态映射为退款状态码")
    void parseRefundCallbackVerified() {
        String body = """
                {"channelRefundNo":"MOCKREF123","refundAmount":10000,
                 "channelRefundStatus":"SUCCESS","nonce":"NONCE-2","sign":"MOCK_SIGN"}""";

        RefundCallbackResult result = adapter.parseRefundCallback(body, Map.of());

        assertThat(result.isVerified()).isTrue();
        assertThat(result.getChannelRefundNo()).isEqualTo("MOCKREF123");
        assertThat(result.getRefundAmount()).isEqualTo(10000L);
        assertThat(result.getRefundStatus()).isEqualTo(RefundStatusEnum.SUCCESS.getCode());
        assertThat(result.getNonce()).isEqualTo("NONCE-2");
    }

    @Test
    @DisplayName("parseRefundCallback 未识别的渠道状态映射为处理中")
    void parseRefundCallbackUnknownStatus() {
        String body = """
                {"channelRefundNo":"MOCKREF123","refundAmount":10000,
                 "channelRefundStatus":"SOMETHING_ELSE","nonce":"NONCE-2","sign":"MOCK_SIGN"}""";

        RefundCallbackResult result = adapter.parseRefundCallback(body, Map.of());

        assertThat(result.getRefundStatus()).isEqualTo(RefundStatusEnum.PROCESSING.getCode());
    }

    // ======================== 夹具 ========================

    private static MallPaymentDO payment() {
        MallPaymentDO payment = new MallPaymentDO();
        payment.setPaymentNo(PAYMENT_NO);
        payment.setOrderNo("ORD20261003000001");
        payment.setPayAmount(89900L);
        payment.setChannelCode("wechat");
        return payment;
    }

    private static MallRefundDO refund() {
        MallRefundDO refund = new MallRefundDO();
        refund.setRefundNo(REFUND_NO);
        refund.setOrderNo("ORD20261003000001");
        refund.setRefundAmount(89900L);
        refund.setChannelCode("wechat");
        return refund;
    }

    private static MallPaymentChannelDO channel() {
        MallPaymentChannelDO channel = new MallPaymentChannelDO();
        channel.setChannelCode("wechat");
        channel.setChannelName("微信支付");
        channel.setConfigJson("{}");
        return channel;
    }
}
