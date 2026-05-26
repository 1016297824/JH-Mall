package com.mall.common.enums.product;

/**
 * 商品审核状态枚举
 *
 * <p>对应数据库 mall_product.verify_status（tinyint unsigned）</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public enum VerifyStatusEnum {

    /** 待审核 */
    PENDING(0, "待审核"),
    /** 审核通过 */
    APPROVED(1, "审核通过"),
    /** 审核驳回 */
    REJECTED(2, "审核驳回");

    /** 审核状态码 */
    private final int code;
    /** 审核状态描述 */
    private final String description;

    /**
     * 构造商品审核状态枚举
     *
     * @param code        审核状态码
     * @param description 审核状态描述
     */
    VerifyStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取审核状态码
     *
     * @return 审核状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取审核状态描述
     *
     * @return 审核状态描述
     */
    public String getDescription() {
        return description;
    }
}
