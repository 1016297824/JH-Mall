package com.mall.product.controller;

import com.mall.common.DTO.PageResult;
import com.mall.common.handler.MallExceptionHandler;
import com.mall.product.VO.SpuDetailVO;
import com.mall.product.VO.SpuVO;
import com.mall.product.service.IHotProductService;
import com.mall.product.service.ISpuService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SpuControllerTest {

    @Mock
    private ISpuService spuService;

    @Mock
    private IHotProductService hotProductService;

    @InjectMocks
    private SpuController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new MallExceptionHandler())
                .build();
    }

    @Test
    void listShouldReturnSpuPage() throws Exception {
        SpuVO vo = new SpuVO();
        vo.setSpuId("1");
        vo.setSpuName("iPhone");
        PageResult<SpuVO> pageResult = PageResult.of(1, 20, 1, List.of(vo));
        when(spuService.page(1, 20, null, null, null, null)).thenReturn(pageResult);

        mockMvc.perform(get("/api/product/spus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.rows[0].spuName").value("iPhone"));
    }

    @Test
    void detailShouldReturnSpuDetail() throws Exception {
        SpuDetailVO detail = new SpuDetailVO();
        detail.setSpuId("1");
        detail.setSpuName("iPhone");
        when(spuService.detail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/product/spus/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.spuName").value("iPhone"));
    }

    @Test
    void hotListShouldReturnSpuList() throws Exception {
        SpuVO vo = new SpuVO();
        vo.setSpuId("1");
        vo.setSpuName("热门商品");
        when(spuService.hotList(20)).thenReturn(List.of(vo));

        mockMvc.perform(get("/api/product/spus/hot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].spuName").value("热门商品"));
    }

    @Test
    void hotListShouldRejectLimitExceed50() throws Exception {
        mockMvc.perform(get("/api/product/spus/hot").param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A0803"));
    }
}
