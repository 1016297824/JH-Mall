package com.mall.common.enums.user;

/**
 * 注册方式枚举
 *
 * <p>对应数据库 mall_user.register_type（varchar）</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public enum RegisterTypeEnum {

    /** 手机号注册 */
    PHONE("phone", "手机号"),
    /** 微信注册 */
    WECHAT("wechat", "微信"),
    /** 邮箱注册 */
    EMAIL("email", "邮箱");

    /** 注册方式码 */
    private final String code;
    /** 注册方式描述 */
    private final String description;

    /**
     * 构造注册方式枚举
     *
     * @param code        注册方式码
     * @param description 注册方式描述
     */
    RegisterTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取注册方式码
     *
     * @return 注册方式码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取注册方式描述
     *
     * @return 注册方式描述
     */
    public String getDescription() {
        return description;
    }
}
