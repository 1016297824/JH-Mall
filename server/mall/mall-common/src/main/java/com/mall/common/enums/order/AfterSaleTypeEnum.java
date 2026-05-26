package com.mall.common.enums.order;

public enum AfterSaleTypeEnum {

    REFUND_ONLY(1, "仅退款"),
    RETURN_REFUND(2, "退货退款"),
    EXCHANGE(3, "换货");

    private final int code;
    private final String description;

    AfterSaleTypeEnum(int code, String description) {
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
