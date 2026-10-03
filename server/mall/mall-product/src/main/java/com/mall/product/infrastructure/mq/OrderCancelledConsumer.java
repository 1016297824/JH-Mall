package com.mall.product.infrastructure.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.constant.MqTopicConstants;
import com.mall.product.service.IStockService;
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
 * <p>消费 {@code mall:order:cancelled}（用户主动取消与超时关单都会投递），
 * 释放该订单预扣的库存（设计文档 §5.7 补偿）。</p>
 *
 * <p><b>为什么必须存在</b>：{@code OrderServiceImpl.cancelOrder} 只做状态流转与发消息，
 * 并不直接释放库存；下单流程里的 {@code compensate()} 只覆盖「下单失败回滚」场景。
 * 若本消费者不接线，用户取消订单后 Redis 预扣记录与 DB 库存占用将<b>永不回补</b>。</p>
 *
 * <p>幂等两层保证：{@link MqDedupGuard} 的 Redis 去重 + {@code releaseStock}
 * 内部「预扣记录存在才释放、释放后删除记录」的天然幂等。</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqTopicConstants.Order.CANCELLED,
        consumerGroup = "mall-product-order-cancelled-consumer"
)
public class OrderCancelledConsumer implements RocketMQListener<MessageExt> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** Redis 去重维度：与 {@code consumerGroup}（带 {@code -consumer} 后缀）不同，仅用于拼去重 key */
    private static final String DEDUP_GROUP = "mall-product-order-cancelled";

    private final IStockService stockService;

    private final MqDedupGuard dedupGuard;

    @Override
    public void onMessage(MessageExt message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        Map<String, Object> payload;
        try {
            payload = OBJECT_MAPPER.readValue(body, Map.class);
        } catch (Exception e) {
            // 抛异常让 RocketMQ 重投，而不是静默丢弃——否则库存永不回补
            log.error("订单取消消息解析失败，将由 RocketMQ 重试: msgId={}", message.getMsgId(), e);
            throw new IllegalStateException("消息体解析失败", e);
        }

        Object orderNo = payload.get("orderNo");
        if (orderNo == null) {
            // 缺 orderNo 属投递方缺陷，重投也不会好，直接丢弃并留痕
            log.error("订单取消消息缺少 orderNo，丢弃: msgId={}, body={}", message.getMsgId(), body);
            return;
        }

        if (!dedupGuard.tryDedup(message.getMsgId(), DEDUP_GROUP)) {
            return;
        }

        try {
            if (!stockService.releaseStock(String.valueOf(orderNo))) {
                throw new IllegalStateException("库存释放未全部成功: orderNo=" + orderNo);
            }
            log.info("订单取消库存释放处理完成: orderNo={}", orderNo);
        } catch (RuntimeException e) {
            // 先记录原始异常，再尽力释放去重标记：
            // 若 release 自身抛异常顶掉原始错误，就失去了唯一的诊断线索
            log.error("订单取消库存释放失败，交由 MQ 重试: orderNo={}", orderNo, e);
            try {
                dedupGuard.release(message.getMsgId(), DEDUP_GROUP);
            } catch (RuntimeException releaseError) {
                // 不可自愈：标记没删掉 → 重投会被去重拦截 → 库存可能永不回补，需人工介入
                log.error("【需人工介入】释放去重标记失败，重投将被去重拦截、库存可能永不回补: msgId={}",
                        message.getMsgId(), releaseError);
            }
            throw e;
        }
    }
}
