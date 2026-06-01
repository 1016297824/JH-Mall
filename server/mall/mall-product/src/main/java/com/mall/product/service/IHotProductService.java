package com.mall.product.service;

import com.mall.product.VO.SpuVO;

import java.util.List;

public interface IHotProductService {

    List<SpuVO> hotList(int limit);

    void incrUv(Long spuId, Long userId);

    void incrHotRank(Long spuId, int quantity);

    void refreshHotRank();
}
