package com.mall.common.enums.product;

public enum PublishStatusEnum {

    OFFLINE(0, "下架"),
    ONLINE(1, "上架");

    private final int code;
    private final String description;

    PublishStatusEnum(int code, String description) {
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
