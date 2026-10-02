package com.mall.marketing.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mall.api.feign.RemoteMarketingService.CalculationResp.AppliedPromotion;
import com.mall.common.enums.marketing.PromotionTypeEnum;
import com.mall.common.enums.marketing.RuleTypeEnum;
import com.mall.marketing.DO.MallPromotionDO;
import com.mall.marketing.DO.MallPromotionRuleDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 促销规则匹配引擎单元测试
 *
 * <p>纯逻辑测试，用真实的 Caffeine 缓存实例（便于验证命中行为）。
 * 覆盖设计文档 {@code docs/design/14_mall-marketing详细设计.md} §3.5 与 §3.4 第 5 步的
 * 「互斥取最大、非互斥可叠加」。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
class PromotionRuleMatcherTest {

    private static final long TOTAL_AMOUNT = 10000L;

    private Cache<String, List<AppliedPromotion>> cache;

    private PromotionRuleMatcher matcher;

    @BeforeEach
    void setUp() {
        cache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(Duration.ofMinutes(1))
                .build();
        matcher = new PromotionRuleMatcher(cache);
    }

    // ═══════════════════════════════════════════════════════════
    // 测试数据构造
    // ═══════════════════════════════════════════════════════════

    /** 构造一个进行中的活动 */
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

    /** 构造一条满减规则 */
    private MallPromotionRuleDO reduceRule(Long promotionId, Long ruleId, Long threshold,
                                           Long benefitAmount, Integer exclusive, Integer sortOrder) {
        MallPromotionRuleDO rule = new MallPromotionRuleDO();
        rule.setId(ruleId);
        rule.setPromotionId(promotionId);
        rule.setRuleType(RuleTypeEnum.FULL_REDUCE.getCode());
        rule.setThresholdAmount(threshold);
        rule.setBenefitAmount(benefitAmount);
        rule.setIsExclusive(exclusive);
        rule.setSortOrder(sortOrder);
        rule.setIsDeleted(0);
        return rule;
    }

    /** 构造一条满折规则 */
    private MallPromotionRuleDO discountRule(Long promotionId, Long ruleId, Long threshold,
                                             Integer benefitRate, Integer exclusive, Integer sortOrder) {
        MallPromotionRuleDO rule = new MallPromotionRuleDO();
        rule.setId(ruleId);
        rule.setPromotionId(promotionId);
        rule.setRuleType(RuleTypeEnum.FULL_DISCOUNT.getCode());
        rule.setThresholdAmount(threshold);
        rule.setBenefitRate(benefitRate);
        rule.setIsExclusive(exclusive);
        rule.setSortOrder(sortOrder);
        rule.setIsDeleted(0);
        return rule;
    }

    /** 构造一条免邮规则 */
    private MallPromotionRuleDO freeShippingRule(Long promotionId, Long ruleId, Long threshold,
                                                 Integer exclusive, Integer sortOrder) {
        MallPromotionRuleDO rule = new MallPromotionRuleDO();
        rule.setId(ruleId);
        rule.setPromotionId(promotionId);
        rule.setRuleType(RuleTypeEnum.FREE_SHIPPING.getCode());
        rule.setThresholdAmount(threshold);
        rule.setIsExclusive(exclusive);
        rule.setSortOrder(sortOrder);
        rule.setIsDeleted(0);
        return rule;
    }

    // ═══════════════════════════════════════════════════════════
    // 命中与门槛
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("命中判定与门槛")
    class Matching {

        @Test
        @DisplayName("满减规则：门槛满足则命中，优惠等于 benefitAmount")
        void reduceRuleHit() {
            List<AppliedPromotion> result = matcher.match(
                    List.of(promotion(1L, "满 100 减 30")),
                    List.of(reduceRule(1L, 11L, 10000L, 3000L, 0, 1)),
                    TOTAL_AMOUNT);

            assertThat(result).hasSize(1);
            AppliedPromotion applied = result.get(0);
            assertThat(applied.getPromotionId()).isEqualTo(1L);
            assertThat(applied.getPromotionName()).isEqualTo("满 100 减 30");
            assertThat(applied.getRuleId()).isEqualTo(11L);
            assertThat(applied.getDiscountAmount()).isEqualTo(3000L);
        }

        @Test
        @DisplayName("门槛未达到则不命中")
        void thresholdNotMet() {
            assertThat(matcher.match(
                    List.of(promotion(1L, "满 500 减 30")),
                    List.of(reduceRule(1L, 11L, 50000L, 3000L, 0, 1)),
                    TOTAL_AMOUNT)).isEmpty();
        }

