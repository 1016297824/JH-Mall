# mall-product 异步链路接线修复

**日期**：2026-10-03
**范围**：`mall-product` 模块内两处「代码已写好但从未接线」的异步链路
**触发**：项目完成度盘点时发现两处 `TODO` 注释，逐条核实后确认为真实缺陷

---

## 一、修复 1：订单取消后库存永不回补 🔴

### 问题

`mall-order` 的 `OrderServiceImpl.cancelOrder()` 只做两件事：

```java
public void cancelOrder(Long userId, String orderNo) {
    MallOrderDO order = requireOwnedOrder(userId, orderNo);
    transitionOrder(order, OrderEventEnum.USER_CANCEL, MqTopicConstants.Order.CANCELLED, "OrderCancelled");
}
```

**没有释放库存**。而全模块唯一调用 `releaseStock` 的地方是 `compensate()`，
它只在**下单流程失败回滚**时触发（`createOrder` 的 3 个 catch 分支）。

因此：**用户主动取消订单 / 订单超时关闭后，Redis 预扣记录与 DB 库存占用永不回补。**

> 对照：`mall-marketing` 的同类消费者（`OrderCancelledConsumer`）是接好的，券会正常释放。
> 只有 `mall-product` 这一侧漏了接线 —— 消费端类存在，但没有
> `@RocketMQMessageListener`，`handleOrderCancelled` 方法体只有一行日志。

### 修复

| 文件 | 改动 |
|---|---|
| `mall-product/pom.xml` | 补 `rocketmq-spring-boot-starter:2.3.5`（原无此依赖） |
| `infrastructure/mq/MqDedupGuard.java` | **新增**（同 marketing / order 的独立副本） |
| `infrastructure/mq/OrderCancelledConsumer.java` | 实现 `RocketMQListener<MessageExt>` + `@RocketMQMessageListener`，解析报文 → 去重 → 调 `stockService.releaseStock(orderNo)` |

**关键语义**：

- **报文解析失败要抛异常**（`IllegalStateException`），让 RocketMQ 重投 —— 静默丢弃 = 库存永不回补。
- **缺少 `orderNo` 则丢弃并留痕** —— 属投递方缺陷，重投也不会好。
- **幂等两层**：`MqDedupGuard` 的 Redis 去重 + `releaseStock` 内部「预扣记录存在才释放、释放后删除记录」的天然幂等。

---

## 二、修复 2：搜索同步是「假补偿」🔴

### 问题

`SearchSyncProducer.syncProduct()` 的设计是：优先 Feign 实时同步 → 失败写 Outbox（状态 `NEW`）→ 由定时任务补偿。

但 `SearchSyncScheduleTask.execute()` **只把状态改成 `SENT`，从未真正投递**：

```java
// 修复前
for (OutboxMessageDO outbox : pendingList) {
    log.info("Compensate outbox: ...");
    outboxMessageMapper.updateStatus(outbox.getId(), "SENT");   // ← 只是改标记
}
```

后果：搜索引擎不可用时的商品变更，消息被标记为「已发送」而索引**实际从未更新** ——
**静默丢数据**，且留下的日志看起来像「补偿成功了」。

### 修复

| 文件 | 改动 |
|---|---|
| `infrastructure/mq/SearchSyncProducer.java` | 新增 `resync(spuId, operation)`：只投递、**不写 Outbox**，返回是否成功 |
| `infrastructure/schedule/SearchSyncScheduleTask.java` | 调用 `resync`，**成功才置 `SENT`**；失败保持 `NEW`；操作码非法置 `FAILED` |
| `controller/inner/RemoteProductInnerController.java` | `compensateOutbox` 的 Javadoc 同步（返回值语义改为「成功补偿数」） |

**为何 `resync` 不写 Outbox**：补偿任务本身在消费 Outbox 记录，
若失败时又写一条新记录，**表会随每轮重试无限增长**。正确做法是保留原记录、下轮重试。

**另外**：`aggregateId` 存 spuId、`eventType` 存操作码（与 `writeOutbox` 的写入方式一致），
故补偿时**无需解析 payload JSON**，直接读这两个字段。

---

## 三、验证

### 单元测试（TDD 三步）

| 阶段 | 结果 |
|---|---|
| RED | `OrderCancelledConsumerTest` 4 例中 3 例红；`SearchSyncProducerTest` 新增 2 例红；`SearchSyncScheduleTaskTest` 5 例中 4 例红（均编译通过） |
| GREEN | 三个测试类 **4 + 4 + 5 = 13 例全绿** |
| 回归 | **全工程 9 模块 602 例 0 失败**（mall-product 45 → **56**，其余模块数量不变） |

### 运行时验证（重新打包并启动）

```
Register the listener to container, listenerBeanName:orderCancelledConsumer
running container: DefaultRocketMQListenerContainer{
    consumerGroup='mall-product-order-cancelled-consumer',
    nameServer='127.0.0.1:9876',
    topic='mall:order:cancelled',
    messageModel=CLUSTERING }
Started MallProductApplication in 12.923 seconds
```

**证明**：消费者已真正注册并订阅 `mall:order:cancelled`（不只是编译通过）。

**顺带确认**：`mall-product-dev.yml` **无需**补 `rocketmq` 配置 ——
`name-server` 从全局 `application-dev.yml` 继承，纯消费者不需要 `producer.group`。
（这与 `mall-payment` 不同：那儿有 `RocketMQTemplate` 注入，缺 `producer.group` 会启动失败。）

### 验证边界（如实说明）

**未做真机 MQ 投递的端到端验证**（即「发一条 `mall:order:cancelled`，确认库存真的被释放」）。
原因：需要构造「有预扣记录的订单」+ 用 MQ 客户端发消息，成本较高。

当前可信度依据：消费者订阅已由运行时日志证实 + `onMessage` 的分支逻辑已由单元测试覆盖。
若需端到端，可后续在联调环境用真实取消订单流程验证。

---

## 四、遗留

- `mall-product` 的 `OutboxMessageMapper.selectPending` 与 `SearchSyncProducer.writeOutbox`
  仍用 `"NEW"` 字符串字面量（本次未改动，值同 `OutboxStatusEnum.NEW.getCode()`），
  后续可统一为枚举。
- `SearchSyncProducer.syncProduct` 的 Javadoc 写「操作类型（CREATE/UPDATE/DELETE）」，
  而 `SyncOperationEnum` 实际只有 `UPSERT` / `DELETE` —— 注释待更正。
