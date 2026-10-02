# mall-payment 模块实施计划

> 设计基线：`docs/design/13_mall-payment详细设计.md`（672 行）
> 契约：`docs/design/07_mall-api契约层设计.md`
> 制定日期：2026-10-03

---

## 0 开工前已定事项

### 0.1 用户已确认的三项决策

| 问题 | 决策 |
|------|------|
| 支付渠道落地方式 | **适配层抽象 + 模拟渠道** —— 定义 `PaymentChannelAdapter` 接口，先只实现 `MockPayAdapter`，本地可跑通「支付→回调→订单流转」全链路；微信/支付宝真实适配留接口，待有商户资质时补 |
| 本次范围 | **核心支付链路** —— 支付单/退款单/状态机/回调/Outbox/C 端 3 接口/3 个 inner 端点/超时关单与补偿定时任务。不含管理端渠道配置（AES 加密） |
| `.gitignore` | **已补** `/temp/` 与 `/.workbuddy/`（防含数据库密码的 SQL 被 `git add -A` 误提交） |

### 0.2 核查中发现的问题（需在实现时处理或记录）

#### ① 设计 §5.3 的状态码注释写错 ⚠️

设计 §5.3 的示例 SQL：

```sql
SET payment_status = 2,  -- PAID
...
AND payment_status = 0   -- UNPAID，乐观锁防并发
```

但 `PaymentStatusEnum` 实际取值是 **`UNPAID(0) / PAID(1) / FAILED(2) / CLOSED(3) / REFUNDING(4) / REFUNDED(5)`**。

→ **`PAID` 应为 `1`，注释里的 `2` 是笔误**（`2` 是 `FAILED`）。
→ 本次实现以**枚举为准**，并更正设计文档该行注释。

> 这与 marketing 那次「枚举与文档差 1」是同类问题，但方向相反：这次**枚举对、文档错**。
> 已核对 DDL：`payment_status tinyint DEFAULT 0`（= UNPAID ✓ 与枚举一致），`refund_status DEFAULT 0`（= PROCESSING ✓）。

#### ② 验签口径自相矛盾（需改设计口径）

| 位置 | 说法 |
|------|------|
| 设计 §3.5 | 「`is_verified` 字段控制验签状态（此处**交给网关/Filter 完成验签**）」 |
| 设计 §5.1 | 「网关验签层：IP 白名单 + 支付平台签名验证（微信 SHA256-RSA / 支付宝 RSA2）」 |
| 设计 §10.1 | 「网关注入 `X-Internal-Verified: true` 头 **≠ 跳过适配器验签**，适配器仍需再验（纵深防御）」 |

**§3.5 / §5.1 的描述在技术上不成立**：ruoyi-gateway 是通用 Spring Cloud Gateway，
不可能实现微信 SHA256-RSA / 支付宝 RSA2 验签 —— 那是**渠道 SDK 的职责**，
且需要私有密钥与平台证书。

→ **本次口径定为**：**渠道适配器层验签**（由 SDK 完成），网关只做 IP 白名单与格式校验。
→ §3.5 / §5.1 的措辞需更正（**属改设计文档，待批准**）。

#### ③ 设计 §13 的结论是错的

§13 称 `InnerSignatureFilter`「通过 `@Component` 自动注册，拦截 `/inner/**`，无需额外配置」。

**已查实全模块未生效**：7 个模块的 `scanBasePackages` 均不含 `com.mall.api`，
且 mall-api 无 `spring.factories`。即当前 `/inner/**` **无任何签名校验**。

→ 本次**不修**（此前已定为「后续单独修」），但**必须同步更正该段文档**，避免继续误导。
→ mall-payment 启动类补 `com.mall.common`（注册 `MallExceptionHandler`），与 marketing 一致。

#### ④ `MqTopicConstants.Payment` 缺一个 topic ⚠️

现有：`CREATED` / `PAID` / `FAILED` / `REFUND_CREATED` / `REFUND_SUCCEEDED`。

但设计 §8.1 要求发布三个事件，其中 **`mall:refund:failed` 无对应常量**。

→ **需补** `REFUND_FAILED = "mall:refund:failed"`（按 AGENTS.md「常量类的新增/补全可先行」）。

#### ⑤ 转移矩阵里的 `UNPAID → UNPAID` 不是状态转移

设计 §7.2.1 第 1 行：`UNPAID --发起支付--> UNPAID`。

这是**同状态**，本质是「预检 + 更新 `channel_payment_no`」，不应建模为状态转移。

→ **实现口径**：状态机只负责**状态变更**（5 条真实转移）；「发起支付」的前置校验与字段更新
由 `PaymentServiceImpl` 承担，不调状态机。

#### ⑥ `RemotePaymentService` 的退款入参形态与设计不同

设计 §6.1 只描述 `createRefund(RefundDTO)`；契约实际提供了**两个**退款入口：

| 契约方法 | 路径 | 用途 |
|------|------|------|
| `createRefund(RefundDTO)` | `POST /inner/payment/refunds` | 传入完整 `paymentNo`（内部/管理端用） |
| `refundByOrderNo(orderNo, refundAmount, afterSaleId)` | `POST /inner/payment/refunds/by-after-sale` | **mall-order 售后主用** —— 订单侧拿不到 `paymentNo`，由 payment 内部解析 |

