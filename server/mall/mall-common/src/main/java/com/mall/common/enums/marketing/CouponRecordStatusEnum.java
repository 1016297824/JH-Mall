package com.mall.common.enums.marketing;

/**
 * 优惠券记录状态枚举
 *
 * <p>对应数据库 mall_coupon_record.record_status（tinyint unsigned）</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public enum CouponRecordStatusEnum {

    /** 可用 */
    AVAILABLE(0, "可用"),
    /** 已锁定 */
    LOCKED(1, "已锁定"),
    /** 已使用 */
    USED(2, "已使用"),
    /** 已释放 */
    RELEASED(3, "已释放"),
    /** 已过期 */
    EXPIRED(4, "已过期");

    /** 记录状态码 */
    private final int code;
    /** 记录状态描述 */
    private final String description;

    /**
     * 构造优惠券记录状态枚举
     *
     * @param code        记录状态码
     * @param description 记录状态描述
     */
    CouponRecordStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取记录状态码
     *
     * @return 记录状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取记录状态描述
     *
     * @return 记录状态描述
     */
    public String getDescription() {
        return description;
    }
}
