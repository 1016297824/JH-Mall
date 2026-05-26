package com.mall.common.enums.payment;

/**
 * 支付状态枚举
 *
 * <p>对应数据库 mall_payment.payment_status（tinyint unsigned）</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public enum PaymentStatusEnum {

    /** 未支付 */
    UNPAID(0, "未支付"),
    /** 已支付 */
    PAID(1, "已支付"),
    /** 支付失败 */
    FAILED(2, "支付失败"),
    /** 已关闭 */
    CLOSED(3, "已关闭"),
    /** 退款中 */
    REFUNDING(4, "退款中"),
    /** 已退款 */
    REFUNDED(5, "已退款");

    /** 支付状态码 */
    private final int code;
    /** 支付状态描述 */
    private final String description;

    /**
     * 构造支付状态枚举
     *
     * @param code        支付状态码
     * @param description 支付状态描述
     */
    PaymentStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取支付状态码
     *
     * @return 支付状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取支付状态描述
     *
     * @return 支付状态描述
     */
    public String getDescription() {
        return description;
    }
}
