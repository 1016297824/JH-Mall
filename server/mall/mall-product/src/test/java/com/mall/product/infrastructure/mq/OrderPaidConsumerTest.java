package com.mall.product.infrastructure.mq;

import com.mall.product.service.IHotProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPaidConsumerTest {

    @Mock
    private IHotProductService hotProductService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private OrderPaidConsumer orderPaidConsumer;

    @Test
    void handleOrderPaidShouldIncrHotRankWhenFirst() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);

        orderPaidConsumer.handleOrderPaid("ORDER001", 1L, 3);

        verify(hotProductService).incrHotRank(1L, 3);
    }

    @Test
    void handleOrderPaidShouldSkipWhenDuplicate() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(false);

        orderPaidConsumer.handleOrderPaid("ORDER001", 1L, 3);

        verify(hotProductService, never()).incrHotRank(1L, 3);
    }
}
