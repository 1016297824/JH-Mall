# JH-Store mall-search 模块详细设计

> 基于系统详细设计 `03_系统详细设计.md` 展开。mall-search 不持有 MySQL 表，数据存储在 Elasticsearch 中。

---

## 1 模块概述

### 1.1 子领域

| 子领域   | 存储 | 说明                                                      |
| -------- | ---- | --------------------------------------------------------- |
| 全文搜索 | ES   | 商品名称/副标题/规格多字段匹配，BM25 相关性排序           |
| 聚合筛选 | ES   | 按类目/品牌/价格区间/标签聚合，动态计算各筛选维度下的数量 |
| 搜索建议 | ES   | 输入补全（completion suggester），基于商品名称            |
| 索引管理 | ES   | 全量重建、增量同步、别名原子切换、版本回滚                |
| 降级兜底 | —   | ES 不可用时转发到 mall-product DB 兜底查询                |

### 1.2 依赖关系

```
mall-search (9307端口)
  ├── MySQL：无（不操作关系数据库）
  ├── Elasticsearch：索引 storage + 搜索查询
  ├── Redis：分布式锁（全量重建防并发）、搜索结果缓存（热点词 1min）
  ├── RocketMQ (Consumer)：消费 mall:search:sync → 增量同步索引
  ├── mall-product (Feign Caller)：调 RemoteProductService 取全量/增量商品数据
  └── mall-product (被调)：提供 RemoteSearchService.syncProduct 供商品服务直推索引
```

> **关键约束**：搜索服务是唯一不操作 MySQL 的业务服务。ES 不可用时降级到 mall-product 的 DB 兜底查询。索引变更通过别名原子切换，不原地修改 mapping。

---

## 2 包结构与接口映射

### 2.1 包结构

```
server/mall/mall-search/
└── src/main/java/com/mall/search/
    ├── MallSearchApplication.java           # Spring Boot 启动类
    ├── controller/
    │   └── SearchController.java            # 单 Controller，C 端搜索 + 管理端维护
    ├── dto/
    │   ├── request/
    │   │   └── SearchReq.java               # 搜索请求（keyword/filters/sort/page）
    │   └── response/
    │       └── SearchResp.java              # 搜索结果（items + aggregations + total）
    ├── vo/
    │   ├── SearchItemVO.java                # 搜索结果单条（spuId/name/price/image）
    │   └── AggregationVO.java               # 聚合统计（类目/品牌/价格区间各 count）
    ├── DO/
    │   └── ProductIndex.java                # ES 索引实体（非 MySQL DO，使用 @Document）
    ├── service/
    │   ├── SearchService.java
    │   ├── IndexService.java
    │   ├── SuggestService.java
    │   └── impl/
    │       ├── SearchServiceImpl.java
    │       ├── IndexServiceImpl.java
    │       └── SuggestServiceImpl.java
    ├── repository/
    │   └── ProductIndexRepository.java      # Spring Data ES Repository
    ├── infrastructure/
    │   ├── mq/
    │   │   └── SearchSyncConsumer.java      # 消费 mall:search:sync 增量同步
    │   └── feign/
    │       └── RemoteProductAdapter.java     # 调 mall-product 取商品数据
    └── convert/
        ├── request/                         # Request → DO（入站）
        └── response/                        # DO → VO（出站）
            └── SearchConvert.java           # ProductIndex → SearchItemVO
```

### 2.2 接口 → Controller 映射

| # | 方法 | 路径                                 | 方法名               | 需登录 | 说明                             |
| - | ---- | ------------------------------------ | -------------------- | :----: | -------------------------------- |
| 1 | GET  | `/api/search`                      | `search(req)`      |   否   | 商品全文搜索（含筛选/排序/聚合） |
| 2 | GET  | `/api/search/suggest`              | `suggest(keyword)` |   否   | 搜索补全建议                     |
| 3 | POST | `/mall-search/index/rebuild` | `rebuildIndex()`   | 管理端 | 触发全量重建     |

### 2.3 Lombok 使用约定

本模块与其它 C 端模块不同：**无 MySQL DO 类**，仅有 ES `@Document` 索引实体。

