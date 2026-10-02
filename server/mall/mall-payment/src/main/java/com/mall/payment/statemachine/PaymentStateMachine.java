package com.mall.payment.statemachine;

import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.payment.PaymentStatusEnum;
import com.mall.common.enums.payment.RefundStatusEnum;
import com.mall.common.exception.BusinessException;
import com.mall.payment.DO.MallPaymentDO;
import com.mall.payment.DO.MallRefundDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付单 / 退款单状态机
 *
 * <p>支付与退款状态变更的<b>唯一入口</b>。约束与 {@code OrderStateMachine}、
 * {@code CouponStateMachine} 保持一致：</p>
 * <ul>
 *   <li>不操作 DB、不持有 Mapper、不管理事务 —— 只做转移合法性判断与前置条件校验</li>
 *   <li>只修改传入对象的内存态字段，落库由 Service 层 CAS UPDATE 完成</li>
 *   <li>无状态单例，Map 只读，转移方法不修改共享状态，线程安全</li>
 * </ul>
 *
 * <p>转移矩阵严格对应设计文档 {@code docs/design/13_mall-payment详细设计.md} §7.2：
 * 支付单 6 条真实转移 + 退款单 3 条。未定义转移与状态码非法统一抛
 * {@link ErrorCode#ORDER_STATUS_ERROR}，前置条件不满足抛
 * {@link ErrorCode#ORDER_ACTION_DENIED}。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Slf4j
@Component
public class PaymentStateMachine {

    /** 状态码 → 支付状态枚举 */
    private static final Map<Integer, PaymentStatusEnum> PAYMENT_STATUS_CODE_MAP = new HashMap<>();

    /** 状态码 → 退款状态枚举 */
    private static final Map<Integer, RefundStatusEnum> REFUND_STATUS_CODE_MAP = new HashMap<>();

    static {
        for (PaymentStatusEnum status : PaymentStatusEnum.values()) {
            PAYMENT_STATUS_CODE_MAP.put(status.getCode(), status);
        }
        for (RefundStatusEnum status : RefundStatusEnum.values()) {
            REFUND_STATUS_CODE_MAP.put(status.getCode(), status);
        }
    }

    /** 支付单转移矩阵：当前状态 → 触发事件 → 转移定义 */
    private final Map<PaymentStatusEnum, Map<PaymentEventEnum, PaymentTransition>> paymentTransitions =
            new EnumMap<>(PaymentStatusEnum.class);

    /** 退款单转移矩阵：当前状态 → 触发事件 → 转移定义 */
    private final Map<RefundStatusEnum, Map<RefundEventEnum, RefundTransition>> refundTransitions =
            new EnumMap<>(RefundStatusEnum.class);

    /**
     * 构造状态机并初始化两套转移矩阵
     */
    public PaymentStateMachine() {
        initPaymentTransitions();
        initRefundTransitions();
    }

    /**
     * 执行支付单状态转移
     *
     * @param payment 当前支付单，状态与相关时间字段会被原地修改
     * @param event   触发事件
     * @return 目标状态
     * @throws BusinessException A0702 状态码非法或转移矩阵无匹配；A0703 前置条件不满足
     */
    public PaymentStatusEnum transition(MallPaymentDO payment, PaymentEventEnum event) {
        PaymentStatusEnum current = PAYMENT_STATUS_CODE_MAP.get(payment.getPaymentStatus());
        if (current == null) {
            log.error("支付单状态码非法: paymentStatus={}, paymentNo={}",
                    payment.getPaymentStatus(), payment.getPaymentNo());
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        PaymentTransition transition = paymentTransitions
                .getOrDefault(current, Map.of())
                .get(event);

        if (transition == null) {
            log.warn("非法状态转移: {} --{}--> ?, paymentNo={}", current, event, payment.getPaymentNo());
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        if (!transition.precondition().test(payment)) {
            log.warn("前置条件不满足: {} --{}--> ?, paymentNo={}, expireTime={}",
                    current, event, payment.getPaymentNo(), payment.getExpireTime());
            throw new BusinessException(ErrorCode.ORDER_ACTION_DENIED);
        }

        PaymentStatusEnum target = transition.targetResolver().apply(payment);
        transition.postAction().accept(payment);
        payment.setPaymentStatus(target.getCode());
        payment.setUpdateTime(LocalDateTime.now());

        log.info("支付单状态转移: {} --{}--> {}, paymentNo={}", current, event, target, payment.getPaymentNo());
        return target;
    }

    /**
     * 执行退款单状态转移
     *
     * @param refund 当前退款单，状态与相关时间字段会被原地修改
     * @param event  触发事件
     * @return 目标状态
     * @throws BusinessException A0702 状态码非法或转移矩阵无匹配；A0703 前置条件不满足
     */
    public RefundStatusEnum refundTransition(MallRefundDO refund, RefundEventEnum event) {
        RefundStatusEnum current = REFUND_STATUS_CODE_MAP.get(refund.getRefundStatus());
        if (current == null) {
            log.error("退款单状态码非法: refundStatus={}, refundNo={}",
                    refund.getRefundStatus(), refund.getRefundNo());
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        RefundTransition transition = refundTransitions
                .getOrDefault(current, Map.of())
                .get(event);

        if (transition == null) {
            log.warn("非法退款转移: {} --{}--> ?, refundNo={}", current, event, refund.getRefundNo());
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        if (!transition.precondition().test(refund)) {
            log.warn("退款前置条件不满足: {} --{}--> ?, refundNo={}", current, event, refund.getRefundNo());
            throw new BusinessException(ErrorCode.ORDER_ACTION_DENIED);
        }

        RefundStatusEnum target = transition.targetResolver().apply(refund);
        transition.postAction().accept(refund);
        refund.setRefundStatus(target.getCode());
        refund.setUpdateTime(LocalDateTime.now());

        log.info("退款单状态转移: {} --{}--> {}, refundNo={}", current, event, target, refund.getRefundNo());
        return target;
    }

    /**
     * 支付单在该状态下是否允许此事件
     *
     * <p>只判断「该状态 + 该事件」是否已定义于转移矩阵，不评估前置条件。</p>
     *
     * @param from  当前状态
     * @param event 事件
     * @return 允许返回 true
     */
    public boolean canTransit(PaymentStatusEnum from, PaymentEventEnum event) {
        return paymentTransitions.getOrDefault(from, Map.of()).containsKey(event);
    }

    /**
     * 退款单在该状态下是否允许此事件
     *
     * <p>只判断「该状态 + 该事件」是否已定义于转移矩阵，不评估前置条件。</p>
     *
     * @param from  当前状态
     * @param event 事件
     * @return 允许返回 true
     */
    public boolean canRefundTransit(RefundStatusEnum from, RefundEventEnum event) {
        return refundTransitions.getOrDefault(from, Map.of()).containsKey(event);
    }

    /**
     * 支付单是否已过期（支付超时关闭前置条件）
     *
     * @param payment 支付单
     * @return 逾期且存在过期时间返回 true
     */
    private boolean paymentExpired(MallPaymentDO payment) {
        return payment.getExpireTime() != null
                && payment.getExpireTime().isBefore(LocalDateTime.now());
    }

    /**
     * 初始化支付单转移矩阵 —— 严格对应设计文档 §7.2.1（6 条真实转移）
     */
    private void initPaymentTransitions() {

        // ── UNPAID ──
        putPayment(PaymentStatusEnum.UNPAID, PaymentEventEnum.PAY_SUCCESS_CALLBACK,
                PaymentTransition.to(PaymentStatusEnum.PAID,
                        payment -> payment.setPaySuccessTime(LocalDateTime.now())));
        putPayment(PaymentStatusEnum.UNPAID, PaymentEventEnum.PAY_FAIL_CALLBACK,
                PaymentTransition.to(PaymentStatusEnum.FAILED));
        // 前置：已过 expire_time 才允许关单，防止定时任务误关未过期支付单
        putPayment(PaymentStatusEnum.UNPAID, PaymentEventEnum.PAY_TIMEOUT_CLOSE,
                PaymentTransition.to(PaymentStatusEnum.CLOSED,
                        this::paymentExpired,
                        payment -> { }));

        // ── PAID ──
        putPayment(PaymentStatusEnum.PAID, PaymentEventEnum.REFUND_START,
                PaymentTransition.to(PaymentStatusEnum.REFUNDING));

        // ── REFUNDING ──
        putPayment(PaymentStatusEnum.REFUNDING, PaymentEventEnum.REFUND_SUCCESS_CALLBACK,
                PaymentTransition.to(PaymentStatusEnum.REFUNDED));
        // 退款失败回退到退款前状态 PAID
        putPayment(PaymentStatusEnum.REFUNDING, PaymentEventEnum.REFUND_FAIL_CALLBACK,
                PaymentTransition.to(PaymentStatusEnum.PAID));
    }

    /**
     * 初始化退款单转移矩阵 —— 严格对应设计文档 §7.2.2（3 条真实转移）
     */
    private void initRefundTransitions() {

        // ── PROCESSING ──
        putRefund(RefundStatusEnum.PROCESSING, RefundEventEnum.REFUND_SUCCESS_CALLBACK,
                RefundTransition.to(RefundStatusEnum.SUCCESS,
                        refund -> refund.setRefundSuccessTime(LocalDateTime.now())));
        putRefund(RefundStatusEnum.PROCESSING, RefundEventEnum.REFUND_FAIL_CALLBACK,
                RefundTransition.to(RefundStatusEnum.FAILED));

        // ── FAILED ──
        putRefund(RefundStatusEnum.FAILED, RefundEventEnum.RETRY_REFUND,
                RefundTransition.to(RefundStatusEnum.PROCESSING));
    }

    /**
     * 注册一条支付单转移
     *
     * @param from       当前状态
     * @param event      触发事件
     * @param transition 转移定义
     */
    private void putPayment(PaymentStatusEnum from, PaymentEventEnum event, PaymentTransition transition) {
        paymentTransitions
                .computeIfAbsent(from, k -> new EnumMap<>(PaymentEventEnum.class))
                .put(event, transition);
    }

    /**
     * 注册一条退款单转移
     *
     * @param from       当前状态
     * @param event      触发事件
     * @param transition 转移定义
     */
    private void putRefund(RefundStatusEnum from, RefundEventEnum event, RefundTransition transition) {
        refundTransitions
                .computeIfAbsent(from, k -> new EnumMap<>(RefundEventEnum.class))
                .put(event, transition);
    }
}
