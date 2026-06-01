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
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotProductServiceImpl implements IHotProductService {

    private final Cache<Long, SpuVO> hotProductCache;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MallProductSpuMapper spuMapper;
    private final MallProductConfigProperties configProps;

    @Override
    public List<SpuVO> hotList(int limit) {
        BoundZSetOperations<String, Object> zSetOps = redisTemplate.boundZSetOps(CacheConstants.Product.HOT_RANK);
        Set<Object> spuIdSet = zSetOps.reverseRange(0, limit - 1);
        if (spuIdSet == null) {
            return fallbackMysql(limit);
        }
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

    private List<SpuVO> fallbackMysql(int limit) {
        Page<MallProductSpuDO> page = new Page<>(1, limit);
        LambdaQueryWrapper<MallProductSpuDO> wrapper = new LambdaQueryWrapper<MallProductSpuDO>()
                .eq(MallProductSpuDO::getPublishStatus, 1)
                .eq(MallProductSpuDO::getVerifyStatus, 1)
                .eq(MallProductSpuDO::getIsDeleted, 0)
                .orderByDesc(MallProductSpuDO::getSalesCount);
        Page<MallProductSpuDO> result = spuMapper.selectPage(page, wrapper);
        return SpuConvert.toSpuVOList(result.getRecords());
    }

    @Override
    public void incrUv(Long spuId, Long userId) {
        redisTemplate.opsForHyperLogLog().add(CacheConstants.Product.UV + spuId, userId.toString());
    }

    @Override
    public void incrHotRank(Long spuId, int quantity) {
        redisTemplate.boundZSetOps(CacheConstants.Product.HOT_RANK)
                .incrementScore(spuId.toString(), quantity * 10.0);
    }

    @Override
    public void refreshHotRank() {
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(CacheConstants.Job.LOCK_HOT_RANK, "1", Duration.ofSeconds(600));
        if (!Boolean.TRUE.equals(locked)) {
            return;
        }
        try {
            BoundZSetOperations<String, Object> zSetOps = redisTemplate.boundZSetOps(CacheConstants.Product.HOT_RANK);
            int rankMaxSize = configProps.getHot().getRankMaxSize();
            Set<ZSetOperations.TypedTuple<Object>> tuples = zSetOps.reverseRangeWithScores(0, rankMaxSize - 1);
            if (tuples == null || tuples.isEmpty()) {
                return;
            }
            double salesWeight = configProps.getHot().getSalesWeight();
            double uvWeight = configProps.getHot().getUvWeight();
            for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
                String spuIdStr = (String) tuple.getValue();
                double salesScore = tuple.getScore();
                Long uv = redisTemplate.opsForHyperLogLog().size(CacheConstants.Product.UV + spuIdStr);
                double newScore = salesScore * salesWeight + uv * uvWeight;
                zSetOps.add(spuIdStr, newScore);
            }
            zSetOps.removeRange(0, -(rankMaxSize + 1));
        } finally {
            redisTemplate.delete(CacheConstants.Job.LOCK_HOT_RANK);
        }
    }
}
