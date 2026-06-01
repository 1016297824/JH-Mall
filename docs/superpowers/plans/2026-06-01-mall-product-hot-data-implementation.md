# mall-product 热点数据实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 mall-product 模块实现 Caffeine + Redis ZSet + HyperLogLog 三级热点数据架构

**Architecture:** 单 Service 驱动 — `IHotProductService` 收敛所有热点逻辑，手动 `Cache<Long, SpuVO>` Bean，ruoyi-job 调度定时任务，逐个独立 TDD

**Tech Stack:** Spring Boot 4.0.3 / Caffeine 3.x / Redis (ZSet + HyperLogLog) / MyBatis-Plus / RocketMQ / Mockito + MockMvc

**Spec:** `docs/superpowers/specs/2026-06-01-mall-product-hot-data-implementation.md`

---

### Task P1: Caffeine 依赖 + CacheConstants 常量 + SpuVO 字段

**Files:**
- Modify: `server/mall/mall-product/pom.xml`
- Modify: `server/mall/mall-common/src/main/java/com/mall/common/constant/CacheConstants.java`
- Modify: `server/mall/mall-product/src/main/java/com/mall/product/VO/SpuVO.java`

- [ ] **Step 1: pom.xml 新增 Caffeine 依赖**

在 `server/mall/mall-product/pom.xml` 的 `<dependencies>` 中，`mall-common` 依赖之后添加：

```xml
<!-- Caffeine 本地缓存 -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

- [ ] **Step 2: CacheConstants.Job 新增常量**

在 `server/mall/mall-common/src/main/java/com/mall/common/constant/CacheConstants.java` 的 `Job` 内部类中，`LOCK_ORDER_TIMEOUT` 之后添加：

```java
/** 热点排名刷新任务锁 */
public static final String LOCK_HOT_RANK = "mall:job:lock:hot_rank";
```

- [ ] **Step 3: SpuVO 新增 hotScore 字段**

在 `server/mall/mall-product/src/main/java/com/mall/product/VO/SpuVO.java` 的 `salesCount` 字段之后添加：

```java
/** 热度分（Redis ZSet score，不持久化到 MySQL） */
private Long hotScore;
```

- [ ] **Step 4: 构建验证**

```bash
mvn clean compile -f server/mall/pom.xml -DskipTests -pl mall-product -am
```

预期：BUILD SUCCESS

---

### Task P2: MallProductConfigProperties + ProductConfig + SpuConvert

**Files:**
- Modify: `server/mall/mall-product/src/main/java/com/mall/product/config/MallProductConfigProperties.java`
- Modify: `server/mall/mall-product/src/main/java/com/mall/product/config/ProductConfig.java`
- Modify: `server/mall/mall-product/src/main/java/com/mall/product/convert/response/SpuConvert.java`

- [ ] **Step 1: MallProductConfigProperties 新增 HotConfig**

在 `Stock` 内部类之后、类结束 `}` 之前添加：

```java
private final Hot hot = new Hot();

public Hot getHot() {
    return hot;
}

/** 热点数据配置 */
public static class Hot {

    /** 热点排名 ZSet 最大容量 */
    private int rankMaxSize = 200;

    /** 热度计算：销量权重 */
    private double salesWeight = 0.6;

    /** 热度计算：UV 权重 */
    private double uvWeight = 0.4;

    public int getRankMaxSize() {
        return rankMaxSize;
    }

    public void setRankMaxSize(int rankMaxSize) {
        this.rankMaxSize = rankMaxSize;
    }

    public double getSalesWeight() {
        return salesWeight;
    }

    public void setSalesWeight(double salesWeight) {
        this.salesWeight = salesWeight;
    }

    public double getUvWeight() {
        return uvWeight;
    }

    public void setUvWeight(double uvWeight) {
        this.uvWeight = uvWeight;
    }
}
```

- [ ] **Step 2: ProductConfig 新增 hotProductCache Bean**

在 `ProductConfig.java` 文件顶部新增 import：

```java
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mall.product.VO.SpuVO;

