package com.mall.api.feign;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * C 端营销服务 Feign 接口
 *
 * <p>提供给 mall-order 调用（优惠试算 / 锁券 / 释放 / 校验）</p>
 *
 * <p>金额字段一律为 {@code Long}，单位<strong>分</strong>。</p>
 *
 * <p>对应设计文档 {@code docs/design/07_mall-api契约层设计.md} §3.6</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@FeignClient(contextId = "mall-marketing", value = "mall-marketing")
public interface RemoteMarketingService {

    /**
     * 优惠试算（不锁定任何资源）
     *
     * <p>下单前调用，仅计算最优优惠组合。内部走两阶段过滤 + 组合择优，
     * 超时 500ms 兜底返回当前最优解。</p>
     *
     * @param req 试算请求（用户 + 商品明细 + 可选指定券）
     * @return 试算结果（原价 / 券优惠 / 促销优惠 / 应付）
     */
    @PostMapping("/inner/marketing/calculate")
    CalculationResp calculate(@RequestBody CalculationReq req);

    /**
     * 锁定优惠券（下单时）
     *
     * <p>把优惠券记录置为锁定态。失败时调用方需释放已锁库存做补偿。</p>
     *
     * @param orderNo      订单号
     * @param couponClaimId 用户优惠券记录 ID
     * @return 是否锁定成功
     */
    @PostMapping("/inner/marketing/coupon/lock")
    boolean lockCoupon(@RequestParam("orderNo") String orderNo,
                       @RequestParam("couponClaimId") Long couponClaimId);

    /**
     * 释放订单已锁定的全部优惠券
     *
     * <p>订单取消 / 超时关单 / 创建订单失败时调用。</p>
     *
     * @param orderNo 订单号
     */
    @PostMapping("/inner/marketing/coupon/release")
    void releaseCoupon(@RequestParam("orderNo") String orderNo);

    /**
     * 校验优惠券可用性
     *
     * @param couponClaimId 用户优惠券记录 ID
     * @param userId        用户 ID
     * @return 可用返回 true
     */
    @GetMapping("/inner/marketing/coupon/validate")
    boolean validateCoupon(@RequestParam("couponClaimId") Long couponClaimId,
                           @RequestParam("userId") Long userId);

    /**
     * 优惠试算请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class CalculationReq {

        /** 用户 ID */
        private Long userId;

        /** 商品明细 */
        private List<CalculationItem> items;

        /**
         * 指定使用的优惠券记录 ID（可选）
         *
         * <p>传了则只试算这一张券；不传则由试算引擎在用户可用券中自动择优。</p>
         */
        private Long couponClaimId;

        /**
         * 试算商品明细
         *
         * <p>必须显式 {@code public static}：它嵌在类 {@code CalculationReq}（非接口）中，
         * 类内嵌套类型默认是包级私有，跨包引用会编译失败。</p>
         */
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CalculationItem {

            /** SKU ID */
            private Long skuId;

            /** 单价（单位：分） */
            private Long price;

            /** 购买数量 */
            private Integer quantity;
        }
    }

    /**
     * 优惠试算结果
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class CalculationResp {

        /** 订单原价（单位：分） */
        private Long originalAmount;

        /** 优惠券优惠总额（单位：分） */
        private Long couponDiscount;

        /** 促销优惠总额（单位：分） */
        private Long promotionDiscount;

        /** 优惠后应付金额（单位：分） */
        private Long finalAmount;

        /** 已应用的优惠券明细 */
        private List<AppliedCoupon> appliedCoupons;

        /** 已应用的促销明细 */
        private List<AppliedPromotion> appliedPromotions;

        /**
         * 命中优惠券明细
         *
         * <p>必须显式 {@code public static}（理由同 {@code CalculationItem}）。</p>
         */
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AppliedCoupon {

            /** 用户优惠券记录 ID */
            private Long couponRecordId;

            /** 优惠券名称 */
            private String couponName;

            /** 该券优惠金额（单位：分） */
            private Long discountAmount;
        }

        /**
         * 命中促销明细
         *
         * <p>必须显式 {@code public static}（理由同 {@code CalculationItem}）。</p>
         */
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AppliedPromotion {

            /** 促销活动 ID */
            private Long promotionId;

            /** 活动名称 */
            private String promotionName;

            /** 命中的规则 ID */
            private Long ruleId;

            /** 该规则优惠金额（单位：分） */
            private Long discountAmount;
        }
    }
}