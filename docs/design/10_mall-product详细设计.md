# JH-Store mall-product 模块详细设计

> 基于系统详细设计 `03_系统详细设计.md` 展开。数据表 DDL 在系统设计 1.2 节统一维护，此处只引用表名和字段。

---

## 1 模块概述

### 1.1 子领域

| 子领域        | 实体                       | 说明                                                                          |
| ------------- | -------------------------- | ----------------------------------------------------------------------------- |
| 类目管理      | `mall_product_category`  | 三级树形类目（A→B→C），支持 path 快速遍历                                   |
| 品牌管理      | `mall_product_brand`     | 品牌 CRUD，关联类目                                                           |
| SPU 管理      | `mall_product_spu`       | 商品主体信息，含上下架和审核状态                                              |
| SKU 管理      | `mall_product_sku`       | 销售规格，含售价/市场价/成本价/重量                                           |
| 库存管理      | `mall_product_sku_stock` | 四段库存（可用/锁定/已售/冻结），乐观锁防超卖                                 |
| 搜索同步      | Outbox                     | 商品变更后实时+异步双通道同步到 ES                                            |
| RocketMQ 事件 | Outbox                     | 生产 `mall:search:sync`，消费 `mall:order:cancelled`、`mall:order:paid` |

### 1.2 依赖关系

```
mall-product (9303端口)
  ├── MySQL：自有表（见表系统设计第 1.2 节）
  ├── Redis：SKU 缓存、类目树缓存、搜索降级锁
  ├── RocketMQ (Producer)：写 Outbox → 投递 mall:search:sync
  ├── RocketMQ (Consumer)：消费 mall:order:cancelled → 释放库存；消费 mall:order:paid → 更新热度排行
  ├── mall-api (Feign)：RemoteProductService 供 mall-order 调用
  │   方法：batchGetSku / reserveStock / releaseStock / restock
  └── mall-search (Feign Caller)：调 RemoteSearchService.syncProduct 直推索引
```

> **关键约束**：商品服务负责库存原子操作，不负责订单金额和优惠计算。搜索索引同步采用双通道（Feign 直推 + Outbox 异步兜底）。提供搜索降级时的 DB 兜底查询。

---

## 2 包结构与接口映射

### 2.1 包结构

