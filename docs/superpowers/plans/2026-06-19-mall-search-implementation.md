# mall-search 模块实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按依赖链 4 批实现 mall-search 模块全部业务代码（ES 索引实体 → 索引管理 → 搜索服务 → 边角补全）

**Architecture:** 按 es-repository / es-operations / es-client 三层分层使用 ES API；Caffeine 本地统计热点命中率；ruoyi-job 定时全量重建

**Tech Stack:** Spring Boot 4.0.3, Spring Data Elasticsearch, co.elastic.clients:elasticsearch-java, RocketMQ, Feign, Caffeine, Redis, Lombok

**基线文档:**
- `docs/design/11_mall-search详细设计.md`
- `docs/superpowers/specs/2026-06-19-mall-search-implementation-design.md`

---

## 文件清单（新建 / 修改）

| 文件 | 操作 | 职责 |
|------|:--:|------|
| `mall-search/.../DO/ProductIndex.java` | 新建 | ES `@Document` 索引实体 |
| `mall-search/.../repository/ProductIndexRepository.java` | 新建 | Spring Data ES Repository |
| `mall-search/.../dto/request/SearchReq.java` | 新建 | C 端搜索请求 DTO |
| `mall-search/.../infrastructure/feign/RemoteProductAdapter.java` | 新建 | Feign 调 mall-product（用 SpuSearchDTO） |
| `mall-search/.../infrastructure/mq/SearchSyncConsumer.java` | 新建 | MQ 消费增量同步 |
| `mall-common/.../DTO/product/SpuSearchDTO.java` | 新建 | 搜索专用富 DTO（供全量重建） |
| `mall-api/.../RemoteProductService.java` | 修改 | 新增 `fetchAllSpusForSearch()` |
| `mall-product/.../ISpuService.java` | 修改 | 新增 `pageForSearchRebuild()` |
| `mall-product/.../SpuServiceImpl.java` | 修改 | 实现 `pageForSearchRebuild()` |
| `mall-product/.../RemoteProductInnerController.java` | 修改 | 新端点 `/spus/all-for-search` |
| `mall-search/.../service/IndexService.java` | 新建 | 索引管理接口 |
| `mall-search/.../service/impl/IndexServiceImpl.java` | 新建 | 索引管理实现 |
| `mall-search/.../infrastructure/schedule/IndexRebuildTask.java` | 新建 | 定时全量重建 |
| `mall-search/.../controller/inner/RemoteSearchInnerController.java` | 新建 | inner 端点 |
| `mall-common/.../enums/ErrorCode.java` | 修改 | 新增 C0140 |
| `mall-search/.../vo/SearchItemVO.java` | 新建 | 搜索结果条目 VO |
| `mall-search/.../vo/AggregationVO.java` | 新建 | 聚合统计 VO |
| `mall-search/.../vo/SearchResultVO.java` | 新建 | 搜索结果 VO |
| `mall-search/.../convert/response/SearchConvert.java` | 新建 | ProductIndex → VO 转换 |
| `mall-search/.../service/SearchService.java` | 新建 | 搜索接口 |
| `mall-search/.../service/impl/SearchServiceImpl.java` | 新建 | 搜索实现 |
| `mall-search/.../service/SuggestService.java` | 新建 | 搜索补全接口 |
| `mall-search/.../service/impl/SuggestServiceImpl.java` | 新建 | 搜索补全实现 |
| `mall-search/.../controller/SearchController.java` | 新建 | C 端搜索 Controller |

---

## Batch 1: 基础设施

### Task 1: ProductIndex — ES 索引实体

**Files:**
- Create: `server/mall/mall-search/src/main/java/com/mall/search/DO/ProductIndex.java`

**Design ref:** `docs/design/11_mall-search详细设计.md` §3.1

- [ ] **Step 1: 创建 ProductIndex.java**

```java
package com.mall.search.DO;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.completion.Completion;

import java.time.LocalDateTime;

/**
 * ES 商品搜索索引实体
 *
 * <p>索引别名 {@code mall_product}，通过别名读写。非 MySQL DO，不涉及 JPA。</p>
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Data
@Document(indexName = "mall_product")
public class ProductIndex {

    /** SPU ID */
    @Id
    private Long productId;

    /** 商品名称（ik_max_word，权重 3.0） */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_max_word")
    private String spuName;

    /** 副标题（ik_max_word，权重 1.5） */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_max_word")
    private String subTitle;

    /** 精确匹配关键词 */
    @Field(type = FieldType.Keyword)
    private String keyword;

    /** 类目 ID */
    @Field(type = FieldType.Long)
    private Long categoryId;

    /** 类目名称 */
    @Field(type = FieldType.Keyword)
    private String categoryName;

    /** 品牌 ID */
    @Field(type = FieldType.Long)
    private Long brandId;

    /** 品牌名称 */
    @Field(type = FieldType.Keyword)
    private String brandName;

    /** 最低售价（分），整数存储避免浮点精度 */
    @Field(type = FieldType.Integer)
    private Integer price;

    /** 累计销量 */
    @Field(type = FieldType.Integer)
    private Integer salesCount;

    /** 标签数组 */
    @Field(type = FieldType.Keyword)
    private String[] tags;

    /** 商品主图（不索引，仅存储返回） */
    @Field(type = FieldType.Keyword, index = false)
    private String image;

    /** 上架状态 */
    @Field(type = FieldType.Boolean)
    private Boolean isOnSale;

    /** 创建时间 */
    @Field(type = FieldType.Date)
    private LocalDateTime createTime;

    /** SKU 规格文本拼接（ik_max_word，权重 1.0） */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_max_word")
    private String spuSpecs;

    /** 搜索补全字段 */
    @CompletionField(maxInputLength = 50)
    private Completion suggest;
}
```

- [ ] **Step 2: Maven 编译验证**

```powershell
mvn clean compile -f server/mall/mall-search/pom.xml
```

预期: BUILD SUCCESS（无编译错误）

