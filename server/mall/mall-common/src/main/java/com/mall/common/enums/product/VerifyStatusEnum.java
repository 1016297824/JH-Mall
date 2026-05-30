package com.mall.common.enums.product;

import lombok.Getter;

/**
 * 商品审核状态枚举
 *
 * <p>对应数据库 mall_product.verify_status（tinyint unsigned）</p>
 *
 * @author JH-Mall
 * @date 2026/05/30
 */
@Getter
public enum VerifyStatusEnum {

    /** 待审核 */
    PENDING(0, "待审核"),
    /** 审核通过 */
    APPROVED(1, "审核通过"),
    /** 审核驳回 */
    REJECTED(2, "审核驳回");

    /** 状态码 */
    private final int code;
    /** 状态描述 */
    private final String desc;

    VerifyStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
