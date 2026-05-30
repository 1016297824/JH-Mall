package com.mall.product.service;

import com.mall.product.VO.CategoryVO;

import java.util.List;

public interface ICategoryService {

    List<CategoryVO> tree();

    CategoryVO getByCategoryId(Long categoryId);
}
