package com.mall.product.service;

import com.mall.product.VO.CategoryVO;

import java.util.List;

/**
 * 类目缓存服务接口
 *
 * <p>对类目查询做 Redis 缓存，减少 DB 压力</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
public interface ICategoryCacheService {

    /**
     * 获取类目树（带缓存）
     *
     * @return 类目树
     */
    List<CategoryVO> getTree();

    /**
     * 刷新类目缓存
     */
    void refreshCache();
}
