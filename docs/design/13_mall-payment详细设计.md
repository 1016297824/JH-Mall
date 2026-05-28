# JH-Store mall-payment 模块详细设计

> 基于系统详细设计 `03_系统详细设计.md` 展开。数据表 DDL 在系统设计 1.4 节统一维护，此处只引用表名和字段。

---

## 1 模块概述

### 1.1 子领域

| 子领域 | 实体 | 说明 |
|--------|------|------|
| 支付单管理 | `mall_payment` | 创建支付单、调支付平台、状态推进、支付单查询 |
| 退款单管理 | `mall_payment_refund` | 创建退款单、调支付渠道退款、状态推进、退款单查询 |
| 支付渠道配置 | `mall_payment_channel` | 多渠道（微信/支付宝）Enable/Disable、API Key 加密存储 |
| 回调处理 | `mall_payment_callback_log` | 支付回调验签、退款回调验签、nonce 防重放、回调日志记录 |
| RocketMQ 事件 | Outbox（无独立实体） | 生产 `mall:payment:paid` / `mall:refund:succeeded` / `mall:refund:failed` |

### 1.2 依赖关系

```
mall-payment (9305端口)
  ├── MySQL：自有表（见表系统设计第 1.4 节）
  ├── Redis：回调幂等去重 (mall:payment:callback:* / mall:payment:refund_callback:*)
  ├── RocketMQ (Producer)：写 Outbox → 投递 mall:payment:paid / mall:refund:succeeded / mall:refund:failed
  ├── RocketMQ (Consumer)：无（支付服务不消费其他服务事件）
  ├── mall-api (Feign)：RemotePaymentService 供 mall-order 调用退款
  ├── mall-order (Feign Caller)：发起支付时校验订单状态和金额
  └── 支付平台：微信支付 / 支付宝 (外部 SDK)
```

> 关键边界：支付服务负责资金通道，不修改订单业务状态、不操作库存、不修改积分。支付成功后通过 RocketMQ 通知 mall-order 推进订单。

---

## 2 包结构与接口映射

### 2.1 完整包结构

```
server/mall/mall-payment/
└── src/main/java/com/mall/payment/
    ├── MallPaymentApplication.java          # Spring Boot 启动类
    ├── controller/
    │   ├── admin/
    │   │   └── PaymentAdminController.java  # /mall-payment/**
    │   └── api/
    │       ├── PaymentApiController.java    # /api/payment/**
    │       └── PaymentCallbackController.java # /callback/payment/**
    ├── DO/
    │   ├── MallPaymentDO.java                 # 对应 mall_payment 表
    │   ├── MallRefundDO.java                  # 对应 mall_payment_refund 表
    │   ├── MallPaymentChannelDO.java          # 对应 mall_payment_channel 表
    │   └── MallPaymentCallbackLogDO.java      # 对应 mall_payment_callback_log 表
    ├── service/
    │   ├── PaymentService.java              # 接口
    │   ├── impl/
    │   │   └── PaymentServiceImpl.java      # 支付核心业务
    │   ├── RefundService.java               # 接口
    │   ├── impl/
    │   │   └── RefundServiceImpl.java       # 退款核心业务
    │   ├── PaymentChannelConfigService.java # 接口
    │   ├── impl/
    │   │   └── PaymentChannelConfigServiceImpl.java # 渠道配置管理
    │   ├── CallbackService.java             # 接口
    │   └── impl/
    │       └── CallbackServiceImpl.java     # 回调处理（验签+推进+写Outbox）
    ├── mapper/
    │   ├── MallPaymentMapper.java
    │   ├── MallRefundMapper.java
    │   ├── MallPaymentChannelMapper.java
    │   └── MallPaymentCallbackLogMapper.java
    ├── statemachine/
    │   └── PaymentStateMachine.java         # 支付单 + 退款单状态机
    ├── infrastructure/
    │   ├── feign/
    │   │   └── RemoteOrderAdapter.java      # 调 mall-order Feign（校验订单）
    │   └── channel/                         # 支付渠道适配层
    │       ├── PaymentChannelAdapter.java   # 渠道统一接口
    │       ├── WechatPayAdapter.java        # 微信支付适配（JSAPI/小程序/H5/Native）
    │       └── AlipayAdapter.java           # 支付宝适配
    ├── dto/                                   # 接口进出 DTO（请求/响应 JSON 契约）
    │   ├── request/                           → PayRequestDTO, RefundRequestDTO, ChannelConfigRequestDTO ...
    │   └── response/                          → PayResultDTO, CallbackDTO ...
    ├── vo/                                    # 视图对象，前端展示（与 DO 字段不同，需转换）
    │   ├── PayParamsVO.java                   # 支付 SDK 调用参数（appId/timeStamp/paySign 等，无 DO 对应）
    │   ├── PaymentVO.java                     # C 端支付单（金额分→元、status→文本）
    │   ├── PaymentDetailVO.java               # 管理端支付单详情（更多字段）
    │   ├── RefundVO.java                      # C 端退款单（金额分→元）
    │   ├── RefundDetailVO.java                # 管理端退款单详情
    │   └── ChannelConfigVO.java               # 管理端渠道配置（屏蔽密钥明文）
    └── convert/                               # 纯转换器（Entity↔DTO↔VO 字段映射）
        ├── PaymentConvert.java                # MallPayment → PaymentVO / PaymentDetailVO / PayResultDTO
        └── RefundConvert.java                 # MallRefund → RefundVO / RefundDetailVO
```

