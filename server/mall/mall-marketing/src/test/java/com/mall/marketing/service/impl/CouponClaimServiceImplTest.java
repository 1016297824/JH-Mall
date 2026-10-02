package com.mall.marketing.service.impl;

import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.marketing.CouponRecordStatusEnum;
import com.mall.common.enums.marketing.CouponStatusEnum;
import com.mall.common.enums.marketing.CouponTypeEnum;
import com.mall.common.constant.MqTopicConstants;
import com.mall.common.exception.BusinessException;
import com.mall.marketing.DO.MallCouponDO;
import com.mall.marketing.DO.MallCouponRecordDO;
import com.mall.marketing.config.MallMarketingConfigProperties;
import com.mall.marketing.dto.response.CouponRecordResp;
import com.mall.marketing.infrastructure.outbox.OutboxPublisher;
import com.mall.marketing.mapper.MallCouponMapper;
import com.mall.marketing.mapper.MallCouponRecordMapper;
import com.mall.marketing.statemachine.CouponStateMachine;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户优惠券服务单元测试
 *
 * <p>Mock 掉 Mapper，保留真实的 {@link CouponStateMachine}（纯逻辑、无状态），
 * 覆盖设计文档 {@code docs/design/14_mall-marketing详细设计.md} §3.2 / §4 / §5 / §6，
 * 以及「券归属校验」这条安全约束。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@ExtendWith(MockitoExtension.class)
class CouponClaimServiceImplTest {

    private static final Long USER_ID = 100L;
    private static final Long OTHER_USER_ID = 999L;
    private static final Long COUPON_ID = 10L;
    private static final Long RECORD_ID = 500L;
    private static final String ORDER_NO = "ORDER_20261002_0001";

    @Mock private MallCouponMapper couponMapper;
    @Mock private MallCouponRecordMapper couponRecordMapper;
    @Mock private MallMarketingConfigProperties config;
    @Mock private OutboxPublisher outboxPublisher;

    /** 真实状态机：状态转移语义需要被真正执行 */
    @Spy private CouponStateMachine couponStateMachine = new CouponStateMachine();

    @InjectMocks private CouponClaimServiceImpl couponClaimService;

    private MallMarketingConfigProperties.Coupon couponConfig;

    @BeforeEach
    void setUp() {
        couponConfig = new MallMarketingConfigProperties.Coupon();
    }

    /**
     * 断言抛出指定错误码的业务异常
     *
     * @param expectedCode 期望的错误码字面量
     * @param callable     被测调用
     */
    private static void assertErrorCode(String expectedCode, ThrowingCallable callable) {
        BusinessException ex = catchThrowableOfType(callable, BusinessException.class);
        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(expectedCode);
    }

    // ═══════════════════════════════════════════════════════════
    // 测试数据构造
    // ═══════════════════════════════════════════════════════════

    /** 可领取的券定义（已发布 + 未过期 + 有剩余量） */
    private MallCouponDO claimableCoupon() {
        MallCouponDO coupon = new MallCouponDO();
        coupon.setId(COUPON_ID);
        coupon.setCouponName("满 100 减 10");
        coupon.setCouponType(CouponTypeEnum.FULL_REDUCE.getCode());
        coupon.setFaceValue(1000L);
        coupon.setMinOrderAmount(10000L);
        coupon.setTotalCount(1000);
        coupon.setRemainCount(1000);
        coupon.setPerUserLimit(1);
        coupon.setUseStartTime(LocalDateTime.now().minusDays(1));
        coupon.setUseEndTime(LocalDateTime.now().plusDays(30));
        coupon.setCouponStatus(CouponStatusEnum.PUBLISHED.getCode());
        coupon.setIsDeleted(0);
        coupon.setVersion(0);
        return coupon;
    }

    /** 构造一条券记录 */
    private MallCouponRecordDO record(CouponRecordStatusEnum status, Long ownerId, LocalDateTime expireTime) {
        MallCouponRecordDO record = new MallCouponRecordDO();
        record.setId(RECORD_ID);
        record.setCouponId(COUPON_ID);
        record.setUserId(ownerId);
        record.setCouponCode("CPN20261002120000000001");
        record.setRecordStatus(status.getCode());
        record.setFaceValue(1000L);
        record.setExpireTime(expireTime);
        record.setIsDeleted(0);
        record.setCreateTime(LocalDateTime.now().minusMinutes(10));
        return record;
    }

