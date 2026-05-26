package com.mall.common.enums.marketing;

/**
 * 优惠券状态枚举
 *
 * <p>对应数据库 mall_coupon.coupon_status（tinyint unsigned）</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public enum CouponStatusEnum {

    /** 草稿 */
    DRAFT(0, "草稿"),
    /** 已发布 */
    PUBLISHED(1, "已发布"),
    /** 已结束 */
    ENDED(2, "已结束"),
    /** 已废弃 */
    DISCARDED(3, "已废弃");

    /** 优惠券状态码 */
    private final int code;
    /** 优惠券状态描述 */
    private final String description;

    /**
     * 构造优惠券状态枚举
     *
     * @param code        优惠券状态码
     * @param description 优惠券状态描述
     */
    CouponStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取优惠券状态码
     *
     * @return 优惠券状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取优惠券状态描述
     *
     * @return 优惠券状态描述
     */
    public String getDescription() {
        return description;
    }
}
