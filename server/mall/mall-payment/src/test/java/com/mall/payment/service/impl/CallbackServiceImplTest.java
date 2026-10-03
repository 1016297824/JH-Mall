package com.mall.payment.service.impl;

import com.mall.common.constant.CacheConstants;
import com.mall.common.constant.MqTopicConstants;
import com.mall.common.enums.payment.PaymentStatusEnum;
import com.mall.common.enums.payment.RefundStatusEnum;
import com.mall.payment.DO.MallPaymentCallbackLogDO;
import com.mall.payment.DO.MallPaymentDO;
import com.mall.payment.DO.MallRefundDO;
import com.mall.payment.config.MallPaymentConfigProperties;
import com.mall.payment.dto.event.PaymentPaidEvent;
import com.mall.payment.dto.event.RefundFailedEvent;
import com.mall.payment.dto.event.RefundSucceededEvent;
import com.mall.payment.dto.response.CallbackResult;
import com.mall.payment.infrastructure.channel.PayCallbackResult;
import com.mall.payment.infrastructure.channel.PaymentChannelAdapter;
import com.mall.payment.infrastructure.channel.PaymentChannelFactory;
import com.mall.payment.infrastructure.channel.RefundCallbackResult;
import com.mall.payment.infrastructure.outbox.OutboxPublisher;
import com.mall.payment.mapper.MallPaymentCallbackLogMapper;
import com.mall.payment.mapper.MallPaymentMapper;
import com.mall.payment.mapper.MallRefundMapper;
import com.mall.payment.statemachine.PaymentStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CallbackServiceImpl 单元测试
 *
 * <p>覆盖设计文档 §5 回调流程与 §10 回调安全：验签分支、nonce 防重放、
 * <b>先落库再应答</b>、金额比对、CAS 幂等。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CallbackServiceImpl 回调服务")
class CallbackServiceImplTest {

    private static final String CHANNEL_CODE = "wechat";

    private static final String PAYMENT_NO = "PAY20261003000001";

    private static final String ORDER_NO = "ORD20261003000001";

    private static final Long USER_ID = 100L;

    private static final String REFUND_NO = "REF20261003000001";

    private static final String CHANNEL_PAYMENT_NO = "MOCKPAY" + PAYMENT_NO;

    private static final String CHANNEL_REFUND_NO = "MOCKREF" + REFUND_NO;

    private static final String NONCE = "NONCE-1";

    private static final Integer VERSION = 0;

    private static final Long PAYMENT_ID = 1L;

    private static final Long PAY_AMOUNT = 89900L;

    /** 退款金额（分）：与支付金额不同，避免断言误用 PAY_AMOUNT 时无法察觉 */
    private static final Long REFUND_AMOUNT = 10000L;

    private static final String PAY_BODY = "{\"sign\":\"MOCK_SIGN\"}";

    private static final String REFUND_BODY = "{\"sign\":\"MOCK_SIGN\"}";

    @Mock
    private MallPaymentCallbackLogMapper callbackLogMapper;

    @Mock
    private MallPaymentMapper paymentMapper;

    @Mock
    private MallRefundMapper refundMapper;

    @Mock
    private PaymentChannelFactory channelFactory;

    @Mock
    private PaymentChannelAdapter channelAdapter;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private MallPaymentConfigProperties configProperties;

    @Mock
    private OutboxPublisher outboxPublisher;

    private CallbackServiceImpl callbackService;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(configProperties.getCallback()).thenReturn(new MallPaymentConfigProperties.Callback());
        when(channelFactory.getAdapter(CHANNEL_CODE)).thenReturn(channelAdapter);
        when(callbackLogMapper.insert(any(MallPaymentCallbackLogDO.class))).thenReturn(1);

