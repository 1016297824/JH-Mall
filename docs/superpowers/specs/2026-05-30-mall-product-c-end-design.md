# mall-product C 端 + 内部 Feign 完整开发设计

> 日期：2026-05-30
> 基线：`docs/design/10_mall-product详细设计.md` / `03_系统详细设计.md` / `07_mall-api契约层设计.md`
> 范围：C 端只读接口 + 内部 Feign 端点（管理端 CRUD 独立后续交付）

## 1. 范围

| 类别 | 内容 |
|------|------|
| C 端 API | 7 个接口（categories×2 / brands×1 / spus×2 / skus×1 / search-fallback×1） |
| 内部 Feign | 5 个端点（batchGetSku / reserveStock / releaseStock / restock / fetchAllSpus） |
| 缓存 | SKU 缓存 10min + 类目树缓存 30min（Cache Aside 双删） |
| 库存 | 四段模型（可用/锁定/已售/冻结），乐观锁防超卖，幂等补偿 |
| 搜索同步 | 双通道（Feign 实时 + Outbox/MQ 异步），补偿任务兜底 |
| MQ 消费 | `mall:order:cancelled` → 释放库存 |
| 搜索降级 | DB LIKE 兜底查询（mall-search 不可用时） |
| mall-api 契约 | 新增 `RemoteProductService` + `RemoteSearchService` Feign 接口 |
| mall-common 常量 | 补全 `CacheConstants.Product` / `MqTopicConstants` / 商品域枚举 |

## 2. 构建顺序（按依赖链 TDD 逐一推进）

```
阶段 1: mall-api 契约 + mall-common 常量补全  （不强制 TDD）
  → 阶段 2: Category  （类目 C 端只读 + 缓存）
  → 阶段 3: Brand     （品牌 C 端只读）
  → 阶段 4: SPU       （商品 C 端只读，含分页/筛选/排序）
  → 阶段 5: SKU       （规格 C 端只读 + Redis 缓存）
  → 阶段 6: Stock     （四段库存 + inner 端点 + 乐观锁）
  → 阶段 7: SearchSync/MQ（Outbox + 搜索同步 + 订单取消消费 + 补偿任务）
  → 阶段 8: SearchFallback（搜索降级兜底查询）
```

每个阶段独立完成 RED→GREEN→验证（阶段 1 外），做完一个再开始下一个。

## 3. 阶段 1：mall-api 契约 + mall-common 常量

### 3.1 RemoteProductService（Feign 契约）

文件：`server/mall/mall-api/src/main/java/com/mall/api/feign/RemoteProductService.java`

```java
@FeignClient(contextId = "mall-product", value = "mall-product")
public interface RemoteProductService {

    @GetMapping("/inner/product/skus")
    List<ProductSkuDTO> batchGetSku(@RequestParam("skuIds") List<Long> skuIds);

    @PostMapping("/inner/product/stock/reserve")
    boolean reserveStock(@RequestParam("orderNo") String orderNo,
                         @RequestBody List<ReserveStockItemRequest> items);

    @PostMapping("/inner/product/stock/release")
    void releaseStock(@RequestParam("orderNo") String orderNo);

    @PostMapping("/inner/product/stock/restock")
    void restock(@RequestParam("skuId") Long skuId,
                 @RequestParam("qty") Integer qty);

    @GetMapping("/inner/product/spus/all")
    PageResult<SpuDTO> fetchAllSpus(@RequestParam("page") int page,
                                    @RequestParam("size") int size);
}
```

内部请求体定义为静态内部类。`ProductSkuDTO` / `SpuDTO` / `PageResult` 放在 `mall-common`。

### 3.2 RemoteSearchService（Feign 契约）

文件：`server/mall/mall-api/src/main/java/com/mall/api/feign/RemoteSearchService.java`

```java
@FeignClient(contextId = "mall-search", value = "mall-search")
public interface RemoteSearchService {
    @PostMapping("/inner/search/product/sync")
    void syncProduct(@RequestBody SearchSyncRequest request);
}
```

### 3.3 mall-common 补全

