# JH-Store mall-product 模块详细设计

> 基于系统详细设计 `03_系统详细设计.md` 展开。数据表 DDL 在系统设计 1.2 节统一维护，此处只引用表名和字段。

---

## 1 模块概述

### 1.1 子领域

| 子领域 | 实体 | 说明 |
|--------|------|------|
| 类目管理 | `mall_product_category` | 三级树形类目（A→B→C），支持 path 快速遍历 |
| 品牌管理 | `mall_product_brand` | 品牌 CRUD，关联类目 |
| SPU 管理 | `mall_product_spu` | 商品主体信息，含上下架和审核状态 |
| SKU 管理 | `mall_product_sku` | 销售规格，含售价/市场价/成本价/重量 |
| 库存管理 | `mall_product_sku_stock` | 四段库存（可用/锁定/已售/冻结），乐观锁防超卖 |
| 搜索同步 | Outbox | 商品变更后实时+异步双通道同步到 ES |
| RocketMQ 事件 | Outbox | 生产 `mall:search:sync`，消费 `mall:order:cancelled` |

### 1.2 依赖关系

```
mall-product (9302端口)
  ├── MySQL：自有表（见表系统设计第 1.2 节）
  ├── Redis：SKU 缓存、类目树缓存、搜索降级锁
  ├── RocketMQ (Producer)：写 Outbox → 投递 mall:search:sync
  ├── RocketMQ (Consumer)：消费 mall:order:cancelled → 释放库存
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
└── src/main/java/com/jhstore/mall/product/
    ├── MallProductApplication.java          # Spring Boot 启动类
    ├── controller/
    │   ├── admin/
    │   │   ├── CategoryAdminController.java # /admin/mall/product/categories/**
    │   │   ├── BrandAdminController.java    # /admin/mall/product/brands/**
    │   │   ├── SpuAdminController.java      # /admin/mall/product/spus/**
    │   │   ├── SkuAdminController.java      # /admin/mall/product/skus/**
    │   │   └── StockAdminController.java    # /admin/mall/product/stock/**
    │   └── api/
    │       ├── CategoryApiController.java   # /api/product/categories/**
    │       ├── BrandApiController.java      # /api/product/brands
    │       ├── SpuApiController.java        # /api/product/spus/**
    │       ├── SkuApiController.java        # /api/product/skus/**
    │       └── SearchFallbackController.java # /api/product/search/fallback
    ├── dto/
    │   ├── request/                         → CreateCategoryReq, CreateBrandReq, CreateSpuReq,
    │   │                                       UpdateSpuStatusReq, AdjustStockReq ...
    │   └── response/                        → CategoryResp, BrandResp, SpuResp, SkuResp, StockResp
    ├── domain/
    │   ├── MallCategoryDO.java              # 对应 mall_product_category 表
    │   ├── MallBrandDO.java                 # 对应 mall_product_brand 表
    │   ├── MallProductSpuDO.java            # 对应 mall_product_spu 表
    │   ├── MallProductSkuDO.java            # 对应 mall_product_sku 表
    │   └── MallSkuStockDO.java              # 对应 mall_product_sku_stock 表
    ├── service/
    │   ├── category/
    │   │   ├── CategoryService.java
    │   │   └── impl/CategoryServiceImpl.java # 类目树 CRUD
    │   ├── brand/
    │   │   ├── BrandService.java
    │   │   └── impl/BrandServiceImpl.java    # 品牌 CRUD
    │   ├── spu/
    │   │   ├── SpuService.java
    │   │   └── impl/SpuServiceImpl.java      # SPU CRUD + 上下架
    │   ├── sku/
    │   │   ├── SkuService.java
    │   │   └── impl/SkuServiceImpl.java      # SKU CRUD + 批量查询
    │   ├── stock/
    │   │   ├── StockService.java
    │   │   └── impl/StockServiceImpl.java    # 库存扣减/锁定/释放/回补
    │   └── cache/
    │       ├── SkuCacheService.java          # SKU Redis 缓存
    │       └── CategoryCacheService.java     # 类目树 Redis 缓存
    ├── mapper/
    │   ├── MallCategoryMapper.java
    │   ├── MallBrandMapper.java
    │   ├── MallProductSpuMapper.java
    │   ├── MallProductSkuMapper.java
    │   └── MallSkuStockMapper.java
    ├── infrastructure/
    │   ├── mq/
    │   │   ├── SearchSyncProducer.java       # Outbox 生产 mall:search:sync
    │   │   └── OrderCancelledConsumer.java   # 消费 mall:order:cancelled 释放库存
    │   └── feign/
    │       └── RemoteSearchAdapter.java      # 调 mall-search 实时同步索引
    ├── convert/                             # 纯转换器（Entity↔DTO↔VO 字段映射）
    │   ├── CategoryConvert.java            # MallCategoryDO → CategoryTreeVO
    │   ├── BrandConvert.java               # MallBrandDO → BrandVO
    │   ├── SpuConvert.java                 # MallProductSpuDO → SpuVO / SpuDetailVO / SpuAdminVO
    │   ├── SkuConvert.java                 # MallProductSkuDO → SkuVO (attrsJson 展开)
    │   └── StockConvert.java               # MallSkuStockDO → StockVO
    └── vo/                                  # 视图对象，前端展示（与 DO 字段不同，需转换）
        ├── CategoryTreeVO.java              # 类目树（嵌套 children），与 DO 扁平行不同
        ├── BrandVO.java                     # 品牌展示
        ├── SpuVO.java                       # C 端 SPU 列表（隐藏 costPrice/verifyStatus）
        ├── SpuDetailVO.java                 # C 端 SPU 详情（含 SKU 列表嵌套）
        ├── SpuAdminVO.java                  # 管理端 SPU 完整信息（含 costPrice/verifyStatus）
        ├── SkuVO.java                       # SKU 展示（attrsJson 展开为列表）
        └── StockVO.java                     # 库存四段数字展示
```

