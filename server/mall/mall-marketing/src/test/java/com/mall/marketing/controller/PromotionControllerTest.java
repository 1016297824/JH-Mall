package com.mall.marketing.controller;

import com.mall.marketing.dto.response.PromotionResp;
import com.mall.marketing.service.PromotionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C 端促销活动控制器单元测试
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@ExtendWith(MockitoExtension.class)
class PromotionControllerTest {

    @Mock private PromotionService promotionService;
    @InjectMocks private PromotionController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/marketing/promotions 返回进行中活动列表")
    void listActivePromotions() throws Exception {
        PromotionResp resp = new PromotionResp();
        resp.setId(1L);
        resp.setPromotionName("满 200 减 30");
        when(promotionService.listActivePromotions(20)).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/marketing/promotions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].promotionName").value("满 200 减 30"));
    }

    @Test
    @DisplayName("limit 超过服务端上限时被截断到 50")
    void listActivePromotionsCapsLimit() throws Exception {
        when(promotionService.listActivePromotions(50)).thenReturn(List.of());

        mockMvc.perform(get("/api/marketing/promotions").param("limit", "999"))
                .andExpect(status().isOk());

        verify(promotionService).listActivePromotions(50);
    }
}
