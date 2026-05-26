package com.mall.common.enums.product;

/**
 * 商品发布状态枚举
 *
 * <p>对应数据库 mall_product.publish_status（tinyint unsigned）</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public enum PublishStatusEnum {

    /** 下架 */
    OFFLINE(0, "下架"),
    /** 上架 */
    ONLINE(1, "上架");

    /** 发布状态码 */
    private final int code;
    /** 发布状态描述 */
    private final String description;

    /**
     * 构造商品发布状态枚举
     *
     * @param code        发布状态码
     * @param description 发布状态描述
     */
    PublishStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取发布状态码
     *
     * @return 发布状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取发布状态描述
     *
     * @return 发布状态描述
     */
    public String getDescription() {
        return description;
    }
}
