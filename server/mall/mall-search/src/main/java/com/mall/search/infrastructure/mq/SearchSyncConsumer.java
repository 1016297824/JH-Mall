package com.mall.search.infrastructure.mq;

import com.mall.common.constant.MqTopicConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 搜索索引增量同步消费者
 *
 * <p>消费 {@code mall:search:sync} 事件，幂等去重后写入 ES 索引。
 * 当前为 Batch 1 空壳，Batch 2 IndexServiceImpl 实现后再启用实际同步逻辑。</p>
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = MqTopicConstants.Product.SEARCH_SYNC,
    consumerGroup = "mall-search-sync-consumer"
)
public class SearchSyncConsumer implements RocketMQListener<String> {

    /**
     * 消费搜索同步消息
     *
     * <p>消息体为 JSON 字符串，Batch 2 将解析消息 → 幂等去重 → IndexService.syncProduct()</p>
     *
     * @param message 消息体 JSON
     */
    @Override
    public void onMessage(String message) {
        log.debug("收到搜索同步消息: {}", message);
        // TODO Batch 2: JSON 解析 → 幂等去重 → IndexService.syncProduct(spuId, operation)
    }
}
