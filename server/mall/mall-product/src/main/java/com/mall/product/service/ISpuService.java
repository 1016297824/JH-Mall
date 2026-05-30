package com.mall.product.service;

import com.mall.common.DTO.PageResult;
import com.mall.common.DTO.product.SpuDTO;
import com.mall.product.VO.SpuDetailVO;
import com.mall.product.VO.SpuVO;

public interface ISpuService {

    PageResult<SpuVO> page(int page, int size, Long categoryId, Long brandId, String keyword, String sort);

    SpuDetailVO detail(Long spuId);

    PageResult<SpuDTO> pageForFullRebuild(int page, int size);
}