### 2.2 接口 → Controller 映射表

| # | HTTP | 路径 | Controller | 方法名 | 认证 | 权限码 |
|---|------|------|-----------|--------|:---:|--------|
| 1 | POST | `/api/payment/payments` | PaymentApiController | `createPayment(payRequest)` | C端token | — |
| 2 | GET | `/api/payment/payments/{paymentId}` | PaymentApiController | `getPayment(paymentId)` | C端token | — |
| 3 | POST | `/api/payment/refunds` | PaymentApiController | `createRefund(refundRequest)` | C端token | — |

管理端接口由若依代码生成器自动生成（支付/退款查询、渠道配置），权限码无需手动维护。

**回调接口（/callback/payment/，无需认证）：**

| # | HTTP | 路径 | Controller | 方法名 | 认证 | 权限码 |
|---|------|------|-----------|--------|:---:|--------|
| 10 | POST | `/callback/payment/{channel}` | PaymentCallbackController | `payCallback(channel, body)` | 无(验签) | — |
| 11 | POST | `/callback/payment/{channel}/refund` | PaymentCallbackController | `refundCallback(channel, body)` | 无(验签) | — |

---

## 3 核心类设计

### 3.1 PaymentStateMachine

位于 `statemachine/PaymentStateMachine.java`，管理**支付单**和**退款单**两套状态转移。

类结构（文字描述）：

- 注入 `MallPaymentMapper` + `MallRefundMapper`，不直接访问其他 Service
- `paymentTransition(paymentNo, event)`：支付单转移入口。内部查 `MallPayment`，校验前置条件，执行 `UPDATE ... WHERE status=? AND version=?`，影响 0 行抛 `A0702`
- `refundTransition(refundNo, event)`：退款单转移入口，逻辑同上
- 前后置检查封装为 `PaymentPrecondition` / `RefundPrecondition` 内部类
- 不负责 RocketMQ 发布和渠道调用，只做 DB 状态推进
- 设计遵循「一个转移方法只做一种转移」，不写通用 transfer() 方法

转移矩阵详见第 6 节。

### 3.2 PaymentServiceImpl

位于 `service/impl/PaymentServiceImpl.java`，支付核心业务。

核心方法：

- `createPayment(PayRequestDTO)`：发起支付
  - ①幂等：按 `idempotent_key = userId_orderNo_channelCode` 查现有支付单，已存在复用、返回相同 payParams
  - ②校验：Feign 调 mall-order 查询订单状态+金额，仅 `WAIT_PAY` 且未过期可支付
  - ③创建支付单：`payment_no` 雪花 + `PAY` 前缀，初始 `payment_status=UNPAID`
  - ④路由渠道：根据 `channel` 字段选支付适配（`WechatPayAdapter`/`AlipayAdapter`），调 `channelAdapter.invokePay()`
  - ⑤落渠道交易号：`channel_payment_no` + `channel_pay_status=NOTPAY`，`pay_expire_time` 同步订单超时时间
  - ⑥返回支付参数（appId / timeStamp / nonceStr / package / signType / paySign）

