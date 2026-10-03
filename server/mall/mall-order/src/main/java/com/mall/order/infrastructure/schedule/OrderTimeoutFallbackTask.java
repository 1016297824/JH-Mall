package com.mall.order.infrastructure.schedule;

import com.mall.common.constant.MqTopicConstants;
import com.mall.order.DO.MallOrderDO;
import com.mall.order.config.MallOrderConfigProperties;
import com.mall.order.infrastructure.mq.OrderTimeoutConsumer;
import com.mall.order.infrastructure.outbox.OutboxPublisher;
import com.mall.order.mapper.MallOrderMapper;
import com.mall.order.statemachine.OrderEventEnum;
import com.mall.order.statemachine.OrderStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单兜底定时任务
 *
 * <p>两类兜底（设计文档 §5.9 / §5.10）：</p>
 * <ol>
 *   <li><b>超时关单兜底</b>——延迟消息可能丢失，每日02:00 扫描已到期仍待支付的订单补关。
 *       与 {@link OrderTimeoutConsumer} 共用关单逻辑，幂等由乐观锁保证。</li>
 *   <li><b>自动确认收货</b>——发货后超过 {@code mall.order.auto-receive-days} 天
 *       用户未确认则自动完成。</li>
 * </ol>
 *
 * <p>单条失败不中断整批，逐条兜住。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutFallbackTask {

    /** 单批处理条数 */
    private static final int BATCH_SIZE = 200;

    private final MallOrderMapper orderMapper;
    private final OrderTimeoutConsumer timeoutConsumer;
    private final OrderStateMachine stateMachine;
    private final OutboxPublisher outboxPublisher;
    private final MallOrderConfigProperties config;

    /**
     * 超时关单兜底日扫
     *
     * <p>cron 取 {@code mall.order.timeout-fallback-cron}，默认 02:00。</p>
     */
    @Scheduled(cron = "${mall.order.timeout-fallback-cron:0 0 2 * * ?}")
    public void closeTimedOutOrders() {
        List<MallOrderDO> orders = orderMapper.selectTimeoutOrders(BATCH_SIZE);
        if (orders.isEmpty()) {
            return;
        }
        log.info("超时关单兜底扫描: 待处理={}", orders.size());
        int closed = 0;
        for (MallOrderDO order : orders) {
            try {
                if (timeoutConsumer.closeTimedOutOrder(order.getOrderNo())) {
                    closed++;
                }
            } catch (Exception e) {
                log.error("超时关单兜底处理失败: orderNo={}", order.getOrderNo(), e);
            }
        }
        log.info("超时关单兜底完成: 扫描={}, 成功={}", orders.size(), closed);
    }

    /**
     * 自动确认收货日扫
     *
     * <p>发货后超过配置天数（默认 15 天）仍为待收货的订单自动流转为已完成。</p>
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void autoConfirmReceipts() {
        int days = config.getAutoReceiveDays();
        List<MallOrderDO> orders = orderMapper.selectAutoConfirmOrders(days, BATCH_SIZE);
        if (orders.isEmpty()) {
            return;
        }
        log.info("自动确认收货扫描: 天数={}, 待处理={}", days, orders.size());
        int completed = 0;
        for (MallOrderDO order : orders) {
            try {
                if (autoConfirmReceipt(order)) {
                    completed++;
                }
            } catch (Exception e) {
                log.error("自动确认收货失败: orderNo={}", order.getOrderNo(), e);
            }
        }
        log.info("自动确认收货完成: 扫描={}, 成功={}", orders.size(), completed);
    }

    /**
     * 单笔自动确认收货
     *
     * @param order 订单
     * @return true=本次完成；false=已被用户确认或其他流程处理
     */
    private boolean autoConfirmReceipt(MallOrderDO order) {
        Integer originStatus = order.getOrderStatus();
        Integer version = order.getVersion();

        stateMachine.transition(order, OrderEventEnum.CONFIRM_RECEIPT);

        int affected = orderMapper.updateStatusCas(order.getOrderNo(), order.getOrderStatus(),
                originStatus, version, null);
        if (affected == 0) {
            log.info("自动确认收货影响 0 行，订单已被用户确认: orderNo={}", order.getOrderNo());
            return false;
        }

        Map<String, Object> completedEvent = new LinkedHashMap<>();
        completedEvent.put("orderNo", order.getOrderNo());
        completedEvent.put("userId", order.getUserId());
        completedEvent.put("autoConfirmed", true);
        // 与用户手动确认收货保持同一套积分/成长值字段（缺失时 mall-user 会静默不发积分）
        long payAmount = order.getPayAmount() == null ? 0L : order.getPayAmount();
        completedEvent.put("orderAmount", payAmount);
        completedEvent.put("points", payAmount / 100);
        outboxPublisher.publish(MqTopicConstants.Order.COMPLETED, "OrderCompleted",
                order.getOrderNo(), completedEvent);
        return true;
    }
}