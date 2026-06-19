# mall-search 实现决策 Spec

> 基于 `docs/design/11_mall-search详细设计.md`，记录头脑风暴中明确的工程决策。

---

## 1 基线文档

本 Spec 不重复设计文档已有内容，只记录讨论中明确的选择和改动。基线文档：

| 文档 | 说明 |
|------|------|
| `docs/design/11_mall-search详细设计.md` | mall-search 模块完整设计 |
| `docs/design/03_05_系统详细设计-核心业务详细流程.md` | §5.5 搜索索引同步流程 |
| `docs/design/03_06_系统详细设计-缓存与一致性设计.md` | §6.4 幂等设计、§6.5 Outbox |

---

## 2 决策清单

### 2.1 命名修正

| 原设计 | 改为 | 原因 |
|--------|------|------|
| `dto/response/SearchResp.java` | `vo/SearchResultVO.java` | C 端展示用 VO，不用 DTO |

`dto/response/` 暂无内容，预留给未来内部 Feign 响应 DTO。

### 2.2 ES API 分层策略

```
ElasticsearchClient        ← co.elastic.clients 官方原生
       ↑
ElasticsearchOperations    ← Spring Data ES 模板
       ↑
ElasticsearchRepository    ← Spring Data ES Repository
```

| 操作 | 用什么 | 原因 |
|------|--------|------|
| `syncProduct` 单条增删 | `ProductIndexRepository.save()` / `.deleteById()` | 一行搞定 |
| `rebuildIndex` 批量灌入 | `ProductIndexRepository.saveAll()` | 简洁 |
| `search` 全文搜索 | `ElasticsearchOperations.search(NativeQuery)` | fluent builder 构造复杂查询 |
| `suggest` 补全 | `ElasticsearchClient.suggest()` | Spring Data 未封装 completion suggester |
| 别名切换 / 回滚 | `ElasticsearchClient.indices().updateAliases()` | Spring Data 未封装 `_aliases` API |

三者底层同一套 `ElasticsearchClient`，分层只是按操作类型选最合适的封装层次，不是选边站。

### 2.3 热点词缓存命中率

使用 Caffeine 本地 `recordStats()` + `stats().hitRate()`，不用 Redis 计数器。

原因：
- 缓存 TTL 只有 1 分钟，Caffeine 窗口天然对齐
- 热点查询实例间均匀分布，单实例命中率足以代表
- 零网络开销，搜索延迟不受影响
- 判断逻辑：`cache.stats().hitRate() > 0.6` → 跳过 ES

### 2.4 定时全量重建

模式参考 mall-product 的 `SearchSyncScheduleTask` + `RemoteProductInnerController`：

```
ruoyi-job (9204端口)
  └─ POST /inner/search/index/rebuild → RemoteSearchInnerController.rebuildIndex()
                                           └─ IndexRebuildTask.execute()
                                                  └─ IndexService.rebuildIndex()
```

- `IndexRebuildTask.java` 放 `infrastructure/schedule/`，`@Component` + `execute()` 方法
- `RemoteSearchInnerController.java` 放 `controller/inner/`，暴露 `/inner/search/**`
- cron 表达式在 ruoyi-job 控制台配置，不在代码也不在 Nacos
- 建议频率：每天凌晨 3 点，作为增量同步的最终一致性兜底

### 2.5 实现顺序

按依赖链 4 批：

```
第1批: 基础设施
  ProductIndex           — @Document 索引实体
  ProductIndexRepository — Spring Data ES Repository
  SearchReq              — 搜索请求 DTO
  RemoteProductAdapter   — Feign 调 mall-product
  SearchSyncConsumer     — MQ 消费 mall:search:sync

第2批: 索引管理
  IndexServiceImpl       — rebuildIndex / syncProduct / rollback
  IndexRebuildTask       — 定时重建
  RemoteSearchInnerController — /inner/search/index/rebuild

第3批: 搜索服务
  SearchServiceImpl      — 全文搜索 + 聚合 + 高亮 + 热点缓存
  SuggestServiceImpl     — completion 搜索补全
  SearchController       — /api/search + /api/search/suggest + /mall-search/index/rebuild

第4批: 边角补全
  降级兜底              — ES 不可用 → mall-product fallback
  索引清理              — 切换后观察 30 分钟清理旧索引
  磁盘告警              — 可用磁盘 < 20% 暂停重建
```

每批独立可验证，不依赖后续批次。

---

## 3 环境约束

| 项 | 值 |
|----|-----|
| ES 版本 | 9.2.5 |
| IK 分词器 | 待安装（设计按已安装处理，不按退化方案） |
| Spring Boot | 4.0.3（BOM 管理 spring-data-elasticsearch 版本） |
| Java | 21 |
| ES 客户端 | `co.elastic.clients:elasticsearch-java`（Spring Data ES 内部封装） |

---

## 4 设计文档已记录、无需变更的内容

| 内容 | 文档位置 |
|------|----------|
| ProductIndex 字段定义 | §3.1 |
| 索引 settings（shards/replicas/refresh_interval） | §3.1 |
| SearchServiceImpl 查询逻辑 | §3.2 |
| IndexServiceImpl 重建流程（7 步） | §3.3 |
| 索引别名策略（单别名 + 原子切换） | §4 |
| MQ 消费幂等（dedup key / TTL） | §5 |
| Nacos 配置（Nacos 配置清单） | §6 |
| 错误码 | §7 |
| 增量回补数据来源（SPU update_time 字段） | `03_05` §5.5.6 |