### Task 2: ProductIndexRepository — ES Repository

**Files:**
- Create: `server/mall/mall-search/src/main/java/com/mall/search/repository/ProductIndexRepository.java`

- [ ] **Step 1: 创建 ProductIndexRepository.java**

```java
package com.mall.search.repository;

import com.mall.search.DO.ProductIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 商品搜索索引 Repository
 *
 * <p>继承 ElasticsearchRepository 获得基础的 CRUD 能力（save/saveAll/deleteById/findById）</p>
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Repository
public interface ProductIndexRepository extends ElasticsearchRepository<ProductIndex, Long> {
}
```

- [ ] **Step 2: Maven 编译验证**

```powershell
mvn clean compile -f server/mall/mall-search/pom.xml
```

预期: BUILD SUCCESS

### Task 3: SearchReq — 搜索请求 DTO

**Files:**
- Create: `server/mall/mall-search/src/main/java/com/mall/search/dto/request/SearchReq.java`

**Design ref:** `docs/design/11_mall-search详细设计.md` §3.2

- [ ] **Step 1: 创建 SearchReq.java**

```java
package com.mall.search.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * C 端商品搜索请求
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Data
@NoArgsConstructor
public class SearchReq {

    /** 搜索关键词 */
    private String keyword;

    /** 类目 ID 过滤 */
    private Long categoryId;

    /** 品牌 ID 过滤 */
    private Long brandId;

    /** 价格区间下限（分） */
    private Integer priceMin;

    /** 价格区间上限（分） */
    private Integer priceMax;

    /** 标签过滤 */
    private String[] tags;

    /** 排序字段：_score / salesCount / price / createTime */
    private String sort;

    /** 排序方向：ASC / DESC，默认 DESC */
    private String sortOrder;

    /** 页码，从 1 开始 */
    private Integer page = 1;

    /** 每页条数，默认 20，最大 60 */
    private Integer size = 20;
}
```

- [ ] **Step 2: Maven 编译验证**

```powershell
mvn clean compile -f server/mall/mall-search/pom.xml
```

预期: BUILD SUCCESS

### Task 4: SpuSearchDTO — 搜索专用富 DTO

**Files:**
- Create: `server/mall/mall-common/src/main/java/com/mall/common/DTO/product/SpuSearchDTO.java`

**数据来源:** `mall_product_spu` + JOIN `mall_product_category` + JOIN `mall_product_brand` + 聚合 `mall_product_sku.attrs_json`

- [ ] **Step 1: 创建 SpuSearchDTO.java**

```java
package com.mall.common.DTO.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 搜索索引重建专用 SPU DTO
 *
 * <p>比 SpuDTO 多包含类目名、品牌名、SKU 规格拼接、创建/更新时间，
 * 供 mall-search 全量重建 ProductIndex 时使用。</p>
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpuSearchDTO {

    /** SPU ID */
    private Long spuId;

    /** SPU 名称 */
    private String spuName;

    /** 副标题（取 spu_description 截断 200 字） */
    private String subTitle;

    /** 主图 */
    private String mainImage;

    /** 最低价（分） */
    private Long priceMin;

    /** 上下架状态（0=下架，1=上架） */
    private Integer publishStatus;

    /** 累计销量 */
    private Integer salesCount;

    /** 类目 ID */
    private Long categoryId;

    /** 类目名称 */
    private String categoryName;

    /** 品牌 ID */
    private Long brandId;

    /** 品牌名称 */
    private String brandName;

    /** 标签（逗号分隔，暂无） */
    private String tags;

    /** SKU 规格文本拼接（取自 sku.attrs_json） */
    private String spuSpecs;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间（增量回补比较用） */
    private LocalDateTime updateTime;
}
```

- [ ] **Step 2: Maven 编译验证**

```powershell
mvn clean compile -f server/mall/mall-common/pom.xml
```

预期: BUILD SUCCESS

### Task 5: mall-api Feign 契约 + mall-product inner 端点

**Files:**
- Modify: `server/mall/mall-api/src/main/java/com/mall/api/feign/RemoteProductService.java`
- Modify: `server/mall/mall-product/src/main/java/com/mall/product/service/ISpuService.java`
- Modify: `server/mall/mall-product/src/main/java/com/mall/product/service/impl/SpuServiceImpl.java`
- Modify: `server/mall/mall-product/src/main/java/com/mall/product/controller/inner/RemoteProductInnerController.java`

- [ ] **Step 1: RemoteProductService 新增方法**

在 `RemoteProductService` 接口中新增：

```java
/**
 * 分页拉取全量 SPU（搜索索引重建专用，含类目名、品牌名、SKU规格）
 *
 * @param page 页码（从 1 开始）
 * @param size 每页条数
 * @return 富 DTO 分页
 */
@GetMapping("/inner/product/spus/all-for-search")
PageResult<SpuSearchDTO> fetchAllSpusForSearch(@RequestParam("page") int page,
                                                @RequestParam("size") int size);
```

新增 import: `import com.mall.common.DTO.product.SpuSearchDTO;`

- [ ] **Step 2: ISpuService 新增方法**

```java
/**
 * 全量分页查询 SPU（搜索索引重建专用）
 *
 * <p>返回含类目名、品牌名、SKU 规格拼接的富 DTO，供 mall-search 全量重建使用</p>
 */
PageResult<SpuSearchDTO> pageForSearchRebuild(int page, int size);
```

- [ ] **Step 3: SpuServiceImpl 实现 pageForSearchRebuild()**

