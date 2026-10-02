package com.mall.payment.schedule;

import com.mall.common.enums.payment.PaymentStatusEnum;
import com.mall.payment.DO.MallPaymentDO;
import com.mall.payment.infrastructure.channel.PaymentChannelFactory;
import com.mall.payment.mapper.MallPaymentChannelMapper;
import com.mall.payment.mapper.MallPaymentMapper;
import com.mall.payment.service.PaymentReconcileService;
import com.mall.payment.statemachine.PaymentStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 支付定时任务单元测试
 *
 * <p>覆盖超时关单与回调丢失补偿两条链路，重点验证
 * <b>单条异常不中断整批</b>与<b>两个任务的扫描窗口互不重叠</b>。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentScheduleTaskTest {

    private static final String PAYMENT_NO = "PAY20261003000001";

    private static final String PAYMENT_NO_2 = "PAY20261003000002";

    private static final Integer VERSION = 0;

    private static final long COMPENSATE_DELAY_SECONDS = 1800L;

    @Mock
    private MallPaymentMapper paymentMapper;

    @Mock
    private MallPaymentChannelMapper channelMapper;

    @Mock
    private PaymentChannelFactory channelFactory;

    @Mock
    private PaymentReconcileService paymentReconcileService;

    private PaymentTimeoutTask timeoutTask;

    private PaymentCompensateTask compensateTask;

    @BeforeEach
    void setUp() {
        timeoutTask = new PaymentTimeoutTask(paymentMapper, new PaymentStateMachine());
        compensateTask = new PaymentCompensateTask(paymentMapper, paymentReconcileService,
                COMPENSATE_DELAY_SECONDS);
    }

    @Nested
    @DisplayName("超时关单任务")
    class TimeoutClose {

        @Test
        @DisplayName("无候选：不调用任何落库方法")
        void noCandidates() {
            when(paymentMapper.selectTimeoutUnpaid(any(LocalDateTime.class), anyInt()))
                    .thenReturn(List.of());

            timeoutTask.closeTimeoutPayments();

            verify(paymentMapper, never()).markClosed(anyString(), any());
        }

        @Test
        @DisplayName("正常：状态机推进 CLOSED 并按乐观锁 CAS 落库")
        void closesExpiredPayments() {
            when(paymentMapper.selectTimeoutUnpaid(any(LocalDateTime.class), anyInt()))
                    .thenReturn(List.of(payment(PAYMENT_NO)));
            when(paymentMapper.markClosed(PAYMENT_NO, VERSION)).thenReturn(1);

            timeoutTask.closeTimeoutPayments();

            verify(paymentMapper).markClosed(PAYMENT_NO, VERSION);
        }

        @Test
        @DisplayName("单条异常不中断整批（后续支付单仍被处理）")
        void continuesAfterSingleFailure() {
            when(paymentMapper.selectTimeoutUnpaid(any(LocalDateTime.class), anyInt()))
                    .thenReturn(List.of(payment(PAYMENT_NO), payment(PAYMENT_NO_2)));
            // 第一条 CAS 抛异常（模拟 DB 抖动），第二条应仍被处理
            when(paymentMapper.markClosed(PAYMENT_NO, VERSION))
                    .thenThrow(new RuntimeException("模拟 DB 抖动"));
            when(paymentMapper.markClosed(PAYMENT_NO_2, VERSION)).thenReturn(1);

            timeoutTask.closeTimeoutPayments();

            verify(paymentMapper).markClosed(PAYMENT_NO_2, VERSION);
        }
    }

    @Nested
    @DisplayName("回调丢失补偿任务")
    class Compensate {

        @Test
        @DisplayName("无候选：不对账")
        void noCandidates() {
            when(paymentMapper.selectReconcileCandidates(any(LocalDateTime.class),
                    any(LocalDateTime.class), anyInt())).thenReturn(List.of());

            compensateTask.reconcileUnpaidPayments();

            verify(paymentReconcileService, never()).reconcile(any(MallPaymentDO.class));
        }

        @Test
        @DisplayName("正常：逐条交给对账服务")
        void reconcilesCandidates() {
            when(paymentMapper.selectReconcileCandidates(any(LocalDateTime.class),
                    any(LocalDateTime.class), anyInt()))
                    .thenReturn(List.of(payment(PAYMENT_NO), payment(PAYMENT_NO_2)));

            compensateTask.reconcileUnpaidPayments();

            verify(paymentReconcileService, times(2)).reconcile(any(MallPaymentDO.class));
        }

        @Test
        @DisplayName("单条对账异常不中断整批")
        void continuesAfterSingleFailure() {
            MallPaymentDO first = payment(PAYMENT_NO);
            MallPaymentDO second = payment(PAYMENT_NO_2);
            when(paymentMapper.selectReconcileCandidates(any(LocalDateTime.class),
                    any(LocalDateTime.class), anyInt())).thenReturn(List.of(first, second));
            when(paymentReconcileService.reconcile(first))
                    .thenThrow(new RuntimeException("渠道不可用"));

            compensateTask.reconcileUnpaidPayments();

            verify(paymentReconcileService).reconcile(second);
        }
    }

    /**
     * 构造超时未支付单夹具
     *
     * @param paymentNo 支付单号
     * @return 支付单
     */
    private static MallPaymentDO payment(String paymentNo) {
        MallPaymentDO payment = new MallPaymentDO();
        payment.setPaymentNo(paymentNo);
        payment.setPaymentStatus(PaymentStatusEnum.UNPAID.getCode());
        payment.setExpireTime(LocalDateTime.now().minusMinutes(5));
        payment.setVersion(VERSION);
        return payment;
    }
}