→ 两个都要实现，`refundByOrderNo` 内部先 `orderNo → paymentNo` 解析再复用主流程。

---

## 1 批次划分

| 批次 | 内容 | TDD | 状态 |
|:---:|------|:---:|:---:|
| 1 | 数据层：5 DO + 5 Mapper | 无需 | ✅ 完成（10 文件，编译通过） |
| 2 | 状态机：支付单 6 条真实转移 + 退款单 3 条 | ✅ | ✅ 完成（23 例全绿） |
| 3 | 渠道适配层：接口 + 工厂 + `MockPayAdapter` | ✅ | ✅ 完成（8 文件，7 例全绿） |
| 4 | Service：`PaymentService` / `RefundService` | ✅ | ✅ 完成（`PaymentService` 15 例 + `RefundService` 12 例） |
| 5 | `CallbackService`：回调验签分支 + nonce 去重 + 先落库再应答 | ✅ | ✅ 完成（18 例全绿，含 `MockPayAdapter` 解析） |
| 6 | 接口层：C 端 3 + inner 3 + callback 2 | ✅ | ✅ 完成（7 个端点 + 19 例全绿；端点 3 待确认） |
| 7 | Outbox + 定时任务（超时关单 / 补偿扫描） | ✅ | ✅ 完成（15 例全绿，模块累计 106 例） |

每批均走「主会话写测试（RED）→ 子 Agent 实现（GREEN）→ 主会话独立复跑验证」三步。

---

## 2 Batch 明细

### Batch 1: 数据层（无 TDD）

对 4 张业务表 + 共用 `mall_outbox` 建 DO/Mapper：

| DO | 对应表 | 关键点 |
|------|------|------|
| `MallPaymentDO` | `mall_payment` | `pay_amount` 为 `Long`（分）；`version` 乐观锁 |
| `MallRefundDO` | `mall_payment_refund` | 同上 |
| `MallPaymentChannelDO` | `mall_payment_channel` | `config_json` 为 `text` |
| `MallPaymentCallbackLogDO` | `mall_payment_callback_log` | `raw_body` 为 `text` |
| `MallOutboxDO` | `mall_outbox` | **与 mall-marketing 共用同一张表**（结构已在 payment 的 DDL 中定义） |

Mapper 关键方法（`@Update` 字段级定向更新，**禁止 `updateById` 整行写回**）：

| 方法 | SQL 要点 |
|------|------|
| `MallPaymentMapper.markPaid(paymentNo, channelPayStatus, version)` | `SET payment_status=1, pay_success_time=NOW(), version=version+1 WHERE payment_no=? AND payment_status=0 AND version=?` |
| `MallPaymentMapper.markClosed(paymentNo, version)` | `SET payment_status=3 ... WHERE payment_status=0 AND version=?` |
| `MallPaymentMapper.markRefunding(paymentNo, version)` | `SET payment_status=4 ... WHERE payment_status=1 AND version=?` |
| `MallPaymentMapper.updateChannelPaymentNo(paymentNo, channelPaymentNo)` | 发起支付后回填 |
| `MallRefundMapper.updateResult(refundNo, status, channelRefundStatus, version)` | 退款终态推进 |
| `MallPaymentCallbackLogMapper.markProcessed(id, processStatus, processResult)` | 回调处理结果回填 |

> 状态码用 `PaymentStatusEnum` / `RefundStatusEnum` 的 `getCode()`，**不要写字面量**
> （设计 §5.3 的注释就是写错字面量导致的）。

### Batch 2: 状态机（TDD）

`statemachine/` 下：

- `PaymentEventEnum`：`PAY_SUCCESS_CALLBACK` / `PAY_FAIL_CALLBACK` / `PAY_TIMEOUT_CLOSE` / `REFUND_START` / `REFUND_SUCCESS_CALLBACK` / `REFUND_FAIL_CALLBACK`
- `RefundEventEnum`：`REFUND_SUCCESS_CALLBACK` / `REFUND_FAIL_CALLBACK` / `RETRY_REFUND`
- `PaymentStateMachine`：

```
transition(paymentNo, PaymentEvent event)     // 支付单
refundTransition(refundNo, RefundEvent event) // 退款单
```

**支付单真实转移（5 条）**：

| 当前 | 事件 | 目标 | 前置 |
|------|------|------|------|
| UNPAID | PAY_SUCCESS_CALLBACK | PAID | — |
| UNPAID | PAY_FAIL_CALLBACK | FAILED | — |
| UNPAID | PAY_TIMEOUT_CLOSE | CLOSED | — |
| PAID | REFUND_START | REFUNDING | 有退款单 |
| REFUNDING | REFUND_SUCCESS_CALLBACK | REFUNDED | — |
| REFUNDING | REFUND_FAIL_CALLBACK | PAID | — |

**退款单转移（3 条）**：`PROCESSING→SUCCESS`、`PROCESSING→FAILED`、`FAILED→PROCESSING`

