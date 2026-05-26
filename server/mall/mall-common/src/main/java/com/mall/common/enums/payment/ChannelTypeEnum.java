package com.mall.common.enums.payment;

public enum ChannelTypeEnum {

    PAY(1, "支付"),
    REFUND(2, "退款");

    private final int code;
    private final String description;

    ChannelTypeEnum(int code, String description) {
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
