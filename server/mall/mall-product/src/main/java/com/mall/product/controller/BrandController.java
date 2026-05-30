package com.mall.product.controller;

import com.mall.common.DTO.MallResult;
import com.mall.product.VO.BrandVO;
import com.mall.product.service.IBrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class BrandController {

    private final IBrandService brandService;

    @GetMapping("/brands")
    public MallResult<List<BrandVO>> list(@RequestParam(required = false) Long categoryId) {
        return MallResult.success(brandService.list(categoryId));
    }
}
