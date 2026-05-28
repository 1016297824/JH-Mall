package com.mall.user.mq;

import com.mall.common.enums.user.BizTypeEnum;
import com.mall.user.service.IMemberService;
import com.mall.user.service.IPointsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * UserOrderCompletedConsumer 单元测试
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@ExtendWith(MockitoExtension.class)
class UserOrderCompletedConsumerTest {

    @Mock
    private IPointsService pointsService;

    @Mock
    private IMemberService memberService;

    @InjectMocks
    private UserOrderCompletedConsumer consumer;

    @Test
    void onMessage_shouldAddPointsAndGrowth() {
        String message = "{\"userId\":1,\"orderNo\":\"ORD001\",\"orderAmount\":10000,\"points\":100}";

        assertDoesNotThrow(() -> consumer.onMessage(message));

        verify(pointsService).addPoints(eq(1L), eq(100), eq(BizTypeEnum.ORDER), eq("ORD001"));
        verify(memberService).addGrowth(eq(1L), eq(100), eq(BizTypeEnum.ORDER), eq("ORD001"));
    }

    @Test
    void onMessage_shouldHandleMissingFields() {
        String message = "{\"userId\":1,\"orderNo\":\"ORD002\"}";

        assertDoesNotThrow(() -> consumer.onMessage(message));

        verify(pointsService, never()).addPoints(eq(1L), eq(0), eq(BizTypeEnum.ORDER), eq("ORD002"));
        verify(memberService, never()).addGrowth(eq(1L), eq(0), eq(BizTypeEnum.ORDER), eq("ORD002"));
    }
}
