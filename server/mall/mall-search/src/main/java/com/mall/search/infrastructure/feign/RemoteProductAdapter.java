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
}
