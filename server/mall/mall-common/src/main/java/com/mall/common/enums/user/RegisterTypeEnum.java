package com.mall.common.enums.user;

public enum RegisterTypeEnum {

    PHONE("phone", "手机号"),
    WECHAT("wechat", "微信"),
    EMAIL("email", "邮箱");

    private final String code;
    private final String description;

    RegisterTypeEnum(String code, String description) {
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
