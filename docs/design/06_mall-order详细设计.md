# JH-Store mall-order 模块详细设计

> 基于系统详细设计 `03_系统详细设计.md` 展开。

---

## 1 模块概述

### 1.1 子领域

| 子领域 | 实体 | 说明 |
|--------|------|------|
| 购物车 | `mall_order_cart` | 用户临时存放待购商品，登录态持久化 |
| 订单 | `mall_order` + `mall_order_item` + `mall_order_amount` | 核心编排，含价格快照和状态机 |
| 售后 | `mall_order_after_sale` | 退款/退货申请与审核 |
| RocketMQ | 消费 `mall:payment:paid` 等、生产 `mall:order:created` 等 | 异步推进订单、发布事件 |

### 1.2 依赖关系

```
mall-order
  → mall-product (Feign)：锁定/释放库存、查询商品状态
  → mall-marketing (Feign)：锁定/释放优惠券、优惠试算
  → mall-payment (Feign)：查询支付单状态、发起退款
  → mall-user (Feign)：校验地址归属
  → mall-auth (Feign)：解密手机号
  → MySQL：自有表（见表系统设计第 1.3 节）
  → Outbox：生产 mall:order:created/paid/cancelled/delivered/completed/refunded
  → RocketMQ Consumer：消费 mall:payment:paid、mall:refund:succeeded
  → Redis：幂等键去重
```

---

## 2 包结构与接口映射

### 2.1 包结构

```
mall-order/src/main/java/com/jhstore/mall/order/
├─ controller/
│  ├─ admin/          → AdminOrderController, AdminAfterSaleController
│  └─ api/            → CartController, OrderController, AfterSaleController
├─ dto/               # 接口进出 DTO
│  ├─ request/        → CreateOrderRequest, AddCartRequest, SubmitAfterSaleRequest ...
│  └─ response/       → OrderDetailResponse, CartListResponse ...
├─ domain/            # 数据库实体（DO）
│  ├─ MallCart.java
│  ├─ MallOrder.java
│  ├─ MallOrderItem.java
│  ├─ MallOrderAmount.java
│  └─ MallAfterSale.java
├─ service/           # 业务逻辑
│  ├─ cart/           → CartService, CartServiceImpl
│  ├─ order/          → OrderService, OrderServiceImpl
│  └─ aftersale/      → AfterSaleService, AfterSaleServiceImpl
├─ mapper/            # MyBatis 数据访问
│  ├─ MallCartMapper.java
│  ├─ MallOrderMapper.java
│  └─ ...
├─ statemachine/      # 订单状态机（独立包）
│  ├─ OrderStatusEnum.java
│  ├─ OrderTransition.java
│  ├─ OrderStateMachine.java
│  └─ OrderEventEnum.java
├─ infrastructure/    # 外部适配
│  ├─ mq/             → OrderEventProducer, PaymentPaidConsumer, RefundSucceededConsumer
  │  ├─ feign/          → RemoteProductAdapter, RemoteMarketingAdapter, RemotePaymentAdapter, RemoteUserAdapter, RemoteAuthAdapter
│  └─ outbox/         → OutboxMessage.java, OutboxMapper.java, OutboxScheduler.java
├─ convert/           # 纯转换器（Entity↔DTO↔VO 字段映射）
│  ├─ CartConvert.java
│  ├─ OrderConvert.java
│  └─ AfterSaleConvert.java
└─ vo/                # 视图对象，前端展示
   ├─ CartVO.java
   ├─ OrderVO.java
   ├─ OrderItemVO.java
   └─ AfterSaleVO.java
```

### 2.2 接口映射

| # | 方法 | 路径 | Controller | 方法 | 权限码 |
|---|------|------|-----------|------|--------|
| 1 | GET | `/api/order/cart/items` | CartController | `listItems` | — |
| 2 | POST | `/api/order/cart/items` | CartController | `addItem` | — |
| 3 | PUT | `/api/order/cart/items/{id}` | CartController | `updateQuantity` | — |
| 4 | DELETE | `/api/order/cart/items/{id}` | CartController | `removeItem` | — |
| 5 | DELETE | `/api/order/cart/items` | CartController | `clearCart` | — |
| 6 | POST | `/api/order/orders` | OrderController | `createOrder` | — |
| 7 | GET | `/api/order/orders` | OrderController | `listOrders` | — |
| 8 | GET | `/api/order/orders/{id}` | OrderController | `getDetail` | — |
| 9 | POST | `/api/order/orders/{id}/cancellation` | OrderController | `cancelOrder` | — |
| 10 | PUT | `/api/order/orders/{id}/receipt` | OrderController | `confirmReceipt` | — |
| 11 | DELETE | `/api/order/orders/{id}` | OrderController | `deleteOrder` | — |
| 12 | POST | `/api/order/after_sales` | AfterSaleController | `submit` | — |
| 13 | GET | `/api/order/after_sales` | AfterSaleController | `list` | — |
| 14 | GET | `/api/order/after_sales/{id}` | AfterSaleController | `detail` | — |

