package com.mall.common.enums.order;

public enum AfterSaleStatusEnum {

    PENDING(0, "待审核"),
    APPROVED(1, "审核通过"),
    REJECTED(2, "审核驳回"),
    RETURNED(3, "买家已退货"),
    RECEIVED(4, "商家已收货"),
    REFUNDING(5, "退款中"),
    COMPLETED(6, "退款完成"),
    CLOSED(7, "已关闭");

    private final int code;
    private final String description;

    AfterSaleStatusEnum(int code, String description) {
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