    /** 构造一条未过期的券记录 */
    private MallCouponRecordDO record(CouponRecordStatusEnum status, Long ownerId) {
        return record(status, ownerId, LocalDateTime.now().plusDays(30));
    }

    // ═══════════════════════════════════════════════════════════
    // 领券
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("领券 claimCoupon")
    class ClaimCoupon {

        @Test
        @DisplayName("正常领取：扣减库存成功并落券记录，返回记录 ID")
        void claimSuccess() {
            when(couponMapper.selectByIdNotDeleted(COUPON_ID)).thenReturn(claimableCoupon());
            when(couponRecordMapper.countClaimed(USER_ID, COUPON_ID)).thenReturn(0L);
            when(couponMapper.decreaseRemainCount(COUPON_ID, 0)).thenReturn(1);
            when(couponRecordMapper.insert(any(MallCouponRecordDO.class))).thenAnswer(inv -> {
                MallCouponRecordDO saved = inv.getArgument(0);
                saved.setId(RECORD_ID);
                return 1;
            });

            Long claimId = couponClaimService.claimCoupon(USER_ID, COUPON_ID);

            assertThat(claimId).isEqualTo(RECORD_ID);
        }

        @Test
        @DisplayName("券记录初始状态 AVAILABLE，面值与过期时间取券定义快照")
        void claimShouldSnapshotFaceValueAndExpireTime() {
            MallCouponDO coupon = claimableCoupon();
            when(couponMapper.selectByIdNotDeleted(COUPON_ID)).thenReturn(coupon);
            when(couponRecordMapper.countClaimed(USER_ID, COUPON_ID)).thenReturn(0L);
            when(couponMapper.decreaseRemainCount(COUPON_ID, 0)).thenReturn(1);
            when(couponRecordMapper.insert(any(MallCouponRecordDO.class))).thenReturn(1);

            couponClaimService.claimCoupon(USER_ID, COUPON_ID);

            ArgumentCaptor<MallCouponRecordDO> captor = ArgumentCaptor.forClass(MallCouponRecordDO.class);
            verify(couponRecordMapper).insert(captor.capture());
            MallCouponRecordDO saved = captor.getValue();

            assertThat(saved.getRecordStatus()).isEqualTo(CouponRecordStatusEnum.AVAILABLE.getCode());
            assertThat(saved.getFaceValue()).isEqualTo(coupon.getFaceValue());
            assertThat(saved.getExpireTime()).isEqualTo(coupon.getUseEndTime());
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getCouponCode()).startsWith("CPN");
        }

        @Test
        @DisplayName("券不存在 → A0610")
        void claimCouponNotFound() {
            when(couponMapper.selectByIdNotDeleted(COUPON_ID)).thenReturn(null);

            assertErrorCode(ErrorCode.COUPON_EXPIRED.getCode(),
                    () -> couponClaimService.claimCoupon(USER_ID, COUPON_ID));
        }

        @Test
        @DisplayName("券未发布（草稿态）→ A0610")
        void claimCouponNotPublished() {
            MallCouponDO coupon = claimableCoupon();
            coupon.setCouponStatus(CouponStatusEnum.DRAFT.getCode());
            when(couponMapper.selectByIdNotDeleted(COUPON_ID)).thenReturn(coupon);

            assertErrorCode(ErrorCode.COUPON_EXPIRED.getCode(),
                    () -> couponClaimService.claimCoupon(USER_ID, COUPON_ID));
        }

        @Test
        @DisplayName("券已过有效期 → A0610")
        void claimCouponExpired() {
            MallCouponDO coupon = claimableCoupon();
            coupon.setUseEndTime(LocalDateTime.now().minusDays(1));
            when(couponMapper.selectByIdNotDeleted(COUPON_ID)).thenReturn(coupon);

            assertErrorCode(ErrorCode.COUPON_EXPIRED.getCode(),
                    () -> couponClaimService.claimCoupon(USER_ID, COUPON_ID));
        }

        @Test
        @DisplayName("超过每人限领数 → A0502，且不扣减库存")
        void claimCouponExceedPerUserLimit() {
            MallCouponDO coupon = claimableCoupon();
            coupon.setPerUserLimit(1);
            when(couponMapper.selectByIdNotDeleted(COUPON_ID)).thenReturn(coupon);
            when(couponRecordMapper.countClaimed(USER_ID, COUPON_ID)).thenReturn(1L);

            assertErrorCode(ErrorCode.RESOURCE_EXISTS.getCode(),
                    () -> couponClaimService.claimCoupon(USER_ID, COUPON_ID));

            verify(couponMapper, never()).decreaseRemainCount(anyLong(), anyInt());
        }

