package com.mall.product.infrastructure.mq;

import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.common.enums.product.SyncOperationEnum;
import com.mall.product.infrastructure.feign.RemoteSearchAdapter;
import com.mall.product.mapper.OutboxMessageMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SearchSyncProducerTest {

    @Mock
    private RemoteSearchAdapter remoteSearchAdapter;

    @Mock
    private OutboxMessageMapper outboxMessageMapper;

    @InjectMocks
    private SearchSyncProducer producer;

    @Test
    void syncProductShouldCallAdapter() {
        producer.syncProduct(1L, SyncOperationEnum.UPSERT);

        verify(remoteSearchAdapter).syncProduct(any());
    }

    @Test
    void syncProductShouldWriteOutboxWhenAdapterFails() {
        doThrow(new BusinessException(ErrorCode.SYSTEM_ERROR)).when(remoteSearchAdapter).syncProduct(any());

        producer.syncProduct(1L, SyncOperationEnum.UPSERT);

        verify(outboxMessageMapper).insert(any(com.mall.product.DO.OutboxMessageDO.class));
    }

    @Test
    @DisplayName("补偿重投：适配器成功时返回 true")
    void resyncReturnsTrueWhenAdapterSucceeds() {
        assertThat(producer.resync(1L, SyncOperationEnum.UPSERT)).isTrue();

        verify(remoteSearchAdapter).syncProduct(any());
    }

    @Test
    @DisplayName("补偿重投失败：返回 false 且不写 Outbox（否则补偿表随每轮重试无限增长）")
    void resyncDoesNotWriteOutboxWhenAdapterFails() {
        doThrow(new BusinessException(ErrorCode.SYSTEM_ERROR)).when(remoteSearchAdapter).syncProduct(any());

        assertThat(producer.resync(1L, SyncOperationEnum.UPSERT)).isFalse();

        verify(outboxMessageMapper, never()).insert(any(com.mall.product.DO.OutboxMessageDO.class));
    }
}
