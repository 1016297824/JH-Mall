package com.mall.product.service;

import com.mall.product.VO.SpuVO;

import java.util.List;

/**
 * 热点商品服务接口
 *
 * <p>提供热点商品排行查询、UV 统计、排名增量更新、全量刷新等功能。
 * 基于 Redis ZSet + Caffeine 两级缓存实现高性能热点排行。</p>
 *
 * @author JH-Mall
 * @date 2026/06/01
 */
public interface IHotProductService {

    /**
     * 获取热点商品列表
     *
     * <p>优先从 Caffeine 本地缓存读取，未命中则回查 Redis ZSet。
     * ZSet 为空时降级到 MySQL 按销量排序。</p>
     *
     * @param limit 返回条数（最大 50）
     * @return 热度降序商品列表
     */
    List<SpuVO> hotList(int limit);

    /**
     * 记录商品 UV（独立访客）
     *
     * <p>使用 HyperLogLog 存储，误差约 0.81%，用于热度分计算。</p>
     *
     * @param spuId  SPU ID
     * @param userId 用户 ID（未登录传 0）
     */
    void incrUv(Long spuId, Long userId);

    /**
     * 全量刷新热点排名
     *
     * <p>定时任务触发，查 MySQL sales_count + Redis UV 重算 ZSet score。
     * 使用 SETNX 分布式锁防止多实例并发。</p>
     */
    void refreshHotRank();
}