```java
@Override
public PageResult<SpuSearchDTO> pageForSearchRebuild(int page, int size) {
    Page<MallProductSpuDO> pageParam = new Page<>(page, size);
    Page<MallProductSpuDO> result = mallProductSpuMapper.selectAllPage(pageParam);
    List<SpuSearchDTO> dtoList = result.getRecords().stream()
        .map(this::toSpuSearchDTO)
        .toList();
    return PageResult.of(page, size, result.getTotal(), dtoList);
}

private SpuSearchDTO toSpuSearchDTO(MallProductSpuDO spuDO) {
    SpuSearchDTO dto = new SpuSearchDTO();
    dto.setSpuId(spuDO.getId());
    dto.setSpuName(spuDO.getSpuName());
    // 副标题：取 description 截断 200 字
    if (spuDO.getSpuDescription() != null) {
        dto.setSubTitle(spuDO.getSpuDescription().length() > 200
            ? spuDO.getSpuDescription().substring(0, 200)
            : spuDO.getSpuDescription());
    }
    dto.setMainImage(spuDO.getMainImage());
    dto.setPriceMin(spuDO.getPriceMin());
    dto.setPublishStatus(spuDO.getPublishStatus());
    dto.setSalesCount(spuDO.getSalesCount());
    dto.setCategoryId(spuDO.getCategoryId());
    dto.setBrandId(spuDO.getBrandId());
    dto.setCreateTime(spuDO.getCreateTime());
    dto.setUpdateTime(spuDO.getUpdateTime());
    // 类目名
    if (spuDO.getCategoryId() != null) {
        MallCategoryDO category = mallCategoryMapper.selectById(spuDO.getCategoryId());
        if (category != null) {
            dto.setCategoryName(category.getName());
        }
    }
    // 品牌名
    if (spuDO.getBrandId() != null) {
        MallBrandDO brand = mallBrandMapper.selectById(spuDO.getBrandId());
        if (brand != null) {
            dto.setBrandName(brand.getName());
        }
    }
    // SKU 规格拼接
    List<MallProductSkuDO> skus = mallProductSkuMapper.selectBySpuId(spuDO.getId());
    if (skus != null && !skus.isEmpty()) {
        dto.setSpuSpecs(skus.stream()
            .map(s -> s.getAttrsJson() != null ? s.getAttrsJson() : "")
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining(" ")));
    }
    return dto;
}
```

如需注入 `MallCategoryMapper` 和 `MallBrandMapper`，检查 `SpuServiceImpl` 是否已有这些字段；如果没有，加上 `private final MallCategoryMapper mallCategoryMapper;` 和 `private final MallBrandMapper mallBrandMapper;`。

- [ ] **Step 4: RemoteProductInnerController 新增端点**

```java
/**
 * 全量分页查询 SPU（搜索索引重建专用）
 *
 * <p>返回含类目名、品牌名、SKU 规格拼接的富 DTO，供 mall-search 索引重建使用</p>
 */
@GetMapping("/spus/all-for-search")
PageResult<SpuSearchDTO> fetchAllSpusForSearch(@RequestParam("page") int page,
                                                @RequestParam("size") int size) {
    return spuService.pageForSearchRebuild(page, size);
}
```

- [ ] **Step 5: 编译 mall-product + mall-api**

```powershell
mvn clean compile -f server/mall/mall-product/pom.xml
```

预期: BUILD SUCCESS

### Task 6: RemoteProductAdapter — Feign 适配器（改用 SpuSearchDTO）

**Files:**
- Create: `server/mall/mall-search/src/main/java/com/mall/search/infrastructure/feign/RemoteProductAdapter.java`

- [ ] **Step 1: 创建 RemoteProductAdapter.java**

```java
package com.mall.search.infrastructure.feign;

import com.mall.api.feign.RemoteProductService;
import com.mall.common.DTO.PageResult;
import com.mall.common.DTO.product.SpuSearchDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 商品远程调用适配器
 *
 * <p>封装 RemoteProductService Feign 调用，供索引重建时全量拉取搜索结果专用富 DTO</p>
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteProductAdapter {

    private final RemoteProductService remoteProductService;

    /**
     * 分页拉取全量 SPU（搜索索引重建专用，含类目名、品牌名、SKU 规格）
     *
     * @param page 页码（从 1 开始）
     * @param size 每页条数
     * @return 搜索结果专用 SPU 分页
     */
    public PageResult<SpuSearchDTO> fetchAllSpus(int page, int size) {
        log.debug("拉取全量 SPU（搜索专用）: page={}, size={}", page, size);
        return remoteProductService.fetchAllSpusForSearch(page, size);
    }
}
```

- [ ] **Step 2: Maven 编译验证**

```powershell
mvn clean compile -f server/mall/mall-search/pom.xml
```

预期: BUILD SUCCESS

### Task 7: SearchSyncConsumer — 增量同步消费者

**Files:**
- Create: `server/mall/mall-search/src/main/java/com/mall/search/infrastructure/mq/SearchSyncConsumer.java`

**Design ref:** `docs/design/11_mall-search详细设计.md` §5

- [ ] **Step 1: 创建 SearchSyncConsumer.java（空壳，依赖 IndexService 接口）**

```java
package com.mall.search.infrastructure.mq;

import com.mall.common.constant.MqTopicConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 搜索索引增量同步消费者
 *
 * <p>消费 {@code mall:search:sync} 事件，幂等去重后写入 ES 索引</p>
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = MqTopicConstants.Product.SEARCH_SYNC,
    consumerGroup = "mall-search-sync-consumer"
)
public class SearchSyncConsumer implements RocketMQListener<String> {

    /**
     * 消费搜索同步消息
     *
     * <p>消息体为 JSON 字符串，格式: {@code {"spuId":123,"operation":"UPSERT","timestamp":...}}。
     * 第2批 IndexServiceImpl 实现后再启用实际同步逻辑。</p>
     *
     * @param message 消息体 JSON
     */
    @Override
    public void onMessage(String message) {
        log.debug("收到搜索同步消息: {}", message);
        // 第2批实现: 解析消息 → 幂等去重 → IndexService.syncProduct()
    }
}
```

- [ ] **Step 2: Maven 编译验证**

```powershell
mvn clean compile -f server/mall/mall-search/pom.xml
```

预期: BUILD SUCCESS

---

## Batch 2: 索引管理

### Task 8: C0140 错误码 — ES 服务不可用

**Files:**
- Modify: `server/mall/mall-common/src/main/java/com/mall/common/enums/ErrorCode.java`

