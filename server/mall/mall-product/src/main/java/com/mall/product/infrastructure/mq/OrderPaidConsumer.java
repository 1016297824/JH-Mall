package com.mall.product.infrastructure.mq;

import com.mall.common.constant.CacheConstants;
import com.mall.product.service.IHotProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 订单支付消息消费者
 *
 * <p>接收订单支付事件，增量更新热点排名</p>
 *
 * @author JH-Mall
 * @date 2026/06/01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaidConsumer {

    private final IHotProductService hotProductService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 处理订单支付事件
     *
     * <p>幂等去重：同一订单号只处理一次，SETNX 防重</p>
     *
     * @param orderNo 订单号
     * @param spuId   SPU ID
     * @param qty     购买数量
     */
    public void handleOrderPaid(String orderNo, Long spuId, int qty) {
        String dedupKey = CacheConstants.Product.HOT_PAID + orderNo;
        Boolean first = redisTemplate.opsForValue().setIfAbsent(dedupKey, "1", Duration.ofDays(7));
        if (Boolean.TRUE.equals(first)) {
            log.info("处理订单支付事件，更新热点排名: orderNo={}, spuId={}, qty={}", orderNo, spuId, qty);
            hotProductService.incrHotRank(spuId, qty);
        } else {
            log.info("订单支付事件已处理，跳过: orderNo={}", orderNo);
        }
    }
}
