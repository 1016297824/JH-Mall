package com.mall.common.enums.order;

public enum OrderStatusEnum {

    WAIT_PAY(0, "待支付"),
    PAID(1, "已支付"),
    WAIT_DELIVER(2, "待发货"),
    WAIT_RECEIVE(3, "待收货"),
    COMPLETED(4, "已完成"),
    CANCELLED(5, "已取消"),
    CLOSED(6, "已关闭"),
    REFUNDING(7, "退款中"),
    REFUNDED(8, "已退款");

    private final int code;
    private final String description;

    OrderStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