- 乐观锁 `UPDATE ... WHERE status=? AND version=?`，影响 0 行抛 `A0702`
- 未定义转移抛 `A0702`
- **不碰渠道 API，不写 Outbox**（遵循设计 §7.3）

- [x] **Step 1（RED）** —— ✅ 2026-10-03，实测 **23 例中 21 例红**（编译通过；未红的 2 例恰好是「期望 false」与「期望状态不变」，空壳行为碰巧符合）
- [x] **Step 2（GREEN）** —— ✅ 2026-10-03（子 Agent 实现，主会话逐行复核）
- [x] **Step 3（验证）** —— ✅ **实测 `Tests run: 23, Failures: 0, Errors: 0`，`BUILD SUCCESS`**

**实测转移矩阵（支付单 6 条 / 退款单 3 条）**：

| 当前 | 事件 | 目标 | 前置 | 后置 |
|------|------|------|------|------|
| UNPAID | PAY_SUCCESS_CALLBACK | PAID | — | 记 `paySuccessTime` |
| UNPAID | PAY_FAIL_CALLBACK | FAILED | — | — |
| UNPAID | PAY_TIMEOUT_CLOSE | CLOSED | **`expireTime` 已过** | — |
| PAID | REFUND_START | REFUNDING | — | — |
| REFUNDING | REFUND_SUCCESS_CALLBACK | REFUNDED | — | — |
| REFUNDING | REFUND_FAIL_CALLBACK | PAID | — | — |
| PROCESSING | REFUND_SUCCESS_CALLBACK | SUCCESS | — | 记 `refundSuccessTime` |
| PROCESSING | REFUND_FAIL_CALLBACK | FAILED | — | — |
| FAILED | RETRY_REFUND | PROCESSING | — | — |

**错误码**：未定义转移 / 状态码非法 → `A0702`；前置条件不满足 → `A0703`（与 `OrderStateMachine` 一致）。

**为何给 `PAY_TIMEOUT_CLOSE` 加过期前置**：定时任务按 `expire_time` 扫单，但若在应用层再加一道
「未过期不得关单」的闸门，可以防住「扫描间隔内刚创建的支付单被误关」这类竞态。

🔴 **与设计 §7.3 的实现偏差（已定，记入 §4）**：设计 §7.3 要求状态机
「内部使用 `MallPaymentMapper.selectByPaymentNo()` 查当前状态」并「乐观锁更新 …… 影响行数 = 0 抛 A0702」，
即**状态机持有 Mapper 并自行落库**。但项目既有的 `OrderStateMachine` / `CouponStateMachine`
都是**纯状态机**（类注释明确写「不操作 DB、不持有 Mapper、不管理事务」），落库由 Service 层 CAS 完成。
本次**按既有风格实现**以保持架构一致性，CAS 落库与「0 行抛 A0702」的职责移到 Service 层。

### Batch 3: 渠道适配层（TDD）

```
infrastructure/channel/
├── PaymentChannelAdapter.java   # invokePay / invokeRefund / queryBill
├── PayResult / RefundResult / ChannelBillResult   # 值对象
├── MockPayAdapter.java          # 本次唯一实现
└── PaymentChannelFactory.java   # 按 channelCode 选适配器
```

**模拟渠道的实现口径**（用户决策「适配层 + 模拟渠道」）：

- 复用 DDL 种子数据的 2 个渠道（`wechat` / `alipay`），**不改 DDL**
- `MockPayAdapter` 行为可预期：
  - `invokePay` → 返回固定结构的支付参数（`mockPayNo = "MOCKPAY" + 时间戳`），
    并提供一个**受控的回调触发入口**（本地联调时手动调 `/callback/payment/{channel}` 完成闭环）
  - `invokeRefund` → 返回 `PROCESSING`（由回调推进终态）
- 工厂按 `mall.payment.mock-enabled` 开关决定是否走模拟实现
  （**仅 dev 为 true**，生产接入真实 SDK 时置 false 并补 `WechatPayAdapter` / `AlipayAdapter`）

> 真实渠道适配器（微信 `wechatpay-java` / 支付宝 `alipay-sdk-java`）**本次不引入依赖**，
> 只保留接口与工厂扩展点。

- [x] **Step 1（RED）** —— ✅ 2026-10-03，实测 **7 例全红**（5 个 NPE + 2 个断言失败，编译通过）
- [x] **Step 2（GREEN）** —— ✅ 2026-10-03（子 Agent 实现，主会话逐行复核）
- [x] **Step 3（验证）** —— ✅ **实测 mall-payment `Tests run: 30, Failures: 0, Errors: 0`，`BUILD SUCCESS`**

**模拟渠道的实测行为（本地联调可按此构造回调报文）**：

| 动作 | 返回值 |
|------|------|
| `invokePay` | 渠道单号 = `"MOCKPAY" + paymentNo`（**确定性推导**）；`payParams` 含 appId/timeStamp/nonceStr/package/signType/paySign 六键，其中 `package = "prepay_id=" + 渠道单号`；渠道状态 `NOTPAY` |
| `invokeRefund` | 渠道退款单号 = `"MOCKREF" + refundNo`；退款状态 **`PROCESSING`**（非终态，等回调推进） |
| `queryBill` | `success=true` + 交易状态 `UNKNOWN`（模拟渠道无真实账单） |

