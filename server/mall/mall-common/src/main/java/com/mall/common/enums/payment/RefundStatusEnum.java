package com.mall.common.enums.payment;

/**
 * 退款状态枚举
 *
 * <p>对应数据库 mall_refund.refund_status（tinyint unsigned）</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public enum RefundStatusEnum {

    /** 处理中 */
    PROCESSING(0, "处理中"),
    /** 退款成功 */
    SUCCESS(1, "退款成功"),
    /** 退款失败 */
    FAILED(2, "退款失败");

    /** 退款状态码 */
    private final int code;
    /** 退款状态描述 */
    private final String description;

    /**
     * 构造退款状态枚举
     *
     * @param code        退款状态码
     * @param description 退款状态描述
     */
    RefundStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取退款状态码
     *
     * @return 退款状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取退款状态描述
     *
     * @return 退款状态描述
     */
    public String getDescription() {
        return description;
    }
}