- `getPayment(userId, paymentId)`：查支付单归属校验 + 支付状态

> 渠道支付参数 `notify_url` 统一由渠道配置中的 `callback_base_url + /callback/payment/{channel}` 拼接。

### 3.3 RefundServiceImpl

位于 `service/impl/RefundServiceImpl.java`，退款核心业务。

核心方法：

- `createRefund(RefundRequestDTO)`：由 mall-order 调 Feign 触发
  - ①前置校验：查支付单 `payment_status=PAID`；累计已退款 ≤ 支付金额；`afterSaleNo` 幂等去重
  - ②创建退款单：`refund_no` 雪花 + `REF` 前缀，初始 `refund_status=PROCESSING`，写入 `idempotent_key=afterSaleNo_channelCode`
  - ③调支付渠道退款：选 `payment` 的原始渠道适配，调 `channelAdapter.invokeRefund(payment, refund)`
  - ④即时结果处理：渠道即时成功 → 更新 `refund_status=SUCCESS`，写 Outbox；渠道返回处理中 → 保持 `PROCESSING`，等异步回调

- `getRefund(userId, refundId)` / `listRefunds`：管理端查询

### 3.4 PaymentChannelConfigServiceImpl

位于 `service/impl/PaymentChannelConfigServiceImpl.java`，支付渠道配置管理。

- `listEnabled()`：查 `is_enabled=1` 的渠道列表（按 `sort_order` 排序），用于前端展示可用支付方式
- `update(channelId, config)`：修改配置。`config_json` 中敏感字段（apiKey、私钥）加密存储，管理端查询响应不返回明文
- 渠道配置内存缓存：Caffeine 本地缓存（TTL 5 分钟），减少每次支付时查 DB。Nacos 配置变更通过 `@RefreshScope` 触发缓存刷新

### 3.5 CallbackServiceImpl

位于 `service/impl/CallbackServiceImpl.java`，支付/退款回调处理。

异步回调是支付链路最关键的节点，必须实现 **先落库再应答 + 幂等 + 防重放**：

核心方法：

- `processPayCallback(channel, rawBody)`：
  - ①记录原始回调日志（`mall_payment_callback_log` 插入）
  - ②`is_verified` 字段控制验签状态（此处交给网关/Filter 完成验签，Service 层依验签结果分支）
  - ③验签失败：标记 → 返回 `400` 给平台
  - ④提取交易号（channelPaymentNo），Redis `SETNX mall:payment:callback:{channel}:{tradeNo}` 去重 → 命中则直接返回 200
  - ⑤根据 `channelPaymentNo` 查 `mall_payment` → `UPDATE SET payment_status=PAID WHERE payment_status=UNPAID`
  - ⑥`payment_status` 推进后在同一本地事务写 Outbox `mall:payment:paid`
  - ⑦返回支付平台要求的成功应答（微信: XML `<return_code>SUCCESS</return_code>`，支付宝: `success`）

- `processRefundCallback(channel, rawBody)`：同支付回调结构，`channelRefundNo` 去重，更新退款单终态后写 Outbox

---

## 4 发起支付流程实现

### 4.1 幂等设计

用户对同一订单同一渠道重复发起支付时必须幂等：

| 幂等键 | 存储 | TTL | 命中后行为 |
|--------|------|-----|-----------|
| `userId_orderNo_channelCode` | DB `mall_payment.idempotent_key` 单字段唯一约束 | 永久 | 复用已有支付单，返回相同的 payParams，不重复调支付平台 |

- 第一次：`uk_idempotent_key` 不冲突 → 正常插入
- 第二次：`DuplicateKeyException` 捕获 → 查已有记录，返回同 payParams
- Redis 冗余缓存一层幂等键（TTL 30分钟），减少 DB 冲突抛异常的次数（性能优化，非强依赖）

