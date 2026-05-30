package com.mall.product.controller;

import com.mall.common.DTO.MallResult;
import com.mall.product.VO.SpuVO;
import com.mall.product.service.ISearchFallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class SearchFallbackController {

    private final ISearchFallbackService searchFallbackService;

    @GetMapping("/search/fallback")
    public MallResult<List<SpuVO>> search(@RequestParam String keyword,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return MallResult.success(searchFallbackService.search(keyword, page, size));
    }
}
