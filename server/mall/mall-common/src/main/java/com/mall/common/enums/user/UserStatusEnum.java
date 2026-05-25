package com.mall.common.enums.user;

/**
 * 用户状态枚举
 * <p>对应数据库 mall_user.user_status（tinyint unsigned）</p>
 *
 * @see docs/design/03_系统详细设计.md §1.1
 * @see docs/design/06_mall-common公共模块设计.md §2.2
 */
public enum UserStatusEnum {

    NORMAL(0, "正常"),
    FROZEN(1, "冻结"),
    DELETED(2, "注销");

    private final int code;
    private final String description;

    UserStatusEnum(int code, String description) {
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