### 4.2 订单校验

① Feign 调 `RemoteOrderAdapter.queryOrder(orderNo)` → 获取订单当前 `status` 和 `pay_amount`（快照金额）
② `status != WAIT_PAY` → 返回 `A0702`（订单状态不允许支付）
③ `pay_amount <= 0` → 返回 `A0602`（金额超出限制）
④ `pay_expire_time < NOW()` → 返回 `A0701`（订单已过期）
⑤ 支付金额以订单快照 `pay_amount` 为准，不重新计算优惠/运费，防止优惠变化导致支付金额不一致

### 4.3 支付渠道路由

`PaymentChannelAdapter` 接口定义：

```java
PayResult invokePay(MallPayment payment, MallPaymentChannel channel, String openid);
RefundResult invokeRefund(MallPayment payment, MallRefund refund, MallPaymentChannel channel);
ChannelBillResult queryBill(String channelPaymentNo, MallPaymentChannel channel);
```

| 适配器 | 适用场景 | SDK 依赖 |
|--------|---------|---------|
| `WechatPayAdapter` | JSAPI（公众号内）、小程序、H5、Native | wechatpay-java |
| `AlipayAdapter` | 手机网站、App、扫码 | alipay-sdk-java |

- 工厂类 `PaymentChannelFactory` 根据 `channel` 字段实例化对应适配
- `WechatPayAdapter`：小程序/JSAPI 传入 `openid`（从 mall-auth 获取）；H5 传入场景信息；Native 返回二维码链接
- `AlipayAdapter`：手机网站返回 form 表单或 trade_no

### 4.4 超时与关闭

支付单 `expire_time` 与订单 `pay_expire_time` 一致（创建后 30 分钟）：

- 到期未支付：支付平台主动关闭订单（调 `closeOrder` API），支付单 status → `CLOSED`
- 到期后用户扫码：支付平台已关闭，返回"订单已过期"
- 定时任务扫描：`WHERE payment_status=UNPAID AND expire_time < NOW()` → 调渠道关单 + 更新 `payment_status=CLOSED`

---

## 5 支付回调流程实现

### 5.1 整体流程

支付回调有两条路径在网关层合流：

1. **网关验签层**：IP 白名单 + 支付平台签名验证（微信 SHA256-RSA / 支付宝 RSA2）
   - 验签失败返回 400，记录告警
   - 验签通过注入 `X-Internal-Verified: true` 头转发到 Service
2. **Service 处理层**：回调日志 + nonce 去重 + DB 更新 + Outbox + 应答

### 5.2 nonce 防重放

| 方案 | 机制 | TTL | 说明 |
|------|------|-----|------|
| A（推荐） | Redis `SETNX mall:payment:callback:{channel}:{tradeNo}` | 24h | 支付平台可能重复回调，同一 tradeNo 只处理一次 |
| B（兜底） | DB `uk_nonce` 唯一约束 | 永久 | Redis 不可用时降级 |

- 处理成功直接返回 200（平台要求的应答），不重复推进状态
- TTL 24h 覆盖支付平台最大重试窗口（微信最多 15 次，最长 6h；支付宝最长 24h）

### 5.3 先落库再应答

> 系统设计 5.2.4 节关键约束：回调先更新支付单为 PAID，再返回 200 OK 给支付平台。

不能反过来——如果先返回 200 再落库，机器崩溃会导致"平台以为成功了、但我们没记录"。

```sql
UPDATE mall_payment
SET payment_status = 2,  -- PAID
    channel_pay_status = #{channelPayStatus},
    pay_success_time = NOW(),
    version = version + 1
WHERE payment_no = #{paymentNo}
  AND payment_status = 0  -- UNPAID，乐观锁防并发
  AND version = #{version}
```

影响 0 行 → 已被其他回调/定时任务处理 → 查询当前状态，已 PAID 则返回 200（幂等）

### 5.4 回调通知订单

支付单状态变为 PAID 后，同一事务写入 Outbox：

