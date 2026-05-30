package com.mall.product.controller;

import com.mall.product.VO.BrandVO;
import com.mall.product.service.IBrandService;
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
class BrandControllerTest {

    @Mock private IBrandService brandService;
    @InjectMocks private BrandController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void listShouldReturnBrandList() throws Exception {
        BrandVO vo = new BrandVO(); vo.setBrandId("1"); vo.setName("Apple");
        when(brandService.list(null)).thenReturn(List.of(vo));

        mockMvc.perform(get("/api/product/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].brandId").value("1"))
                .andExpect(jsonPath("$.data[0].name").value("Apple"));
    }
}