| 类层级 | 注解 | 说明 |
|--------|------|------|
| `DO/ProductIndex.java` | `@Data` | ES 索引实体（`@Document`），非 JPA Entity，`@Data` 可用 |
| `dto/request/`, `dto/response/` | `@Data` + `@NoArgsConstructor` | — |
| `vo/` | `@Data` | 视图对象 |
| `service/impl/` | `@Slf4j` + `@RequiredArgsConstructor` | 构造器注入 |
| `controller/` | `@Slf4j` + `@RequiredArgsConstructor` | — |
| `convert/request/`, `convert/response/` | 无 Lombok | 静态转换方法 |

> `ProductIndex` 是 ES 实体（Spring Data Elasticsearch），非 JPA Entity，`@Data` 不违反"禁止用于 JPA Entity"约束。

详见 `AGENTS.md` §Lombok 使用规范。

---

## 3 核心类设计

### 3.1 ProductIndex — ES 索引实体

位于 `DO/ProductIndex.java`，使用 `@Document(indexName = "mall_product")` 通过别名读写。

| 字段         | ES 类型               | 说明                           |
| ------------ | --------------------- | ------------------------------ |
| productId    | long                  | SPU ID                         |
| spuName      | text (ik_max_word)    | 商品名称，权重 3.0             |
| subTitle     | text (ik_max_word)    | 副标题，权重 1.5               |
| keyword      | keyword               | 精确匹配用，如品牌词、热搜词   |
| categoryId   | long                  | 类目 ID，用于 term 过滤        |
| categoryName | keyword               | 类目名称，聚合展示             |
| brandId      | long                  | 品牌 ID                        |
| brandName    | keyword               | 品牌名称                       |
| price        | integer               | 最低售价（分），C 端除以 100   |
| salesCount   | integer               | 累计销量，排序用               |
| tags         | keyword[]             | 标签数组，term 过滤 + 聚合     |
| image        | keyword (index=false) | 商品主图，不索引仅返回         |
| isOnSale     | boolean               | 上架状态，filter 过滤下架商品  |
| createTime   | date                  | 创建时间，新品排序             |
| spuSpecs     | text (ik_max_word)    | SKU 规格文本拼接，权重 1.0     |
| suggest      | completion            | 搜索补全，使用 `ik_max_word` |

**索引 settings：**

- `number_of_shards`: 3（生产），1（开发）
- `number_of_replicas`: 1（生产），0（开发）
- `refresh_interval`: 5s

**约束：**

- `price` 用 integer（分），避免浮点精度；前端除以 100 显示
- `image` 不索引，节省空间
- `isOnSale` 用 filter（不参与评分且缓存，性能优于 must）
- 禁止存 PII（手机号、邮箱、身份证等不进 ES）
- `suggest` 用 `completion` 类型，`max_input_length=50`

### 3.2 SearchServiceImpl

位于 `service/impl/SearchServiceImpl.java`，ES 查询核心。

**search(req)**：

- 分词器：`ik_max_word`（最细粒度），保证高召回
- 全文搜索：`multiMatchQuery` 跨 `spuName`(3.0) + `subTitle`(1.5) + `spuSpecs`(1.0)，`type=BEST_FIELDS`（ES 9.x 需改为 `"best_fields"` 字符串或 `bool/should`）
- 筛选：全部用 `filter`（不参与评分、自动缓存），包括类目/品牌/价格区间/标签/`isOnSale=true`
- 排序：`_score`(综合) / `salesCount DESC`(销量) / `price ASC|DESC`(价格) / `createTime DESC`(新品)
- 分页：单页最大 60 条，`from+size ≤ 10000`，超限返回 `A0802`
- 高亮：`spuName` 用 `<em>` 标签包裹
- 聚合：类目（terms 50）/ 品牌（terms 50）/ 价格区间（range: 0-100/100-200/200+）
- 超时 2s，超时返回部分结果

**热点词缓存**：搜索结果 Redis 缓存 1 分钟（key: `mall:search:result:{md5(query)}`），缓存命中率 >60% 的搜索不走 ES。

### 3.3 IndexServiceImpl

位于 `service/impl/IndexServiceImpl.java`，索引生命周期管理。

**rebuildIndex()**：