```
server/mall/mall-product/
└── src/main/java/com/mall/product/
    ├── MallProductApplication.java          # Spring Boot 启动类
    ├── controller/
    │   ├── inner/
    │   │   └── RemoteProductInnerController.java # /inner/product/** 内部 Feign 端点
    │   ├── CategoryController.java           # /api/product/categories/**
    │   ├── BrandController.java              # /api/product/brands
    │   ├── SpuController.java                # /api/product/spus/**
    │   ├── SkuController.java                # /api/product/skus/**
    │   └── SearchFallbackController.java     # /api/product/search/fallback
    ├── DTO/
    │   ├── request/                         → CreateCategoryReq, CreateBrandReq, CreateSpuReq,
    │   │                                       UpdateSpuStatusReq, AdjustStockReq ...
    │   └── response/                        → CategoryResp, BrandResp, SpuResp, SkuResp, StockResp
    ├── DO/
    │   ├── MallCategoryDO.java                # 对应 mall_product_category 表
    │   ├── MallBrandDO.java                   # 对应 mall_product_brand 表
    │   ├── MallProductSpuDO.java              # 对应 mall_product_spu 表
    │   ├── MallProductSkuDO.java              # 对应 mall_product_sku 表
    │   └── MallSkuStockDO.java                # 对应 mall_product_sku_stock 表
    ├── service/
    │   ├── CategoryService.java
    │   ├── BrandService.java
    │   ├── SpuService.java
    │   ├── SkuService.java
    │   ├── StockService.java
    │   ├── SkuCacheService.java
    │   ├── CategoryCacheService.java
    │   ├── HotProductService.java
    │   └── impl/
    │       ├── CategoryServiceImpl.java
    │       ├── BrandServiceImpl.java
    │       ├── SpuServiceImpl.java
    │       ├── SkuServiceImpl.java
    │       ├── StockServiceImpl.java
    │       ├── SkuCacheServiceImpl.java
    │       ├── CategoryCacheServiceImpl.java
    │       └── HotProductServiceImpl.java
    ├── mapper/
    │   ├── MallCategoryMapper.java
    │   ├── MallBrandMapper.java
    │   ├── MallProductSpuMapper.java
    │   ├── MallProductSkuMapper.java
    │   └── MallSkuStockMapper.java
    ├── infrastructure/
    │   ├── mq/
    │   │   ├── SearchSyncProducer.java       # Outbox 生产 mall:search:sync
    │   │   ├── OrderCancelledConsumer.java   # 消费 mall:order:cancelled 释放库存
    │   │   └── OrderPaidConsumer.java        # 消费 mall:order:paid 更新热度排行
    │   └── feign/
    │       └── RemoteSearchAdapter.java      # 调 mall-search 实时同步索引
    ├── convert/
    │   ├── request/                        # Request/VO → DO（入站）
    │   └── response/                       # DO → VO（出站）
    │       ├── CategoryConvert.java
    │       ├── BrandConvert.java
    │       ├── SpuConvert.java
    │       ├── SkuConvert.java
    │       └── StockConvert.java
    └── VO/                                  # 视图对象，前端展示（与 DO 字段不同，需转换）
        ├── CategoryTreeVO.java              # 类目树（嵌套 children），与 DO 扁平行不同
        ├── BrandVO.java                     # 品牌展示
        ├── SpuVO.java                       # C 端 SPU 列表（隐藏 costPrice/verifyStatus）
        ├── SpuDetailVO.java                 # C 端 SPU 详情（含 SKU 列表嵌套）
        ├── SpuAdminVO.java                  # 管理端 SPU 完整信息（含 costPrice/verifyStatus）
        ├── SkuVO.java                       # SKU 展示（attrsJson 展开为列表）
        └── StockVO.java                     # 库存四段数字展示
```

### 2.2 接口 → Controller 映射

| # | 方法 | 路径                                     | Controller               | 方法名                 | 需登录 | 权限码 |
| - | ---- | ---------------------------------------- | ------------------------ | ---------------------- | :----: | ------ |
| 1 | GET  | `/api/product/categories`              | CategoryApiController    | `tree()`             |   否   | —     |
| 2 | GET  | `/api/product/categories/{categoryId}` | CategoryApiController    | `detail(categoryId)` |   否   | —     |
| 3 | GET  | `/api/product/brands`                  | BrandApiController       | `list(params)`       |   否   | —     |
| 4 | GET  | `/api/product/spus`                    | SpuApiController         | `list(params)`       |   否   | —     |
| 5 | GET  | `/api/product/spus/{spuId}`            | SpuApiController         | `detail(spuId)`      |   否   | —     |
| 6 | GET  | `/api/product/skus/{skuId}`            | SkuApiController         | `detail(skuId)`      |   否   | —     |
| 7 | GET  | `/api/product/search/fallback`         | SearchFallbackController | `search(params)`     |   否   | —     |
| 8 | GET  | `/api/product/spus/hot`                | SpuApiController         | `hotList(limit)`     |   否   | —     |

管理端接口由若依代码生成器自动生成（类目/品牌/SPU/SKU/库存 CRUD + 上下架），权限码无需手动维护。

### 2.3 Lombok 使用约定

本模块类层级与 Lombok 注解映射（与 mall-user 一致）：