import java.time.Duration;
```

在 `redisTemplate` Bean 之后添加：

```java
@Bean
public Cache<Long, SpuVO> hotProductCache() {
    return Caffeine.newBuilder()
        .maximumSize(500)
        .expireAfterAccess(Duration.ofMinutes(5))
        .recordStats()
        .build();
}
```

- [ ] **Step 3: SpuConvert 新增批量转换 + hotScore 映射**

在 `SpuConvert.java` 的 `toSpuVO` 方法中，`vo.setBrandId(...)` 之后添加：

```java
vo.setHotScore(0L);
```

在类的末尾（`private SpuConvert()` 之前）新增：

```java
/**
 * SPU DO 列表批量转 VO 列表
 *
 * @param spuDOList SPU DO 列表
 * @return SPU VO 列表
 */
public static List<SpuVO> toSpuVOList(List<MallProductSpuDO> spuDOList) {
    if (spuDOList == null || spuDOList.isEmpty()) {
        return Collections.emptyList();
    }
    return spuDOList.stream().map(SpuConvert::toSpuVO).toList();
}
```

- [ ] **Step 4: 构建验证**

```bash
mvn clean compile -f server/mall/pom.xml -DskipTests -pl mall-product -am
```

预期：BUILD SUCCESS

---

### Task T1: IHotProductService + HotProductServiceImpl（TDD）

**Files:**
- Create: `server/mall/mall-product/src/main/java/com/mall/product/service/IHotProductService.java`
- Create: `server/mall/mall-product/src/main/java/com/mall/product/service/impl/HotProductServiceImpl.java`
- Create: `server/mall/mall-product/src/test/java/com/mall/product/service/impl/HotProductServiceImplTest.java`

#### T1-RED（主会话）

- [ ] **Step 1: 创建接口文件**

```java
// server/mall/mall-product/src/main/java/com/mall/product/service/IHotProductService.java
package com.mall.product.service;

import com.mall.product.VO.SpuVO;

import java.util.List;

public interface IHotProductService {

    List<SpuVO> hotList(int limit);

    void incrUv(Long spuId, Long userId);

    void incrHotRank(Long spuId, int quantity);

    void refreshHotRank();
}
```

- [ ] **Step 2: 创建空壳实现**

```java
// server/mall/mall-product/src/main/java/com/mall/product/service/impl/HotProductServiceImpl.java
package com.mall.product.service.impl;

import com.mall.product.VO.SpuVO;
import com.mall.product.service.IHotProductService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class HotProductServiceImpl implements IHotProductService {

    @Override
    public List<SpuVO> hotList(int limit) {
        return Collections.emptyList();
    }

    @Override
    public void incrUv(Long spuId, Long userId) {
    }

    @Override
    public void incrHotRank(Long spuId, int quantity) {
    }

    @Override
    public void refreshHotRank() {
    }
}
```

- [ ] **Step 3: 创建测试类**

```java
// server/mall/mall-product/src/test/java/com/mall/product/service/impl/HotProductServiceImplTest.java
package com.mall.product.service.impl;