### 2.2 接口 → Controller 映射

| # | 方法 | 路径 | Controller | 方法名 | 需登录 | 权限码 |
|---|------|------|-----------|--------|:---:|--------|
| 1 | GET | `/api/product/categories` | CategoryApiController | `tree()` | 否 | — |
| 2 | GET | `/api/product/categories/{categoryId}` | CategoryApiController | `detail(categoryId)` | 否 | — |
| 3 | GET | `/api/product/brands` | BrandApiController | `list(params)` | 否 | — |
| 4 | GET | `/api/product/spus` | SpuApiController | `list(params)` | 否 | — |
| 5 | GET | `/api/product/spus/{spuId}` | SpuApiController | `detail(spuId)` | 否 | — |
| 6 | GET | `/api/product/skus/{skuId}` | SkuApiController | `detail(skuId)` | 否 | — |
| 7 | GET | `/api/product/search/fallback` | SearchFallbackController | `search(params)` | 否 | — |
| 8 | POST | `/admin/mall/product/categories` | CategoryAdminController | `create(req)` | 管理端 | `mall:product:create` |
| 9 | PUT | `/admin/mall/product/categories/{categoryId}` | CategoryAdminController | `update(categoryId, req)` | 管理端 | `mall:product:update` |
| 10 | DELETE | `/admin/mall/product/categories/{categoryId}` | CategoryAdminController | `delete(categoryId)` | 管理端 | `mall:product:delete` |
| 11 | POST | `/admin/mall/product/brands` | BrandAdminController | `create(req)` | 管理端 | `mall:product:create` |
| 12 | PUT | `/admin/mall/product/brands/{brandId}` | BrandAdminController | `update(brandId, req)` | 管理端 | `mall:product:update` |
| 13 | DELETE | `/admin/mall/product/brands/{brandId}` | BrandAdminController | `delete(brandId)` | 管理端 | `mall:product:delete` |
| 14 | GET | `/admin/mall/product/spus` | SpuAdminController | `list(params)` | 管理端 | `mall:product:list` |
| 15 | GET | `/admin/mall/product/spus/{spuId}` | SpuAdminController | `detail(spuId)` | 管理端 | `mall:product:detail` |
| 16 | POST | `/admin/mall/product/spus` | SpuAdminController | `create(req)` | 管理端 | `mall:product:create` |
| 17 | PUT | `/admin/mall/product/spus/{spuId}` | SpuAdminController | `update(spuId, req)` | 管理端 | `mall:product:update` |
| 18 | PUT | `/admin/mall/product/spus/{spuId}/status` | SpuAdminController | `updateStatus(spuId, req)` | 管理端 | `mall:product:update` |
| 19 | GET | `/admin/mall/product/skus` | SkuAdminController | `listBySpu(spuId)` | 管理端 | `mall:product:list` |
| 20 | PUT | `/admin/mall/product/skus/{skuId}` | SkuAdminController | `update(skuId, req)` | 管理端 | `mall:product:update` |
| 21 | GET | `/admin/mall/product/stock/{skuId}` | StockAdminController | `query(skuId)` | 管理端 | `mall:product:list` |
| 22 | PUT | `/admin/mall/product/stock/{skuId}` | StockAdminController | `adjust(skuId, req)` | 管理端 | `mall:product:update` |

