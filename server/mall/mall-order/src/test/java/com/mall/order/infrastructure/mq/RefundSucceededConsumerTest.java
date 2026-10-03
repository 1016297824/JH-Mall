package com.mall.order.infrastructure.mq;

import com.mall.common.mq.MqDedupGuard;
import com.mall.order.service.AfterSaleService;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 退款成功消费者单元测试
 *
 * <p>核心约束：售后回调失败时必须<b>释放去重标记</b>再抛异常，否则重投被去重拦截，
 * 售后单永远停在退款中、退货退款的库存也不会回补。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class RefundSucceededConsumerTest {

    private static final String AFTER_SALE_NO = "AS_001";

    private static final String MSG_ID = "MSG_001";

    /** 与消费者内的 CONSUMER_GROUP 一致 */
    private static final String DEDUP_GROUP = "mall-order-refund-succeeded";

    @Mock private AfterSaleService afterSaleService;

    @Mock private MqDedupGuard dedupGuard;

    @InjectMocks private RefundSucceededConsumer consumer;

    /** 构造一条 MQ 消息 */
    private MessageExt message(String body) {
        MessageExt message = new MessageExt();
        message.setBody(body.getBytes(StandardCharsets.UTF_8));
        message.setMsgId(MSG_ID);
        return message;
    }

    @Test
    @DisplayName("正常消费：推进售后单完成，成功路径不释放去重标记")
    void shouldProcessRefundSucceeded() {
        when(dedupGuard.tryDedup(MSG_ID, DEDUP_GROUP)).thenReturn(true);

        consumer.onMessage(message("{\"afterSaleNo\":\"" + AFTER_SALE_NO + "\",\"refundAmount\":100}"));

        verify(afterSaleService).refundCallback(eq(AFTER_SALE_NO), eq(100L));
        verify(dedupGuard, never()).release(anyString(), anyString());
    }

    @Test
    @DisplayName("售后回调失败：释放去重标记并抛异常，交由 MQ 重投")
    void shouldReleaseDedupWhenRefundCallbackFails() {
        when(dedupGuard.tryDedup(MSG_ID, DEDUP_GROUP)).thenReturn(true);
        doThrow(new RuntimeException("DB 不可用"))
                .when(afterSaleService).refundCallback(eq(AFTER_SALE_NO), any());

        assertThatThrownBy(() -> consumer.onMessage(message("{\"afterSaleNo\":\"" + AFTER_SALE_NO + "\"}")))
                .isInstanceOf(RuntimeException.class);

        verify(dedupGuard).release(MSG_ID, DEDUP_GROUP);
    }
}