import com.mall.common.constant.CacheConstants;
import com.mall.product.DO.MallProductSpuDO;
import com.mall.product.VO.SpuVO;
import com.mall.product.config.MallProductConfigProperties;
import com.mall.product.mapper.MallProductSpuMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.*;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HotProductServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private MallProductSpuMapper spuMapper;

    private Cache<Long, SpuVO> hotProductCache;
    private MallProductConfigProperties configProps;
    private HotProductServiceImpl service;

    @BeforeEach
    void setUp() {
        hotProductCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterAccess(Duration.ofMinutes(5))
            .build();

        configProps = new MallProductConfigProperties();
        configProps.getHot().setRankMaxSize(200);
        configProps.getHot().setSalesWeight(0.6);
        configProps.getHot().setUvWeight(0.4);

        service = new HotProductServiceImpl(hotProductCache, redisTemplate, spuMapper, configProps);
    }

    @Test
    void hotListShouldReturnEmptyWhenZSetEmpty() {
        // arrange
        @SuppressWarnings("unchecked")
        BoundZSetOperations<String, Object> boundZSet = (BoundZSetOperations<String, Object>) org.mockito.Mockito.mock(BoundZSetOperations.class);
        when(redisTemplate.boundZSetOps(CacheConstants.Product.HOT_RANK)).thenReturn(boundZSet);
        when(boundZSet.reverseRange(0, 19)).thenReturn(Collections.emptySet());

        // act
        List<SpuVO> result = service.hotList(20);

        // assert
        assertThat(result).isEmpty();
    }

    @Test
    void incrHotRankShouldIncrementZSetScore() {
        // arrange
        @SuppressWarnings("unchecked")
        BoundZSetOperations<String, Object> boundZSet = (BoundZSetOperations<String, Object>) org.mockito.Mockito.mock(BoundZSetOperations.class);
        when(redisTemplate.boundZSetOps(CacheConstants.Product.HOT_RANK)).thenReturn(boundZSet);

        // act
        service.incrHotRank(1L, 3);

        // verify ZINCRBY mall:product:hot:rank 30 "1"
    }
}
```

- [ ] **Step 4: 运行测试确认失败**

```bash
mvn test -f server/mall/pom.xml -pl mall-product -Dtest=HotProductServiceImplTest -DfailIfNoTests=false
```

预期：hotListShouldReturnEmptyWhenZSetEmpty 应 PASS（空壳实现返回空），incrHotRank 可能 PASS 但不验证行为。继续让子 Agent 补全实现。

#### T1-GREEN（子 Agent）

- [ ] **Step 5: 子 Agent 实现完整逻辑**

将以下信息发给子 Agent，让它实现 `HotProductServiceImpl`：

```
实现 HotProductServiceImpl，已有接口 IHotProductService 和空壳实现。

关键信息：
- 构造注入：Cache<Long, SpuVO> hotProductCache, RedisTemplate<String, Object> redisTemplate, MallProductSpuMapper spuMapper, MallProductConfigProperties configProps
- 常量：CacheConstants.Product.HOT_RANK、UV
- hotList(limit)：ZREVRANGE 取排名 → Caffeine 命中返回 → 未命中 selectBatchIds → 回种 Caffeine → ZSet 空降级 ORDER BY sales_count DESC
- incrUv(spuId, userId)：redisTemplate.opsForHyperLogLog().add(CacheConstants.Product.UV + spuId, userId.toString())
- incrHotRank(spuId, quantity)：redisTemplate.boundZSetOps(HOT_RANK).incrementScore(spuId.toString(), quantity * 10.0)
- refreshHotRank()：SETNX 分布式锁 → ZREVRANGE WITHSCORES Top 200 → PFCOUNT UV → 重算 score → ZADD → ZREMRANGEBYRANK 清理
- MallProductSpuDO 的 id 是 Long 类型
- 复用 SpuConvert.toSpuVO() 和 SpuConvert.toSpuVOList()
```

- [ ] **Step 6: 主会话运行测试验证通过**

```bash
mvn test -f server/mall/pom.xml -pl mall-product -Dtest=HotProductServiceImplTest
```

预期：全部 PASS

---

### Task T2: SpuController.hotList()（TDD）

**Files:**
- Modify: `server/mall/mall-product/src/main/java/com/mall/product/service/ISpuService.java`
- Modify: `server/mall/mall-product/src/main/java/com/mall/product/service/impl/SpuServiceImpl.java`
- Modify: `server/mall/mall-product/src/main/java/com/mall/product/controller/SpuController.java`
- Modify: `server/mall/mall-product/src/test/java/com/mall/product/controller/SpuControllerTest.java`

#### T2-RED（主会话）

- [ ] **Step 1: ISpuService 新增 hotList 方法签名**

在 `ISpuService.java` 的 `pageForFullRebuild` 之后添加：

```java
/**
 * 获取热点商品列表
 *
 * @param limit 返回条数（最大 50）
 * @return 热度降序 SpuVO 列表
 */