**工厂路由**：`mock-enabled=true` → 返回**注入的同一实例** `MockPayAdapter`（测试用 `isSameAs` 断言钉死）；
`false` → 抛 `PAYMENT_SERVICE_ERROR(C0210)`。

> ⚠️ 配置键更正：原写 `mall.payment.channel.mock-enabled`，**实际落地为 `mall.payment.mock-enabled`**
> （`MallPaymentConfigProperties.mockEnabled`）。

### Batch 4: Service（TDD）

**`PaymentServiceImpl`**

- `createPayment(PayRequestDTO)`：
  1. **幂等**：`idempotent_key = userId + "_" + orderNo + "_" + channelCode`，
     DB 唯一约束；`DuplicateKeyException` 捕获 → 查已有单返回相同参数
  2. **订单校验**：Feign `RemoteOrderService.queryOrder(orderNo)` →
     非 `WAIT_PAY` 抛 `A0702`；`payAmount <= 0` 抛 `A0602`；`payExpireTime` 已过抛 `A0701`
  3. 创建支付单：`payment_no` 前缀 `PAY`，`payment_status = UNPAID`
  4. 路由渠道 → `channelAdapter.invokePay()`
  5. 回填 `channel_payment_no`、`notify_url`、`expire_time`（与订单一致）
  6. 返回支付参数

- `getPayment(userId, paymentId)`：**归属校验**（非本人 → `A0501`）

- [x] **Step 1（RED）: `PaymentServiceImplTest`** —— ✅ 2026-10-03，实测 **14 例全红**（10 失败 + 4 错误，编译通过）
- [x] **Step 2（GREEN）: `PaymentServiceImpl`** —— ✅ 2026-10-03（子 Agent 实现，主会话逐行复核）
- [x] **Step 3（验证）** —— ✅ **实测 mall-payment `Tests run: 44, Failures: 0, Errors: 0`，`BUILD SUCCESS`**（30 + 14）

**实测行为**：

- **幂等优先级最高**：先按幂等键查已有支付单，命中则复用且**完全跳过订单校验**（含那次 Feign 调用）
- **订单校验顺序**：不存在 → `A0701`；非 `WAIT_PAY` → `A0702`；已过期 → `A0702`；金额非正 → `A0602`
- **渠道路由成功后才落库**：`invokePay` 失败时抛 `C0210` 且**不产生支付单**（有专测 `verify insert never`）
- 金额与过期时间均取**订单快照**（不重算优惠）；`notify_url = callbackBaseUrl + /callback/payment/{channel}`
- 查询支付单：「不存在」与「非本人」返回**同一** `A0501`，不泄露归属

🔴 **与设计 §4.2 的偏差**：设计第 ④ 条把「订单已过期」写成 `A0701`（`ORDER_NOT_FOUND`，意为「订单不存在」），
**语义不符**（过期 ≠ 不存在），且会让排查方向完全跑偏。本次实现改用 **`A0702`**（订单状态异常）。

- [x] **归属校验已补**（2026-10-03，用户批准）—— ✅ RED 阶段 15 例中**恰 1 例红**（新增的
  `orderOwnedByAnotherUser`）→ 实现加校验 → **实测 `Tests run: 45, Failures: 0, Errors: 0`**

  **为何需要**：设计 §4.2 的校验清单里没有这一条，但缺了它，用户 A 可用用户 B 的 `orderNo`
  发起支付（A 自掏腰包替 B 付），且支付成功后 `mall:payment:paid` 会推进 B 的订单为已支付
  —— 属**未授权操作他人订单**。

  **实现位置**：`buildNewPayment` 中「订单不存在」之后、「状态校验」之前 ——
  **先判归属再判状态，连订单状态都不透露**。「非本人」与「订单不存在」返回**同一** `A0501`，
  避免攻击者靠错误码差异探测他人订单是否存在。

**`RefundServiceImpl`**

- [x] **Step 1（RED）: `RefundServiceImplTest`** —— ✅ 2026-10-03，实测 **12 例全红**（7 失败 + 5 错误，编译通过）
- [x] **Step 2（GREEN）: `RefundServiceImpl`** —— ✅ 2026-10-03（子 Agent 实现，主会话逐行复核）
- [x] **Step 3（验证）** —— ✅ **实测 mall-payment `Tests run: 57, Failures: 0, Errors: 0`，`BUILD SUCCESS`**（45 + 12）

**实测行为**：

- **幂等键 `afterSaleNo_channelCode`**，命中则直接复用返回，**不做任何渠道交互**（连渠道配置都不读）
- 校验顺序：支付单不存在 → `A0501`；退款金额非正 / 累计超额（**含 PROCESSING**）→ `A0602`；
  支付单非可退款态 → `A0702`（由状态机抛）
