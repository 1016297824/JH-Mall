package com.mall.search.controller.inner;

import com.mall.search.infrastructure.schedule.IndexRebuildTask;
import com.mall.search.service.IndexService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RemoteSearchInnerController 单元测试
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@WebMvcTest(RemoteSearchInnerController.class)
class RemoteSearchInnerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IndexRebuildTask indexRebuildTask;

    @MockitoBean
    private IndexService indexService;

    @Test
    void rebuildIndex_shouldDelegateToTask() throws Exception {
        mockMvc.perform(post("/inner/search/index/rebuild"))
                .andExpect(status().isOk());
        verify(indexRebuildTask).execute();
    }

    @Test
    void syncProduct_shouldDelegateWithTimestamp() throws Exception {
        // 契约 RemoteSearchService.syncProduct 声明的就是这个路径；
        // 它此前在 mall-search 侧从未实现，导致 mall-product 的实时同步恒 404 并全部降级到 Outbox
        mockMvc.perform(post("/inner/search/product/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"spuId\":101,\"operation\":\"UPSERT\",\"timestamp\":1791010000000}"))
                .andExpect(status().isOk());

        verify(indexService).syncProduct(101L, "UPSERT", 1791010000000L);
    }
}
