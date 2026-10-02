package com.mall.payment.statemachine;

import com.mall.common.enums.payment.RefundStatusEnum;
import com.mall.payment.DO.MallRefundDO;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 退款单状态转移定义
 *
 * <p>与 {@link PaymentTransition} 同构，作用于 {@code MallRefundDO}。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 * @param targetResolver 目标状态解析器
 * @param precondition   前置条件
 * @param postAction     后置动作（内存态字段赋值），落库由 Service 层 CAS 完成
 */
public record RefundTransition(
        Function<MallRefundDO, RefundStatusEnum> targetResolver,
        Predicate<MallRefundDO> precondition,
        Consumer<MallRefundDO> postAction
) {

    /**
     * 固定目标状态、无前置条件、无后置动作
     *
     * @param target 目标状态
     * @return 转移定义
     */
    public static RefundTransition to(RefundStatusEnum target) {
        return new RefundTransition(refund -> target, refund -> true, refund -> { });
    }

    /**
     * 固定目标状态，带后置动作
     *
     * @param target     目标状态
     * @param postAction 后置动作
     * @return 转移定义
     */
    public static RefundTransition to(RefundStatusEnum target, Consumer<MallRefundDO> postAction) {
        return new RefundTransition(refund -> target, refund -> true, postAction);
    }

    /**
     * 固定目标状态，带前置条件与后置动作
     *
     * @param target       目标状态
     * @param precondition 前置条件
     * @param postAction   后置动作
     * @return 转移定义
     */
    public static RefundTransition to(RefundStatusEnum target,
                                      Predicate<MallRefundDO> precondition,
                                      Consumer<MallRefundDO> postAction) {
        return new RefundTransition(refund -> target, precondition, postAction);
    }
}
