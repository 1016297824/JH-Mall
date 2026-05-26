package com.mall.common.enums.marketing;

public enum CouponRecordStatusEnum {

    AVAILABLE(0, "可用"),
    LOCKED(1, "已锁定"),
    USED(2, "已使用"),
    RELEASED(3, "已释放"),
    EXPIRED(4, "已过期");

    private final int code;
    private final String description;

    CouponRecordStatusEnum(int code, String description) {
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