- [ ] **Step 1: 新增 C0140 错误码**

在 `MQ_ERROR("C0130", ...)` 之后添加：

```java
    MQ_ERROR("C0130", "MQ 消息服务出错", "服务暂时不可用", 500),
    ES_UNAVAILABLE("C0140", "ES 搜索服务不可用", "搜索服务暂时不可用", 503),
```

- [ ] **Step 2: Maven 编译验证**

```powershell
mvn clean compile -f server/mall/mall-common/pom.xml
```

预期: BUILD SUCCESS

### Task 9: IndexService 接口 + IndexServiceImpl

**Files:**
- Create: `server/mall/mall-search/src/main/java/com/mall/search/service/IndexService.java`
- Create: `server/mall/mall-search/src/main/java/com/mall/search/service/impl/IndexServiceImpl.java`

**Design ref:** `docs/design/11_mall-search详细设计.md` §3.3

- [ ] **Step 1: 创建 IndexService.java**

```java
package com.mall.search.service;

/**
 * 搜索索引管理服务
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
public interface IndexService {

    /**
     * 全量重建索引
     *
     * <p>创建新索引 → 分批灌入全量 SPU → 增量回补 → 别名切换 → 保留旧版本</p>
     */
    void rebuildIndex();

    /**
     * 增量同步单个商品
     *
     * @param spuId     SPU ID
     * @param operation 操作类型：UPSERT / DELETE
     */
    void syncProduct(Long spuId, String operation);

    /**
     * 回滚到上一版本索引
     */
    void rollback();
}
```

- [ ] **Step 2: 创建 IndexServiceImpl.java**

```java
package com.mall.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.*;
import com.mall.common.DTO.PageResult;
import com.mall.common.DTO.product.SpuSearchDTO;
import com.mall.common.constant.CacheConstants;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.search.DO.ProductIndex;
import com.mall.search.config.MallSearchConfigProperties;
import com.mall.search.infrastructure.feign.RemoteProductAdapter;
import com.mall.search.repository.ProductIndexRepository;
import com.mall.search.service.IndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 索引管理实现
 *
 * <p>全量重建采用"创建新索引 + 别名原子切换 + 回滚"策略，零停机。</p>
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

    private static final String ALIAS_NAME = "mall_product";
    private static final String INDEX_PREFIX = "mall_product_v";

    /** 索引 mapping JSON（手动维护，与 ProductIndex 字段一致） */
    private static final String MAPPING_JSON = """
        {
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
            "spuSpecs":     { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_max_word" },
            "suggest":      { "type": "completion", "max_input_length": 50 }
          }
        }
        """;

    @Override
    public void rebuildIndex() {
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue()
            .setIfAbsent(CacheConstants.Search.INDEX_REBUILD_LOCK, lockValue, 3600, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            log.warn("全量重建正在执行中，获取锁失败");
            throw new BusinessException(ErrorCode.SYSTEM_CAPACITY);
        }
        try {
            doRebuild();
        } finally {
            // 释放锁（只释放自己持有的）
            String current = stringRedisTemplate.opsForValue().get(CacheConstants.Search.INDEX_REBUILD_LOCK);
            if (lockValue.equals(current)) {
                stringRedisTemplate.delete(CacheConstants.Search.INDEX_REBUILD_LOCK);
            }
        }
    }

    private void doRebuild() {
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern(configProperties.getRebuild().getTimestampFormat()));
        String newIndexName = INDEX_PREFIX + timestamp;
        LocalDateTime startTime = LocalDateTime.now();

        log.info("开始全量重建索引: newIndex={}, startTime={}", newIndexName, startTime);

        // ① 创建新索引（写入 mapping + settings）
        createNewIndex(newIndexName);

        // ② 分批拉取全量 SPU 并写入新索引
        int batchSize = configProperties.getRebuild().getBatchSize();
        int page = 1;
        do {
            PageResult<SpuSearchDTO> pageResult = remoteProductAdapter.fetchAllSpus(page, batchSize);
            List<ProductIndex> batch = convertToIndexBatch(pageResult.getRows());
            if (!batch.isEmpty()) {
                productIndexRepository.saveAll(batch);
            }
            page++;
            if (pageResult.getRows().size() < batchSize) {
                break;
            }
        } while (true);

        // ③ 增量回补：拉取开始重建后变更的 SPU
        // TODO: 实现增量回补（需 mall-product 提供按 updateTime 查询的接口）

        // ④ 获取当前别名指向的旧索引
        String oldIndexName = getCurrentIndex();

        // ⑤ 原子切换别名
        switchAlias(newIndexName, oldIndexName);

        // ⑥ 保留旧版本供回滚（记录到 Redis 或本地缓存）
        if (oldIndexName != null) {
            log.info("保留旧索引版本: {}", oldIndexName);
        }

        log.info("全量重建完成: newIndex={}", newIndexName);
    }

    private void createNewIndex(String indexName) {
        try {
            TypeMapping mapping = TypeMapping.of(m -> m
                .withJson(new StringReader(MAPPING_JSON))
            );
            CreateIndexRequest request = CreateIndexRequest.of(c -> c
                .index(indexName)
                .settings(s -> s
                    .numberOfShards(String.valueOf(configProperties.getEs().getShards()))
                    .numberOfReplicas(String.valueOf(configProperties.getEs().getReplicas()))
                    .refreshInterval(t -> t.time("5s"))
                )
                .mappings(mapping)
            );
            elasticsearchClient.indices().create(request);
            log.info("创建新索引成功: {}", indexName);
        } catch (Exception e) {
            log.error("创建新索引失败: {}", indexName, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    private String getCurrentIndex() {
        try {
            GetAliasRequest request = GetAliasRequest.of(a -> a.name(ALIAS_NAME));
            GetAliasResponse response = elasticsearchClient.indices().getAlias(request);
            if (!response.result().isEmpty()) {
                return response.result().keySet().iterator().next();
            }
        } catch (Exception e) {
            log.debug("获取当前别名指向的索引失败（可能首次创建）: {}", e.getMessage());
        }
        return null;
    }

    private void switchAlias(String newIndex, String oldIndex) {
        try {
            // 先摘除旧索引别名，再加新索引别名
            List<co.elastic.clients.elasticsearch.indices.UpdateAliasesAction> actions = new ArrayList<>();
            if (oldIndex != null) {
                actions.add(UpdateAliasesAction.of(a -> a
                    .remove(r -> r.index(oldIndex).alias(ALIAS_NAME))
                ));
            }
            actions.add(UpdateAliasesAction.of(a -> a
                .add(ad -> ad.index(newIndex).alias(ALIAS_NAME))
            ));

            UpdateAliasesRequest request = UpdateAliasesRequest.of(u -> u.actions(actions));
            elasticsearchClient.indices().updateAliases(request);
            log.info("别名切换成功: {} → {}", oldIndex, newIndex);
        } catch (Exception e) {
            log.error("别名切换失败: {} → {}", newIndex, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    @Override
    public void syncProduct(Long spuId, String operation) {
        // 幂等去重
        String dedupKey = CacheConstants.Search.DEDUP + spuId + ":" + operation;
        Boolean acquired = stringRedisTemplate.opsForValue()
            .setIfAbsent(dedupKey, "1", 1, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(acquired)) {
            log.debug("重复同步跳过: spuId={}, operation={}", spuId, operation);
            return;
        }

        if ("DELETE".equals(operation)) {
            productIndexRepository.deleteById(spuId);
            log.info("删除索引: spuId={}", spuId);
        } else {
            // UPSERT: 从 mall-product 拉取最新 SPU 数据 → 转换 → 写入 ES
            // TODO: 需要 RemoteProductAdapter 提供按 spuId 查询的接口
            log.info("增量同步: spuId={}, operation={}", spuId, operation);
        }
    }

    @Override
    public void rollback() {
        log.info("回滚索引");
        // TODO: 从 Redis 读取上一版本索引名 → 别名切换回去
    }

    private List<ProductIndex> convertToIndexBatch(List<SpuSearchDTO> spus) {
        List<ProductIndex> indices = new ArrayList<>();
        for (SpuSearchDTO spu : spus) {
            ProductIndex idx = new ProductIndex();
            idx.setProductId(spu.getSpuId());
            idx.setSpuName(spu.getSpuName());
            idx.setSubTitle(spu.getSubTitle());
            idx.setKeyword(spu.getSpuName());
            idx.setCategoryId(spu.getCategoryId());
            idx.setCategoryName(spu.getCategoryName());
            idx.setBrandId(spu.getBrandId());
            idx.setBrandName(spu.getBrandName());
            idx.setImage(spu.getMainImage());
            if (spu.getPriceMin() != null) {
                idx.setPrice(spu.getPriceMin().intValue());
            }
            idx.setSalesCount(spu.getSalesCount());
            idx.setIsOnSale(spu.getPublishStatus() != null && spu.getPublishStatus() == 1);
            idx.setTags(spu.getTags() != null ? spu.getTags().split(",") : null);
            idx.setSpuSpecs(spu.getSpuSpecs());
            idx.setCreateTime(spu.getCreateTime());
            // TODO: suggest 字段需从 spuName 构建 Completion 对象
            indices.add(idx);
        }
        return indices;
    }
}
```