- **校验与渠道调用都通过后才落库**：渠道 `success=false`（请求未送达）抛 `C0211` 且**不落退款单**
- 渠道**即时成功** → 退款单 `SUCCESS` + 支付单 `REFUNDED`（同步推进两套状态 + 回写渠道退款单号）
- `refundByOrderNo` 先按 `selectPaidByOrderNo` 解析「已支付」支付单，再复用主流程；
  `afterSaleId` 字符串化后充当 `afterSaleNo` 参与幂等键

🔴 **一个被测试反向扭曲的实现顺序（已修正）**：初版实现把「读渠道配置」放在了幂等判断**之前**。
根因是我的 `idempotentReusesExisting` 用例多打了一个 `channelMapper` 的桩，而 STRICT_STUBS 下
无用桩会报错 → 实现被迫去调用它。
**修正**：删掉多余桩，并显式加 `verify(channelMapper, never()).selectByChannelCode(...)`
把「幂等短路先于一切渠道交互」钉死，实现随之调整。

> ⚠️ **教训**：Mockito 的 STRICT_STUBS 不只防「漏桩」，也会因**多余的桩**反向塑造实现结构。
> 桩要精确到「实现真的会走到的分支」，否则会把实现带歪。

**其他取舍**（子 Agent 报告）：渠道「明确 FAILED」分支测试未覆盖，实现按语义补了
`markFailed` + `revertToPaid`（避免退款单卡在 PROCESSING）；`markRefunding` 的 CAS 返回值未校验
（并发下的简化，有幂等键兜底）。

- `createRefund(RefundDTO)`：
  1. 前置：支付单 `PAID`；`累计已退款 + 本次 ≤ pay_amount`；`idempotent_key = afterSaleNo + "_" + channelCode` 去重
  2. 建退款单：`refund_no` 前缀 `REF`，`refund_status = PROCESSING`
  3. `channelAdapter.invokeRefund()`
  4. 即时 `SUCCESS` → 推进终态 + 写 Outbox；`PROCESSING` → 等回调

- `refundByOrderNo(orderNo, refundAmount, afterSaleId)`：先解析 `orderNo → paymentNo`，
  再复用 `createRefund` 主流程

### Batch 5: CallbackService（TDD）

> **前置修复（2026-10-03，开始本批前完成）**：发现一个阻塞项 —— `RefundServiceImpl` 原本只在退款
> **成功**时才写 `channel_refund_no`，而渠道退款回调**只携带渠道侧退款单号**，
> 导致处于 PROCESSING 的退款单在收到回调时**无法被定位**。
>
> 已补 `MallRefundMapper.updateChannelRefundNo` 并在「渠道受理后」立即回填。
> ⚠️ 该方法**刻意不改 `version`**：它只是字段回填而非状态推进，若递增 version
> 反而会让后续 `markSuccess` 的 CAS 条件失配。
>
> TDD：RED 阶段 13 例中**恰 1 例红** → GREEN 后 **58/58 全绿**。

`processPayCallback(channel, rawBody)`：

- [x] **Step 1（RED）** —— ✅ 2026-10-03，实测 **23 例中 18 例红**（MockPayAdapter 新增 5 例 + CallbackService 13 例全红）
- [x] **Step 2（GREEN）** —— ✅ 2026-10-03（子 Agent 实现，主会话逐行复核 + 修掉 1 个真实缺陷与 2 处测试错误）
- [x] **Step 3（验证）** —— ✅ **实测 mall-payment `Tests run: 76, Failures: 0, Errors: 0`，`BUILD SUCCESS`**

🔴 **修复的真实缺陷**：回调只携带**渠道**支付单号，而 `MallPaymentMapper` 当时**没有**
「按渠道单号查支付单」的方法，实现被迫用按**本地**单号查的方法去接它 ——
**生产环境支付回调永远查不到支付单**。已补 `selectByChannelPaymentNo(channelPaymentNo)`。

> ⚠️ **为何这个缺陷能躲过测试**：Mockito 对任意入参都能打桩，
> **方法语义错了照样绿**。Mock 测试无法发现「用错了方法」这类问题，跨边界的字段映射须人工核对。

**实测回调处理顺序（支付）**：

```
parse → insert(callback_log) → 验签分支 → nonce SETNX → selectByChannelPaymentNo
→ 金额比对 → 状态机推进 + markPaid(CAS) → markProcessed → 应答
```

- **日志先于状态推进**（`InOrder` 专测）
- **金额与本地支付单不符即拒**（防回调篡改）
- **CAS 0 行 = 幂等成功**（渠道会重复回调）
- **验签失败不抛业务异常** → `CallbackResult.success=false` 由 Controller 转 400
  （设计 §12：回调验签失败「无业务错误码」，平台只看 HTTP 码与应答体）
- nonce Key = `CacheConstants.Payment.CALLBACK + {channel}:{nonce}`，TTL 取 `nonce-ttl`

**原始步骤描述**：

1. 插入 `mall_payment_callback_log`（记 `raw_body`）
2. **适配器层验签**（见 §0.2 ②）→ 失败标记 `is_verified=2` 并返回 400
3. **nonce 防重放**：Redis `SETNX` `CacheConstants.Payment.CALLBACK + {channel}:{tradeNo}`，
   TTL 取 `mall.payment.callback.nonce-ttl`（默认 86400s）→ 命中直接返回 200
