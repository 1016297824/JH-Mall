package com.mall.common.enums.order;

public enum CancelTypeEnum {

    USER_CANCEL("user_cancel", "用户取消"),
    TIMEOUT_CANCEL("timeout_cancel", "超时取消"),
    ADMIN_CANCEL("admin_cancel", "管理员取消");

    private final String code;
    private final String description;

    CancelTypeEnum(String code, String description) {
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
