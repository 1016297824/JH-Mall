package com.mall.common.enums.order;

/**
 * 订单状态枚举
 *
 * <p>对应数据库 mall_order.order_status（tinyint unsigned）</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public enum OrderStatusEnum {

    /** 待支付 */
    WAIT_PAY(0, "待支付"),
    /** 已支付 */
    PAID(1, "已支付"),
    /** 待发货 */
    WAIT_DELIVER(2, "待发货"),
    /** 待收货 */
    WAIT_RECEIVE(3, "待收货"),
    /** 已完成 */
    COMPLETED(4, "已完成"),
    /** 已取消 */
    CANCELLED(5, "已取消"),
    /** 已关闭 */
    CLOSED(6, "已关闭"),
    /** 退款中 */
    REFUNDING(7, "退款中"),
    /** 已退款 */
    REFUNDED(8, "已退款");

    /** 订单状态码 */
    private final int code;
    /** 订单状态描述 */
    private final String description;

    /**
     * 构造订单状态枚举
     *
     * @param code        订单状态码
     * @param description 订单状态描述
     */
    OrderStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取订单状态码
     *
     * @return 订单状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取订单状态描述
     *
     * @return 订单状态描述
     */
    public String getDescription() {
        return description;
    }
}