---

## 3 核心类设计

### 3.1 CategoryServiceImpl

位于 `service/category/impl/CategoryServiceImpl.java`，类目树管理。

- `tree()`：查 `is_visible=1` 的全部类目，内存中构建三级树。Redis 缓存 30 分钟（`mall:product:category:tree`），管理端增删改后主动刷新缓存
- `create(req)`：校验 parentId 存在性。自动计算 `level=parent.level+1`，生成 `path=parent.path/id`。同级 name 不重复
- `update(id, req)`：允许改 name/icon/sortOrder/isVisible，不允许改 parentId
- `delete(id)`：子类目非空或有关联 SPU → 拒绝 `A0503`

### 3.2 SpuServiceImpl

位于 `service/spu/impl/SpuServiceImpl.java`，SPU 管理 + 上下架。

- `listC(params)`：`WHERE publish_status=1 AND is_deleted=0`，支持按类目/品牌/关键词/排序分页。C 端不返回 `cost_price` 和 `verify_status`
- `detailC(spuId)`：含 SKU 列表（`publish_status` 过滤下架 SKU）。C 端不返回 `cost_price`
- `create(req)`：同一事务内创建 SPU + 批量创建 SKU + 初始化库存。SKU 至少 1 条。`sku_code` 雪花 ID + SKU 前缀。创建后自动更新 `price_min`/`price_max`
- `updateStatus(spuId, publishStatus)`：上架时校验 `verify_status=1`（审核通过）+ 至少 1 个 SKU 有库存。**上下架后触发搜索同步**（见第 7 节）

**价格同步**：SKU 修改价格后，异步更新 SPU 的 `price_min`/`price_max`（Redis 延迟队列 1s 去重合并）。

### 3.3 StockServiceImpl

位于 `service/stock/impl/StockServiceImpl.java`，库存核心操作。

**四段库存模型**：

| 字段 | 含义 | 操作 |
|------|------|------|
| `available_stock` | 可下单量 | 下单 -N / 取消 +N |
| `locked_stock` | 下单锁定 | 下单 +N / 支付 -N / 取消 -N |
| `sold_stock` | 已售 | 支付 +N |
| `frozen_stock` | 售后冻结 | 售后 +N / 完成或拒绝 -N |

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

位于 `service/cache/SkuCacheService.java`，SKU 信息 Redis 缓存。

**Key**：`mall:product:sku:{skuId}`

| 缓存内容 | TTL | 策略 |
|---------|:---:|------|
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

位于 `service/cache/CategoryCacheService.java`，类目树 Redis 缓存。

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

| 防护层 | 机制 |
|--------|------|
| version | 乐观锁，防并发覆盖 |
| available_stock >= qty | 防库存扣到负数 |
| 失败处理 | 影响 0 行 → 返回 A0521，上层重试或拒绝 |

### 4.2 库存补偿

| 场景 | 补偿 |
|------|------|
| 下单后 30 分钟未支付 | 消费 `mall:order:cancelled` → 释放库存 |
| 订单取消 | 同上 |
| Outbox 投递失败 | Outbox 重试 3 次，最终投递到死信 → 人工或补偿任务扫描 |
| 补偿重复执行 | `orderNo + skuId` Redis 幂等去重（24h TTL） |

### 4.3 库存流水

所有库存变更写入 `mall_stock_log` 表（预留）：

| 字段 | 说明 |
|------|------|
| sku_id | SKU ID |
| order_no | 关联订单号 |
| change_type | 类型：LOCK/UNLOCK/SOLD/RESTOCK/ADJUST |
| change_qty | 变动数量 |
| before/after | 变动前后可用库存 |
| create_time | 变更时间 |

> 当前阶段库存流水表为预留设计，初期可只记订单操作日志，防止表膨胀。

---

## 5 搜索索引同步

### 5.1 双通道策略

商品变更（新增/修改/上下架/改价）后，通过双通道确保 ES 索引最终一致：

