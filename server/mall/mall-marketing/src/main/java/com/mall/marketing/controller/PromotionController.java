package com.mall.marketing.controller;

import com.mall.common.DTO.MallResult;
import com.mall.marketing.dto.response.PromotionResp;
import com.mall.marketing.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端促销活动控制器
 *
 * <p>对应设计文档 {@code docs/design/14_mall-marketing详细设计.md} §2.2 端点 4（无需登录）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@RestController
@RequestMapping("/api/marketing/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    /** 单次返回上限的下界，非正数兜底为该值 */
    private static final int MIN_LIMIT = 1;

    /** 单次返回上限的上界，超过则截断为该值 */
    private static final int MAX_LIMIT = 50;

    /**
     * 查询当前进行中的活动列表（无需登录）
     *
     * @param limit 单次返回上限，归一化到 {@code [1, 50]} 后透传
     * @return 进行中的活动列表
     */
    @GetMapping
    public MallResult<List<PromotionResp>> listActivePromotions(
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return MallResult.success(promotionService.listActivePromotions(normalizeLimit(limit)));
    }

    /**
     * 归一化单次返回上限
     *
     * <p>下界 {@value #MIN_LIMIT}、上界 {@value #MAX_LIMIT}：小于下界的兜底为 1，
     * 超过上界的截断为 50，其余原样透传。</p>
     *
     * @param limit 原始 limit 参数
     * @return 落在 {@code [1, 50]} 区间内的 limit
     */
    private int normalizeLimit(int limit) {
        if (limit < MIN_LIMIT) {
            return MIN_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
