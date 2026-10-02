package com.mall.order.statemachine;

import com.mall.common.enums.order.OrderStatusEnum;
import com.mall.common.exception.BusinessException;
import com.mall.order.DO.MallOrderDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * 订单状态机单元测试
 *
 * <p>纯逻辑测试，不依赖 Spring 容器与数据库。覆盖设计文档 §6.3 转移矩阵全部条目。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
class OrderStateMachineTest {

    private static final String ORDER_NO = "ORDER_TEST_001";

    private OrderStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new OrderStateMachine();
    }

    /**
     * 构造一个处于指定状态的订单
     */
    private MallOrderDO order(OrderStatusEnum status) {
        MallOrderDO order = new MallOrderDO();
        order.setId(1L);
        order.setOrderNo(ORDER_NO);
        order.setOrderStatus(status.getCode());
        order.setTotalAmount(10000L);
        order.setPayAmount(10000L);
        order.setPayExpireTime(LocalDateTime.now().plusMinutes(30));
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        return order;
    }

    /**
     * 执行转移并断言目标状态
     */
    private void assertTransit(MallOrderDO order, OrderEventEnum event, OrderStatusEnum expected) {
        assertThat(stateMachine.transition(order, event)).isEqualTo(expected);
        assertThat(order.getOrderStatus()).isEqualTo(expected.getCode());
    }

    /**
     * 断言抛出指定错误码的业务异常
     */
    private void assertDenied(MallOrderDO order, OrderEventEnum event, String expectedCode) {
        BusinessException ex = catchThrowableOfType(
                () -> stateMachine.transition(order, event), BusinessException.class);
        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(expectedCode);
    }

    // ═══════════════════════════════════════════════════════════
    // WAIT_PAY
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("WAIT_PAY 待支付")
    class WaitPay {

        @Test
        @DisplayName("PAY_SUCCESS → PAID")
        void paySuccess() {
            assertTransit(order(OrderStatusEnum.WAIT_PAY), OrderEventEnum.PAY_SUCCESS, OrderStatusEnum.PAID);
        }

        @Test
        @DisplayName("PAY_SUCCESS 但订单已过期 → A0703")
        void paySuccessExpired() {
            MallOrderDO o = order(OrderStatusEnum.WAIT_PAY);
            o.setPayExpireTime(LocalDateTime.now().minusMinutes(1));
            assertDenied(o, OrderEventEnum.PAY_SUCCESS, "A0703");
        }

        @Test
        @DisplayName("USER_CANCEL → CANCELLED")
        void userCancel() {
            assertTransit(order(OrderStatusEnum.WAIT_PAY), OrderEventEnum.USER_CANCEL, OrderStatusEnum.CANCELLED);
        }

        @Test
        @DisplayName("PAY_TIMEOUT 已到期 → CLOSED")
        void payTimeout() {
            MallOrderDO o = order(OrderStatusEnum.WAIT_PAY);
            o.setPayExpireTime(LocalDateTime.now().minusMinutes(1));
            assertTransit(o, OrderEventEnum.PAY_TIMEOUT, OrderStatusEnum.CLOSED);
        }

        @Test
        @DisplayName("PAY_TIMEOUT 未到期 → A0703")
        void payTimeoutNotYet() {
            assertDenied(order(OrderStatusEnum.WAIT_PAY), OrderEventEnum.PAY_TIMEOUT, "A0703");
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PAID
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PAID 已支付")
    class Paid {

        @Test
        @DisplayName("SELLER_DELIVER 物流信息已填 → WAIT_DELIVER")
        void sellerDeliver() {
            MallOrderDO o = order(OrderStatusEnum.PAID);
            o.setLogisticsCompany("顺丰速运");
            o.setLogisticsNo("SF1234567890");
            assertTransit(o, OrderEventEnum.SELLER_DELIVER, OrderStatusEnum.WAIT_DELIVER);
        }

        @Test
        @DisplayName("SELLER_DELIVER 物流信息缺失 → A0703")
        void sellerDeliverWithoutLogistics() {
            assertDenied(order(OrderStatusEnum.PAID), OrderEventEnum.SELLER_DELIVER, "A0703");
        }

        @Test
        @DisplayName("SELLER_DELIVER 仅填单号缺公司 → A0703")
        void sellerDeliverPartialLogistics() {
            MallOrderDO o = order(OrderStatusEnum.PAID);
            o.setLogisticsNo("SF1234567890");
            assertDenied(o, OrderEventEnum.SELLER_DELIVER, "A0703");
        }

        @Test
        @DisplayName("FORCE_CANCEL → CANCELLED")
        void forceCancel() {
            assertTransit(order(OrderStatusEnum.PAID), OrderEventEnum.FORCE_CANCEL, OrderStatusEnum.CANCELLED);
        }

        @Test
        @DisplayName("REFUND_ONLY 未发货 → REFUNDING")
        void refundOnly() {
            assertTransit(order(OrderStatusEnum.PAID), OrderEventEnum.REFUND_ONLY, OrderStatusEnum.REFUNDING);
        }

        @Test
        @DisplayName("REFUND_ONLY 已发货 → A0703")
        void refundOnlyAfterDelivered() {
            MallOrderDO o = order(OrderStatusEnum.PAID);
            o.setDeliveryTime(LocalDateTime.now().minusDays(1));
            assertDenied(o, OrderEventEnum.REFUND_ONLY, "A0703");
        }
    }

    // ═══════════════════════════════════════════════════════════
    // WAIT_DELIVER / WAIT_RECEIVE / COMPLETED
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("WAIT_DELIVER 待发货")
    class WaitDeliver {

        @Test
        @DisplayName("LOGISTICS_PICK → WAIT_RECEIVE")
        void logisticsPick() {
            assertTransit(order(OrderStatusEnum.WAIT_DELIVER),
                    OrderEventEnum.LOGISTICS_PICK, OrderStatusEnum.WAIT_RECEIVE);
        }
    }

    @Nested
    @DisplayName("WAIT_RECEIVE 待收货")
    class WaitReceive {

        @Test
        @DisplayName("CONFIRM_RECEIPT → COMPLETED")
        void confirmReceipt() {
            assertTransit(order(OrderStatusEnum.WAIT_RECEIVE),
                    OrderEventEnum.CONFIRM_RECEIPT, OrderStatusEnum.COMPLETED);
        }

        @Test
        @DisplayName("RETURN_REFUND → REFUNDING")
        void returnRefund() {
            assertTransit(order(OrderStatusEnum.WAIT_RECEIVE),
                    OrderEventEnum.RETURN_REFUND, OrderStatusEnum.REFUNDING);
        }
    }

    @Nested
    @DisplayName("COMPLETED 已完成")
    class Completed {

        @Test
        @DisplayName("AFTER_SALE 收货 7 天内 → REFUNDING")
        void afterSaleWithinPeriod() {
            MallOrderDO o = order(OrderStatusEnum.COMPLETED);
            o.setCompleteTime(LocalDateTime.now().minusDays(3));
            assertTransit(o, OrderEventEnum.AFTER_SALE, OrderStatusEnum.REFUNDING);
        }

        @Test
        @DisplayName("AFTER_SALE 超过 7 天 → A0703")
        void afterSaleExpired() {
            MallOrderDO o = order(OrderStatusEnum.COMPLETED);
            o.setCompleteTime(LocalDateTime.now().minusDays(8));
            assertDenied(o, OrderEventEnum.AFTER_SALE, "A0703");
        }

        @Test
        @DisplayName("COMPLETED --PAY_SUCCESS--> 非法转移 → A0702")
        void illegalTransition() {
            assertDenied(order(OrderStatusEnum.COMPLETED), OrderEventEnum.PAY_SUCCESS, "A0702");
        }
    }

    // ═══════════════════════════════════════════════════════════
    // REFUNDING —— 退款失败动态回退
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("REFUNDING 退款中")
    class Refunding {

        @Test
        @DisplayName("进入 REFUNDING 时记录退款前状态")
        void recordPreRefundStatusOnEnterRefunding() {
            MallOrderDO o = order(OrderStatusEnum.PAID);
            stateMachine.transition(o, OrderEventEnum.REFUND_ONLY);
            assertThat(o.getPreRefundStatus()).isEqualTo(OrderStatusEnum.PAID.getCode());
        }

        @Test
        @DisplayName("WAIT_RECEIVE 进入 REFUNDING 时记录退款前状态")
        void recordPreRefundStatusFromWaitReceive() {
            MallOrderDO o = order(OrderStatusEnum.WAIT_RECEIVE);
            stateMachine.transition(o, OrderEventEnum.RETURN_REFUND);
            assertThat(o.getPreRefundStatus()).isEqualTo(OrderStatusEnum.WAIT_RECEIVE.getCode());
        }

        @Test
        @DisplayName("REFUND_FAIL 按 pre_refund_status 精确回退到 WAIT_RECEIVE")
        void refundFailToWaitReceiveByPreRefundStatus() {
            MallOrderDO o = order(OrderStatusEnum.REFUNDING);
            o.setPreRefundStatus(OrderStatusEnum.WAIT_RECEIVE.getCode());
            // 时间戳会推断为 WAIT_DELIVER，pre_refund_status 应优先
            o.setDeliveryTime(LocalDateTime.now().minusDays(2));
            assertTransit(o, OrderEventEnum.REFUND_FAIL, OrderStatusEnum.WAIT_RECEIVE);
        }

        @Test
        @DisplayName("REFUND_SUCCESS → REFUNDED")
        void refundSuccess() {
            assertTransit(order(OrderStatusEnum.REFUNDING),
                    OrderEventEnum.REFUND_SUCCESS, OrderStatusEnum.REFUNDED);
        }

        @Test
        @DisplayName("REFUND_FAIL 无 pre_refund_status 时回退时间戳推断 → PAID")
        void refundFailFallbackToPaid() {
            assertTransit(order(OrderStatusEnum.REFUNDING), OrderEventEnum.REFUND_FAIL, OrderStatusEnum.PAID);
        }

        @Test
        @DisplayName("REFUND_FAIL 无 pre_refund_status 时回退时间戳推断 → WAIT_DELIVER")
        void refundFailFallbackToWaitDeliver() {
            MallOrderDO o = order(OrderStatusEnum.REFUNDING);
            o.setDeliveryTime(LocalDateTime.now().minusDays(2));
            assertTransit(o, OrderEventEnum.REFUND_FAIL, OrderStatusEnum.WAIT_DELIVER);
        }

        @Test
        @DisplayName("REFUND_FAIL 无 pre_refund_status 时回退时间戳推断 → COMPLETED")
        void refundFailFallbackToCompleted() {
            MallOrderDO o = order(OrderStatusEnum.REFUNDING);
            o.setDeliveryTime(LocalDateTime.now().minusDays(2));
            o.setCompleteTime(LocalDateTime.now().minusDays(1));
            assertTransit(o, OrderEventEnum.REFUND_FAIL, OrderStatusEnum.COMPLETED);
        }

        @Test
        @DisplayName("pre_refund_status 非法值 → 回退时间戳推断")
        void refundFailIllegalPreRefundStatus() {
            MallOrderDO o = order(OrderStatusEnum.REFUNDING);
            o.setPreRefundStatus(99);
            o.setCompleteTime(LocalDateTime.now().minusDays(1));
            assertTransit(o, OrderEventEnum.REFUND_FAIL, OrderStatusEnum.COMPLETED);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 终态清理
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("终态清理")
    class Finalize {

        @Test
        @DisplayName("CANCELLED 满 7 天 → CLOSED")
        void cancelledFinalize() {
            MallOrderDO o = order(OrderStatusEnum.CANCELLED);
            o.setUpdateTime(LocalDateTime.now().minusDays(8));
            assertTransit(o, OrderEventEnum.PAY_TIMEOUT, OrderStatusEnum.CLOSED);
        }

        @Test
        @DisplayName("CANCELLED 未满 7 天 → A0703")
        void cancelledNotYet() {
            MallOrderDO o = order(OrderStatusEnum.CANCELLED);
            o.setUpdateTime(LocalDateTime.now().minusDays(2));
            assertDenied(o, OrderEventEnum.PAY_TIMEOUT, "A0703");
        }

        @Test
        @DisplayName("REFUNDED 满 7 天 → CLOSED")
        void refundedFinalize() {
            MallOrderDO o = order(OrderStatusEnum.REFUNDED);
            o.setUpdateTime(LocalDateTime.now().minusDays(8));
            assertTransit(o, OrderEventEnum.PAY_TIMEOUT, OrderStatusEnum.CLOSED);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 通用
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("canTransit 正确反映转移矩阵")
    void canTransit() {
        assertThat(stateMachine.canTransit(OrderStatusEnum.WAIT_PAY, OrderEventEnum.PAY_SUCCESS)).isTrue();
        assertThat(stateMachine.canTransit(OrderStatusEnum.WAIT_PAY, OrderEventEnum.CONFIRM_RECEIPT)).isFalse();
        assertThat(stateMachine.canTransit(OrderStatusEnum.COMPLETED, OrderEventEnum.AFTER_SALE)).isTrue();
    }

    @Test
    @DisplayName("状态码非法 → A0702")
    void illegalStatusCode() {
        MallOrderDO o = order(OrderStatusEnum.PAID);
        o.setOrderStatus(99);
        assertDenied(o, OrderEventEnum.PAY_SUCCESS, "A0702");
    }

    @Test
    @DisplayName("转移后 updateTime 被刷新")
    void updateTimeRefreshed() {
        MallOrderDO o = order(OrderStatusEnum.WAIT_PAY);
        o.setUpdateTime(LocalDateTime.now().minusHours(1));
        stateMachine.transition(o, OrderEventEnum.PAY_SUCCESS);
        assertThat(o.getUpdateTime()).isAfter(LocalDateTime.now().minusMinutes(1));
    }
}