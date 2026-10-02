package com.mall.payment.statemachine;

import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.payment.PaymentStatusEnum;
import com.mall.common.enums.payment.RefundStatusEnum;
import com.mall.common.exception.BusinessException;
import com.mall.payment.DO.MallPaymentDO;
import com.mall.payment.DO.MallRefundDO;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * PaymentStateMachine 单元测试
 *
 * <p>覆盖设计文档 {@code docs/design/13_mall-payment详细设计.md} §7.2 的全部转移：
 * 支付单 6 条真实转移（设计矩阵 8 行中，第 1 行「发起支付」非状态转移、
 * 第 8 行与第 6 行重复，故为 6 条）+ 退款单 3 条。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@DisplayName("PaymentStateMachine 支付单/退款单状态机")
class PaymentStateMachineTest {

    private static final String PAYMENT_NO = "PAY20261003000001";

    private static final String ORDER_NO = "ORD20261003000001";

    private static final String REFUND_NO = "REF20261003000001";

    private final PaymentStateMachine stateMachine = new PaymentStateMachine();

    // ======================== 支付单合法转移 ========================

    @Nested
    @DisplayName("支付单合法转移")
    class LegalTransitions {

        @Test
        @DisplayName("UNPAID --支付成功回调--> PAID，并记录 paySuccessTime")
        void unpaidPaySuccessToPaid() {
            MallPaymentDO payment = payment(PaymentStatusEnum.UNPAID);

            PaymentStatusEnum target = stateMachine.transition(payment, PaymentEventEnum.PAY_SUCCESS_CALLBACK);

            assertThat(target).isEqualTo(PaymentStatusEnum.PAID);
            assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatusEnum.PAID.getCode());
            assertThat(payment.getPaySuccessTime()).isNotNull();
        }

