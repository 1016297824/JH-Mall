package com.mall.order.statemachine;

import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.order.OrderStatusEnum;
import com.mall.common.exception.BusinessException;
import com.mall.order.DO.MallOrderDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单状态机
 *
 * <p>订单状态变更的<b>唯一入口</b>。设计约束见
 * {@code docs/design/12_mall-order详细设计.md} §6.4：</p>
 * <ul>
 *   <li>不操作 DB、不持有 Mapper、不管理事务 —— 只做转移合法性判断与前置条件校验</li>
 *   <li>后置动作通过回调触发 —— 实际 Outbox 写入由 Service 层在事务内完成</li>
 *   <li>无状态单例，Map 只读，{@link #transition} 不修改共享状态，线程安全</li>
 * </ul>
 *
 * <p>转移矩阵严格对应设计文档 §6.3（表17 行 → 14 个「状态×事件」条目，
 * 其中 {@code REFUND_FAIL} 动态回退到 4 个可能目标）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Slf4j
@Component
public class OrderStateMachine {

    /** 售后可申请期限（天），对应 mall.order.refund-days */
    private static final int REFUND_DAYS = 7;

    /** 取消 / 退款后进入终态的等待天数 */
    private static final int FINALIZE_DAYS = 7;

    /** 状态码 → 枚举 */
    private static final Map<Integer, OrderStatusEnum> STATUS_CODE_MAP = new HashMap<>();

    static {
        for (OrderStatusEnum status : OrderStatusEnum.values()) {
            STATUS_CODE_MAP.put(status.getCode(), status);
        }
    }

    /** 转移矩阵：当前状态 → 触发事件 → 转移定义 */
    private final Map<OrderStatusEnum, Map<OrderEventEnum, OrderTransition>> transitions =
            new EnumMap<>(OrderStatusEnum.class);

    public OrderStateMachine() {
        initTransitions();
    }

    /**
     * 执行状态转移
     *
     * @param order 当前订单，状态与更新时间会被原地修改
     * @param event 触发事件
     * @return 目标状态
     * @throws BusinessException A0702 转移矩阵无匹配；A0703 前置条件不满足
     */
    public OrderStatusEnum transition(MallOrderDO order, OrderEventEnum event) {
        OrderStatusEnum current = STATUS_CODE_MAP.get(order.getOrderStatus());
        if (current == null) {
            log.error("订单状态码非法: orderStatus={}, orderNo={}", order.getOrderStatus(), order.getOrderNo());
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        OrderTransition transition = transitions
                .getOrDefault(current, Map.of())
                .get(event);

        if (transition == null) {
            log.warn("非法状态转移: {} --{}--> ?, orderNo={}", current, event, order.getOrderNo());
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        if (!transition.precondition().test(order)) {
            log.warn("前置条件不满足: {} --{}--> ?, orderNo={}", current, event, order.getOrderNo());
            throw new BusinessException(ErrorCode.ORDER_ACTION_DENIED);
        }

        OrderStatusEnum target = transition.targetResolver().apply(order);
        transition.postAction().accept(order);
        order.setOrderStatus(target.getCode());
        order.setUpdateTime(LocalDateTime.now());

        log.info("状态转移: {} --{}--> {}, orderNo={}", current, event, target, order.getOrderNo());
        return target;
    }

    /**
     * 该状态下是否允许此事件
     *
     * <p>供管理端 / 前端做按钮级校验，避免展示非法操作入口。</p>
     *
     * @param from  当前状态
     * @param event 事件
     * @return 允许返回 true
     */
    public boolean canTransit(OrderStatusEnum from, OrderEventEnum event) {
        return transitions.getOrDefault(from, Map.of()).containsKey(event);
    }

    /**
     * 解析退款失败后的回退目标状态
     *
     * <p>设计文档 §6.3 中 {@code REFUND_FAIL} 需回退到退款发生前的状态
     * （PAID / WAIT_DELIVER / WAIT_RECEIVE / COMPLETED 之一）。</p>
     *
     * <p>优先读 {@code pre_refund_status}（进入 REFUNDING 时由本类写入）。
     * 该列为空时回退到按时间戳推断的兼容逻辑，仅用于迁移前的历史数据。</p>
     */
    private OrderStatusEnum resolvePreRefundStatus(MallOrderDO order) {
        Integer preRefundStatus = order.getPreRefundStatus();
        if (preRefundStatus != null) {
            OrderStatusEnum resolved = STATUS_CODE_MAP.get(preRefundStatus);
            if (resolved != null) {
                return resolved;
            }
            log.warn("pre_refund_status 非法，回退到时间戳推断: {}, orderNo={}",
                    preRefundStatus, order.getOrderNo());
        }
        if (order.getCompleteTime() != null) {
            return OrderStatusEnum.COMPLETED;
        }
        if (order.getDeliveryTime() != null) {
            return OrderStatusEnum.WAIT_DELIVER;
        }
        return OrderStatusEnum.PAID;
    }

    /**
     * 进入退款中时记录退款前状态，供退款失败时精确回退
     */
    private void recordPreRefundStatus(MallOrderDO order, OrderStatusEnum current) {
        order.setPreRefundStatus(current.getCode());
    }

    /**
     * 是否满足强制取消条件（设计文档 {@code 03_04_系统详细设计-状态机详细设计.md}）
     *
     * <p>强制取消只面向<b>已支付未发货且实付金额为 0</b> 的异常订单（如支付通道测试单）。
     * 有金额的订单必须走 {@code PAID -> REFUNDING} 退款流程——强制取消不会调渠道原路退款，
     * 放行有金额的订单会让钱凭空消失。</p>
     */
    private boolean forceCancelAllowed(MallOrderDO order) {
        return order.getDeliveryTime() == null
                && order.getPayAmount() != null
                && order.getPayAmount() == 0L;
    }

    /**
     * 物流信息是否已填写（发货前置条件）
     */
    private boolean logisticsFilled(MallOrderDO order) {
        return order.getLogisticsCompany() != null && !order.getLogisticsCompany().isBlank()
                && order.getLogisticsNo() != null && !order.getLogisticsNo().isBlank();
    }

    /**
     * 是否仍在售后期限内
     */
    private boolean withinRefundPeriod(MallOrderDO order) {
        if (order.getCompleteTime() == null) {
            return false;
        }
        return order.getCompleteTime().plusDays(REFUND_DAYS).isAfter(LocalDateTime.now());
    }

    /**
     * 是否已过终态清理等待期
     *
     * <p>{@code mall_order} 无cancel_time 之外的退款完成时间字段，
     * 退款场景回退到 update_time 作为近似。</p>
     */
    private boolean finalized(MallOrderDO order) {
        LocalDateTime base = order.getUpdateTime();
        if (base == null) {
            return false;
        }
        return base.plusDays(FINALIZE_DAYS).isBefore(LocalDateTime.now());
    }

    /**
     * 初始化转移矩阵 —— 严格对应设计文档 §6.3
     */
    private void initTransitions() {

        // ── WAIT_PAY ──
        // 前置「支付金额 ≥ 应付金额」需支付单数据，由 Service / Consumer 层校验
        put(OrderStatusEnum.WAIT_PAY, OrderEventEnum.PAY_SUCCESS,
                new OrderTransition(
                        order -> OrderStatusEnum.PAID,
                        order -> order.getPayExpireTime().isAfter(LocalDateTime.now()),
                        order -> { }));
        put(OrderStatusEnum.WAIT_PAY, OrderEventEnum.USER_CANCEL,
                OrderTransition.to(OrderStatusEnum.CANCELLED));
        put(OrderStatusEnum.WAIT_PAY, OrderEventEnum.PAY_TIMEOUT,
                new OrderTransition(
                        order -> OrderStatusEnum.CLOSED,
                        order -> order.getPayExpireTime().isBefore(LocalDateTime.now()),
                        order -> { }));

        // ── PAID ──
        // 前置「物流单号 + 公司已填写」（V1.0.6 补齐字段后可下沉到本层校验）
        put(OrderStatusEnum.PAID, OrderEventEnum.SELLER_DELIVER,
                new OrderTransition(
                        order -> OrderStatusEnum.WAIT_DELIVER,
                        this::logisticsFilled,
                        order -> { }));
        put(OrderStatusEnum.PAID, OrderEventEnum.FORCE_CANCEL,
                new OrderTransition(
                        order -> OrderStatusEnum.CANCELLED,
                        this::forceCancelAllowed,
                        order -> { }));
        // 前置「订单未发货」；进入退款中时记录退款前状态
        put(OrderStatusEnum.PAID, OrderEventEnum.REFUND_ONLY,
                new OrderTransition(
                        order -> OrderStatusEnum.REFUNDING,
                        order -> order.getDeliveryTime() == null,
                        order -> recordPreRefundStatus(order, OrderStatusEnum.PAID)));

        // ── WAIT_DELIVER ──
        put(OrderStatusEnum.WAIT_DELIVER, OrderEventEnum.LOGISTICS_PICK,
                OrderTransition.to(OrderStatusEnum.WAIT_RECEIVE));

        // ── WAIT_RECEIVE ──
        put(OrderStatusEnum.WAIT_RECEIVE, OrderEventEnum.CONFIRM_RECEIPT,
                OrderTransition.to(OrderStatusEnum.COMPLETED));
        put(OrderStatusEnum.WAIT_RECEIVE, OrderEventEnum.RETURN_REFUND,
                new OrderTransition(
                        order -> OrderStatusEnum.REFUNDING,
                        order -> true,
                        order -> recordPreRefundStatus(order, OrderStatusEnum.WAIT_RECEIVE)));

        // ── COMPLETED ──
        // 前置「收货后 7 天内」
        put(OrderStatusEnum.COMPLETED, OrderEventEnum.AFTER_SALE,
                new OrderTransition(
                        order -> OrderStatusEnum.REFUNDING,
                        this::withinRefundPeriod,
                        order -> recordPreRefundStatus(order, OrderStatusEnum.COMPLETED)));

        // ── REFUNDING ──
        put(OrderStatusEnum.REFUNDING, OrderEventEnum.REFUND_SUCCESS,
                OrderTransition.to(OrderStatusEnum.REFUNDED));
        // 退款失败 → 动态回退到退款前状态（设计文档 §6.3 的 4 条规则）
        put(OrderStatusEnum.REFUNDING, OrderEventEnum.REFUND_FAIL,
                OrderTransition.dynamic(this::resolvePreRefundStatus));

        // ── 终态清理 ──
        put(OrderStatusEnum.CANCELLED, OrderEventEnum.PAY_TIMEOUT,
                new OrderTransition(
                        order -> OrderStatusEnum.CLOSED,
                        this::finalized,
                        order -> { }));
        put(OrderStatusEnum.REFUNDED, OrderEventEnum.PAY_TIMEOUT,
                new OrderTransition(
                        order -> OrderStatusEnum.CLOSED,
                        this::finalized,
                        order -> { }));
    }

    private void put(OrderStatusEnum from, OrderEventEnum event, OrderTransition transition) {
        transitions
                .computeIfAbsent(from, k -> new EnumMap<>(OrderEventEnum.class))
                .put(event, transition);
    }
}