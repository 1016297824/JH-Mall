package com.mall.search.controller;

import com.mall.common.DTO.MallResult;
import com.mall.search.DTO.request.SearchReqDTO;
import com.mall.search.service.IndexService;
import com.mall.search.service.SearchService;
import com.mall.search.service.SuggestService;
import com.mall.search.vo.SearchResultVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端商品搜索 Controller
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final SuggestService suggestService;
    private final IndexService indexService;

    @Operation(summary = "商品全文检索")
    @PostMapping("/api/search")
    public MallResult<SearchResultVO> search(@Valid @RequestBody SearchReqDTO req) {
        return MallResult.success(searchService.search(req));
    }

    @Operation(summary = "搜索建议")
    @GetMapping("/api/search/suggest")
    public MallResult<List<String>> suggest(@RequestParam("keyword") String keyword) {
        return MallResult.success(suggestService.suggest(keyword));
    }

    @Operation(summary = "全量重建搜索索引")
    @PostMapping("/mall-search/index/rebuild")
    public MallResult<Void> rebuildIndex() {
        indexService.rebuildIndex();
        return MallResult.success(null);
    }
}
