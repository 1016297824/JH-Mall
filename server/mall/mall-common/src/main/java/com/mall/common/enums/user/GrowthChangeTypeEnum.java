package com.mall.common.enums.user;

/**
 * 成长值变更类型枚举
 *
 * <p>对应数据库 mall_growth_log.change_type（tinyint unsigned）</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public enum GrowthChangeTypeEnum {

    /** 增加 */
    INCREASE(1, "增加"),
    /** 减少 */
    DECREASE(2, "减少");

    /** 变更类型码 */
    private final int code;
    /** 变更类型描述 */
    private final String description;

    /**
     * 构造成长值变更类型枚举
     *
     * @param code        变更类型码
     * @param description 变更类型描述
     */
    GrowthChangeTypeEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取变更类型码
     *
     * @return 变更类型码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取变更类型描述
     *
     * @return 变更类型描述
     */
    public String getDescription() {
        return description;
    }
}
