## 七、搜索详细设计

### 7.1 ES 索引 mapping

#### 7.1.1 分词器

采用 **ik_max_word**（最细粒度），保证高召回。

| 对比 | ik_max_word | ik_smart |
|------|:--:|:--:|
| "苹果手机" | `苹果` `手机` `苹` `果` `手` `机` | `苹果` `手机` |
| 搜索"苹果"→命中"苹果手机" | ✅ | ✅ |
| 搜索"手机"→命中"苹果手机" | ✅ | ✅ |
| 搜索"果"→命中"苹果手机" | ⚠️ 噪音 | ❌ |

**约束：** 粗粒度需求通过 keyword 字段补足，不建双字段。

#### 7.1.2 索引 mapping

```json
{
  "mappings": {
    "properties": {
      "productId":    { "type": "long" },
      "spuName":      { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_max_word" },
      "subTitle":     { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_max_word" },
      "keyword":      { "type": "keyword" },
      "categoryId":   { "type": "long" },
      "categoryName": { "type": "keyword" },
      "brandId":      { "type": "long" },
      "brandName":    { "type": "keyword" },
      "price":        { "type": "integer" },
      "salesCount":   { "type": "integer" },
      "tags":         { "type": "keyword" },
      "image":        { "type": "keyword", "index": false },
      "isOnSale":     { "type": "boolean" },
      "createTime":   { "type": "date" },
      "spuSpecs":     { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_max_word" }
    }
  },
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1,
    "refresh_interval": "5s"
  }
}
```

#### 7.1.3 字段约束

| 约束 | 说明 |
|------|------|
| price 用 integer（分） | 避免浮点精度问题；客户端除以 100 显示 |
| image 不索引 | 仅用于返回，不用于搜索，节省索引空间 |
| tags 用 keyword 数组 | 支持多值精确匹配和聚合统计 |
| 禁止存 PII | 手机号、邮箱、身份证等不进 ES 索引 |

### 7.2 索引别名切换策略

#### 7.2.1 别名命名

采用**单别名**策略，读写共用同一别名：

```
别名: mall_product → 指向唯一生效索引
索引: mall_product_v{yyyyMMddHHmmss}
```

| 方案 | 说明 |
|------|------|
| 当前索引 | `mall_product_v20260517120000`（示例） |
| 别名 | `mall_product` → `mall_product_v20260517120000` |
| 新索引 | `mall_product_v20260518020000`（全量重建产物） |

**为什么不用读写分离：** 商城搜索以读为主，写极少（仅全量重建和增量同步），读写分离增加复杂度但收益极小。

#### 7.2.2 原子切换

全量重建完成后，通过 ES `_aliases` API 原子切换：

```json
POST /_aliases
{
  "actions": [
    { "remove": { "index": "mall_product_v20260517120000", "alias": "mall_product" } },
    { "add":    { "index": "mall_product_v20260518020000", "alias": "mall_product" } }
  ]
}
```

**关键约束：**

| 约束 | 说明 |
|------|------|
| 原子性 | `_aliases` 的 actions 数组在 ES 集群内部是原子操作 |
| 零停机 | 新索引上线后所有搜索请求立即路由到新索引，无感知 |
| 切换顺序 | 先 `remove` 再 `add`，确保同一时刻仅一个索引承载别名 |

#### 7.2.3 回滚策略

若新索引上线后发现搜索质量问题，可立即回滚到上一版本：

```json
POST /_aliases
{
  "actions": [
    { "remove": { "index": "mall_product_v20260518020000", "alias": "mall_product" } },
    { "add":    { "index": "mall_product_v20260517120000", "alias": "mall_product" } }
  ]
}
```

回滚后必须同步增量变更（回滚窗口内新上架/下架的商品）。

#### 7.2.4 索引生命周期

```
保留规则：
  ├─ 当前版本（别名指向）→ 保留
  ├─ 上一版本             → 保留（回滚用）
  └─ 更早版本             → 删除（释放磁盘）
```