        @Test
        @DisplayName("门槛为 null 视为无门槛，命中")
        void nullThresholdMeansNoThreshold() {
            assertThat(matcher.match(
                    List.of(promotion(1L, "无门槛减 5")),
                    List.of(reduceRule(1L, 11L, null, 500L, 0, 1)),
                    TOTAL_AMOUNT)).hasSize(1);
        }

        @Test
        @DisplayName("满折规则：优惠 = 原价 ×(100 − 折扣率)/100")
        void discountRuleHit() {
            // 8.5 折 → 优惠 15%
            List<AppliedPromotion> result = matcher.match(
                    List.of(promotion(1L, "满 100 享 8.5 折")),
                    List.of(discountRule(1L, 11L, 10000L, 85, 0, 1)),
                    TOTAL_AMOUNT);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDiscountAmount()).isEqualTo(1500L);
        }

        @Test
        @DisplayName("满折规则折扣率为 null 时优惠记 0，不抛异常")
        void discountRuleWithNullRate() {
            List<AppliedPromotion> result = matcher.match(
                    List.of(promotion(1L, "异常折扣")),
                    List.of(discountRule(1L, 11L, 10000L, null, 0, 1)),
                    TOTAL_AMOUNT);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDiscountAmount()).isZero();
        }

        @Test
        @DisplayName("免邮规则：命中但优惠金额记 0（运费不在试算金额口径内）")
        void freeShippingRuleContributesZero() {
            List<AppliedPromotion> result = matcher.match(
                    List.of(promotion(1L, "满 100 包邮")),
                    List.of(freeShippingRule(1L, 11L, 10000L, 0, 1)),
                    TOTAL_AMOUNT);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDiscountAmount()).isZero();
        }

        @Test
        @DisplayName("规则所属活动不在候选列表中则忽略")
        void ruleOfUnknownPromotionIgnored() {
            assertThat(matcher.match(
                    List.of(promotion(1L, "活动一")),
                    List.of(reduceRule(2L, 21L, 10000L, 3000L, 0, 1)),
                    TOTAL_AMOUNT)).isEmpty();
        }

        @Test
        @DisplayName("benefitAmount 为 null 的满减规则：命中但优惠记 0")
        void reduceRuleWithNullBenefit() {
            List<AppliedPromotion> result = matcher.match(
                    List.of(promotion(1L, "异常满减")),
                    List.of(reduceRule(1L, 11L, 10000L, null, 0, 1)),
                    TOTAL_AMOUNT);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDiscountAmount()).isZero();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 互斥与叠加
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("互斥与叠加")
    class ExclusiveAndStacking {

        @Test
        @DisplayName("多条非互斥规则同时命中：全部保留（可叠加）")
        void nonExclusiveRulesStack() {
            List<AppliedPromotion> result = matcher.match(
                    List.of(promotion(1L, "活动一"), promotion(2L, "活动二")),
                    List.of(reduceRule(1L, 11L, 10000L, 1000L, 0, 1),
                            reduceRule(2L, 21L, 10000L, 2000L, 0, 1)),
                    TOTAL_AMOUNT);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(AppliedPromotion::getDiscountAmount)
                    .containsExactly(1000L, 2000L);
        }

        @Test
        @DisplayName("多条互斥规则同时命中：只保留优惠最大的一条")
        void exclusiveRulesKeepLargest() {
            List<AppliedPromotion> result = matcher.match(
                    List.of(promotion(1L, "活动一"), promotion(2L, "活动二")),
                    List.of(reduceRule(1L, 11L, 10000L, 1000L, 1, 1),
                            reduceRule(2L, 21L, 10000L, 2500L, 1, 2)),
                    TOTAL_AMOUNT);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRuleId()).isEqualTo(21L);
            assertThat(result.get(0).getDiscountAmount()).isEqualTo(2500L);
        }

        @Test
        @DisplayName("互斥规则中优惠更大的在输入中靠后，仍应胜出")
        void exclusiveKeepsLargestRegardlessOfOrder() {
            List<AppliedPromotion> result = matcher.match(
                    List.of(promotion(1L, "活动一"), promotion(2L, "活动二")),
                    List.of(reduceRule(1L, 11L, 10000L, 2500L, 1, 1),
                            reduceRule(2L, 21L, 10000L, 1000L, 1, 2)),
                    TOTAL_AMOUNT);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDiscountAmount()).isEqualTo(2500L);
        }

        @Test
        @DisplayName("互斥规则优惠并列时，取 sort_order 更小者，保证结果稳定")
        void exclusiveTieBreaksBySortOrder() {
            List<AppliedPromotion> result = matcher.match(
                    List.of(promotion(1L, "活动一"), promotion(2L, "活动二")),
                    List.of(reduceRule(1L, 11L, 10000L, 1500L, 1, 5),
                            reduceRule(2L, 21L, 10000L, 1500L, 1, 2)),
                    TOTAL_AMOUNT);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRuleId()).isEqualTo(21L);
        }

        @Test
        @DisplayName("互斥与非互斥混合：非互斥全部叠加，互斥只留最大")
        void mixedExclusiveAndNonExclusive() {
            List<AppliedPromotion> result = matcher.match(
                    List.of(promotion(1L, "活动一"), promotion(2L, "活动二"), promotion(3L, "活动三")),
                    List.of(reduceRule(1L, 11L, 10000L, 1000L, 0, 1),
                            reduceRule(2L, 21L, 10000L, 9000L, 1, 2),
                            reduceRule(3L, 31L, 10000L, 3000L, 1, 3)),
                    TOTAL_AMOUNT);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(AppliedPromotion::getRuleId).containsExactly(11L, 21L);
            assertThat(result).extracting(AppliedPromotion::getDiscountAmount)
                    .containsExactly(1000L, 9000L);
        }

        @Test
        @DisplayName("未命中的规则不参与互斥择优")
        void unmatchedExclusiveRuleIsIgnored() {
            List<AppliedPromotion> result = matcher.match(
                    List.of(promotion(1L, "活动一"), promotion(2L, "活动二")),
                    List.of(reduceRule(1L, 11L, 10000L, 1000L, 1, 1),
                            // 门槛 50000 未达到，虽然优惠更大也不该胜出
                            reduceRule(2L, 21L, 50000L, 9000L, 1, 2)),
                    TOTAL_AMOUNT);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRuleId()).isEqualTo(11L);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 结果顺序与缓存
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("结果顺序与缓存")
    class OrderAndCache {

        @Test
        @DisplayName("输出保持输入规则顺序，结果稳定可预期")
        void resultKeepsInputOrder() {
            List<AppliedPromotion> result = matcher.match(
                    List.of(promotion(1L, "活动一"), promotion(2L, "活动二")),
                    List.of(reduceRule(2L, 21L, 10000L, 2000L, 0, 1),
                            reduceRule(1L, 11L, 10000L, 1000L, 0, 2)),
                    TOTAL_AMOUNT);

            assertThat(result).extracting(AppliedPromotion::getRuleId).containsExactly(21L, 11L);
        }

        @Test
        @DisplayName("同一「活动集合 + 订单金额」第二次调用命中缓存，不再重算")
        void sameInputHitsCache() {
            List<MallPromotionDO> promotions = List.of(promotion(1L, "活动一"));
            List<MallPromotionRuleDO> rules = List.of(reduceRule(1L, 11L, 10000L, 3000L, 0, 1));

            List<AppliedPromotion> first = matcher.match(promotions, rules, TOTAL_AMOUNT);
            List<AppliedPromotion> second = matcher.match(promotions, rules, TOTAL_AMOUNT);

            assertThat(cache.estimatedSize()).isEqualTo(1);
            assertThat(second).isEqualTo(first);
        }

        @Test
        @DisplayName("订单金额不同 → 缓存键不同，分别缓存")
        void differentAmountUsesDifferentCacheKey() {
            List<MallPromotionDO> promotions = List.of(promotion(1L, "活动一"));
            List<MallPromotionRuleDO> rules = List.of(reduceRule(1L, 11L, 10000L, 3000L, 0, 1));

            matcher.match(promotions, rules, 10000L);
            matcher.match(promotions, rules, 20000L);

            assertThat(cache.estimatedSize()).isEqualTo(2);
        }

        @Test
        @DisplayName("活动集合变化 → 缓存键不同，不会读到旧活动的匹配结果")
        void changedPromotionSetUsesDifferentCacheKey() {
            List<MallPromotionRuleDO> rules = List.of(
                    reduceRule(1L, 11L, 10000L, 1000L, 0, 1),
                    reduceRule(2L, 21L, 10000L, 2000L, 0, 1));

            List<AppliedPromotion> onePromotion = matcher.match(
                    List.of(promotion(1L, "活动一")), rules, TOTAL_AMOUNT);
            List<AppliedPromotion> twoPromotions = matcher.match(
                    List.of(promotion(1L, "活动一"), promotion(2L, "活动二")), rules, TOTAL_AMOUNT);

            assertThat(onePromotion).hasSize(1);
            assertThat(twoPromotions).hasSize(2);
            assertThat(cache.estimatedSize()).isEqualTo(2);
        }
    }
}