| 通道 | 方式 | 特点 |
|------|------|------|
| 实时 | Feign 直调 `RemoteSearchService.syncProduct(productIndex)` | 同步调用，即时生效 |
| 异步 | 写 Outbox `mall:search:sync`，定时投递 RocketMQ → mall-search 消费 | 实时通道失败时降级兜底 |
| 补偿 | ruoyi-job 定时扫描 Outbox 未投递记录 | 兜底通道失败时的最后保障 |

### 5.2 触发时机

| 操作 | 同步内容 |
|------|---------|
| 上架 | `UPSERT` 完整 ProductIndex |
| 下架 | `DELETE` 或标记 `publishStatus=0` |
| 改价 | `UPDATE` 价格字段 |
| 改名/改图 | `UPDATE` 对应字段 |
| 新增 SPU | `UPSERT` |
| 删除 SPU | `DELETE` |

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

| 约束 | 说明 |
|------|------|
| 只返回已上架商品 | `publish_status=1` |
| 分页限制 | 最大 100 条/页 |
| LIKE 防慢查询 | 关键词长度 ≥ 2 且 ≤ 50 |
| 超时 | 3s 超时返回空 |
| 排序 | 仅支持 `sales_count DESC` |

---

## 7 RocketMQ 事件

### 7.1 发布的事件

| Topic | Payload 字段 | 发布时机 |
|-------|-------------|---------|
| `mall:search:sync` | `spuId`、`operation`（UPSERT/DELETE）、`timestamp` | 商品信息变更后 |

### 7.2 消费的事件

| Topic | 消费者类 | 处理流程 |
|-------|---------|---------|
| `mall:order:cancelled` | `OrderCancelledConsumer` | ①幂等去重 ②查订单项 SKU 列表 ③逐 SKU 释放库存 `releaseStock(skuId, qty)` ④幂等 key：`orderNo + skuId` |

### 7.3 重试策略

| 重试次数 | 间隔 | next_retry_time |
|:-------:|------|----------------|
| 第 1 次 | 10s | NOW() + 10s |
| 第 2 次 | 30s | NOW() + 30s |
| 第 3 次 | 60s | NOW() + 60s |
| 超过 3 次 | — | status = FAILED，死信处理 |

---

## 8 Nacos 配置

### 8.1 DataId: `mall-product-dev.yml`

```yaml
spring:
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

mybatis:
  typeAliasesPackage: com.mall.product.**.domain
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
```

> 以上配置通过 Nacos 下发，支持 `@RefreshScope` 运行时动态刷新。

### 8.2 本地配置文件 `bootstrap.yml`

```yaml
# mall-product 商品服务
server:
  port: 9302

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

### 8.3 配置项说明

| 配置项 | 默认值 | 单位 | 说明 |
|--------|--------|:---:|------|
| `mall.product.sku.cache-ttl` | 600 | 秒 | SKU 缓存 TTL（10min） |
| `mall.product.category.cache-ttl` | 1800 | 秒 | 类目树缓存 TTL（30min） |
| `mall.product.search.sync-batch-size` | 100 | 条 | 搜索同步 Outbox 单次投递上限 |
| `mall.product.search.fallback.timeout` | 3000 | ms | 降级搜索超时 |
| `mall.product.search.fallback.max-size` | 100 | 条 | 降级搜索单页上限 |
| `mall.product.stock.compensate.ttl` | 86400 | 秒 | 库存释放幂等键 TTL（24h） |

---

## 9 错误码汇总

| 错误码 | HTTP | userTip | 说明 |
|--------|:----:|---------|------|
| 00000 | 200 | — | 成功 |
| A0301 | 401 | 请先登录 | 未登录 |
| A0320 | 403 | 无权限访问 | 权限不足 |
| A0401 | 400 | 请完整填写信息 | 必填参数为空 |
| A0501 | 404 | 资源不存在 | SPU/SKU/类目/品牌不存在 |
| A0503 | 400 | 资源不可操作 | 类目非空不可删除 / 品牌被引用 / SPU 状态异常 |
| A0520 | 400 | 商品已下架 | 该商品当前不可购买 |
| A0521 | 400 | 商品库存不足 | 下单时 available_stock 不够 |
| A0802 | 400 | 搜索结果超出限制 | 降级搜索超限 |
| B0001 | 500 | 系统繁忙，请稍后再试 | 未预期异常 |
| C0110 | 500 | 服务暂时不可用 | Redis 连接失败 |
| C0120 | 500 | 数据库异常 | MySQL 连接失败 |

> 全部错误码来自系统设计第二章。

---
