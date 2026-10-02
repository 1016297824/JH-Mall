package com.mall.common.enums;

/**
 * 发件箱状态枚举
 *
 * <p>对应数据库 mall_outbox.outbox_status（varchar）</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public enum OutboxStatusEnum {

    /** 新建 */
    NEW("NEW", "新建"),
    /** 待发送 */
    PENDING("PENDING", "待发送"),
    /** 已发送 */
    SENT("SENT", "已发送"),
    /** 失败 */
    FAILED("FAILED", "失败"),
    /**
     * 已取消
     *
     * <p>用于支付成功后取消尚未投递的支付超时延迟消息
     * （topic = {@code mall:order:timeout}），见设计文档 §5.9。
     * 未投递的消息被取消后不再参与调度，避免已支付订单被延迟消息误关单。</p>
     */
    CANCELLED("CANCELLED", "已取消");

    /** 发件箱状态码 */
    private final String code;
    /** 发件箱状态描述 */
    private final String description;

    /**
     * 构造发件箱状态枚举
     *
     * @param code        发件箱状态码
     * @param description 发件箱状态描述
     */
    OutboxStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取发件箱状态码
     *
     * @return 发件箱状态码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取发件箱状态描述
     *
     * @return 发件箱状态描述
     */
    public String getDescription() {
        return description;
    }
}