4. 按 `channelPaymentNo` 查支付单 → 状态机推进 `UNPAID → PAID`（CAS）
5. **同一事务写 Outbox** `mall:payment:paid`
6. 返回渠道要求的成功应答（微信 XML / 支付宝纯文本）
7. 回填 `callback_log.process_status`

`processRefundCallback(channel, rawBody)`：同结构，幂等键 `REFUND_CALLBACK`，
终态推进后写 `mall:refund:succeeded` / `mall:refund:failed`。

**关键约束**：**先落库再应答**（设计 §5.3）—— 不能先 200 再落库。

### Batch 6: 接口层（TDD）

| # | 方法 | 路径 | 说明 | 状态 |
|---|------|------|------|:---:|
| 1 | POST | `/api/payment/payments` | 发起支付（C 端 token，userId 从 `X-User-Id` 取） | ✅ |
| 2 | GET | `/api/payment/payments/{paymentId}` | 查询支付单 | ✅ |
| 3 | POST | `/api/payment/refunds` | 发起退款（C 端） | ⏸ **待确认**，见下 |
| 4 | POST | `/callback/payment/{channel}` | 支付回调（**无认证，验签**） | ✅ |
| 5 | POST | `/callback/payment/{channel}/refund` | 退款回调（**无认证，验签**） | ✅ |
| 6 | POST | `/inner/payment/refunds` | inner：createRefund | ✅ |
| 7 | POST | `/inner/payment/refunds/by-after-sale` | inner：refundByOrderNo | ✅ |
| 8 | GET | `/inner/payment/status` | inner：getPaymentStatus | ✅ |

🔴 **两条信任边界必须区分**（同 marketing 的教训）：

- C 端接口的 userId **只能来自 `X-User-Id` 请求头**，请求体不得携带（否则归属校验形同虚设）
- inner 端点**返回裸对象**（不包 `MallResult`），路径必须与契约字符级一致，否则 Feign 404

**✅ 已完成（2026-10-03）**：3 个控制器共 7 个端点 + 19 个用例（`RefundServiceImplTest` 新增的
`getPaymentStatus` 3 例 + 3 个控制器测试类 12 例 + 原 4 例）。RED 阶段 12 例全红（编译通过）、
Service 侧 19 例全绿；GREEN 后 **mall-payment 全量 91 例全绿**，主会话独立复跑确认。

**实现要点（复核确认）**：

- `PaymentController` 的 userId 取自 `request.getHeader(X_USER_ID)`；`PayRequestDTO` 刻意不含
  `userId` 字段，从类型层面杜绝请求体伪造身份。
- `RemotePaymentInnerController` 三个方法均**直返裸对象**，测试用
  `jsonPath("$.code").doesNotExist()` / `jsonPath("$.data").doesNotExist()` 钉死。
- `PaymentCallbackController`：验签失败 → **HTTP 400** + 失败原因（不包 `MallResult`，设计 §12）；
  成功 → 200 + 平台应答体；`rawBody` **字节级原样透传**（测试断言 `isEqualTo`，任何裁剪都会破坏签名）。
- 🐛 **子 Agent 发现并修掉的坑**：`StringHttpMessageConverter` 在响应未声明 charset 时默认按
  **ISO-8859-1** 编码，会把中文失败原因（「验签失败」）写成 `????`。已显式声明
  `text/plain;charset=UTF-8`。
- `getPaymentStatus` 放在 `RefundService`（而非 `PaymentService`）：语义是「可退款状态快照」
  （含 `refundedAmount`，与超额校验同口径），且 `RefundServiceImpl` 已注入两个 Mapper，
  不需改动 `PaymentServiceImpl` 的构造器（其测试用 `new` 手写构造，改签名会波及已通过的 15 个用例）。
  `sumRefundedAmount` 返回 null 时归一为 0。

**⏸ 端点 3（`POST /api/payment/refunds` C 端发起退款）未实现，待确认**：

设计 §2.2 接口映射表列了这条 C 端端点，但**与 §6.1 冲突** —— §6.1 明确写
「退款发起（**Feign 触发**）：mall-order 审核售后通过后，调 Feign
`RemotePaymentService.createRefund(RefundDTO)`」，且 mall-order 侧确有 `AfterSaleController`
（`POST` 创建售后）。若 C 端可直接发起退款，等于**绕过售后审核**，且前端可自报
`refundAmount`，资金语义存疑。故暂不实现，待业务口径确认后再补（补的话需在 C 端边界
做支付单归属校验，`RefundDTO` 本身没有 userId）。

### Batch 7: Outbox + 定时任务（TDD）

- `infrastructure/outbox/OutboxPublisher` / `OutboxScheduler`：照 marketing 的副本实现
  （退避重试 `min(base * 2^retry, 120s) + random(0,5s)`，最多 3 次）
- ~~`infrastructure/mq/MqDedupGuard`~~：**本模块不需要** —— payment 只做生产者，
  没有 RocketMQ 消费者；回调侧的 nonce 去重已在 `CallbackServiceImpl` 用
  Redis SETNX 实现，无需第二套去重机制