```
topic: mall:payment:paid
payload: { "paymentNo": "PAY...", "orderNo": "ORD...", "payAmount": 89900, "payTime": "2026-05-17T10:30:00", "channelPaymentNo": "4200001234..." }
```

mall-order 消费后推进订单 `WAIT_PAY → PAID`（由订单状态机保证幂等）。

### 5.5 补偿

| 场景 | 补偿机制 |
|------|---------|
| 支付单已 PAID 但订单仍是 WAIT_PAY | ruoyi-job 每 60s 扫描 `payment.status=PAID AND order.status=WAIT_PAY` 不一致记录，重放 `mall:payment:paid` |
| 支付平台回调未收到（网络中断） | 定时任务扫描 `UNPAID + 创建超 30min` 的支付单，主动调支付平台 `queryOrder` API 查交易状态 |

---

## 6 退款流程实现

### 6.1 退款发起（Feign 触发）

mall-order 审核售后通过后，调 Feign `RemotePaymentService.createRefund(RefundDTO)`。

① **幂等**：`idempotent_key = afterSaleNo + channelCode`（DB 唯一约束）→ 重复调用返回已有退款单
② **金额校验**：`累计已退款 + 本次退款 <= 支付金额`
③ **创建退款单**：`refund_no` 前缀 `REF`，`refund_status=PROCESSING`，`channel_code` 与原始支付渠道一致
④ **异步调支付渠道**：调 `channelAdapter.invokeRefund(payment, refund)`
   - 微信：`Refunds.apply()` 同步返回 `status=SUCCESS/PROCESSING/FAIL`
   - 支付宝：`AlipayTradeRefundResponse` 返回 `fund_change=Y/N`
⑤ **即时结果**：SUCCESS → 更新 `refund_status=SUCCESS` + 写 Outbox；PROCESSING → 不动，等回调

### 6.2 退款回调

与支付回调流程一致，具体见系统设计 5.3.4/5.3.5 节：

- `payment_no` → `refund_no`，`tradeNo` → `channelRefundNo`
- 幂等键 `mall:payment:refund_callback:{channel}:{refundNo}`，Redis SETNX TTL 24h
- 成功后写 Outbox：`mall:refund:succeeded` / `mall:refund:failed`

### 6.3 退款失败处理

退款回调返回 FAILED 时：

① `refund_status` 不推进 → 维持 PROCESSING（等待后续处理）
② 写 Outbox `mall:refund:failed` → mall-order 消费后通知用户退款失败
③ 操作员可重试退款（调 `retryRefund(refundNo)` → 重新调 `channelAdapter.invokeRefund`）

---

## 7 状态机实现

### 7.1 状态定义

#### 7.1.1 支付单状态

| 状态 | 含义 | 可转移 |
|------|------|:---:|
| UNPAID | 待支付，创建支付单后初始状态 | 是 |
| PAID | 支付成功，收到支付平台回调 | 是 |
| FAILED | 支付失败，支付平台返回明确失败 | 否 |
| CLOSED | 已关闭，超时未支付或用户主动取消 | 否 |
| REFUNDING | 退款中，已发起退款 | 是 |
| REFUNDED | 已退款，退款成功回调 | 否 |

#### 7.1.2 退款单状态

| 状态 | 含义 | 可转移 |
|------|------|:---:|
| PROCESSING | 退款中，已创建退款单 + 调渠道 | 是 |
| SUCCESS | 退款成功，渠道回调确认 | 否 |
| FAILED | 退款失败，渠道回调返回失败 | 是（可重试→PROCESSING） |

### 7.2 转移矩阵

#### 7.2.1 支付单转移

