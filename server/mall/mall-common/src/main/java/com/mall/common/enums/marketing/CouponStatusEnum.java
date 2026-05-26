package com.mall.common.enums.marketing;

public enum CouponStatusEnum {

    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    ENDED(2, "已结束"),
    DISCARDED(3, "已废弃");

    private final int code;
    private final String description;

    CouponStatusEnum(int code, String description) {
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
