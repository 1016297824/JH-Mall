package com.mall.common.enums.payment;

/**
 * 支付通道类型枚举
 *
 * <p>对应数据库 mall_payment_channel.channel_type（tinyint unsigned）</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public enum ChannelTypeEnum {

    /** 支付 */
    PAY(1, "支付"),
    /** 退款 */
    REFUND(2, "退款");

    /** 通道类型码 */
    private final int code;
    /** 通道类型描述 */
    private final String description;

    /**
     * 构造支付通道类型枚举
     *
     * @param code        通道类型码
     * @param description 通道类型描述
     */
    ChannelTypeEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取通道类型码
     *
     * @return 通道类型码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取通道类型描述
     *
     * @return 通道类型描述
     */
    public String getDescription() {
        return description;
    }
}