- [ ] **Step 2: Maven 编译验证**

```powershell
mvn clean compile -f server/mall/mall-search/pom.xml
```

预期: BUILD SUCCESS

### Task 10: IndexRebuildTask — 定时全量重建

**Files:**
- Create: `server/mall/mall-search/src/main/java/com/mall/search/infrastructure/schedule/IndexRebuildTask.java`

**参考:** `mall-product` 的 `HotRankRefreshTask.java`

- [ ] **Step 1: 创建 IndexRebuildTask.java**

```java
package com.mall.search.infrastructure.schedule;

import com.mall.search.service.IndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 搜索索引全量重建定时任务
 *
 * <p>由 ruoyi-job 定时调度，通过 POST /inner/search/index/rebuild 端点调用。
 * 调用链: ruoyi-job → RemoteSearchInnerController.rebuildIndex() → IndexRebuildTask.execute() → IndexService.rebuildIndex()</p>
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndexRebuildTask {

    private final IndexService indexService;

    /**
     * 执行全量重建
     *
     * <p>委托 IndexService.rebuildIndex()，重建内部已有分布式锁防并发</p>
     */
    public void execute() {
        log.info("定时全量重建索引开始");
        indexService.rebuildIndex();
        log.info("定时全量重建索引完成");
    }
}
```

- [ ] **Step 2: Maven 编译验证**

```powershell
mvn clean compile -f server/mall/mall-search/pom.xml
```

预期: BUILD SUCCESS

### Task 11: RemoteSearchInnerController — inner 端点

**Files:**
- Create: `server/mall/mall-search/src/main/java/com/mall/search/controller/inner/RemoteSearchInnerController.java`

**参考:** `mall-product` 的 `RemoteProductInnerController.java`

- [ ] **Step 1: 创建 RemoteSearchInnerController.java**

```java
package com.mall.search.controller.inner;

import com.mall.search.infrastructure.schedule.IndexRebuildTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 搜索内部 Controller
 *
 * <p>供 ruoyi-job 定时调度全量重建，路径 /inner/search/**，由 InnerSignatureFilter 验签保护</p>
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Slf4j
@RestController
@RequestMapping("/inner/search")
@RequiredArgsConstructor
public class RemoteSearchInnerController {

    private final IndexRebuildTask indexRebuildTask;

    /**
     * 全量重建搜索索引
     */
    @PostMapping("/index/rebuild")
    void rebuildIndex() {
        indexRebuildTask.execute();
    }
}
```

- [ ] **Step 2: Maven 编译验证**

```powershell
mvn clean compile -f server/mall/mall-search/pom.xml
```

