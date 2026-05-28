package com.mall.common.enums.user;

/**
 * 性别枚举
 *
 * <p>对应数据库 mall_user.gender（tinyint unsigned）</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public enum GenderEnum {

    /** 未知 */
    UNKNOWN(0, "未知"),
    /** 男 */
    MALE(1, "男"),
    /** 女 */
    FEMALE(2, "女");

    /** 性别码 */
    private final int code;
    /** 性别描述 */
    private final String description;

    /**
     * 构造性别枚举
     *
     * @param code        性别码
     * @param description 性别描述
     */
    GenderEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取性别码
     *
     * @return 性别码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取性别描述
     *
     * @return 性别描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据 code 获取性别枚举
     *
     * @param code 性别码
     * @return 对应的枚举值，不存在返回 null
     */
    public static GenderEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (GenderEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
