# mall-product 热点数据实现 Spec

> 基于设计文档 `10_mall-product详细设计.md` §8 和 `03_06_系统详细设计-缓存与一致性设计.md` §6.1.3。
> 实现日期：2026-06-01

---

## 1 方案选择

**方案 A：单 Service 驱动** — `IHotProductService` 收敛所有热点逻辑。

- 手动 `Cache<Long, SpuVO>` Bean（不用 Spring Cache 注解）
- ruoyi-job 调度定时任务（复用 MallProductTask 模式）
- 逐个独立 TDD：主会话 RED → 子 Agent GREEN → 主会话验证

---

## 2 文件变更清单

### 2.1 修改（14 个文件）

| 文件 | 位置 | 改动 |
|------|------|------|
| `pom.xml` | `server/mall/mall-product/` | 新增 Caffeine 依赖 |
| `ProductConfig.java` | `mall-product/config/` | 新增 `hotProductCache` Bean |
| `MallProductConfigProperties.java` | `mall-product/config/` | 新增 `HotConfig` 内部类 |
| `ISpuService.java` | `mall-product/service/` | 新增 `hotList(int)` 方法 |
| `SpuServiceImpl.java` | `mall-product/service/impl/` | `hotList()` 委托 + `detailC()` 嵌入 incrUv |
| `SpuController.java` | `mall-product/controller/` | 新增 `GET /api/product/spus/hot` |
| `SpuVO.java` | `mall-product/VO/` | 新增 `hotScore` 字段 |
| `SpuConvert.java` | `mall-product/convert/response/` | 新增批量转换 + hotScore 映射 |
| `CacheConstants.java` | `mall-common/constant/` | `Job` 新增 `LOCK_HOT_RANK` |
| `RemoteProductService.java` | `mall-api/feign/` | 新增 `refreshHotRank()` |
| `RemoteProductInnerController.java` | `mall-product/controller/inner/` | 新增 `/hot/refresh` |
| `MallProductTask.java` | `ruoyi-job/task/` | 新增 `refreshHotRank()` |

### 2.2 新增（3 个文件）

| 文件 | 位置 | 说明 |
|------|------|------|
| `IHotProductService.java` | `mall-product/service/` | 热点数据接口 |
| `HotProductServiceImpl.java` | `mall-product/service/impl/` | Caffeine+Redis ZSet+HyperLogLog 实现 |
| `OrderPaidConsumer.java` | `mall-product/infrastructure/mq/` | 消费 `mall:order:paid` |

---

## 3 Caffeine 缓存配置

### 3.1 依赖

```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

版本由 Spring Boot Parent 管理。

### 3.2 Cache Bean

在 `ProductConfig.java` 中新增：

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

| 参数 | 值 | 说明 |
|------|:--:|------|
| maximumSize | 500 | 热点商品数上限 |
| expireAfterAccess | 5 分钟 | 持续被访问则不过期 |
| recordStats | 开启 | Actuator 可监控命中率 |

---

## 4 IHotProductService 接口设计

> **前置条件**：启动类需加 `@EnableAsync` 支持 `incrUv` 异步执行。
> 错误码 `A0803` 需在 `mall-common/enums/ErrorCode.java` 中存在。

```java
public interface IHotProductService {

    /** 热点商品列表。Caffeine → Redis ZSet → MySQL 三级降级 */
    List<SpuVO> hotList(int limit);

    /** 记录详情页 UV（异步），Redis HyperLogLog 去重 */
    @Async
    void incrUv(Long spuId, Long userId);

    /** 支付成功更新销量排名：ZINCRBY mall:product:hot:rank {qty*10} {spuId} */
    void incrHotRank(Long spuId, int quantity);

    /** 定时任务：遍历 ZSet Top200 + PFCOUNT UV → 重算 score → ZADD 更新 */
    void refreshHotRank();
}
```

### 4.1 hotList(limit) 流程

```
ZREVRANGE mall:product:hot:rank 0 {limit-1}
  → 命中 Caffeine → 直接返回
  → 未命中 → selectBatchIds(missedIds) → 回种 Caffeine → 返回
  → ZSet 空（冷启动）→ ORDER BY sales_count DESC LIMIT {limit}
```

### 4.2 refreshHotRank() 流程

```
SETNX mall:job:lock:hot_rank TTL 600s
ZREVRANGE WITHSCORES Top 200
遍历：PFCOUNT mall:product:uv:{spuId}
score = ZSCORE * salesWeight(0.6) + uv * uvWeight(0.4)
ZADD + ZREMRANGEBYRANK 0 -201 清理冷数据
释放锁
```

### 4.3 依赖注入

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class HotProductServiceImpl implements IHotProductService {
    private final Cache<Long, SpuVO> hotProductCache;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MallProductSpuMapper spuMapper;
    private final MallProductConfigProperties configProps;
}
```

