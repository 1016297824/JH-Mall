package com.mall.user.infrastructure.mq;

import com.mall.common.enums.user.BizTypeEnum;
import com.mall.common.mq.MqDedupGuard;
import com.mall.user.service.IMemberService;
import com.mall.user.service.IPointsService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * UserOrderCompletedConsumer 单元测试
 *
 * <p>积分与成长值是<b>不可逆的资产变动</b>：重复投递会让用户白拿一份，
 * 静默吞异常则会让用户永远拿不到。故重点锁定去重、失败释放标记与异常上抛三条。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class UserOrderCompletedConsumerTest {

    private static final String MSG_ID = "MSG_20261003000009";

    private static final String CONSUMER_GROUP = "mall-user-order-completed";

    private static final String BODY =
            "{\"userId\":1,\"orderNo\":\"ORD001\",\"orderAmount\":10000,\"points\":100}";

    @Mock
    private IPointsService pointsService;

    @Mock
    private IMemberService memberService;

    @Mock
    private MqDedupGuard dedupGuard;

    @InjectMocks
    private UserOrderCompletedConsumer consumer;

    /** 构造一条带 msgId 的 RocketMQ 消息 */
    private MessageExt message(String body) {
        MessageExt message = new MessageExt();
        message.setMsgId(MSG_ID);
        message.setBody(body.getBytes(StandardCharsets.UTF_8));
        return message;
    }

    @Test
    @DisplayName("首次投递：发放积分与成长值")
    void shouldAddPointsAndGrowthOnFirstDelivery() {
        when(dedupGuard.tryDedup(MSG_ID, CONSUMER_GROUP)).thenReturn(true);

        consumer.onMessage(message(BODY));

        verify(pointsService).addPoints(eq(1L), eq(100), eq(BizTypeEnum.ORDER), eq("ORD001"));
        verify(memberService).addGrowth(eq(1L), eq(100), eq(BizTypeEnum.ORDER), eq("ORD001"));
    }

    @Test
    @DisplayName("重复投递：直接跳过，不得二次发放")
    void shouldSkipOnDuplicateDelivery() {
        when(dedupGuard.tryDedup(MSG_ID, CONSUMER_GROUP)).thenReturn(false);

        consumer.onMessage(message(BODY));

        verifyNoInteractions(pointsService);
        verifyNoInteractions(memberService);
    }

    @Test
    @DisplayName("发放失败：释放去重标记并上抛，交由 MQ 重投（吞掉就等于永久不发）")
    void shouldReleaseDedupAndRethrowOnFailure() {
        when(dedupGuard.tryDedup(MSG_ID, CONSUMER_GROUP)).thenReturn(true);
        doThrow(new IllegalStateException("DB 抖动"))
                .when(pointsService).addPoints(anyLong(), anyInt(), any(), any());

        assertThatThrownBy(() -> consumer.onMessage(message(BODY)))
                .isInstanceOf(IllegalStateException.class);

        verify(dedupGuard).release(MSG_ID, CONSUMER_GROUP);
    }

    @Test
    @DisplayName("缺少 userId：属数据缺陷，留痕丢弃且不占用重投机会")
    void shouldDropMessageWithoutUserId() {
        consumer.onMessage(message("{\"orderNo\":\"ORD002\",\"points\":100}"));

        verifyNoInteractions(pointsService);
        verifyNoInteractions(memberService);
        // 校验先于去重：数据缺陷重投也治不好，占位只会让毒丸反复占据队首
        verifyNoInteractions(dedupGuard);
    }

    @Test
    @DisplayName("消息体无法解析：上抛交由 MQ 重投，不占用去重标记")
    void shouldRethrowOnUnparsableBody() {
        assertThatThrownBy(() -> consumer.onMessage(message("not-a-json")))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(dedupGuard);
    }
}