| 文件 | 新增内容 |
|------|---------|
| `CacheConstants.Product` | `SKU` / `CATEGORY_TREE` / `CATEGORY` / `STOCK_RESERVE` / `OUTBOX` / `NEWEST_LIST` / `TAG` / `HOT_RANK` / `UV` |
| `MqTopicConstants.Product` / `MqTopicConstants.Order` / `MqTopicConstants.Search` | `SEARCH_SYNC` / `CANCELLED` / `SYNC` |
| `enums/product/PublishStatusEnum` | OFFLINE(0) / ONLINE(1) |
| `enums/product/VerifyStatusEnum` | PENDING(0) / APPROVED(1) / REJECTED(2) |
| `enums/product/SyncOperationEnum` | UPSERT / DELETE |

## 4. 阶段 2：Category（类目管理）

C 端只读，类目树内存构建三级嵌套，Redis 缓存 30min。

| 顺序 | 产出 | TDD |
|:---:|------|:---:|
| 2.1 | `MallCategoryDO` — `@TableName("mall_product_category")`，13 字段 | — |
| 2.2 | `MallCategoryMapper` — 继承 `BaseMapper<MallCategoryDO>`，`LambdaQueryWrapper` 查询 | — |
| 2.3 | `CategoryVO` — `@Data`，含 `List<CategoryVO> children` 构建树 | — |
| 2.4 | `CategoryConvert` (response) — `DO → VO` 静态转换 | ✅ |
| 2.5 | `ICategoryService` / `CategoryServiceImpl` — `tree()`（全量查 → 内存构建三级树）| ✅ |
| 2.6 | `CategoryCacheService` — `getTree()`（先查 Redis → miss 则 DB 回填） | ✅ |
| 2.7 | `CategoryController` — `GET /api/product/categories` + `GET /api/product/categories/{id}` | ✅ |

**C 端端点：**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/product/categories` | 三级嵌套类目树（匿名） |
| GET | `/api/product/categories/{id}` | 类目详情（匿名） |

## 5. 阶段 3：Brand（品牌管理）

| 顺序 | 产出 | TDD |
|:---:|------|:---:|
| 3.1 | `MallBrandDO` — `@TableName("mall_product_brand")` | — |
| 3.2 | `MallBrandMapper` — `LambdaQueryWrapper`，支持按 categoryId 筛选关联 SPU 的品牌 | — |
| 3.3 | `BrandVO` | — |
| 3.4 | `BrandConvert` (response) | ✅ |
| 3.5 | `IBrandService` / `BrandServiceImpl` — `listByCategory(categoryId)` | ✅ |
| 3.6 | `BrandController` — `GET /api/product/brands` | ✅ |

**C 端端点：**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/product/brands` | 品牌列表，可选 `?categoryId=` 筛选（匿名） |

## 6. 阶段 4：SPU（商品主数据）

C 端过滤：只返回已上架且审核通过的 SPU，不返回 `cost_price`。

| 顺序 | 产出 | TDD |
|:---:|------|:---:|
| 4.1 | `MallProductSpuDO` — `@TableName("mall_product_spu")` | — |
| 4.2 | `MallProductSpuMapper` — `LambdaQueryWrapper`，分页 + 筛选 + 排序 | — |
| 4.3 | `SpuVO` / `SpuDetailVO`（含 `List<SkuBriefVO>`） | — |
| 4.4 | `SpuConvert` (response) | ✅ |
| 4.5 | `ISpuService` / `SpuServiceImpl` — `page(SpuPageQuery)` / `detail(spuId)` | ✅ |
| 4.6 | `SpuController` — `GET /api/product/spus` + `GET /api/product/spus/{spuId}` | ✅ |

**分页查询参数：** `page`(默认1) / `size`(默认20, 最大100) / `categoryId` / `brandId` / `keyword` / `sort`(default/price_asc/price_desc/sales_desc)

**C 端端点：**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/product/spus` | 分页列表（匿名） |
| GET | `/api/product/spus/{spuId}` | 详情含 SKU 列表（匿名） |

## 7. 阶段 5：SKU（销售规格 + 缓存）

| 顺序 | 产出 | TDD |
|:---:|------|:---:|
| 5.1 | `MallProductSkuDO` — `@TableName("mall_product_sku")` | — |
| 5.2 | `MallProductSkuMapper` — `LambdaQueryWrapper` | — |
| 5.3 | `SkuVO`（含实时库存字段） | — |
| 5.4 | `SkuConvert` (response) | ✅ |
| 5.5 | `ISkuService` / `SkuServiceImpl` — `getBySkuId(skuId)` | ✅ |
| 5.6 | `SkuCacheService` — Cache Aside 双删，TTL 10min | ✅ |
| 5.7 | `SkuController` — `GET /api/product/skus/{skuId}` | ✅ |

**C 端端点：**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/product/skus/{skuId}` | SKU 详情含实时库存（匿名） |

