package com.mall.product.service;

import com.mall.product.VO.SkuVO;

public interface ISkuCacheService {

    SkuVO getBySkuId(Long skuId);

    void evictCache(Long skuId);
}