| 类层级                                      | 注解                                      | 说明                            |
| ------------------------------------------- | ----------------------------------------- | ------------------------------- |
| `DO/`                                     | `@Data` + `@Builder`                  | 数据库实体（MallCategoryDO 等） |
| `dto/request/`                            | `@Data` + `@NoArgsConstructor`        | Jackson 反序列化                |
| `dto/response/`                           | `@Data`                                 | 响应 DTO                        |
| `vo/`                                     | `@Data`                                 | 视图对象（CategoryTreeVO 等）   |
| `service/impl/`                           | `@Slf4j` + `@RequiredArgsConstructor` | 构造器注入                      |
| `controller/`                             | `@Slf4j` + `@RequiredArgsConstructor` | —                              |
| `convert/request/`, `convert/response/` | 无 Lombok                                 | 纯静态转换方法                  |

详见 `AGENTS.md` §Lombok 使用规范。

---

### 2.4 Mapper 层编码规范

本模块 Mapper 层遵循项目统一规范：

| 操作             | 用什么                                               | 说明                                                       |
| ---------------- | ---------------------------------------------------- | ---------------------------------------------------------- |
| **SELECT** | `LambdaQueryWrapper` + `BaseMapper.selectPage()` | 类型安全，字段变更 IDE 即时感知                            |
| **UPDATE** | `@Update` 注解                                     | 简洁直读，算术运算 `SET field = field + #{value}` 必须用 |
| **INSERT** | `BaseMapper.insert()`                              | MyBatis-Plus 内置                                          |
| **DELETE** | `BaseMapper.update()` 软删除                       | 统一逻辑删除 `set is_deleted = 1`                        |

---

## 3 核心类设计

### 3.1 CategoryServiceImpl

位于 `service/impl/CategoryServiceImpl.java`，类目树管理。

- `tree()`：查 `is_visible=1` 的全部类目，内存中构建三级树。Redis 缓存 30 分钟（`mall:product:category:tree`），管理端增删改后主动刷新缓存
- `create(req)`：校验 parentId 存在性。自动计算 `level=parent.level+1`，生成 `path=parent.path/id`。同级 name 不重复
- `update(id, req)`：允许改 name/icon/sortOrder/isVisible，不允许改 parentId
- `delete(id)`：子类目非空或有关联 SPU → 拒绝 `A0503`

### 3.2 SpuServiceImpl

位于 `service/impl/SpuServiceImpl.java`，SPU 管理 + 上下架。

- `listC(params)`：`WHERE publish_status=1 AND is_deleted=0`，支持按类目/品牌/关键词/排序分页。C 端不返回 `cost_price` 和 `verify_status`
- `detailC(spuId)`：含 SKU 列表（`publish_status` 过滤下架 SKU）。C 端不返回 `cost_price`
- `create(req)`：同一事务内创建 SPU + 批量创建 SKU + 初始化库存。SKU 至少 1 条。`sku_code` 雪花 ID + SKU 前缀。创建后自动更新 `price_min`/`price_max`
- `updateStatus(spuId, publishStatus)`：上架时校验 `verify_status=1`（审核通过）+ 至少 1 个 SKU 有库存。**上下架后触发搜索同步**（见第 7 节）

**价格同步**：SKU 修改价格后，异步更新 SPU 的 `price_min`/`price_max`（Redis 延迟队列 1s 去重合并）。

### 3.3 StockServiceImpl

位于 `service/impl/StockServiceImpl.java`，库存核心操作。

**四段库存模型**：

| 字段                | 含义     | 操作                        |
| ------------------- | -------- | --------------------------- |
| `available_stock` | 可下单量 | 下单 -N / 取消 +N           |
| `locked_stock`    | 下单锁定 | 下单 +N / 支付 -N / 取消 -N |
| `sold_stock`      | 已售     | 支付 +N                     |
| `frozen_stock`    | 售后冻结 | 售后 +N / 完成或拒绝 -N     |

**约束**：`available + locked + sold + frozen = total_stock`（恒等式）

**reserveStock(skuId, qty, orderNo)**：下单锁库存（Feign 由 mall-order 调用）