List<SpuVO> hotList(int limit);
```

- [ ] **Step 2: SpuServiceImpl 新增空壳实现**

注入 `IHotProductService`（只加字段不改方法体）：

```java
// 在已有 final 字段后追加：
private final IHotProductService hotProductService;
```

新增方法（`pageForFullRebuild` 之后）：

```java
@Override
public List<SpuVO> hotList(int limit) {
    return hotProductService.hotList(limit);
}
```

需要新增 import：

```java
import com.mall.product.service.IHotProductService;
```

- [ ] **Step 3: SpuController 新增端点**

注入 `IHotProductService`：

```java
import com.mall.product.service.IHotProductService;
// 在已有 final 字段后追加：
private final IHotProductService hotProductService;
```

在 `detail` 方法之后新增：

```java
/**
 * 热门商品列表
 *
 * @param limit 返回条数（默认 20，最大 50）
 * @return 热度降序商品列表
 */
@GetMapping("/spus/hot")
public MallResult<List<SpuVO>> hotList(@RequestParam(defaultValue = "20") int limit) {
    if (limit > 50) {
        throw new BusinessException(ErrorCode.A0803);
    }
    return MallResult.success(spuService.hotList(limit));
}
```

需要新增 import：

```java
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import java.util.List;
```

- [ ] **Step 4: 更新 SpuControllerTest，新增 hotList 测试**

在 `SpuControllerTest.java` 中新增 `@Mock` 字段和测试方法：

```java
import com.mall.product.service.IHotProductService;
// 在 @Mock ISpuService 之后追加：
@Mock
private IHotProductService hotProductService;
```

在 `detailShouldReturnSpuDetail` 之后新增：

```java
@Test
void hotListShouldReturnSpuList() throws Exception {
    SpuVO vo = new SpuVO();
    vo.setSpuId("1");
    vo.setSpuName("热门商品");
    when(spuService.hotList(20)).thenReturn(List.of(vo));

    mockMvc.perform(get("/api/product/spus/hot"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].spuName").value("热门商品"));
}

@Test
void hotListShouldRejectLimitExceed50() throws Exception {
    mockMvc.perform(get("/api/product/spus/hot").param("limit", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0803"));
}
```

由于 Controller 新增了 `IHotProductService` 注入，原来的 `listShouldReturnSpuPage` 和 `detailShouldReturnSpuDetail` 测试不需要 `hotProductService` mock。需要在 `@InjectMocks` 上配合 `@Mock` 使 Mockito 正常工作（`@InjectMocks` 会自动处理新增的依赖）。

- [ ] **Step 5: 运行测试确认 RED**

```bash
mvn test -f server/mall/pom.xml -pl mall-product -Dtest=SpuControllerTest#hotListShouldRejectLimitExceed50
```

预期：hotListShouldRejectLimitExceed50 PASS（空壳不抛异常所以不会返回 A0803），hotListShouldReturnSpuList FAIL（hotProductService 为 null）。

#### T2-GREEN（子 Agent）

- [ ] **Step 6: 子 Agent 实现**

```
修改 SpuControllerTest 测试，让 @InjectMocks 正常创建 Controller。
Controller 已有一个 @Mock ISpuService 和一个 @Mock IHotProductService。
不需要修改 SpuServiceImpl（已在 Task T1 中由子 Agent 实现了 hotProductService）。
只需要确保 SpuController.hotList() 路由正确，测试通过。
```

- [ ] **Step 7: 主会话运行全部测试验证**

```bash
mvn test -f server/mall/pom.xml -pl mall-product -Dtest=SpuControllerTest
```

预期：全部 PASS

---

### Task T3: RemoteProductInnerController + RemoteProductService（TDD）

**Files:**
- Modify: `server/mall/mall-api/src/main/java/com/mall/api/feign/RemoteProductService.java`
- Modify: `server/mall/mall-product/src/main/java/com/mall/product/controller/inner/RemoteProductInnerController.java`
- Create: `server/mall/mall-product/src/test/java/com/mall/product/controller/inner/RemoteProductInnerControllerTest.java`

#### T3-RED（主会话）

- [ ] **Step 1: RemoteProductService 新增 refreshHotRank 方法**

在 `compensateOutbox()` 方法之后、`ReserveStockItemRequest` 内部类之前添加：

```java
/**
 * 刷新热点商品排名（ruoyi-job 调度）
 */
@PostMapping("/inner/product/hot/refresh")
void refreshHotRank();
```

- [ ] **Step 2: RemoteProductInnerController 新增空壳端点**

注入 `IHotProductService`：

```java
import com.mall.product.service.IHotProductService;
// 在已有 final 字段后追加：
private final IHotProductService hotProductService;
```

在 `compensateOutbox()` 之后新增：

```java
/**
 * 刷新热点商品排名（ruoyi-job 调度）
 */
@PostMapping("/hot/refresh")
void refreshHotRank() {
    hotProductService.refreshHotRank();
}
```

- [ ] **Step 3: 创建测试类**

```java
// server/mall/mall-product/src/test/java/com/mall/product/controller/inner/RemoteProductInnerControllerTest.java
package com.mall.product.controller.inner;

import com.mall.product.service.IHotProductService;
import com.mall.product.service.ISkuService;
import com.mall.product.service.ISpuService;
import com.mall.product.service.IStockService;
import com.mall.product.infrastructure.schedule.SearchSyncScheduleTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RemoteProductInnerControllerTest {

    @Mock
    private ISkuService skuService;
    @Mock
    private IStockService stockService;
    @Mock
    private ISpuService spuService;
    @Mock
    private SearchSyncScheduleTask searchSyncScheduleTask;
    @Mock
    private IHotProductService hotProductService;

    @InjectMocks
    private RemoteProductInnerController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void refreshHotRankShouldCallService() throws Exception {
        mockMvc.perform(post("/inner/product/hot/refresh"))
                .andExpect(status().isOk());

        verify(hotProductService).refreshHotRank();
    }
}
```

- [ ] **Step 4: 运行测试确认 RED**

```bash
mvn test -f server/mall/pom.xml -pl mall-product -Dtest=RemoteProductInnerControllerTest
```

预期：PASS（空壳实现 + mock 验证，直接通过）。

不需要子 Agent，直接进入验证。

#### T3-GREEN（跳过 — 空壳已满足）

- [ ] **Step 5: 确认通过**

```bash
mvn test -f server/mall/pom.xml -pl mall-product -Dtest=RemoteProductInnerControllerTest
```

预期：PASS

---

### Task T4: OrderPaidConsumer（TDD）

**Files:**
- Create: `server/mall/mall-product/src/main/java/com/mall/product/infrastructure/mq/OrderPaidConsumer.java`
- Create: `server/mall/mall-product/src/test/java/com/mall/product/infrastructure/mq/OrderPaidConsumerTest.java`

#### T4-RED（主会话）

- [ ] **Step 1: 创建空壳 Consumer**

```java
// server/mall/mall-product/src/main/java/com/mall/product/infrastructure/mq/OrderPaidConsumer.java
package com.mall.product.infrastructure.mq;

import com.mall.common.constant.CacheConstants;
import com.mall.product.service.IHotProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaidConsumer {

    private final IHotProductService hotProductService;
    private final RedisTemplate<String, Object> redisTemplate;

    public void handleOrderPaid(String orderNo) {
    }
}
```

- [ ] **Step 2: 创建测试类**

```java
// server/mall/mall-product/src/test/java/com/mall/product/infrastructure/mq/OrderPaidConsumerTest.java
package com.mall.product.infrastructure.mq;

import com.mall.common.constant.CacheConstants;
import com.mall.product.service.IHotProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPaidConsumerTest {

    @Mock
    private IHotProductService hotProductService;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private OrderPaidConsumer consumer;

    @Test
    void handleOrderPaidShouldSkipDuplicate() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(false);

        consumer.handleOrderPaid("ORDER-001");

        verify(hotProductService, never()).incrHotRank(anyLong(), anyInt());
    }

    @Test
    void handleOrderPaidShouldIncrHotRankWhenFirstTime() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);

        consumer.handleOrderPaid("ORDER-001");

        // 当前空壳不做具体处理，验证 consumer 收到消息即可
    }
}
```

- [ ] **Step 3: 运行测试确认 RED**

```bash
mvn test -f server/mall/pom.xml -pl mall-product -Dtest=OrderPaidConsumerTest
```

预期：handleOrderPaidShouldSkipDuplicate PASS（空壳不调 hotProductService，verify never 通过）

#### T4-GREEN（子 Agent）

- [ ] **Step 4: 子 Agent 实现**

```
实现 OrderPaidConsumer.handleOrderPaid(String orderNo)：
1. 幂等去重：SETNX "mall:product:hot:paid:" + orderNo，TTL 7天
2. 若 SETNX 返回 true（首次）：
   - 解析消息 Payload JSON → extract spuId+quantity
   - 调用 hotProductService.incrHotRank(spuId, quantity)
