package com.mall.product.controller;

import com.mall.common.DTO.MallResult;
import com.mall.product.VO.BrandVO;
import com.mall.product.service.IBrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 品牌 Controller
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class BrandController {

    private final IBrandService brandService;

    /**
     * 获取品牌列表
     *
     * @param categoryId 类目 ID（可选）
     * @return 品牌列表
     */
    @GetMapping("/brands")
    public MallResult<List<BrandVO>> list(@RequestParam(required = false) Long categoryId) {
        return MallResult.success(brandService.list(categoryId));
    }
}