| 当前状态 | 触发事件 | 事件源 | 目标状态 | 前置条件 | 后置动作 |
|---------|---------|:-----:|---------|---------|---------|
| UNPAID | 发起支付 | 用户 | UNPAID | 订单 WAIT_PAY、金额>0、未过期 | ①调支付平台获取 `prepay_id` ②更新 `channel_payment_no` |
| UNPAID | 支付成功回调 | 回调 | PAID | 渠道验签通过、金额匹配、订单一致 | ①更新 `pay_success_time` ②同一事务写 Outbox `mall:payment:paid` |
| UNPAID | 支付失败回调 | 回调 | FAILED | 渠道返回明确失败 | ①记录失败原因到 `channel_pay_status` |
| UNPAID | 支付超时/关闭 | 系统/用户 | CLOSED | 超时或用户主动关闭 | ①调渠道关单 API |
| PAID | 发起退款 | 系统 | REFUNDING | mall-order 审核通过 | ①创建退款单 ②调渠道退款 API |
| REFUNDING | 退款成功回调 | 回调 | REFUNDED | 渠道验签通过 | ①更新 `refund_success_time` ②同一事务写 Outbox `mall:refund:succeeded` |
| REFUNDING | 退款失败回调 | 回调 | PAID | 渠道返回明确失败 | ①记录失败原因 ②写 Outbox `mall:refund:failed` |
| REFUNDING | 全额退款完成 | 回调 | REFUNDED | 所有退款成功 | — |

#### 7.2.2 退款单转移

| 当前状态 | 触发事件 | 事件源 | 目标状态 | 前置条件 | 后置动作 |
|---------|---------|:-----:|---------|---------|---------|
| PROCESSING | 退款成功回调 | 回调 | SUCCESS | 渠道验签通过 | ①记录 `refund_success_time` |
| PROCESSING | 退款失败回调 | 回调 | FAILED | 渠道返回明确失败 | ①记录失败原因 |
| FAILED | 重试退款 | 操作员 | PROCESSING | 退款单未过期 | ①重新调渠道退款 API |

### 7.3 类设计

`PaymentStateMachine` 核心方法：

```
// 支付单转移
transition(paymentNo, PaymentEvent event)
// 退款单转移
refundTransition(refundNo, RefundEvent event)
```

- 内部使用 `MallPaymentMapper.selectByPaymentNo()` 查当前状态
- 乐观锁更新：`UPDATE ... WHERE payment_status=? AND version=?`
- 影响行数 = 0 抛 `A0702`（状态转移非法）
- 不放 RocketMQ 发布逻辑，不放渠道 API 调用逻辑
- 与 Service 集成方式：Service 调状态机推进成功后，再调渠道 API / 写 Outbox

---

## 8 RocketMQ 事件

### 8.1 mall-payment 发布的事件

| Topic | Payload 字段 | 发布时机 |
|-------|-------------|---------|
| `mall:payment:paid` | `paymentNo`、`orderNo`、`userId`、`payAmount`（分）、`payTime`、`channelPaymentNo`、`channelCode` | 支付回调处理成功后 |
| `mall:refund:succeeded` | `refundNo`、`paymentNo`、`orderNo`、`afterSaleNo`、`userId`、`refundAmount`（分）、`refundTime`、`channelRefundNo` | 退款回调成功 |
| `mall:refund:failed` | `refundNo`、`paymentNo`、`orderNo`、`afterSaleNo`、`userId`、`refundAmount`（分）、`failReason`、`channelRefundNo` | 退款回调失败 |

> Payload 使用稳定 DTO，不直接序列化 MallPayment/MallRefund。字段命名 lowerCamelCase。

### 8.2 Outbox 重试策略

同系统设计 6.3.3 节：

| 重试次数 | 间隔 | next_retry_time |
|:-------:|------|----------------|
| 第 1 次 | 10s | NOW() + 10s |
| 第 2 次 | 30s | NOW() + 30s |
| 第 3 次 | 60s | NOW() + 60s |
| 超过 3 次 | — | status = FAILED，死信处理 |

### 8.3 关键约束

- Outbox INSERT 与支付单/退款单状态 UPDATE 必须在 **同一本地事务**
- 投递顺序不要求严格顺序，消费方（mall-order）通过状态机 + 幂等保证最终一致
- 已投递记录延迟 7 天清理

---

## 9 支付渠道适配层

### 9.1 策略模式设计

```
PaymentService
  └── PaymentChannelFactory.getAdapter(channel)  // 工厂选适配器
        ├── WechatPayAdapter                     // 微信支付（JSAPI/小程序/H5/Native）
        └── AlipayAdapter                        // 支付宝

RefundService
  └── PaymentChannelFactory.getAdapter(payment.getChannelCode())  // 同渠道退款
```

