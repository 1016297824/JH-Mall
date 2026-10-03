package com.mall.payment.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.constant.MqTopicConstants;
import com.mall.payment.DO.MallOutboxDO;
import com.mall.payment.dto.event.PaymentPaidEvent;
import com.mall.payment.dto.event.RefundSucceededEvent;
import com.mall.payment.mapper.MallOutboxMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Outbox 消息发布器测试
 *
 * <p>重点是<b>时间字段的序列化契约</b>：发布器用裸 {@link ObjectMapper} 序列化 payload，
 * 未注册 JavaTimeModule，payload 里出现 {@code LocalDateTime} 会在写 Outbox 时抛
 * {@code InvalidDefinitionException}，并因异常发生在业务事务内而让整笔回调/对账失败。
 * 这类字段一旦被重新引入，本测试会立刻失败。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock private MallOutboxMapper outboxMapper;

    @InjectMocks private OutboxPublisher outboxPublisher;

    @Test
    @DisplayName("支付成功事件：时间字段必须是 ISO-8601 字符串且 payload 可被解析")
    void shouldSerializePaymentPaidEventWithIsoTime() throws Exception {
        PaymentPaidEvent event = new PaymentPaidEvent("PAY20261003000001", "ORD20261003000001", 47L,
                100L, "2026-05-17T10:30:00", "MOCKPAYPAY20261003000001", "wechat");

        outboxPublisher.publish(MqTopicConstants.Payment.PAID, "PaymentPaid",
                OutboxPublisher.AGGREGATE_PAYMENT, event.getPaymentNo(), event);

        MallOutboxDO saved = capturedMessage();
        assertThat(saved.getPayload()).contains("\"payTime\":\"2026-05-17T10:30:00\"");
        // 能被解析才说明确实是一段合法 JSON，而不是半截字符串
        assertThat(new ObjectMapper().readTree(saved.getPayload()).get("payTime").asText())
                .isEqualTo("2026-05-17T10:30:00");
    }

    @Test
    @DisplayName("退款成功事件：时间字段必须是 ISO-8601 字符串且 payload 可被解析")
    void shouldSerializeRefundSucceededEventWithIsoTime() throws Exception {
        RefundSucceededEvent event = new RefundSucceededEvent("REF20261003000001",
                "PAY20261003000001", "ORD20261003000001", "AS20261003000001", 47L, 100L,
                "2026-05-17T11:00:00", "MOCKREFREF20261003000001");

        outboxPublisher.publish(MqTopicConstants.Payment.REFUND_SUCCEEDED, "RefundSucceeded",
                OutboxPublisher.AGGREGATE_REFUND, event.getRefundNo(), event);

        MallOutboxDO saved = capturedMessage();
        assertThat(saved.getPayload()).contains("\"refundTime\":\"2026-05-17T11:00:00\"");
        assertThat(new ObjectMapper().readTree(saved.getPayload()).get("refundTime").asText())
                .isEqualTo("2026-05-17T11:00:00");
    }

    /**
     * 取出落库的 Outbox 消息
     *
     * @return 落库消息
     */
    private MallOutboxDO capturedMessage() {
        ArgumentCaptor<MallOutboxDO> captor = ArgumentCaptor.forClass(MallOutboxDO.class);
        verify(outboxMapper).insert(captor.capture());
        return captor.getValue();
    }
}
