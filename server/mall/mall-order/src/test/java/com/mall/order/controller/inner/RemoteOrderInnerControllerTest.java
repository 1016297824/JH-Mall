package com.mall.order.controller.inner;

import com.mall.order.mapper.MallOrderMapper;
import com.mall.order.service.AfterSaleService;
import com.mall.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link RemoteOrderInnerController} 单元测试
 *
 * <p>覆盖发货 / 揽收两个内部端点。参数靠名字字符串与 Feign 侧对齐，
 * 写错只会在运行时 400，必须由本用例锁住绑定。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class RemoteOrderInnerControllerTest {

    @Mock private MallOrderMapper orderMapper;

    @Mock private OrderService orderService;

    @Mock private AfterSaleService afterSaleService;

    @InjectMocks private RemoteOrderInnerController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("发货端点：绑定 orderNo/logisticsCompany/logisticsNo 并透传 Service")
    void deliverShouldBindAllParams() throws Exception {
        mockMvc.perform(post("/inner/order/deliver")
                        .param("orderNo", "ORD001")
                        .param("logisticsCompany", "顺丰速运")
                        .param("logisticsNo", "SF123456"))
                .andExpect(status().isOk());

        verify(orderService).deliver("ORD001", "顺丰速运", "SF123456");
    }

    @Test
    @DisplayName("发货端点：缺物流单号时绑定层直接 400")
    void deliverShouldRejectMissingLogisticsNo() throws Exception {
        mockMvc.perform(post("/inner/order/deliver")
                        .param("orderNo", "ORD001")
                        .param("logisticsCompany", "顺丰速运"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("揽收端点：绑定 orderNo 并透传 Service")
    void logisticsPickShouldBindOrderNo() throws Exception {
        mockMvc.perform(post("/inner/order/logistics-pick").param("orderNo", "ORD001"))
                .andExpect(status().isOk());

        verify(orderService).logisticsPick("ORD001");
    }

    @Test
    @DisplayName("强制取消端点：绑定 orderNo 与客服填写的 cancelReason 并透传 Service")
    void forceCancelShouldBindParams() throws Exception {
        mockMvc.perform(post("/inner/order/force-cancel")
                        .param("orderNo", "ORD001")
                        .param("cancelReason", "支付通道测试单"))
                .andExpect(status().isOk());

        verify(orderService).forceCancel("ORD001", "支付通道测试单");
    }

    @Test
    @DisplayName("售后审核通过端点：绑定 afterSaleId/remark 并透传 Service")
    void approveAfterSaleShouldBindParams() throws Exception {
        mockMvc.perform(post("/inner/order/after-sale/approve")
                        .param("afterSaleId", "77")
                        .param("remark", "同意退款"))
                .andExpect(status().isOk());

        verify(afterSaleService).approve(77L, "同意退款");
    }

    @Test
    @DisplayName("售后驳回端点：绑定 afterSaleId/remark 并透传 Service")
    void rejectAfterSaleShouldBindParams() throws Exception {
        mockMvc.perform(post("/inner/order/after-sale/reject")
                        .param("afterSaleId", "77")
                        .param("remark", "不符合退款条件"))
                .andExpect(status().isOk());

        verify(afterSaleService).reject(77L, "不符合退款条件");
    }
}