管理端接口由若依代码生成器自动生成（CRUD + 发货/审核售后等），权限码无需手动维护。

---

## 3 核心类设计

### 3.1 OrderStateMachine

- 职责：状态转移控制，所有订单状态变更的唯一入口
- 核心依赖：`OrderStatusEnum`（9 个状态）、`OrderEventEnum`（12 个事件）
- `transition(order, event)`：
  - 查转移矩阵 → 无匹配抛出 `OrderStateException(A0702)`
  - 执行前置条件校验（金额、期限等）
  - 更新 `order.status` + `updateTime`
  - 返回后置动作列表供 Service 层执行
- 转移矩阵与系统详细设计第 4 章状态机表一致
- 禁止 Service 层直接 `update status`

### 3.2 OrderServiceImpl

- 职责：下单编排、状态推进、取消、确认收货
- 依赖：`OrderStateMachine`、`MallOrderMapper`、`MallOrderItemMapper`、`MallOrderAmountMapper`、`OutboxMapper`、`RemoteProductAdapter`、`RemoteMarketingAdapter`、`RemotePaymentAdapter`、`RemoteUserAdapter`、`Redis`（幂等键）
- `createOrder(req)`：幂等校验 → 参数校验 → 锁库存 → 锁优惠 → 创建订单+写 Outbox（同一事务）
- `payCallback(orderNo)`：调 `stateMachine.transition()`，WAIT_PAY → PAID
- `cancelOrder(orderNo)`：调 `stateMachine.transition()` → 写 `mall:order:cancelled` Outbox
- `confirmReceipt(orderNo)`：调 `stateMachine.transition()`，WAIT_RECEIVE → COMPLETED

### 3.3 CartServiceImpl

- 职责：购物车增删改查
- 依赖：`MallCartMapper`、`RemoteProductAdapter`、`CartConvert`
- `listCart()`：查购物车 → 批量 Feign 取 SKU 实时价和库存 → CartConvert 组装 CartVO
- `addItem()`：校验 SKU 存在+库存充足 → 已有则合并数量，否则 INSERT
- `updateQuantity()`：校验新数量 ≤ 库存 → UPDATE
- `removeItem()`：物理删除
- `clearCart()`：批量物理删除

### 3.4 AfterSaleServiceImpl

- 职责：售后申请与审核
- 依赖：`MallAfterSaleMapper`、`RemotePaymentAdapter`、`OrderStateMachine`、`AfterSaleConvert`
- `submit(req)`：校验订单状态 → 创建售后单（PENDING）
- `approve(afterSaleId)`：管理端审核通过 → 调 mall-payment 发起退款
- `reject(afterSaleId)`：管理端拒绝，通知用户
- `refundCallback()`：消费 `mall:refund:succeeded` → 推进售后状态

---

## 4 购物车设计

### 4.1 添加商品

1. 校验 `skuId` 存在、商品上架、库存 > 0：调 `RemoteProductService.batchGetSku([skuId])`，返回 `ProductSkuDTO`（含 `isOnSale`、`availableQty`）
2. 查购物车是否已有该 SKU：已有则合并数量（`quantity += 新增数量`），无则 INSERT
3. 合并后的数量不得超库存上限
4. 同一用户同一 SKU 唯一（`uk_user_sku` 唯一索引）

### 4.2 修改数量

1. 校验购物车项属于当前用户
2. 新数量 > 0 且 ≤ 该 SKU 库存：调 `RemoteProductService.batchGetSku([skuId])` 取 `availableQty`
3. UPDATE `quantity`

### 4.3 删除商品

1. 校验购物车项属于当前用户
2. 物理 DELETE

### 4.4 清空购物车

1. 批量 DELETE 当前用户所有购物车项

### 4.5 查看购物车

