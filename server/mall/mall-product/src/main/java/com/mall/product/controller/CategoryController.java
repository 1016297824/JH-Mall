package com.mall.product.controller;

import com.mall.common.DTO.MallResult;
import com.mall.product.VO.CategoryVO;
import com.mall.product.service.ICategoryCacheService;
import com.mall.product.service.ICategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class CategoryController {

    private final ICategoryCacheService categoryCacheService;
    private final ICategoryService categoryService;

    @GetMapping("/categories")
    public MallResult<List<CategoryVO>> tree() {
        return MallResult.success(categoryCacheService.getTree());
    }

    @GetMapping("/categories/{categoryId}")
    public MallResult<CategoryVO> detail(@PathVariable Long categoryId) {
        return MallResult.success(categoryService.getByCategoryId(categoryId));
    }
}
