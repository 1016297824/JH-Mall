package com.mall.marketing.statemachine;

import com.mall.common.enums.marketing.CouponRecordStatusEnum;
import com.mall.marketing.DO.MallCouponRecordDO;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 优惠券记录状态转移定义
 *
 * <p>与 {@code com.mall.order.statemachine.OrderTransition} 同构：
 * 目标状态用 {@link Function} 表达以保留扩展空间（当前所有转移均为固定目标），
 * 前置条件不满足抛 A0612，后置动作只做<b>内存态</b>字段赋值。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 * @param targetResolver 目标状态解析器
 * @param precondition   前置条件，不满足抛 A0612
 * @param postAction     后置动作（内存态字段赋值），落库由 Service 层 CAS 完成
 */
public record CouponTransition(
        Function<MallCouponRecordDO, CouponRecordStatusEnum> targetResolver,
        Predicate<MallCouponRecordDO> precondition,
        Consumer<MallCouponRecordDO> postAction
) {

    /**
     * 固定目标状态、无前置条件、无后置动作
     *
     * @param target 目标状态
     * @return 转移定义
     */
    public static CouponTransition to(CouponRecordStatusEnum target) {
        return new CouponTransition(record -> target, record -> true, record -> { });
    }

    /**
     * 固定目标状态，带后置动作
     *
     * @param target     目标状态
     * @param postAction 后置动作
     * @return 转移定义
     */
    public static CouponTransition to(CouponRecordStatusEnum target, Consumer<MallCouponRecordDO> postAction) {
        return new CouponTransition(record -> target, record -> true, postAction);
    }

    /**
     * 固定目标状态，带前置条件与后置动作
     *
     * @param target       目标状态
     * @param precondition 前置条件
     * @param postAction   后置动作
     * @return 转移定义
     */
    public static CouponTransition to(CouponRecordStatusEnum target,
                                      Predicate<MallCouponRecordDO> precondition,
                                      Consumer<MallCouponRecordDO> postAction) {
        return new CouponTransition(record -> target, precondition, postAction);
    }
}