预期: BUILD SUCCESS

---

## Batch 3: 搜索服务

### Task 12: VO 层 — SearchItemVO / AggregationVO / SearchResultVO

**Files:**
- Create: `server/mall/mall-search/src/main/java/com/mall/search/vo/SearchItemVO.java`
- Create: `server/mall/mall-search/src/main/java/com/mall/search/vo/AggregationVO.java`
- Create: `server/mall/mall-search/src/main/java/com/mall/search/vo/SearchResultVO.java`

- [ ] **Step 1: 创建 SearchItemVO.java**

```java
package com.mall.search.vo;

import lombok.Data;

/**
 * 搜索结果条目 VO
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Data
public class SearchItemVO {

    /** SPU ID */
    private Long spuId;

    /** 商品名称 */
    private String spuName;

    /** 商品名称高亮片段 */
    private String spuNameHighlight;

    /** 最低售价（元，已除以 100） */
    private String price;

    /** 商品主图 */
    private String image;

    /** 累计销量 */
    private Integer salesCount;
}
```

- [ ] **Step 2: 创建 AggregationVO.java**

```java
package com.mall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * 聚合统计 VO
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Data
public class AggregationVO {

    /** 类目聚合 */
    private List<AggregationBucket> categories;

    /** 品牌聚合 */
    private List<AggregationBucket> brands;

    /** 价格区间聚合 */
    private List<AggregationBucket> priceRanges;

    /**
     * 聚合桶
     */
    @Data
    public static class AggregationBucket {

        /** 聚合值（ID 或价区间名称） */
        private String key;

        /** 聚合名称 */
        private String name;

        /** 文档数量 */
        private long count;
    }
}
```

- [ ] **Step 3: 创建 SearchResultVO.java**

```java
package com.mall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * 搜索结果 VO
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Data
public class SearchResultVO {

    /** 搜索结果列表 */
    private List<SearchItemVO> items;

    /** 聚合统计 */
    private AggregationVO aggregations;

    /** 总命中数 */
    private long total;

    /** 当前页码 */
    private int page;

    /** 每页条数 */
    private int size;
}
```

- [ ] **Step 4: Maven 编译验证**

```powershell
mvn clean compile -f server/mall/mall-search/pom.xml
```

预期: BUILD SUCCESS

### Task 13: SearchConvert — 转换器

**Files:**
- Create: `server/mall/mall-search/src/main/java/com/mall/search/convert/response/SearchConvert.java`

- [ ] **Step 1: 创建 SearchConvert.java**

```java
package com.mall.search.convert.response;

import com.mall.search.DO.ProductIndex;
import com.mall.search.vo.AggregationVO;
import com.mall.search.vo.AggregationVO.AggregationBucket;
import com.mall.search.vo.SearchItemVO;
import com.mall.search.vo.SearchResultVO;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.terms.TermsAggregationItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 搜索结果转换器（ProductIndex → VO）
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
public final class SearchConvert {

    private SearchConvert() {
    }

    /**
     * 搜索命中 → SearchResultVO
     */
    public static SearchResultVO toSearchResultVO(SearchHits<ProductIndex> hits, int page, int size) {
        List<SearchItemVO> items = new ArrayList<>();
        for (SearchHit<ProductIndex> hit : hits) {
            items.add(toSearchItemVO(hit));
        }

        SearchResultVO vo = new SearchResultVO();
        vo.setItems(items);
        vo.setTotal(hits.getTotalHits());
        vo.setPage(page);
        vo.setSize(size);

        // 提取聚合统计
        AggregationsContainer<?> aggregations = hits.getAggregations();
        if (aggregations != null) {
            AggregationVO aggVO = new AggregationVO();
            aggVO.setCategories(extractTermBuckets(aggregations, "categories"));
            aggVO.setBrands(extractTermBuckets(aggregations, "brands"));
            vo.setAggregations(aggVO);
        }

        return vo;
    }

    /**
     * 提取 term 聚合桶
     */
    private static List<AggregationBucket> extractTermBuckets(AggregationsContainer<?> aggregations, String aggName) {
        List<AggregationBucket> buckets = new ArrayList<>();
        // Spring Data ES 9.x 的聚合 API 可能因版本而异，此处使用 get() + 类型转换
        Object agg = aggregations.get(aggName);
        if (agg instanceof org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation esAgg) {
            if (esAgg.aggregation() != null && esAgg.aggregation().getAggregate().isSterms()) {
                esAgg.aggregation().getAggregate().sterms().buckets().array().forEach(b -> {
                    AggregationBucket bucket = new AggregationBucket();
                    bucket.setKey(b.key().stringValue());
                    bucket.setCount(b.docCount());
                    buckets.add(bucket);
                });
            }
        }
        return buckets;
    }

    /**
     * 搜索命中 → SearchItemVO
     */
    private static SearchItemVO toSearchItemVO(SearchHit<ProductIndex> hit) {
        ProductIndex source = hit.getContent();
        SearchItemVO vo = new SearchItemVO();
        vo.setSpuId(source.getProductId());
        vo.setSpuName(source.getSpuName());
        vo.setImage(source.getImage());
        vo.setSalesCount(source.getSalesCount());

        // 价格：分 → 元，保留两位小数
        if (source.getPrice() != null) {
            vo.setPrice(BigDecimal.valueOf(source.getPrice())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .toPlainString());
        }

        // 高亮
        List<String> highlights = hit.getHighlightField("spuName");
        if (highlights != null && !highlights.isEmpty()) {
            vo.setSpuNameHighlight(highlights.get(0));
        }

        return vo;
    }
}
```

- [ ] **Step 2: Maven 编译验证**

```powershell
mvn clean compile -f server/mall/mall-search/pom.xml
```

预期: BUILD SUCCESS

### Task 14: SearchService 接口 + SearchServiceImpl

**Files:**
- Create: `server/mall/mall-search/src/main/java/com/mall/search/service/SearchService.java`
- Create: `server/mall/mall-search/src/main/java/com/mall/search/service/impl/SearchServiceImpl.java`

