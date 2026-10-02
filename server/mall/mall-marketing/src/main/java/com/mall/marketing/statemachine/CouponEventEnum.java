package com.mall.marketing.statemachine;

/**
 * 优惠券记录事件枚举
 *
 * <p>触发优惠券记录状态转移的事件，定义见
 * {@code docs/design/14_mall-marketing详细设计.md} §3.3 转移矩阵。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
public enum CouponEventEnum {

    /** 下单锁定（事件源：mall-order，经 Feign 调用） */
    LOCK(1),

    /** 订单支付成功（事件源：RocketMQ {@code mall:order:paid}） */
    PAY_SUCCESS(2),

    /** 订单取消 / 超时关闭（事件源：RocketMQ {@code mall:order:cancelled}） */
    ORDER_CANCEL(3),

    /** 有效期到期（事件源：定时任务 CouponExpireTask） */
    EXPIRE(4);

    /** 事件码 */
    private final int code;

    CouponEventEnum(int code) {
        this.code = code;
    }

    /**
     * 获取事件码
     *
     * @return 事件码
     */
    public int getCode() {
        return code;
    }
}
