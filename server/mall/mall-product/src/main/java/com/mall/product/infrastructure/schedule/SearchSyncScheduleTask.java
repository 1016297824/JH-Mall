package com.mall.product.infrastructure.schedule;

import com.mall.common.constant.MqTopicConstants;
import com.mall.product.DO.OutboxMessageDO;
import com.mall.product.mapper.OutboxMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchSyncScheduleTask {

    private final OutboxMessageMapper outboxMessageMapper;

    @Scheduled(fixedDelay = 30000)
    public void compensate() {
        List<OutboxMessageDO> pendingList = outboxMessageMapper.selectPending(
                MqTopicConstants.Product.SEARCH_SYNC, 100);
        for (OutboxMessageDO outbox : pendingList) {
            try {
                log.info("Compensate outbox: messageId={}, spuId={}", outbox.getMessageId(), outbox.getAggregateId());
                outboxMessageMapper.updateStatus(outbox.getId(), "SENT");
            } catch (Exception e) {
                log.error("Failed to compensate outbox: messageId={}", outbox.getMessageId(), e);
            }
        }
    }
}
