package com.mall.marketing.infrastructure.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.constant.MqTopicConstants;
import com.mall.marketing.service.CouponClaimService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 订单支付成功事件消费者
 *
 * <p>消费 {@code mall:order:paid}，核销该订单下所有锁定券（LOCKED → USED），
 * 并写 Outbox 记录核销事实（设计文档 §5.2 / §7.2）。</p>
 *
 * <p>幂等两层保证：{@link MqDedupGuard} 的 Redis 去重（broker msgId）+
 * Service 层「按 orderNo 查 LOCKED 记录」的天然幂等。
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
        topic = MqTopicConstants.Order.PAID,
        consumerGroup = "mall-marketing-order-paid-consumer"
)
public class OrderPaidConsumer implements RocketMQListener<MessageExt> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 去重维度：与 consumerGroup 区分开，便于按业务语义检索 */
    private static final String DEDUP_GROUP = "mall-marketing-order-paid";

    private final CouponClaimService couponClaimService;

    private final MqDedupGuard dedupGuard;

    @Override
    public void onMessage(MessageExt message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        Map<String, Object> payload;
        try {
            payload = OBJECT_MAPPER.readValue(body, Map.class);
        } catch (Exception e) {
            log.error("支付成功消息解析失败，将由 RocketMQ 重试: msgId={}", message.getMsgId(), e);
            throw new IllegalStateException("消息体解析失败", e);
        }

        Object orderNo = payload.get("orderNo");
        if (orderNo == null) {
            log.error("支付成功消息缺少 orderNo，丢弃: msgId={}, body={}", message.getMsgId(), body);
            return;
        }

        if (!dedupGuard.tryDedup(message.getMsgId(), DEDUP_GROUP)) {
            return;
        }

        couponClaimService.useCoupon(String.valueOf(orderNo));
        log.info("支付成功券核销处理完成: orderNo={}", orderNo);
    }
}
