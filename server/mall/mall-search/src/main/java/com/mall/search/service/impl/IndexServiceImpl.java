package com.mall.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.nodes.FileSystemTotal;
import co.elastic.clients.elasticsearch.nodes.NodesStatsResponse;
import co.elastic.clients.elasticsearch.nodes.Stats;
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
        // 重建开始时刻：灌数期间发生的变更可能被随后到达的旧快照覆盖，结束时按此重扫补齐
        LocalDateTime rebuildStart = LocalDateTime.now();
        try {
            // 动手前先看磁盘：重建会删光全部 mall_product* 索引再灌全量，
            // 磁盘写满会让新索引只写一半，连回滚都没得退
            checkDiskWatermark();
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

            // ⑤ 增量回补：补齐灌数期间被旧快照覆盖的变更（见 backfillChangesSince 的说明）
            int backfilled = backfillChangesSince(rebuildStart, batchSize);
            log.info("增量回补完成，共重写 {} 条", backfilled);

            // ⑥ 保留上一版本供回滚，30min 后清理
            if (oldIndexName != null) {
                scheduleOldIndexCleanup(oldIndexName);
            }

            log.info("全量重建索引完成");
        } catch (BusinessException e) {
            // 业务异常原样透传，避免被下方兜底捕获后丢失原始错误码
            throw e;
        } catch (Exception e) {
            // 捕获 Exception 而非 IOException：ES Java Client 在连接失败等场景抛的是
            // ElasticsearchException（运行时异常），仅捕获 IOException 会让其穿透为未处理异常
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
    public void syncProduct(Long spuId, String operation, long sourceTimestamp) {
        // 乱序保护：MQ 重投不保证顺序，一条陈旧的 UPSERT 落在 DELETE 之后会让已删除的商品复活。
        // 只接受比「上一次已生效的同步」更新的消息。
        if (!acceptNewer(spuId, sourceTimestamp)) {
            log.info("搜索同步消息早于已生效版本，跳过: spuId={}, operation={}, ts={}",
                    spuId, operation, sourceTimestamp);
            return;
        }
        // 不做时间窗去重：ES 的 upsert/delete 本身幂等，而"1 小时内只同步一次"
        // 会让同一商品的第二次变更被静默丢弃（索引停在旧值，直到手工全量重建）。
        if ("DELETE".equals(operation)) {
            productIndexRepository.deleteById(spuId);
        } else if ("UPSERT".equals(operation)) {
            upsertProduct(spuId);
        }
    }

    /**
     * 判断该同步消息是否比已生效版本更新，是则记录并放行
     *
     * <p>用 Redis 记录每个 spuId 最近一次已生效的同步时间戳。读取与写入不是原子操作，
     * 极端并发下仍可能放过一条稍旧的消息，但相比原先完全没有保护已是实质改善；
     * 强一致需要把时间戳写进 ES 文档并在写入时比较（代价是每次同步多一次读）。</p>
     *
     * @param spuId           SPU ID
     * @param sourceTimestamp 生产端时间戳（epoch 毫秒）
     * @return 应处理返回 true
     */
    private boolean acceptNewer(Long spuId, long sourceTimestamp) {
        String key = CacheConstants.Search.SYNC_TS + spuId;
        try {
            String last = stringRedisTemplate.opsForValue().get(key);
            if (last != null && Long.parseLong(last) >= sourceTimestamp) {
                return false;
            }
            stringRedisTemplate.opsForValue().set(key, String.valueOf(sourceTimestamp), 7, TimeUnit.DAYS);
            return true;
        } catch (Exception e) {
            // Redis 故障不应阻断索引同步：宁可失去乱序保护，也不能让索引停更
            log.warn("读取搜索同步时间戳失败，跳过乱序保护: spuId={}", spuId, e);
            return true;
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
     * 回补重建期间的增量变更
     *
     * <p><b>为什么必须回补</b>：重建是「先切别名 → 再分批灌全量」，于是存在这个竞态——
     * 第 N 页被读出（商品 X = 旧值）→ X 改价，实时同步把新值写进新索引 →
     * {@code saveAll(第 N 页)} 又把 X 覆盖回旧值。此后这次变更已被实时同步消费掉，
     * 没有任何机制会再修正它，索引就永久停在旧值。</p>
     *
     * <p>做法是按 {@code update_time >= rebuildStart} 重扫一遍逐条重写。
     * 边界取 {@code >=}：同秒内的变更不会被漏，代价只是边界那条重复处理一次，
     * 而 ES 的 upsert 本身幂等。回补期间新增的变更会由实时同步通道自行处理，
     * 故只需扫到「拉取时不再返回新页」为止。</p>
     *
     * @param since     重建开始时刻
     * @param batchSize 单批条数
     * @return 实际重写的条数
     */
    private int backfillChangesSince(LocalDateTime since, int batchSize) {
        int page = 1;
        int backfilled = 0;
        while (true) {
            PageResult<SpuSearchDTO> pageResult =
                    remoteProductAdapter.fetchSpusUpdatedSince(since.toString(), page, batchSize);
            List<SpuSearchDTO> rows = pageResult == null ? null : pageResult.getRows();
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
            backfilled += batch.size();
            if ((long) page * batchSize >= pageResult.getTotal()) {
                break;
            }
            page++;
        }
        return backfilled;
    }

    /**
     * 校验各 ES 节点磁盘可用率，低于水位线时拒绝全量重建
     *
     * <p>调用 {@code /_nodes/stats} 取各节点的 {@code fs.total}，按
     * {@code mall.search.disk.warning-threshold}（默认 20）判定：该值语义为
     * <b>可用空间占比下限</b>，低于它即视为磁盘将满。</p>
     *
     * <p><b>取不到磁盘信息时只告警不阻断</b>：ES 不可达本身会让重建在后续步骤失败并报出真实原因，
     * 而在这里误判拦截会把「运维想重建」变成「必须先去看监控」。</p>
     */
    private void checkDiskWatermark() {
        int threshold = configProperties.getDisk() == null
                ? 0 : configProperties.getDisk().getWarningThreshold();
        if (threshold <= 0) {
            return;
        }
        try {
            NodesStatsResponse stats = elasticsearchClient.nodes().stats();
            if (stats == null || stats.nodes() == null) {
                return;
            }
            for (Map.Entry<String, Stats> entry : stats.nodes().entrySet()) {
                FileSystemTotal total = diskTotalOf(entry.getValue());
                if (total == null || total.totalInBytes() == null || total.availableInBytes() == null) {
                    continue;
                }
                long totalBytes = total.totalInBytes();
                if (totalBytes <= 0) {
                    continue;
                }
                long freePercent = total.availableInBytes() * 100 / totalBytes;
                if (freePercent < threshold) {
                    log.error("ES 节点磁盘可用率低于水位线，拒绝全量重建: node={}, available={}%, threshold={}%",
                            entry.getKey(), freePercent, threshold);
                    throw new BusinessException(ErrorCode.SYSTEM_CAPACITY);
                }
            }
        } catch (BusinessException e) {
            // 业务异常原样透传，避免被下方兜底吞成「跳过校验」
            throw e;
        } catch (Exception e) {
            log.warn("获取 ES 节点磁盘信息失败，跳过水位校验继续重建: {}", e.getMessage());
        }
    }

    /**
     * 取节点磁盘统计中的 total 段
     *
     * @param node 节点统计，可为 null
     * @return total 段，节点未上报文件系统信息时返回 null
     */
    private FileSystemTotal diskTotalOf(Stats node) {
        if (node == null || node.fs() == null) {
            return null;
        }
        return node.fs().total();
    }

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
        try {
            // 单条查询：原先逐页拉全量比对来定位这一条，数据量大时单次同步 O(N)
            SpuSearchDTO dto = remoteProductAdapter.fetchSpuForSearch(spuId);
            if (dto == null) {
                log.warn("增量同步 UPSERT 未找到商品（可能已删除）: spuId={}", spuId);
                return;
            }
            ProductIndexDO indexDO = SpuSearchConvert.toProductIndex(dto);
            if (indexDO != null) {
                productIndexRepository.save(indexDO);
            }
        } catch (Exception e) {
            // 必须上抛：调用方 SearchSyncProducer 只在本方法抛异常时才写 Outbox 兜底，
            // 吞掉异常会让接口对 Feign 返回 200，索引与库的差异再无人修正
            log.error("增量同步 UPSERT 失败，交由调用方写 Outbox 兜底: spuId={}", spuId, e);
            throw e;
        }
    }

}