- `UPDATE mall_product_sku_stock SET available_stock=available_stock-#{qty}, locked_stock=locked_stock+#{qty}, version=version+1 WHERE sku_id=? AND version=? AND available_stock>=#{qty}`
- 乐观锁防超卖：`available_stock>=qty` + `version` 双重保护
- 影响 0 行 → 库存不足 `A0521`

**releaseStock(orderNo)**：取消订单释放库存（消费 `mall:order:cancelled`）

- 根据 `orderNo` 查订单项，逐 SKU 回退：`UPDATE ... SET available_stock=available_stock+#{qty}, locked_stock=locked_stock-#{qty} WHERE sku_id=? AND version=?`
- 幂等去重：`orderNo + skuId` Redis key，防重复释放

**restock(skuId, qty)**：售后退货回补（Feign 由 mall-order 调用）

- 仅退货退款回补：`UPDATE ... SET available_stock=available_stock+#{qty}, frozen_stock=frozen_stock-#{qty}`

**adjust(skuId, delta)**：管理端调整库存

- 审计日志记录操作人 + 调整原因
- delta 可为正（补货）或负（盘点扣减），修改 `total_stock` 和 `available_stock`

### 3.4 SkuCacheService

位于 `service/SkuCacheService.java`，SKU 信息 Redis 缓存。

**Key**：`mall:product:sku:{skuId}`

| 缓存内容                                       |  TTL  | 策略               |
| ---------------------------------------------- | :---: | ------------------ |
| 完整 SkuDTO（price/stock/name/image/isOnSale） | 10min | 读时回种，写时删除 |

**读路径**：

1. 先查 Redis → 命中返回
2. 未命中 → 查 MySQL → 回种 Redis（TTL 10min）
3. Redis 不可用 → 查 MySQL 兜底

**写路径（SKU 变更时）**：

1. 更新 MySQL
2. 删除 Redis 缓存 `DEL mall:product:sku:{skuId}`
3. 延迟 500ms 再删一次（防读写并发 - Cache Aside 双删）

**批量查询 batchGetSku(skuIds)**（Feign 接口，mall-order 购物车/下单用）：

1. 批量 Redis `MGET` → 命中的直接返回，未命中的列表
2. 未命中列表查 MySQL → 批量回种 Redis → 合并返回

### 3.5 CategoryCacheService

位于 `service/CategoryCacheService.java`，类目树 Redis 缓存。

- Key：`mall:product:category:tree`，TTL 30min
- 应用启动时预热 + 类目变更时刷新
- 不缓存单类目（类目树一次返回完整树即可）

---

## 4 库存并发控制

### 4.1 乐观锁防超卖

```sql
-- 下单锁库存
UPDATE mall_product_sku_stock
SET available_stock = available_stock - #{qty},
    locked_stock = locked_stock + #{qty},
    version = version + 1
WHERE sku_id = #{skuId}
  AND version = #{version}
  AND available_stock >= #{qty}
```

| 防护层                 | 机制                                    |
| ---------------------- | --------------------------------------- |
| version                | 乐观锁，防并发覆盖                      |
| available_stock >= qty | 防库存扣到负数                          |
| 失败处理               | 影响 0 行 → 返回 A0521，上层重试或拒绝 |

### 4.2 库存补偿

| 场景                 | 补偿                                                   |
| -------------------- | ------------------------------------------------------ |
| 下单后 30 分钟未支付 | 消费 `mall:order:cancelled` → 释放库存              |
| 订单取消             | 同上                                                   |
| Outbox 投递失败      | Outbox 重试 3 次，最终投递到死信 → 人工或补偿任务扫描 |
| 补偿重复执行         | `orderNo + skuId` Redis 幂等去重（24h TTL）          |

### 4.3 库存流水

所有库存变更写入 `mall_stock_log` 表（预留）：

| 字段         | 说明                                  |
| ------------ | ------------------------------------- |
| sku_id       | SKU ID                                |
| order_no     | 关联订单号                            |
| change_type  | 类型：LOCK/UNLOCK/SOLD/RESTOCK/ADJUST |
| change_qty   | 变动数量                              |
| before/after | 变动前后可用库存                      |
| create_time  | 变更时间                              |

