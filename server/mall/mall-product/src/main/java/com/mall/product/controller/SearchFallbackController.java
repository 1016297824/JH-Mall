package com.mall.product.controller;

import com.mall.common.DTO.MallResult;
import com.mall.product.VO.SpuVO;
import com.mall.product.service.ISearchFallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 搜索降级 Controller
 *
 * <p>搜索引擎不可用时，由此 Controller 提供降级搜索能力</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class SearchFallbackController {

    private final ISearchFallbackService searchFallbackService;

    /**
     * 降级搜索
     *
     * @param keyword 搜索关键词
     * @param page    页码
     * @param size    每页条数
     * @return SPU 列表
     */
    @GetMapping("/search/fallback")
    public MallResult<List<SpuVO>> search(@RequestParam String keyword,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return MallResult.success(searchFallbackService.search(keyword, page, size));
    }
}
