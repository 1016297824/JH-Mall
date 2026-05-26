package com.mall.common.enums.user;

public enum GrowthChangeTypeEnum {

    INCREASE(1, "增加"),
    DECREASE(2, "减少");

    private final int code;
    private final String description;

    GrowthChangeTypeEnum(int code, String description) {
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