> 当前阶段库存流水表为预留设计，初期可只记订单操作日志，防止表膨胀。

---

## 5 搜索索引同步

### 5.1 双通道策略

商品变更（新增/修改/上下架/改价）后，通过双通道确保 ES 索引最终一致：

| 通道 | 方式                                                                  | 特点                     |
| ---- | --------------------------------------------------------------------- | ------------------------ |
| 实时 | Feign 直调 `RemoteSearchService.syncProduct(productIndex)`          | 同步调用，即时生效       |
| 异步 | 写 Outbox `mall:search:sync`，定时投递 RocketMQ → mall-search 消费 | 实时通道失败时降级兜底   |
| 补偿 | ruoyi-job 定时扫描 Outbox 未投递记录                                  | 兜底通道失败时的最后保障 |

### 5.1.1 全量重建数据供给

供 mall-search 全量重建时调用，通过 `RemoteProductInnerController` 暴露 `/inner/product/spus/all-for-search` 端点：

| 方法 | 返回 | 说明 |
|------|------|------|
| `pageForSearchRebuild(page, size)` | `PageResult<SpuSearchDTO>` | 分页查询全量 SPU，JOIN 类目名、品牌名，聚合 SKU 规格文本 |

`SpuSearchDTO`（定义于 `mall-common`）比 `SpuDTO` 多包含 `subTitle`、`categoryName`、`brandName`、`spuSpecs`、`createTime`、`updateTime`、`tags`，满足 ES `ProductIndex` 全量重建所需字段。

### 5.2 触发时机

| 操作      | 同步内容                              |
| --------- | ------------------------------------- |
| 上架      | `UPSERT` 完整 ProductIndex          |
| 下架      | `DELETE` 或标记 `publishStatus=0` |
| 改价      | `UPDATE` 价格字段                   |
| 改名/改图 | `UPDATE` 对应字段                   |
| 新增 SPU  | `UPSERT`                            |
| 删除 SPU  | `DELETE`                            |

### 5.3 实现

```
SpuServiceImpl.updateStatus(spuId, publishStatus):
  ① 更新 publish_status
  ② 同一事务写 Outbox mall:search:sync
  ③ 事务提交后 → 异步调 RemoteSearchAdapter.syncProduct(productIndex)
     ├─ 成功 → 更新 Outbox status=SENT
     └─ 失败 → 不阻塞，Outbox 定时投递兜底
```

> 关键：实时通道失败不报错返回给管理端用户，不阻塞商品操作。索引最终一致性由 Outbox 保证。

---

## 6 搜索降级兜底

ES 不可用时，mall-search 自动降级到 mall-product 的 DB 查询：

`GET /api/product/search/fallback`：`SELECT * FROM mall_product_spu WHERE publish_status=1 AND spu_name LIKE CONCAT('%',?,'%') LIMIT ?,?`

| 约束             | 说明                        |
| ---------------- | --------------------------- |
| 只返回已上架商品 | `publish_status=1`        |
| 分页限制         | 最大 100 条/页              |
| LIKE 防慢查询    | 关键词长度 ≥ 2 且 ≤ 50    |
| 超时             | 3s 超时返回空               |
| 排序             | 仅支持 `sales_count DESC` |

---

## 7 RocketMQ 事件

### 7.1 发布的事件

| Topic                | Payload 字段                                             | 发布时机       |
| -------------------- | -------------------------------------------------------- | -------------- |
| `mall:search:sync` | `spuId`、`operation`（UPSERT/DELETE）、`timestamp` | 商品信息变更后 |

### 7.2 消费的事件

