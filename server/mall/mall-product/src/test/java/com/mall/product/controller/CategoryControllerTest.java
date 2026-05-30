package com.mall.product.controller;

import com.mall.product.VO.CategoryVO;
import com.mall.product.service.ICategoryCacheService;
import com.mall.product.service.ICategoryService;
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
class CategoryControllerTest {

    @Mock
    private ICategoryCacheService categoryCacheService;

    @Mock
    private ICategoryService categoryService;

    @InjectMocks
    private CategoryController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void treeShouldReturnCategoryTree() throws Exception {
        CategoryVO vo = new CategoryVO();
        vo.setCategoryId("1");
        vo.setName("手机数码");
        when(categoryCacheService.getTree()).thenReturn(List.of(vo));

        mockMvc.perform(get("/api/product/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].categoryId").value("1"))
                .andExpect(jsonPath("$.data[0].name").value("手机数码"));
    }

    @Test
    void detailShouldReturnCategory() throws Exception {
        CategoryVO vo = new CategoryVO();
        vo.setCategoryId("1");
        vo.setName("手机数码");
        when(categoryService.getByCategoryId(1L)).thenReturn(vo);

        mockMvc.perform(get("/api/product/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryId").value("1"))
                .andExpect(jsonPath("$.data.name").value("手机数码"));
    }
}