## 8. 阶段 6：Stock（四段库存 + 内部端点）

四段库存模型：`available_stock`（可用）/ `locked_stock`（锁定）/ `sold_stock`（已售）/ `frozen_stock`（售后冻结）。恒等式：`available + locked + sold + frozen = total_stock`。

| 顺序 | 产出 | TDD |
|:---:|------|:---:|
| 6.1 | `MallSkuStockDO` — `@TableName("mall_product_sku_stock")`，含 `version` 乐观锁 | — |
| 6.2 | `MallSkuStockMapper` — `@Update` 注解乐观锁 SQL | — |
| 6.3 | `IStockService` / `StockServiceImpl` — `reserveStock` / `releaseStock` / `restock` | ✅ |
| 6.4 | `RemoteProductInnerController` — `/inner/product/stock/**` + `/inner/product/skus` | ✅ |

**内部端点：**

| 方法 | 路径 | 调用方 | 说明 |
|------|------|--------|------|
| GET | `/inner/product/skus` | mall-order | 批量查 SKU（含 isOnSale/availableQty/price） |
| POST | `/inner/product/stock/reserve` | mall-order | 下单锁库存，乐观锁防超卖，返回 boolean |
| POST | `/inner/product/stock/release` | mall-order | 取消释放库存，幂等去重 |
| POST | `/inner/product/stock/restock` | mall-order | 退货回补可用库存 |

**乐观锁 SQL：**
```sql
UPDATE mall_product_sku_stock
SET available_stock = available_stock - #{qty},
    locked_stock = locked_stock + #{qty},
    version = version + 1
WHERE sku_id = #{skuId}
  AND version = #{version}
  AND available_stock >= #{qty}
```

**幂等去重：** Redis key `mall:product:stock:reserve:{orderNo}:{skuId}`，TTL 24h。

## 9. 阶段 7：搜索同步 + MQ

### 9.1 Outbox 消息表

| 顺序 | 产出 | TDD |
|:---:|------|:---:|
| 7.1 | `OutboxMessageDO` / `OutboxMessageMapper` — 通用 Outbox，`biz_type` 区分业务 | — |

### 9.2 搜索同步

| 顺序 | 产出 | TDD |
|:---:|------|:---:|
| 7.2 | `RemoteSearchAdapter` — 封装 `RemoteSearchService` Feign 调用，异常转 `BusinessException` | ✅ |
| 7.3 | `SearchSyncProducer` — 商品变更后实时调 Feign → 失败写 Outbox `mall:search:sync` | ✅ |

### 9.3 MQ 消费 + 补偿

| 顺序 | 产出 | TDD |
|:---:|------|:---:|
| 7.4 | `OrderCancelledConsumer` — 消费 `mall:order:cancelled`，释放库存，幂等去重 | ✅ |
| 7.5 | `SearchSyncScheduleTask` — 定时扫描 Outbox `status=NEW`，补偿投递 | ✅ |

**MQ 事件：**

| 方向 | Topic | Payload | 说明 |
|------|-------|---------|------|
| 发布 | `mall:search:sync` | spuId, operation(UPSERT/DELETE), timestamp | 商品信息变更 |
| 消费 | `mall:order:cancelled` | orderNo, userId, cancelReason | 释放锁定库存 |

**双通道搜索同步策略：**

| 通道 | 方式 | 特点 |
|------|------|------|
| 实时 | Feign 直调 `RemoteSearchService.syncProduct()` | 同步调用，即时生效 |
| 异步 | 实时失败 → 写 Outbox → 定时投递 RocketMQ `mall:search:sync` | 降级兜底 |
| 补偿 | ruoyi-job 定时扫描 Outbox 未投递记录 | 最后保障 |

> mall-search 模块当前不存在，实时通道调用会失败降级到 Outbox 异步通道。消息在 RocketMQ 中堆积，等 mall-search 上线后消费。

## 10. 阶段 8：搜索降级兜底

ES 不可用时，mall-search 降级到 mall-product 的 DB 查询。

| 顺序 | 产出 | TDD |
|:---:|------|:---:|
| 8.1 | `SearchFallbackController` — `GET /api/product/search/fallback` | ✅ |