---

## 5 Controller 扩展

### 5.1 SpuController

```java
@GetMapping("/spus/hot")
public MallResult<List<SpuVO>> hotList(@RequestParam(defaultValue = "20") int limit) {
    if (limit > 50) {
        throw new BusinessException(ErrorCode.A0803);
    }
    return MallResult.success(hotProductService.hotList(limit));
}
```

### 5.2 SpuServiceImpl.detailC() 嵌入

```java
// 返回前：
hotProductService.incrUv(spuId, userId);
```

匿名用户传 0，HyperLogLog 去重无影响。

### 5.3 RemoteProductInnerController 新增

```java
@PostMapping("/hot/refresh")
public void refreshHotRank() {
    hotProductService.refreshHotRank();
}
```

---

## 6 MQ Consumer — OrderPaidConsumer

> **前置依赖**：mall-order 在 `mall:order:paid` 消息 Payload 中需附带 `items: [{spuId, quantity}]`。
> 若当前 Payload 不含此字段，则改为在 Consumer 中通过订单项查询（需 Feign 调 mall-order）。

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaidConsumer {
    private final IHotProductService hotProductService;
    private final RedisTemplate<String, Object> redisTemplate;

    // 消费 mall:order:paid，Payload items 含 [{spuId, quantity}]
    // 幂等去重：SETNX mall:product:hot:paid:{orderNo} TTL 7天
    // 遍历 items → hotProductService.incrHotRank(spuId, quantity)
}
```

---

## 7 定时任务（ruoyi-job）

`MallProductTask.java` 新增：

```java
public void refreshHotRank() {
    remoteProductService.refreshHotRank();
}
```

---

## 8 配置属性

`MallProductConfigProperties` 新增：

```java
private HotConfig hot = new HotConfig();

@Data
public static class HotConfig {
    private int rankMaxSize = 200;
    private double salesWeight = 0.6;
    private double uvWeight = 0.4;
}
```

对应 Nacos `mall-product-dev.yml`：

```yaml
mall:
  product:
    hot:
      rank-max-size: 200
      sales-weight: 0.6
      uv-weight: 0.4
```

---

## 9 常量补充

`CacheConstants.Job` 新增：

```java
public static final String LOCK_HOT_RANK = "mall:job:lock:hot_rank";
```

## 10 VO 扩展

`SpuVO.java` 新增字段：

```java
/** 热度分（Redis ZSet score，不持久化到 MySQL） */
private Long hotScore;
```

---

## 11 TDD 实施顺序

### 先行准备（无需 TDD）

| 顺序 | 文件 | 操作 |
|:---:|------|------|
| P1 | `pom.xml` | Caffeine 依赖 |
| P2 | `CacheConstants.java` | `LOCK_HOT_RANK` |
| P3 | `MallProductConfigProperties.java` | `HotConfig` |
| P4 | `ProductConfig.java` | `hotProductCache` Bean |
| P5 | `SpuVO.java` | `hotScore` 字段 |
| P6 | `SpuConvert.java` | 批量转换方法 |

### TDD 循环（逐个 RED→GREEN→验证）

| 轮次 | 类 | 依赖 |
|:---:|------|------|
| T1 | `IHotProductService` + `HotProductServiceImpl` | P1-P6 |
| T2 | `SpuController.hotList()` | T1 |
| T3 | `RemoteProductInnerController` | T1 |
| T4 | `OrderPaidConsumer` | T1 |

### 收尾（无需 TDD）

| 顺序 | 文件 | 操作 |
|:---:|------|------|
| F1 | `SpuServiceImpl.detailC()` | 嵌入 incrUv |
| F2 | `RemoteProductService.java` | 新增方法 |
| F3 | `MallProductTask.java` | 新增方法 |

---

## 12 错误码

| 错误码 | userTip | 说明 |
|--------|---------|------|
| A0803 | 热门商品查询参数无效 | limit > 50 |

---

## 13 不做的事

- **不修改数据库**：hotScore 仅为 VO 层计算字段，热度数据全部存 Redis + Caffeine
- **不新增 VO**：复用已有 `SpuVO`
- **不引入 Spring Cache 注解**：手动 Cache Bean 精确控制
- **不新增 Feign 客户端**：复用已有 `RemoteProductService`
