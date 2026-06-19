package com.mall.search.infrastructure.schedule;

import com.mall.search.service.IndexService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/**
 * IndexRebuildTask 单元测试
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@ExtendWith(MockitoExtension.class)
class IndexRebuildTaskTest {

    @Mock
    private IndexService indexService;

    @InjectMocks
    private IndexRebuildTask task;

    @Test
    void execute_shouldDelegateToIndexService() {
        task.execute();
        verify(indexService).rebuildIndex();
    }
}
