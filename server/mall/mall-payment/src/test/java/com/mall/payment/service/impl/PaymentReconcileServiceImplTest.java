package com.mall.payment.service.impl;

import com.mall.common.constant.MqTopicConstants;
import com.mall.common.enums.payment.PaymentStatusEnum;
import com.mall.payment.DO.MallPaymentChannelDO;
import com.mall.payment.DO.MallPaymentDO;
import com.mall.payment.dto.event.PaymentPaidEvent;
import com.mall.payment.infrastructure.channel.ChannelBillResult;
import com.mall.payment.infrastructure.channel.PaymentChannelAdapter;
import com.mall.payment.infrastructure.channel.PaymentChannelFactory;
import com.mall.payment.infrastructure.outbox.OutboxPublisher;
import com.mall.payment.mapper.MallPaymentChannelMapper;
import com.mall.payment.mapper.MallPaymentMapper;
import com.mall.payment.statemachine.PaymentStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PaymentReconcileServiceImpl 单元测试
 *
 * <p>覆盖设计文档 §5.5「回调丢失补偿」：主动向渠道对账，
 * 渠道确认已收款则补记 PAID 并补发 {@code mall:payment:paid}。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class PaymentReconcileServiceImplTest {

    private static final String PAYMENT_NO = "PAY20261003000001";

    private static final String ORDER_NO = "ORD20261003000001";

    private static final String CHANNEL_CODE = "wechat";

    private static final String CHANNEL_PAYMENT_NO = "MOCKPAY" + PAYMENT_NO;

    private static final Long USER_ID = 100L;

    private static final Long PAY_AMOUNT = 89900L;

    private static final Integer VERSION = 0;

    @Mock
    private MallPaymentMapper paymentMapper;

    @Mock
    private MallPaymentChannelMapper channelMapper;

    @Mock
    private PaymentChannelFactory channelFactory;

    @Mock
    private PaymentChannelAdapter channelAdapter;

    @Mock
    private OutboxPublisher outboxPublisher;

    private PaymentReconcileServiceImpl reconcileService;

    @BeforeEach
    void setUp() {
        // 状态机是无状态纯逻辑组件，直接用真实实现，验证状态确实被推进
        reconcileService = new PaymentReconcileServiceImpl(paymentMapper, channelMapper,
                channelFactory, new PaymentStateMachine(), outboxPublisher);
    }

    @Nested
    @DisplayName("对账不成立的分支")
    class NoReconcile {

        @Test
        @DisplayName("尚未发起渠道（无渠道单号）→ 不查询、不落库")
        void skipsWhenNoChannelPaymentNo() {
            MallPaymentDO payment = unpaidPayment();
            payment.setChannelPaymentNo(null);

            assertThat(reconcileService.reconcile(payment)).isFalse();

            verify(channelFactory, never()).getAdapter(anyString());
            verify(paymentMapper, never()).markPaid(anyString(), any(), any());
        }

        @Test
        @DisplayName("渠道查询失败（success=false）→ 保守不落库")
        void skipsWhenBillQueryFailed() {
            stubChannel();
            when(channelAdapter.queryBill(eq(CHANNEL_PAYMENT_NO), any(MallPaymentChannelDO.class))).thenReturn(bill(false, "UNKNOWN"));

            assertThat(reconcileService.reconcile(unpaidPayment())).isFalse();

            verify(paymentMapper, never()).markPaid(anyString(), any(), any());
            verify(outboxPublisher, never()).publish(anyString(), anyString(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("渠道返回未支付状态 → 不落库（不能把未支付当已支付）")
        void skipsWhenChannelNotPaid() {
            stubChannel();
            when(channelAdapter.queryBill(eq(CHANNEL_PAYMENT_NO), any(MallPaymentChannelDO.class))).thenReturn(bill(true, "UNKNOWN"));

            assertThat(reconcileService.reconcile(unpaidPayment())).isFalse();

            verify(paymentMapper, never()).markPaid(anyString(), any(), any());
        }

        @Test
        @DisplayName("渠道确认已支付但本地 CAS 未命中（已被并发处理）→ 返回 false 且不补发事件")
        void skipsWhenCasMissed() {
            stubChannel();
            when(channelAdapter.queryBill(eq(CHANNEL_PAYMENT_NO), any(MallPaymentChannelDO.class))).thenReturn(bill(true, "SUCCESS"));
            when(paymentMapper.markPaid(eq(PAYMENT_NO), eq("SUCCESS"), eq(VERSION))).thenReturn(0);

            assertThat(reconcileService.reconcile(unpaidPayment())).isFalse();

            // CAS 0 行说明状态已被他人推进，此时再发事件会造成重复消费
            verify(outboxPublisher, never()).publish(anyString(), anyString(), anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("对账成立：补记 PAID")
    class Reconciled {

        @Test
        @DisplayName("渠道确认已收款 → CAS 推进 PAID + 补发 mall:payment:paid")
        void marksPaidAndPublishesEvent() {
            stubChannel();
            when(channelAdapter.queryBill(eq(CHANNEL_PAYMENT_NO), any(MallPaymentChannelDO.class))).thenReturn(bill(true, "SUCCESS"));
            when(paymentMapper.markPaid(PAYMENT_NO, "SUCCESS", VERSION)).thenReturn(1);

            assertThat(reconcileService.reconcile(unpaidPayment())).isTrue();

            verify(paymentMapper).markPaid(PAYMENT_NO, "SUCCESS", VERSION);

            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(outboxPublisher).publish(eq(MqTopicConstants.Payment.PAID), eq("PaymentPaid"),
                    eq(OutboxPublisher.AGGREGATE_PAYMENT), eq(PAYMENT_NO), payloadCaptor.capture());

            PaymentPaidEvent event = (PaymentPaidEvent) payloadCaptor.getValue();
            assertThat(event.getPaymentNo()).isEqualTo(PAYMENT_NO);
            assertThat(event.getOrderNo()).isEqualTo(ORDER_NO);
            assertThat(event.getUserId()).isEqualTo(USER_ID);
            assertThat(event.getPayAmount()).isEqualTo(PAY_AMOUNT);
            assertThat(event.getChannelPaymentNo()).isEqualTo(CHANNEL_PAYMENT_NO);
            assertThat(event.getChannelCode()).isEqualTo(CHANNEL_CODE);
            assertThat(event.getPayTime()).isNotNull();
        }
    }

    // ======================== 夹具 ========================

    /**
     * 打桩渠道配置与适配器
     */
    private void stubChannel() {
        when(channelMapper.selectByChannelCode(CHANNEL_CODE)).thenReturn(new MallPaymentChannelDO());
        when(channelFactory.getAdapter(CHANNEL_CODE)).thenReturn(channelAdapter);
    }

    /**
     * 构造「未支付」支付单夹具
     *
     * @return 支付单
     */
    private static MallPaymentDO unpaidPayment() {
        MallPaymentDO payment = new MallPaymentDO();
        payment.setPaymentNo(PAYMENT_NO);
        payment.setOrderNo(ORDER_NO);
        payment.setUserId(USER_ID);
        payment.setPayAmount(PAY_AMOUNT);
        payment.setChannelCode(CHANNEL_CODE);
        payment.setChannelPaymentNo(CHANNEL_PAYMENT_NO);
        payment.setPaymentStatus(PaymentStatusEnum.UNPAID.getCode());
        payment.setExpireTime(LocalDateTime.now().plusMinutes(30));
        payment.setVersion(VERSION);
        return payment;
    }

    /**
     * 构造渠道账单查询结果
     *
     * @param success 查询是否成功
     * @param status  渠道侧交易状态
     * @return 账单结果
     */
    private static ChannelBillResult bill(boolean success, String status) {
        ChannelBillResult result = new ChannelBillResult();
        result.setSuccess(success);
        result.setChannelTradeStatus(status);
        result.setChannelPaymentNo(CHANNEL_PAYMENT_NO);
        result.setTradeAmount(PAY_AMOUNT);
        return result;
    }
}
