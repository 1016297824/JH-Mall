package com.mall.order.infrastructure.mq;

import com.mall.common.mq.MqDedupGuard;
import com.mall.order.infrastructure.outbox.OutboxPublisher;
import com.mall.order.mapper.MallOrderMapper;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 支付超时关单消费者单元测试
 *
 * <p>核心约束：关单过程异常时必须<b>释放去重标记</b>再抛异常，否则重投被去重拦截，
 * 订单永远停留在待支付且不会超时关闭（库存与优惠券也不会释放）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class OrderTimeoutConsumerTest {

    private static final String ORDER_NO = "ORDER_001";

    private static final String MSG_ID = "MSG_001";

    /** 与消费者内的 CONSUMER_GROUP 一致 */
    private static final String DEDUP_GROUP = "mall-order-timeout";

    @Mock private MallOrderMapper orderMapper;

    @Mock private OutboxPublisher outboxPublisher;

    @Mock private MqDedupGuard dedupGuard;

    @InjectMocks private OrderTimeoutConsumer consumer;

    /** 构造一条 MQ 消息 */
    private MessageExt message(String body) {
        MessageExt message = new MessageExt();
        message.setBody(body.getBytes(StandardCharsets.UTF_8));
        message.setMsgId(MSG_ID);
        return message;
    }

    @Test
    @DisplayName("订单不存在：视为已处理完毕，不抛异常也不释放去重标记")
    void shouldSkipWhenOrderNotFound() {
        when(dedupGuard.tryDedup(MSG_ID, DEDUP_GROUP)).thenReturn(true);
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenReturn(null);

        consumer.onMessage(message("{\"orderNo\":\"" + ORDER_NO + "\"}"));

        verify(dedupGuard, never()).release(anyString(), anyString());
    }

    @Test
    @DisplayName("关单过程异常：释放去重标记并抛异常，交由 MQ 重投")
    void shouldReleaseDedupWhenCloseFails() {
        when(dedupGuard.tryDedup(MSG_ID, DEDUP_GROUP)).thenReturn(true);
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenThrow(new RuntimeException("DB 不可用"));

        assertThatThrownBy(() -> consumer.onMessage(message("{\"orderNo\":\"" + ORDER_NO + "\"}")))
                .isInstanceOf(RuntimeException.class);

        verify(dedupGuard).release(MSG_ID, DEDUP_GROUP);
    }
}
