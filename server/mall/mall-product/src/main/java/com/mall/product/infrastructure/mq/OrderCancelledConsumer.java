package com.mall.product.infrastructure.mq;

import com.mall.common.constant.CacheConstants;
import com.mall.common.constant.MqTopicConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 订单取消消息消费者
 *
 * <p>接收订单取消事件，释放预扣库存</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelledConsumer {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 处理订单取消事件
     *
     * @param orderNo 订单号
     */
    public void handleOrderCancelled(String orderNo) {
        log.info("Received order cancelled event: orderNo={}", orderNo);
    }
}