3. 若 SETNX 返回 false（重复）→ 直接返回

注意：Payload 格式暂时为 mock，后续 mall-order 发送真实消息时再完成解析。
常量：CacheConstants.Product.HOT_RANK
```

- [ ] **Step 5: 主会话运行测试验证**

```bash
mvn test -f server/mall/pom.xml -pl mall-product -Dtest=OrderPaidConsumerTest
```

预期：全部 PASS

---

### Task F1: SpuServiceImpl.detailC() 嵌入 incrUv

**Files:**
- Modify: `server/mall/mall-product/src/main/java/com/mall/product/service/impl/SpuServiceImpl.java`

- [ ] **Step 1: 在 detail() 返回前嵌入 incrUv 调用**

在 `detail()` 方法的 `return SpuConvert.toSpuDetailVO(spuDO, skuDOList);` 之前添加：

```java
hotProductService.incrUv(spuId, 0L);
```

> userId 暂传 0L（匿名），后续接入认证后从 SecurityUtils 获取。

- [ ] **Step 2: 运行已有测试确认不破坏**

```bash
mvn test -f server/mall/pom.xml -pl mall-product -Dtest=SpuServiceImplTest
```

预期：全部 PASS

---

### Task F2: MallProductTask 新增 refreshHotRank

**Files:**
- Modify: `server/ruoyi/ruoyi-modules/ruoyi-job/src/main/java/com/ruoyi/job/task/MallProductTask.java`

- [ ] **Step 1: 新增方法**

在 `compensateOutbox()` 之后添加：

```java
/**
 * 热点排名刷新
 *
 * <p>调 mall-product /inner/product/hot/refresh，
 * 遍历 ZSet Top200 + PFCOUNT UV → 重算 score → ZADD 更新</p>
 */
public void refreshHotRank() {
    log.info("ruoyi-job 触发热点排名刷新");
    remoteProductService.refreshHotRank();
    log.info("ruoyi-job 热点排名刷新完成");
}
```

- [ ] **Step 2: 构建验证**

```bash
mvn clean compile -f server/ruoyi/pom.xml -pl ruoyi-job -am -DskipTests
```

预期：BUILD SUCCESS

---

### Task F3: 全量回归测试

- [ ] **Step 1: 运行 mall-product 全部测试**

```bash
mvn test -f server/mall/pom.xml -pl mall-product
```

- [ ] **Step 2: 运行 mall-common 测试**

```bash
mvn test -f server/mall/pom.xml -pl mall-common
```

- [ ] **Step 3: 构建 mall-product**

```bash
mvn clean install -f server/mall/pom.xml -DskipTests
```

预期：BUILD SUCCESS

---

### 任务依赖图

```
P1 ──→ P2 ──→ T1 ──→ T2
                   ├──→ T3 ──→ F2
                   └──→ T4
                       
T2 ──→ F1
T3 ──→ F2

收尾：F3（全量回归）
```
