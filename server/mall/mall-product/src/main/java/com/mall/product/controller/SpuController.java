package com.mall.product.controller;

import com.mall.common.DTO.MallResult;
import com.mall.common.DTO.PageResult;
import com.mall.product.VO.SpuDetailVO;
import com.mall.product.VO.SpuVO;
import com.mall.product.service.ISpuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class SpuController {

    private final ISpuService spuService;

    @GetMapping("/spus")
    public MallResult<PageResult<SpuVO>> list(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               @RequestParam(required = false) Long categoryId,
                                               @RequestParam(required = false) Long brandId,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String sort) {
        return MallResult.success(spuService.page(page, size, categoryId, brandId, keyword, sort));
    }

    @GetMapping("/spus/{spuId}")
    public MallResult<SpuDetailVO> detail(@PathVariable Long spuId) {
        return MallResult.success(spuService.detail(spuId));
    }
}
