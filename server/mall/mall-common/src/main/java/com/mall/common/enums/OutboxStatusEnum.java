package com.mall.common.enums;

public enum OutboxStatusEnum {

    NEW("NEW", "新建"),
    PENDING("PENDING", "待发送"),
    SENT("SENT", "已发送"),
    FAILED("FAILED", "失败");

    private final String code;
    private final String description;

    OutboxStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