| 配置 | 值 | 说明 |
|------|-----|------|
| 保留版本数 | 2 | 当前 + 上一版 |
| 清理时机 | 切换成功并观察 30 分钟后 | 不立即删，留观察窗口 |
| 磁盘保护 | 保留版本数上限 + 可用磁盘空间 < 20% 告警 | 防止磁盘写满 |

### 7.3 搜索实现

#### 7.3.1 查询构建方式

采用 **Spring Data Elasticsearch `NativeQuery`**，项目已有依赖，风格一致。

#### 7.3.2 全文搜索

```java
Query query = NativeQuery.builder()
    .withQuery(QueryBuilders.multiMatchQuery(keyword)
        .field("spuName", 3.0f)
        .field("subTitle", 1.5f)
        .field("spuSpecs", 1.0f)
        .type(MultiMatchQueryBuilder.Type.BEST_FIELDS))
    .build();
```

#### 7.3.3 筛选

```java
Query query = NativeQuery.builder()
    .withQuery(QueryBuilders.boolQuery()
        .must(multiMatchQuery(...))
        .filter(QueryBuilders.termQuery("categoryId", categoryId))
        .filter(QueryBuilders.termQuery("brandId", brandId))
        .filter(QueryBuilders.rangeQuery("price").gte(minPrice).lte(maxPrice))
        .filter(QueryBuilders.termsQuery("tags", selectedTags))
        .filter(QueryBuilders.termQuery("isOnSale", true)))
    .build();
```

#### 7.3.4 排序

| 排序方式 | ES 实现 | 默认方向 |
|---------|--------|:---:|
| 综合排序 | `_score`（BM25 相关性） | DESC |
| 销量优先 | `salesCount` | DESC |
| 价格低→高 | `price` | ASC |
| 价格高→低 | `price` | DESC |
| 新品优先 | `createTime` | DESC |

#### 7.3.5 分页

| 约束 | 说明 |
|------|------|
| 单页最大 | 60 条 |
| 最大页数 | from + size ≤ 10000（ES 默认限制） |
| 超限处理 | 超过 10000 条返回 A0802"搜索结果超出限制"，引导用户细化筛选 |

#### 7.3.6 高亮

```java
Query query = NativeQuery.builder()
    .withQuery(...)
    .withHighlightQuery(new HighlightQuery(
        new HighlightBuilder().field("spuName")
            .preTags("<em>").postTags("</em>").numberOfFragments(0),
        HighlightQuery.FieldParameters.class))
    .build();
```

#### 7.3.7 聚合统计

```java
Query query = NativeQuery.builder()
    .withQuery(...)
    .withAggregation("category_agg", Aggregation.of(a ->
        a.terms(t -> t.field("categoryId").size(50))))
    .withAggregation("brand_agg", Aggregation.of(a ->
        a.terms(t -> t.field("brandId").size(50))))
    .withAggregation("price_ranges", Aggregation.of(a ->
        a.range(r -> r.field("price")
            .addRange("0-100", r2 -> r2.to("10000"))
            .addRange("100-200", r2 -> r2.from("10000").to("20000"))
            .addRange("200+", r2 -> r2.from("20000")))))
    .build();
```

#### 7.3.8 搜索建议

```json
// mapping 中增加 suggest 字段
"suggest": { "type": "completion", "analyzer": "ik_max_word" }
```

商品上架/修改名称时更新 `suggest` 字段；下架时从索引删除。

#### 7.3.9 关键约束

| 约束 | 说明 |
|------|------|
| filter 不用 must | filter 不参与评分且缓存，性能优于 must |
| 搜索超时 | ES 查询超时 2s，超时返回部分结果 + 降级提示 |
| ES 不可用降级 | 返回 A0801，提示用户稍后重试 |
| 敏感数据 | ES 索引不存 PII，全是商品公开数据 |
| 并发搜索 | 不限制并发搜索，由网关层 Sentinel 限流 |

---
