package com.mall.product.controller.inner;

import com.mall.common.enums.product.SyncOperationEnum;
import com.mall.product.infrastructure.mq.SearchSyncProducer;
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

    @Mock
    private SearchSyncProducer searchSyncProducer;

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

    /**
     * POST /inner/product/spus/sync-search：UPSERT 走同步、DELETE 走删除
     *
     * <p>这是「商品变更 → ES」的唯一触发入口，参数名靠字符串与 Feign 对齐。</p>
     */
    @Test
    void syncSearchIndexShouldDelegateWithResolvedOperation() throws Exception {
        mockMvc.perform(post("/inner/product/spus/sync-search")
                        .param("spuId", "1001")
                        .param("operation", "UPSERT"))
                .andExpect(status().isOk());

        verify(searchSyncProducer).syncProduct(1001L, SyncOperationEnum.UPSERT);

        mockMvc.perform(post("/inner/product/spus/sync-search")
                        .param("spuId", "1002")
                        .param("operation", "DELETE"))
                .andExpect(status().isOk());

        verify(searchSyncProducer).syncProduct(1002L, SyncOperationEnum.DELETE);
    }

    /**
     * 无法识别的操作码按 UPSERT 处理：宁可按更新投递，也不要静默不同步
     */
    @Test
    void syncSearchIndexShouldFallbackToUpsert() throws Exception {
        mockMvc.perform(post("/inner/product/spus/sync-search")
                        .param("spuId", "1003")
                        .param("operation", "WHATEVER"))
                .andExpect(status().isOk());

        verify(searchSyncProducer).syncProduct(1003L, SyncOperationEnum.UPSERT);
    }
}
