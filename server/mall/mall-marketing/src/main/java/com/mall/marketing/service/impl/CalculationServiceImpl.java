package com.mall.marketing.service.impl;

import com.mall.api.feign.RemoteMarketingService.CalculationReq;
import com.mall.api.feign.RemoteMarketingService.CalculationReq.CalculationItem;
import com.mall.api.feign.RemoteMarketingService.CalculationResp;
import com.mall.api.feign.RemoteMarketingService.CalculationResp.AppliedCoupon;
import com.mall.api.feign.RemoteMarketingService.CalculationResp.AppliedPromotion;
import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.marketing.CouponRecordStatusEnum;
import com.mall.common.enums.marketing.CouponTypeEnum;
import com.mall.common.exception.BusinessException;
import com.mall.marketing.DO.MallCouponDO;
import com.mall.marketing.DO.MallCouponRecordDO;
import com.mall.marketing.DO.MallPromotionDO;
import com.mall.marketing.DO.MallPromotionRuleDO;
import com.mall.marketing.mapper.MallCouponMapper;
import com.mall.marketing.mapper.MallCouponRecordMapper;
import com.mall.marketing.mapper.MallPromotionMapper;
import com.mall.marketing.mapper.MallPromotionRuleMapper;
import com.mall.marketing.service.CalculationService;
import com.mall.marketing.service.PromotionRuleMatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 优惠试算服务实现
 *
 * <p>两阶段过滤 + 择优：先按订单原价过滤券与促销门槛，再在候选券中取优惠最大的一张，
 * 与促销优惠叠加得到应付金额。全程只读，不锁定任何资源，也不调远程服务。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CalculationServiceImpl implements CalculationService {

    /** 折扣率百分比基数：{@code discount_rate=85} 表示以 100 为满分的 8.5 折 */
    private static final int RATE_BASE = 100;

    /**
     * 自动择优时单次拉取的可用券记录上限
     *
     * <p><b>为何不用配置项 {@code mall.marketing.calculation.max-candidates}（20）</b>：
     * 该值是为设计 §3.4 的「2^N 组合搜索」做候选剪枝而设的——组合搜索下少算几张只是近似最优。
     * 但本模块经 2026-10-02 决策改为<b>单一券择优</b>（一笔订单最多 1 张券），
     * 此时必须<b>评估全部候选项</b>才能选出真正最优的一张；若沿用 20 的上限，
     * 持有 20 张以上可用券的用户会漏选（查询按领取时间倒序，靠后的券永远进不了候选）。
     * 故改用一个纯安全兜底上限，防止异常数据把内存打爆。</p>
     */
    private static final int CANDIDATE_COUPON_LIMIT = 200;

    /** 单次拉取的进行中活动上限 */
    private static final int ACTIVE_PROMOTION_LIMIT = 50;

    private final MallCouponRecordMapper couponRecordMapper;

    private final MallCouponMapper couponMapper;

    private final MallPromotionMapper promotionMapper;

    private final MallPromotionRuleMapper promotionRuleMapper;

    private final PromotionRuleMatcher promotionRuleMatcher;

    @Override
    public CalculationResp calculate(CalculationReq req) {
        // ① 订单原价 = Σ(单价 × 数量)
        long originalAmount = sumItems(req.getItems());

        // ② 券：指定券走严格校验（不可用即抛错）；未指定则在可用券中自动择优，一笔订单最多 1 张
        AppliedCoupon appliedCoupon = req.getCouponClaimId() == null
                ? autoSelectBestCoupon(req.getUserId(), originalAmount)
                : applySpecifiedCoupon(req.getUserId(), req.getCouponClaimId(), originalAmount);

        // ③ 促销：查进行中活动 + 规则，交由规则引擎匹配，可与券叠加
        List<AppliedPromotion> appliedPromotions = matchActivePromotions(originalAmount);

        // ④ 金额截断：券优惠不超过原价；促销优惠不超过「原价 − 券优惠」；应付不为负
        long couponDiscount = appliedCoupon == null ? 0L : appliedCoupon.getDiscountAmount();
        long promotionDiscount = Math.min(sumDiscounts(appliedPromotions),
                Math.max(0L, originalAmount - couponDiscount));
        long finalAmount = Math.max(0L, originalAmount - couponDiscount - promotionDiscount);

        CalculationResp resp = new CalculationResp();
        resp.setOriginalAmount(originalAmount);
        resp.setCouponDiscount(couponDiscount);
        resp.setPromotionDiscount(promotionDiscount);
        resp.setFinalAmount(finalAmount);
        resp.setAppliedCoupons(appliedCoupon == null ? List.of() : List.of(appliedCoupon));
        resp.setAppliedPromotions(appliedPromotions);

        log.debug("优惠试算完成: userId={}, originalAmount={}, couponDiscount={}, promotionDiscount={}, finalAmount={}",
                req.getUserId(), originalAmount, couponDiscount, promotionDiscount, finalAmount);
        return resp;
    }

    /**
     * 累加商品明细得到订单原价（单位：分）
     *
     * <p>单价与数量缺失的明细按 0 计入，不抛异常。</p>
     *
     * @param items 商品明细，可为 null 或空
     * @return 订单原价（单位：分），最小为 0
     */
    private long sumItems(List<CalculationItem> items) {
        if (items == null || items.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        for (CalculationItem item : items) {
            if (item == null || item.getPrice() == null || item.getQuantity() == null) {
                continue;
            }
            total += item.getPrice() * item.getQuantity();
        }
        return total;
    }

    /**
     * 自动择优：在用户可用券记录中取优惠最大的一张
     *
     * <p>过滤规则：记录已过期跳过、券定义不存在跳过、门槛（{@code min_order_amount}）不满足跳过；
     * 优惠额一律取券记录的领取快照面值。</p>
     *
     * @param userId         用户 ID
     * @param originalAmount 订单原价（单位：分）
     * @return 命中的券明细，无可用券时返回 {@code null}
     */
    private AppliedCoupon autoSelectBestCoupon(Long userId, long originalAmount) {
        List<MallCouponRecordDO> records = couponRecordMapper.selectUserRecords(userId,
                CouponRecordStatusEnum.AVAILABLE.getCode(), CANDIDATE_COUPON_LIMIT);
        if (records == null || records.isEmpty()) {
            return null;
        }

        // 批量补券定义，避免逐条查库（N+1）
        Set<Long> couponIds = records.stream()
                .filter(Objects::nonNull)
                .map(MallCouponRecordDO::getCouponId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, MallCouponDO> couponMap = new HashMap<>();
        if (!couponIds.isEmpty()) {
            for (MallCouponDO coupon : couponMapper.selectByIdsNotDeleted(couponIds)) {
                if (coupon != null && coupon.getId() != null) {
                    couponMap.put(coupon.getId(), coupon);
                }
            }
        }

        LocalDateTime now = LocalDateTime.now();
        AppliedCoupon best = null;
        long bestDiscount = 0L;
        for (MallCouponRecordDO record : records) {
            if (record == null || isExpired(record, now)) {
                continue;
            }
            MallCouponDO coupon = couponMap.get(record.getCouponId());
            if (coupon == null) {
                // 券定义已被删除：跳过该券，不影响其余候选
                continue;
            }
            if (!isThresholdMet(coupon, originalAmount)) {
                continue;
            }
            long discount = calcCouponDiscount(coupon, record, originalAmount);
            if (discount > bestDiscount) {
                bestDiscount = discount;
                best = buildAppliedCoupon(record, coupon, discount);
            }
        }
        return best;
    }

    /**
     * 指定券试算：严格校验归属与可用性，任一不满足即抛业务异常让下单失败
     *
     * @param userId         用户 ID
     * @param couponClaimId  指定的券记录 ID
     * @param originalAmount 订单原价（单位：分）
     * @return 该券的命中明细
     * @throws BusinessException 券不存在/非本人（A0501）、已使用（A0613）、已过期（A0610）、
     *                           已锁定或定义缺失（A0612）、门槛不满足（A0612）
     */
    private AppliedCoupon applySpecifiedCoupon(Long userId, Long couponClaimId, long originalAmount) {
        MallCouponRecordDO record = couponRecordMapper.selectByIdNotDeleted(couponClaimId);
        // 券不存在与非本人返回同一错误码，避免泄露券归属信息
        if (record == null || !Objects.equals(userId, record.getUserId())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        Integer status = record.getRecordStatus();
        if (status != null && status == CouponRecordStatusEnum.USED.getCode()) {
            throw new BusinessException(ErrorCode.COUPON_USED);
        }
        if (status != null && status == CouponRecordStatusEnum.EXPIRED.getCode()) {
            throw new BusinessException(ErrorCode.COUPON_EXPIRED);
        }
        if (status == null || status != CouponRecordStatusEnum.AVAILABLE.getCode()) {
            // 已锁定 / 已释放等非可用态
            throw new BusinessException(ErrorCode.COUPON_CONDITION_NOT_MET);
        }
        if (isExpired(record, LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.COUPON_EXPIRED);
        }

        MallCouponDO coupon = couponMapper.selectByIdNotDeleted(record.getCouponId());
        if (coupon == null) {
            throw new BusinessException(ErrorCode.COUPON_CONDITION_NOT_MET);
        }
        if (!isThresholdMet(coupon, originalAmount)) {
            throw new BusinessException(ErrorCode.COUPON_CONDITION_NOT_MET);
        }
        return buildAppliedCoupon(record, coupon, calcCouponDiscount(coupon, record, originalAmount));
    }

    /**
     * 匹配进行中的促销活动，得到命中的促销明细
     *
     * @param originalAmount 订单原价（单位：分），门槛按原价判定
     * @return 命中的促销明细，无进行中活动时为不可变空列表
     */
    private List<AppliedPromotion> matchActivePromotions(long originalAmount) {
        List<MallPromotionDO> promotions = promotionMapper.selectActive(LocalDateTime.now(),
                ACTIVE_PROMOTION_LIMIT);
        if (promotions == null || promotions.isEmpty()) {
            return List.of();
        }
        List<Long> promotionIds = promotions.stream()
                .filter(Objects::nonNull)
                .map(MallPromotionDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<MallPromotionRuleDO> rules = promotionRuleMapper.selectByPromotionIds(promotionIds);
        return promotionRuleMatcher.match(promotions, rules, originalAmount);
    }

    /**
     * 汇总促销明细的优惠金额
     *
     * @param appliedPromotions 命中的促销明细
     * @return 促销优惠总额（单位：分），最小为 0
     */
    private long sumDiscounts(List<AppliedPromotion> appliedPromotions) {
        long sum = 0L;
        if (appliedPromotions == null) {
            return sum;
        }
        for (AppliedPromotion promotion : appliedPromotions) {
            if (promotion != null && promotion.getDiscountAmount() != null) {
                sum += promotion.getDiscountAmount();
            }
        }
        return sum;
    }

    /**
     * 判定券记录是否已过期
     *
     * @param record 券记录
     * @param now    当前时间
     * @return 过期时间为空或未晚于当前时间返回 true
     */
    private boolean isExpired(MallCouponRecordDO record, LocalDateTime now) {
        return record.getExpireTime() == null || !record.getExpireTime().isAfter(now);
    }

    /**
     * 判定订单原价是否达到券使用门槛
     *
     * @param coupon         券定义
     * @param originalAmount 订单原价（单位：分）
     * @return 达到门槛返回 true，门槛为空视为无门槛
     */
    private boolean isThresholdMet(MallCouponDO coupon, long originalAmount) {
        Long minOrderAmount = coupon.getMinOrderAmount();
        return minOrderAmount == null || originalAmount >= minOrderAmount;
    }

    /**
     * 计算单张券的优惠金额（单位：分）
     *
     * <p>折扣券按 {@code 原价 ×(100 − discount_rate)/100} 整除，并受 {@code discount_limit} 封顶；
     * 满减券 / 无门槛券取券记录快照面值。最终均不超过订单原价。</p>
     *
     * @param coupon         券定义
     * @param record         券记录（承载领取快照面值）
     * @param originalAmount 订单原价（单位：分）
     * @return 优惠金额（单位：分），最小为 0
     */
    private long calcCouponDiscount(MallCouponDO coupon, MallCouponRecordDO record, long originalAmount) {
        Integer couponType = coupon.getCouponType();
        if (couponType != null && couponType == CouponTypeEnum.DISCOUNT.getCode()) {
            Integer discountRate = coupon.getDiscountRate();
            if (discountRate == null) {
                return 0L;
            }
            long discount = originalAmount * (RATE_BASE - discountRate) / RATE_BASE;
            if (discount < 0L) {
                discount = 0L;
            }
            Long discountLimit = coupon.getDiscountLimit();
            if (discountLimit != null && discountLimit >= 0L && discount > discountLimit) {
                discount = discountLimit;
            }
            return Math.min(discount, originalAmount);
        }
        // 满减券 / 无门槛券：面值取券记录的领取快照，且不超过原价
        long faceValue = record.getFaceValue() == null ? 0L : record.getFaceValue();
        if (faceValue < 0L) {
            faceValue = 0L;
        }
        return Math.min(faceValue, originalAmount);
    }

    /**
     * 组装命中券明细
     *
     * @param record   券记录
     * @param coupon   券定义
     * @param discount 优惠金额（单位：分）
     * @return 命中券明细
     */
    private AppliedCoupon buildAppliedCoupon(MallCouponRecordDO record, MallCouponDO coupon, long discount) {
        AppliedCoupon applied = new AppliedCoupon();
        applied.setCouponRecordId(record.getId());
        applied.setCouponName(coupon.getCouponName());
        applied.setDiscountAmount(discount);
        return applied;
    }
}
