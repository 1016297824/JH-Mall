package com.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.constant.CacheConstants;
import com.mall.product.DO.MallProductSpuDO;
import com.mall.product.VO.SpuVO;
import com.mall.product.config.MallProductConfigProperties;
import com.mall.product.convert.response.SpuConvert;
import com.mall.product.mapper.MallProductSpuMapper;
import com.mall.product.service.IHotProductService;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

/**
 * 热点商品服务实现
 *
 * <p>Caffeine+Redis 两级缓存实现高性能热点排行。
 * Caffeine 存放热数据 VO（最大 500 条，5min 过期），Redis ZSet 维护全局排名。</p>
 *
 * @author JH-Mall
 * @date 2026/06/01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotProductServiceImpl implements IHotProductService {

    /** Caffeine 本地热点商品缓存（key = spuId，5min 无访问过期） */
    private final Cache<Long, SpuVO> hotProductCache;
    /** Redis 模板（ZSet 排行 + HyperLogLog UV + 分布式锁） */
    private final RedisTemplate<String, Object> redisTemplate;
    /** SPU Mapper（ZSet 为空时 MySQL 降级） */
    private final MallProductSpuMapper spuMapper;
    /** 热点配置（rankMaxSize / salesWeight / uvWeight） */
    private final MallProductConfigProperties configProps;

    @Override
    public List<SpuVO> hotList(int limit) {
        // 第 1 步：从 Redis ZSet 获取热度降序的 spuId 列表（score=综合热度分）
        BoundZSetOperations<String, Object> zSetOps = redisTemplate.boundZSetOps(CacheConstants.Product.HOT_RANK);
        Set<Object> spuIdSet = zSetOps.reverseRange(0, limit - 1);
        // ZSet 为空（首次启动或缓存过期），降级到 MySQL 按销量排序兜底
        if (spuIdSet == null || spuIdSet.isEmpty()) {
            synchronized (this) {
                spuIdSet = zSetOps.reverseRange(0, limit - 1);
                if (spuIdSet == null || spuIdSet.isEmpty()) {
                    return fallbackMysql(limit);
                }
            }
        }
        // 第 2 步：遍历 ZSet 中的 spuId，优先从 Caffeine 本地缓存获取 VO
        // 用 ArrayList 维护与 ZSet 一致的排名顺序（通过 null 占位），保证返回顺序与 Redis 排名一致
        List<SpuVO> result = new ArrayList<>();
        List<Long> missedIds = new ArrayList<>();
        for (Object obj : spuIdSet) {
            String spuIdStr = (String) obj;
            Long spuId = Long.valueOf(spuIdStr);
            SpuVO cached = hotProductCache.getIfPresent(spuId);
            if (cached != null) {
                // Caffeine 命中，直接加入结果集
                result.add(cached);
            } else {
                // Caffeine 未命中，占位 null 保持索引位置，稍后批量回查 MySQL 后按原位置填入
                result.add(null);
                missedIds.add(spuId);
            }
        }
        // 第 3 步：Caffeine 未命中的 ID 批量回查 MySQL，按 ZSet 排名顺序回填并回种缓存
        if (!missedIds.isEmpty()) {
            List<MallProductSpuDO> spuDOList = spuMapper.selectByIds(missedIds);
            Map<Long, SpuVO> voMap = new HashMap<>();
            for (SpuVO vo : SpuConvert.toSpuVOList(spuDOList)) {
                Long id = Long.valueOf(vo.getSpuId());
                voMap.put(id, vo);
                hotProductCache.put(id, vo);
            }
            List<Object> spuIdList = new ArrayList<>(spuIdSet);
            for (int i = 0; i < result.size(); i++) {
                if (result.get(i) == null) {
                    Long spuId = Long.valueOf((String) spuIdList.get(i));
                    result.set(i, voMap.get(spuId));
                }
            }
        }
        result.removeIf(Objects::isNull);
        return result;
    }

    /**
     * MySQL 降级查询（ZSet 为空时按销量排序兜底）
     *
     * <p>降级的同时回填 Caffeine 和 ZSet 缓存，避免后续请求持续降级到 DB</p>
     *
     * @param limit 返回条数
     * @return 按销量降序的 SPU 列表
     */
    private List<SpuVO> fallbackMysql(int limit) {
        // 按 rankMaxSize（默认 200）取全部种子数据，用于回填缓存，不局限于 limit
        int seedSize = configProps.getHot().getRankMaxSize();
        Page<MallProductSpuDO> page = new Page<>(1, seedSize);
        LambdaQueryWrapper<MallProductSpuDO> wrapper = new LambdaQueryWrapper<MallProductSpuDO>()
                .eq(MallProductSpuDO::getPublishStatus, 1)
                .eq(MallProductSpuDO::getVerifyStatus, 1)
                .eq(MallProductSpuDO::getIsDeleted, 0)
                .orderByDesc(MallProductSpuDO::getSalesCount);
        Page<MallProductSpuDO> result = spuMapper.selectPage(page, wrapper);
        List<SpuVO> allList = SpuConvert.toSpuVOList(result.getRecords());
        // 回填 Caffeine 本地缓存 + Redis ZSet，种子数据比请求量更大，使 Caffeine 有更充足的命中储备
        BoundZSetOperations<String, Object> zSetOps = redisTemplate.boundZSetOps(CacheConstants.Product.HOT_RANK);
        for (SpuVO vo : allList) {
            hotProductCache.put(Long.valueOf(vo.getSpuId()), vo);
            zSetOps.add(vo.getSpuId(), vo.getSalesCount() != null ? vo.getSalesCount() * 10.0 : 0);
        }
        // 只返回调用方请求的 limit 条，多余的种子数据留在缓存中供后续请求使用
        return allList.size() > limit ? allList.subList(0, limit) : allList;
    }

    @Override
    public void incrUv(Long spuId, Long userId) {
        // 使用 Redis HyperLogLog 统计 UV，误差约 0.81%，空间占用固定 12KB
        // key = mall:product:uv:{spuId}，value = userId，去重统计独立访客
        redisTemplate.opsForHyperLogLog().add(CacheConstants.Product.UV + spuId, userId.toString());
    }

    @Override
    public void refreshHotRank() {
        // 使用 SETNX 获取分布式锁，防止多实例并发刷新热点排名
        // key = mall:job:lock:hot:rank，TTL 600 秒防止锁未释放导致死锁
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(CacheConstants.Job.LOCK_HOT_RANK, "1", Duration.ofSeconds(600));
        if (!Boolean.TRUE.equals(locked)) {
            // 其他实例已持有锁，直接跳过本次刷新
            return;
        }
        try {
            // 从 MySQL 查询已上架已审核的 SPU，按销量降序取前 rankMaxSize 条
            int rankMaxSize = configProps.getHot().getRankMaxSize();
            Page<MallProductSpuDO> page = new Page<>(1, rankMaxSize);
            LambdaQueryWrapper<MallProductSpuDO> wrapper = new LambdaQueryWrapper<MallProductSpuDO>()
                    .eq(MallProductSpuDO::getPublishStatus, 1)
                    .eq(MallProductSpuDO::getVerifyStatus, 1)
                    .eq(MallProductSpuDO::getIsDeleted, 0)
                    .orderByDesc(MallProductSpuDO::getSalesCount);
            Page<MallProductSpuDO> result = spuMapper.selectPage(page, wrapper);
            List<MallProductSpuDO> spuList = result.getRecords();
            if (spuList.isEmpty()) {
                return;
            }
            // 从配置读取销量权重和 UV 权重（默认 0.6 : 0.4）
            double salesWeight = configProps.getHot().getSalesWeight();
            double uvWeight = configProps.getHot().getUvWeight();
            BoundZSetOperations<String, Object> zSetOps = redisTemplate.boundZSetOps(CacheConstants.Product.HOT_RANK);
            // 清空旧 ZSet，全量重建新排名
            redisTemplate.delete(CacheConstants.Product.HOT_RANK);
            // 记录本轮 Top N 的 spuId，后续用于清理过期 UV 键
            Set<String> topSpuIds = new java.util.HashSet<>();
            for (MallProductSpuDO spu : spuList) {
                String spuIdStr = String.valueOf(spu.getId());
                topSpuIds.add(spuIdStr);
                // 综合热度分 = 销量 * 10 * 销量权重 + PFCOUNT(UV) * 10 * UV 权重
                // PFCOUNT 返回 HyperLogLog 的基数估算值
                int salesCount = spu.getSalesCount() != null ? spu.getSalesCount() : 0;
                Long uv = redisTemplate.opsForHyperLogLog().size(CacheConstants.Product.UV + spuIdStr);
                double newScore = salesCount * 10 * salesWeight + uv * 10 * uvWeight;
                zSetOps.add(spuIdStr, newScore);
            }
            // 清理未进入 Top N 的过期 UV HyperLogLog 键，防止 Redis 内存无限膨胀
            Set<String> uvKeys = redisTemplate.keys(CacheConstants.Product.UV + "*");
            if (uvKeys != null) {
                for (String uvKey : uvKeys) {
                    String spuId = uvKey.substring(CacheConstants.Product.UV.length());
                    if (!topSpuIds.contains(spuId)) {
                        redisTemplate.delete(uvKey);
                    }
                }
            }
        } finally {
            // 释放分布式锁
            redisTemplate.delete(CacheConstants.Job.LOCK_HOT_RANK);
        }
    }
}
