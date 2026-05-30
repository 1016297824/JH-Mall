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
    OFFLINE(0, "已下架"),
    ONLINE(1, "已上架");

    private final int code;
    private final String desc;

    PublishStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