        callbackService = new CallbackServiceImpl(callbackLogMapper, paymentMapper, refundMapper,
                channelFactory, new PaymentStateMachine(), stringRedisTemplate, configProperties,
                outboxPublisher);
    }

    @Nested
    @DisplayName("支付回调")
    class PayCallback {

        @Test
        @DisplayName("正常流程：支付单 CAS 推进 PAID，返回成功应答")
        void succeeds() {
            stubPayVerified();

            CallbackResult result = callbackService.processPayCallback(CHANNEL_CODE, PAY_BODY, Map.of());

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getResponseBody()).isNotBlank();
            verify(paymentMapper).markPaid(eq(PAYMENT_NO), eq("SUCCESS"), eq(VERSION));
        }

        @Test
        @DisplayName("回调日志先落库，再推进支付单状态（先落库再应答）")
        void logsBeforeAdvancingState() {
            stubPayVerified();

            callbackService.processPayCallback(CHANNEL_CODE, PAY_BODY, Map.of());

            InOrder inOrder = inOrder(callbackLogMapper, paymentMapper);
            inOrder.verify(callbackLogMapper).insert(any(MallPaymentCallbackLogDO.class));
            inOrder.verify(paymentMapper).markPaid(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("验签失败：不推进状态，也不占用 nonce")
        void verifyFailed() {
            PayCallbackResult unverified = new PayCallbackResult();
            unverified.setVerified(false);
            unverified.setFailReason("签名不符");
            when(channelAdapter.parsePayCallback(anyString(), any())).thenReturn(unverified);

            CallbackResult result = callbackService.processPayCallback(CHANNEL_CODE, PAY_BODY, Map.of());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getFailReason()).isNotBlank();
            verify(paymentMapper, never()).markPaid(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("nonce 重复：直接返回成功，不重复推进状态")
        void nonceDeduplicated() {
            stubPayVerified();
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(false);

            CallbackResult result = callbackService.processPayCallback(CHANNEL_CODE, PAY_BODY, Map.of());

            assertThat(result.isSuccess()).isTrue();
            verify(paymentMapper, never()).markPaid(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("nonce 键格式为 mall:payment:callback:{channel}:{nonce}")
        void nonceKeyFormat() {
            stubPayVerified();

            callbackService.processPayCallback(CHANNEL_CODE, PAY_BODY, Map.of());

            verify(valueOperations).setIfAbsent(
                    eq(CacheConstants.Payment.CALLBACK + CHANNEL_CODE + ":" + NONCE),
                    anyString(), anyLong(), any(TimeUnit.class));
        }

        @Test
        @DisplayName("支付单不存在（渠道单号查不到）：处理失败")
        void paymentNotFound() {
            stubPayVerified();
            when(paymentMapper.selectByChannelPaymentNo(CHANNEL_PAYMENT_NO)).thenReturn(null);

            CallbackResult result = callbackService.processPayCallback(CHANNEL_CODE, PAY_BODY, Map.of());

            assertThat(result.isSuccess()).isFalse();
            verify(paymentMapper, never()).markPaid(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("金额与本地支付单不符：拒绝并告警（防回调篡改）")
        void amountMismatch() {
            stubPayVerified();
            MallPaymentDO payment = payment(PaymentStatusEnum.UNPAID);
            payment.setPayAmount(1L);
            when(paymentMapper.selectByChannelPaymentNo(CHANNEL_PAYMENT_NO)).thenReturn(payment);

            CallbackResult result = callbackService.processPayCallback(CHANNEL_CODE, PAY_BODY, Map.of());

            assertThat(result.isSuccess()).isFalse();
            verify(paymentMapper, never()).markPaid(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("CAS 未命中（已被并发处理）：幂等返回成功")
        void alreadyProcessed() {
            stubPayVerified();
            when(paymentMapper.markPaid(anyString(), anyString(), any())).thenReturn(0);

            CallbackResult result = callbackService.processPayCallback(CHANNEL_CODE, PAY_BODY, Map.of());

            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("支付回调成功：同一事务内补发 mall:payment:paid，payload 为精简 DTO")
        void publishesPaymentPaidEvent() {
            stubPayVerified();
            when(paymentMapper.markPaid(anyString(), anyString(), any())).thenReturn(1);
            MallPaymentDO payment = payment(PaymentStatusEnum.UNPAID);
            when(paymentMapper.selectByChannelPaymentNo(CHANNEL_PAYMENT_NO)).thenReturn(payment);

            CallbackResult result = callbackService.processPayCallback(CHANNEL_CODE, PAY_BODY, Map.of());

            assertThat(result.isSuccess()).isTrue();

            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(outboxPublisher).publish(eq(MqTopicConstants.Payment.PAID), eq("PaymentPaid"),
                    eq(OutboxPublisher.AGGREGATE_PAYMENT), eq(PAYMENT_NO), payloadCaptor.capture());

            PaymentPaidEvent event = (PaymentPaidEvent) payloadCaptor.getValue();
            assertThat(event.getPaymentNo()).isEqualTo(PAYMENT_NO);
            assertThat(event.getOrderNo()).isEqualTo(ORDER_NO);
            assertThat(event.getUserId()).isEqualTo(USER_ID);
            assertThat(event.getPayAmount()).isEqualTo(PAY_AMOUNT);
            assertThat(event.getChannelCode()).isEqualTo(CHANNEL_CODE);
            // 事件契约要求 ISO-8601 字符串（设计文档 §8.3）：
            // Outbox 的裸 ObjectMapper 序列化 LocalDateTime 会直接抛异常，整条回调都会失败
            assertThat(LocalDateTime.parse(event.getPayTime())).isNotNull();
        }

        @Test
        @DisplayName("CAS 未命中时不补发事件（避免消费方收到重复的支付成功事实）")
        void doesNotPublishWhenCasMissed() {
            stubPayVerified();
            when(paymentMapper.markPaid(anyString(), anyString(), any())).thenReturn(0);

            callbackService.processPayCallback(CHANNEL_CODE, PAY_BODY, Map.of());

            verify(outboxPublisher, never()).publish(anyString(), anyString(), anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("退款回调")
    class RefundCallback {

        @Test
        @DisplayName("退款成功：退款单推进 SUCCESS，支付单推进 REFUNDED")
        void refundSucceeds() {
            stubRefundVerified();
            when(refundMapper.selectByChannelRefundNo(CHANNEL_REFUND_NO))
                    .thenReturn(refund(RefundStatusEnum.PROCESSING));
            when(paymentMapper.selectById(PAYMENT_ID)).thenReturn(payment(PaymentStatusEnum.REFUNDING));
            when(refundMapper.markSuccess(anyString(), anyString(), anyString(), any())).thenReturn(1);
            when(paymentMapper.markRefunded(anyString(), any())).thenReturn(1);

            CallbackResult result = callbackService.processRefundCallback(CHANNEL_CODE, REFUND_BODY, Map.of());

            assertThat(result.isSuccess()).isTrue();
            verify(refundMapper).markSuccess(eq(REFUND_NO), eq(CHANNEL_REFUND_NO), eq("SUCCESS"), any());
            verify(paymentMapper).markRefunded(eq(PAYMENT_NO), any());
        }

        @Test
        @DisplayName("验签失败：不推进任何状态")
        void refundVerifyFailed() {
            RefundCallbackResult unverified = new RefundCallbackResult();
            unverified.setVerified(false);
            unverified.setFailReason("签名不符");
            when(channelAdapter.parseRefundCallback(anyString(), any())).thenReturn(unverified);

            CallbackResult result = callbackService.processRefundCallback(CHANNEL_CODE, REFUND_BODY, Map.of());

            assertThat(result.isSuccess()).isFalse();
            verify(refundMapper, never()).markSuccess(anyString(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("nonce 重复：直接返回成功")
        void refundNonceDeduplicated() {
            stubRefundVerified();
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(false);

            CallbackResult result = callbackService.processRefundCallback(CHANNEL_CODE, REFUND_BODY, Map.of());

            assertThat(result.isSuccess()).isTrue();
            verify(refundMapper, never()).markSuccess(anyString(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("退款单不存在（渠道退款单号查不到）：处理失败")
        void refundNotFound() {
            stubRefundVerified();
            when(refundMapper.selectByChannelRefundNo(CHANNEL_REFUND_NO)).thenReturn(null);

            CallbackResult result = callbackService.processRefundCallback(CHANNEL_CODE, REFUND_BODY, Map.of());

            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("退款失败回调：退款单推进 FAILED，支付单回退 PAID")
        void refundFailed() {
            stubRefundVerified();
            RefundCallbackResult failed = verifiedRefundCallback();
            failed.setRefundStatus(RefundStatusEnum.FAILED.getCode());
            failed.setChannelRefundStatus("FAILED");
            when(channelAdapter.parseRefundCallback(anyString(), any())).thenReturn(failed);
            when(refundMapper.selectByChannelRefundNo(CHANNEL_REFUND_NO))
                    .thenReturn(refund(RefundStatusEnum.PROCESSING));
            when(paymentMapper.selectById(PAYMENT_ID)).thenReturn(payment(PaymentStatusEnum.REFUNDING));
            when(refundMapper.markFailed(anyString(), anyString(), any())).thenReturn(1);
            when(paymentMapper.revertToPaid(anyString(), any())).thenReturn(1);

            CallbackResult result = callbackService.processRefundCallback(CHANNEL_CODE, REFUND_BODY, Map.of());

            assertThat(result.isSuccess()).isTrue();
            verify(refundMapper).markFailed(eq(REFUND_NO), eq("FAILED"), any());
            verify(paymentMapper).revertToPaid(eq(PAYMENT_NO), any());
        }

        @Test
        @DisplayName("退款成功：补发 mall:refund:succeeded，含售后单号与退款金额")
        void publishesRefundSucceededEvent() {
            refundSucceeds();

            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(outboxPublisher).publish(eq(MqTopicConstants.Payment.REFUND_SUCCEEDED),
                    eq("RefundSucceeded"), eq(OutboxPublisher.AGGREGATE_REFUND),
                    eq(REFUND_NO), payloadCaptor.capture());

            RefundSucceededEvent event = (RefundSucceededEvent) payloadCaptor.getValue();
            assertThat(event.getRefundNo()).isEqualTo(REFUND_NO);
            assertThat(event.getOrderNo()).isEqualTo(ORDER_NO);
            assertThat(event.getUserId()).isEqualTo(USER_ID);
            assertThat(event.getRefundAmount()).isEqualTo(REFUND_AMOUNT);
            assertThat(event.getChannelRefundNo()).isEqualTo(CHANNEL_REFUND_NO);
        }

        @Test
        @DisplayName("退款失败：补发 mall:refund:failed，含失败原因")
        void publishesRefundFailedEvent() {
            refundFailed();

            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(outboxPublisher).publish(eq(MqTopicConstants.Payment.REFUND_FAILED),
                    eq("RefundFailed"), eq(OutboxPublisher.AGGREGATE_REFUND),
                    eq(REFUND_NO), payloadCaptor.capture());

            RefundFailedEvent event = (RefundFailedEvent) payloadCaptor.getValue();
            assertThat(event.getRefundNo()).isEqualTo(REFUND_NO);
            assertThat(event.getOrderNo()).isEqualTo(ORDER_NO);
            assertThat(event.getUserId()).isEqualTo(USER_ID);
            assertThat(event.getRefundAmount()).isEqualTo(REFUND_AMOUNT);
            assertThat(event.getChannelRefundNo()).isEqualTo(CHANNEL_REFUND_NO);
        }
    }

    // ======================== 打桩与夹具 ========================

    /**
     * 打桩「验签通过 + nonce 未重复」的支付回调路径
     */
    private void stubPayVerified() {
        when(channelAdapter.parsePayCallback(anyString(), any())).thenReturn(verifiedPayCallback());
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(paymentMapper.selectByChannelPaymentNo(CHANNEL_PAYMENT_NO))
                .thenReturn(payment(PaymentStatusEnum.UNPAID));
        when(paymentMapper.markPaid(anyString(), anyString(), any())).thenReturn(1);
    }

    /**
     * 打桩「验签通过 + nonce 未重复」的退款回调路径
     */
    private void stubRefundVerified() {
        when(channelAdapter.parseRefundCallback(anyString(), any())).thenReturn(verifiedRefundCallback());
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
    }

    /**
     * 构造验签通过的支付回调解析结果
     *
     * @return 解析结果
     */
    private static PayCallbackResult verifiedPayCallback() {
        PayCallbackResult result = new PayCallbackResult();
        result.setVerified(true);
        result.setChannelPaymentNo(CHANNEL_PAYMENT_NO);
        result.setPayAmount(PAY_AMOUNT);
        result.setChannelPayStatus("SUCCESS");
        result.setNonce(NONCE);
        return result;
    }

    /**
     * 构造验签通过的退款回调解析结果
     *
     * @return 解析结果
     */
    private static RefundCallbackResult verifiedRefundCallback() {
        RefundCallbackResult result = new RefundCallbackResult();
        result.setVerified(true);
        result.setChannelRefundNo(CHANNEL_REFUND_NO);
        result.setRefundAmount(10000L);
        result.setRefundStatus(RefundStatusEnum.SUCCESS.getCode());
        result.setChannelRefundStatus("SUCCESS");
        result.setNonce(NONCE);
        return result;
    }

    /**
     * 构造支付单夹具
     *
     * @param status 支付单状态
     * @return 支付单
     */
    private static MallPaymentDO payment(PaymentStatusEnum status) {
        MallPaymentDO payment = new MallPaymentDO();
        payment.setId(1L);
        payment.setPaymentNo(PAYMENT_NO);
        payment.setOrderNo(ORDER_NO);
        payment.setUserId(USER_ID);
        payment.setPayAmount(PAY_AMOUNT);
        payment.setChannelCode(CHANNEL_CODE);
        payment.setPaymentStatus(status.getCode());
        payment.setVersion(VERSION);
        return payment;
    }

    /**
     * 构造退款单夹具
     *
     * @param status 退款单状态
     * @return 退款单
     */
    private static MallRefundDO refund(RefundStatusEnum status) {
        MallRefundDO refund = new MallRefundDO();
        refund.setId(1L);
        refund.setRefundNo(REFUND_NO);
        refund.setPaymentId(1L);
        refund.setOrderNo(ORDER_NO);
        refund.setRefundAmount(REFUND_AMOUNT);
        refund.setChannelCode(CHANNEL_CODE);
        refund.setChannelRefundNo(CHANNEL_REFUND_NO);
        refund.setRefundStatus(status.getCode());
        refund.setVersion(VERSION);
        return refund;
    }
}