        @Test
        @DisplayName("UNPAID --支付失败回调--> FAILED")
        void unpaidPayFailToFailed() {
            MallPaymentDO payment = payment(PaymentStatusEnum.UNPAID);

            PaymentStatusEnum target = stateMachine.transition(payment, PaymentEventEnum.PAY_FAIL_CALLBACK);

            assertThat(target).isEqualTo(PaymentStatusEnum.FAILED);
            assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatusEnum.FAILED.getCode());
        }

        @Test
        @DisplayName("UNPAID 且已过期 --支付超时关闭--> CLOSED")
        void unpaidTimeoutToClosed() {
            MallPaymentDO payment = payment(PaymentStatusEnum.UNPAID);
            payment.setExpireTime(LocalDateTime.now().minusMinutes(1));

            PaymentStatusEnum target = stateMachine.transition(payment, PaymentEventEnum.PAY_TIMEOUT_CLOSE);

            assertThat(target).isEqualTo(PaymentStatusEnum.CLOSED);
            assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatusEnum.CLOSED.getCode());
        }

        @Test
        @DisplayName("PAID --发起退款--> REFUNDING")
        void paidRefundStartToRefunding() {
            MallPaymentDO payment = payment(PaymentStatusEnum.PAID);

            PaymentStatusEnum target = stateMachine.transition(payment, PaymentEventEnum.REFUND_START);

            assertThat(target).isEqualTo(PaymentStatusEnum.REFUNDING);
            assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatusEnum.REFUNDING.getCode());
        }

        @Test
        @DisplayName("REFUNDING --退款成功回调--> REFUNDED")
        void refundingRefundSuccessToRefunded() {
            MallPaymentDO payment = payment(PaymentStatusEnum.REFUNDING);

            PaymentStatusEnum target = stateMachine.transition(payment, PaymentEventEnum.REFUND_SUCCESS_CALLBACK);

            assertThat(target).isEqualTo(PaymentStatusEnum.REFUNDED);
            assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatusEnum.REFUNDED.getCode());
        }

        @Test
        @DisplayName("REFUNDING --退款失败回调--> 回退 PAID")
        void refundingRefundFailBackToPaid() {
            MallPaymentDO payment = payment(PaymentStatusEnum.REFUNDING);

            PaymentStatusEnum target = stateMachine.transition(payment, PaymentEventEnum.REFUND_FAIL_CALLBACK);

            assertThat(target).isEqualTo(PaymentStatusEnum.PAID);
            assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatusEnum.PAID.getCode());
        }
    }

    // ======================== 支付单非法转移 ========================

    @Nested
    @DisplayName("支付单非法转移（未定义于转移矩阵）")
    class IllegalTransitions {

        @Test
        @DisplayName("UNPAID --发起退款--> 非法，抛 A0702")
        void unpaidRefundStart() {
            assertErrorCode(ErrorCode.ORDER_STATUS_ERROR.getCode(),
                    () -> stateMachine.transition(payment(PaymentStatusEnum.UNPAID), PaymentEventEnum.REFUND_START));
        }

        @Test
        @DisplayName("PAID --支付成功回调--> 非法（重复回调），抛 A0702")
        void paidPaySuccessCallback() {
            assertErrorCode(ErrorCode.ORDER_STATUS_ERROR.getCode(),
                    () -> stateMachine.transition(payment(PaymentStatusEnum.PAID),
                            PaymentEventEnum.PAY_SUCCESS_CALLBACK));
        }

        @Test
        @DisplayName("CLOSED --支付成功回调--> 非法（已关单），抛 A0702")
        void closedPaySuccessCallback() {
            assertErrorCode(ErrorCode.ORDER_STATUS_ERROR.getCode(),
                    () -> stateMachine.transition(payment(PaymentStatusEnum.CLOSED),
                            PaymentEventEnum.PAY_SUCCESS_CALLBACK));
        }

        @Test
        @DisplayName("PAID --支付超时关闭--> 非法（已支付不可关单），抛 A0702")
        void paidTimeoutClose() {
            assertErrorCode(ErrorCode.ORDER_STATUS_ERROR.getCode(),
                    () -> stateMachine.transition(payment(PaymentStatusEnum.PAID),
                            PaymentEventEnum.PAY_TIMEOUT_CLOSE));
        }

        @Test
        @DisplayName("REFUNDED --发起退款--> 非法（已退款终态），抛 A0702")
        void refundedRefundStart() {
            assertErrorCode(ErrorCode.ORDER_STATUS_ERROR.getCode(),
                    () -> stateMachine.transition(payment(PaymentStatusEnum.REFUNDED),
                            PaymentEventEnum.REFUND_START));
        }

        @Test
        @DisplayName("FAILED --支付成功回调--> 非法（终态），抛 A0702")
        void failedPaySuccessCallback() {
            assertErrorCode(ErrorCode.ORDER_STATUS_ERROR.getCode(),
                    () -> stateMachine.transition(payment(PaymentStatusEnum.FAILED),
                            PaymentEventEnum.PAY_SUCCESS_CALLBACK));
        }
    }

    // ======================== 前置条件失败 ========================

    @Nested
    @DisplayName("前置条件不满足")
    class PreconditionFailures {

        @Test
        @DisplayName("UNPAID 未过期 --支付超时关闭--> 抛 A0703（防定时任务误关单）")
        void timeoutCloseNotExpired() {
            MallPaymentDO payment = payment(PaymentStatusEnum.UNPAID);
            payment.setExpireTime(LocalDateTime.now().plusMinutes(30));

            assertErrorCode(ErrorCode.ORDER_ACTION_DENIED.getCode(),
                    () -> stateMachine.transition(payment, PaymentEventEnum.PAY_TIMEOUT_CLOSE));
        }

        @Test
        @DisplayName("UNPAID 无过期时间 --支付超时关闭--> 抛 A0703")
        void timeoutCloseWithoutExpireTime() {
            MallPaymentDO payment = payment(PaymentStatusEnum.UNPAID);
            payment.setExpireTime(null);

            assertErrorCode(ErrorCode.ORDER_ACTION_DENIED.getCode(),
                    () -> stateMachine.transition(payment, PaymentEventEnum.PAY_TIMEOUT_CLOSE));
        }

        @Test
        @DisplayName("前置条件失败时不改动状态字段")
        void preconditionFailureKeepsStatus() {
            MallPaymentDO payment = payment(PaymentStatusEnum.UNPAID);
            payment.setExpireTime(LocalDateTime.now().plusMinutes(30));

            catchThrowableOfType(() -> stateMachine.transition(payment, PaymentEventEnum.PAY_TIMEOUT_CLOSE),
                    BusinessException.class);

            assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatusEnum.UNPAID.getCode());
        }
    }

    // ======================== 退款单转移 ========================

    @Nested
    @DisplayName("退款单转移")
    class RefundTransitions {

        @Test
        @DisplayName("PROCESSING --退款成功回调--> SUCCESS，并记录 refundSuccessTime")
        void processingRefundSuccessToSuccess() {
            MallRefundDO refund = refund(RefundStatusEnum.PROCESSING);

            RefundStatusEnum target = stateMachine.refundTransition(refund,
                    RefundEventEnum.REFUND_SUCCESS_CALLBACK);

            assertThat(target).isEqualTo(RefundStatusEnum.SUCCESS);
            assertThat(refund.getRefundStatus()).isEqualTo(RefundStatusEnum.SUCCESS.getCode());
            assertThat(refund.getRefundSuccessTime()).isNotNull();
        }

        @Test
        @DisplayName("PROCESSING --退款失败回调--> FAILED")
        void processingRefundFailToFailed() {
            MallRefundDO refund = refund(RefundStatusEnum.PROCESSING);

            RefundStatusEnum target = stateMachine.refundTransition(refund, RefundEventEnum.REFUND_FAIL_CALLBACK);

            assertThat(target).isEqualTo(RefundStatusEnum.FAILED);
            assertThat(refund.getRefundStatus()).isEqualTo(RefundStatusEnum.FAILED.getCode());
        }

        @Test
        @DisplayName("FAILED --重试退款--> PROCESSING")
        void failedRetryToProcessing() {
            MallRefundDO refund = refund(RefundStatusEnum.FAILED);

            RefundStatusEnum target = stateMachine.refundTransition(refund, RefundEventEnum.RETRY_REFUND);

            assertThat(target).isEqualTo(RefundStatusEnum.PROCESSING);
            assertThat(refund.getRefundStatus()).isEqualTo(RefundStatusEnum.PROCESSING.getCode());
        }

        @Test
        @DisplayName("SUCCESS --重试退款--> 非法（终态），抛 A0702")
        void successRetryRefund() {
            assertErrorCode(ErrorCode.ORDER_STATUS_ERROR.getCode(),
                    () -> stateMachine.refundTransition(refund(RefundStatusEnum.SUCCESS),
                            RefundEventEnum.RETRY_REFUND));
        }

        @Test
        @DisplayName("SUCCESS --退款失败回调--> 非法（终态），抛 A0702")
        void successRefundFailCallback() {
            assertErrorCode(ErrorCode.ORDER_STATUS_ERROR.getCode(),
                    () -> stateMachine.refundTransition(refund(RefundStatusEnum.SUCCESS),
                            RefundEventEnum.REFUND_FAIL_CALLBACK));
        }

        @Test
        @DisplayName("FAILED --退款成功回调--> 非法（需先重试），抛 A0702")
        void failedRefundSuccessCallback() {
            assertErrorCode(ErrorCode.ORDER_STATUS_ERROR.getCode(),
                    () -> stateMachine.refundTransition(refund(RefundStatusEnum.FAILED),
                            RefundEventEnum.REFUND_SUCCESS_CALLBACK));
        }
    }

    // ======================== canTransit ========================

    @Nested
    @DisplayName("canTransit 转移预判")
    class CanTransit {

        @Test
        @DisplayName("已定义的转移返回 true")
        void allowedTransitions() {
            assertThat(stateMachine.canTransit(PaymentStatusEnum.UNPAID,
                    PaymentEventEnum.PAY_SUCCESS_CALLBACK)).isTrue();
            assertThat(stateMachine.canTransit(PaymentStatusEnum.PAID,
                    PaymentEventEnum.REFUND_START)).isTrue();
            assertThat(stateMachine.canRefundTransit(RefundStatusEnum.FAILED,
                    RefundEventEnum.RETRY_REFUND)).isTrue();
        }

        @Test
        @DisplayName("未定义的转移返回 false")
        void deniedTransitions() {
            assertThat(stateMachine.canTransit(PaymentStatusEnum.UNPAID,
                    PaymentEventEnum.REFUND_START)).isFalse();
            assertThat(stateMachine.canTransit(PaymentStatusEnum.REFUNDED,
                    PaymentEventEnum.REFUND_SUCCESS_CALLBACK)).isFalse();
            assertThat(stateMachine.canRefundTransit(RefundStatusEnum.SUCCESS,
                    RefundEventEnum.RETRY_REFUND)).isFalse();
        }
    }

    // ======================== 夹具与断言辅助 ========================

    /**
     * 构造指定状态的支付单夹具（默认未过期）
     *
     * @param status 支付单状态
     * @return 支付单
     */
    private static MallPaymentDO payment(PaymentStatusEnum status) {
        MallPaymentDO payment = new MallPaymentDO();
        payment.setPaymentNo(PAYMENT_NO);
        payment.setOrderNo(ORDER_NO);
        payment.setPaymentStatus(status.getCode());
        payment.setVersion(0);
        payment.setExpireTime(LocalDateTime.now().plusMinutes(30));
        return payment;
    }

    /**
     * 构造指定状态的退款单夹具
     *
     * @param status 退款单状态
     * @return 退款单
     */
    private static MallRefundDO refund(RefundStatusEnum status) {
        MallRefundDO refund = new MallRefundDO();
        refund.setRefundNo(REFUND_NO);
        refund.setOrderNo(ORDER_NO);
        refund.setRefundStatus(status.getCode());
        refund.setVersion(0);
        return refund;
    }

    /**
     * 断言执行结果抛出的业务异常错误码
     *
     * @param expectedCode 期望错误码
     * @param callable     待执行逻辑
     */
    private static void assertErrorCode(String expectedCode, ThrowingCallable callable) {
        BusinessException ex = catchThrowableOfType(callable, BusinessException.class);
        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(expectedCode);
    }
}