- `schedule/PaymentTimeoutTask`：扫描 `payment_status = UNPAID AND expire_time < NOW()`
  → 置 `CLOSED`
- `schedule/PaymentCompensateTask` + `service/PaymentReconcileService`：
  扫描回调丢失的单，主动 `queryBill` 对账，渠道确认已收款则补记 `PAID` + 补发事件

> ⚠️ `@Scheduled.fixedDelay` 单位是**毫秒**而配置项是**秒**，必须做换算
> （用 `"...:3600}000"`，仅适用于整数秒配置）。

**✅ 已完成（2026-10-03）**：mall-payment **106 例全绿**（91 + 15 新增）；
**全工程 9 个模块 591 例 0 失败**。

**实现要点**：

- **事件投递接线**（`CallbackServiceImpl`）：支付回调 CAS 命中后补发
  `mall:payment:paid`；退款成功/失败补发 `mall:refund:succeeded` / `mall:refund:failed`。
  **CAS 未命中时不补发** —— 状态已被并发方推进，再发事件属无谓的重复消费（有用例钉住）。
- **payload 用独立 DTO**（`dto/event/`）而非 DO：DO 含 `version` / `isDeleted` /
  `idempotentKey` 等内部字段，一旦进消息就成了对外契约（设计 §8.1 的字段约束）。
- **`userId` 取自支付单而非退款单**：退款单虽有 `userId` 字段，但该字段由谁写入不确定，
  支付单的 `userId` 才是权威来源。
- **两个定时任务的扫描窗口刻意不重叠**：补偿任务只处理**未过期**的单
  （`expire_time > NOW()` 且 `create_time < NOW() - 30min`），关单任务只处理**已过期**的单。
  这样避免「对账刚判定未支付 → 关单任务随即关闭 → 回调才到达」被两个任务同时放大。
- **关单不调渠道 API**：微信 Native / 支付宝当面付的订单过期后由渠道侧自动失效，
  本地置 `CLOSED` 只为保证本地状态一致。需要显式关单的渠道可在适配器补
  `closeOrder` 后接入。
- **`PaymentReconcileService` 单独成服务**：让「CAS 落库 + Outbox 落库」处于同一事务
  （`@Transactional` 在同类内自调用不生效，写在任务类里会被代理绕过）。
- **`compensateDelaySeconds` 走构造器参数**而非字段 `@Value`：便于单测直接传固定值。
- 启动类补 `@EnableScheduling` 与 `scanBasePackages` 含 `com.mall.common`
  （注册 `MallExceptionHandler`）；顺带更正类注释里写错的端口 9304 → **9305**。

> 💡 **本次踩的坑**：`PaymentReconcileServiceImplTest` 把 `queryBill` 的桩写成
> `queryBill(PAY_NO, null)`，而实现传的是从 `channelMapper` 查出的渠道对象（非 null）。
> Mockito 严格模式下参数不匹配报 `PotentialStubbingProblem`。
> **配合 `any(Class)` 而非 `null` 打桩**才能与真实调用对齐。

---

## 3 配置变更

| 位置 | 变更 | 状态 |
|------|------|:---:|
| `mall-payment/pom.xml` | 加 `rocketmq-spring-boot-starter`、`caffeine`、`spring-boot-starter-test` | ✅ |
| `mall-payment` 启动类 | 补 `@EnableScheduling` + `scanBasePackages` 加 `com.mall.common`；端口注释 9304 → 9305 | ✅ 2026-10-03 |
| `mall-common` `MqTopicConstants` | 补 `Payment.REFUND_FAILED`（见 §0.2 ④） | ✅ |
| Nacos `mall-payment-dev.yml` | 补 `mock-enabled: true`、`callback-base-url: http://localhost:8080` | ✅ 2026-10-03 |
| Nacos `ruoyi-gateway-dev.yml` | 新增 `/callback/**` 路由（StripPrefix=0）+ `security.ignore.whites` 补 `/callback/**` | ✅ 2026-10-03 |
| Nacos `application-dev.yml` | 无需变更（`name-server` 已在） | — |

### 3.1 回调端点的网关配置（2026-10-03 已执行）

变更前核查了两个过滤器的实际匹配范围，结论与初稿不同：

| 过滤器 | 处理范围 | 回调是否需要登记 |
|--------|----------|:---:|
| `AuthFilter`（管理端 JWT） | 全部路径，命中 `security.ignore.whites` 即放行 | ✅ **必须加** `/callback/**`，否则支付平台无 token → 401 |
| `MallAuthFilter`（C 端 JWT） | **只处理 `/api/**`**（源码第 97 行 `StringUtils.isMatch("/api/**", path)`） | ❌ 不需要，回调不走这条链 |

> ⚠️ 初稿写的「白名单补 `/callback/payment/**` 到 `mall.security.anonymous-paths`」是**错的** ——
> 那是 `MallAuthFilter` 的白名单，而它根本不处理 `/callback/**`，加进去是无效条目。
> 真正需要登记的是 `security.ignore.whites`。

