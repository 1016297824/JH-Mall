package com.mall.order.infrastructure.mq;

import com.mall.order.mapper.MallOutboxMapper;
import com.mall.order.service.OrderService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 支付成功消费者单元测试
 *
 * <p>核心约束：业务失败时必须<b>释放去重标记</b>再抛异常。只抛异常不释放标记，
 * RocketMQ 重投会被 {@code tryDedup} 拦截，订单将永远停在待支付（用户已付款）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class PaymentPaidConsumerTest {

    private static final String ORDER_NO = "ORDER_001";

    private static final String MSG_ID = "MSG_001";

    /** 与消费者内的 CONSUMER_GROUP 一致 */
    private static final String DEDUP_GROUP = "mall-order-payment-paid";

    @Mock private OrderService orderService;

    @Mock private MallOutboxMapper outboxMapper;

    @Mock private MqDedupGuard dedupGuard;

    @InjectMocks private PaymentPaidConsumer consumer;

    /** 构造一条 MQ 消息 */
    private MessageExt message(String body) {
        MessageExt message = new MessageExt();
        message.setBody(body.getBytes(StandardCharsets.UTF_8));
        message.setMsgId(MSG_ID);
        return message;
    }

    @Test
    @DisplayName("正常消费：推进订单状态，且成功路径不释放去重标记")
    void shouldProcessPaidEvent() {
        when(dedupGuard.tryDedup(MSG_ID, DEDUP_GROUP)).thenReturn(true);

        consumer.onMessage(message("{\"orderNo\":\"" + ORDER_NO + "\"}"));

        verify(orderService).payCallback(ORDER_NO);
        verify(dedupGuard, never()).release(anyString(), anyString());
    }

    @Test
    @DisplayName("状态推进失败：释放去重标记并抛异常，交由 MQ 重投")
    void shouldReleaseDedupWhenPayCallbackFails() {
        when(dedupGuard.tryDedup(MSG_ID, DEDUP_GROUP)).thenReturn(true);
        doThrow(new RuntimeException("DB 不可用")).when(orderService).payCallback(ORDER_NO);

        assertThatThrownBy(() -> consumer.onMessage(message("{\"orderNo\":\"" + ORDER_NO + "\"}")))
                .isInstanceOf(RuntimeException.class);

        verify(dedupGuard).release(MSG_ID, DEDUP_GROUP);
    }

    @Test
    @DisplayName("重复投递：去重命中时不推进状态")
    void shouldSkipDuplicateDelivery() {
        when(dedupGuard.tryDedup(MSG_ID, DEDUP_GROUP)).thenReturn(false);

        consumer.onMessage(message("{\"orderNo\":\"" + ORDER_NO + "\"}"));

        verify(orderService, never()).payCallback(anyString());
    }
}
