package com.mall.product.service;

import com.mall.product.VO.CategoryVO;

import java.util.List;

/**
 * 类目服务接口
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
public interface ICategoryService {

    /**
     * 获取类目树
     *
     * @return 类目树（多级结构）
     */
    List<CategoryVO> tree();

    /**
     * 根据类目 ID 获取详情
     *
     * @param categoryId 类目 ID
     * @return 类目信息
     */
    CategoryVO getByCategoryId(Long categoryId);
}
