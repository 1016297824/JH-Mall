package com.mall.product.infrastructure.mq;

import com.mall.api.feign.RemoteSearchService.SearchSyncRequest;
import com.mall.common.constant.MqTopicConstants;
import com.mall.common.enums.product.SyncOperationEnum;
import com.mall.product.DO.OutboxMessageDO;
import com.mall.product.infrastructure.feign.RemoteSearchAdapter;
import com.mall.product.mapper.OutboxMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 搜索同步消息生产者
 *
 * <p>实时同步搜索索引，失败时降级写入 Outbox 表，由定时任务补偿</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchSyncProducer {

    private final RemoteSearchAdapter remoteSearchAdapter;
    private final OutboxMessageMapper outboxMessageMapper;

    /**
     * 同步商品到搜索索引
     *
     * <p>采用 Outbox 模式：优先实时调用搜索引擎，失败则将消息持久化到 Outbox 表，
     * 由后台定时任务 (SearchSyncScheduleTask) 补偿投递。</p>
     *
     * @param spuId     SPU ID
     * @param operation 操作类型（CREATE/UPDATE/DELETE）
     */
    public void syncProduct(Long spuId, SyncOperationEnum operation) {
        // 构造搜索同步请求体（含操作类型和时间戳）
        SearchSyncRequest request = new SearchSyncRequest();
        request.setSpuId(spuId);
        request.setOperation(operation.getCode());
        request.setTimestamp(System.currentTimeMillis());
        // 第一优先：实时 Feign 调用搜索引擎同步
        try {
            remoteSearchAdapter.syncProduct(request);
        } catch (Exception e) {
            log.warn("实时同步失败，降级到 Outbox: spuId={}, operation={}", spuId, operation);
            // 第二优先：实时失败后写入 Outbox 表，由定时任务保证最终一致性
            writeOutbox(spuId, operation, request);
        }
    }

    /**
     * 写入 Outbox 表（可靠消息持久化）
     *
     * <p>记录消息 ID、主题、事件类型、聚合信息、payload 和初始状态 NEW，
     * 后续由补偿定时任务扫描投递。</p>
     */
    private void writeOutbox(Long spuId, SyncOperationEnum operation, SearchSyncRequest request) {
        OutboxMessageDO outbox = new OutboxMessageDO();
        outbox.setMessageId(UUID.randomUUID().toString().replace("-", ""));
        outbox.setTopic(MqTopicConstants.Product.SEARCH_SYNC);
        outbox.setEventType(operation.getCode());
        outbox.setAggregateType("SPU");
        outbox.setAggregateId(String.valueOf(spuId));
        outbox.setPayload("{\"spuId\":" + spuId + ",\"operation\":\"" + operation.getCode() + "\",\"timestamp\":" + request.getTimestamp() + "}");
        outbox.setStatus("NEW");
        outbox.setRetryCount(0);
        outbox.setCreateTime(LocalDateTime.now());
        outbox.setUpdateTime(LocalDateTime.now());
        outboxMessageMapper.insert(outbox);
        log.info("Outbox written: spuId={}, operation={}", spuId, operation);
    }
}
