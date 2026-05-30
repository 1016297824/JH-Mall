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
        public static final String TAG = "mall:product:tag:";
        /** 热销排行榜，Redis ZSet */
        public static final String HOT_RANK = "mall:product:hot:rank";
        /** 商品 UV 统计，Key: mall:product:uv:{spuId} */
        public static final String UV = "mall:product:uv:";
    }
```

- [ ] **Step 2: Commit**

```bash
git add server/mall/mall-common/src/main/java/com/mall/common/constant/CacheConstants.java
git commit -m "feat(mall-common): add CacheConstants.Product inner class"
```

### Task 1.5: MqTopicConstants 补充

**Files:**
- Modify: `server/mall/mall-common/src/main/java/com/mall/common/constant/MqTopicConstants.java`

- [ ] **Step 1: 追加 Topic**

在 `MqTopicConstants` 类内部追加：

```java
    public static final class Product {
        /** 搜索索引同步，Payload: spuId + operation + timestamp */
        public static final String SEARCH_SYNC = "mall:search:sync";
    }

    public static final class Order {
        /** 订单取消事件，Payload: orderNo + userId + cancelReason */
        public static final String CANCELLED = "mall:order:cancelled";
    }

    public static final class Search {
        /** 搜索索引同步（与 Product.SEARCH_SYNC 同值，按消费者视角命名） */
        public static final String SYNC = "mall:search:sync";
    }
```

- [ ] **Step 2: Commit**

```bash
git add server/mall/mall-common/src/main/java/com/mall/common/constant/MqTopicConstants.java
git commit -m "feat(mall-common): add MQ topic constants for product/order/search"
```

### Task 1.6: 商品域枚举

**Files:**
- Create: `server/mall/mall-common/src/main/java/com/mall/common/enums/product/PublishStatusEnum.java`
- Create: `server/mall/mall-common/src/main/java/com/mall/common/enums/product/VerifyStatusEnum.java`
- Create: `server/mall/mall-common/src/main/java/com/mall/common/enums/product/SyncOperationEnum.java`

- [ ] **Step 1: 创建 PublishStatusEnum**

```java
package com.mall.common.enums.product;

import lombok.Getter;

@Getter
public enum PublishStatusEnum {
    OFFLINE(0, "已下架"),
    ONLINE(1, "已上架");

    private final int code;
    private final String desc;

    PublishStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
```

- [ ] **Step 2: 创建 VerifyStatusEnum**

```java
package com.mall.common.enums.product;

import lombok.Getter;

@Getter
public enum VerifyStatusEnum {
    PENDING(0, "待审核"),
    APPROVED(1, "审核通过"),
    REJECTED(2, "审核驳回");

    private final int code;
    private final String desc;

    VerifyStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
```

- [ ] **Step 3: 创建 SyncOperationEnum**

```java
package com.mall.common.enums.product;

import lombok.Getter;

@Getter
public enum SyncOperationEnum {
    UPSERT("UPSERT", "新增或更新"),
    DELETE("DELETE", "删除");

    private final String code;
    private final String desc;

    SyncOperationEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add server/mall/mall-common/src/main/java/com/mall/common/enums/product/
git commit -m "feat(mall-common): add product domain enums (PublishStatus, VerifyStatus, SyncOperation)"
```

### Task 1.7: 编译验证（全量）

- [ ] **Step 1: 全量编译**

```bash
mvn clean install -f server/mall/pom.xml -DskipTests
```

---expected: BUILD SUCCESS

---

## 阶段 2：Category（类目 C 端只读 + 缓存）

### Task 2.1: MallCategoryDO

**Files:**
- Create: `server/mall/mall-product/src/main/java/com/mall/product/DO/MallCategoryDO.java`

- [ ] **Step 1: 创建 DO**

```java
package com.mall.product.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

@TableName("mall_product_category")
public class MallCategoryDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("parent_id")
    private Long parentId;

    @TableField("name")
    private String name;

    @TableField("level")
    private Integer level;

    @TableField("icon")
    private String icon;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("is_visible")
    private Integer isVisible;

