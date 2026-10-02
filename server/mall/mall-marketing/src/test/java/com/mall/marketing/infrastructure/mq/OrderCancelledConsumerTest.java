package com.mall.marketing.infrastructure.mq;

import com.mall.marketing.service.CouponClaimService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 订单取消消费者单元测试
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@ExtendWith(MockitoExtension.class)
class OrderCancelledConsumerTest {

    private static final String ORDER_NO = "ORDER_001";
    private static final String MSG_ID = "MSG_001";

    @Mock private CouponClaimService couponClaimService;
    @Mock private MqDedupGuard dedupGuard;
    @InjectMocks private OrderCancelledConsumer consumer;

    /** 构造一条 MQ 消息 */
    private MessageExt message(String body) {
        MessageExt message = new MessageExt();
        message.setBody(body.getBytes(StandardCharsets.UTF_8));
        message.setMsgId(MSG_ID);
        return message;
    }

    @Test
    @DisplayName("正常消费：释放该订单的锁定券并回补库存")
    void shouldReleaseCoupon() {
        when(dedupGuard.tryDedup(MSG_ID, "mall-marketing-order-cancelled")).thenReturn(true);

        consumer.onMessage(message("{\"orderNo\":\"" + ORDER_NO + "\",\"cancelReason\":\"USER_CANCEL\"}"));

        verify(couponClaimService).releaseCoupon(ORDER_NO);
    }

    @Test
    @DisplayName("重复投递：幂等去重命中时不做任何处理（防重复回补库存）")
    void shouldSkipDuplicateDelivery() {
        when(dedupGuard.tryDedup(MSG_ID, "mall-marketing-order-cancelled")).thenReturn(false);

        consumer.onMessage(message("{\"orderNo\":\"" + ORDER_NO + "\"}"));

        verify(couponClaimService, never()).releaseCoupon(any());
    }

    @Test
    @DisplayName("消息缺少 orderNo：丢弃且不占用去重标记")
    void shouldDropMessageWithoutOrderNo() {
        consumer.onMessage(message("{\"cancelReason\":\"PAY_TIMEOUT\"}"));

        verify(couponClaimService, never()).releaseCoupon(any());
        verify(dedupGuard, never()).tryDedup(any(), any());
    }

    @Test
    @DisplayName("消息体解析失败：抛异常交由 RocketMQ 重试，不静默吞掉")
    void shouldThrowWhenBodyUnparsable() {
        MessageExt message = message("not-a-json");

        assertThatThrownBy(() -> consumer.onMessage(message))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("解析失败");

        verify(couponClaimService, never()).releaseCoupon(any());
    }
}
