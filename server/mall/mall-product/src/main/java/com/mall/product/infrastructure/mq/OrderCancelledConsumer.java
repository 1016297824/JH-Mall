package com.mall.product.infrastructure.mq;

import com.mall.common.constant.CacheConstants;
import com.mall.common.constant.MqTopicConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelledConsumer {

    private final RedisTemplate<String, Object> redisTemplate;

    public void handleOrderCancelled(String orderNo) {
        log.info("Received order cancelled event: orderNo={}", orderNo);
    }
}
