package com.mall.search.controller.inner;

import com.mall.search.infrastructure.schedule.IndexRebuildTask;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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

    @Test
    void rebuildIndex_shouldDelegateToTask() throws Exception {
        mockMvc.perform(post("/inner/search/index/rebuild"))
                .andExpect(status().isOk());
        verify(indexRebuildTask).execute();
    }
}
