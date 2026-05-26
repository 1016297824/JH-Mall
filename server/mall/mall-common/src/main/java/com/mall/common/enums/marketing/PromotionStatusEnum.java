package com.mall.common.enums.marketing;

public enum PromotionStatusEnum {

    PENDING(0, "未开始"),
    ACTIVE(1, "进行中"),
    ENDED(2, "已结束"),
    CLOSED(3, "已关闭");

    private final int code;
    private final String description;

    PromotionStatusEnum(int code, String description) {
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
