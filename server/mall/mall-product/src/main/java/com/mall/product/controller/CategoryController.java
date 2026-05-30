package com.mall.product.controller;

import com.mall.common.DTO.MallResult;
import com.mall.product.VO.CategoryVO;
import com.mall.product.service.ICategoryCacheService;
import com.mall.product.service.ICategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 类目 Controller
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class CategoryController {

    private final ICategoryCacheService categoryCacheService;
    private final ICategoryService categoryService;

    /**
     * 获取类目树
     *
     * @return 类目树
     */
    @GetMapping("/categories")
    public MallResult<List<CategoryVO>> tree() {
        return MallResult.success(categoryCacheService.getTree());
    }

    /**
     * 获取类目详情
     *
     * @param categoryId 类目 ID
     * @return 类目信息
     */
    @GetMapping("/categories/{categoryId}")
    public MallResult<CategoryVO> detail(@PathVariable Long categoryId) {
        return MallResult.success(categoryService.getByCategoryId(categoryId));
    }
}
