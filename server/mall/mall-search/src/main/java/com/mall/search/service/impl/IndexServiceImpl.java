package com.mall.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.mall.common.DTO.PageResult;
import com.mall.common.DTO.product.SpuSearchDTO;
import com.mall.common.constant.CacheConstants;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.search.DO.ProductIndexDO;
import com.mall.search.config.MallSearchConfigProperties;
import com.mall.search.convert.request.SpuSearchConvert;
import com.mall.search.infrastructure.feign.RemoteProductAdapter;
import com.mall.search.repository.ProductIndexRepository;
import com.mall.search.service.IndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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

    private static final ScheduledExecutorService CLEANUP_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "es-index-cleanup");
                t.setDaemon(true);
                return t;
            });

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
        String newIndexName = null;
        String oldIndexName = null;
        try {
            // TODO: (JH-Mall, 2026/06/19) ES 9.x Java client 获取磁盘信息的 API 需后续确认，
            //       在 doRebuild 前调用 /_nodes/stats 检查各节点磁盘使用率，超过水位线时抛 BusinessException(ErrorCode.SYSTEM_CAPACITY)
            log.info("全量重建索引开始");

            // ① 清理所有 mall_product* 旧索引（含 Spring Data ES 自动创建的 + 上次失败的版本化索引）
            deleteAllProductIndices();

            // ② 创建新索引并写入 mapping + settings
            String version = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern(configProperties.getRebuild().getTimestampFormat()));
            newIndexName = "mall_product_v" + version;
            createIndexWithMapping(newIndexName);
            log.info("新索引创建完成: {}", newIndexName);

            // ②b 先切别名到新索引，再用 saveAll 写入（利用 Spring Data ES 序列化，避免日期格式不兼容）
            oldIndexName = getCurrentIndexName();
            switchAlias(oldIndexName, newIndexName);

            // ③ 分批拉取 mall-product 全量商品并写入新索引
            int batchSize = configProperties.getRebuild().getBatchSize();
            int page = 1;
            long totalIndexed = 0L;
            while (true) {
                PageResult<SpuSearchDTO> pageResult = remoteProductAdapter.fetchAllSpusForSearch(page, batchSize);
                List<SpuSearchDTO> rows = pageResult.getRows();
                if (rows == null || rows.isEmpty()) {
                    break;
                }
                List<ProductIndexDO> batch = new ArrayList<>(rows.size());
                for (SpuSearchDTO dto : rows) {
                    ProductIndexDO indexDO = SpuSearchConvert.toProductIndex(dto);
                    if (indexDO != null) {
                        batch.add(indexDO);
                    }
                }
                productIndexRepository.saveAll(batch);
                totalIndexed += batch.size();
                log.info("全量重建进度: page={}, 已索引 {} 条", page, totalIndexed);
                if ((long) page * batchSize >= pageResult.getTotal()) {
                    break;
                }
                page++;
            }
            log.info("全量灌入完成，共索引 {} 条", totalIndexed);

            // ⑤ 增量回补（简化实现：T1 后变更需 mall-product 提供时间窗口查询，待后续补充）
            // TODO: (JH-Mall, 2026/06/19) 增量回补 — 重建开始后至切别名前的变更扫描 + Outbox 回放

            // ⑥ 保留上一版本供回滚，30min 后清理
            if (oldIndexName != null) {
                scheduleOldIndexCleanup(oldIndexName);
            }

            log.info("全量重建索引完成");
        } catch (IOException e) {
            log.error("全量重建索引失败", e);
            if (newIndexName != null) {
                String indexToDelete = newIndexName;
                try {
                    elasticsearchClient.indices().delete(d -> d.index(indexToDelete));
                } catch (Exception ignored) {
                    // 清理失败不影响主流程
                }
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
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
        } else if ("UPSERT".equals(operation)) {
            upsertProduct(spuId);
        }
    }

    @Override
    public void rollback() {
        try {
            String currentIndex = getCurrentIndexName();
            if (currentIndex == null) {
                log.warn("回滚失败：当前无生效索引");
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
            // 查找上一个版本索引（按名称倒序，取第一个非当前索引）
            String previousIndex = findPreviousIndex(currentIndex);
            if (previousIndex == null) {
                log.warn("回滚失败：无上一版本索引");
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
            switchAlias(currentIndex, previousIndex);
            log.info("索引回滚完成: {} → {}", currentIndex, previousIndex);
        } catch (Exception e) {
            log.error("索引回滚失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    // ======================== 私有辅助方法 ========================

    /**
     * 清理所有 mall_product 相关索引（版本化 + 同名索引）
     *
     * <p>应对 Spring Data ES 启动时自动创建同名索引，以及上次重建失败残留的版本化索引。</p>
     */
    private void deleteAllProductIndices() {
        try {
            // getAlias 通配列出所有 mall_product* 索引，逐个删除
            var response = elasticsearchClient.indices().getAlias(a -> a.name("mall_product*"));
            var indices = response.aliases().keySet();
            if (!indices.isEmpty()) {
                for (String indexName : indices) {
                    elasticsearchClient.indices().delete(d -> d.index(indexName));
                }
                log.info("已清理 {} 个 mall_product 索引: {}", indices.size(), indices);
            }
        } catch (Exception e) {
            log.debug("清理 mall_product* 索引失败（可能已不存在）: {}", e.getMessage());
        }
    }

    /**
     * 在新索引上创建 mapping + settings
     *
     * @param indexName 新索引名
     */
    private void createIndexWithMapping(String indexName) throws IOException {
        elasticsearchClient.indices().create(c -> c
                .index(indexName)
                .settings(s -> s
                        .numberOfShards(Integer.toString(configProperties.getEs().getShards()))
                        .numberOfReplicas(Integer.toString(configProperties.getEs().getReplicas()))
                        .refreshInterval(ri -> ri.time("5s"))
                )
                .mappings(m -> m
                        .properties("productId", p -> p.long_(l -> l))
                        .properties("spuName", p -> p.text(t -> t
                                .analyzer("ik_max_word").searchAnalyzer("ik_max_word")))
                        .properties("subTitle", p -> p.text(t -> t
                                .analyzer("ik_max_word").searchAnalyzer("ik_max_word")))
                        .properties("keyword", p -> p.keyword(k -> k))
                        .properties("categoryId", p -> p.long_(l -> l))
                        .properties("categoryName", p -> p.keyword(k -> k))
                        .properties("brandId", p -> p.long_(l -> l))
                        .properties("brandName", p -> p.keyword(k -> k))
                        .properties("price", p -> p.integer(i -> i))
                        .properties("salesCount", p -> p.integer(i -> i))
                        .properties("tags", p -> p.keyword(k -> k))
                        .properties("image", p -> p.keyword(k -> k.index(false)))
                        .properties("isOnSale", p -> p.boolean_(b -> b))
                        .properties("createTime", p -> p.date(d -> d.format("yyyy-MM-dd'T'HH:mm:ss||yyyy-MM-dd")))
                        .properties("spuSpecs", p -> p.text(t -> t
                                .analyzer("ik_max_word").searchAnalyzer("ik_max_word")))
                        .properties("suggest", p -> p.completion(cp -> cp
                                .analyzer("ik_max_word").maxInputLength(50)))
                )
        );
    }

    /**
     * 获取当前别名 {@code mall_product} 指向的索引名
     *
     * @return 索引名，别名不存在时返回 null
     */
    private String getCurrentIndexName() {
        try {
            var response = elasticsearchClient.indices().getAlias(a -> a.name("mall_product"));
            return response.aliases().keySet().stream().findFirst().orElse(null);
        } catch (Exception e) {
            log.debug("获取别名索引失败（可能首次创建）: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 原子切换别名（先 remove 旧再 add 新）
     *
     * @param oldIndexName 旧索引名，可以为 null（首次创建）
     * @param newIndexName 新索引名
     */
    private void switchAlias(String oldIndexName, String newIndexName) throws IOException {
        elasticsearchClient.indices().updateAliases(ua -> ua
                .actions(actions -> {
                    // 先移除所有索引上的 mall_product 别名（含旧 is_write_index）
                    actions.remove(r -> r.index("mall_product_v*").alias("mall_product"));
                    // 再加到新索引，指定为 write index
                    actions.add(a -> a.index(newIndexName).alias("mall_product").isWriteIndex(true));
                    return actions;
                })
        );
    }

    /**
     * 延迟 30 分钟清理旧索引
     *
     * @param oldIndexName 待清理的旧索引名
     */
    private void scheduleOldIndexCleanup(String oldIndexName) {
        CLEANUP_EXECUTOR.schedule(() -> {
            try {
                elasticsearchClient.indices().delete(d -> d.index(oldIndexName));
                log.info("旧索引清理完成: {}", oldIndexName);
            } catch (Exception e) {
                log.warn("旧索引清理失败: {}", oldIndexName, e);
            }
        }, 30, TimeUnit.MINUTES);
    }

    /**
     * 查找上一个版本索引（按名称倒序，排除当前索引）
     *
     * @param currentIndex 当前生效索引名
     * @return 上一版本索引名，不存在时返回 null
     */
    private String findPreviousIndex(String currentIndex) {
        try {
            var response = elasticsearchClient.indices().getAlias(a -> a.name("mall_product_v*"));
            return response.aliases().keySet().stream()
                    .filter(name -> !name.equals(currentIndex))
                    .max(Comparator.naturalOrder())
                    .orElse(null);
        } catch (Exception e) {
            log.warn("查找上一版本索引失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 mall-product 拉取最新 SpuSearchDTO 并写入 ES（UPSERT）
     *
     * <p>当前通过分页遍历全量数据查找目标 spuId，后续 mall-product 提供单条查询 API 后可优化。</p>
     *
     * @param spuId 商品 SPU ID
     */
    private void upsertProduct(Long spuId) {
        int page = 1;
        int batchSize = configProperties.getRebuild().getBatchSize();
        try {
            while (true) {
                PageResult<SpuSearchDTO> pageResult = remoteProductAdapter.fetchAllSpusForSearch(page, batchSize);
                List<SpuSearchDTO> rows = pageResult.getRows();
                if (rows == null || rows.isEmpty()) {
                    break;
                }
                for (SpuSearchDTO dto : rows) {
                    if (spuId.equals(dto.getSpuId())) {
                        ProductIndexDO indexDO = SpuSearchConvert.toProductIndex(dto);
                        if (indexDO != null) {
                            productIndexRepository.save(indexDO);
                        }
                        return;
                    }
                }
                if ((long) page * batchSize >= pageResult.getTotal()) {
                    break;
                }
                page++;
            }
            log.warn("增量同步 UPSERT 未找到商品: spuId={}", spuId);
        } catch (Exception e) {
            log.error("增量同步 UPSERT 失败: spuId={}", spuId, e);
        }
    }

}
