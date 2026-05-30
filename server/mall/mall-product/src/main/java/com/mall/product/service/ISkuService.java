package com.mall.product.service;

import com.mall.common.DTO.product.ProductSkuDTO;
import com.mall.product.VO.SkuVO;

import java.util.List;

public interface ISkuService {

    SkuVO getBySkuId(Long skuId);

    List<ProductSkuDTO> batchGetSkuDTOs(List<Long> skuIds);
}