**C 端端点：**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/product/search/fallback` | DB LIKE 查询，`?keyword=` 必填（2-50 字），分页 `?page=&size=` |

**约束：**
- 只返回已上架商品（`publish_status=1`）
- 关键词长度 >= 2 且 <= 50
- 最大 100 条/页，3s 超时

## 11. 包结构

```
server/mall/mall-product/src/main/java/com/mall/product/
├── MallProductApplication.java                # 已有
├── config/
│   ├── MallProductConfigProperties.java       # 已有
│   ├── ProductConfig.java                     # 已有
│   └── MybatisPlusConfig.java                 # 已有
├── DO/
│   ├── MallCategoryDO.java                    # 🆕
│   ├── MallBrandDO.java                       # 🆕
│   ├── MallProductSpuDO.java                  # 🆕
│   ├── MallProductSkuDO.java                  # 🆕
│   ├── MallSkuStockDO.java                    # 🆕
│   └── OutboxMessageDO.java                   # 🆕
├── mapper/
│   ├── MallCategoryMapper.java                # 🆕
│   ├── MallBrandMapper.java                   # 🆕
│   ├── MallProductSpuMapper.java              # 🆕
│   ├── MallProductSkuMapper.java              # 🆕
│   ├── MallSkuStockMapper.java                # 🆕
│   └── OutboxMessageMapper.java               # 🆕
├── VO/
│   ├── CategoryVO.java                        # 🆕
│   ├── BrandVO.java                           # 🆕
│   ├── SpuVO.java                             # 🆕
│   ├── SpuDetailVO.java                       # 🆕
│   ├── SkuVO.java                             # 🆕
│   └── StockVO.java                           # 🆕
├── convert/
│   ├── response/
│   │   ├── CategoryConvert.java               # 🆕
│   │   ├── BrandConvert.java                  # 🆕
│   │   ├── SpuConvert.java                    # 🆕
│   │   └── SkuConvert.java                    # 🆕
│   └── request/                                    # 暂无 C 端写入，预留
├── service/
│   ├── ICategoryService.java                  # 🆕
│   ├── IBrandService.java                     # 🆕
│   ├── ISpuService.java                       # 🆕
│   ├── ISkuService.java                       # 🆕
│   ├── IStockService.java                     # 🆕
│   ├── ICategoryCacheService.java              # 🆕
│   ├── ISkuCacheService.java                   # 🆕
│   └── impl/
│       ├── CategoryServiceImpl.java           # 🆕
│       ├── BrandServiceImpl.java              # 🆕
│       ├── SpuServiceImpl.java                # 🆕
│       ├── SkuServiceImpl.java                # 🆕
│       └── StockServiceImpl.java              # 🆕
├── controller/
│   ├── inner/
│   │   └── RemoteProductInnerController.java  # 🆕 内部 Feign 端点
│   ├── CategoryController.java                # 🆕
│   ├── BrandController.java                   # 🆕
│   ├── SpuController.java                     # 🆕
│   ├── SkuController.java                     # 🆕
│   └── SearchFallbackController.java          # 🆕
└── infrastructure/
    ├── mq/
    │   ├── SearchSyncProducer.java           # 🆕
    │   └── OrderCancelledConsumer.java       # 🆕
    ├── feign/
    │   └── RemoteSearchAdapter.java          # 🆕
    └── schedule/
        └── SearchSyncScheduleTask.java       # 🆕
```

## 12. 关键设计决策

| 决策 | 结论 |
|------|------|
| 构建策略 | 方案 A：按依赖链逐一 TDD（mall-api → Category → Brand → SPU → SKU → Stock → SearchSync/MQ → 搜索降级） |
| 管理端 | 独立后续交付，本次不涉及 |
| 库存模型 | 四段库存（可用/锁定/已售/冻结），乐观锁防超卖 |
| 搜索同步 | 双通道（Feign 实时 + Outbox/MQ 异步），最终一致性 |
| mall-search 缺失 | 实时通道失败自动降级到 Outbox，消息在 MQ 堆积待消费 |
| 搜索降级 | ES 不可用时，提供 DB LIKE 兜底查询 |
| C 端错误 | 一律 `throw new BusinessException(ErrorCode.XXX)` |
| 金额单位 | 统一以"分"存储（`bigint unsigned`） |
| 缓存策略 | SKU 10min（Cache Aside 双删），类目树 30min |
| 库存补偿 | MQ 消费 + Redis 幂等去重（orderNo+skuId，24h TTL） |
| Feign 适配 | `infrastructure/feign/` 封装，异常统一转 BusinessException |
