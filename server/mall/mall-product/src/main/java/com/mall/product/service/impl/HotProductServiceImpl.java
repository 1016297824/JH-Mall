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
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
        // 从 Redis ZSet 获取热度降序的 spuId 列表
        BoundZSetOperations<String, Object> zSetOps = redisTemplate.boundZSetOps(CacheConstants.Product.HOT_RANK);
        Set<Object> spuIdSet = zSetOps.reverseRange(0, limit - 1);
        if (spuIdSet == null || spuIdSet.isEmpty()) {
            return fallbackMysql(limit);
        }
        // 优先从 Caffeine 本地缓存读取，未命中再查 MySQL
        List<SpuVO> result = new ArrayList<>();
        List<Long> missedIds = new ArrayList<>();
        for (Object obj : spuIdSet) {
            String spuIdStr = (String) obj;
            Long spuId = Long.valueOf(spuIdStr);
            SpuVO cached = hotProductCache.getIfPresent(spuId);
            if (cached != null) {
                result.add(cached);
            } else {
                missedIds.add(spuId);
            }
        }
        // 批量回查未命中 ID 并回种 Caffeine 缓存
        if (!missedIds.isEmpty()) {
            List<MallProductSpuDO> spuDOList = spuMapper.selectBatchIds(missedIds);
            List<SpuVO> voList = SpuConvert.toSpuVOList(spuDOList);
            for (SpuVO vo : voList) {
                hotProductCache.put(Long.valueOf(vo.getSpuId()), vo);
            }
            result.addAll(voList);
        }
        return result;
    }

    /**
     * MySQL 降级查询（ZSet 为空时按销量排序兜底）
     *
     * @param limit 返回条数
     * @return 按销量降序的 SPU 列表
     */
    private List<SpuVO> fallbackMysql(int limit) {
        Page<MallProductSpuDO> page = new Page<>(1, limit);
        LambdaQueryWrapper<MallProductSpuDO> wrapper = new LambdaQueryWrapper<MallProductSpuDO>()
                .eq(MallProductSpuDO::getPublishStatus, 1)
                .eq(MallProductSpuDO::getVerifyStatus, 1)
                .eq(MallProductSpuDO::getIsDeleted, 0)
                .orderByDesc(MallProductSpuDO::getSalesCount);
        Page<MallProductSpuDO> result = spuMapper.selectPage(page, wrapper);
        List<SpuVO> voList = SpuConvert.toSpuVOList(result.getRecords());
        BoundZSetOperations<String, Object> zSetOps = redisTemplate.boundZSetOps(CacheConstants.Product.HOT_RANK);
        for (SpuVO vo : voList) {
            hotProductCache.put(Long.valueOf(vo.getSpuId()), vo);
            zSetOps.add(vo.getSpuId(), vo.getSalesCount() != null ? vo.getSalesCount() * 10.0 : 0);
        }
        return voList;
    }

    @Override
    public void incrUv(Long spuId, Long userId) {
        redisTemplate.opsForHyperLogLog().add(CacheConstants.Product.UV + spuId, userId.toString());
    }

    @Override
    public void refreshHotRank() {
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(CacheConstants.Job.LOCK_HOT_RANK, "1", Duration.ofSeconds(600));
        if (!Boolean.TRUE.equals(locked)) {
            return;
        }
        try {
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
            double salesWeight = configProps.getHot().getSalesWeight();
            double uvWeight = configProps.getHot().getUvWeight();
            BoundZSetOperations<String, Object> zSetOps = redisTemplate.boundZSetOps(CacheConstants.Product.HOT_RANK);
            redisTemplate.delete(CacheConstants.Product.HOT_RANK);
            Set<String> topSpuIds = new java.util.HashSet<>();
            for (MallProductSpuDO spu : spuList) {
                String spuIdStr = String.valueOf(spu.getId());
                topSpuIds.add(spuIdStr);
                int salesCount = spu.getSalesCount() != null ? spu.getSalesCount() : 0;
                Long uv = redisTemplate.opsForHyperLogLog().size(CacheConstants.Product.UV + spuIdStr);
                double newScore = salesCount * 10 * salesWeight + uv * uvWeight;
                zSetOps.add(spuIdStr, newScore);
            }
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
            redisTemplate.delete(CacheConstants.Job.LOCK_HOT_RANK);
        }
    }
}