### 9.2 渠道差异抽象

| 差异点 | 微信 | 支付宝 |
|--------|------|--------|
| 支付参数返回 | 字段串（appId/timeStamp/nonceStr/package/signType/paySign） | tradeNo 或 form 表单 |
| 回调签名验证 | SHA256-RSA + APIv3 证书 | RSA2 + 支付宝公钥 |
| 回调应答格式 | XML `<return_code>SUCCESS</return_code>` | 纯文本 `success` |
| 退款 | 证书双向认证 | 应用私钥签名 |
| 交易号字段 | `transaction_id` | `trade_no` |

### 9.3 配置加密

`mall_payment_channel.config_json` 中敏感字段（商户私钥、API Key、证书序列号）AES-256-GCM 加密存储：

- 写入：管理端 PUT 时，`PaymentChannelConfigServiceImpl.update()` 加密后落库
- 读取：渠道适配器读取时，解密后使用，解密密钥从 Nacos 配置获取
- 查询：管理端 GET 响应不返回 `apiKey`、`privateKey` 等字段明文，返回 `***` 掩码
- 错误处理：解密失败记录告警（非业务异常），不影响其他渠道

---

## 10 回调安全

### 10.1 验签

支付平台回调的安全由网关 + 渠道适配器双层保障：

| 层 | 方式 | 作用 |
|----|------|------|
| 网关层 | IP 白名单 + 基础格式校验 | 拦截无效请求 |
| 适配器层 | 支付平台 SDK 验签（微信 SHA256-RSA / 支付宝 RSA2） | 确认消息来源真实性 |

- 适配器验签失败 → `CallbackServiceImpl` 记录 `is_verified=2`（验签失败）→ 告警 → 返回 400
- 网关注入 `X-Internal-Verified: true` 头 ≠ 跳过适配器验签，适配器仍需再验（纵深防御）

### 10.2 nonce 防重放

见系统设计 7.3.3 节：

- 幂等键格式：`mall:payment:callback:{channel}:{tradeNo}`（支付）/ `mall:payment:refund_callback:{channel}:{refundNo}`（退款）
- Redis `SETNX`，TTL 24h
- 命中直接返回 200（平台要求的成功应答），不重复处理
- Redis 不可用降级到 DB `uk_nonce` 唯一约束

### 10.3 回调日志完整记录

`mall_payment_callback_log` 表记录每次回调完整信息（`raw_body` 原始报文、`nonce`、`is_verified`、`process_status`），用于：
- 对账：与支付平台对账时回溯
- 问题排查：回调处理失败时完整复现
- 审计：不可篡改的支付证据

---

## 11 Nacos 配置

### 11.1 DataId: `mall-payment-dev.yml`

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

mybatis-plus:
  typeAliasesPackage: com.mall.payment.**.DO
  mapperLocations: classpath:mapper/**/*.xml

springdoc:
  gatewayUrl: http://localhost:8080/${spring.application.name}
  api-docs:
    enabled: true
  info:
    title: '支付模块接口文档'
    description: '支付模块接口描述'
    contact:
      name: RuoYi
      url: https://ruoyi.vip

mall:
  payment:
    callback:
      timeout: 30
      nonce-ttl: 86400
    channels: wechat,alipay
    channel:
      cache-ttl: 300
      wechat:
        app-id:
        mch-id:
        api-v3-key:
        private-key:
        serial-no:
      alipay:
        app-id:
        private-key:
        public-key:
    active-query-delay: 1800
```

> 以上配置通过 Nacos 下发，支持 `@RefreshScope` 热更新（标 * 的需重启生效）。
> 配置项通过 `MallPaymentConfigProperties`（`@ConfigurationProperties(prefix = "mall.payment")` + `@RefreshScope`）注入，各 Service/Controller 通过构造注入获取，禁止使用 `@Value`。

### 11.2 本地配置文件 `bootstrap.yml`

```yaml
# mall-payment 支付服务
server:
  port: 9305

spring:
  application:
    name: mall-payment
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

### 11.3 配置项说明

