package com.mall.product.infrastructure.mq;

import com.mall.common.mq.MqDedupGuard;
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
import static org.mockito.Mockito.doThrow;
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

    /** 去重维度值（与注解中的 consumerGroup 差一个 -consumer 后缀，仅用于拼去重 key） */
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
        when(stockService.releaseStock(ORDER_NO)).thenReturn(true);

        consumer.onMessage(message("{\"orderNo\":\"" + ORDER_NO + "\",\"userId\":100}"));

        verify(stockService).releaseStock(ORDER_NO);
        // 成功路径不应释放去重标记（否则重复投递会被重复处理）
        verify(dedupGuard, never()).release(anyString(), anyString());
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

    @Test
    @DisplayName("释放未全部成功：释放去重标记并抛异常，交由 MQ 重投")
    void shouldThrowAndReleaseDedupWhenReleaseFails() {
        when(dedupGuard.tryDedup(MSG_ID, DEDUP_GROUP)).thenReturn(true);
        when(stockService.releaseStock(ORDER_NO)).thenReturn(false);

        assertThatThrownBy(() -> consumer.onMessage(message("{\"orderNo\":\"" + ORDER_NO + "\"}")))
                .isInstanceOf(IllegalStateException.class);

        // 不释放标记的话，重投会被去重拦截 → 库存永不回补
        verify(dedupGuard).release(MSG_ID, DEDUP_GROUP);
    }

    @Test
    @DisplayName("释放抛异常：同样释放去重标记并向上抛出")
    void shouldReleaseDedupWhenReleaseThrows() {
        when(dedupGuard.tryDedup(MSG_ID, DEDUP_GROUP)).thenReturn(true);
        when(stockService.releaseStock(ORDER_NO)).thenThrow(new RuntimeException("DB 不可用"));

        assertThatThrownBy(() -> consumer.onMessage(message("{\"orderNo\":\"" + ORDER_NO + "\"}")))
                .isInstanceOf(RuntimeException.class);

        verify(dedupGuard).release(MSG_ID, DEDUP_GROUP);
    }

    @Test
    @DisplayName("释放标记本身失败：原始异常仍须上抛，不能被 release 的异常顶掉")
    void shouldKeepOriginalExceptionWhenDedupReleaseFails() {
        when(dedupGuard.tryDedup(MSG_ID, DEDUP_GROUP)).thenReturn(true);
        when(stockService.releaseStock(ORDER_NO)).thenReturn(false);
        doThrow(new RuntimeException("Redis 不可用")).when(dedupGuard).release(MSG_ID, DEDUP_GROUP);

        assertThatThrownBy(() -> consumer.onMessage(message("{\"orderNo\":\"" + ORDER_NO + "\"}")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("库存释放未全部成功");
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