1. 查当前用户全部购物车项
2. 提取所有 `skuId`，调 `RemoteProductService.batchGetSku(skuIds)` 取实时价格 + 库存
3. 标注库存不足和已下架商品（前端灰显）
4. `batchGetSku` 超时或报错时，降级用购物车表 `price` 冗余字段展示

### 4.6 Redis 缓存层

购物车数据用 Redis Hash 加速，key `mall:order:cart:{userId}`，field=`skuId`，value=`quantity`。

**读路径：**
1. 先查 Redis Hash → 命中则只需补 Feign 拉最新价格
2. 未命中 → 查 MySQL → 回种 Redis（TTL 30 分钟）

**写路径：**
1. 先更新 MySQL（持久化）
2. 同步更新 Redis：增改 `HSET`，删 `HDEL`，清空 `DEL`
3. Redis 不可用时跳过，兜底走 MySQL

**约束：** MySQL 为购物车唯一事实来源，Redis 仅作缓存加速。

---

## 5 下单流程实现

### 5.1 流程总览

```
幂等校验 → 参数校验 → 锁库存 → 锁优惠 → 创建订单+Outbox → 返回
   │                     │         │
   │                     └─ 任一步失败：按 5.7 补偿规则回滚 ←
   └─ 命中幂等键：直接返回已有 orderNo
```

### 5.2 幂等校验

1. 从请求头取 `Idempotent-Key`（UUID v4，前端生成）
2. Redis `SETNX mall:order:idempotent:{userId}:{clientRequestNo} orderNo EX 1800`
3. 命中 → 直接返回已有 orderNo（200，相同 data）
4. 未命中 → 完成后续流程后，orderNo 回填 Redis value

### 5.3 参数校验

| 校验项 | 方式 | 失败返回 |
|--------|------|---------|
| 购物车非空 | 查 `mall_order_cart WHERE user_id=? AND selected=1` | A0710 |
| 地址存在且归属 | 调 `RemoteUserService.validateAddress(userId, addressId)` | A0501 |
| 商品上架+库存 | 调 `RemoteProductService.batchGetSku(skuIds)`，校验 `isOnSale` + `availableQty` | A0520 |
| 不超限购 | 校验购买数量 ≤ 单品限购上限 | A0711 |
| 优惠券可用 | 若传了 couponId，调 `RemoteMarketingService.validateCoupon(couponId, userId)` | A0610/A0612 |

### 5.4 锁定库存

1. 调 `RemoteProductService.reserveStock(orderNo, List<{skuId, qty}>)`
2. mall-product 本地事务：`UPDATE sku SET locked_qty+=#{qty}, available_qty-=#{qty} WHERE sku_id=? AND available_qty>=#{qty}`
3. 返回影响行数，行数 < 请求数 → 全部回滚，返回 A0521

### 5.5 锁定优惠券

1. 调 `RemoteMarketingService.lockCoupon(orderNo, couponId)`
2. mall-marketing 本地事务：`UPDATE user_coupon SET status='LOCKED' WHERE id=? AND status='AVAILABLE'`
3. 成功 → 继续
4. 失败 → 调 `RemoteProductService.releaseStock(orderNo)` 补偿释放库存，返回 A0612

### 5.6 创建订单

同一本地事务内完成：

1. **计算金额**：∑(SKU售价 × 数量) + 运费 − 优惠分摊
2. **INSERT mall_order**：`order_no`, `user_id`, `status='WAIT_PAY'`, `total_amount`, `discount_amount`, `freight_amount`, `pay_amount`, `pay_expire_time=NOW()+30min`
3. **批量 INSERT mall_order_item**：每条含 `sku_id`, `price_snapshot`（下单时价）, `quantity`, `total_price`
4. **INSERT mall_order_amount**：含 `total_amount`, `discount_amount`, `pay_amount`
5. **INSERT outbox**：`topic='mall:order:created'`, `payload={orderNo, userId, payAmount, payExpireTime}`, `status='NEW'`
6. 若使用优惠券，记录 `coupon_snapshot`

### 5.7 补偿规则

| 失败位置 | 已锁定资源 | 补偿操作 | 返回 |
|---------|-----------|---------|------|
| 库存锁定 | 无 | 无需补偿 | A0521 |
| 库存已锁、优惠锁定失败 | 库存 | 调 `RemoteProductService.releaseStock(orderNo)` | A0612 |
| 资源已锁、订单创建失败 | 库存+优惠券 | 同步调 `releaseStock()` + `RemoteMarketingService.releaseCoupon(orderNo)` | B0001 |
| 网络超时（未收到响应） | 可能已锁 | 客户端用同 Idempotent-Key 重试，幂等返回已有 orderNo | — |

