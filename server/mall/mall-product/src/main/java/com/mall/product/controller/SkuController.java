package com.mall.product.controller;

import com.mall.common.DTO.MallResult;
import com.mall.product.VO.SkuVO;
import com.mall.product.service.ISkuCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class SkuController {

    private final ISkuCacheService skuCacheService;

    @GetMapping("/skus/{skuId}")
    public MallResult<SkuVO> detail(@PathVariable Long skuId) {
        return MallResult.success(skuCacheService.getBySkuId(skuId));
    }
}
