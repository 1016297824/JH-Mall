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

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchSyncProducer {

    private final RemoteSearchAdapter remoteSearchAdapter;
    private final OutboxMessageMapper outboxMessageMapper;

    public void syncProduct(Long spuId, SyncOperationEnum operation) {
        SearchSyncRequest request = new SearchSyncRequest();
        request.setSpuId(spuId);
        request.setOperation(operation.getCode());
        request.setTimestamp(System.currentTimeMillis());

        try {
            remoteSearchAdapter.syncProduct(request);
        } catch (Exception e) {
            log.warn("实时同步失败，降级到 Outbox: spuId={}, operation={}", spuId, operation);
            writeOutbox(spuId, operation, request);
        }
    }

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
