package com.mall.product.controller.inner;

import com.mall.product.infrastructure.schedule.SearchSyncScheduleTask;
import com.mall.product.infrastructure.schedule.HotRankRefreshTask;
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

/**
 * {@link RemoteProductInnerController} 单元测试
 *
 * <p>覆盖 Feign 内部调用的 Outbox 补偿、热点排名刷新等端点</p>
 *
 * @author JH-Mall
 * @date 2026/06/01
 */
@ExtendWith(MockitoExtension.class)
class RemoteProductInnerControllerTest {

    @Mock
    private ISkuService skuService;

    @Mock
    private IStockService stockService;

    @Mock
    private ISpuService spuService;

    @Mock
    private HotRankRefreshTask hotRankRefreshTask;

    @Mock
    private SearchSyncScheduleTask searchSyncScheduleTask;

    @InjectMocks
    private RemoteProductInnerController controller;

    private MockMvc mockMvc;

    /**
     * 使用 MockMvc 独立构建内部 Controller 层测试
     */
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

    /**
     * POST /inner/product/hot/refresh 应调用 hotProductService.refreshHotRank()
     */
    @Test
    void refreshHotRankShouldCallService() throws Exception {
        mockMvc.perform(post("/inner/product/hot/refresh"))
                .andExpect(status().isOk());

        verify(hotRankRefreshTask).execute();
    }

    /**
     * POST /inner/product/stock/restock 应绑全 skuId/qty/bizNo 并透传给 Service
     *
     * <p>{@code bizNo} 是新增的必填幂等键，Feign 侧靠参数名字符串对齐，
     * 写错只会在运行时 400——必须由本用例锁住绑定。</p>
     */
    @Test
    void restockShouldBindAllParams() throws Exception {
        mockMvc.perform(post("/inner/product/stock/restock")
                        .param("skuId", "101")
                        .param("qty", "5")
                        .param("bizNo", "AS001"))
                .andExpect(status().isOk());

        verify(stockService).restock(101L, 5, "AS001");
    }

    /**
     * 缺少 bizNo 时必须在参数绑定层直接 400，而不是静默执行非幂等回补
     */
    @Test
    void restockShouldRejectMissingBizNo() throws Exception {
        mockMvc.perform(post("/inner/product/stock/restock")
                        .param("skuId", "101")
                        .param("qty", "5"))
                .andExpect(status().isBadRequest());
    }
}