### 5.8 事务边界

```
@Transactional
├─ RemoteProductService.reserveStock()     → mall-product 独立事务
├─ RemoteMarketingService.lockCoupon()     → mall-marketing 独立事务
└─ INSERT order + items + amount + outbox  ← 同一本地事务
```

Feign 调用失败不会自动回滚远程事务，需手动调 `releaseStock()` / `releaseCoupon()` 补偿。

### 5.9 超时关闭竞态处理

支付回调与超时扫描可能同时触发 WAIT_PAY 的状态转移：

| 触发方 | 事件 | 目标状态 |
|--------|------|---------|
| 支付回调 | PAY_SUCCESS | PAID |
| 超时扫描 | PAY_TIMEOUT | CLOSED |

**防护机制：** 超时关闭使用乐观锁：

```sql
UPDATE mall_order
SET status = 'CLOSED', close_time = NOW()
WHERE order_no = ? AND status = 'WAIT_PAY'
```

- 支付回调先到 → status 已变为 PAID → `WHERE status='WAIT_PAY'` 影响 0 行 → 超时关闭跳过
- 超时扫描先到 → status 已变为 CLOSED → 支付回调时状态机报 `A0702` → 回调记录日志后返回 200（不推进已关闭订单）

### 5.10 自动确认收货

发货后超 15 天用户未手动确认收货，定时任务自动完成：

1. ruoyi-job 每天扫描：`WHERE status='WAIT_RECEIVE' AND delivery_time < NOW() - INTERVAL 15 DAY`
2. 逐条调 `OrderServiceImpl.autoConfirmReceipt(orderNo)`：
   - `stateMachine.transition(order, CONFIRM_RECEIPT)` → COMPLETED
   - 写 Outbox `mall:order:completed`
3. 乐观锁防护：`WHERE status='WAIT_RECEIVE'`，已手动确认的订单影响 0 行则跳过
4. 配置项：`mall.order.auto-receive-days=15`（Nacos 下发，`@RefreshScope`）

---

## 6 订单状态机实现

### 6.1 状态枚举

| 枚举值 | 含义 | 终态 |
|--------|------|:---:|
| `WAIT_PAY` | 待支付，下单后初始状态 | 否 |
| `PAID` | 已支付，等待发货 | 否 |
| `WAIT_DELIVER` | 待发货，物流信息已填写但未揽收 | 否 |
| `WAIT_RECEIVE` | 待收货，快递已揽收 | 否 |
| `COMPLETED` | 已完成，用户确认收货 | 是 |
| `CANCELLED` | 已取消，用户或管理端主动取消 | 否（→CLOSED） |
| `CLOSED` | 已关闭，不可再操作 | 是 |
| `REFUNDING` | 退款中，售后已审核调用支付渠道 | 否 |
| `REFUNDED` | 已退款，渠道退款完成 | 否（→CLOSED） |

### 6.2 事件枚举

| 枚举值 | 含义 | 来源 |
|--------|------|:---:|
| `PAY_SUCCESS` | 支付平台回调支付成功 | 回调 |
| `USER_CANCEL` | 用户主动取消订单 | 用户 |
| `PAY_TIMEOUT` | 超时 30 分钟未支付，定时任务触发 | 系统 |
| `SELLER_DELIVER` | 管理端填写物流信息并确认发货 | 管理端 |
| `LOGISTICS_PICK` | 快递公司揽收回调 | 回调 |
| `CONFIRM_RECEIPT` | 用户点击确认收货 | 用户 |
| `FORCE_CANCEL` | 管理端强制取消（客服审核通过） | 管理端 |
| `REFUND_ONLY` | 发起仅退款售后（未发货） | 系统 |
| `RETURN_REFUND` | 发起退货退款售后（已发货） | 系统 |
| `AFTER_SALE` | 已完成后用户申请售后维权 | 系统 |
| `REFUND_SUCCESS` | 支付平台退款成功回调 | 回调 |
| `REFUND_FAIL` | 支付平台退款失败回调 | 回调 |

### 6.3 转移矩阵

