package com.mall.common.enums.product;

import lombok.Getter;

/**
 * 商品发布状态枚举
 *
 * <p>对应数据库 mall_product.publish_status（tinyint unsigned）</p>
 *
 * @author JH-Mall
 * @date 2026/05/30
 */
@Getter
public enum PublishStatusEnum {

    /** 已下架 */
    OFFLINE(0, "已下架"),
    /** 已上架 */
    ONLINE(1, "已上架");

    /** 状态码 */
    private final int code;
    /** 状态描述 */
    private final String desc;

    PublishStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