| 配置项 | 默认值 | 单位 | 说明 |
|--------|--------|:---:|------|
| `mall.payment.callback.timeout` | 30 | 秒 | 支付平台回调/主动查询超时时间 |
| `mall.payment.callback.nonce-ttl` | 86400 | 秒 | 回调 nonce 去重 TTL（默认 24h） |
| `mall.payment.channels` | `wechat,alipay` | — | 启用的支付渠道列表，逗号分隔（*） |
| `mall.payment.channel.cache-ttl` | 300 | 秒 | 渠道配置本地缓存 TTL |
| `mall.payment.active-query-delay` | 1800 | 秒 | 支付单创建后 N 秒未支付，主动调渠道查交易状态 |
| `mall.payment.channel.wechat.app-id` | — | — | 微信 AppId（*） |
| `mall.payment.channel.wechat.mch-id` | — | — | 微信商户号（*） |
| `mall.payment.channel.wechat.api-v3-key` | — | — | 微信 APIv3 密钥（加密存储） |
| `mall.payment.channel.wechat.private-key` | — | — | 微信商户私钥（加密存储，PEM 格式） |
| `mall.payment.channel.wechat.serial-no` | — | — | 微信商户证书序列号（*） |
| `mall.payment.channel.alipay.app-id` | — | — | 支付宝 AppId（*） |
| `mall.payment.channel.alipay.private-key` | — | — | 支付宝应用私钥（加密存储） |
| `mall.payment.channel.alipay.public-key` | — | — | 支付宝公钥（*） |

---

## 12 错误码汇总

| 错误码 | HTTP | userTip | 说明 |
|--------|:----:|---------|------|
| 00000 | 200 | — | 成功 |
| A0301 | 401 | 请先登录 | 未登录 |
| A0320 | 403 | 无权限访问 | 权限不足 |
| A0410 | 429 | 请求过于频繁，请稍后再试 | 触发限流 |
| A0501 | 404 | 支付单不存在 | paymentNo 不存在或已被逻辑删除 |
| A0502 | 404 | 退款单不存在 | refundNo 不存在或已被逻辑删除 |
| A0601 | 400 | 余额不足 | 支付金额超过余额（预留） |
| A0602 | 400 | 支付金额超出限制 | 支付金额 ≤ 0 或超过订单金额 |
| A0614 | 400 | 退款金额超出可退金额 | 累计已退款 + 本次退款 > 已支付金额 |
| A0615 | 400 | 支付渠道已禁用 | 选的支付渠道 is_enabled=0 |
| A0701 | 400 | 订单不存在或已过期 | 订单不存在或 pay_expire_time 已过 |
| A0702 | 400 | 订单状态不允许支付 | 订单不是 WAIT_PAY 状态 |
| B0001 | 500 | 系统繁忙，请稍后再试 | 未预期异常 |
| C0110 | 500 | 服务暂时不可用 | Redis 连接失败 |
| C0120 | 500 | 数据库异常 | MySQL 连接失败 |
| C0210 | 503 | 支付服务调用失败 | 调支付平台 SDK 失败 |
| C0211 | 503 | 退款服务调用失败 | 调支付平台退款 API 失败 |

> 系统设计第二章错误码表定义了统一错误码体系。A0614/A0615 为 payment 模块新增用户侧错误码，建议补入系统设计 A06xx 段。
> 回调验签失败 → HTTP 400（无业务错误码），回调处理异常 → callback_log 记录 + 日志告警，不产生 errorCode。

---

## 13 服务间 Feign 安全（接收方）

mall-payment 供 mall-order 通过 Feign 内部调用，由 `mall-api` 共享的 `InnerSignatureFilter`（`com.mall.api.infrastructure.security`）验签。

- `InnerSignatureFilter` 通过 `@Component` 自动注册，拦截 `/inner/**`，无需额外配置
- 白名单：`/actuator/health`、`/callback/payment/**`（支付平台走自身验签）
- 错误码 `A0311`/`A0312`

签名算法详见 [03_系统详细设计.md §7.3](file:///e:/Workspace/AI/JH-Mall/docs/design/03_系统详细设计.md#L4346)。
