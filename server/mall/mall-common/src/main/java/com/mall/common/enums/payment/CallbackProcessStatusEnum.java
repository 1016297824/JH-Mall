package com.mall.common.enums.payment;

/**
 * 回调处理状态枚举
 *
 * <p>对应数据库 mall_payment_callback.process_status（tinyint unsigned）</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public enum CallbackProcessStatusEnum {

    /** 待处理 */
    PENDING(0, "待处理"),
    /** 处理成功 */
    SUCCESS(1, "处理成功"),
    /** 处理失败 */
    FAILED(2, "处理失败"),
    /** 重复通知 */
    DUPLICATE(3, "重复通知");

    /** 处理状态码 */
    private final int code;
    /** 处理状态描述 */
    private final String description;

    /**
     * 构造回调处理状态枚举
     *
     * @param code        处理状态码
     * @param description 处理状态描述
     */
    CallbackProcessStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取处理状态码
     *
     * @return 处理状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取处理状态描述
     *
     * @return 处理状态描述
     */
    public String getDescription() {
        return description;
    }
}