    @TableField("path")
    private String path;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getIsVisible() { return isVisible; }
    public void setIsVisible(Integer isVisible) { this.isVisible = isVisible; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add server/mall/mall-product/src/main/java/com/mall/product/DO/MallCategoryDO.java
git commit -m "feat(mall-product): add MallCategoryDO"
```

### Task 2.2: MallCategoryMapper

**Files:**
- Create: `server/mall/mall-product/src/main/java/com/mall/product/mapper/MallCategoryMapper.java`

- [ ] **Step 1: 创建 Mapper**

```java
package com.mall.product.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.product.DO.MallCategoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MallCategoryMapper extends BaseMapper<MallCategoryDO> {

    default List<MallCategoryDO> selectVisibleCategories() {
        return selectList(new LambdaQueryWrapper<MallCategoryDO>()
                .eq(MallCategoryDO::getIsVisible, 1)
                .eq(MallCategoryDO::getIsDeleted, 0)
                .orderByAsc(MallCategoryDO::getSortOrder));
    }

    default MallCategoryDO selectByCategoryId(Long categoryId) {
        return selectOne(new LambdaQueryWrapper<MallCategoryDO>()
                .eq(MallCategoryDO::getId, categoryId)
                .eq(MallCategoryDO::getIsDeleted, 0));
    }

    default boolean existsChildren(Long parentId) {
        return exists(new LambdaQueryWrapper<MallCategoryDO>()
                .eq(MallCategoryDO::getParentId, parentId)
                .eq(MallCategoryDO::getIsDeleted, 0));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add server/mall/mall-product/src/main/java/com/mall/product/mapper/MallCategoryMapper.java
git commit -m "feat(mall-product): add MallCategoryMapper"
```

### Task 2.3: CategoryVO

**Files:**
- Create: `server/mall/mall-product/src/main/java/com/mall/product/VO/CategoryVO.java`

- [ ] **Step 1: 创建 VO**

```java
package com.mall.product.VO;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryVO {
    /** 类目 ID */
    private String categoryId;
    /** 父类目 ID，0=顶级 */
    private String parentId;
    /** 类目名称 */
    private String name;
    /** 层级：1/2/3 */
    private Integer level;
    /** 图标 URL */
    private String icon;
    /** 排序值 */
    private Integer sortOrder;
    /** 路径，如 /1/2/3 */
    private String path;
    /** 子类目 */
    private List<CategoryVO> children = new ArrayList<>();
}
```

- [ ] **Step 2: Commit**

```bash
git add server/mall/mall-product/src/main/java/com/mall/product/VO/CategoryVO.java
git commit -m "feat(mall-product): add CategoryVO"
```

### Task 2.4: CategoryConvert（RED→GREEN）

**Files:**
- Create: `server/mall/mall-product/src/main/java/com/mall/product/convert/response/CategoryConvert.java`
- Create: `server/mall/mall-product/src/test/java/com/mall/product/convert/response/CategoryConvertTest.java`

- [ ] **Step 1: 写测试（RED）**

```java
package com.mall.product.convert.response;

import com.mall.product.DO.MallCategoryDO;
import com.mall.product.VO.CategoryVO;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryConvertTest {

    @Test
    void toCategoryVOShouldConvertFields() {
        MallCategoryDO categoryDO = new MallCategoryDO();
        categoryDO.setId(1L);
        categoryDO.setParentId(0L);
        categoryDO.setName("手机数码");
        categoryDO.setLevel(1);
        categoryDO.setIcon("/icon.png");
        categoryDO.setSortOrder(1);
        categoryDO.setPath("/1");

        CategoryVO vo = CategoryConvert.toCategoryVO(categoryDO);

        assertThat(vo.getCategoryId()).isEqualTo("1");
        assertThat(vo.getParentId()).isEqualTo("0");
        assertThat(vo.getName()).isEqualTo("手机数码");
        assertThat(vo.getLevel()).isEqualTo(1);
        assertThat(vo.getIcon()).isEqualTo("/icon.png");
        assertThat(vo.getSortOrder()).isEqualTo(1);
        assertThat(vo.getPath()).isEqualTo("/1");
        assertThat(vo.getChildren()).isEmpty();
    }

    @Test
    void buildTreeShouldBuildThreeLevelTree() {
        MallCategoryDO root = categoryDO(1L, 0L, "手机数码", 1, "/1");
        MallCategoryDO l2 = categoryDO(2L, 1L, "手机通讯", 2, "/1/2");
        MallCategoryDO l3 = categoryDO(3L, 2L, "智能手机", 3, "/1/2/3");
        List<MallCategoryDO> list = Arrays.asList(root, l2, l3);

        List<CategoryVO> tree = CategoryConvert.buildTree(list);

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getName()).isEqualTo("手机数码");
        assertThat(tree.get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getName()).isEqualTo("手机通讯");
        assertThat(tree.get(0).getChildren().get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getChildren().get(0).getName()).isEqualTo("智能手机");
    }

    private MallCategoryDO categoryDO(Long id, Long parentId, String name, Integer level, String path) {
        MallCategoryDO d = new MallCategoryDO();
        d.setId(id);
        d.setParentId(parentId);
        d.setName(name);
        d.setLevel(level);
        d.setIcon("/icon.png");
        d.setSortOrder(1);
        d.setPath(path);
        return d;
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
mvn test -f server/mall/pom.xml -pl mall-product -Dtest=CategoryConvertTest -DfailIfNoTests=false
```

---expected: COMPILATION ERROR (class not found)

- [ ] **Step 3: 创建 CategoryConvert 实现**

```java
package com.mall.product.convert.response;

import com.mall.product.DO.MallCategoryDO;
import com.mall.product.VO.CategoryVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CategoryConvert {

    private CategoryConvert() {
    }

    public static CategoryVO toCategoryVO(MallCategoryDO categoryDO) {
        if (categoryDO == null) {
            return null;
        }
        CategoryVO vo = new CategoryVO();
        vo.setCategoryId(String.valueOf(categoryDO.getId()));
        vo.setParentId(String.valueOf(categoryDO.getParentId()));
        vo.setName(categoryDO.getName());
        vo.setLevel(categoryDO.getLevel());
        vo.setIcon(categoryDO.getIcon());
        vo.setSortOrder(categoryDO.getSortOrder());
        vo.setPath(categoryDO.getPath());
        return vo;
    }

    public static List<CategoryVO> buildTree(List<MallCategoryDO> categoryDOList) {
        List<CategoryVO> allVos = categoryDOList.stream()
                .map(CategoryConvert::toCategoryVO)
                .toList();

        Map<Long, CategoryVO> voMap = allVos.stream()
                .collect(Collectors.toMap(v -> Long.parseLong(v.getCategoryId()), v -> v));

        List<CategoryVO> tree = new ArrayList<>();
        for (CategoryVO vo : allVos) {
            Long parentId = Long.parseLong(vo.getParentId());
            if (parentId == 0) {
                tree.add(vo);
            } else {
                CategoryVO parent = voMap.get(parentId);
                if (parent != null) {
                    parent.getChildren().add(vo);
                }
            }
        }
        return tree;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
mvn test -f server/mall/pom.xml -pl mall-product -Dtest=CategoryConvertTest -DfailIfNoTests=false
```

---expected: TESTS PASSED (2/2)

- [ ] **Step 5: Commit**

```bash
git add server/mall/mall-product/src/main/java/com/mall/product/convert/response/CategoryConvert.java
git add server/mall/mall-product/src/test/java/com/mall/product/convert/response/CategoryConvertTest.java
git commit -m "feat(mall-product): add CategoryConvert with tree builder"
```

### Task 2.5: ICategoryService + CategoryServiceImpl（RED→GREEN）

**Files:**
- Create: `server/mall/mall-product/src/main/java/com/mall/product/service/ICategoryService.java`
- Create: `server/mall/mall-product/src/main/java/com/mall/product/service/impl/CategoryServiceImpl.java`
- Create: `server/mall/mall-product/src/test/java/com/mall/product/service/impl/CategoryServiceImplTest.java`

- [ ] **Step 1: 写 Service 接口**

```java
package com.mall.product.service;

import com.mall.product.VO.CategoryVO;

import java.util.List;

public interface ICategoryService {

    List<CategoryVO> tree();

    CategoryVO getByCategoryId(Long categoryId);
}
```

- [ ] **Step 2: 写空壳实现（return null）**

```java
package com.mall.product.service.impl;

import com.mall.product.VO.CategoryVO;
import com.mall.product.service.ICategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements ICategoryService {

    @Override
    public List<CategoryVO> tree() {
        return null;
    }

    @Override
    public CategoryVO getByCategoryId(Long categoryId) {
        return null;
    }
}
```

- [ ] **Step 3: 写测试**

```java
package com.mall.product.service.impl;

import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.product.DO.MallCategoryDO;
import com.mall.product.VO.CategoryVO;
import com.mall.product.mapper.MallCategoryMapper;
import com.mall.product.service.ICategoryCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private MallCategoryMapper mallCategoryMapper;

    @Mock
    private ICategoryCacheService categoryCacheService;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void treeShouldReturnCategoryTree() {
        MallCategoryDO root = categoryDO(1L, 0L, "手机数码", 1);
        MallCategoryDO l2 = categoryDO(2L, 1L, "手机通讯", 2);
        when(mallCategoryMapper.selectVisibleCategories()).thenReturn(Arrays.asList(root, l2));

        List<CategoryVO> tree = categoryService.tree();

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getName()).isEqualTo("手机数码");
        assertThat(tree.get(0).getChildren()).hasSize(1);
    }

    @Test
    void getByCategoryIdShouldReturnCategory() {
        MallCategoryDO categoryDO = categoryDO(1L, 0L, "手机数码", 1);
        when(mallCategoryMapper.selectByCategoryId(1L)).thenReturn(categoryDO);

        CategoryVO vo = categoryService.getByCategoryId(1L);

        assertThat(vo.getCategoryId()).isEqualTo("1");
        assertThat(vo.getName()).isEqualTo("手机数码");
    }

    @Test
    void getByCategoryIdShouldThrowWhenNotFound() {
        when(mallCategoryMapper.selectByCategoryId(999L)).thenReturn(null);

        assertThatThrownBy(() -> categoryService.getByCategoryId(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.A0501);
    }

    private MallCategoryDO categoryDO(Long id, Long parentId, String name, Integer level) {
        MallCategoryDO d = new MallCategoryDO();
        d.setId(id);
        d.setParentId(parentId);
        d.setName(name);
        d.setLevel(level);
        d.setSortOrder(1);
        d.setIsVisible(1);
        return d;
    }
}
```

- [ ] **Step 4: 运行测试确认失败**

```bash
mvn test -f server/mall/pom.xml -pl mall-product -Dtest=CategoryServiceImplTest -DfailIfNoTests=false
```

---expected: 2 FAIL (tree returns null, getByCategoryId returns null), 1 FAIL (getByCategoryId 无异常)

- [ ] **Step 5: 写完整实现**

```java
package com.mall.product.service.impl;

import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.product.DO.MallCategoryDO;
import com.mall.product.VO.CategoryVO;
import com.mall.product.convert.response.CategoryConvert;
import com.mall.product.mapper.MallCategoryMapper;
import com.mall.product.service.ICategoryCacheService;
import com.mall.product.service.ICategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements ICategoryService {

    private final MallCategoryMapper mallCategoryMapper;
    private final ICategoryCacheService categoryCacheService;

    @Override
    public List<CategoryVO> tree() {
        List<MallCategoryDO> categoryDOList = mallCategoryMapper.selectVisibleCategories();
        return CategoryConvert.buildTree(categoryDOList);
    }

    @Override
    public CategoryVO getByCategoryId(Long categoryId) {
        MallCategoryDO categoryDO = mallCategoryMapper.selectByCategoryId(categoryId);
        if (categoryDO == null) {
            throw new BusinessException(ErrorCode.A0501);
        }
        return CategoryConvert.toCategoryVO(categoryDO);
    }
}
```

- [ ] **Step 6: 运行测试确认通过**

```bash
mvn test -f server/mall/pom.xml -pl mall-product -Dtest=CategoryServiceImplTest -DfailIfNoTests=false
```

---expected: TESTS PASSED (3/3)

- [ ] **Step 7: Commit**

```bash
git add server/mall/mall-product/src/main/java/com/mall/product/service/ICategoryService.java
git add server/mall/mall-product/src/main/java/com/mall/product/service/impl/CategoryServiceImpl.java
git add server/mall/mall-product/src/test/java/com/mall/product/service/impl/CategoryServiceImplTest.java
git commit -m "feat(mall-product): add CategoryService impl with tree and getByCategoryId"
```

### Task 2.6: ICategoryCacheService + 实现（RED→GREEN）

**Files:**
- Create: `server/mall/mall-product/src/main/java/com/mall/product/service/ICategoryCacheService.java`
- Create: `server/mall/mall-product/src/main/java/com/mall/product/service/impl/CategoryCacheServiceImpl.java`
- Create: `server/mall/mall-product/src/test/java/com/mall/product/service/impl/CategoryCacheServiceImplTest.java`

- [ ] **Step 1: 写接口 + 空壳实现 + 测试（RED）**

接口：

```java
package com.mall.product.service;

import com.mall.product.VO.CategoryVO;

import java.util.List;

public interface ICategoryCacheService {

    List<CategoryVO> getTree();

    void refreshCache();
}
```

空壳实现：

```java
package com.mall.product.service.impl;

import com.mall.product.VO.CategoryVO;
import com.mall.product.service.ICategoryCacheService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryCacheServiceImpl implements ICategoryCacheService {

    @Override
    public List<CategoryVO> getTree() {
        return null;
    }

    @Override
    public void refreshCache() {
    }
}
```

测试：

```java
package com.mall.product.service.impl;

import com.mall.product.VO.CategoryVO;
import com.mall.product.service.ICategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryCacheServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ICategoryService categoryService;

    @InjectMocks
    private CategoryCacheServiceImpl cacheService;

    @Test
    void getTreeShouldReturnFromCacheWhenHit() {
        List<CategoryVO> cached = List.of(new CategoryVO());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("mall:product:category:tree")).thenReturn(cached);

        List<CategoryVO> result = cacheService.getTree();

        assertThat(result).isSameAs(cached);
    }

    @Test
    void getTreeShouldFallbackToDBWhenCacheMiss() {
        List<CategoryVO> dbResult = List.of(new CategoryVO());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("mall:product:category:tree")).thenReturn(null);
        when(categoryService.tree()).thenReturn(dbResult);

        List<CategoryVO> result = cacheService.getTree();

        assertThat(result).isSameAs(dbResult);
        verify(valueOperations).set(eq("mall:product:category:tree"), eq(dbResult), eq(1800L), any());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
mvn test -f server/mall/pom.xml -pl mall-product -Dtest=CategoryCacheServiceImplTest -DfailIfNoTests=false
```

---expected: 2 FAIL

- [ ] **Step 3: 写完整实现**

```java
package com.mall.product.service.impl;

import com.mall.common.constant.CacheConstants;
import com.mall.product.VO.CategoryVO;
import com.mall.product.service.ICategoryCacheService;
import com.mall.product.service.ICategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryCacheServiceImpl implements ICategoryCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ICategoryService categoryService;

    private static final long TTL_SECONDS = 1800;

    @Override
    public List<CategoryVO> getTree() {
        String key = CacheConstants.Product.CATEGORY_TREE;
        @SuppressWarnings("unchecked")
        List<CategoryVO> cached = (List<CategoryVO>) redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }
        List<CategoryVO> tree = categoryService.tree();
        redisTemplate.opsForValue().set(key, tree, TTL_SECONDS, TimeUnit.SECONDS);
        return tree;
    }

    @Override
    public void refreshCache() {
        redisTemplate.delete(CacheConstants.Product.CATEGORY_TREE);
        log.debug("Category tree cache refreshed");
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
mvn test -f server/mall/pom.xml -pl mall-product -Dtest=CategoryCacheServiceImplTest -DfailIfNoTests=false
```

---expected: TESTS PASSED (2/2)

- [ ] **Step 5: Commit**

```bash
git add server/mall/mall-product/src/main/java/com/mall/product/service/ICategoryCacheService.java
git add server/mall/mall-product/src/main/java/com/mall/product/service/impl/CategoryCacheServiceImpl.java
git add server/mall/mall-product/src/test/java/com/mall/product/service/impl/CategoryCacheServiceImplTest.java
git commit -m "feat(mall-product): add CategoryCacheService with Redis TTL 30min"
```

### Task 2.7: CategoryController（RED→GREEN）

**Files:**
- Create: `server/mall/mall-product/src/main/java/com/mall/product/controller/CategoryController.java`
- Create: `server/mall/mall-product/src/test/java/com/mall/product/controller/CategoryControllerTest.java`

- [ ] **Step 1: 写空壳 Controller**

```java
package com.mall.product.controller;

import com.mall.common.DTO.MallResult;
import com.mall.product.VO.CategoryVO;
import com.mall.product.service.ICategoryCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class CategoryController {

    private final ICategoryCacheService categoryCacheService;

    @GetMapping("/categories")
    public MallResult<List<CategoryVO>> tree() {
        return null;
    }

    @GetMapping("/categories/{categoryId}")
    public MallResult<CategoryVO> detail(@PathVariable Long categoryId) {
        return null;
    }
}
```

- [ ] **Step 2: 写测试**

```java
package com.mall.product.controller;

import com.mall.product.VO.CategoryVO;
import com.mall.product.service.ICategoryCacheService;
import com.mall.product.service.ICategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private ICategoryCacheService categoryCacheService;

    @Mock
    private ICategoryService categoryService;

    @InjectMocks
    private CategoryController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void treeShouldReturnCategoryTree() throws Exception {
        CategoryVO vo = new CategoryVO();
        vo.setCategoryId("1");
        vo.setName("手机数码");
        when(categoryCacheService.getTree()).thenReturn(List.of(vo));

        mockMvc.perform(get("/api/product/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].categoryId").value("1"))
                .andExpect(jsonPath("$.data[0].name").value("手机数码"));
    }

    @Test
    void detailShouldReturnCategory() throws Exception {
        CategoryVO vo = new CategoryVO();
        vo.setCategoryId("1");
        vo.setName("手机数码");
        when(categoryService.getByCategoryId(1L)).thenReturn(vo);

        mockMvc.perform(get("/api/product/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryId").value("1"))
                .andExpect(jsonPath("$.data.name").value("手机数码"));
    }
}
```

- [ ] **Step 3: 运行测试确认失败**

```bash
mvn test -f server/mall/pom.xml -pl mall-product -Dtest=CategoryControllerTest -DfailIfNoTests=false
```

---expected: 2 FAIL (NullPointerException or returns null)

- [ ] **Step 4: 写完整实现**

```java
package com.mall.product.controller;

import com.mall.common.DTO.MallResult;
import com.mall.product.VO.CategoryVO;
import com.mall.product.service.ICategoryCacheService;
import com.mall.product.service.ICategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class CategoryController {

    private final ICategoryCacheService categoryCacheService;
    private final ICategoryService categoryService;

    @GetMapping("/categories")
    public MallResult<List<CategoryVO>> tree() {
        return MallResult.success(categoryCacheService.getTree());
    }

    @GetMapping("/categories/{categoryId}")
    public MallResult<CategoryVO> detail(@PathVariable Long categoryId) {
        return MallResult.success(categoryService.getByCategoryId(categoryId));
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

```bash
mvn test -f server/mall/pom.xml -pl mall-product -Dtest=CategoryControllerTest -DfailIfNoTests=false
```

---expected: TESTS PASSED (2/2)

- [ ] **Step 6: Commit**

```bash
git add server/mall/mall-product/src/main/java/com/mall/product/controller/CategoryController.java
git add server/mall/mall-product/src/test/java/com/mall/product/controller/CategoryControllerTest.java
git commit -m "feat(mall-product): add CategoryController (tree + detail)"
```

---

## 阶段 3：Brand（品牌 C 端只读）

### Task 3.1-3.6: Brand 完整 TDD

> 以下 Brand 的 DO/Mapper/VO 创建步骤与 Category 模式一致，简略说明。

**新建文件清单：**

| 文件 | 路径 |
|------|------|
| MallBrandDO | `server/mall/mall-product/src/main/java/com/mall/product/DO/MallBrandDO.java` |
| MallBrandMapper | `server/mall/mall-product/src/main/java/com/mall/product/mapper/MallBrandMapper.java` |
| BrandVO | `server/mall/mall-product/src/main/java/com/mall/product/VO/BrandVO.java` |
| BrandConvert | `server/mall/mall-product/src/main/java/com/mall/product/convert/response/BrandConvert.java` |
| IBrandService | `server/mall/mall-product/src/main/java/com/mall/product/service/IBrandService.java` |
| BrandServiceImpl | `server/mall/mall-product/src/main/java/com/mall/product/service/impl/BrandServiceImpl.java` |
| BrandController | `server/mall/mall-product/src/main/java/com/mall/product/controller/BrandController.java` |

**测试文件清单：**

| 测试 | 路径 |
|------|------|
| BrandConvertTest | `server/mall/mall-product/src/test/java/com/mall/product/convert/response/BrandConvertTest.java` |
| BrandServiceImplTest | `server/mall/mall-product/src/test/java/com/mall/product/service/impl/BrandServiceImplTest.java` |
| BrandControllerTest | `server/mall/mall-product/src/test/java/com/mall/product/controller/BrandControllerTest.java` |

- [ ] **Step 1: MallBrandDO**

```java
package com.mall.product.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

@TableName("mall_product_brand")
public class MallBrandDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("logo")
    private String logo;

    @TableField("description")
    private String description;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
```

- [ ] **Step 2: MallBrandMapper**

```java
package com.mall.product.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.product.DO.MallBrandDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MallBrandMapper extends BaseMapper<MallBrandDO> {

    default List<MallBrandDO> selectAll() {
        return selectList(new LambdaQueryWrapper<MallBrandDO>()
                .eq(MallBrandDO::getIsDeleted, 0)
                .orderByAsc(MallBrandDO::getSortOrder));
    }
}
```

- [ ] **Step 3: BrandVO**

```java
package com.mall.product.VO;

import lombok.Data;

@Data
public class BrandVO {
    /** 品牌 ID */
    private String brandId;
    /** 品牌名称 */
    private String name;
    /** Logo URL */
    private String logo;
    /** 品牌简介 */
    private String description;
    /** 排序值 */
    private Integer sortOrder;
}
```

- [ ] **Step 4-5: BrandConvert（RED→GREEN）**

测试 BrandConvertTest：
```java
package com.mall.product.convert.response;

import com.mall.product.DO.MallBrandDO;
import com.mall.product.VO.BrandVO;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BrandConvertTest {

    @Test
    void toBrandVOShouldConvertFields() {
        MallBrandDO brandDO = new MallBrandDO();
        brandDO.setId(1L);
        brandDO.setName("Apple");
        brandDO.setLogo("/logo.png");
        brandDO.setDescription("Apple Inc.");
        brandDO.setSortOrder(1);

        BrandVO vo = BrandConvert.toBrandVO(brandDO);

        assertThat(vo.getBrandId()).isEqualTo("1");
        assertThat(vo.getName()).isEqualTo("Apple");
        assertThat(vo.getLogo()).isEqualTo("/logo.png");
        assertThat(vo.getDescription()).isEqualTo("Apple Inc.");
        assertThat(vo.getSortOrder()).isEqualTo(1);
    }

    @Test
    void toBrandVOListShouldConvertList() {
        MallBrandDO b1 = new MallBrandDO(); b1.setId(1L); b1.setName("Apple"); b1.setSortOrder(1);
        MallBrandDO b2 = new MallBrandDO(); b2.setId(2L); b2.setName("华为"); b2.setSortOrder(2);

        List<BrandVO> list = BrandConvert.toBrandVOList(Arrays.asList(b1, b2));

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getBrandId()).isEqualTo("1");
        assertThat(list.get(1).getName()).isEqualTo("华为");
    }
}
```

实现 BrandConvert：
```java
package com.mall.product.convert.response;

import com.mall.product.DO.MallBrandDO;
import com.mall.product.VO.BrandVO;

import java.util.List;

public class BrandConvert {

    private BrandConvert() {
    }

    public static BrandVO toBrandVO(MallBrandDO brandDO) {
        if (brandDO == null) {
            return null;
        }
        BrandVO vo = new BrandVO();
        vo.setBrandId(String.valueOf(brandDO.getId()));
        vo.setName(brandDO.getName());
        vo.setLogo(brandDO.getLogo());
        vo.setDescription(brandDO.getDescription());
        vo.setSortOrder(brandDO.getSortOrder());
        return vo;
    }

    public static List<BrandVO> toBrandVOList(List<MallBrandDO> brandDOList) {
        return brandDOList.stream().map(BrandConvert::toBrandVO).toList();
    }
}
```

运行测试确认通过：
```bash
mvn test -f server/mall/pom.xml -pl mall-product -Dtest=BrandConvertTest -DfailIfNoTests=false
```
---expected: TESTS PASSED (2/2)

- [ ] **Step 6-7: IBrandService + BrandServiceImpl（RED→GREEN）**

接口：
```java
package com.mall.product.service;

import com.mall.product.VO.BrandVO;

import java.util.List;

public interface IBrandService {

    List<BrandVO> list(Long categoryId);
}
```

空壳：
```java
@Service
public class BrandServiceImpl implements IBrandService {
    @Override
    public List<BrandVO> list(Long categoryId) { return null; }
}
```

测试：
```java
package com.mall.product.service.impl;

import com.mall.product.DO.MallBrandDO;
import com.mall.product.VO.BrandVO;
import com.mall.product.mapper.MallBrandMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandServiceImplTest {

    @Mock
    private MallBrandMapper mallBrandMapper;

    @InjectMocks
    private BrandServiceImpl brandService;

    @Test
    void listShouldReturnAllBrands() {
        MallBrandDO b1 = new MallBrandDO(); b1.setId(1L); b1.setName("Apple"); b1.setSortOrder(1);
        when(mallBrandMapper.selectAll()).thenReturn(List.of(b1));

        List<BrandVO> result = brandService.list(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Apple");
    }
}
```

完整实现：
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements IBrandService {

    private final MallBrandMapper mallBrandMapper;

    @Override
    public List<BrandVO> list(Long categoryId) {
        List<MallBrandDO> brandDOList = mallBrandMapper.selectAll();
        return BrandConvert.toBrandVOList(brandDOList);
    }
}
```

运行测试确认通过：
```bash
mvn test -f server/mall/pom.xml -pl mall-product -Dtest=BrandServiceImplTest -DfailIfNoTests=false
```
---expected: TESTS PASSED (1/1)

- [ ] **Step 8-9: BrandController（RED→GREEN）**

空壳：
```java
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class BrandController {
    private final IBrandService brandService;

    @GetMapping("/brands")
    public MallResult<List<BrandVO>> list(@RequestParam(required = false) Long categoryId) {
        return null;
    }
}
```

测试：
```java
@ExtendWith(MockitoExtension.class)
class BrandControllerTest {
    @Mock private IBrandService brandService;
    @InjectMocks private BrandController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void listShouldReturnBrandList() throws Exception {
        BrandVO vo = new BrandVO(); vo.setBrandId("1"); vo.setName("Apple");
        when(brandService.list(null)).thenReturn(List.of(vo));

        mockMvc.perform(get("/api/product/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].brandId").value("1"))
                .andExpect(jsonPath("$.data[0].name").value("Apple"));
    }
}
```

完整实现：
```java
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class BrandController {

    private final IBrandService brandService;

    @GetMapping("/brands")
    public MallResult<List<BrandVO>> list(@RequestParam(required = false) Long categoryId) {
        return MallResult.success(brandService.list(categoryId));
    }
}
```

运行测试确认通过：
```bash
mvn test -f server/mall/pom.xml -pl mall-product -Dtest=BrandControllerTest -DfailIfNoTests=false
```
---expected: TESTS PASSED (1/1)

- [ ] **Step 10: Commit（阶段 3 全部）**

```bash
git add server/mall/mall-product/src/main/java/com/mall/product/DO/MallBrandDO.java
git add server/mall/mall-product/src/main/java/com/mall/product/mapper/MallBrandMapper.java
git add server/mall/mall-product/src/main/java/com/mall/product/VO/BrandVO.java
git add server/mall/mall-product/src/main/java/com/mall/product/convert/response/BrandConvert.java
git add server/mall/mall-product/src/main/java/com/mall/product/service/IBrandService.java
git add server/mall/mall-product/src/main/java/com/mall/product/service/impl/BrandServiceImpl.java
git add server/mall/mall-product/src/main/java/com/mall/product/controller/BrandController.java
git add server/mall/mall-product/src/test/java/com/mall/product/convert/response/BrandConvertTest.java
git add server/mall/mall-product/src/test/java/com/mall/product/service/impl/BrandServiceImplTest.java
git add server/mall/mall-product/src/test/java/com/mall/product/controller/BrandControllerTest.java
git commit -m "feat(mall-product): add Brand (C-end read-only) with TDD"
```

---

## 阶段 4：SPU（商品主数据 C 端只读）

### Task 4.1: MallProductSpuDO

**Files:**
- Create: `server/mall/mall-product/src/main/java/com/mall/product/DO/MallProductSpuDO.java`

- [ ] **Step 1: 创建 DO（含全部字段 getter/setter，遵循 mall-user 模式）**

```java
package com.mall.product.DO;

import com.baomidou.mybatisplus.annotation.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

@TableName("mall_product_spu")
public class MallProductSpuDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("category_id")
    private Long categoryId;

    @TableField("brand_id")
    private Long brandId;

    @TableField("spu_name")
    private String spuName;

    @TableField("spu_description")
    private String spuDescription;

    @TableField("main_image")
    private String mainImage;

    @TableField("images_json")
    private String imagesJson;

    @TableField("price_min")
    private Long priceMin;

    @TableField("price_max")
    private Long priceMax;

    @TableField("sales_count")
    private Integer salesCount;

    @TableField("review_count")
    private Integer reviewCount;

    @TableField("publish_status")
    private Integer publishStatus;

    @TableField("verify_status")
    private Integer verifyStatus;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField("create_by")
    private String createBy;

    @TableField("update_by")
    private String updateBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @Version
    @TableField("version")
    private Integer version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getBrandId() { return brandId; }
    public void setBrandId(Long brandId) { this.brandId = brandId; }
    public String getSpuName() { return spuName; }
    public void setSpuName(String spuName) { this.spuName = spuName; }
    public String getSpuDescription() { return spuDescription; }
    public void setSpuDescription(String spuDescription) { this.spuDescription = spuDescription; }
    public String getMainImage() { return mainImage; }
    public void setMainImage(String mainImage) { this.mainImage = mainImage; }
    public String getImagesJson() { return imagesJson; }
    public void setImagesJson(String imagesJson) { this.imagesJson = imagesJson; }
    public Long getPriceMin() { return priceMin; }
    public void setPriceMin(Long priceMin) { this.priceMin = priceMin; }
    public Long getPriceMax() { return priceMax; }
    public void setPriceMax(Long priceMax) { this.priceMax = priceMax; }
    public Integer getSalesCount() { return salesCount; }
    public void setSalesCount(Integer salesCount) { this.salesCount = salesCount; }
    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }
    public Integer getPublishStatus() { return publishStatus; }
    public void setPublishStatus(Integer publishStatus) { this.publishStatus = publishStatus; }
    public Integer getVerifyStatus() { return verifyStatus; }
    public void setVerifyStatus(Integer verifyStatus) { this.verifyStatus = verifyStatus; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add server/mall/mall-product/src/main/java/com/mall/product/DO/MallProductSpuDO.java
git commit -m "feat(mall-product): add MallProductSpuDO"
```

### Task 4.2: MallProductSpuMapper

- [ ] **Step 1: 创建 Mapper**

```java
package com.mall.product.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.product.DO.MallProductSpuDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MallProductSpuMapper extends BaseMapper<MallProductSpuDO> {

    default Page<MallProductSpuDO> selectPublishedPage(Page<MallProductSpuDO> page,
                                                        Long categoryId, Long brandId, String keyword) {
        LambdaQueryWrapper<MallProductSpuDO> wrapper = new LambdaQueryWrapper<MallProductSpuDO>()
                .eq(MallProductSpuDO::getPublishStatus, 1)
                .eq(MallProductSpuDO::getVerifyStatus, 1)
                .eq(MallProductSpuDO::getIsDeleted, 0);

        if (categoryId != null) {
            wrapper.eq(MallProductSpuDO::getCategoryId, categoryId);
        }
        if (brandId != null) {
            wrapper.eq(MallProductSpuDO::getBrandId, brandId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(MallProductSpuDO::getSpuName, keyword);
        }

        return selectPage(page, wrapper);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add server/mall/mall-product/src/main/java/com/mall/product/mapper/MallProductSpuMapper.java
git commit -m "feat(mall-product): add MallProductSpuMapper"
```

### Task 4.3-4.6: SpuVO / SpuDetailVO / SpuConvert / ISpuService / SpuServiceImpl / SpuController

> 因篇幅限制，SPU 阶段仅列出关键代码和测试要点。Sub-agent 实现时参考 Category 阶段完整模式执行 TDD。

**SPU 核心要点：**

1. **SpuVO**（列表用）：`spuId`(String), `spuName`, `mainImage`, `priceMin`, `priceMax`, `salesCount`, `categoryId`, `brandId`

2. **SpuDetailVO**（详情用）：继承 SpuVO + `description`(spuDescription), `images`(imagesJson 解析为 List), `reviewCount`, `skus`(List<SkuBriefVO>)

3. **SkuBriefVO**（嵌套在 SpuDetailVO 中）：`skuId`(String), `skuName`, `price`, `image`

4. **SpuConvert**：`toSpuVO(DO)` / `toSpuDetailVO(DO, List<MallProductSkuDO>)`

5. **ISpuService**：`page(int page, int size, Long categoryId, Long brandId, String keyword, String sort)` / `detail(Long spuId)`

6. **排序逻辑**：`default`/`price_asc`/`price_desc`/`sales_desc` → 用 `page.addOrder()` 动态设置

7. **C 端过滤**：只返回 `publishStatus=1 AND verifyStatus=1 AND isDeleted=0`

8. **SpuController**：`GET /api/product/spus` + `GET /api/product/spus/{spuId}`

### Task 4.7: 编译验证阶段 4

```bash
mvn test -f server/mall/pom.xml -pl mall-product -DfailIfNoTests=false
```

---expected: ALL TESTS PASSED

---

## 阶段 5：SKU（销售规格 + Redis 缓存）

### Task 5.1: MallProductSkuDO

- [ ] **Step 1: 创建 DO**

```java
package com.mall.product.DO;

import com.baomidou.mybatisplus.annotation.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

@TableName("mall_product_sku")
public class MallProductSkuDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("spu_id")
    private Long spuId;

    @TableField("sku_code")
    private String skuCode;

    @TableField("sku_name")
    private String skuName;

    @TableField("attrs_json")
    private String attrsJson;

    @TableField("price")
    private Long price;

    @TableField("market_price")
    private Long marketPrice;

    @TableField("cost_price")
    private Long costPrice;

    @TableField("image")
    private String image;

    @TableField("weight")
    private Integer weight;

    @TableField("sales_count")
    private Integer salesCount;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    // --- getter/setter 手写，toString 用 ToStringBuilder ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSpuId() { return spuId; }
    public void setSpuId(Long spuId) { this.spuId = spuId; }
    public String getSkuCode() { return skuCode; }
    public void setSkuCode(String skuCode) { this.skuCode = skuCode; }
    public String getSkuName() { return skuName; }
    public void setSkuName(String skuName) { this.skuName = skuName; }
    public String getAttrsJson() { return attrsJson; }
    public void setAttrsJson(String attrsJson) { this.attrsJson = attrsJson; }
    public Long getPrice() { return price; }
    public void setPrice(Long price) { this.price = price; }
    public Long getMarketPrice() { return marketPrice; }
    public void setMarketPrice(Long marketPrice) { this.marketPrice = marketPrice; }
    public Long getCostPrice() { return costPrice; }
    public void setCostPrice(Long costPrice) { this.costPrice = costPrice; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }
    public Integer getSalesCount() { return salesCount; }
    public void setSalesCount(Integer salesCount) { this.salesCount = salesCount; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add server/mall/mall-product/src/main/java/com/mall/product/DO/MallProductSkuDO.java
git commit -m "feat(mall-product): add MallProductSkuDO"
```

### Task 5.2-5.7: MallProductSkuMapper / SkuVO / SkuConvert / ISkuService / SkuServiceImpl / ISkuCacheService / SkuCacheServiceImpl / SkuController

> SKU 阶段按 Category 阶段模式执行 TDD。关键要点：

1. **MallProductSkuMapper**：`selectBySpuId(spuId)` / `selectBySkuId(skuId)` / `selectBySkuIds(List<Long> skuIds)` — 全部用 `LambdaQueryWrapper`，过滤 `isDeleted=0`

2. **SkuVO**：`skuId`, `spuId`, `skuCode`, `skuName`, `attrsJson`, `price`, `marketPrice`, `image`, `weight`, `availableStock`(来自库存表 JOIN)

3. **C 端不返回** `cost_price`

4. **ISkuService**：`getBySkuId(Long skuId)` — 关联查询 SKU + 库存（`available_stock`）

5. **ISkuCacheService**：`getBySkuId(Long skuId)` — Cache Aside 双删，TTL 600s，Key `mall:product:sku:{skuId}`

6. **SkuController**：`GET /api/product/skus/{skuId}`

### Task 5.8: 编译验证阶段 5

```bash
mvn test -f server/mall/pom.xml -pl mall-product -DfailIfNoTests=false
```

---expected: ALL TESTS PASSED

---

## 阶段 6：Stock（四段库存 + 内部端点）

### Task 6.1: MallSkuStockDO

- [ ] **Step 1: 创建 DO**

```java
package com.mall.product.DO;

import com.baomidou.mybatisplus.annotation.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

@TableName("mall_product_sku_stock")
public class MallSkuStockDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("sku_id")
    private Long skuId;

    @TableField("total_stock")
    private Integer totalStock;

    @TableField("available_stock")
    private Integer availableStock;

    @TableField("locked_stock")
    private Integer lockedStock;

    @TableField("sold_stock")
    private Integer soldStock;

    @TableField("frozen_stock")
    private Integer frozenStock;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @Version
    @TableField("version")
    private Integer version;

    // --- 完整 getter/setter + toString ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public Integer getTotalStock() { return totalStock; }
    public void setTotalStock(Integer totalStock) { this.totalStock = totalStock; }
    public Integer getAvailableStock() { return availableStock; }
    public void setAvailableStock(Integer availableStock) { this.availableStock = availableStock; }
    public Integer getLockedStock() { return lockedStock; }
    public void setLockedStock(Integer lockedStock) { this.lockedStock = lockedStock; }
    public Integer getSoldStock() { return soldStock; }
    public void setSoldStock(Integer soldStock) { this.soldStock = soldStock; }
    public Integer getFrozenStock() { return frozenStock; }
    public void setFrozenStock(Integer frozenStock) { this.frozenStock = frozenStock; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add server/mall/mall-product/src/main/java/com/mall/product/DO/MallSkuStockDO.java
git commit -m "feat(mall-product): add MallSkuStockDO (4-segment stock)"
```

### Task 6.2: MallSkuStockMapper（乐观锁 SQL）

- [ ] **Step 1: 创建 Mapper**

```java
package com.mall.product.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.product.DO.MallSkuStockDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MallSkuStockMapper extends BaseMapper<MallSkuStockDO> {

    @Update("UPDATE mall_product_sku_stock " +
            "SET available_stock = available_stock - #{qty}, " +
            "locked_stock = locked_stock + #{qty}, " +
            "version = version + 1 " +
            "WHERE sku_id = #{skuId} " +
            "AND version = #{version} " +
            "AND available_stock >= #{qty}")
    int reserveStock(@Param("skuId") Long skuId,
                     @Param("qty") Integer qty,
                     @Param("version") Integer version);

    @Update("UPDATE mall_product_sku_stock " +
            "SET available_stock = available_stock + #{qty}, " +
            "locked_stock = locked_stock - #{qty}, " +
            "version = version + 1 " +
            "WHERE sku_id = #{skuId} " +
            "AND version = #{version}")
    int releaseStock(@Param("skuId") Long skuId,
                     @Param("qty") Integer qty,
                     @Param("version") Integer version);

    @Update("UPDATE mall_product_sku_stock " +
            "SET available_stock = available_stock + #{qty}, " +
            "version = version + 1 " +
            "WHERE sku_id = #{skuId} " +
            "AND version = #{version}")
    int restock(@Param("skuId") Long skuId,
                @Param("qty") Integer qty,
                @Param("version") Integer version);

    default MallSkuStockDO selectBySkuId(Long skuId) {
        return selectOne(new LambdaQueryWrapper<MallSkuStockDO>()
                .eq(MallSkuStockDO::getSkuId, skuId)
                .eq(MallSkuStockDO::getIsDeleted, 0));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add server/mall/mall-product/src/main/java/com/mall/product/mapper/MallSkuStockMapper.java
git commit -m "feat(mall-product): add MallSkuStockMapper with optimistic lock SQL"
```

### Task 6.3: IStockService + StockServiceImpl（RED→GREEN）

> TDD 模式，与 Category 阶段一致。关键测试点：

1. `reserveStock(orderNo, items)` — 每个 item 乐观锁扣减，影响 0 行抛 `A0521`(库存不足)
2. `releaseStock(orderNo)` — 幂等去重（Redis `mall:product:stock:reserve:{orderNo}:{skuId}`，TTL 24h），释放库存
3. `restock(skuId, qty)` — 回补可用库存

**实现要点：**
```java
// reserveStock 核心逻辑
for (ReserveStockItemRequest item : items) {
    MallSkuStockDO stock = mallSkuStockMapper.selectBySkuId(item.getSkuId());
    if (stock == null) {
        throw new BusinessException(ErrorCode.A0501);
    }
    int affected = mallSkuStockMapper.reserveStock(item.getSkuId(), item.getQty(), stock.getVersion());
    if (affected == 0) {
        throw new BusinessException(ErrorCode.A0521);
    }
    redisTemplate.opsForValue().set(
        CacheConstants.Product.STOCK_RESERVE + orderNo + ":" + item.getSkuId(),
        item.getQty(), 86400, TimeUnit.SECONDS);
}

// releaseStock 核心逻辑（幂等）
for (ReserveStockItemRequest item : items) {
    String idempotentKey = CacheConstants.Product.STOCK_RESERVE + orderNo + ":" + item.getSkuId();
    Integer reservedQty = (Integer) redisTemplate.opsForValue().get(idempotentKey);
    if (reservedQty == null) {
        log.warn("Duplicate release for orderNo={}, skuId={}", orderNo, item.getSkuId());
        continue;
    }
    MallSkuStockDO stock = mallSkuStockMapper.selectBySkuId(item.getSkuId());
    mallSkuStockMapper.releaseStock(item.getSkuId(), reservedQty, stock.getVersion());
    redisTemplate.delete(idempotentKey);
}
```

### Task 6.4: RemoteProductInnerController（RED→GREEN）

> 内部端点，路由 `/inner/product`，返回裸类型（非 MallResult），由 `InnerSignatureFilter` 保护。

```java
@Slf4j
@RestController
@RequestMapping("/inner/product")
@RequiredArgsConstructor
public class RemoteProductInnerController {

    private final ISkuService skuService;
    private final IStockService stockService;

    @GetMapping("/skus")
    List<ProductSkuDTO> batchGetSku(@RequestParam("skuIds") List<Long> skuIds) {
        return skuService.batchGetSkuDTOs(skuIds);
    }

    @PostMapping("/stock/reserve")
    boolean reserveStock(@RequestParam("orderNo") String orderNo,
                         @RequestBody List<RemoteProductService.ReserveStockItemRequest> items) {
        return stockService.reserveStock(orderNo, items);
    }

    @PostMapping("/stock/release")
    void releaseStock(@RequestParam("orderNo") String orderNo) {
        stockService.releaseStock(orderNo);
    }

    @PostMapping("/stock/restock")
    void restock(@RequestParam("skuId") Long skuId,
                 @RequestParam("qty") Integer qty) {
        stockService.restock(skuId, qty);
    }

    @GetMapping("/spus/all")
    PageResult<SpuDTO> fetchAllSpus(@RequestParam("page") int page,
                                     @RequestParam("size") int size) {
        return spuService.pageForFullRebuild(page, size);
    }
}
```

- [ ] **Step X: 编译验证阶段 6**

```bash
mvn test -f server/mall/pom.xml -pl mall-product -DfailIfNoTests=false
```

---expected: ALL TESTS PASSED

---

## 阶段 7：搜索同步 + MQ

### Task 7.1: OutboxMessageDO + OutboxMessageMapper

- [ ] **Step 1: 创建 DO**

```java
package com.mall.product.DO;

import com.baomidou.mybatisplus.annotation.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

@TableName("mall_outbox_message")
public class OutboxMessageDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("biz_type")
    private String bizType;

    @TableField("biz_id")
    private String bizId;

    @TableField("payload")
    private String payload;

    @TableField("status")
    private Integer status;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    // --- 完整 getter/setter + toString ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public String getBizId() { return bizId; }
    public void setBizId(String bizId) { this.bizId = bizId; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
```

- [ ] **Step 2: 创建 Mapper**

```java
package com.mall.product.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.product.DO.OutboxMessageDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OutboxMessageMapper extends BaseMapper<OutboxMessageDO> {

    default List<OutboxMessageDO> selectPending(String bizType, int limit) {
        return selectList(new LambdaQueryWrapper<OutboxMessageDO>()
                .eq(OutboxMessageDO::getBizType, bizType)
                .eq(OutboxMessageDO::getStatus, 0)
                .last("LIMIT " + limit));
    }

    @Update("UPDATE mall_outbox_message SET status = #{status}, retry_count = retry_count + 1 WHERE id = #{id}")
    int updateStatus(Long id, Integer status);
}
```

- [ ] **Step 3: Commit**

```bash
git add server/mall/mall-product/src/main/java/com/mall/product/DO/OutboxMessageDO.java
git add server/mall/mall-product/src/main/java/com/mall/product/mapper/OutboxMessageMapper.java
git commit -m "feat(mall-product): add OutboxMessageDO + Mapper"
```

### Task 7.2: RemoteSearchAdapter

- [ ] **Step 1: 创建**

```java
package com.mall.product.infrastructure.feign;

import com.mall.api.feign.RemoteSearchService;
import com.mall.api.feign.RemoteSearchService.SearchSyncRequest;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteSearchAdapter {

    private final RemoteSearchService remoteSearchService;

    public void syncProduct(SearchSyncRequest request) {
        try {
            remoteSearchService.syncProduct(request);
        } catch (Exception e) {
            log.error("实时同步搜索索引失败, spuId={}, operation={}", request.getSpuId(), request.getOperation(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add server/mall/mall-product/src/main/java/com/mall/product/infrastructure/feign/RemoteSearchAdapter.java
git commit -m "feat(mall-product): add RemoteSearchAdapter"
```

### Task 7.3-7.5: SearchSyncProducer / OrderCancelledConsumer / SearchSyncScheduleTask

> 按 TDD 模式分别实现：
> - `SearchSyncProducer` — 实时调 Feign，捕获异常后写 Outbox 降级
> - `OrderCancelledConsumer` — `@RocketMQMessageListener`，幂等去重释放库存
> - `SearchSyncScheduleTask` — `@Scheduled` 定时扫描 Outbox 补偿投递

---

## 阶段 8：搜索降级兜底

### Task 8.1: SearchFallbackController

- [ ] **Step 1: 创建**

```java
package com.mall.product.controller;

import com.mall.common.DTO.MallResult;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.product.DO.MallProductSpuDO;
import com.mall.product.VO.SpuVO;
import com.mall.product.convert.response.SpuConvert;
import com.mall.product.mapper.MallProductSpuMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class SearchFallbackController {

    private final MallProductSpuMapper mallProductSpuMapper;

    @GetMapping("/search/fallback")
    public MallResult<List<SpuVO>> search(@RequestParam String keyword,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        if (keyword.length() < 2 || keyword.length() > 50) {
            throw new BusinessException(ErrorCode.A0802);
        }
        if (size > 100) {
            size = 100;
        }

        Page<MallProductSpuDO> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<MallProductSpuDO> wrapper = new LambdaQueryWrapper<MallProductSpuDO>()
                .eq(MallProductSpuDO::getPublishStatus, 1)
                .eq(MallProductSpuDO::getIsDeleted, 0)
                .like(MallProductSpuDO::getSpuName, keyword);

        Page<MallProductSpuDO> result = mallProductSpuMapper.selectPage(pageParam, wrapper);

        List<SpuVO> voList = result.getRecords().stream()
                .map(SpuConvert::toSpuVO)
                .toList();

        return MallResult.success(voList);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add server/mall/mall-product/src/main/java/com/mall/product/controller/SearchFallbackController.java
git commit -m "feat(mall-product): add SearchFallbackController (DB LIkE fallback)"
```

---

## 最终验证

- [ ] **全量编译 + 测试**

```bash
mvn clean install -f server/mall/pom.xml -DskipTests
mvn test -f server/mall/pom.xml -pl mall-product -DfailIfNoTests=false
```

---expected: BUILD SUCCESS + ALL TESTS PASSED

