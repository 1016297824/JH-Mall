package com.mall.common.enums.order;

/**
 * 售后状态枚举
 *
 * <p>对应数据库 mall_after_sale.after_sale_status（tinyint unsigned）</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public enum AfterSaleStatusEnum {

    /** 待审核 */
    PENDING(0, "待审核"),
    /** 审核通过 */
    APPROVED(1, "审核通过"),
    /** 审核驳回 */
    REJECTED(2, "审核驳回"),
    /** 买家已退货 */
    RETURNED(3, "买家已退货"),
    /** 商家已收货 */
    RECEIVED(4, "商家已收货"),
    /** 退款中 */
    REFUNDING(5, "退款中"),
    /** 退款完成 */
    COMPLETED(6, "退款完成"),
    /** 已关闭 */
    CLOSED(7, "已关闭");

    /** 售后状态码 */
    private final int code;
    /** 售后状态描述 */
    private final String description;

    /**
     * 构造售后状态枚举
     *
     * @param code        售后状态码
     * @param description 售后状态描述
     */
    AfterSaleStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取售后状态码
     *
     * @return 售后状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取售后状态描述
     *
     * @return 售后状态描述
     */
    public String getDescription() {
        return description;
    }
}
