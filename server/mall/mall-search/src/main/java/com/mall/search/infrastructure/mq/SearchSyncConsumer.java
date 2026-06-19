package com.mall.search.infrastructure.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.constant.MqTopicConstants;
import com.mall.search.service.IndexService;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = MqTopicConstants.Product.SEARCH_SYNC,
    consumerGroup = "mall-search-sync-consumer"
)
public class SearchSyncConsumer implements RocketMQListener<String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final IndexService indexService;

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
        SyncMessage syncMessage;
        try {
            syncMessage = OBJECT_MAPPER.readValue(message, SyncMessage.class);
        } catch (Exception e) {
            log.error("搜索同步消息解析失败: {}", message, e);
            return;
        }
        if (syncMessage == null || syncMessage.getSpuId() == null) {
            log.warn("搜索同步消息缺少 spuId: {}", message);
            return;
        }
        String operation = syncMessage.getOperation() != null ? syncMessage.getOperation() : "UPSERT";
        // 幂等去重在 IndexService.syncProduct 内部处理
        indexService.syncProduct(syncMessage.getSpuId(), operation);
    }

    /**
     * 搜索同步消息体
     *
     * <p>Payload: {@code spuId, operation}（UPSERT / DELETE），{@code timestamp}</p>
     */
    @Data
    @NoArgsConstructor
    private static class SyncMessage {
        /** SPU ID */
        private Long spuId;
        /** 操作类型：UPSERT / DELETE */
        private String operation;
    }
}
