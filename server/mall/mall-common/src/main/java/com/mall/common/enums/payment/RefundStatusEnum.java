package com.mall.common.enums.payment;

public enum RefundStatusEnum {

    PROCESSING(0, "处理中"),
    SUCCESS(1, "退款成功"),
    FAILED(2, "退款失败");

    private final int code;
    private final String description;

    RefundStatusEnum(int code, String description) {
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
