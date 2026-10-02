package com.mall.marketing.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.mall.api.feign.RemoteMarketingService.CalculationResp.AppliedPromotion;
import com.mall.common.enums.marketing.RuleTypeEnum;
import com.mall.marketing.DO.MallPromotionDO;
import com.mall.marketing.DO.MallPromotionRuleDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 促销规则匹配引擎
 *
 * <p>对应设计文档 {@code docs/design/14_mall-marketing详细设计.md} §3.5「互斥取最大、
 * 非互斥可叠加」：先按规则输入顺序逐条判定门槛与优惠额，再在命中的互斥规则中择优保留一条，
 * 非互斥规则全部保留，最终输出仍保持规则输入顺序。</p>
 *
 * <p>结果按「订单金额 + 活动 ID 集合」缓存于注入的 Caffeine 实例，TTL 内不重复计算。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionRuleMatcher {

    /** 折扣率百分比基数：{@code benefit_rate=85} 表示以 100 为满分的 8.5 折 */
    private static final int RATE_BASE = 100;

    /** 规则互斥标志：1=互斥 */
    private static final int EXCLUSIVE_FLAG = 1;

    /** 排序值缺失时的兜底值，保证并列择优结果稳定 */
    private static final int DEFAULT_SORT_ORDER = Integer.MAX_VALUE;

    /** 促销规则匹配结果本地缓存（key = 订单金额 + 活动 ID 集合） */
    private final Cache<String, List<AppliedPromotion>> promotionMatchCache;

    /**
     * 计算一组活动在给定订单金额下命中的优惠明细
     *
     * @param promotions  候选活动（调用方已按「进行中」过滤）
     * @param rules       候选规则（含未命中的）
     * @param totalAmount 订单总原价（单位：分）
     * @return 命中的优惠明细（保持规则输入顺序，可能为空列表）
     */
    public List<AppliedPromotion> match(List<MallPromotionDO> promotions,
                                       List<MallPromotionRuleDO> rules,
                                       long totalAmount) {
        String cacheKey = buildCacheKey(promotions, totalAmount);
        List<AppliedPromotion> cached = promotionMatchCache.get(cacheKey,
                key -> doMatch(promotions, rules, totalAmount));
        return cached == null ? List.of() : cached;
    }

    /**
     * 构造缓存键
     *
     * <p>键同时包含订单金额与活动 ID 集合：金额不同或活动集合变化（上下线）都必须分开缓存，
     * 避免读到其它金额或其它活动组合的匹配结果。</p>
     *
     * @param promotions  候选活动
     * @param totalAmount 订单总原价（单位：分）
     * @return 缓存键，形如 {@code "10000:1,2"}
     */
    private String buildCacheKey(List<MallPromotionDO> promotions, long totalAmount) {
        String promotionIds = "";
        if (promotions != null && !promotions.isEmpty()) {
            promotionIds = promotions.stream()
                    .filter(Objects::nonNull)
                    .map(MallPromotionDO::getId)
                    .filter(Objects::nonNull)
                    .sorted()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
        }
        return totalAmount + ":" + promotionIds;
    }

    /**
     * 执行实际的匹配计算（缓存未命中时调用）
     *
     * @param promotions  候选活动
     * @param rules       候选规则
     * @param totalAmount 订单总原价（单位：分）
     * @return 命中的优惠明细（保持规则输入顺序）
     */
    private List<AppliedPromotion> doMatch(List<MallPromotionDO> promotions,
                                           List<MallPromotionRuleDO> rules,
                                           long totalAmount) {
        if (promotions == null || promotions.isEmpty() || rules == null || rules.isEmpty()) {
            return List.of();
        }

        // 1. 建立活动 ID → 活动 索引；所属活动不在候选列表中的规则直接忽略
        Map<Long, MallPromotionDO> promotionMap = new HashMap<>(promotions.size());
        for (MallPromotionDO promotion : promotions) {
            if (promotion != null && promotion.getId() != null) {
                promotionMap.put(promotion.getId(), promotion);
            }
        }

        // 2. 按规则输入顺序逐条判定门槛，收集命中的规则及其优惠额
        List<MatchedRule> matchedRules = new ArrayList<>(rules.size());
        for (MallPromotionRuleDO rule : rules) {
            if (rule == null || !promotionMap.containsKey(rule.getPromotionId())) {
                continue;
            }
            if (!isThresholdMet(rule.getThresholdAmount(), totalAmount)) {
                continue;
            }
            matchedRules.add(new MatchedRule(rule, calcDiscount(rule, totalAmount)));
        }

        // 3. 互斥规则只保留优惠最大的一条（并列取 sort_order 更小者）
        MatchedRule bestExclusive = pickBestExclusive(matchedRules);

        // 4. 过滤掉落选的互斥规则，其余保持输入顺序输出
        List<AppliedPromotion> result = new ArrayList<>(matchedRules.size());
        for (MatchedRule matched : matchedRules) {
            if (isExclusive(matched.rule) && matched != bestExclusive) {
                continue;
            }
            MallPromotionDO promotion = promotionMap.get(matched.rule.getPromotionId());
            AppliedPromotion applied = new AppliedPromotion();
            applied.setPromotionId(matched.rule.getPromotionId());
            applied.setPromotionName(promotion == null ? null : promotion.getPromotionName());
            applied.setRuleId(matched.rule.getId());
            applied.setDiscountAmount(matched.discountAmount);
            result.add(applied);
        }

        log.debug("促销规则匹配完成: totalAmount={}, 候选规则数={}, 命中数={}, 结果数={}",
                totalAmount, rules.size(), matchedRules.size(), result.size());
        // 返回不可变副本：该列表会被 Caffeine 缓存并原样交给调用方，
        // 若返回可变列表，调用方任何改动都会污染缓存
        return List.copyOf(result);
    }

    /**
     * 判定规则门槛是否达到
     *
     * @param thresholdAmount 门槛金额（单位：分），可为 null 表示无门槛
     * @param totalAmount     订单总原价（单位：分）
     * @return 达到门槛返回 true
     */
    private boolean isThresholdMet(Long thresholdAmount, long totalAmount) {
        return thresholdAmount == null || totalAmount >= thresholdAmount;
    }

    /**
     * 判定规则是否互斥
     *
     * @param rule 促销规则
     * @return 互斥返回 true
     */
    private boolean isExclusive(MallPromotionRuleDO rule) {
        return rule.getIsExclusive() != null && rule.getIsExclusive() == EXCLUSIVE_FLAG;
    }

    /**
     * 计算单条规则的优惠金额（单位：分）
     *
     * <p>满减取 {@code benefit_amount}；满折按「原价 ×(100 − 折扣率)/100」整除；
     * 免邮不在试算金额口径内，恒为 0；关键字段缺失时按 0 处理，不抛异常。</p>
     *
     * @param rule        促销规则
     * @param totalAmount 订单总原价（单位：分）
     * @return 优惠金额（单位：分），最小为 0
     */
    private long calcDiscount(MallPromotionRuleDO rule, long totalAmount) {
        Integer ruleType = rule.getRuleType();
        if (ruleType == null) {
            return 0L;
        }
        if (ruleType == RuleTypeEnum.FULL_REDUCE.getCode()) {
            return rule.getBenefitAmount() == null ? 0L : rule.getBenefitAmount();
        }
        if (ruleType == RuleTypeEnum.FULL_DISCOUNT.getCode()) {
            Integer benefitRate = rule.getBenefitRate();
            return benefitRate == null ? 0L : totalAmount * (RATE_BASE - benefitRate) / RATE_BASE;
        }
        return 0L;
    }

    /**
     * 在命中的互斥规则中择优：优惠金额最大者胜出，并列时取 sort_order 更小者
     *
     * @param matchedRules 命中的规则列表（含互斥与非互斥）
     * @return 胜出的互斥规则，无互斥命中时返回 null
     */
    private MatchedRule pickBestExclusive(List<MatchedRule> matchedRules) {
        MatchedRule best = null;
        for (MatchedRule matched : matchedRules) {
            if (!isExclusive(matched.rule)) {
                continue;
            }
            if (best == null || isBetter(matched, best)) {
                best = matched;
            }
        }
        return best;
    }

    /**
     * 比较两条互斥规则的优劣
     *
     * @param candidate 候选规则
     * @param current   当前最优规则
     * @return 候选更优（优惠更大，或优惠并列但 sort_order 更小）返回 true
     */
    private boolean isBetter(MatchedRule candidate, MatchedRule current) {
        if (candidate.discountAmount != current.discountAmount) {
            return candidate.discountAmount > current.discountAmount;
        }
        return sortOrder(candidate.rule) < sortOrder(current.rule);
    }

    /**
     * 读取规则排序值，缺失时按兜底值处理
     *
     * @param rule 促销规则
     * @return 排序值，越小越优先
     */
    private int sortOrder(MallPromotionRuleDO rule) {
        return rule.getSortOrder() == null ? DEFAULT_SORT_ORDER : rule.getSortOrder();
    }

    /**
     * 命中规则的中间结果
     *
     * <p>同时保留规则本体（互斥标志、排序值）与算得的优惠额，便于择优后再还原输出。</p>
     */
    private static final class MatchedRule {

        /** 命中的规则 */
        private final MallPromotionRuleDO rule;

        /** 规则优惠金额（单位：分） */
        private final long discountAmount;

        /**
         * 构造命中规则的中间结果
         *
         * @param rule           命中的规则
         * @param discountAmount 规则优惠金额（单位：分）
         */
        private MatchedRule(MallPromotionRuleDO rule, long discountAmount) {
            this.rule = rule;
            this.discountAmount = discountAmount;
        }
    }
}