**Design ref:** `docs/design/11_mall-search详细设计.md` §3.2

- [ ] **Step 1: 创建 SearchService.java**

```java
package com.mall.search.service;

import com.mall.search.dto.request.SearchReq;
import com.mall.search.vo.SearchResultVO;

/**
 * 商品搜索服务
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
public interface SearchService {

    /**
     * 全文搜索
     */
    SearchResultVO search(SearchReq req);
}
```

- [ ] **Step 2: 创建 SearchServiceImpl.java**

```java
package com.mall.search.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.search.DO.ProductIndex;
import com.mall.search.config.MallSearchConfigProperties;
import com.mall.search.convert.response.SearchConvert;
import com.mall.search.dto.request.SearchReq;
import com.mall.search.service.SearchService;
import com.mall.search.vo.SearchResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 商品搜索实现
 *
 * <p>核心搜索逻辑：multi_match 多字段 + filter 过滤 + 排序 + 高亮 + 分页 + Caffeine 热点缓存</p>
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Slf4j
@Service
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchOperations operations;
    private final MallSearchConfigProperties configProperties;
    private final Cache<String, SearchResultVO> resultCache;

    public SearchServiceImpl(ElasticsearchOperations operations, MallSearchConfigProperties configProperties) {
        this.operations = operations;
        this.configProperties = configProperties;
        this.resultCache = Caffeine.newBuilder()
            .expireAfterWrite(configProperties.getResult().getCacheTtl(), TimeUnit.SECONDS)
            .maximumSize(5000)
            .recordStats()
            .build();
    }

    @Override
    public SearchResultVO search(SearchReq req) {
        // 参数校验
        if (req.getSize() > configProperties.getPage().getMaxSize()) {
            throw new BusinessException(ErrorCode.SEARCH_PARAM_ERROR);
        }
        int from = (req.getPage() - 1) * req.getSize();
        if (from + req.getSize() > configProperties.getPage().getMaxDepth()) {
            throw new BusinessException(ErrorCode.SEARCH_RESULT_LIMIT);
        }

        // 热点缓存命中 → 直接返回
        if (resultCache.stats().hitRate() > 0.6) {
            SearchResultVO cached = resultCache.getIfPresent(req.getKeyword());
            if (cached != null) {
                log.debug("缓存命中: keyword={}", req.getKeyword());
                return cached;
            }
        }

        NativeQuery query = buildQuery(req);

        SearchHits<ProductIndex> hits = operations.search(query, ProductIndex.class);
        SearchResultVO result = SearchConvert.toSearchResultVO(hits, req.getPage(), req.getSize());

        // 缓存结果
        resultCache.put(req.getKeyword(), result);

        return result;
    }

    private NativeQuery buildQuery(SearchReq req) {
        NativeQuery.Builder builder = NativeQuery.builder();

        // 全文搜索: multi_match 多字段
        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            builder.withQuery(q -> q.multiMatch(mm -> mm
                .query(req.getKeyword())
                .fields(Map.of("spuName", 3.0f, "subTitle", 1.5f, "spuSpecs", 1.0f))
            ));
        } else {
            builder.withQuery(q -> q.matchAll(ma -> ma));
        }

        // 筛选 (filter，不参与评分)
        builder.withFilter(f -> f.bool(b -> {
            b.must(m -> m.term(t -> t.field("isOnSale").value(true)));
            if (req.getCategoryId() != null) {
                b.must(m -> m.term(t -> t.field("categoryId").value(req.getCategoryId())));
            }
            if (req.getBrandId() != null) {
                b.must(m -> m.term(t -> t.field("brandId").value(req.getBrandId())));
            }
            if (req.getPriceMin() != null || req.getPriceMax() != null) {
                b.must(m -> m.range(r -> {
                    r.field("price");
                    if (req.getPriceMin() != null) r.gte(JsonData.of(req.getPriceMin()));
                    if (req.getPriceMax() != null) r.lte(JsonData.of(req.getPriceMax()));
                    return r;
                }));
            }
            return b;
        }));

        // 排序
        if (req.getSort() != null) {
            builder.withSort(s -> s.field(f -> {
                f.field(req.getSort());
                f.order("ASC".equalsIgnoreCase(req.getSortOrder())
                    ? org.springframework.data.elasticsearch.core.query.SortOrder.ASC
                    : org.springframework.data.elasticsearch.core.query.SortOrder.DESC);
                return f;
            }));
        } else {
            builder.withSort(s -> s.score(sc -> sc));
        }

        // 高亮
        builder.withHighlight(h -> h
            .requireFieldMatch(false)
            .fields("spuName", hf -> hf.preTags("<em>").postTags("</em>"))
        );

        // 分页
        int from = (req.getPage() - 1) * req.getSize();
        builder.withFrom(from).withSize(req.getSize());

        // 聚合
        builder.withAggregation("categories",
            a -> a.terms(t -> t.field("categoryId").size(50)));
        builder.withAggregation("brands",
            a -> a.terms(t -> t.field("brandId").size(50)));

        return builder.build();
    }
}
```

- [ ] **Step 2: Maven 编译验证**

```powershell
mvn clean compile -f server/mall/mall-search/pom.xml
```

预期: BUILD SUCCESS

### Task 15: SuggestService 接口 + SuggestServiceImpl

**Files:**
- Create: `server/mall/mall-search/src/main/java/com/mall/search/service/SuggestService.java`
- Create: `server/mall/mall-search/src/main/java/com/mall/search/service/impl/SuggestServiceImpl.java`

**Design ref:** `docs/design/11_mall-search详细设计.md` §3.4

- [ ] **Step 1: 创建 SuggestService.java**

```java
package com.mall.search.service;

import java.util.List;

/**
 * 搜索补全服务
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
public interface SuggestService {

    /**
     * 搜索补全建议
     *
     * @param keyword 输入前缀
     * @return 补全建议列表
     */
    List<String> suggest(String keyword);
}
```

