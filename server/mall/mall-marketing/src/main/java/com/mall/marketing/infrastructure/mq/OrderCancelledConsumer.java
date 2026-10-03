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
 * 订单取消事件消费者
 *
 * <p>消费 {@code mall:order:cancelled}（订单取消 / 超时关闭都会投递），
 * 释放该订单下所有锁定券（LOCKED → RELEASED）并回补券定义库存
 * （设计文档 §5.3 / §7.2）。</p>
 *
 * <p>幂等两层保证：{@link MqDedupGuard} 的 Redis 去重 + Service 层
 * 「按 orderNo 查 LOCKED 记录、以 CAS 影响行数决定是否回补库存」的天然幂等。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqTopicConstants.Order.CANCELLED,
        consumerGroup = "mall-marketing-order-cancelled-consumer"
)
public class OrderCancelledConsumer implements RocketMQListener<MessageExt> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 去重维度：与 consumerGroup 区分开，便于按业务语义检索 */
    private static final String DEDUP_GROUP = "mall-marketing-order-cancelled";

    private final CouponClaimService couponClaimService;

    private final MqDedupGuard dedupGuard;

    @Override
    public void onMessage(MessageExt message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        Map<String, Object> payload;
        try {
            payload = OBJECT_MAPPER.readValue(body, Map.class);
        } catch (Exception e) {
            log.error("订单取消消息解析失败，将由 RocketMQ 重试: msgId={}", message.getMsgId(), e);
            throw new IllegalStateException("消息体解析失败", e);
        }

        Object orderNo = payload.get("orderNo");
        if (orderNo == null) {
            log.error("订单取消消息缺少 orderNo，丢弃: msgId={}, body={}", message.getMsgId(), body);
            return;
        }

        if (!dedupGuard.tryDedup(message.getMsgId(), DEDUP_GROUP)) {
            return;
        }

        try {
            couponClaimService.releaseCoupon(String.valueOf(orderNo));
            log.info("订单取消券释放处理完成: orderNo={}", orderNo);
        } catch (RuntimeException e) {
            // 先记原始异常，再尽力释放去重标记：否则 MQ 重投会被去重拦截，券永久锁死
            log.error("订单取消券释放失败，交由 MQ 重试: orderNo={}", orderNo, e);
            try {
                dedupGuard.release(message.getMsgId(), DEDUP_GROUP);
            } catch (RuntimeException releaseError) {
                log.error("【需人工介入】释放去重标记失败，重投将被去重拦截: msgId={}",
                        message.getMsgId(), releaseError);
            }
            throw e;
        }
    }
}
