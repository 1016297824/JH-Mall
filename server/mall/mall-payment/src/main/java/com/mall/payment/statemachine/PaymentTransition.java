package com.mall.payment.statemachine;

import com.mall.common.enums.payment.PaymentStatusEnum;
import com.mall.payment.DO.MallPaymentDO;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 支付单状态转移定义
 *
 * <p>与 {@code com.mall.order.statemachine.OrderTransition}、
 * {@code com.mall.marketing.statemachine.CouponTransition} 同构：
 * 目标状态用 {@link Function} 表达以保留扩展空间（当前所有转移均为固定目标），
 * 前置条件不满足抛业务异常，后置动作只做<b>内存态</b>字段赋值。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 * @param targetResolver 目标状态解析器
 * @param precondition   前置条件
 * @param postAction     后置动作（内存态字段赋值），落库由 Service 层 CAS 完成
 */
public record PaymentTransition(
        Function<MallPaymentDO, PaymentStatusEnum> targetResolver,
        Predicate<MallPaymentDO> precondition,
        Consumer<MallPaymentDO> postAction
) {

    /**
     * 固定目标状态、无前置条件、无后置动作
     *
     * @param target 目标状态
     * @return 转移定义
     */
    public static PaymentTransition to(PaymentStatusEnum target) {
        return new PaymentTransition(payment -> target, payment -> true, payment -> { });
    }

    /**
     * 固定目标状态，带后置动作
     *
     * @param target     目标状态
     * @param postAction 后置动作
     * @return 转移定义
     */
    public static PaymentTransition to(PaymentStatusEnum target, Consumer<MallPaymentDO> postAction) {
        return new PaymentTransition(payment -> target, payment -> true, postAction);
    }

    /**
     * 固定目标状态，带前置条件与后置动作
     *
     * @param target       目标状态
     * @param precondition 前置条件
     * @param postAction   后置动作
     * @return 转移定义
     */
    public static PaymentTransition to(PaymentStatusEnum target,
                                       Predicate<MallPaymentDO> precondition,
                                       Consumer<MallPaymentDO> postAction) {
        return new PaymentTransition(payment -> target, precondition, postAction);
    }
}
