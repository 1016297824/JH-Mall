package com.mall.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.mall.common.DTO.PageResult;
import com.mall.common.DTO.product.SpuSearchDTO;
import com.mall.common.constant.CacheConstants;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.search.DO.ProductIndexDO;
import com.mall.search.config.MallSearchConfigProperties;
import com.mall.search.infrastructure.feign.RemoteProductAdapter;
import com.mall.search.repository.ProductIndexRepository;
import com.mall.search.service.IndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 商品搜索索引管理服务实现
 *
 * <p>负责全量/增量索引重建、商品同步及回滚。</p>
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexService {

    private final ProductIndexRepository productIndexRepository;
    private final ElasticsearchClient elasticsearchClient;
    private final RemoteProductAdapter remoteProductAdapter;
    private final StringRedisTemplate stringRedisTemplate;
    private final MallSearchConfigProperties configProperties;

    @Override
    public void rebuildIndex() {
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(CacheConstants.Search.INDEX_REBUILD_LOCK, lockValue, 3600, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            log.warn("全量重建正在执行中");
            throw new BusinessException(ErrorCode.SYSTEM_CAPACITY);
        }
        try {
            // TODO: (JH-Mall, 2026/06/19) ES 9.x Java client 获取磁盘信息的 API 需后续确认，
            //       在 doRebuild 前调用 /_nodes/stats 检查各节点磁盘使用率，超过水位线时抛 BusinessException(ErrorCode.SYSTEM_CAPACITY)
            // TODO: 完整重建逻辑（创建索引→全量灌入→增量回补→别名切换）
            log.info("全量重建索引开始");
            // TODO: (JH-Mall, 2026/06/19) 重建完成后 30min 延时清理旧索引（ScheduledExecutorService 或 Redis 延迟队列）
            log.info("全量重建索引完成");
        } finally {
            String current = stringRedisTemplate.opsForValue().get(CacheConstants.Search.INDEX_REBUILD_LOCK);
            if (lockValue.equals(current)) {
                stringRedisTemplate.delete(CacheConstants.Search.INDEX_REBUILD_LOCK);
            }
        }
    }

    @Override
    public void syncProduct(Long spuId, String operation) {
        String dedupKey = CacheConstants.Search.DEDUP + spuId + ":" + operation;
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", 1, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(acquired)) {
            return;
        }
        if ("DELETE".equals(operation)) {
            productIndexRepository.deleteById(spuId);
        } else {
            // TODO: (JH-Mall, 2026/06/19) UPSERT 需要从 mall-product 获取完整 SpuSearchDTO 转换后写入 ES。
            //       当前仅记录日志，索引重建/定时补偿时完整数据写入。
            log.info("增量同步 UPSERT 暂未实现: spuId={}", spuId);
        }
    }

    @Override
    public void rollback() {
        // TODO: 回滚实现
    }
}
