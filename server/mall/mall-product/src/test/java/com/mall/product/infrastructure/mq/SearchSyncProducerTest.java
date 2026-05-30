package com.mall.product.infrastructure.mq;

import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.common.enums.product.SyncOperationEnum;
import com.mall.product.infrastructure.feign.RemoteSearchAdapter;
import com.mall.product.mapper.OutboxMessageMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
}
