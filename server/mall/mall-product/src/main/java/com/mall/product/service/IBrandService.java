package com.mall.product.service;

import com.mall.product.VO.BrandVO;

import java.util.List;

/**
 * 品牌服务接口
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
public interface IBrandService {

    /**
     * 获取品牌列表
     *
     * @param categoryId 类目 ID（可选，传 null 返回全部）
     * @return 品牌列表
     */
    List<BrandVO> list(Long categoryId);
}
