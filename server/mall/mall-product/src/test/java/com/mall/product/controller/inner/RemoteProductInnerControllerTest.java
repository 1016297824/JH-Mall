package com.mall.product.controller.inner;

import com.mall.product.infrastructure.schedule.SearchSyncScheduleTask;
import com.mall.product.service.IHotProductService;
import com.mall.product.service.ISkuService;
import com.mall.product.service.ISpuService;
import com.mall.product.service.IStockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RemoteProductInnerControllerTest {

    @Mock
    private ISkuService skuService;

    @Mock
    private IStockService stockService;

    @Mock
    private ISpuService spuService;

    @Mock
    private IHotProductService hotProductService;

    @Mock
    private SearchSyncScheduleTask searchSyncScheduleTask;

    @InjectMocks
    private RemoteProductInnerController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void compensateOutboxShouldCallTask() throws Exception {
        mockMvc.perform(post("/inner/product/outbox/compensate"))
                .andExpect(status().isOk());

        verify(searchSyncScheduleTask).execute();
    }

    @Test
    void refreshHotRankShouldCallService() throws Exception {
        mockMvc.perform(post("/inner/product/hot/refresh"))
                .andExpect(status().isOk());

        verify(hotProductService).refreshHotRank();
    }
}
