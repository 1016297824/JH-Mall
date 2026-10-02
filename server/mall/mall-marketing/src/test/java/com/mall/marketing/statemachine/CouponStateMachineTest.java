package com.mall.marketing.statemachine;

import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.marketing.CouponRecordStatusEnum;
import com.mall.common.exception.BusinessException;
import com.mall.marketing.DO.MallCouponRecordDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * 优惠券记录状态机单元测试
 *
 * <p>纯逻辑测试，不依赖 Spring 容器与数据库。覆盖设计文档
 * {@code docs/design/14_mall-marketing详细设计.md} §3.3 转移矩阵全部条目
 * （5 条合法转移 + 非法转移 + 前置条件不满足）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
class CouponStateMachineTest {

    private static final String ORDER_NO = "ORDER_TEST_001";

    private CouponStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new CouponStateMachine();
    }

    /**
     * 构造一条处于指定状态的券记录
     *
     * @param status     记录状态
     * @param expireTime 过期时间
     */
    private MallCouponRecordDO record(CouponRecordStatusEnum status, LocalDateTime expireTime) {
        MallCouponRecordDO record = new MallCouponRecordDO();
        record.setId(1L);
        record.setCouponId(10L);
        record.setUserId(100L);
        record.setCouponCode("CPN20261002120000000001");
        record.setRecordStatus(status.getCode());
        record.setFaceValue(1000L);
        record.setExpireTime(expireTime);
        record.setIsDeleted(0);
        record.setCreateTime(LocalDateTime.now());
        return record;
    }

    /** 构造一条未过期的券记录 */
    private MallCouponRecordDO validRecord(CouponRecordStatusEnum status) {
        return record(status, LocalDateTime.now().plusDays(30));
    }

    @Nested
    @DisplayName("合法转移")
    class LegalTransitions {

        @Test
        @DisplayName("AVAILABLE --下单锁定--> LOCKED，并记录 lock_time（order_no 由 Service 层预先写入）")
        void availableLockToLocked() {
            MallCouponRecordDO record = validRecord(CouponRecordStatusEnum.AVAILABLE);
            // 状态机不感知 orderNo，由 Service 层在转移前写入（见 CouponStateMachine 类注释）
            record.setOrderNo(ORDER_NO);

            CouponRecordStatusEnum target = stateMachine.transition(record, CouponEventEnum.LOCK);

            assertThat(target).isEqualTo(CouponRecordStatusEnum.LOCKED);
            assertThat(record.getRecordStatus()).isEqualTo(CouponRecordStatusEnum.LOCKED.getCode());
            assertThat(record.getOrderNo()).isEqualTo(ORDER_NO);
            assertThat(record.getLockTime()).isNotNull();
        }

        @Test
        @DisplayName("LOCKED --订单支付成功--> USED，并记录 use_time")
        void lockedPaySuccessToUsed() {
            MallCouponRecordDO record = validRecord(CouponRecordStatusEnum.LOCKED);
            record.setOrderNo(ORDER_NO);

            CouponRecordStatusEnum target = stateMachine.transition(record, CouponEventEnum.PAY_SUCCESS);

            assertThat(target).isEqualTo(CouponRecordStatusEnum.USED);
            assertThat(record.getRecordStatus()).isEqualTo(CouponRecordStatusEnum.USED.getCode());
            assertThat(record.getUseTime()).isNotNull();
        }

        @Test
        @DisplayName("LOCKED --订单取消--> RELEASED，清除 order_no 并记录 release_time")
        void lockedOrderCancelToReleased() {
            MallCouponRecordDO record = validRecord(CouponRecordStatusEnum.LOCKED);
            record.setOrderNo(ORDER_NO);
            record.setLockTime(LocalDateTime.now().minusMinutes(5));

            CouponRecordStatusEnum target = stateMachine.transition(record, CouponEventEnum.ORDER_CANCEL);

            assertThat(target).isEqualTo(CouponRecordStatusEnum.RELEASED);
            assertThat(record.getRecordStatus()).isEqualTo(CouponRecordStatusEnum.RELEASED.getCode());
            assertThat(record.getOrderNo()).isNull();
            assertThat(record.getReleaseTime()).isNotNull();
        }

        @Test
        @DisplayName("AVAILABLE --有效期到期--> EXPIRED")
        void availableExpireToExpired() {
            MallCouponRecordDO record = record(CouponRecordStatusEnum.AVAILABLE,
                    LocalDateTime.now().minusDays(1));

            CouponRecordStatusEnum target = stateMachine.transition(record, CouponEventEnum.EXPIRE);

            assertThat(target).isEqualTo(CouponRecordStatusEnum.EXPIRED);
            assertThat(record.getRecordStatus()).isEqualTo(CouponRecordStatusEnum.EXPIRED.getCode());
        }

        @Test
        @DisplayName("RELEASED --超原有效期--> EXPIRED")
        void releasedExpireToExpired() {
            MallCouponRecordDO record = record(CouponRecordStatusEnum.RELEASED,
                    LocalDateTime.now().minusDays(1));

            CouponRecordStatusEnum target = stateMachine.transition(record, CouponEventEnum.EXPIRE);

            assertThat(target).isEqualTo(CouponRecordStatusEnum.EXPIRED);
            assertThat(record.getRecordStatus()).isEqualTo(CouponRecordStatusEnum.EXPIRED.getCode());
        }
    }

    @Nested
    @DisplayName("非法转移：抛 A0702")
    class IllegalTransitions {

        @Test
        @DisplayName("LOCKED --下单锁定--> 非法（已锁定不能重复锁）")
        void lockedCannotLockAgain() {
            MallCouponRecordDO record = validRecord(CouponRecordStatusEnum.LOCKED);

            BusinessException ex = catchThrowableOfType(
                    () -> stateMachine.transition(record, CouponEventEnum.LOCK), BusinessException.class);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ORDER_STATUS_ERROR.getCode());
        }

        @Test
        @DisplayName("AVAILABLE --订单支付成功--> 非法（未锁定不能核销）")
        void availableCannotPaySuccess() {
            MallCouponRecordDO record = validRecord(CouponRecordStatusEnum.AVAILABLE);

            BusinessException ex = catchThrowableOfType(
                    () -> stateMachine.transition(record, CouponEventEnum.PAY_SUCCESS), BusinessException.class);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ORDER_STATUS_ERROR.getCode());
        }

        @Test
        @DisplayName("USED 为终态，--订单取消--> 非法")
        void usedCannotCancel() {
            MallCouponRecordDO record = validRecord(CouponRecordStatusEnum.USED);

            BusinessException ex = catchThrowableOfType(
                    () -> stateMachine.transition(record, CouponEventEnum.ORDER_CANCEL), BusinessException.class);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ORDER_STATUS_ERROR.getCode());
        }

        @Test
        @DisplayName("EXPIRED 为终态，--过期--> 非法")
        void expiredCannotExpireAgain() {
            MallCouponRecordDO record = record(CouponRecordStatusEnum.EXPIRED,
                    LocalDateTime.now().minusDays(1));

            BusinessException ex = catchThrowableOfType(
                    () -> stateMachine.transition(record, CouponEventEnum.EXPIRE), BusinessException.class);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ORDER_STATUS_ERROR.getCode());
        }

        @Test
        @DisplayName("USED 为终态，--过期--> 非法")
        void usedCannotExpire() {
            MallCouponRecordDO record = record(CouponRecordStatusEnum.USED,
                    LocalDateTime.now().minusDays(1));

            BusinessException ex = catchThrowableOfType(
                    () -> stateMachine.transition(record, CouponEventEnum.EXPIRE), BusinessException.class);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ORDER_STATUS_ERROR.getCode());
        }

        @Test
        @DisplayName("状态码非法（库里出现未定义值）→ A0702")
        void illegalStatusCode() {
            MallCouponRecordDO record = validRecord(CouponRecordStatusEnum.AVAILABLE);
            record.setRecordStatus(99);

            BusinessException ex = catchThrowableOfType(
                    () -> stateMachine.transition(record, CouponEventEnum.LOCK), BusinessException.class);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ORDER_STATUS_ERROR.getCode());
        }
    }

    @Nested
    @DisplayName("前置条件不满足：抛 A0612")
    class PreconditionFailures {

        @Test
        @DisplayName("券已过期仍尝试锁定 → A0612")
        void lockExpiredCoupon() {
            MallCouponRecordDO record = record(CouponRecordStatusEnum.AVAILABLE,
                    LocalDateTime.now().minusMinutes(1));

            BusinessException ex = catchThrowableOfType(
                    () -> stateMachine.transition(record, CouponEventEnum.LOCK), BusinessException.class);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.COUPON_CONDITION_NOT_MET.getCode());
        }

        @Test
        @DisplayName("券未到期却触发过期 → A0612")
        void expireNotYetDue() {
            MallCouponRecordDO record = validRecord(CouponRecordStatusEnum.AVAILABLE);

            BusinessException ex = catchThrowableOfType(
                    () -> stateMachine.transition(record, CouponEventEnum.EXPIRE), BusinessException.class);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.COUPON_CONDITION_NOT_MET.getCode());
        }

        @Test
        @DisplayName("锁定态但缺 order_no 仍尝试核销 → A0612")
        void paySuccessWithoutOrderNo() {
            MallCouponRecordDO record = validRecord(CouponRecordStatusEnum.LOCKED);

            BusinessException ex = catchThrowableOfType(
                    () -> stateMachine.transition(record, CouponEventEnum.PAY_SUCCESS), BusinessException.class);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.COUPON_CONDITION_NOT_MET.getCode());
        }
    }

    @Nested
    @DisplayName("canTransit 查询")
    class CanTransit {

        @Test
        @DisplayName("矩阵内组合返回 true，矩阵外返回 false")
        void canTransitMatchesMatrix() {
            assertThat(stateMachine.canTransit(CouponRecordStatusEnum.AVAILABLE, CouponEventEnum.LOCK)).isTrue();
            assertThat(stateMachine.canTransit(CouponRecordStatusEnum.LOCKED, CouponEventEnum.PAY_SUCCESS)).isTrue();
            assertThat(stateMachine.canTransit(CouponRecordStatusEnum.LOCKED, CouponEventEnum.ORDER_CANCEL)).isTrue();
            assertThat(stateMachine.canTransit(CouponRecordStatusEnum.RELEASED, CouponEventEnum.EXPIRE)).isTrue();

            assertThat(stateMachine.canTransit(CouponRecordStatusEnum.USED, CouponEventEnum.LOCK)).isFalse();
            assertThat(stateMachine.canTransit(CouponRecordStatusEnum.EXPIRED, CouponEventEnum.EXPIRE)).isFalse();
        }
    }
}
