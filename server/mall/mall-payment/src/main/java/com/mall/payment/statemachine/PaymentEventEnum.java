package com.mall.payment.statemachine;

/**
 * 支付单状态转移事件
 *
 * <p>触发支付单状态转移的事件，定义见
 * {@code docs/design/13_mall-payment详细设计.md} §7.2.1 转移矩阵。</p>
 *
 * <p><b>与设计文档的差异</b>：设计 §7.2.1 第 1 行「发起支付」的目标状态仍是
 * {@code UNPAID}，本质是「预检 + 回填渠道单号」而非状态转移，故<b>不</b>在此定义事件，
 * 由 {@code PaymentServiceImpl} 直接处理；第 8 行「全额退款完成」与第 6 行
 * 「退款成功回调」是同一转移，合并为 {@link #REFUND_SUCCESS_CALLBACK}。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
public enum PaymentEventEnum {

    /** 支付成功回调（事件源：渠道回调，经 PaymentCallbackController 验签后触发） */
    PAY_SUCCESS_CALLBACK(1),

    /** 支付失败回调（事件源：渠道回调，渠道返回明确失败） */
    PAY_FAIL_CALLBACK(2),

    /** 支付超时关闭（事件源：定时任务 PaymentTimeoutTask，扫描已过 expire_time 的 UNPAID 支付单） */
    PAY_TIMEOUT_CLOSE(3),

    /** 发起退款（事件源：RefundService，mall-order 售后审核通过后经 Feign 触发） */
    REFUND_START(4),

    /** 退款成功回调（事件源：渠道退款回调；亦覆盖设计 §7.2.1 的「全额退款完成」） */
    REFUND_SUCCESS_CALLBACK(5),

    /** 退款失败回调（事件源：渠道退款回调，渠道返回明确失败，支付单回退到 PAID） */
    REFUND_FAIL_CALLBACK(6);

    /** 事件码 */
    private final int code;

    PaymentEventEnum(int code) {
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
