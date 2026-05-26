package com.mall.common.enums.marketing;

/**
 * 促销规则类型枚举
 *
 * <p>对应数据库 mall_promotion_rule.rule_type（tinyint unsigned）</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public enum RuleTypeEnum {

    /** 满减 */
    FULL_REDUCE(1, "满减"),
    /** 满折 */
    FULL_DISCOUNT(2, "满折"),
    /** 免邮 */
    FREE_SHIPPING(3, "免邮");

    /** 规则类型码 */
    private final int code;
    /** 规则类型描述 */
    private final String description;

    /**
     * 构造促销规则类型枚举
     *
     * @param code        规则类型码
     * @param description 规则类型描述
     */
    RuleTypeEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取规则类型码
     *
     * @return 规则类型码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取规则类型描述
     *
     * @return 规则类型描述
     */
    public String getDescription() {
        return description;
    }
}