| Topic                    | 消费者类                   | 处理流程                                                                                                           |
| ------------------------ | -------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| `mall:order:cancelled` | `OrderCancelledConsumer` | ①幂等去重 ②查订单项 SKU 列表 ③逐 SKU 释放库存 `releaseStock(skuId, qty)` ④幂等 key：`orderNo + skuId`      |
| `mall:order:paid`      | `OrderPaidConsumer`      | ①幂等去重 ②查订单项 SKU 列表 ③逐 SKU → 查所属 SPU ④`HotProductService.hotRank(spuId, qty)` 更新 ZSet 热度分 |

### 7.3 重试策略

| 重试次数 | 间隔 | next_retry_time           |
| :-------: | ---- | ------------------------- |
|  第 1 次  | 10s  | NOW() + 10s               |
|  第 2 次  | 30s  | NOW() + 30s               |
|  第 3 次  | 60s  | NOW() + 60s               |
| 超过 3 次 | —   | status = FAILED，死信处理 |

---

## 8 热点数据设计

> 热点数据设计遵循 `03_06_系统详细设计-缓存与一致性设计.md` §6.1.3 中定义的 Caffeine `hotProduct` 缓存和 Redis ZSet `mall:product:hot:rank` 排行机制。

### 8.1 设计动机

商城首页、推荐位、榜单页需要展示**热门商品列表**，这些商品访问量大（高并发读），且排名需要定期更新。如果每次都查 MySQL `ORDER BY sales_count DESC LIMIT N`，数据库压力大且无法引入实时行为（UV 浏览）权重。因此采用 Caffeine + Redis 两级缓存 + 定时计算排行的架构。

### 8.2 技术架构

```
┌─────────────────────────────────────────────────┐
│  C 端请求 GET /api/product/spus/hot?limit=20     │
└──────────────┬──────────────────────────────────┘
               ↓
┌─────────────────────────────────────────────────┐
│ L1: Caffeine hotProduct (500容量/5min访问过期)    │
│     key = spuId → 返回 SpuHotVO                   │
└──────────────┬──────────────────────────────────┘
               ↓ (未命中)
┌─────────────────────────────────────────────────┐
│ L2: Redis ZSet mall:product:hot:rank              │
│     member=spuId, score=热度分 → ZREVRANGE 取TopN │
└──────────────┬──────────────────────────────────┘
               ↓ (未命中 或 冷启动)
┌─────────────────────────────────────────────────┐
│ MySQL: ORDER BY sales_count DESC LIMIT N          │
└─────────────────────────────────────────────────┘
```

### 8.3 HotProductService

位于 `service/HotProductService.java`，热点商品查询 + 排名管理。

**hotList(limit)**：返回热点商品列表

1. 从 Redis ZSet `mall:product:hot:rank` `ZREVRANGE 0 {limit-1}` 取排名 ID 列表
2. 批量从 Caffeine `hotProduct` 读取 → 命中直接返回
3. 未命中列表 → 批量查 MySQL → 回种 Caffeine → 返回
4. 若 Redis ZSet 为空（冷启动）→ 降级查 MySQL `ORDER BY sales_count DESC LIMIT {limit}`

**incrUv(spuId, userId)**：商品详情页访问时记录 UV

- 每次商品详情页加载时调用，异步执行不阻塞响应
- 使用 Redis HyperLogLog 按天分片：`PFADD mall:product:uv:{spuId}:{yyyyMMdd} {userId}` 去重统计
- 日键设 TTL = uvWindowDays + 2，到期自动过期，无需手动清理

**hotRank(skuId, quantity)**：下单成功后更新热度（消费 `mall:order:paid`）

- `ZINCRBY mall:product:hot:rank {quantity * 10} {spuId}`（销量权重）

### 8.4 热度计算模型

热度分 = 销量 × 10 × 销量权重 + 滑动窗口 UV × 10 × UV 权重

| 维度   | 数据来源                                                        |         刷新频率         | Redis 操作         |
| ------ | --------------------------------------------------------------- | :----------------------: | ------------------ |
| 销量分 | `mall_product_spu.sales_count`                                |    支付成功时实时更新    | —                 |
| UV 分  | Redis HyperLogLog 日分片 `mall:product:uv:{spuId}:{yyyyMMdd}` | 商品详情页访问时异步写入 | `PFADD` 去重统计 |

