package com.mall.common.enums.user;

/**
 * 积分/成长值业务类型枚举
 *
 * @author system
 * @date 2026/05/28
 */
public enum BizTypeEnum {

    /** 下单赠送 */
    ORDER("order", "下单赠送"),

    /** 签到 */
    SIGN_IN("signin", "签到"),

    /** 评价 */
    REVIEW("review", "评价"),

    /** 退款扣除 */
    REFUND("refund", "退款扣除"),

    /** 管理员调整 */
    ADMIN("admin", "管理员调整"),

    /** 积分过期 */
    EXPIRE("expire", "积分过期");

    private final String code;

    private final String name;

    BizTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 根据 code 获取枚举
     *
     * @param code 业务类型编码
     * @return 对应的枚举值，不存在返回 null
     */
    public static BizTypeEnum fromCode(String code) {
        for (BizTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
