package com.mall.order.statemachine;

import com.mall.common.enums.order.OrderStatusEnum;
import com.mall.order.DO.MallOrderDO;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 订单状态转移定义
 *
 * <p>目标状态用 {@link Function} 而非固定值，是为了支持
 * {@link OrderEventEnum#REFUND_FAIL} —— 它需要按退款发生前的订单状态动态回退
 * （设计文档 §6.3 中对应 PAID / WAIT_DELIVER / WAIT_RECEIVE / COMPLETED 四条规则）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 * @param targetResolver 目标状态解析器（可动态）
 * @param precondition   前置条件，不满足抛 A0703
 * @param postAction     后置动作（写 Outbox 等），在 Service 层事务内执行
 */
public record OrderTransition(
        Function<MallOrderDO, OrderStatusEnum> targetResolver,
        Predicate<MallOrderDO> precondition,
        Consumer<MallOrderDO> postAction
) {

    /**
     * 固定目标状态、无前置条件、无后置动作
     */
    public static OrderTransition to(OrderStatusEnum target) {
        return new OrderTransition(order -> target, order -> true, order -> { });
    }

    /**
     * 固定目标状态，带后置动作
     */
    public static OrderTransition to(OrderStatusEnum target, Consumer<MallOrderDO> postAction) {
        return new OrderTransition(order -> target, order -> true, postAction);
    }

    /**
     * 固定目标状态，带前置条件
     */
    public static OrderTransition to(OrderStatusEnum target, Predicate<MallOrderDO> precondition) {
        return new OrderTransition(order -> target, precondition, order -> { });
    }

    /**
     * 动态目标状态（按订单当前业务态解析），无前置条件
     */
    public static OrderTransition dynamic(Function<MallOrderDO, OrderStatusEnum> targetResolver) {
        return new OrderTransition(targetResolver, order -> true, order -> { });
    }
}