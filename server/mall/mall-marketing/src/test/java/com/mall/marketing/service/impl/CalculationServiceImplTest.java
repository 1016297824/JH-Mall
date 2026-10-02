package com.mall.marketing.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mall.api.feign.RemoteMarketingService.CalculationReq;
import com.mall.api.feign.RemoteMarketingService.CalculationReq.CalculationItem;
import com.mall.api.feign.RemoteMarketingService.CalculationResp;
import com.mall.api.feign.RemoteMarketingService.CalculationResp.AppliedPromotion;
import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.marketing.CouponRecordStatusEnum;
import com.mall.common.enums.marketing.CouponStatusEnum;
import com.mall.common.enums.marketing.CouponTypeEnum;
import com.mall.common.enums.marketing.PromotionTypeEnum;
import com.mall.common.enums.marketing.RuleTypeEnum;
import com.mall.common.exception.BusinessException;
import com.mall.marketing.DO.MallCouponDO;
import com.mall.marketing.DO.MallCouponRecordDO;
import com.mall.marketing.DO.MallPromotionDO;
import com.mall.marketing.DO.MallPromotionRuleDO;
import com.mall.marketing.mapper.MallCouponMapper;
import com.mall.marketing.mapper.MallCouponRecordMapper;
import com.mall.marketing.mapper.MallPromotionMapper;
import com.mall.marketing.mapper.MallPromotionRuleMapper;
import com.mall.marketing.service.PromotionRuleMatcher;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 优惠试算服务单元测试
 *
 * <p>Mock 掉 Mapper，保留真实的 {@link PromotionRuleMatcher}（含真实 Caffeine 缓存），
 * 覆盖设计文档 {@code docs/design/14_mall-marketing详细设计.md} §3.4，
 * 以及 2026-10-02 确认的两条口径（单一券择优、券与促销可叠加）。</p>
 *
 * <p><b>重点覆盖的安全约束</b>：指定券必须校验归属，防止用户使用他人的券抵扣。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@ExtendWith(MockitoExtension.class)
class CalculationServiceImplTest {

    private static final Long USER_ID = 100L;
    private static final Long OTHER_USER_ID = 999L;
    private static final Long RECORD_ID = 500L;
    private static final Long COUPON_ID = 10L;
    private static final long TOTAL_AMOUNT = 10000L;

    @Mock private MallCouponRecordMapper couponRecordMapper;
    @Mock private MallCouponMapper couponMapper;
    @Mock private MallPromotionMapper promotionMapper;
    @Mock private MallPromotionRuleMapper promotionRuleMapper;

    private CalculationServiceImpl calculationService;