变更后实测：路由表 14 条含 `mall-payment-callback`（`Path=/callback/**`、`StripPrefix=0`）；
`ignore.whites` 含 `/callback/**` 且 `/api/**` 仍在；`anonymous-paths` 未被改动（19 条）；
两份 YAML 均解析通过、`md5 = MD5(content)` 自洽。

SQL 与留档：`temp/nacos-config-backup-20261002/apply-payment-gateway-fix.sql`；
回滚：同目录 `rollback-config-fix.sql`（已扩展到 5 个 DataId，按 `MIN(nid)` 取最早留档）。

> 💡 **踩到的坑**：`his_config_info` 的**主键是 `nid`（自增）**，`id` 才对应 `config_info.id`。
> 首次执行时把行号写进了 `id` 列、`nid` 取了原表 id，撞上已存在的主键值而报
> `Duplicate entry '11'`。已修正为「`nid` 自增、`id` 取原表主键」。
> 另外同一 DataId 多次变更后会有多条留档，回滚必须显式取 `MIN(nid)`，
> 否则 `JOIN` 命中多行、更新结果取决于 JOIN 顺序。

---

## 4 待办与差异记录

| # | 事项 | 状态 |
|---|------|------|
| 1 | 设计 §5.3 `payment_status = 2 -- PAID` 笔误 | ✅ 已更正为 `1` |
| 2 | 设计 §3.5/§5.1 验签口径（网关 vs 适配器） | ⏸ 待批准改为「适配器层验签」 |
| 3 | 设计 §13 `InnerSignatureFilter` 自动注册的错误结论 | ⏸ 待更正 |
| 4 | `MqTopicConstants.Payment` 缺 `REFUND_FAILED` | ✅ 已补 |
| 5 | 真实微信/支付宝渠道适配器 | ⏸ 留接口，待商户资质 |
| 6 | 管理端渠道配置（AES-256-GCM） | ⏸ 不在本次范围 |
| 7 | `InnerSignatureFilter` 全模块未注册 | ⏸ 独立待办，本次不修 |
| 8 | 设计 §7.3 要求状态机持有 Mapper 自行落库，与既有纯状态机风格冲突 | ✅ 已按既有风格实现（见 Batch 2 说明） |
| 9 | `MallPaymentApplication` 类注释端口写 9304，实际为 9305 | ✅ 已更正为 9305 |
| 10 | 设计 §2.2 有 C 端 `POST /api/payment/refunds`，但 §6.1 写退款由 Feign（售后审核后）触发 | ⏸ **待业务确认**；暂未实现（见 Batch 6 说明） |
| 11 | 设计 §5.3 步骤 ② 写「验签交给网关/Filter」，但网关无法实现渠道 SDK 验签 | ✅ 已按「适配器层验签」实现（同 #2） |
| 12 | `his_config_info` 主键是 `nid` 而非 `id`（与直觉相反） | ✅ 已记入 §3.1，SQL 与回滚脚本均已修正 |
| 13 | 超时关单与补偿任务的扫描间隔/延迟（`timeout-scan-interval` 等）在设计和 Nacos 里都没有 | ✅ 用 `@Scheduled` 默认值兜底，不配也能跑 |
| 14 | 🔴 支付单已 `CLOSED` 后收到「支付成功」回调 —— 状态机无该转移 | ⏸ **待确认**，见 §4.1 |

### 4.1 🔴 待确认：已关闭的支付单收到支付成功回调

**场景**：用户在支付单过期的**临界时刻**完成付款，但本地关单任务已先把单置为 `CLOSED`，
随后渠道回调送达并对账为「已支付」。

**当前行为**：`PaymentStateMachine` 里 `CLOSED` **没有出边**，`PAY_SUCCESS_CALLBACK`
属未定义转移 → 抛 `A0702` → 回调接口返回 HTTP 400 → 平台重试 → 永远失败。
**结果是用户付了钱、本地却不认**，需要人工介入。

**已做的缓解**：

- 两个定时任务的扫描窗口不重叠（对账只处理未过期的单），把竞态窗口压到最小；
- 但对账任务本身有 30 分钟延迟（`compensate-delay`），窗口无法真正消除 ——
  这是分布式系统的固有竞态。

**建议方案（未实施，待批准）**：

1. **回调侧检测 + 告警**：`CallbackServiceImpl` 在支付回调分支里判断
   `payment.paymentStatus == CLOSED` 且回调为成功 → 记 `ERROR` 级日志
   （含 paymentNo / orderNo / 金额），返回成功应答避免平台无限重试。
   成本低，但不解决资金问题。
2. **自动退款**：检测到上述情况后，自动发起一笔全额退款（复用 `RefundService`），
   把"付了钱但订单关了"转成"钱退回用户"。
   这需要确定「以什么名义建退款单」——正常退款链路要求有 `afterSaleNo`，
   而这里没有售后单，得引入一个新的触发源。
3. **状态机补 `CLOSED → PAID` 转移**：直接接受这笔支付。
   但这会让「已关单」的订单又能被推进为已支付，需评估订单侧是否还能承接
   （订单可能也已超时关闭），风险最大。

我倾向 **方案 1 + 2 组合**（先告警、再自动退款），但这涉及资金语义与新触发源，
须经确认后再实现。
