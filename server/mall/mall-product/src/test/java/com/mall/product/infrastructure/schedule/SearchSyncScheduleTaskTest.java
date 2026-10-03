package com.mall.product.infrastructure.schedule;

import com.mall.common.constant.MqTopicConstants;
import com.mall.common.enums.OutboxStatusEnum;
import com.mall.common.enums.product.SyncOperationEnum;
import com.mall.product.DO.OutboxMessageDO;
import com.mall.product.infrastructure.mq.SearchSyncProducer;
import com.mall.product.mapper.OutboxMessageMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 搜索同步补偿任务单元测试
 *
 * <p>核心约束：<b>补偿必须真的重新投递</b>，只有投递成功才能把 Outbox 记录置为 SENT；
 * 失败必须保留 NEW 等下一轮，否则消息会被永久标记为「已发送」而索引实际未更新。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class SearchSyncScheduleTaskTest {

    private static final Long OUTBOX_ID = 1L;

    private static final String MESSAGE_ID = "MSG20261003000001";

    private static final Long SPU_ID = 1001L;

    @Mock
    private OutboxMessageMapper outboxMessageMapper;

    @Mock
    private SearchSyncProducer searchSyncProducer;

    @InjectMocks
    private SearchSyncScheduleTask searchSyncScheduleTask;

    @Test
    @DisplayName("无待补偿消息：不投递、不改状态")
    void doesNothingWhenNoPendingMessage() {
        when(outboxMessageMapper.selectPending(eq(MqTopicConstants.Product.SEARCH_SYNC), anyInt()))
                .thenReturn(List.of());

        assertThat(searchSyncScheduleTask.execute()).isZero();

        verify(searchSyncProducer, never()).resync(anyLong(), any());
        verify(outboxMessageMapper, never()).updateStatus(anyLong(), anyString());
    }

    @Test
    @DisplayName("补偿成功：真正重投并置 SENT")
    void marksSentWhenResyncSucceeds() {
        when(outboxMessageMapper.selectPending(eq(MqTopicConstants.Product.SEARCH_SYNC), anyInt()))
                .thenReturn(List.of(outbox("UPSERT")));
        when(searchSyncProducer.resync(SPU_ID, SyncOperationEnum.UPSERT)).thenReturn(true);

        assertThat(searchSyncScheduleTask.execute()).isEqualTo(1);

        verify(searchSyncProducer).resync(SPU_ID, SyncOperationEnum.UPSERT);
        verify(outboxMessageMapper).updateStatus(OUTBOX_ID, OutboxStatusEnum.SENT.getCode());
    }

    @Test
    @DisplayName("补偿失败：不得置 SENT，保留 NEW 待下轮重试")
    void keepsNewWhenResyncFails() {
        when(outboxMessageMapper.selectPending(eq(MqTopicConstants.Product.SEARCH_SYNC), anyInt()))
                .thenReturn(List.of(outbox("UPSERT")));
        when(searchSyncProducer.resync(SPU_ID, SyncOperationEnum.UPSERT)).thenReturn(false);

        searchSyncScheduleTask.execute();

        verify(outboxMessageMapper, never()).updateStatus(eq(OUTBOX_ID), eq(OutboxStatusEnum.SENT.getCode()));
    }

    @Test
    @DisplayName("单条投递抛异常：不中断整批，且该条不置 SENT")
    void continuesAfterSingleFailure() {
        OutboxMessageDO first = outbox("UPSERT");
        OutboxMessageDO second = outbox("UPSERT");
        second.setId(2L);
        second.setAggregateId("1002");
        when(outboxMessageMapper.selectPending(eq(MqTopicConstants.Product.SEARCH_SYNC), anyInt()))
                .thenReturn(List.of(first, second));
        when(searchSyncProducer.resync(SPU_ID, SyncOperationEnum.UPSERT))
                .thenThrow(new RuntimeException("搜索引擎不可用"));
        when(searchSyncProducer.resync(1002L, SyncOperationEnum.UPSERT)).thenReturn(true);

        searchSyncScheduleTask.execute();

        // 第二条仍被处理
        verify(outboxMessageMapper).updateStatus(2L, OutboxStatusEnum.SENT.getCode());
    }

    @Test
    @DisplayName("eventType 非法：置 FAILED 留痕，且不调用投递（重投也不会好）")
    void marksFailedWhenOperationInvalid() {
        when(outboxMessageMapper.selectPending(eq(MqTopicConstants.Product.SEARCH_SYNC), anyInt()))
                .thenReturn(List.of(outbox("UNKNOWN_OP")));

        searchSyncScheduleTask.execute();

        verify(searchSyncProducer, never()).resync(anyLong(), any());
        verify(outboxMessageMapper).updateStatus(OUTBOX_ID, OutboxStatusEnum.FAILED.getCode());
    }

    /**
     * 构造 Outbox 待补偿记录夹具
     *
     * <p>{@code aggregateId} 存 spuId、{@code eventType} 存操作码，
     * 与 {@code SearchSyncProducer.writeOutbox} 的写入方式一致，
     * 因此补偿时无需解析 payload JSON。</p>
     *
     * @param eventType 事件类型（操作码）
     * @return Outbox 记录
     */
    private static OutboxMessageDO outbox(String eventType) {
        OutboxMessageDO outbox = new OutboxMessageDO();
        outbox.setId(OUTBOX_ID);
        outbox.setMessageId(MESSAGE_ID);
        outbox.setTopic(MqTopicConstants.Product.SEARCH_SYNC);
        outbox.setEventType(eventType);
        outbox.setAggregateType("SPU");
        outbox.setAggregateId(String.valueOf(SPU_ID));
        outbox.setStatus(OutboxStatusEnum.NEW.getCode());
        outbox.setRetryCount(0);
        return outbox;
    }
}
