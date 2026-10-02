package com.mall.order.statemachine;

/**
 * 订单事件枚举
 *
 * <p>触发订单状态转移的事件，定义见
 * {@code docs/design/12_mall-order详细设计.md} §6.2。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
public enum OrderEventEnum {

    /** 预留：公共退款（当前设计未使用） */
    COMMON_REFUND(0),

    /** 支付平台回调支付成功 */
    PAY_SUCCESS(1),

    /** 用户主动取消订单 */
    USER_CANCEL(2),

    /** 超时未支付，定时任务或延迟消息触发 */
    PAY_TIMEOUT(3),

    /** 管理端填写物流信息并确认发货 */
    SELLER_DELIVER(4),

    /** 快递公司揽收回调 */
    LOGISTICS_PICK(5),

    /** 用户点击确认收货 */
    CONFIRM_RECEIPT(6),

    /** 管理端强制取消（客服审核通过） */
    FORCE_CANCEL(7),

    /** 发起仅退款售后（未发货） */
    REFUND_ONLY(8),

    /** 发起退货退款售后（已发货） */
    RETURN_REFUND(9),

    /** 已完成后用户申请售后维权 */
    AFTER_SALE(10),

    /** 支付平台退款成功回调 */
    REFUND_SUCCESS(11),

    /** 支付平台退款失败回调 */
    REFUND_FAIL(12);

    /** 事件码 */
    private final int code;

    OrderEventEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}