| 当前状态 | 事件 | 目标状态 | 前置条件 | 后置动作 |
|---------|------|---------|---------|---------|
| WAIT_PAY | PAY_SUCCESS | PAID | 支付金额 ≥ 应付金额，订单未过期 | ①写 Outbox `mall:order:paid` ②通知商家 |
| WAIT_PAY | USER_CANCEL | CANCELLED | — | ①写 Outbox `mall:order:cancelled`（由 mall-product/mall-marketing 消费释放资源） |
| WAIT_PAY | PAY_TIMEOUT | CLOSED | `pay_expire_time < NOW()` | 同 USER_CANCEL |
| PAID | SELLER_DELIVER | WAIT_DELIVER | 物流单号 + 公司已填写 | ①写 Outbox `mall:order:delivered` ②记录物流信息 |
| WAIT_DELIVER | LOGISTICS_PICK | WAIT_RECEIVE | 快递已揽收 | ①更新物流状态 ②通知用户 |
| PAID | FORCE_CANCEL | CANCELLED | 客服审核通过 | ①调 mall-payment 原路退款 ②释放库存+优惠券 |
| PAID | REFUND_ONLY | REFUNDING | 售后审核通过，订单未发货 | ①调 mall-payment 创建退款单 |
| WAIT_RECEIVE | CONFIRM_RECEIPT | COMPLETED | — | ①写 Outbox `mall:order:completed` ②赠送积分 |
| WAIT_RECEIVE | RETURN_REFUND | REFUNDING | 售后审核通过 | 同 REFUND_ONLY |
| COMPLETED | AFTER_SALE | REFUNDING | 收货后 7 天内 | 同 REFUND_ONLY |
| REFUNDING | REFUND_SUCCESS | REFUNDED | 渠道退款完成 | ①写 Outbox `mall:order:refunded` ②退货退款则调 mall-product 回补库存 |
| REFUNDING | REFUND_FAIL | PAID | 渠道退款失败（原状态 PAID） | ①通知用户 ②标记人工介入 |
| REFUNDING | REFUND_FAIL | WAIT_DELIVER | 渠道退款失败（原状态 WAIT_DELIVER） | 同 PAID |
| REFUNDING | REFUND_FAIL | WAIT_RECEIVE | 渠道退款失败（原状态 WAIT_RECEIVE） | 同 PAID |
| REFUNDING | REFUND_FAIL | COMPLETED | 渠道退款失败（原状态 COMPLETED） | 同 PAID |
| CANCELLED | PAY_TIMEOUT | CLOSED | 取消后满 7 天 | 清理中间数据 |
| REFUNDED | PAY_TIMEOUT | CLOSED | 退款后满 7 天 | 清理中间数据 |

> 表中未定义的转移 → 抛 `OrderStateException(A0702)`。前置条件不满足 → 抛 `OrderStateException(A0703)`。

### 6.4 OrderStateMachine 类设计

**三个核心数据结构：**

- 转移矩阵：`Map<OrderStatusEnum, Map<OrderEventEnum, OrderStatusEnum>>` — 构造时静态初始化 6.3 表
- 前置条件表：`Map<OrderStatusEnum, Map<OrderEventEnum, Predicate<MallOrder>>>` — 校验金额、期限等
- 后置动作表：`Map<OrderStatusEnum, Map<OrderEventEnum, Consumer<MallOrder>>>` — 写 Outbox、通知等

**`transition(order, event)` 执行步骤：**

1. 从转移矩阵查 `target = TRANSITION[order.status][event]`，为 null → 抛 `A0702`
2. 执行前置条件 `PRECONDITIONS[order.status][event].test(order)`，false → 抛 `A0703`
3. 更新 `order.status = target`，`order.update_time = NOW()`
4. 执行后置动作 `POST_ACTIONS[order.status][event].accept(order)`（写 Outbox 等）
5. 返回目标状态

**3 个核心约束：**

- 状态机层不操作 DB、不持有 Mapper、不管理事务——只做状态逻辑判断
- 后置动作通过回调触发——实际的 Outbox 写入在 Service 层事务内完成
- 状态机是无状态单例 Bean，线程安全（Map 只读，`transition()` 不修改共享状态）

### 6.5 与 OrderServiceImpl 的集成

**调用模式：**

