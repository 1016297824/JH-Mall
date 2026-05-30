package com.mall.product.controller;

import com.mall.common.DTO.MallResult;
import com.mall.product.VO.SkuVO;
import com.mall.product.service.ISkuCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * SKU Controller
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class SkuController {

    private final ISkuCacheService skuCacheService;

    /**
     * 获取 SKU 详情
     *
     * @param skuId SKU ID
     * @return SKU 详情
     */
    @GetMapping("/skus/{skuId}")
    public MallResult<SkuVO> detail(@PathVariable Long skuId) {
        return MallResult.success(skuCacheService.getBySkuId(skuId));
    }
}
