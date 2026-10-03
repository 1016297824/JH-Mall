package com.mall.product.infrastructure.mq;

import com.mall.product.service.IStockService;
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
 * 订单取消消费者单元测试
 *
 * <p>覆盖「订单取消/超时关闭 → 释放预扣库存」链路：正常释放、重复投递幂等、
 * 报文异常重试、缺少关键字段丢弃。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class OrderCancelledConsumerTest {

    private static final String ORDER_NO = "ORD20261003000001";

    private static final String MSG_ID = "MSG_20261003000001";

    /** 与消费者注解中的 consumerGroup 保持一致 */
    private static final String DEDUP_GROUP = "mall-product-order-cancelled";

    @Mock
    private IStockService stockService;

    @Mock
    private MqDedupGuard dedupGuard;

    @InjectMocks
    private OrderCancelledConsumer consumer;

    @Test
    @DisplayName("正常消费：释放该订单预扣的库存")
    void shouldReleaseStock() {
        when(dedupGuard.tryDedup(MSG_ID, DEDUP_GROUP)).thenReturn(true);

        consumer.onMessage(message("{\"orderNo\":\"" + ORDER_NO + "\",\"userId\":100}"));

        verify(stockService).releaseStock(ORDER_NO);
    }

    @Test
    @DisplayName("重复投递：去重命中时不重复释放库存")
    void shouldSkipDuplicateDelivery() {
        when(dedupGuard.tryDedup(MSG_ID, DEDUP_GROUP)).thenReturn(false);

        consumer.onMessage(message("{\"orderNo\":\"" + ORDER_NO + "\"}"));

        verify(stockService, never()).releaseStock(anyString());
    }

    @Test
    @DisplayName("报文无法解析：抛异常交由 MQ 重试，不静默丢弃（否则库存永不回补）")
    void shouldThrowWhenBodyInvalid() {
        assertThatThrownBy(() -> consumer.onMessage(message("not-a-json")))
                .isInstanceOf(IllegalStateException.class);

        verify(stockService, never()).releaseStock(anyString());
    }

    @Test
    @DisplayName("缺少 orderNo：丢弃消息且不触发释放")
    void shouldSkipWhenOrderNoMissing() {
        consumer.onMessage(message("{\"userId\":100}"));

        verify(stockService, never()).releaseStock(anyString());
    }

    /**
     * 构造一条 MQ 消息
     *
     * @param body 消息体
     * @return 消息
     */
    private static MessageExt message(String body) {
        MessageExt message = new MessageExt();
        message.setBody(body.getBytes(StandardCharsets.UTF_8));
        message.setMsgId(MSG_ID);
        return message;
    }
}
