package com.mall.common.enums.marketing;

public enum RuleTypeEnum {

    FULL_REDUCE(1, "满减"),
    FULL_DISCOUNT(2, "满折"),
    FREE_SHIPPING(3, "免邮");

    private final int code;
    private final String description;

    RuleTypeEnum(int code, String description) {
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
