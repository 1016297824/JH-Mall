package com.mall.marketing.infrastructure.outbox;

import com.mall.common.enums.OutboxStatusEnum;
import com.mall.marketing.DO.MallOutboxDO;
import com.mall.marketing.mapper.MallOutboxMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Outbox 发布器单元测试
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    private static final String TOPIC = "mall:coupon:used";
    private static final String EVENT_TYPE = "CouponUsed";
    private static final String AGGREGATE_ID = "500";

    @Mock private MallOutboxMapper outboxMapper;
    @InjectMocks private OutboxPublisher outboxPublisher;

    /** 券核销事件的最小 payload */
    private Map<String, Object> payload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("couponRecordId", 500L);
        payload.put("orderNo", "ORDER_001");
        return payload;
    }

    @Test
    @DisplayName("发布消息：落库为 NEW 状态、聚合类型为 COUPON、待重试次数为 0")
    void publishShouldInsertNewMessage() {
        when(outboxMapper.insert(any(MallOutboxDO.class))).thenReturn(1);

        outboxPublisher.publish(TOPIC, EVENT_TYPE, AGGREGATE_ID, payload());

        ArgumentCaptor<MallOutboxDO> captor = ArgumentCaptor.forClass(MallOutboxDO.class);
        verify(outboxMapper).insert(captor.capture());
        MallOutboxDO saved = captor.getValue();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getMessageId()).isNotBlank();
        assertThat(saved.getTopic()).isEqualTo(TOPIC);
        assertThat(saved.getEventType()).isEqualTo(EVENT_TYPE);
        assertThat(saved.getAggregateType()).isEqualTo(OutboxPublisher.AGGREGATE_COUPON);
        assertThat(saved.getAggregateId()).isEqualTo(AGGREGATE_ID);
        assertThat(saved.getStatus()).isEqualTo(OutboxStatusEnum.NEW.getCode());
        assertThat(saved.getRetryCount()).isZero();
        assertThat(saved.getCreateTime()).isNotNull();
        assertThat(saved.getUpdateTime()).isNotNull();
    }

    @Test
    @DisplayName("payload 被序列化为 JSON 文本，且不含 DO 结构")
    void publishShouldSerializePayloadAsJson() {
        when(outboxMapper.insert(any(MallOutboxDO.class))).thenReturn(1);

        outboxPublisher.publish(TOPIC, EVENT_TYPE, AGGREGATE_ID, payload());

        ArgumentCaptor<MallOutboxDO> captor = ArgumentCaptor.forClass(MallOutboxDO.class);
        verify(outboxMapper).insert(captor.capture());

        assertThat(captor.getValue().getPayload())
                .contains("\"couponRecordId\":500")
                .contains("\"orderNo\":\"ORDER_001\"");
    }

    @Test
    @DisplayName("payload 序列化失败：抛 IllegalStateException 让业务事务回滚，不静默落库")
    void publishShouldFailOnUnserializablePayload() {
        assertThatThrownBy(() -> outboxPublisher.publish(TOPIC, EVENT_TYPE, AGGREGATE_ID, new Object()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("序列化失败");

        verify(outboxMapper, org.mockito.Mockito.never()).insert(any(MallOutboxDO.class));
    }

    @Test
    @DisplayName("连续发布两条消息：id 与 messageId 都不重复")
    void publishShouldGenerateUniqueIds() {
        when(outboxMapper.insert(any(MallOutboxDO.class))).thenReturn(1);

        outboxPublisher.publish(TOPIC, EVENT_TYPE, AGGREGATE_ID, payload());
        outboxPublisher.publish(TOPIC, EVENT_TYPE, AGGREGATE_ID, payload());

        ArgumentCaptor<MallOutboxDO> captor = ArgumentCaptor.forClass(MallOutboxDO.class);
        verify(outboxMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertThat(captor.getAllValues().get(0).getId())
                .isNotEqualTo(captor.getAllValues().get(1).getId());
        assertThat(captor.getAllValues().get(0).getMessageId())
                .isNotEqualTo(captor.getAllValues().get(1).getMessageId());
    }
}
