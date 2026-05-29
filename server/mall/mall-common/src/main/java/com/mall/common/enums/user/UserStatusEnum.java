package com.mall.common.enums.user;

/**
 * 用户状态枚举
 *
 * <p>对应数据库 mall_user.user_status（tinyint unsigned）</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 * @see 03_系统详细设计.md §1.1
 * @see 06_mall-common公共模块设计.md §2.2
 */
public enum UserStatusEnum {

    /** 正常 */
    NORMAL(0, "正常"),
    /** 冻结 */
    FROZEN(1, "冻结"),
    /** 注销 */
    DELETED(2, "注销");

    /** 状态码 */
    private final int code;
    /** 状态描述 */
    private final String description;

    /**
     * 构造用户状态枚举
     *
     * @param code        状态码
     * @param description 状态描述
     */
    UserStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取状态码
     *
     * @return 状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取状态描述
     *
     * @return 状态描述
     */
    public String getDescription() {
        return description;
    }
}