```
OrderServiceImpl.payCallback(orderNo):
  1. orderMapper.selectByOrderNo(orderNo)
  2. stateMachine.transition(order, PAY_SUCCESS)     // 状态机逻辑
  3. orderMapper.updateById(order)                    // 持久化状态变更
  4. outboxMapper.insert(OrderPaidEvent)              // 写 Outbox（同一事务）
```

**职责划分：**

| 层 | 负责 | 不负责 |
|----|------|--------|
| OrderStateMachine | 转移合法性、前置条件、后置动作定义 | DB 操作、事务、远程调用 |
| OrderServiceImpl | 事务边界、DB 读写、Outbox 写入、Feign 补偿 | 状态合法性判断 |

---

## 7 RocketMQ 消费

### 7.1 生产（Outbox 投递）

6 个 topic，由订单创建或状态转移时写 Outbox，定时任务投递：

| Topic | 触发场景 | 消费者 | 备注 |
|-------|---------|--------|------|
| `mall:order:created` | 下单成功 | 暂无 | 预留通知 |
| `mall:order:paid` | WAIT_PAY→PAID | 暂无 | 预留商家通知 |
| `mall:order:cancelled` | 取消/超时关闭 | mall-product、mall-marketing | 释放库存+优惠券 |
| `mall:order:delivered` | 商家发货 | 暂无 | 预留物流通知 |
| `mall:order:completed` | 确认收货 | 暂无 | 预留积分发放 |
| `mall:order:refunded` | 退款完成 | 暂无 | 预留通知 |

**Payload 字段规范：**

| Topic | Payload 字段 |
|-------|-------------|
| `mall:order:created` | `orderNo`, `userId`, `payAmount`, `payExpireTime` |
| `mall:order:paid` | `orderNo`, `userId`, `paidTime` |
| `mall:order:cancelled` | `orderNo`, `userId`, `cancelReason` (USER_CANCEL / PAY_TIMEOUT / FORCE_CANCEL) |
| `mall:order:delivered` | `orderNo`, `logisticsCompany`, `logisticsNo` |
| `mall:order:completed` | `orderNo`, `userId` |
| `mall:order:refunded` | `orderNo`, `userId`, `refundAmount` |

**Payload 约束：**

- 统一 JSON 格式，字段命名 lowerCamelCase
- 禁止序列化数据库实体（Domain/DO），必须为精简 DTO
- 避免将整个 MallOrder 对象放入 payload

### 7.2 重试策略

Outbox 投递失败后的指数退避：

| 参数 | 值 |
|------|-----|
| base | 10s |
| max_delay | 120s |
| random_jitter | 0~5s |
| retry_count 上限 | 3 次 |
| 死信队列 | `{topic}:dlq` |

| 重试次数 | 间隔 |
|:---:|------|
| 第 1 次 | 10s + jitter |
| 第 2 次 | 30s + jitter |
| 第 3 次 | 60s + jitter |
| 超过 3 次 | status=FAILED，进入死信，人工介入 |

### 7.3 消费

| Topic | 消费者类 | 处理流程 |
|-------|---------|---------|
| `mall:payment:paid` | `PaymentPaidConsumer` | ①幂等去重 ②调 `OrderServiceImpl.payCallback(orderNo)` ③状态机 WAIT_PAY→PAID ④Service 层写 Outbox `mall:order:paid` |
| `mall:refund:succeeded` | `RefundSucceededConsumer` | ①幂等去重 ②调 `AfterSaleServiceImpl.refundCallback()` ③如为退货退款，调 `RemoteProductService.restock(skuId, qty)` 回补库存 |

### 7.4 消费幂等

- Key：`mall:mq:dedup:{messageId}:mall-order`
- 操作：消费前 Redis SETNX，命中 → 直接 ACK 跳过
- TTL：24h（超过消息最长存活时间）
- 消费成功后不主动删除，待 TTL 自然过期

---

## 8 售后设计

### 8.1 售后类型

| 类型 | 条件 | 库存处理 |
|------|------|---------|
| 仅退款 | 未发货（WAIT_PAY / PAID） | 不回补库存 |
| 退货退款 | 已发货（WAIT_RECEIVE / COMPLETED） | 收到退货后调 `RemoteProductService.restock(skuId, qty)` 回补 |

### 8.2 申请流程

