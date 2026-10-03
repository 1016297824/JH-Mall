package com.mall.order.infrastructure.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.constant.MqTopicConstants;
import com.mall.order.mapper.MallOutboxMapper;
import com.mall.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 支付成功事件消费者
 *
 * <p>消费 {@code mall:payment:paid}，推进订单 WAIT_PAY → PAID（设计文档 §7.3）。</p>
 *
 * <p>幂等两层保证：MqDedupGuard 的 Redis 去重（broker msgId）+ 状态机转移的 A0702兜底。
 * 用 {@link MessageExt} 而非 String，以便取到 broker 分配的 msgId——
 * 它在同一消息重投时保持稳定，且不污染业务 payload。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqTopicConstants.Payment.PAID,
        consumerGroup = "mall-order-payment-paid-consumer"
)
public class PaymentPaidConsumer implements RocketMQListener<MessageExt> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String CONSUMER_GROUP = "mall-order-payment-paid";

    /** 订单聚合类型，用于取消超时消息 */
    private static final String AGGREGATE_ORDER = "ORDER";

    private final OrderService orderService;
    private final MallOutboxMapper outboxMapper;
    private final MqDedupGuard dedupGuard;

    @Override
    public void onMessage(MessageExt message) {
        String body = new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8);
        Map<String, Object> payload;
        try {
            payload = OBJECT_MAPPER.readValue(body, Map.class);
        } catch (Exception e) {
            log.error("支付成功消息解析失败，将由RocketMQ 重试: msgId={}", message.getMsgId(), e);
            throw new IllegalStateException("消息体解析失败", e);
        }

        Object orderNo = payload.get("orderNo");
        if (orderNo == null) {
            log.error("支付成功消息缺少 orderNo，丢弃: msgId={}, body={}", message.getMsgId(), body);
            return;
        }

        if (!dedupGuard.tryDedup(message.getMsgId(), CONSUMER_GROUP)) {
            return;
        }

        try {
            // 推进订单状态；内部已处理重复回调
            orderService.payCallback(String.valueOf(orderNo));

            // 支付成功，取消尚未投递的支付超时延迟消息（设计文档 §5.9）
            int cancelled = outboxMapper.cancelPending(AGGREGATE_ORDER, String.valueOf(orderNo),
                    MqTopicConstants.Order.TIMEOUT);
            log.info("支付成功处理完成: orderNo={}, 取消超时消息={} 条", orderNo, cancelled);
        } catch (RuntimeException e) {
            // 先记原始异常，再尽力释放去重标记：否则 MQ 重投会被去重拦截，
            // 用户已付款但订单永远停在待支付
            log.error("支付成功处理失败，交由 MQ 重试: orderNo={}", orderNo, e);
            try {
                dedupGuard.release(message.getMsgId(), CONSUMER_GROUP);
            } catch (RuntimeException releaseError) {
                log.error("【需人工介入】释放去重标记失败，重投将被去重拦截: msgId={}",
                        message.getMsgId(), releaseError);
            }
            throw e;
        }
    }
}