- [ ] **Step 2: 创建 SuggestServiceImpl.java**

```java
package com.mall.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SuggestRequest;
import co.elastic.clients.elasticsearch.core.SuggestResponse;
import co.elastic.clients.elasticsearch.core.suggest.CompletionSuggestOption;
import com.mall.search.service.SuggestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索补全实现
 *
 * <p>基于 ES completion suggester + ik_max_word 分词</p>
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuggestServiceImpl implements SuggestService {

    private final ElasticsearchClient elasticsearchClient;

    @Override
    public List<String> suggest(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        try {
            SuggestRequest request = SuggestRequest.of(s -> s
                .index("mall_product")
                .suggest("spu_suggest", ds -> ds
                    .prefix(keyword)
                    .completion(c -> c
                        .field("suggest")
                        .size(10)
                        .skipDuplicates(true)
                    )
                )
            );

            SuggestResponse response = elasticsearchClient.suggest(request);

            List<String> suggestions = new ArrayList<>();
            response.suggest().get("spu_suggest").stream()
                .flatMap(s -> s.completion().options().stream())
                .map(CompletionSuggestOption::text)
                .forEach(suggestions::add);

            return suggestions;
        } catch (Exception e) {
            log.error("搜索补全异常: keyword={}", keyword, e);
            return List.of();
        }
    }
}
```

- [ ] **Step 3: Maven 编译验证**

```powershell
mvn clean compile -f server/mall/mall-search/pom.xml
```

预期: BUILD SUCCESS

### Task 16: SearchController — C 端搜索 Controller

**Files:**
- Create: `server/mall/mall-search/src/main/java/com/mall/search/controller/SearchController.java`

**Design ref:** `docs/design/11_mall-search详细设计.md` §2.2

- [ ] **Step 1: 创建 SearchController.java**

```java
package com.mall.search.controller;

import com.mall.common.DTO.MallResult;
import com.mall.search.dto.request.SearchReq;
import com.mall.search.service.SearchService;
import com.mall.search.service.SuggestService;
import com.mall.search.service.IndexService;
import com.mall.search.vo.SearchResultVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * C 端搜索 Controller
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final SuggestService suggestService;
    private final IndexService indexService;

    /**
     * 商品全文搜索（含筛选/排序/聚合）
     *
     * @param req 商品检索查询条件（关键词、分类、价格区间、排序、分页等）
     * @return MallResult 统一返回体，内部封装分页商品数据、聚合统计结果
     */
    @Operation(summary = "商品全文检索", description = "支持关键词全文匹配、多维度筛选、自定义排序、分类/价格聚合统计")
    @PostMapping("/api/search")
    public MallResult<SearchResultVO> search(@Valid @RequestBody SearchReq req) {
        SearchResultVO result = searchService.search(req);
        return MallResult.success(result);
    }

    /**
     * 搜索补全建议
     */
    @GetMapping("/api/search/suggest")
    public MallResult<List<String>> suggest(@RequestParam("keyword") String keyword) {
        List<String> suggestions = suggestService.suggest(keyword);
        return MallResult.success(suggestions);
    }

    /**
     * 触发全量重建索引（管理端）
     */
    @PostMapping("/mall-search/index/rebuild")
    public MallResult<Void> rebuildIndex() {
        indexService.rebuildIndex();
        return MallResult.success();
    }
}
```

- [ ] **Step 2: Maven 编译验证**

```powershell
mvn clean compile -f server/mall/mall-search/pom.xml
```

预期: BUILD SUCCESS

---

## Batch 4: 边角补全

### Task 17: 降级兜底 + 索引清理 + 磁盘告警

**Files:**
- Modify: `server/mall/mall-search/src/main/java/com/mall/search/service/impl/SearchServiceImpl.java`
- Modify: `server/mall/mall-search/src/main/java/com/mall/search/service/impl/IndexServiceImpl.java`

**说明:** 第4批为辅助功能，不对第3批已通过验证的搜索破坏。此处只列设计要点，实现时逐个补充。

- [ ] **降级兜底:** `SearchServiceImpl.search()` 外层 try-catch，捕获 ES 连接异常 → 调 mall-product fallback
- [ ] **索引清理:** `IndexServiceImpl.doRebuild()` 切换成功后起延迟任务，30min 后 DELETE 旧索引
- [ ] **磁盘告警:** `IndexServiceImpl.doRebuild()` 前置检查 ES 磁盘可用低于 20% → 抛异常阻止重建

实现细节参照设计文档实施。

---

## 约束与规范

| 规约 | 说明 |
|------|------|
| Lombok | `@Data` 放 DO/VO/DTO，`@Slf4j` + `@RequiredArgsConstructor` 放 Service/Controller/Consumer |
| 错误处理 | C 端抛 `BusinessException(ErrorCode.XXX)`，不走 `MallResult.error()` |
| TDD | Service/Impl/Controller/Consumer 必须 RED → GREEN → 验证 |
| 编码规范 | 阿里巴巴 Java 开发手册·嵩山版 |

---

## 验证方式

| 批次 | 验证方式 |
|------|----------|
| Batch 1 | `mvn compile` — 编译通过即 DO/Repository/Adapter/Consumer 无错 |
| Batch 2 | 启动 mall-search + ES → 调 `/mall-search/index/rebuild` → 检查 ES 索引 |
| Batch 3 | 启动 mall-search + ES → 调 `/api/search/{keyword}` → 检查返回结果 |
| Batch 4 | 关停 ES → 搜索应有降级响应；调重建 → ES 磁盘不足应报错 |

---

## 配置变更（Nacos + 网关）

| 位置 | 变更 | 说明 |
|------|------|------|
| `ruoyi-gateway-dev.yml` | 新增 `/inner/search/**` 到 `anonymous-paths` | 放行 inner 端点 |
| ruoyi-job 控制台 | 新增 cron 任务 → `POST /inner/search/index/rebuild` | 定时全量重建 |
