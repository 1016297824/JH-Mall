# mall-product C 端 + 内部 Feign 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 逐阶段构建 mall-product 模块的 C 端查询接口 + 内部 Feign 端点 + 缓存 + 四段库存 + 搜索同步/降级

**Architecture:** 按依赖链 TDD 逐一推进（mall-api → Category → Brand → SPU → SKU → Stock → SearchSync/MQ → 搜索降级），DO/Mapper/VO 无需 TDD，Service/Controller/Converter 强制 RED→GREEN→验证

**Tech Stack:** Java 21 / Spring Boot 4.0.3 / MyBatis-Plus 3.5.15 / Redis / RocketMQ / Mockito + MockMvc

**基线 Spec:** `docs/superpowers/specs/2026-05-30-mall-product-c-end-design.md`

---

## 编码约定速查

| 约定 | 说明 |
|------|------|
| DO | `@TableName` + `@TableId(AUTO)` + `@TableField`，手写 getter/setter，`ToStringBuilder.reflectionToString()` |
| Mapper | 继承 `BaseMapper<DO>`，SELECT 用 `LambdaQueryWrapper`，UPDATE 用 `@Update` |
| Service 接口 | `I{Business}Service`，返回 VO |
| Service 实现 | `@Service` + `@RequiredArgsConstructor` + `@Slf4j`，异常 `throw new BusinessException(ErrorCode.XXX)` |
| Controller | `@RestController`，路由 `/api/product`，返回 `MallResult<T>`，从 `X-User-Id` Header 取用户 |
| Inner Controller | `controller/inner/`，路由 `/inner/product`，返回裸类型 |
| Convert | 纯静态方法，`response/` 下 `toXxxVO(DO)`，工具类 `private` 构造器 |
| VO | `@Data` + 字段 `/** Javadoc */` |
| 测试 | `@ExtendWith(MockitoExtension.class)`，镜像目录，Service 用 `@Mock` + `@InjectMocks`，Controller 用 `MockMvc` |
| 包结构 B | `infrastructure/` 下含 `mq/`、`feign/`、`schedule/` |

---

## 阶段 1：mall-api 契约 + mall-common 常量

> 本阶段无 TDD 要求（纯数据/常量/接口声明）。

### Task 1.1: RemoteProductService Feign 接口

**Files:**
- Create: `server/mall/mall-api/src/main/java/com/mall/api/feign/RemoteProductService.java`

**依赖：** 阶段 1.3 的 `ProductSkuDTO`、`SpuDTO`、`PageResult` 需先创建，或使用内联写法先占位。

- [ ] **Step 1: 创建 Feign 接口**

```java
package com.mall.api.feign;

import com.mall.common.DTO.product.ProductSkuDTO;
import com.mall.common.DTO.product.SpuDTO;
import com.mall.common.DTO.PageResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

- [ ] **Step 2: 在同一个文件中添加内部请求体**

在 `RemoteProductService` 接口内部追加：

```java
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class ReserveStockItemRequest {
        /** SKU ID */
        private Long skuId;
        /** 锁定数量 */
        private Integer qty;
    }
```

- [ ] **Step 3: 编译验证**

```bash
mvn clean install -f server/mall/pom.xml -pl mall-api -DskipTests
```

---expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add server/mall/mall-api/src/main/java/com/mall/api/feign/RemoteProductService.java
git commit -m "feat(mall-api): add RemoteProductService Feign contract"
```

### Task 1.2: RemoteSearchService Feign 接口

**Files:**
- Create: `server/mall/mall-api/src/main/java/com/mall/api/feign/RemoteSearchService.java`

- [ ] **Step 1: 创建 Feign 接口**

```java
package com.mall.api.feign;

import com.mall.api.feign.RemoteSearchService.SearchSyncRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(contextId = "mall-search", value = "mall-search")
public interface RemoteSearchService {

    @PostMapping("/inner/search/product/sync")
    void syncProduct(@RequestBody SearchSyncRequest request);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class SearchSyncRequest {
        /** SPU ID */
        private Long spuId;
        /** 同步操作类型 */
        private String operation;
        /** 时间戳 */
        private Long timestamp;
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn clean install -f server/mall/pom.xml -pl mall-api -DskipTests
```

---expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add server/mall/mall-api/src/main/java/com/mall/api/feign/RemoteSearchService.java
git commit -m "feat(mall-api): add RemoteSearchService Feign contract"
```

### Task 1.3: mall-common DTO（跨模块共享）

**Files:**
- Create: `server/mall/mall-common/src/main/java/com/mall/common/DTO/product/ProductSkuDTO.java`
- Create: `server/mall/mall-common/src/main/java/com/mall/common/DTO/product/SpuDTO.java`
- Create: `server/mall/mall-common/src/main/java/com/mall/common/DTO/PageResult.java`

- [ ] **Step 1: 创建 ProductSkuDTO**

```java
package com.mall.common.DTO.product;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSkuDTO {
    /** SKU ID */
    private Long skuId;
    /** 所属 SPU ID */
    private Long spuId;
    /** SKU 编码 */
    private String skuCode;
    /** SKU 销售名称 */
    private String skuName;
    /** 销售价（分） */
    private Long price;
    /** SKU 图片 */
    private String image;
    /** 是否在售 */
    private Boolean isOnSale;
    /** 可用库存 */
    private Integer availableQty;
}
```

- [ ] **Step 2: 创建 SpuDTO**

```java
package com.mall.common.DTO.product;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpuDTO {
    /** SPU ID */
    private Long spuId;
    /** SPU 名称 */
    private String spuName;
    /** 主图 */
    private String mainImage;
    /** 最低价（分） */
    private Long priceMin;
    /** 最高价（分） */
    private Long priceMax;
    /** 上下架状态 */
    private Integer publishStatus;
    /** 累计销量 */
    private Integer salesCount;
    /** 类目 ID */
    private Long categoryId;
    /** 品牌 ID */
    private Long brandId;
}
```

- [ ] **Step 3: 创建 PageResult**

```java
package com.mall.common.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    /** 当前页码 */
    private int page;
    /** 每页条数 */
    private int size;
    /** 总条数 */
    private long total;
    /** 数据列表 */
    private List<T> rows;

    public static <T> PageResult<T> of(int page, int size, long total, List<T> rows) {
        return new PageResult<>(page, size, total, rows);
    }
}
```

- [ ] **Step 4: 编译验证**

```bash
mvn clean install -f server/mall/pom.xml -pl mall-common -DskipTests
```

---expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add server/mall/mall-common/src/main/java/com/mall/common/DTO/product/
git add server/mall/mall-common/src/main/java/com/mall/common/DTO/PageResult.java
git commit -m "feat(mall-common): add ProductSkuDTO, SpuDTO, PageResult"
```

### Task 1.4: CacheConstants.Product 内部类

**Files:**
- Modify: `server/mall/mall-common/src/main/java/com/mall/common/constant/CacheConstants.java`

- [ ] **Step 1: 追加 Product 内部类**

在 `CacheConstants` 类内部追加：

```java
    public static final class Product {
        /** SKU 信息缓存，Key: mall:product:sku:{skuId} */
        public static final String SKU = "mall:product:sku:";
        /** 类目树缓存，Key: mall:product:category:tree */
        public static final String CATEGORY_TREE = "mall:product:category:tree";
        /** 类目详情缓存，Key: mall:product:category:{categoryId} */
        public static final String CATEGORY = "mall:product:category:";
        /** 库存扣减幂等键，Key: mall:product:stock:reserve:{orderNo}:{skuId} */
        public static final String STOCK_RESERVE = "mall:product:stock:reserve:";
        /** Outbox 消息幂等键 */
        public static final String OUTBOX = "mall:product:outbox:";
        /** C 端新品列表，Redis List */
        public static final String NEWEST_LIST = "mall:product:newest:list";
        /** 商品标签集合，Key: mall:product:tag:{tagId} */
        public static final String