        @Test
        @DisplayName("乐观锁扣减失败（已领完/版本冲突）→ A0611，且不落记录")
        void claimCouponDepleted() {
            when(couponMapper.selectByIdNotDeleted(COUPON_ID)).thenReturn(claimableCoupon());
            when(couponRecordMapper.countClaimed(USER_ID, COUPON_ID)).thenReturn(0L);
            when(couponMapper.decreaseRemainCount(COUPON_ID, 0)).thenReturn(0);

            assertErrorCode(ErrorCode.COUPON_DEPLETED.getCode(),
                    () -> couponClaimService.claimCoupon(USER_ID, COUPON_ID));

            verify(couponRecordMapper, never()).insert(any(MallCouponRecordDO.class));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 锁券
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("锁券 lockCoupon")
    class LockCoupon {

        @Test
        @DisplayName("正常锁定：AVAILABLE → LOCKED 并落库")
        void lockSuccess() {
            when(couponRecordMapper.selectByIdNotDeleted(RECORD_ID)).thenReturn(
                    record(CouponRecordStatusEnum.AVAILABLE, USER_ID));
            when(couponRecordMapper.lockById(RECORD_ID, ORDER_NO)).thenReturn(1);

            assertThat(couponClaimService.lockCoupon(RECORD_ID, ORDER_NO)).isTrue();

            verify(couponRecordMapper).lockById(RECORD_ID, ORDER_NO);
        }

        @Test
        @DisplayName("券记录不存在 → A0501")
        void lockRecordNotFound() {
            when(couponRecordMapper.selectByIdNotDeleted(RECORD_ID)).thenReturn(null);

            assertErrorCode(ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    () -> couponClaimService.lockCoupon(RECORD_ID, ORDER_NO));
        }

        @Test
        @DisplayName("券已被占用（LOCKED）→ 返回 false，不抛异常（契约返回 boolean）")
        void lockAlreadyLocked() {
            when(couponRecordMapper.selectByIdNotDeleted(RECORD_ID)).thenReturn(
                    record(CouponRecordStatusEnum.LOCKED, USER_ID));

            assertThat(couponClaimService.lockCoupon(RECORD_ID, ORDER_NO)).isFalse();

            verify(couponRecordMapper, never()).lockById(anyLong(), any());
        }

        @Test
        @DisplayName("券已过期 → 返回 false，不落库")
        void lockExpiredCoupon() {
            when(couponRecordMapper.selectByIdNotDeleted(RECORD_ID)).thenReturn(
                    record(CouponRecordStatusEnum.AVAILABLE, USER_ID, LocalDateTime.now().minusMinutes(1)));

            assertThat(couponClaimService.lockCoupon(RECORD_ID, ORDER_NO)).isFalse();

            verify(couponRecordMapper, never()).lockById(anyLong(), any());
        }

        @Test
        @DisplayName("并发下 CAS 落库失败（影响 0 行）→ 返回 false")
        void lockCasFailed() {
            when(couponRecordMapper.selectByIdNotDeleted(RECORD_ID)).thenReturn(
                    record(CouponRecordStatusEnum.AVAILABLE, USER_ID));
            when(couponRecordMapper.lockById(RECORD_ID, ORDER_NO)).thenReturn(0);

            assertThat(couponClaimService.lockCoupon(RECORD_ID, ORDER_NO)).isFalse();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 核销
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("核销 useCoupon")
    class UseCoupon {

        /** 构造一条已被该订单锁定的券记录 */
        private MallCouponRecordDO lockedRecord() {
            MallCouponRecordDO locked = record(CouponRecordStatusEnum.LOCKED, USER_ID);
            locked.setOrderNo(ORDER_NO);
            return locked;
        }

        @Test
        @DisplayName("订单下有锁定券：逐条 LOCKED → USED")
        void useSuccess() {
            when(couponRecordMapper.selectByOrderNoAndStatus(ORDER_NO,
                    CouponRecordStatusEnum.LOCKED.getCode())).thenReturn(List.of(lockedRecord()));
            when(couponRecordMapper.markUsedById(RECORD_ID, ORDER_NO)).thenReturn(1);

            couponClaimService.useCoupon(ORDER_NO);

            verify(couponRecordMapper).markUsedById(RECORD_ID, ORDER_NO);
        }

        @Test
        @DisplayName("订单下无锁定券（重复投递）→ 幂等，不做任何写操作")
        void useIdempotent() {
            when(couponRecordMapper.selectByOrderNoAndStatus(ORDER_NO,
                    CouponRecordStatusEnum.LOCKED.getCode())).thenReturn(List.of());

            couponClaimService.useCoupon(ORDER_NO);

            verify(couponRecordMapper, never()).markUsedById(anyLong(), any());
            verify(outboxPublisher, never()).publish(any(), any(), any(), any());
        }

        @Test
        @DisplayName("核销成功：写 Outbox 核销事实，payload 含设计 §7.1 的全部字段")
        void useShouldPublishOutboxEvent() {
            MallCouponRecordDO locked = lockedRecord();
            when(couponRecordMapper.selectByOrderNoAndStatus(ORDER_NO,
                    CouponRecordStatusEnum.LOCKED.getCode())).thenReturn(List.of(locked));
            when(couponRecordMapper.markUsedById(RECORD_ID, ORDER_NO)).thenReturn(1);

            couponClaimService.useCoupon(ORDER_NO);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(outboxPublisher).publish(eq(MqTopicConstants.Coupon.USED), eq("CouponUsed"),
                    eq(String.valueOf(RECORD_ID)), payloadCaptor.capture());

            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload).containsEntry("couponRecordId", RECORD_ID)
                    .containsEntry("couponId", COUPON_ID)
                    .containsEntry("userId", USER_ID)
                    .containsEntry("orderNo", ORDER_NO)
                    .containsEntry("faceValue", 1000L);
            assertThat(payload.get("useTime")).isNotNull();
        }

        @Test
        @DisplayName("CAS 落库失败（竞态）→ 不写 Outbox，避免留下未真正核销的假事实")
        void useShouldNotPublishOutboxWhenCasFailed() {
            MallCouponRecordDO locked = lockedRecord();
            when(couponRecordMapper.selectByOrderNoAndStatus(ORDER_NO,
                    CouponRecordStatusEnum.LOCKED.getCode())).thenReturn(List.of(locked));
            when(couponRecordMapper.markUsedById(RECORD_ID, ORDER_NO)).thenReturn(0);

            couponClaimService.useCoupon(ORDER_NO);

            verify(outboxPublisher, never()).publish(any(), any(), any(), any());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 释放
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("释放 releaseCoupon")
    class ReleaseCoupon {

        /** 构造一条已被该订单锁定的券记录 */
        private MallCouponRecordDO lockedRecord() {
            MallCouponRecordDO locked = record(CouponRecordStatusEnum.LOCKED, USER_ID);
            locked.setOrderNo(ORDER_NO);
            return locked;
        }

        @Test
        @DisplayName("订单下有锁定券：逐条 LOCKED → RELEASED 并回补库存")
        void releaseSuccess() {
            when(couponRecordMapper.selectByOrderNoAndStatus(ORDER_NO,
                    CouponRecordStatusEnum.LOCKED.getCode())).thenReturn(List.of(lockedRecord()));
            when(couponRecordMapper.releaseById(RECORD_ID, ORDER_NO)).thenReturn(1);

            couponClaimService.releaseCoupon(ORDER_NO);

            verify(couponRecordMapper).releaseById(RECORD_ID, ORDER_NO);
            verify(couponMapper).increaseRemainCount(COUPON_ID);
        }

        @Test
        @DisplayName("无锁定券（重复投递）→ 幂等，不回补库存")
        void releaseIdempotent() {
            when(couponRecordMapper.selectByOrderNoAndStatus(ORDER_NO,
                    CouponRecordStatusEnum.LOCKED.getCode())).thenReturn(List.of());

            couponClaimService.releaseCoupon(ORDER_NO);

            verify(couponRecordMapper, never()).releaseById(anyLong(), any());
            verify(couponMapper, never()).increaseRemainCount(anyLong());
        }

        @Test
        @DisplayName("CAS 落库失败（已被并发释放）→ 不回补库存，避免重复回补")
        void releaseCasFailedShouldNotRestock() {
            when(couponRecordMapper.selectByOrderNoAndStatus(ORDER_NO,
                    CouponRecordStatusEnum.LOCKED.getCode())).thenReturn(List.of(lockedRecord()));
            when(couponRecordMapper.releaseById(RECORD_ID, ORDER_NO)).thenReturn(0);

            couponClaimService.releaseCoupon(ORDER_NO);

            verify(couponMapper, never()).increaseRemainCount(anyLong());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 归属校验（安全）
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("归属校验 validateCoupon")
    class ValidateCoupon {

        @Test
        @DisplayName("券不属于该用户 → A0501（防越权用券）")
        void validateForeignCoupon() {
            when(couponRecordMapper.selectByIdNotDeleted(RECORD_ID)).thenReturn(
                    record(CouponRecordStatusEnum.AVAILABLE, OTHER_USER_ID));

            assertErrorCode(ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    () -> couponClaimService.validateCoupon(RECORD_ID, USER_ID));
        }

        @Test
        @DisplayName("券记录不存在 → A0501（与「非本人」同错误码，不泄露归属）")
        void validateRecordNotFound() {
            when(couponRecordMapper.selectByIdNotDeleted(RECORD_ID)).thenReturn(null);

            assertErrorCode(ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    () -> couponClaimService.validateCoupon(RECORD_ID, USER_ID));
        }

        @Test
        @DisplayName("券状态非 AVAILABLE（已锁定/已使用）→ false")
        void validateWrongStatus() {
            when(couponRecordMapper.selectByIdNotDeleted(RECORD_ID)).thenReturn(
                    record(CouponRecordStatusEnum.USED, USER_ID));

            assertThat(couponClaimService.validateCoupon(RECORD_ID, USER_ID)).isFalse();
        }

        @Test
        @DisplayName("券已过期 → false")
        void validateExpired() {
            when(couponRecordMapper.selectByIdNotDeleted(RECORD_ID)).thenReturn(
                    record(CouponRecordStatusEnum.AVAILABLE, USER_ID, LocalDateTime.now().minusMinutes(1)));

            assertThat(couponClaimService.validateCoupon(RECORD_ID, USER_ID)).isFalse();
        }

        @Test
        @DisplayName("本人的可用券 → true")
        void validateSuccess() {
            when(couponRecordMapper.selectByIdNotDeleted(RECORD_ID)).thenReturn(
                    record(CouponRecordStatusEnum.AVAILABLE, USER_ID));

            assertThat(couponClaimService.validateCoupon(RECORD_ID, USER_ID)).isTrue();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 批量置过期
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("批量置过期 expireCoupons")
    class ExpireCoupons {

        @Test
        @DisplayName("按配置批量上限扫描并置 EXPIRED，返回影响行数")
        void expireReturnsAffectedRows() {
            when(config.getCoupon()).thenReturn(couponConfig);
            when(couponRecordMapper.expireBatch(couponConfig.getExpireBatchSize())).thenReturn(7);

            assertThat(couponClaimService.expireCoupons()).isEqualTo(7);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 我的优惠券
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("我的优惠券 listMyCoupons")
    class ListMyCoupons {

        @Test
        @DisplayName("批量补券名，不逐条查库（避免 N+1）")
        void listShouldEnrichCouponNameInBatch() {
            MallCouponRecordDO r1 = record(CouponRecordStatusEnum.AVAILABLE, USER_ID);
            MallCouponRecordDO r2 = record(CouponRecordStatusEnum.LOCKED, USER_ID);
            r2.setId(501L);
            when(couponRecordMapper.selectUserRecords(USER_ID, null, 20)).thenReturn(List.of(r1, r2));
            when(couponMapper.selectByIdsNotDeleted(anyCollection())).thenReturn(List.of(claimableCoupon()));

            List<CouponRecordResp> result = couponClaimService.listMyCoupons(USER_ID, null, 20);

            assertThat(result).hasSize(2);
            assertThat(result).allSatisfy(resp -> {
                assertThat(resp.getCouponName()).isEqualTo("满 100 减 10");
                assertThat(resp.getRecordStatusDesc()).isNotBlank();
            });
            verify(couponMapper, times(1)).selectByIdsNotDeleted(anyCollection());
        }

        @Test
        @DisplayName("空结果时直接返回空列表，不查券定义")
        void listEmpty() {
            when(couponRecordMapper.selectUserRecords(USER_ID, null, 20)).thenReturn(List.of());

            assertThat(couponClaimService.listMyCoupons(USER_ID, null, 20)).isEmpty();

            verify(couponMapper, never()).selectByIdsNotDeleted(anyCollection());
        }
    }
}
