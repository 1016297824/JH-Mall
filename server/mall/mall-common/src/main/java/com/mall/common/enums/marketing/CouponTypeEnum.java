package com.mall.common.enums.marketing;

public enum CouponTypeEnum {

    FULL_REDUCE(1, "满减券"),
    DISCOUNT(2, "折扣券"),
    NO_THRESHOLD(3, "无门槛券");

    private final int code;
    private final String description;

    CouponTypeEnum(int code, String description) {
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
