package com.mall.common.enums.payment;

public enum CallbackProcessStatusEnum {

    PENDING(0, "待处理"),
    SUCCESS(1, "处理成功"),
    FAILED(2, "处理失败"),
    DUPLICATE(3, "重复通知");

    private final int code;
    private final String description;

    CallbackProcessStatusEnum(int code, String description) {
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