> **滑动窗口 UV**：每次定时任务读取最近 `uvWindowDays`（默认 7）天的日分片键，使用 `PFCOUNT key1 ... keyN` 多键联合基数估算，跨天用户自动去重。日键设 TTL 自动过期，避免累计 UV 导致排名头部固化。

**定时任务 refreshHotRank()（每 10 分钟）**：

1. Redis 分布式锁 `mall:job:lock:hot:rank`，SETNX TTL 600s
2. MySQL 查已上架 SPU，按销量降序取前 `rankMaxSize`（默认 200）条
3. 遍历每条 SPU，取最近 N 天 UV 日分片做多键 `PFCOUNT`：
   ```
   PFCOUNT mall:product:uv:{spuId}:{D-6} ... mall:product:uv:{spuId}:{D}
   ```
4. 计算综合热度分 = `salesCount * 10 * salesWeight + uv * 10 * uvWeight`
5. 清空旧 ZSet → 全量写入新 ZSet `mall:product:hot:rank`
6. 释放锁

> UV 日分片键（`mall:product:uv:{spuId}:{yyyyMMdd}`）设 TTL = uvWindowDays + 2 天，到期自动过期，无需手动清理。

### 8.5 缓存击穿防护

热点商品的详情页（`GET /api/product/spus/{spuId}`）也受益于 Caffeine L1 缓存。当 Caffeine 过期且 Redis 未命中时，使用分布式锁防止大量请求打到 DB：

```java
// SpuServiceImpl.detailC(spuId)
String lockKey = "mall:product:hot:rebuild:" + spuId;
Boolean locked = redisTemplate.opsForValue()
    .setIfAbsent(lockKey, "1", Duration.ofSeconds(10));
if (Boolean.TRUE.equals(locked)) {
    try {
        // 查 DB → 回种 Caffeine + Redis
    } finally {
        redisTemplate.delete(lockKey);
    }
} else {
    // 自旋等待 100ms 重试 Caffeine
}
```

### 8.6 热点数据生命周期

```
商品上架 ──→ 纳入热点计算池（ZSet 初始 score=0）
   │
支付成功 ──→ 销量递增（mall_product_spu.sales_count）
   │
详情页访问 ──→ PFADD 写入当日 UV 日分片键
   │
定时任务 ──→ 全量重建 ZSet（PFCOUNT 多键联合去重最近 N 天 UV）
   │
商品下架 ──→ ZREM 从 ZSet 移除 → 删除 Caffeine
   │
UV 日分片 ──→ TTL 自动过期（窗口天数 + 2 天）
```

### 8.7 C 端接口

#### GET /api/product/spus/hot — 热门商品列表

| 参数  | 类型    | 必填 | 默认值 | 说明              |
| ----- | ------- | :--: | :----: | ----------------- |
| limit | Integer |  否  |   20   | 返回条数，最大 50 |

**成功响应**：

```json
{
  "code": "00000",
  "data": [
    {
      "spuId": 1001,
      "spuName": "iPhone 15 Pro Max",
      "mainImage": "https://cdn.example.com/img/1001.jpg",
      "minPrice": 899900,
      "salesCount": 15680,
      "hotScore": 98200
    }
  ]
}
```

**错误码**：A0401（limit > 50）

---

## 9 Nacos 配置

### 9.1 DataId: `mall-product-dev.yml`

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
             dataId: sentinel-mall-product
             groupId: DEFAULT_GROUP
             data-type: json
             rule-type: flow
    data:
    redis:
      host: localhost
      port: 6379
      password:
  datasource:
    dynamic:
      primary: master
      datasource:
        master:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://localhost:3306/mall?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8
          username: root
          password: 138992

mybatis-plus:
  typeAliasesPackage: com.mall.product.**.DO
  mapperLocations: classpath:mapper/**/*.xml