1. Controller 接收 `POST /api/order/after_sales`（orderId、reason、refundType、itemIds）
2. `AfterSaleServiceImpl.submit()`：
   - 调 `orderMapper.selectByOrderNo()` + `stateMachine.transition()` 校验订单状态可退款
   - 校验：7 天期限内、退款金额 ≤ 已付金额、同一 SKU 未重复
   - `afterSaleMapper.insert()` → 创建售后单，`status=PENDING`
   - 自动审核：仅退款 + 金额 ≤ 阈值 → 直接 `status=APPROVED`，跳到 8.3 发起退款

### 8.3 审核与退款

1. Controller 接收 `PUT /mall-order/after_sales/{id}/status`
2. 通过：`AfterSaleServiceImpl.approve()`：
   - `afterSaleMapper.updateStatus(id, APPROVED)`
   - 调 `RemotePaymentService.refund(payOrderNo, refundAmount, afterSaleId)` → mall-payment 创建退款单并调支付渠道
3. 拒绝：`AfterSaleServiceImpl.reject()`：
   - `afterSaleMapper.updateStatus(id, REJECTED)`，通知用户

### 8.4 回调处理

1. `RefundSucceededConsumer` 消费 `mall:refund:succeeded`
2. `AfterSaleServiceImpl.refundCallback()`：
   - `afterSaleMapper.updateStatus(id, SUCCESS)`，`completed_time=NOW()`
   - UPDATE `mall_order.refunded_amount += 退款金额`
   - 若退货退款 → 调 `RemoteProductService.restock(skuId, qty)` 回补库存
3. 退款失败 → `afterSaleMapper.updateStatus(id, FAILED)`，标记人工介入

---

## 9 Nacos 配置

### 9.1 DataId: `mall-order-dev.yml`

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
  typeAliasesPackage: com.mall.order.**.domain
  mapperLocations: classpath:mapper/**/*.xml

springdoc:
  gatewayUrl: http://localhost:8080/${spring.application.name}
  api-docs:
    enabled: true
  info:
    title: '订单模块接口文档'
    description: '订单模块接口描述'
    contact:
      name: RuoYi
      url: https://ruoyi.vip

mall:
  order:
    pay-expire-minutes: 30
    timeout-scan-interval: 30
    timeout-batch-size: 500
    refund-days: 7
    auto-receive-days: 15
    cart-max-items: 99
    auto-approve-threshold: 10000
```

> 以上配置通过 Nacos 下发，支持 `@RefreshScope` 运行时动态刷新。

### 9.2 本地配置文件 `bootstrap.yml`

```yaml
# mall-order 订单服务
server:
  port: 9304

spring:
  application:
    name: mall-order
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

| 配置项 | 默认值 | 单位 | 说明 |
|--------|:---:|------|------|
| `mall.order.pay-expire-minutes` | 30 | 分钟 | 下单后未支付自动关闭 |
| `mall.order.timeout-scan-interval` | 30 | 秒 | 超时扫描间隔 |
| `mall.order.timeout-batch-size` | 500 | 条 | 单次扫描上限 |
| `mall.order.refund-days` | 7 | 天 | 收货后可申请售后 |
| `mall.order.auto-receive-days` | 15 | 天 | 发货后自动确认收货 |
| `mall.order.cart-max-items` | 99 | 个 | 购物车最多商品数 |
| `mall.order.auto-approve-threshold` | 10000 | 分（¥100） | 售后自动审核金额阈值 |

---

## 10 错误码

定义见系统设计第 2 章，以下为本模块使用的错误码：

| 错误码 | 说明 | 本模块触发场景 |
|--------|------|--------------|
| A0301 | 访问未授权 | 未登录访问 C 端接口 |
| A0320 | 无操作权限 | 管理端权限码不足 |
| A0501 | 资源不存在 | SKU/地址/优惠券/购物车项/售后单不存在 |
| A0520 | 商品已下架 | 下单时商品 isOnSale=false |
| A0521 | 库存不足 | 锁库存失败 |
| A0612 | 不满足优惠券使用条件 | 锁优惠券失败 |
| A0701 | 订单不存在 | 查询/操作订单时不存在 |
| A0702 | 订单状态异常 | 状态机转移矩阵无匹配 |
| A0703 | 订单状态不允许当前操作 | 前置条件不满足（如删除非终态订单） |
| A0710 | 购物车为空 | 下单时购物车无选中项 |
| A0711 | 超出购买数量限制 | 超过限购上限 |
| B0001 | 系统执行出错 | 订单创建事务失败 |

> 以上配置通过 Nacos 下发，支持 `@RefreshScope` 运行时动态刷新。
