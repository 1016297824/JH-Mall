package com.mall.search.infrastructure.feign;

import com.mall.api.feign.RemoteProductService;
import com.mall.common.DTO.PageResult;
import com.mall.common.DTO.product.SpuSearchDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 商品远程调用适配器
 *
 * <p>封装 RemoteProductService Feign 调用，供索引重建时全量拉取搜索结果专用富 DTO</p>
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteProductAdapter {

    private final RemoteProductService remoteProductService;

    /**
     * 分页拉取全量 SPU（搜索索引重建专用，含类目名、品牌名、SKU 规格）
     *
     * @param page 页码（从 1 开始）
     * @param size 每页条数
     * @return 搜索结果专用 SPU 分页
     */
    public PageResult<SpuSearchDTO> fetchAllSpusForSearch(int page, int size) {
        log.debug("拉取全量 SPU（搜索专用）: page={}, size={}", page, size);
        return remoteProductService.fetchAllSpusForSearch(page, size);
    }

    /**
     * 按 spuId 拉取单条 SPU（搜索增量同步专用）
     *
     * <p>替代全表分页扫描：增量同步只需一条，无需逐页拉全量比对。</p>
     *
     * @param spuId SPU ID
     * @return 搜索结果专用 SPU；商品不存在返回 null
     */
    public SpuSearchDTO fetchSpuForSearch(Long spuId) {
        log.debug("拉取单条 SPU（搜索专用）: spuId={}", spuId);
        return remoteProductService.getSpuForSearch(spuId);
    }

    /**
     * 分页拉取指定时刻之后有变更的 SPU（重建增量回补专用）
     *
     * @param since 起始时刻，ISO-8601 字符串
     * @param page  页码（从 1 开始）
     * @param size  每页条数
     * @return 搜索结果专用 SPU 分页
     */
    public PageResult<SpuSearchDTO> fetchSpusUpdatedSince(String since, int page, int size) {
        log.debug("拉取增量变更 SPU（搜索专用）: since={}, page={}", since, page);
        return remoteProductService.fetchSpusUpdatedSince(since, page, size);
    }
}