springdoc:
  gatewayUrl: http://localhost:8080/${spring.application.name}
  api-docs:
    enabled: true
  info:
    title: '商品模块接口文档'
    description: '商品模块接口描述'
    contact:
      name: RuoYi
      url: https://ruoyi.vip

mall:
  product:
    sku:
      cache-ttl: 600
    category:
      cache-ttl: 1800
    search:
      sync-batch-size: 100
      fallback:
        timeout: 3000
        max-size: 100
    stock:
      compensate:
        ttl: 86400
    hot:
      rank-max-size: 200
      hot-list-limit: 50
      sales-weight: 0.6
      uv-weight: 0.4
      uv-window-days: 7
```

> 以上配置通过 Nacos 下发，支持 `@RefreshScope` 运行时动态刷新。
> 配置项通过 `MallProductConfigProperties`（`@ConfigurationProperties(prefix = "mall.product")` + `@RefreshScope`）注入，各 Service/Controller 通过构造注入获取，禁止使用 `@Value`。

### 9.2 本地配置文件 `bootstrap.yml`

```yaml
# mall-product 商品服务
server:
  port: 9303

spring:
  application:
    name: mall-product
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

### 9.3 配置项说明

| 配置项                                    | 默认值             | 单位 | 说明                         |
| ----------------------------------------- | ------------------ | :--: | ---------------------------- |
| `mall.product.sku.cache-ttl`            | 600                |  秒  | SKU 缓存 TTL（10min）        |
| `mall.product.category.cache-ttl`       | 1800               |  秒  | 类目树缓存 TTL（30min）      |
| `mall.product.search.sync-batch-size`   | 100                |  条  | 搜索同步 Outbox 单次投递上限 |
| `mall.product.search.fallback.timeout`  | 3000               |  ms  | 降级搜索超时                 |
| `mall.product.search.fallback.max-size` | 100                |  条  | 降级搜索单页上限             |
| `mall.product.stock.compensate.ttl`     | 86400              |  秒  | 库存释放幂等键 TTL（24h）    |
| `mall.product.hot.rank.max-size`        | 200                |  条  | 热点排名 ZSet 最大容量       |
| `mall.product.hot.rank.refresh-cron`    | `0 */10 * * * ?` |  —  | 热点排名定时刷新 cron        |
| `mall.product.hot.sales-weight`         | 0.6                |  —  | 热度计算：销量权重           |
| `mall.product.hot.uv-weight`            | 0.4                |  —  | 热度计算：UV 权重            |
| `mall.product.hot.uv-window-days`       | 7                  |  天  | UV 滑动窗口天数（最近 N 天） |

---

## 10 错误码汇总

| 错误码 | HTTP | userTip              | 说明                                         |
| ------ | :--: | -------------------- | -------------------------------------------- |
| A0520  | 400 | 商品已下架           | 该商品当前不可购买                           |
| A0521  | 400 | 商品库存不足         | 下单时 available_stock 不够                  |
| A0803  | 400 | 热门商品查询参数无效 | limit 超过最大值 50                          |

> 公共错误码（A0301/A0320/A0401/A0501/A0503/B0001/C0110/C0120 等）见 `06_mall-common公共模块设计.md` §6.1。

---

## 11 服务间 Feign 安全（接收方）

mall-product 供 mall-order 通过 Feign 内部调用（库存查询/锁定/释放等 `/inner/` 接口），由 `mall-api` 共享的 `InnerSignatureFilter`（`com.mall.api.infrastructure.security`）验签。

- `InnerSignatureFilter` 通过 `@Component` 自动注册，拦截 `/inner/**`，无需额外配置
- 白名单：`/actuator/health`
- 错误码 `A0311`/`A0312`

签名算法详见 [03_系统详细设计.md §7.3](file:///e:/Workspace/AI/JH-Mall/docs/design/03_系统详细设计.md#L4346)。