    @BeforeEach
    void setUp() {
        Cache<String, List<AppliedPromotion>> cache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(Duration.ofMinutes(1))
                .build();
        calculationService = new CalculationServiceImpl(couponRecordMapper, couponMapper,
                promotionMapper, promotionRuleMapper, new PromotionRuleMatcher(cache));
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

    /** 构造试算请求：单个商品，总价可控 */
    private CalculationReq req(Long userId, Long couponClaimId, long price) {
        CalculationReq req = new CalculationReq();
        req.setUserId(userId);
        req.setCouponClaimId(couponClaimId);
        req.setItems(List.of(new CalculationItem(101L, price, 1)));
        return req;
    }

    /** 构造券记录 */
    private MallCouponRecordDO record(Long id, Long couponId, CouponRecordStatusEnum status,
                                     Long faceValue, LocalDateTime expireTime) {
        MallCouponRecordDO record = new MallCouponRecordDO();
        record.setId(id);
        record.setCouponId(couponId);
        record.setUserId(USER_ID);
        record.setCouponCode("CPN" + id);
        record.setRecordStatus(status.getCode());
        record.setFaceValue(faceValue);
        record.setExpireTime(expireTime);
        record.setIsDeleted(0);
        record.setCreateTime(LocalDateTime.now().minusDays(1));
        return record;
    }

    /** 构造未过期的可用券记录 */
    private MallCouponRecordDO availableRecord(Long id, Long couponId, Long faceValue) {
        return record(id, couponId, CouponRecordStatusEnum.AVAILABLE, faceValue,
                LocalDateTime.now().plusDays(30));
    }

    /** 构造满减券定义 */
    private MallCouponDO reduceCoupon(Long id, String name, Long faceValue, Long minOrderAmount) {
        MallCouponDO coupon = new MallCouponDO();
        coupon.setId(id);
        coupon.setCouponName(name);
        coupon.setCouponType(CouponTypeEnum.FULL_REDUCE.getCode());
        coupon.setFaceValue(faceValue);
        coupon.setMinOrderAmount(minOrderAmount);
        coupon.setCouponStatus(CouponStatusEnum.PUBLISHED.getCode());
        coupon.setIsDeleted(0);
        return coupon;
    }

    /** 构造折扣券定义 */
    private MallCouponDO discountCoupon(Long id, String name, Integer discountRate, Long discountLimit) {
        MallCouponDO coupon = new MallCouponDO();
        coupon.setId(id);
        coupon.setCouponName(name);
        coupon.setCouponType(CouponTypeEnum.DISCOUNT.getCode());
        coupon.setDiscountRate(discountRate);
        coupon.setDiscountLimit(discountLimit);
        coupon.setMinOrderAmount(0L);
        coupon.setCouponStatus(CouponStatusEnum.PUBLISHED.getCode());
        coupon.setIsDeleted(0);
        return coupon;
    }

    /** 构造进行中的满减活动 */
    private MallPromotionDO promotion(Long id, String name) {
        MallPromotionDO promotion = new MallPromotionDO();
        promotion.setId(id);
        promotion.setPromotionName(name);
        promotion.setPromotionType(PromotionTypeEnum.FULL_REDUCE.getCode());
        promotion.setStartTime(LocalDateTime.now().minusDays(1));
        promotion.setEndTime(LocalDateTime.now().plusDays(1));
        promotion.setIsDeleted(0);
        return promotion;
    }

    /** 构造非互斥满减规则 */
    private MallPromotionRuleDO priceRule(Long promotionId, Long ruleId, Long threshold, Long benefitAmount) {
        MallPromotionRuleDO rule = new MallPromotionRuleDO();
        rule.setId(ruleId);
        rule.setPromotionId(promotionId);
        rule.setRuleType(RuleTypeEnum.FULL_REDUCE.getCode());
        rule.setThresholdAmount(threshold);
        rule.setBenefitAmount(benefitAmount);
        rule.setIsExclusive(0);
        rule.setSortOrder(1);
        rule.setIsDeleted(0);
        return rule;
    }

    // ═══════════════════════════════════════════════════════════
    // 基础与边界
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("基础与边界")
    class Basics {

        @Test
        @DisplayName("无可用券、无促销：应付等于原价，两个优惠额均为 0")
        void noDiscountAtAll() {
            when(couponRecordMapper.selectUserRecords(eq(USER_ID),
                    eq(CouponRecordStatusEnum.AVAILABLE.getCode()), anyInt())).thenReturn(List.of());

            CalculationResp resp = calculationService.calculate(req(USER_ID, null, TOTAL_AMOUNT));

            assertThat(resp.getOriginalAmount()).isEqualTo(TOTAL_AMOUNT);
            assertThat(resp.getCouponDiscount()).isZero();
            assertThat(resp.getPromotionDiscount()).isZero();
            assertThat(resp.getFinalAmount()).isEqualTo(TOTAL_AMOUNT);
            assertThat(resp.getAppliedCoupons()).isEmpty();
            assertThat(resp.getAppliedPromotions()).isEmpty();
        }

        @Test
        @DisplayName("商品明细为空：原价与应付均为 0")
        void emptyItems() {
            when(couponRecordMapper.selectUserRecords(eq(USER_ID),
                    eq(CouponRecordStatusEnum.AVAILABLE.getCode()), anyInt())).thenReturn(List.of());
            CalculationReq req = new CalculationReq();
            req.setUserId(USER_ID);
            req.setItems(List.of());

            CalculationResp resp = calculationService.calculate(req);

            assertThat(resp.getOriginalAmount()).isZero();
            assertThat(resp.getFinalAmount()).isZero();
        }

        @Test
        @DisplayName("多商品按 单价×数量 累加原价")
        void multipleItems() {
            when(couponRecordMapper.selectUserRecords(eq(USER_ID),
                    eq(CouponRecordStatusEnum.AVAILABLE.getCode()), anyInt())).thenReturn(List.of());
            CalculationReq req = new CalculationReq();
            req.setUserId(USER_ID);
            req.setItems(List.of(new CalculationItem(101L, 1000L, 3),
                    new CalculationItem(102L, 500L, 2)));

            assertThat(calculationService.calculate(req).getOriginalAmount()).isEqualTo(4000L);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 券：自动择优（一笔订单最多 1 张）
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("券自动择优")
    class AutoSelectCoupon {

        @Test
        @DisplayName("多张可用券时只应用优惠最大的一张")
        void pickBestSingleCoupon() {
            when(couponRecordMapper.selectUserRecords(eq(USER_ID),
                    eq(CouponRecordStatusEnum.AVAILABLE.getCode()), anyInt()))
                    .thenReturn(List.of(availableRecord(501L, 10L, 1000L),
                            availableRecord(502L, 11L, 3000L)));
            when(couponMapper.selectByIdsNotDeleted(any()))
                    .thenReturn(List.of(reduceCoupon(10L, "满减券 A", 1000L, 0L),
                            reduceCoupon(11L, "满减券 B", 3000L, 0L)));

            CalculationResp resp = calculationService.calculate(req(USER_ID, null, TOTAL_AMOUNT));

            assertThat(resp.getCouponDiscount()).isEqualTo(3000L);
            // 单一券口径：命中明细最多一条
            assertThat(resp.getAppliedCoupons()).hasSize(1);
            assertThat(resp.getAppliedCoupons().get(0).getCouponRecordId()).isEqualTo(502L);
            assertThat(resp.getAppliedCoupons().get(0).getCouponName()).isEqualTo("满减券 B");
        }

        @Test
        @DisplayName("门槛未达到的券被剔除，取剩下里最优的一张")
        void skipCouponBelowThreshold() {
            when(couponRecordMapper.selectUserRecords(eq(USER_ID),
                    eq(CouponRecordStatusEnum.AVAILABLE.getCode()), anyInt()))
                    .thenReturn(List.of(availableRecord(501L, 10L, 5000L),
                            availableRecord(502L, 11L, 500L)));
            when(couponMapper.selectByIdsNotDeleted(any()))
                    .thenReturn(List.of(reduceCoupon(10L, "满 500 减 50", 5000L, 50000L),
                            reduceCoupon(11L, "满 100 减 5", 500L, 10000L)));

            CalculationResp resp = calculationService.calculate(req(USER_ID, null, TOTAL_AMOUNT));

            assertThat(resp.getCouponDiscount()).isEqualTo(500L);
            assertThat(resp.getAppliedCoupons().get(0).getCouponRecordId()).isEqualTo(502L);
        }

        @Test
        @DisplayName("已过期的券记录被跳过")
        void skipExpiredRecord() {
            when(couponRecordMapper.selectUserRecords(eq(USER_ID),
                    eq(CouponRecordStatusEnum.AVAILABLE.getCode()), anyInt()))
                    .thenReturn(List.of(
                            record(501L, 10L, CouponRecordStatusEnum.AVAILABLE, 9000L,
                                    LocalDateTime.now().minusMinutes(1)),
                            availableRecord(502L, 11L, 500L)));
            when(couponMapper.selectByIdsNotDeleted(any()))
                    .thenReturn(List.of(reduceCoupon(10L, "已过期大额券", 9000L, 0L),
                            reduceCoupon(11L, "有效小额券", 500L, 0L)));

            assertThat(calculationService.calculate(req(USER_ID, null, TOTAL_AMOUNT))
                    .getCouponDiscount()).isEqualTo(500L);
        }

        @Test
        @DisplayName("没有任何券满足门槛：不应用优惠券")
        void noCouponQualifies() {
            when(couponRecordMapper.selectUserRecords(eq(USER_ID),
                    eq(CouponRecordStatusEnum.AVAILABLE.getCode()), anyInt()))
                    .thenReturn(List.of(availableRecord(501L, 10L, 5000L)));
            when(couponMapper.selectByIdsNotDeleted(any()))
                    .thenReturn(List.of(reduceCoupon(10L, "满 500 减 50", 5000L, 50000L)));

            CalculationResp resp = calculationService.calculate(req(USER_ID, null, TOTAL_AMOUNT));

            assertThat(resp.getCouponDiscount()).isZero();
            assertThat(resp.getAppliedCoupons()).isEmpty();
        }

        @Test
        @DisplayName("面值取券记录的领取快照，而非券定义的当前值")
        void useSnapshotFaceValue() {
            when(couponRecordMapper.selectUserRecords(eq(USER_ID),
                    eq(CouponRecordStatusEnum.AVAILABLE.getCode()), anyInt()))
                    .thenReturn(List.of(availableRecord(501L, 10L, 1000L)));
            // 券定义的面值已被改成 9999，但记录快照是 1000
            when(couponMapper.selectByIdsNotDeleted(any()))
                    .thenReturn(List.of(reduceCoupon(10L, "券", 9999L, 0L)));

            assertThat(calculationService.calculate(req(USER_ID, null, TOTAL_AMOUNT))
                    .getCouponDiscount()).isEqualTo(1000L);
        }

        @Test
        @DisplayName("券定义已不存在时跳过该券，不抛异常")
        void skipRecordWithoutCouponDef() {
            when(couponRecordMapper.selectUserRecords(eq(USER_ID),
                    eq(CouponRecordStatusEnum.AVAILABLE.getCode()), anyInt()))
                    .thenReturn(List.of(availableRecord(501L, 10L, 1000L)));
            when(couponMapper.selectByIdsNotDeleted(any())).thenReturn(List.of());

            assertThat(calculationService.calculate(req(USER_ID, null, TOTAL_AMOUNT))
                    .getCouponDiscount()).isZero();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 券：折扣券计算
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("折扣券计算")
    class DiscountCouponCalc {

        @Test
        @DisplayName("按折扣率计算优惠：8.5 折 → 优惠 15%")
        void computeByRate() {
            when(couponRecordMapper.selectUserRecords(eq(USER_ID),
                    eq(CouponRecordStatusEnum.AVAILABLE.getCode()), anyInt()))
                    .thenReturn(List.of(availableRecord(501L, 10L, 0L)));
            when(couponMapper.selectByIdsNotDeleted(any()))
                    .thenReturn(List.of(discountCoupon(10L, "8.5 折券", 85, null)));

            assertThat(calculationService.calculate(req(USER_ID, null, TOTAL_AMOUNT))
                    .getCouponDiscount()).isEqualTo(1500L);
        }

        @Test
        @DisplayName("折扣优惠受 discount_limit 封顶")
        void respectDiscountLimit() {
            when(couponRecordMapper.selectUserRecords(eq(USER_ID),
                    eq(CouponRecordStatusEnum.AVAILABLE.getCode()), anyInt()))
                    .thenReturn(List.of(availableRecord(501L, 10L, 0L)));
            // 5 折本应优惠 5000，但上限 2000
            when(couponMapper.selectByIdsNotDeleted(any()))
                    .thenReturn(List.of(discountCoupon(10L, "5 折券", 50, 2000L)));

            assertThat(calculationService.calculate(req(USER_ID, null, TOTAL_AMOUNT))
                    .getCouponDiscount()).isEqualTo(2000L);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 券：指定券（含归属校验）
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("指定优惠券")
    class SpecifiedCoupon {

        @Test
        @DisplayName("指定券时只算这一张，不再查询用户可用券列表")
        void useSpecifiedCouponOnly() {
            when(couponRecordMapper.selectByIdNotDeleted(RECORD_ID))
                    .thenReturn(availableRecord(RECORD_ID, COUPON_ID, 2000L));
            when(couponMapper.selectByIdNotDeleted(COUPON_ID))
                    .thenReturn(reduceCoupon(COUPON_ID, "指定券", 2000L, 0L));

            CalculationResp resp = calculationService.calculate(req(USER_ID, RECORD_ID, TOTAL_AMOUNT));

            assertThat(resp.getCouponDiscount()).isEqualTo(2000L);
            assertThat(resp.getAppliedCoupons()).hasSize(1);
            verify(couponRecordMapper, never()).selectUserRecords(any(), any(), anyInt());
        }

        @Test
        @DisplayName("🔴 指定他人的券 → A0501（防越权用券）")
        void specifiedCouponOfOtherUser() {
            MallCouponRecordDO others = availableRecord(RECORD_ID, COUPON_ID, 2000L);
            others.setUserId(OTHER_USER_ID);
            when(couponRecordMapper.selectByIdNotDeleted(RECORD_ID)).thenReturn(others);

            assertErrorCode(ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    () -> calculationService.calculate(req(USER_ID, RECORD_ID, TOTAL_AMOUNT)));
        }

        @Test
        @DisplayName("指定的券不存在 → A0501")
        void specifiedCouponNotFound() {
            when(couponRecordMapper.selectByIdNotDeleted(RECORD_ID)).thenReturn(null);

            assertErrorCode(ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    () -> calculationService.calculate(req(USER_ID, RECORD_ID, TOTAL_AMOUNT)));
        }

        @Test
        @DisplayName("🔴 指定券但门槛不满足 → A0612，而不是返回 0 折扣")
        void specifiedCouponBelowThreshold() {
            when(couponRecordMapper.selectByIdNotDeleted(RECORD_ID))
                    .thenReturn(availableRecord(RECORD_ID, COUPON_ID, 5000L));
            when(couponMapper.selectByIdNotDeleted(COUPON_ID))
                    .thenReturn(reduceCoupon(COUPON_ID, "满 500 减 50", 5000L, 50000L));

            assertErrorCode(ErrorCode.COUPON_CONDITION_NOT_MET.getCode(),
                    () -> calculationService.calculate(req(USER_ID, RECORD_ID, TOTAL_AMOUNT)));
        }

        @Test
        @DisplayName("指定的券已使用 → A0613")
        void specifiedCouponAlreadyUsed() {
            when(couponRecordMapper.selectByIdNotDeleted(RECORD_ID))
                    .thenReturn(record(RECORD_ID, COUPON_ID, CouponRecordStatusEnum.USED, 2000L,
                            LocalDateTime.now().plusDays(30)));

            assertErrorCode(ErrorCode.COUPON_USED.getCode(),
                    () -> calculationService.calculate(req(USER_ID, RECORD_ID, TOTAL_AMOUNT)));
        }

        @Test
        @DisplayName("指定的券已过期 → A0610")
        void specifiedCouponExpired() {
            when(couponRecordMapper.selectByIdNotDeleted(RECORD_ID))
                    .thenReturn(record(RECORD_ID, COUPON_ID, CouponRecordStatusEnum.AVAILABLE, 2000L,
                            LocalDateTime.now().minusMinutes(1)));

            assertErrorCode(ErrorCode.COUPON_EXPIRED.getCode(),
                    () -> calculationService.calculate(req(USER_ID, RECORD_ID, TOTAL_AMOUNT)));
        }

        @Test
        @DisplayName("指定的券已被其他订单锁定 → A0612")
        void specifiedCouponLocked() {
            when(couponRecordMapper.selectByIdNotDeleted(RECORD_ID))
                    .thenReturn(record(RECORD_ID, COUPON_ID, CouponRecordStatusEnum.LOCKED, 2000L,
                            LocalDateTime.now().plusDays(30)));

            assertErrorCode(ErrorCode.COUPON_CONDITION_NOT_MET.getCode(),
                    () -> calculationService.calculate(req(USER_ID, RECORD_ID, TOTAL_AMOUNT)));
        }

        @Test
        @DisplayName("指定的券对应定义已被删除 → A0612")
        void specifiedCouponDefMissing() {
            when(couponRecordMapper.selectByIdNotDeleted(RECORD_ID))
                    .thenReturn(availableRecord(RECORD_ID, COUPON_ID, 2000L));
            when(couponMapper.selectByIdNotDeleted(COUPON_ID)).thenReturn(null);

            assertErrorCode(ErrorCode.COUPON_CONDITION_NOT_MET.getCode(),
                    () -> calculationService.calculate(req(USER_ID, RECORD_ID, TOTAL_AMOUNT)));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 促销叠加与金额截断
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("促销叠加与金额截断")
    class PromotionAndTruncation {

        @Test
        @DisplayName("券优惠与促销优惠可叠加，应付 = 原价 − 券 − 促销")
        void couponAndPromotionStack() {
            when(couponRecordMapper.selectByIdNotDeleted(RECORD_ID))
                    .thenReturn(availableRecord(RECORD_ID, COUPON_ID, 1000L));
            when(couponMapper.selectByIdNotDeleted(COUPON_ID))
                    .thenReturn(reduceCoupon(COUPON_ID, "指定券", 1000L, 0L));
            when(promotionMapper.selectActive(any(LocalDateTime.class), anyInt()))
                    .thenReturn(List.of(promotion(1L, "满 100 减 30")));
            when(promotionRuleMapper.selectByPromotionIds(any()))
                    .thenReturn(List.of(priceRule(1L, 11L, 10000L, 3000L)));

            CalculationResp resp = calculationService.calculate(req(USER_ID, RECORD_ID, TOTAL_AMOUNT));

            assertThat(resp.getOriginalAmount()).isEqualTo(TOTAL_AMOUNT);
            assertThat(resp.getCouponDiscount()).isEqualTo(1000L);
            assertThat(resp.getPromotionDiscount()).isEqualTo(3000L);
            assertThat(resp.getFinalAmount()).isEqualTo(6000L);
            assertThat(resp.getAppliedPromotions()).hasSize(1);
        }

        @Test
        @DisplayName("促销门槛按订单原价判定，不受券优惠影响")
        void promotionThresholdUsesOriginalAmount() {
            // 券刻意取小额（1000），使剩余额度 9000 > 促销 3000，避免被截断掩盖判定口径。
            // 若门槛按「券后金额 9000」判定，则规则门槛 10000 不满足 → 促销为 0；
            // 只有按「订单原价 10000」判定才会命中 3000，故该用例能真正区分两种口径。
            when(couponRecordMapper.selectByIdNotDeleted(RECORD_ID))
                    .thenReturn(availableRecord(RECORD_ID, COUPON_ID, 1000L));
            when(couponMapper.selectByIdNotDeleted(COUPON_ID))
                    .thenReturn(reduceCoupon(COUPON_ID, "指定券", 1000L, 0L));
            when(promotionMapper.selectActive(any(LocalDateTime.class), anyInt()))
                    .thenReturn(List.of(promotion(1L, "满 100 减 30")));
            when(promotionRuleMapper.selectByPromotionIds(any()))
                    .thenReturn(List.of(priceRule(1L, 11L, 10000L, 3000L)));

            CalculationResp resp = calculationService.calculate(req(USER_ID, RECORD_ID, TOTAL_AMOUNT));

            assertThat(resp.getCouponDiscount()).isEqualTo(1000L);
            assertThat(resp.getPromotionDiscount()).isEqualTo(3000L);
            assertThat(resp.getFinalAmount()).isEqualTo(6000L);
        }

        @Test
        @DisplayName("券优惠不超过原价")
        void couponDiscountCappedByTotal() {
            when(couponRecordMapper.selectByIdNotDeleted(RECORD_ID))
                    .thenReturn(availableRecord(RECORD_ID, COUPON_ID, 20000L));
            when(couponMapper.selectByIdNotDeleted(COUPON_ID))
                    .thenReturn(reduceCoupon(COUPON_ID, "大额券", 20000L, 0L));

            CalculationResp resp = calculationService.calculate(req(USER_ID, RECORD_ID, TOTAL_AMOUNT));

            assertThat(resp.getCouponDiscount()).isEqualTo(TOTAL_AMOUNT);
            assertThat(resp.getFinalAmount()).isZero();
        }

        @Test
        @DisplayName("券 + 促销超过原价时，促销部分被截断，应付不为负")
        void finalAmountNeverNegative() {
            when(couponRecordMapper.selectByIdNotDeleted(RECORD_ID))
                    .thenReturn(availableRecord(RECORD_ID, COUPON_ID, 8000L));
            when(couponMapper.selectByIdNotDeleted(COUPON_ID))
                    .thenReturn(reduceCoupon(COUPON_ID, "大额券", 8000L, 0L));
            when(promotionMapper.selectActive(any(LocalDateTime.class), anyInt()))
                    .thenReturn(List.of(promotion(1L, "满 100 减 50")));
            when(promotionRuleMapper.selectByPromotionIds(any()))
                    .thenReturn(List.of(priceRule(1L, 11L, 10000L, 5000L)));

            CalculationResp resp = calculationService.calculate(req(USER_ID, RECORD_ID, TOTAL_AMOUNT));

            assertThat(resp.getCouponDiscount()).isEqualTo(8000L);
            // 剩余 2000，促销 5000 被截断到 2000
            assertThat(resp.getPromotionDiscount()).isEqualTo(2000L);
            assertThat(resp.getFinalAmount()).isZero();
        }

        @Test
        @DisplayName("指定券时也仍会计算促销优惠")
        void promotionStillEvaluatedWithSpecifiedCoupon() {
            when(couponRecordMapper.selectByIdNotDeleted(RECORD_ID))
                    .thenReturn(availableRecord(RECORD_ID, COUPON_ID, 1000L));
            when(couponMapper.selectByIdNotDeleted(COUPON_ID))
                    .thenReturn(reduceCoupon(COUPON_ID, "指定券", 1000L, 0L));
            when(promotionMapper.selectActive(any(LocalDateTime.class), anyInt()))
                    .thenReturn(List.of(promotion(1L, "未达门槛活动")));
            when(promotionRuleMapper.selectByPromotionIds(any()))
                    .thenReturn(List.of(priceRule(1L, 11L, 99999L, 3000L)));

            CalculationResp resp = calculationService.calculate(req(USER_ID, RECORD_ID, TOTAL_AMOUNT));

            assertThat(resp.getPromotionDiscount()).isZero();
            assertThat(resp.getAppliedPromotions()).isEmpty();
            assertThat(resp.getFinalAmount()).isEqualTo(9000L);
        }
    }
}
