package com.mall.product.service;

import com.mall.product.VO.SkuVO;

/**
 * SKU 缓存服务接口
 *
 * <p>对 SKU 查询做 Redis 缓存</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
public interface ISkuCacheService {

    /**
     * 获取 SKU 详情（带缓存）
     *
     * @param skuId SKU ID
     * @return SKU 详情
     */
    SkuVO getBySkuId(Long skuId);

    /**
     * 清除 SKU 缓存
     *
     * @param skuId SKU ID
     */
    void evictCache(Long skuId);
}
