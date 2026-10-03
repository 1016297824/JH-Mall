package com.mall.marketing.infrastructure.outbox;

import com.mall.common.enums.OutboxStatusEnum;
import com.mall.marketing.DO.MallOutboxDO;
import com.mall.marketing.mapper.MallOutboxMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Outbox 投递调度器单元测试
 *
 * <p>重点覆盖设计文档 §7.3 的退避重试与上限置 FAILED 行为。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@ExtendWith(MockitoExtension.class)
class OutboxSchedulerTest {

    private static final String TOPIC = "mall_coupon_used";
    private static final Long MESSAGE_PK = 1L;

    @Mock private MallOutboxMapper outboxMapper;
    @Mock private RocketMQTemplate rocketMQTemplate;
    @InjectMocks private OutboxScheduler outboxScheduler;

    /** 构造一条待投递消息 */
    private MallOutboxDO message(Integer retryCount) {
        MallOutboxDO message = new MallOutboxDO();
        message.setId(MESSAGE_PK);
        message.setMessageId("MSG_001");
        message.setTopic(TOPIC);
        message.setPayload("{\"orderNo\":\"ORDER_001\"}");
        message.setRetryCount(retryCount);
        return message;
    }

    /** 让投递必失败 */
    private void stubSendFailure() {
        when(rocketMQTemplate.syncSend(anyString(), any(Message.class)))
                .thenThrow(new RuntimeException("broker down"));
    }

    @Test
    @DisplayName("无待投递消息时不发生任何投递")
    void nothingToDispatch() {
        when(outboxMapper.selectPending(100)).thenReturn(List.of());

        outboxScheduler.dispatch();

        verify(rocketMQTemplate, never()).syncSend(anyString(), any(Message.class));
    }

    @Test
    @DisplayName("投递成功：发送到对应 topic 并标记 SENT")
    void dispatchSuccess() {
        when(outboxMapper.selectPending(100)).thenReturn(List.of(message(0)));

        outboxScheduler.dispatch();

        ArgumentCaptor<Message<String>> captor = ArgumentCaptor.forClass(Message.class);
        verify(rocketMQTemplate).syncSend(eq(TOPIC), captor.capture());
        assertThat(captor.getValue().getPayload()).isEqualTo("{\"orderNo\":\"ORDER_001\"}");
        verify(outboxMapper).markSent(MESSAGE_PK);
    }

    @Test
    @DisplayName("首次失败：重试次数置 1、状态仍为 NEW、安排下一次重试时间")
    void firstFailureSchedulesRetry() {
        when(outboxMapper.selectPending(100)).thenReturn(List.of(message(0)));
        stubSendFailure();

        outboxScheduler.dispatch();

        ArgumentCaptor<LocalDateTime> nextCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(outboxMapper).markRetry(eq(MESSAGE_PK), eq(OutboxStatusEnum.NEW.getCode()),
                eq(1), nextCaptor.capture());
        assertThat(nextCaptor.getValue()).isAfter(LocalDateTime.now());
        verify(outboxMapper, never()).markSent(any());
    }

    @Test
    @DisplayName("第二次失败：重试次数累加为 2，仍为 NEW")
    void secondFailureSchedulesRetry() {
        when(outboxMapper.selectPending(100)).thenReturn(List.of(message(1)));
        stubSendFailure();

        outboxScheduler.dispatch();

        verify(outboxMapper).markRetry(eq(MESSAGE_PK), eq(OutboxStatusEnum.NEW.getCode()),
                eq(2), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("达到重试上限（第 3 次失败）→ 置 FAILED 且不再安排重试时间")
    void exhaustedRetryMarksFailed() {
        when(outboxMapper.selectPending(100)).thenReturn(List.of(message(2)));
        stubSendFailure();

        outboxScheduler.dispatch();

        verify(outboxMapper).markRetry(MESSAGE_PK, OutboxStatusEnum.FAILED.getCode(), 3, null);
    }

    @Test
    @DisplayName("扫描异常：吞掉异常并返回，不向上抛出（避免调度线程被中断）")
    void scanFailureShouldNotPropagate() {
        when(outboxMapper.selectPending(100)).thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> outboxScheduler.dispatch()).doesNotThrowAnyException();

        verify(rocketMQTemplate, never()).syncSend(anyString(), any(Message.class));
    }

    @Test
    @DisplayName("单条投递异常不影响同批次其余消息")
    void oneFailureShouldNotBlockOthers() {
        MallOutboxDO second = message(0);
        second.setId(2L);
        second.setMessageId("MSG_002");
        when(outboxMapper.selectPending(100)).thenReturn(List.of(message(0), second));
        // 第一条必失败、第二条正常
        when(rocketMQTemplate.syncSend(anyString(), any(Message.class)))
                .thenThrow(new RuntimeException("broker down"))
                .thenReturn(null);

        outboxScheduler.dispatch();

        verify(outboxMapper).markRetry(eq(MESSAGE_PK), eq(OutboxStatusEnum.NEW.getCode()),
                eq(1), any(LocalDateTime.class));
        verify(outboxMapper).markSent(2L);
    }
}
