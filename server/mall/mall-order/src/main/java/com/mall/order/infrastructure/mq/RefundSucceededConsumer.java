package com.mall.order.infrastructure.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.constant.MqTopicConstants;
import com.mall.common.mq.MqDedupGuard;
import com.mall.order.service.AfterSaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 退款成功事件消费者
 *
 * <p>消费 {@code mall:refund:succeeded}，推进售后单完成并在退货退款场景回补库存
 * （设计文档 §7.3）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqTopicConstants.Payment.REFUND_SUCCEEDED,
        consumerGroup = "mall-order-refund-succeeded-consumer"
)
public class RefundSucceededConsumer implements RocketMQListener<MessageExt> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String CONSUMER_GROUP = "mall-order-refund-succeeded";

    private final AfterSaleService afterSaleService;
    private final MqDedupGuard dedupGuard;

    @Override
    public void onMessage(MessageExt message) {
        String body = new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8);
        Map<String, Object> payload;
        try {
            payload = OBJECT_MAPPER.readValue(body, Map.class);
        } catch (Exception e) {
            log.error("退款成功消息解析失败，将由 RocketMQ 重试: msgId={}", message.getMsgId(), e);
            throw new IllegalStateException("消息体解析失败", e);
        }

        Object afterSaleNo = payload.get("afterSaleNo");
        if (afterSaleNo == null) {
            log.error("退款成功消息缺少 afterSaleNo，丢弃: msgId={}, body={}", message.getMsgId(), body);
            return;
        }
        if (!dedupGuard.tryDedup(message.getMsgId(), CONSUMER_GROUP)) {
            return;
        }

        try {
            Object refundAmount = payload.get("refundAmount");
            afterSaleService.refundCallback(
                    String.valueOf(afterSaleNo),
                    refundAmount == null ? null : Long.valueOf(String.valueOf(refundAmount)));
            log.info("退款成功消费完成: afterSaleNo={}", afterSaleNo);
        } catch (RuntimeException e) {
            // 先记原始异常，再尽力释放去重标记：否则 MQ 重投会被去重拦截，
            // 售后单永远停在退款中、退货退款的库存也不回补
            log.error("退款成功消费失败，交由 MQ 重试: afterSaleNo={}", afterSaleNo, e);
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
