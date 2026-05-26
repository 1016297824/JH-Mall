package com.mall.common.enums.payment;

public enum PaymentStatusEnum {

    UNPAID(0, "未支付"),
    PAID(1, "已支付"),
    FAILED(2, "支付失败"),
    CLOSED(3, "已关闭"),
    REFUNDING(4, "退款中"),
    REFUNDED(5, "已退款");

    private final int code;
    private final String description;

    PaymentStatusEnum(int code, String description) {
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