- ①Redis 分布式锁 `mall:search:index:rebuild_lock`，SETNX + UUID + 看门狗 3600s
- ②创建新索引 `mall_product_v{yyyyMMddHHmmss}`，写入 mapping + settings
- ③分批拉取 mall-product 全量商品：`RemoteProductAdapter.fetchAllSpus(page, 500)`
- ④批量写入新索引（`BulkRequest`，每批 500 条）
- ⑤增量回补：全量完成后，扫描 T1（重建开始时刻）后的商品变更，同步到新索引
- ⑥原子切换别名：`_aliases` API，先 remove 旧索引再 add 新索引（单别名策略，读写共用）
- ⑦保留上一版本索引供回滚，更早版本删除

**syncProduct(productIndex)**：增量同步（Feign 直调）

- 操作类型：`UPSERT`（上架/改价/改名）或 `DELETE`（下架/删除）
- 写入 ES，失败记录日志不阻塞（异步 Outbox 兜底）

**rollback()**：回滚到上一版本索引，通过 `_aliases` API 原子切换

**索引生命周期**：

| 配置       | 值                     | 说明                 |
| ---------- | ---------------------- | -------------------- |
| 保留版本数 | 2                      | 当前 + 上一版        |
| 清理时机   | 切换成功观察 30 分钟后 | 不立即删，留观察窗口 |
| 磁盘告警   | 可用磁盘 < 20%         | 暂停新索引创建       |

### 3.4 SuggestServiceImpl

位于 `service/impl/SuggestServiceImpl.java`，搜索补全。

- 基于 ES `completion` suggester + `ik_max_word` 分词
- 商品上架/改名时更新 `suggest` 字段
- 输入 "苹果" → 返回 "苹果手机"、"苹果电脑"、"苹果耳机"
- Redis 缓存热门建议 5 分钟

### 3.5 搜索降级

ES 不可用时：

| 策略     | 触发                | 行为                                                            |
| -------- | ------------------- | --------------------------------------------------------------- |
| 转发降级 | ES 连接超时或 5xx   | 调 `mall-product` 的 `/api/product/search/fallback` DB 兜底 |
| 直接降级 | mall-product 也失败 | 返回 `A0801`，userTip "搜索服务暂时不可用"                    |
| 缓存兜底 | Redis 有缓存        | 优先返回缓存的搜索结果                                          |

---

## 4 ES 索引别名策略

### 4.1 单别名

读写共用同一别名，不做读写分离（搜索场景以读为主，写极少）：

```
别名: mall_product → 指向唯一生效索引
索引: mall_product_v{yyyyMMddHHmmss}
```

### 4.2 原子切换

```json
POST /_aliases
{
  "actions": [
    { "remove": { "index": "mall_product_v20260517120000", "alias": "mall_product" } },
    { "add":    { "index": "mall_product_v20260518020000", "alias": "mall_product" } }
  ]
}
```

- `_aliases` actions 数组在 ES 集群内部是原子操作
- 先 remove 再 add，确保同一时刻仅一个索引承载别名
- 新索引上线后所有搜索立即路由到新索引，无感知零停机

### 4.3 回滚

新索引上线后发现搜索质量问题，立即回滚：

```json
POST /_aliases
{
  "actions": [
    { "remove": { "index": "mall_product_v20260518020000", "alias": "mall_product" } },
    { "add":    { "index": "mall_product_v20260517120000", "alias": "mall_product" } }
  ]
}
```

回滚后必须同步增量变更（回滚窗口内新增/下架/改价的商品）。

---

## 5 RocketMQ 事件

### 5.1 消费的事件

| Topic                | 消费者类               | 处理流程                                                                                                   |
| -------------------- | ---------------------- | ---------------------------------------------------------------------------------------------------------- |
| `mall:search:sync` | `SearchSyncConsumer` | ①幂等去重 `mall:search:dedup:{productId}:{operation}` ②调 `IndexService.syncProduct()` ③增量写入 ES |

### 5.2 幂等

- Key：`mall:search:dedup:{productId}:{operation}`（productId + UPSERT/DELETE）
- Redis SETNX，TTL 1h，覆盖最长补偿周期
- 命中 → 跳过重复同步

---

## 6 Nacos 配置

