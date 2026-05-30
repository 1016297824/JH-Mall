package com.mall.product.service;

import com.mall.product.VO.SpuVO;
import java.util.List;

public interface ISearchFallbackService {
    List<SpuVO> search(String keyword, int page, int size);
}
