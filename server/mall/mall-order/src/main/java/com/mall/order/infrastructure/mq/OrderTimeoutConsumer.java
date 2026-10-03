package com.mall.order.infrastructure.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.constant.MqTopicConstants;
import com.mall.common.enums.order.OrderStatusEnum;
import com.mall.common.mq.MqDedupGuard;
import com.mall.order.DO.MallOrderDO;
import com.mall.order.infrastructure.outbox.OutboxPublisher;
import com.mall.order.mapper.MallOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付超时关单消费者
 *
 * <p>消费延迟消息 {@code mall:order:timeout}，关闭超时未支付订单（设计文档 §7.3）。</p>
 *
 * <p><b>双重校验</b>：除幂等去重外，还需确认订单确实处于 WAIT_PAY 且已到期——
 * 因为延迟消息可能在支付成功后仍被投递。</p>
 *
 * <p><b>竞态防护</b>：关单走 {@code MallOrderMapper.closeByTimeout}，其SQL 带
 * {@code WHERE order_status = 0}。若支付回调先到（状态已变 1），影响 0 行即跳过，
 * 不会误关已支付订单。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqTopicConstants.Order.TIMEOUT,
        consumerGroup = "mall-order-timeout-consumer"
)
public class OrderTimeoutConsumer implements RocketMQListener<MessageExt> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String CONSUMER_GROUP = "mall-order-timeout";

    private final MallOrderMapper orderMapper;
    private final OutboxPublisher outboxPublisher;
    private final MqDedupGuard dedupGuard;

    // 事务边界必须落在 onMessage（外部入口，经代理调用）：closeTimedOutOrder 是 self-invocation，
    // 给它加 @Transactional 不会经过代理、事务不生效。关单与 Outbox 落库必须同事务，
    // 否则 closeByTimeout 已提交而 publish 失败时，重投会因订单已是 CLOSED 直接跳过，
    // order:cancelled 永不投递、库存与券永不回补。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(MessageExt message) {
        String body = new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8);
        Map<String, Object> payload;
        try {
            payload = OBJECT_MAPPER.readValue(body, Map.class);
        } catch (Exception e) {
            log.error("超时消息解析失败，将由 RocketMQ 重试: msgId={}", message.getMsgId(), e);
            throw new IllegalStateException("消息体解析失败", e);
        }

        Object orderNo = payload.get("orderNo");
        if (orderNo == null) {
            log.error("超时消息缺少 orderNo，丢弃: msgId={}, body={}", message.getMsgId(), body);
            return;
        }

        if (!dedupGuard.tryDedup(message.getMsgId(), CONSUMER_GROUP)) {
            return;
        }

        try {
            closeTimedOutOrder(String.valueOf(orderNo));
        } catch (RuntimeException e) {
            // 先记原始异常，再尽力释放去重标记：否则 MQ 重投会被去重拦截，
            // 订单永远停在待支付且不会超时关闭（库存与券也不释放）
            log.error("超时关单失败，交由 MQ 重试: orderNo={}", orderNo, e);
            try {
                dedupGuard.release(message.getMsgId(), CONSUMER_GROUP);
            } catch (RuntimeException releaseError) {
                log.error("【需人工介入】释放去重标记失败，重投将被去重拦截: msgId={}",
                        message.getMsgId(), releaseError);
            }
            throw e;
        }
    }

    /**
     * 关闭超时未支付订单
     *
     * @param orderNo 订单号
     * @return true=本次关单成功；false=已支付/已关闭/不存在，跳过
     */
    public boolean closeTimedOutOrder(String orderNo) {
        MallOrderDO order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            log.warn("超时关单但订单不存在，跳过: orderNo={}", orderNo);
            return false;
        }
        // 双重校验：状态 + 是否真的到期
        if (order.getOrderStatus() != OrderStatusEnum.WAIT_PAY.getCode()) {
            log.info("订单非待支付状态，跳过关单: orderNo={}, status={}", orderNo, order.getOrderStatus());
            return false;
        }
        if (order.getPayExpireTime() != null && order.getPayExpireTime().isAfter(LocalDateTime.now())) {
            log.info("订单未到期，跳过关单: orderNo={}, payExpireTime={}", orderNo, order.getPayExpireTime());
            return false;
        }

        int affected = orderMapper.closeByTimeout(orderNo);
        if (affected == 0) {
            // 支付回调抢先一步，状态已变
            log.info("关单影响 0 行，订单已被支付或其他流程处理: orderNo={}", orderNo);
            return false;
        }

        // 发order:cancelled，由 mall-product / mall-marketing 消费释放库存与优惠券
        Map<String, Object> cancelledEvent = new LinkedHashMap<>();
        cancelledEvent.put("orderNo", orderNo);
        cancelledEvent.put("userId", order.getUserId());
        cancelledEvent.put("cancelReason", "PAY_TIMEOUT");
        outboxPublisher.publish(MqTopicConstants.Order.CANCELLED, "OrderCancelled",
                orderNo, cancelledEvent);

        log.info("超时关单成功: orderNo={}", orderNo);
        return true;
    }
}
