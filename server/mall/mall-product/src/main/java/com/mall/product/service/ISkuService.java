package com.mall.product.service;

import com.mall.common.DTO.product.ProductSkuDTO;
import com.mall.product.VO.SkuVO;

import java.util.List;

/**
 * SKU 服务接口
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
public interface ISkuService {

    /**
     * 根据 SKU ID 查询 SKU 详情
     *
     * @param skuId SKU ID
     * @return SKU 详情
     */
    SkuVO getBySkuId(Long skuId);

    /**
     * 批量查询 SKU DTO（供 Feign 内部调用）
     *
     * @param skuIds SKU ID 列表
     * @return SKU DTO 列表
     */
    List<ProductSkuDTO> batchGetSkuDTOs(List<Long> skuIds);
}
