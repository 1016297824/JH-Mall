package com.mall.product.controller;

import com.mall.product.VO.SkuVO;
import com.mall.product.service.ISkuCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SkuControllerTest {

    @Mock private ISkuCacheService skuCacheService;
    @InjectMocks private SkuController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void detailShouldReturnSku() throws Exception {
        SkuVO vo = new SkuVO(); vo.setSkuId("101"); vo.setSkuName("256GB"); vo.setPrice(699900L);
        when(skuCacheService.getBySkuId(101L)).thenReturn(vo);

        mockMvc.perform(get("/api/product/skus/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.skuId").value("101"))
                .andExpect(jsonPath("$.data.skuName").value("256GB"));
    }
}