### 6.1 DataId: `mall-search-dev.yml`

```yaml
spring:
  cloud:
    sentinel:
      eager: true
      transport:
         dashboard: 127.0.0.1:8718
       datasource:
         ds1:
           nacos:
             server-addr: 127.0.0.1:8848
             dataId: sentinel-mall-search
             groupId: DEFAULT_GROUP
             data-type: json
             rule-type: flow
    datasource:
    dynamic:
      primary: master
      datasource:
        master:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://localhost:3306/mall?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8
          username: root
          password: 138992
  elasticsearch:
    uris: http://localhost:9200
    connection-timeout: 5s
    socket-timeout: 30s

springdoc:
  gatewayUrl: http://localhost:8080/${spring.application.name}
  api-docs:
    enabled: true
  info:
    title: '搜索模块接口文档'
    description: '搜索模块接口描述'
    contact:
      name: RuoYi
      url: https://ruoyi.vip

mall:
  search:
    es:
      hosts: localhost:9200
      shards: 3
      replicas: 1
    query:
      timeout: 2000
    page:
      max-size: 60
      max-depth: 10000
    suggest:
      cache-ttl: 300
    result:
      cache-ttl: 60
    rebuild:
      batch-size: 500
      timestamp-format: yyyyMMddHHmmss
    index:
      keep-versions: 2
    disk:
      warning-threshold: 20
```

> 以上配置通过 Nacos 下发，支持 `@RefreshScope` 运行时动态刷新（标 * 的需重启生效）。
> 配置项通过 `MallSearchConfigProperties`（`@ConfigurationProperties(prefix = "mall.search")` + `@RefreshScope`）注入，各 Service/Controller 通过构造注入获取，禁止使用 `@Value`。

### 6.2 本地配置文件 `bootstrap.yml`

```yaml
# mall-search 搜索服务
server:
  port: 9307

spring:
  application:
    name: mall-search
  profiles:
    active: dev
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
      config:
        server-addr: 127.0.0.1:8848
  config:
    file-extension: yml
    import:
      - nacos:application-${spring.profiles.active}.${spring.config.file-extension}
      - nacos:${spring.application.name}-${spring.profiles.active}.${spring.config.file-extension}
```

> 注：`file-extension` 和 `import` 必须放在 `spring.config.*` 下（而非 `spring.cloud.nacos.config.*`），否则 Nacos 配置不会被加载。

### 6.3 配置项说明

| 配置项                                   | 默认值             | 说明                      |
| ---------------------------------------- | ------------------ | ------------------------- |
| `mall.search.es.hosts` *                 | `localhost:9200` | ES 集群地址               |
| `mall.search.es.shards` *                | 3                  | 索引分片数（生产）        |
| `mall.search.es.replicas` *              | 1                  | 索引副本数（生产）        |
| `mall.search.query.timeout`            | 2000ms             | 搜索超时                  |
| `mall.search.page.max-size`            | 60                 | 单页最大条数              |
| `mall.search.page.max-depth`           | 10000              | 分页最大深度（from+size） |
| `mall.search.suggest.cache-ttl`        | 300s               | 搜索建议缓存              |
| `mall.search.result.cache-ttl`         | 60s                | 搜索结果缓存              |
| `mall.search.rebuild.batch-size`       | 500                | 全量重建单批条数          |
| `mall.search.rebuild.timestamp-format` | `yyyyMMddHHmmss` | 索引版本号格式            |
| `mall.search.index.keep-versions`      | 2                  | 保留索引版本数            |
| `mall.search.disk.warning-threshold`   | 20%                | 磁盘告警阈值              |

---

## 7 错误码汇总

| 错误码 | HTTP | userTip                          | 说明              |
| ------ | :--: | -------------------------------- | ----------------- |
| 00000  | 200 | —                               | 成功              |
| A0801  | 400 | 搜索关键词有误                   | 搜索参数不合法    |
| A0802  | 400 | 搜索结果超出限制，请细化筛选条件 | 分页超过深度上限  |
| C0110  | 500 | 服务暂时不可用                   | Redis 连接失败    |
| C0140  | 503 | 搜索服务暂时不可用               | ES 连接失败或超时 |

> 全部错误码来自系统设计第二章。

---
