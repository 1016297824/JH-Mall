package com.mall.common.enums.order;

/**
 * 取消类型枚举
 *
 * <p>对应数据库 mall_order.cancel_type（varchar）</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public enum CancelTypeEnum {

    /** 用户取消 */
    USER_CANCEL("user_cancel", "用户取消"),
    /** 超时取消 */
    TIMEOUT_CANCEL("timeout_cancel", "超时取消"),
    /** 管理员取消 */
    ADMIN_CANCEL("admin_cancel", "管理员取消");

    /** 取消类型码 */
    private final String code;
    /** 取消类型描述 */
    private final String description;

    /**
     * 构造取消类型枚举
     *
     * @param code        取消类型码
     * @param description 取消类型描述
     */
    CancelTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取取消类型码
     *
     * @return 取消类型码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取取消类型描述
     *
     * @return 取消类型描述
     */
    public String getDescription() {
        return description;
    }
}
