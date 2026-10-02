package com.mall.payment.statemachine;

/**
 * 退款单状态转移事件
 *
 * <p>触发退款单状态转移的事件，定义见
 * {@code docs/design/13_mall-payment详细设计.md} §7.2.2 转移矩阵。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
public enum RefundEventEnum {

    /** 退款成功回调（事件源：渠道退款回调，验签通过后触发） */
    REFUND_SUCCESS_CALLBACK(1),

    /** 退款失败回调（事件源：渠道退款回调，渠道返回明确失败） */
    REFUND_FAIL_CALLBACK(2),

    /** 重试退款（事件源：管理端操作员，对 FAILED 的退款单重新发起） */
    RETRY_REFUND(3);

    /** 事件码 */
    private final int code;

    RefundEventEnum(int code) {
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
