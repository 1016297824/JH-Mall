package com.mall.product.controller;

import com.mall.common.DTO.MallResult;
import com.mall.common.DTO.PageResult;
import com.mall.product.VO.SpuDetailVO;
import com.mall.product.VO.SpuVO;
import com.mall.product.service.ISpuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * SPU Controller
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class SpuController {

    private final ISpuService spuService;

    /**
     * 分页查询 SPU
     *
     * @param page      页码
     * @param size      每页条数
     * @param categoryId 类目 ID（可选）
     * @param brandId   品牌 ID（可选）
     * @param keyword   关键词（可选）
     * @param sort      排序方式（可选）
     * @return SPU 分页
     */
    @GetMapping("/spus")
    public MallResult<PageResult<SpuVO>> list(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                @RequestParam(required = false) Long categoryId,
                                                @RequestParam(required = false) Long brandId,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String sort) {
        return MallResult.success(spuService.page(page, size, categoryId, brandId, keyword, sort));
    }

    /**
     * 获取 SPU 详情（含 SKU 列表）
     *
     * @param spuId SPU ID
     * @return SPU 详情
     */
    @GetMapping("/spus/{spuId}")
    public MallResult<SpuDetailVO> detail(@PathVariable Long spuId) {
        return MallResult.success(spuService.detail(spuId));
    }
}
