package com.mall.product.service;

import com.mall.product.VO.CategoryVO;

import java.util.List;

public interface ICategoryCacheService {

    List<CategoryVO> getTree();

    void refreshCache();
}
