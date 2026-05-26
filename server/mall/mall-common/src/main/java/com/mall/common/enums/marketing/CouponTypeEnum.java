package com.mall.common.enums.marketing;

/**
 * 优惠券类型枚举
 *
 * <p>对应数据库 mall_coupon.coupon_type（tinyint unsigned）</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public enum CouponTypeEnum {

    /** 满减券 */
    FULL_REDUCE(1, "满减券"),
    /** 折扣券 */
    DISCOUNT(2, "折扣券"),
    /** 无门槛券 */
    NO_THRESHOLD(3, "无门槛券");

    /** 优惠券类型码 */
    private final int code;
    /** 优惠券类型描述 */
    private final String description;

    /**
     * 构造优惠券类型枚举
     *
     * @param code        优惠券类型码
     * @param description 优惠券类型描述
     */
    CouponTypeEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取优惠券类型码
     *
     * @return 优惠券类型码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取优惠券类型描述
     *
     * @return 优惠券类型描述
     */
    public String getDescription() {
        return description;
    }
}
