package com.mall.common.enums.order;

/**
 * 售后类型枚举
 *
 * <p>对应数据库 mall_after_sale.after_sale_type（tinyint unsigned）</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public enum AfterSaleTypeEnum {

    /** 仅退款 */
    REFUND_ONLY(1, "仅退款"),
    /** 退货退款 */
    RETURN_REFUND(2, "退货退款"),
    /** 换货 */
    EXCHANGE(3, "换货");

    /** 售后类型码 */
    private final int code;
    /** 售后类型描述 */
    private final String description;

    /**
     * 构造售后类型枚举
     *
     * @param code        售后类型码
     * @param description 售后类型描述
     */
    AfterSaleTypeEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取售后类型码
     *
     * @return 售后类型码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取售后类型描述
     *
     * @return 售后类型描述
     */
    public String getDescription() {
        return description;
    }
}
