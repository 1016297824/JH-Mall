package com.mall.product.service;

import com.mall.product.VO.BrandVO;

import java.util.List;

public interface IBrandService {

    List<BrandVO> list(Long categoryId);
}
