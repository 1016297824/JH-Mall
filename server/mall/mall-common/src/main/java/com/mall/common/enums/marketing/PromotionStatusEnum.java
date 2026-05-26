package com.mall.common.enums.marketing;

/**
 * 促销活动状态枚举
 *
 * <p>对应数据库 mall_promotion.promotion_status（tinyint unsigned）</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public enum PromotionStatusEnum {

    /** 未开始 */
    PENDING(0, "未开始"),
    /** 进行中 */
    ACTIVE(1, "进行中"),
    /** 已结束 */
    ENDED(2, "已结束"),
    /** 已关闭 */
    CLOSED(3, "已关闭");

    /** 促销状态码 */
    private final int code;
    /** 促销状态描述 */
    private final String description;

    /**
     * 构造促销活动状态枚举
     *
     * @param code        促销状态码
     * @param description 促销状态描述
     */
    PromotionStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取促销状态码
     *
     * @return 促销状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取促销状态描述
     *
     * @return 促销状态描述
     */
    public String getDescription() {
        return description;
    }
}
