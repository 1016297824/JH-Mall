# mall-marketing 模块实现计划

> **For agentic workers:** 步骤使用 checkbox（`- [ ]`）语法跟踪。

**Goal:** 按 6 个批次实现 mall-marketing 全部业务代码（数据层 → 券状态机 → 券定义与领券 → 试算引擎 → C 端接口与 inner 端点 → MQ 消费与定时任务），解除 mall-order 下单链路对营销服务的阻塞。

**Architecture:** 券记录状态机为无状态单例，是 `record_status` 变更的唯一入口；试算引擎只读不写、500ms 超时兜底，锁定才改状态；跨服务一致性沿用 mall-order 的 Outbox 可靠投递模式；券库存用「乐观锁 `version` + `remain_count>0`」防并发超领。

**Tech Stack:** Spring Boot 4.0.3, MyBatis-Plus, RocketMQ, Feign, Redis, Lombok

**基线文档:**
- `docs/design/14_mall-marketing详细设计.md`（473 行，本模块实现的唯一事实源）
- `docs/design/03_01_系统详细设计-数据库详细设计.md` §营销表（record_status 取值）
- `docs/design/03_04_系统详细设计-状态机详细设计.md`（券记录状态机）
- `docs/design/07_mall-api契约层设计.md` §3.6（Feign 契约）

**现状：** 模块仅有启动类 + 3 个 config（`MallMarketingApplication` / `MallMarketingConfigProperties` / `MarketingConfig` / `MybatisPlusConfig`）+ `bootstrap.yml`，无任何业务代码。DDL 已就绪（`db/mall-sql/V1.0.0__create_mall_marketing_tables.sql`，含 3 张种子优惠券），**无需新增迁移脚本**。

**上游依赖方已就位：** mall-order 已实现并已调用本模块 4 个 inner 端点（`OrderServiceImpl` 第 121/137/228 行），当前调用必然 404。本模块落地后该链路自动打通。

---

## 0 开工前必须定的 4 个问题

> 这 4 项会影响实现细节，其中 0.1 影响所有状态判断，必须先定。

### 0.1 ⚠️ `CouponRecordStatusEnum` 与设计文档差 1（**必修**）

| 来源 | AVAILABLE | LOCKED | USED | RELEASED | EXPIRED |
|------|:---:|:---:|:---:|:---:|:---:|
| 设计文档 14 §3.2/§5 + 03_01 §营销表 | **1** | **2** | **3** | **4** | **5** |
| `mall-common/enums/marketing/CouponRecordStatusEnum` | 0 | 1 | 2 | 3 | 4 |
| 实际 DDL `record_status ... DEFAULT 1` | — | — | — | — | — |

- 设计文档佐证：14 §5.1「`UPDATE SET record_status=2 ... WHERE record_status=1`」、§6「`record_status IN (1,4)` 扫描 → `SET record_status=5`」。
- 该枚举**全仓零引用**（`grep` 无任何使用点），修正无副作用。
- 附带：03_01 文档写 `NOT NULL DEFAULT 0`，实际 DDL 是 `DEFAULT 1`。若按「1=AVAILABLE」，DDL 的 1 正确，03_01 的描述是笔误（`DEFAULT 0` 在 1~5 映射下是非法状态）。

> **建议**：按 AGENTS.md「有冲突时以设计文档为准」，把枚举修正为 `AVAILABLE(1) … EXPIRED(5)`，并同步修正 03_01 的 DEFAULT 描述。此项属「常量类补全」，可先行。

### 0.2 ⚠️ `InnerSignatureFilter` 实际全模块未生效（**跨模块问题**）

- `InnerSignatureFilter` 位于 `mall-api` 的 `com.mall.api.infrastructure.security`，标注 `@Component`，注释自称「各模块无需额外配置」。
- 但：`mall-api` 无 `spring.factories` / `AutoConfiguration.imports`；7 个模块的 `scanBasePackages` 均为 `{com.mall.<module>, com.mall.common}`，**无一个包含 `com.mall.api`**；全仓无 `@ComponentScan`。
- 结论：`/inner/**` 端点**当前没有任何签名校验**，`X-Internal-*` 头校验链路是死代码。
- 影响面：不止 marketing，order / product / user / search 的 inner 端点同样裸奔。

> **建议**：本模块实现按「过滤器会生效」编写（不改设计），但**该缺陷单独开一项修复**，不混进本次开发——它要动 7 个模块的启动类。请明确：①本次一并修；②本次只记录、后续单独修。

### 0.3 mall-marketing 缺 RocketMQ 依赖与客户端配置（**必修**）

- `server/mall/mall-marketing/pom.xml` 无 `rocketmq-spring-boot-starter`（mall-order 已加 2.3.5）。
- 参照 mall-order 的 `461a00e` 提交教训：该 starter 的自动配置以 `rocketmq.name-server` + `rocketmq.producer.group` 为生效条件，缺一则**不创建任何 RocketMQ Bean**，`RocketMQTemplate` 注入失败 → 服务起不来。

> **2026-10-02 更正**：**不需要改 `bootstrap.yml`**。经排查确证：`spring-cloud-starter-bootstrap`
> 由 `server/ruoyi/pom.xml` 的 `<dependencies>` 继承到所有 mall 模块（fat jar 内可见），
> `bootstrap.yml` 一定被读取；同时 Nacos 全局 `application-dev.yml` 的 `rocketmq` 块也确实生效
> （证据：`mall-search` 的 bootstrap.yml 与模块级 Nacos 配置都没有 rocketmq，但其
> `SearchSyncConsumer` 正常运行）。因此 rocketmq 配置的**唯一来源应放 Nacos**，
> 与 AGENTS.md「配置唯一来源：Nacos 服务端」一致。原先加到 `bootstrap.yml` 的两项已**撤销**。

### 0.4 Caffeine 依赖缺失

- 设计 §3.4 要求「促销规则匹配结果缓存：同一 `promotionId + totalAmount` 1 分钟内不重复计算（Caffeine 本地缓存）」。
- `mall-marketing/pom.xml` 无 caffeine（`mall-product` / `mall-search` 已有）。

> **建议**：加依赖，与 mall-product、mall-search 保持一致（版本随父 pom 管理）。

---

## 1 文件清单

### Batch 1: 数据层（无 TDD）

| 文件 | 说明 |
|------|------|
| `DO/MallCouponDO.java` | `mall_marketing_coupon` |
| `DO/MallCouponRecordDO.java` | `mall_marketing_coupon_record` |
| `DO/MallPromotionDO.java` | `mall_marketing_promotion` |
| `DO/MallPromotionRuleDO.java` | `mall_marketing_promotion_rule` |
| `DO/MallOutboxDO.java` | `mall_outbox`（与 mall-order 同表，各模块各自持有 DO/Mapper，沿用现有模式） |
| `mapper/MallCouponMapper.java` | 含 `@Update` 乐观锁扣减/回补 |
| `mapper/MallCouponRecordMapper.java` | 含 `@Update` 按 orderNo 批量释放 |
| `mapper/MallPromotionMapper.java` | |
| `mapper/MallPromotionRuleMapper.java` | |
| `mapper/MallOutboxMapper.java` | |

### Batch 2: 券记录状态机（TDD）

| 文件 | 说明 |
|------|------|
| `statemachine/CouponStateMachine.java` | 5 状态 5 转移，`transition()` 唯一入口 |
| `statemachine/CouponEventEnum.java` | 锁定 / 支付成功 / 订单取消 / 过期 |
| `test/.../CouponStateMachineTest.java` | 纯逻辑单测，仿 `OrderStateMachineTest` |

### Batch 3: 券定义与领券（TDD）

| 文件 | 说明 |
|------|------|
| `service/CouponDefService.java` + `impl/CouponDefServiceImpl.java` | 可领券列表 / 创建 / 修改 / 删除（软删+废弃） |
| `service/CouponClaimService.java` + `impl/CouponClaimServiceImpl.java` | 领券 / 锁券 / 核销 / 释放 / 我的券 / 过期批处理 |
| `service/PromotionService.java` + `impl/PromotionServiceImpl.java` | 进行中活动查询 |
| `DTO/request/` `DTO/response/` | `CreateCouponReq` / `CreatePromotionReq` / `CreateRuleReq` / `CouponDefResp` / `CouponRecordResp` |
| `convert/response/CouponConvert.java`、`CouponRecordConvert.java` | DO → Resp 静态转换 |
| `convert/request/CouponRequestConvert.java` | Req → DO |

### Batch 4: 试算引擎（TDD）

| 文件 | 说明 |
|------|------|
| `service/PromotionRuleMatcher.java` | 规则匹配（门槛 + 适用范围 + 时间段；互斥/叠加） |
| `service/CalculationService.java` + `impl/CalculationServiceImpl.java` | 两阶段过滤 + 组合枚举 + 择优 + 500ms 兜底 |

### Batch 5: C 端接口 + inner 端点（TDD）

| 文件 | 说明 |
|------|------|
| `controller/CouponController.java` | `/api/marketing/coupons` 3 个端点 |
| `controller/PromotionController.java` | `/api/marketing/promotions` 1 个端点 |
| `controller/CalculationController.java` | `/api/marketing/calculations` 1 个端点 |
| `controller/inner/RemoteMarketingInnerController.java` | 4 个 `/inner/marketing/**` 端点 |

### Batch 6: Outbox + MQ + 定时任务

| 文件 | 说明 |
|------|------|
| `infrastructure/outbox/OutboxPublisher.java` | 写 `mall_outbox`（`mall:coupon:used`） |
| `infrastructure/outbox/OutboxScheduler.java` | 扫描 NEW/FAILED 投递、退避重试 |
| `infrastructure/mq/OrderPaidConsumer.java` | `mall:order:paid` → 核销 |
| `infrastructure/mq/OrderCancelledConsumer.java` | `mall:order:cancelled` → 释放 + 回补库存 |
| `infrastructure/mq/MqDedupGuard.java` | Redis SETNX 幂等（键 `mall:mq:dedup:{messageId}:mall-marketing`） |
| `infrastructure/schedule/CouponExpireTask.java` | 每小时扫 `record_status IN (1,4) AND expire_time < NOW()` |

> **Outbox 基础件复用决策**：mall-order 的 `OutboxPublisher` / `OutboxScheduler` / `MallOutboxDO` / `MallOutboxMapper` 是**模块私有**的。本模块建议**照抄一份同构副本**（不动 mall-order，避免跨模块重构风险）；是否上提到 `mall-common` 作为公共件，另议。

---

## 2 Batch 明细

### Batch 1: 数据层

- [x] **Step 1: 5 个 DO** —— ✅ 2026-10-02，字段严格对齐 DDL，金额一律 `Long`（分）
- [x] **Step 2: 5 个 Mapper** —— ✅ 2026-10-02

| Mapper 方法 | 写法 | 说明 |
|------|------|------|
| `MallCouponMapper.decreaseRemainCount(id, version)` | `@Update` | `SET remain_count=remain_count-1, version=version+1 WHERE id=#{id} AND version=#{version} AND remain_count>0` |
| `MallCouponMapper.increaseRemainCount(id)` | `@Update` | `SET remain_count=remain_count+1 WHERE id=#{id}` |
| `MallCouponRecordMapper.lockById(id, orderNo)` | `@Update` | `SET record_status=2, order_no=?, lock_time=NOW() WHERE id=? AND record_status=1` |
| `MallCouponRecordMapper.markUsedById(id, orderNo)` | `@Update` | `SET record_status=3, use_time=NOW() WHERE id=? AND order_no=? AND record_status=2` |
| `MallCouponRecordMapper.releaseById(id, orderNo)` | `@Update` | `SET record_status=4, order_no=NULL, release_time=NOW() WHERE id=? AND order_no=? AND record_status=2` |
| `MallCouponRecordMapper.expireBatch(batchSize)` | `@Update` | `SET record_status=5 WHERE record_status IN (1,4) AND expire_time < NOW() LIMIT #{batchSize}` |

> 与计划的差异：原计划用 `releaseByOrderNo` 批量释放，实际改为**按记录逐条 CAS**
> （`releaseById`）——设计 §5.3 要求「逐条 `LOCKED → RELEASED`」，且逐条才能
> 精确回补每张券对应的 `remain_count`。

- [x] **Step 3: 确认 `MybatisPlusConfig` + `@MapperScan("com.mall.marketing.mapper")` 可用** —— ✅ 已就绪

### Batch 2: 券记录状态机（TDD）

- [ ] **Step 1（RED）: 写 `CouponStateMachineTest`** —— 覆盖设计 §3.3 转移矩阵全部条目：

| 当前状态 | 事件 | 目标 | 后置动作 |
|---------|------|------|---------|
| AVAILABLE | LOCK | LOCKED | 记录 `order_no` + `lock_time` |
| LOCKED | PAY_SUCCESS | USED | 记录 `use_time` |
| LOCKED | ORDER_CANCEL | RELEASED | 清 `order_no`、回补 `remain_count`（由 Service 层执行） |
| AVAILABLE | EXPIRE | EXPIRED | — |
| RELEASED | EXPIRE | EXPIRED | — |
| 其余组合 | 任意 | 抛异常 | — |

  运行确认失败（RED）。
- [x] **Step 2（GREEN）: 实现 `CouponEventEnum` + `CouponTransition` + `CouponStateMachine`** —— ✅ 2026-10-02；未定义转移抛 A0702，前置条件不满足抛 A0612
- [x] **Step 3（验证）: 主会话跑 `mvn test -Dtest=CouponStateMachineTest` 全绿** —— ✅ **2026-10-02 实测通过**：`Tests run: 15, Failures: 0, Errors: 0`，`BUILD SUCCESS`（11.3s）。mall-api / mall-common / mall-marketing 三模块编译均通过。
  - 命令：`mvn -f server/mall/pom.xml -pl mall-marketing -am -Dtest=CouponStateMachineTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - ⚠️ 必须带 `-Dsurefire.failIfNoSpecifiedTests=false`：`-am` 会连带构建 mall-common，那里没有匹配的测试类，surefire 3.1.2 会以 `No tests matching pattern` 报错中断（旧的 `-DfailIfNoTests=false` 已不生效）

> **状态机职责边界（已确定）**：状态机不感知 `orderNo`，因此在 `LOCK` 场景下
> **由 Service 层先写入 `record.orderNo` 再调用 `transition`** —— 与 mall-order
> 「先缓存 `originStatus` 再转移」的做法同源。`ORDER_CANCEL` 的后置动作会把
> `orderNo` 置空，Service 层必须**先取出 orderNo** 再转移，否则落库 CAS 条件丢失。

### Batch 3: 券定义与领券（TDD）

- [x] **Step 1.a（RED）: `CouponClaimServiceImplTest`** —— ✅ 2026-10-02，实测 **25 个用例全红**（17 断言失败 + 8 `UnnecessaryStubbing`，编译通过）
- [x] **Step 2.a（GREEN）: `CouponClaimServiceImpl`** —— ✅ 2026-10-02（子 Agent 实现，主会话逐行复核）
- [x] **Step 1.b（RED）: `CouponDefServiceImplTest`** —— ✅ 2026-10-02，实测 **25 个用例全红**（编译通过）
- [x] **Step 2.b（GREEN）: `CouponDefServiceImpl`** —— ✅ 2026-10-02（子 Agent 实现，主会话逐行复核并修正一处并发缺陷）
- [x] **Step 1.c（RED）: `PromotionServiceImplTest`** —— ✅ 2026-10-02，全量跑出 **83 个用例中恰有 3 个红**，全部落在该类（其余 80 绿）
- [x] **Step 2.c（GREEN）: `PromotionServiceImpl`** —— ✅ 2026-10-02。纯委托（`selectActive` → `PromotionConvert`），**未再起子 Agent**——该规则目的在于避免主会话把实现思路喂给子 Agent，纯委托无实现可隐藏
- [x] **Step 3（验证）: 测试全绿** —— ✅ **实测 mall-marketing `Tests run: 83, Failures: 0, Errors: 0`，`BUILD SUCCESS`**（上游 mall-api 38 + mall-common 16 同时全绿，证明枚举改动未波及他人）
- [x] **补充: Convert 层测试** —— ✅ 2026-10-02，补齐计划中「Converter ✅ TDD」的缺口，新增 `CouponConvertTest` / `CouponRequestConvertTest` / `PromotionConvertTest`（各 5 例，共 15 例）。其中 `CouponRequestConvertTest` 钉死了「修改时不得触碰 `remainCount` / `couponStatus` / `version` / `createTime`」这条库存一致性不变式

**Batch 3 用例分布（83 例）**：券状态机 15 ｜ 领券服务 25 ｜ 券定义服务 25 ｜ 活动服务 3 ｜ Convert 15

**实现要点（复核确认）**：
- `lockCoupon` 返回 `boolean` 契约：记录不存在抛 A0501；已占用/已过期等不满足条件**返回 false**（内部捕获状态机抛出的 `BusinessException` 转换），由 mall-order 侧转 A0612。`orderNo` 在 `transition` **之前**写入记录（状态机不感知订单号）。
- `releaseCoupon` **必须先缓存 `orderNo` 再 `transition`**——`ORDER_CANCEL` 的后置动作会清空 `orderNo`，否则 CAS 落库条件丢失。
- 库存回补**以 CAS 实际影响行数为准**（`releaseById(...) > 0` 才 `increaseRemainCount`），避免 MQ 重复投递导致重复回补。
- 🔴 **新增安全约束**：`validateCoupon` 对「券不存在」与「非本人」返回**同一错误码 A0501**，避免泄露归属信息。这是本轮发现的下单越权漏洞的营销侧防线，详见 §6 差异记录 #7。
- 🔴 **券定义服务禁止整行写回**（主会话复核时发现并修正）：`CouponDefServiceImpl` 原用 `updateById(existing)`，会把查询出来的整行字段（含 `remain_count`）一并写回。已发布的券随时可能被领取，**「管理端改名/废弃」与「用户领券」交叉执行时，会用陈旧快照覆盖刚扣减的库存 → 超发**。已改为字段级定向更新 `updateNameById` / `updateStatusById`，并新增断言「`updateById` 必须不被调用」。草稿态无此风险（草稿不可被领取），仍走 `updateById` 全字段更新。
- 券定义校验错误码分两类：**必填缺失 → `PARAM_MISSING`(A0401)**、**取值非法 → `PARAM_INVALID`(A0402)**。

> `coupon_code` 生成：`CPN` + `System.currentTimeMillis()` + 6 位随机（`ThreadLocalRandom`），依赖 `uk_coupon_code` 唯一索引兜底。
>
> 领券并发防护采用**乐观锁**（同设计 §4），**不**引入 `CacheConstants.Marketing.COUPON_LOCK` 分布式锁——该 Key 设计上属于 `orderNo` 维度、用于下单锁券防并发，与领券无关。

> `coupon_code` 生成：`CPN` + `System.currentTimeMillis()` + 6 位随机（`ThreadLocalRandom`），依赖 `uk_coupon_code` 唯一索引兜底。
>
> 领券并发防护采用**乐观锁**（同设计 §4），**不**引入 `CacheConstants.Marketing.COUPON_LOCK` 分布式锁——该 Key 设计上属于 `orderNo` 维度、用于下单锁券防并发，与领券无关。

### Batch 4: 试算引擎（TDD）

## 🔴 开工前已定的两条业务口径（2026-10-02 用户确认）

> 设计 §3.4 要求对候选券做 **2^N 全组合搜索**（即允许一笔订单叠加多张券），
> 但契约层是**单值**的：`CreateOrderRequest.couponRecordId` 是 `Long`、
> `RemoteMarketingService.lockCoupon(orderNo, couponClaimId)` 一次只锁一张、
> `OrderServiceImpl` 也只在非空时调一次。若引擎自动选出多张券，mall-order 会把
> **多张券的优惠算进 `pay_amount` 却只锁 0~1 张券** —— 优惠送出去、券未占用未核销。
> 该矛盾由三个文件对读得出，非推测。

| 问题 | 结论 |
|------|------|
| 一笔订单最多用几张券 | **最多 1 张**，取优惠最大者。与契约单值口径一致，不会出现「优惠算了但券没锁」。2^N 枚举因此退化为「挑最优单张」 |
| 券与促销能否同时享用 | **可叠加，相加**：`finalAmount = max(0, 原价 − 券优惠 − 促销优惠)`，与设计 §3.4 返回的四个金额字段一致 |

> 若将来要支持多券叠加，需**同步**改造 mall-order 为逐张锁券，否则会重现上述资金漏洞。

- [x] **Step 1.a（RED）: `PromotionRuleMatcherTest`** —— ✅ 2026-10-02，实测 **18 个用例全红**（编译通过）
- [x] **Step 2.a（GREEN）: `PromotionRuleMatcher`** —— ✅ 2026-10-02（子 Agent 实现，主会话逐行复核）
  - 语义：仅「所属活动在候选列表内」的规则参与；未命中的规则**不参与**互斥择优；互斥规则只留优惠最大者（并列取 `sort_order` 更小）；非互斥全部保留；输出保持输入顺序；免邮命中但优惠恒为 0
  - 缓存：注入 `MarketingConfig#promotionMatchCache`，**键 = 订单金额 + 活动 ID 集合（升序）**，故金额不同或活动上下线都不会读到脏数据
  - 🔴 复核修正：`doMatch` 原返回可变 `ArrayList`，而该列表会被缓存并原样交给调用方 —— **调用方一旦改动就会污染缓存**，已改为 `List.copyOf(result)` 返回不可变副本
- [x] **Step 1.b（RED）: `CalculationServiceImplTest`** —— ✅ 2026-10-02，实测 **24 个用例全红**（7 断言失败 + 17 error，编译通过）
- [x] **Step 2.b（GREEN）: `CalculationServiceImpl`** —— ✅ 2026-10-02（子 Agent 实现，主会话逐行复核）
  - 促销侧：查进行中活动 + 规则 1 次后交给 `PromotionRuleMatcher`（缓存由其内部处理）
  - 券侧：自动择优按 `min_order_amount <= 原价` 过滤 → 逐张算折扣 → **取优惠最大的一张**；面值取**券记录快照**而非定义当前值
  - 🔴 **越权归属校验已落地**：指定券时校验归属 `userId`，「券不存在」与「非本人」返回**同一错误码 A0501**，不泄露归属
  - 🔴 **指定券不可用时按状态分别抛错**：`USED`→A0613、`EXPIRED`/已过有效期→A0610、`LOCKED`/`RELEASED`/定义缺失/门槛不满足→A0612。**门槛不满足必须抛错让下单失败**，不能返回 0 折扣（否则券被 mall-order 锁住却没抵扣）
  - 金额截断：券优惠 ≤ 原价；促销优惠 ≤ 原价 − 券优惠；应付不为负。**这样订单的 `discount_amount` 才等于 `total_amount − pay_amount`**，不会自相矛盾
- [x] **Step 3（验证）: 测试全绿** —— ✅ **实测 mall-marketing `Tests run: 125, Failures: 0, Errors: 0`**（上游 mall-api 38 + mall-common 16 同时全绿）

**Batch 4 用例分布（42 例）**：规则引擎 18 ｜ 试算引擎 24

### ⚠️ 实现中的两处偏差（已处理）

1. **配置项 `max-candidates` 不再使用**：该值（20）是为设计 §3.4 的「2^N 组合搜索」做候选剪枝设计的。改为**单一券择优**后，候选项少算几张会**漏选真正最优的券**，故改用固定安全兜底上限 `CANDIDATE_COUPON_LIMIT = 200`（纯防内存爆，非剪枝），并在代码里写明理由。
2. **配置项 `timeout`（500ms）未实现**：该兜底是为 2^N 枚举的超时快照设计的。单一券择优下计算复杂度为 O(n)，不存在超时风险，实现超时机制属无效复杂度。

> 上述两项使 `mall.marketing.calculation.max-candidates` 与 `timeout` 成为**无使用方的配置项**，
> 已记入 §6 差异记录，建议后续清理或改为单一券模型下的新语义。

### 🔴 主会话复核时发现自己测试存在自相矛盾（子 Agent 主动上报，未擅自改测试）

`promotionThresholdUsesOriginalAmount` 与 `finalAmountNeverNegative` 两个用例**输入完全相同**
（原价 10000、券优惠 8000、剩余 2000），仅促销原始值不同（3000 vs 5000），
却分别要求 `promotionDiscount` 为 **3000** 与 **2000** —— 即要求 `f(3000)=3000` 且 `f(5000)=2000`，
`f` 非单调，**任何「上限/截断」型实现都不可能同时通过**。

根因：前者本意是验证「促销门槛按**原价**判定」，却把券面值误设为 8000，导致剩余额度只剩 2000，
3000 的促销被截断，掩盖了它想验证的东西。
**修正**：把该用例的券面值改为 **1000**（剩余 9000 > 3000，不被截断），并补注释说明它为何能区分
「按原价判门槛」与「按券后金额判门槛」两种口径。修正后 24/24 全绿。

> 本批次是**金额正确性核心**，必须 TDD，禁止先写实现。

### Batch 5: C 端接口 + inner 端点（TDD）

- [x] **Step 1（RED）: Controller 测试**（MockMvc + Mock Service）—— ✅ 2026-10-02，实测 **15 个用例全红**（编译通过）
- [x] **Step 2（GREEN）: 实现 3 个 C 端 Controller + 1 个 inner Controller** —— ✅ 2026-10-02（子 Agent 实现，主会话逐行复核）

| # | 方法 | 路径 | 需登录 |
|---|------|------|:---:|
| 1 | GET | `/api/marketing/coupons` | 否 |
| 2 | POST | `/api/marketing/coupons/{couponDefId}/claims` | 是 |
| 3 | GET | `/api/marketing/coupons/claims` | 是 |
| 4 | GET | `/api/marketing/promotions` | 否 |
| 5 | POST | `/api/marketing/calculations` | 是 |

**inner 端点（路径必须与 `mall-api` 契约完全一致，否则 Feign 404）：**

| 方法 | 路径 | 签名 | 返回 |
|------|------|------|------|
| POST | `/inner/marketing/calculate` | `@RequestBody CalculationReq` | `CalculationResp` |
| POST | `/inner/marketing/coupon/lock` | `@RequestParam orderNo, couponClaimId` | `boolean` |
| POST | `/inner/marketing/coupon/release` | `@RequestParam orderNo` | `void` |
| GET | `/inner/marketing/coupon/validate` | `@RequestParam couponClaimId, userId` | `boolean` |

> inner 端点返回**裸对象**，不包 `MallResult`——与 `RemoteOrderInnerController` 保持一致（Feign 契约声明的是 `CalculationResp`，不是 `MallResult<CalculationResp>`）。

- [x] **Step 3（验证）: 测试全绿** —— ✅ **实测 mall-marketing `Tests run: 140, Failures: 0, Errors: 0`，`BUILD SUCCESS`**（上游 mall-api 38 + mall-common 16 同时全绿）

### 🔴 本批次的关键安全设计：C 端试算请求**刻意不含 userId**

`dto/request/CalculationRequest` **没有 `userId` 字段**，用户身份只能来自网关下发的
`X-User-Id` 请求头，Controller 取出后再拼装契约对象 `CalculationReq`。

**理由**：若允许请求体提供 userId，则 Batch 4 实现的「券归属校验」可被一行 JSON 绕过——
攻击者把 userId 填成受害者、再带上受害者的 `couponClaimId`，即可通过归属校验去抵扣别人的券。
`CalculationControllerTest#calculateIgnoresUserIdInBody` 专门往请求体塞 `"userId":999`
而请求头给 `100`，断言 Service 收到 **100**，把这个约束钉死。

> 注意「内部端点」是另一条信任边界：`/inner/marketing/calculate` 的 `userId` 由 mall-order
> 从下单用户传入，属于服务间可信调用，故内部端点不做此限制。

### 其他实现要点

- `limit` 参数归一化为闭区间 `[1, 50]`（下界兜底非法值，上界防大页查询）
- 内部端点 4 个方法**返回裸对象**，并有测试断言根级字段存在、`$.code`/`$.data` 不存在——
  防止有人「顺手」包一层 `MallResult` 导致 Feign 反序列化失败
- `lockCoupon` 的契约签名是 `(orderNo, couponClaimId)`，而 Service 签名是 `(couponClaimId, orderNo)`，
  Controller 负责换序；内部端点测试对此有断言

### ✅ Batch 5 完成后的集成状态

营销侧 4 个 inner 端点已就绪，`mall-order` 的下单链路（`calculate` → `lockCoupon` → 失败补偿
`releaseCoupon`）**在营销侧不再 404**。待 Batch 6 补齐 MQ 消费与定时任务后，
「取消释放」「支付核销」「过期」三条异步链路才能闭环。

### Batch 6: Outbox + MQ + 定时任务

- [x] **Step 1: Outbox 基础件** —— ✅ 2026-10-02，`OutboxPublisher`（写 `mall_outbox`，聚合类型 `COUPON`，payload 序列化失败抛异常让业务事务回滚）、`OutboxScheduler`（退避 10s/30s/60s + 0~5s 抖动，第 3 次失败置 FAILED）、`MqDedupGuard`（Redis SETNX，TTL 24h）
- [x] **Step 2: 两个消费者** —— ✅ 2026-10-02，`OrderPaidConsumer`（核销）、`OrderCancelledConsumer`（释放 + 回补）；统一经 `MqDedupGuard` 去重。二者都用 `MessageExt` 取 broker 的 `msgId`（重投时稳定，且不污染业务 payload）
- [x] **Step 3: 过期定时任务** —— ✅ 2026-10-02，`CouponExpireTask`，扫描间隔取配置 `expire-scan-interval`（**秒**）
- [x] **Step 4: 启动类改造** —— ✅ 2026-10-02，`MallMarketingApplication` 补 `@EnableScheduling` + `scanBasePackages` 加 `com.mall.common`（注册 `MallExceptionHandler`，与 mall-order 同理由）
- [x] **Step 5（补做）: `useCoupon` 接线 Outbox** —— ✅ 2026-10-02，Batch 3 时按计划把 Outbox 推迟到本批；本轮**先补测试见红**（27 个用例中恰 1 个红），再改实现见绿

**Batch 6 用例分布（26 例）**：Outbox 基础件 15 ｜ 消费者 8 ｜ 过期任务 3

### 两处实现细节值得记录

1. **`useCoupon` 只在 CAS 真正生效时写 Outbox**：`markUsedById` 影响 0 行说明券并未真正核销（竞态），此时若也写核销事实会留下**假账**。故 `affected == 0` 时 `continue`，并有用例专门断言「CAS 失败不得写 Outbox」。
2. **`CouponExpireTask` 的扫描间隔单位换算**：`@Scheduled.fixedDelay` 单位是**毫秒**，而配置项 `expire-scan-interval` 单位是**秒**。直接写 `fixedDelayString = "${...:3600}"` 会被当成 3600ms（**3.6 秒扫一次**），故写成 `"...:3600}000"` 完成换算。**该写法仅适用于整数秒配置**，已写入代码注释。

> **事件链路核对（已验证上游真实存在，非推测）**：mall-order `OrderServiceImpl` 第 406/388 行分别经 Outbox 发布
> `mall:order:paid` / `mall:order:cancelled`；`OrderTimeoutConsumer` 第 109 行在超时关单后同样补发 `mall:order:cancelled`。
> 故「支付核销」与「取消/超时释放」两条链路的事件源都存在，本模块两个消费者各自对得上。

---

## 3 约束与规范

| 规约 | 说明 |
|------|------|
| 状态变更 | **必须**经 `CouponStateMachine.transition()`，禁止 Service 直接 update `record_status` |
| Mapper | SELECT 用 `LambdaQueryWrapper`；UPDATE 用 `@Update`；算术运算必须用（设计 §2.4） |
| 金额 | 一律 `Long`，单位**分**，禁止 `double` / `BigDecimal` 参与运算 |
| 错误处理 | 抛 `BusinessException(ErrorCode.XXX)`，不走 `return MallResult.error()` |
| 消息 Payload | 禁止序列化 DO，字段 lowerCamelCase，金额单位为分 |
| Lombok | DO 用 `@Data`；Service/Controller/Consumer 用 `@Slf4j` + `@RequiredArgsConstructor` |
| 配置读取 | 经 `MallMarketingConfigProperties` 构造注入，**禁止 `@Value`** |
| 编码规范 | 阿里巴巴 Java 开发手册·嵩山版；改文件用最小行级替换，禁止整文件覆写 |

---

## 4 验证方式

| 批次 | 验证方式 |
|------|----------|
| Batch 1 | `mvn compile` 通过 |
| Batch 2 | `mvn test -Dtest=CouponStateMachineTest` 全绿 |
| Batch 3 | 领券测试全绿；启动后 `POST /api/marketing/coupons/1/claims` 领券成功、`remain_count` 自减 |
| Batch 4 | 试算测试全绿；`POST /api/marketing/calculations` 返回合理优惠组合 |
| Batch 5 | inner 端点用 `curl` 带 `X-Internal-*` 头调通（若 §0.2 已修） |
| Batch 6 | 下单 → 取消，观察券 `record_status` 2→4 且 `remain_count` 回补；支付成功观察 2→3 |

**跨模块端到端验证（本模块完成后的验收标准）：**
1. 领券 → 加购 → 下单：`OrderServiceImpl` 的 `calculate` / `lockCoupon` 不再 404，订单 `discount_amount` 与券优惠一致
2. 取消订单 → `mall:order:cancelled` → 券释放 + 库存回补
3. 支付成功 → `mall:order:paid` → 券核销

---

## 5 配置变更

> **2026-10-02 已实测核对**（数据源：Nacos 外部存储 `ry-config.config_info`，
> 见 AGENTS.md「查看配置用 MySQL 或 Nacos 控制台」）。原先凭设计文档推断的两处**判断有误**，已在下方更正。

### 5.1 代码侧（已完成）

| 位置 | 变更 | 状态 |
|------|------|:--:|
| `server/mall/mall-marketing/pom.xml` | 加 `rocketmq-spring-boot-starter` 2.3.5 | ✅ |
| `server/mall/mall-marketing/pom.xml` | 加 `caffeine` | ✅ |
| ~~`server/mall/mall-marketing/src/main/resources/bootstrap.yml`~~ | ~~补 `rocketmq.name-server` + `rocketmq.producer.group`~~ | ❌ **已撤销**：Nacos 全局配置已提供 `name-server`，producer group 已落在 `mall-marketing-dev.yml`；`bootstrap.yml` 恢复为标准模板（与 `mall-search` 结构一致），避免双源歧义 |

### 5.2 Nacos 侧（待用户确认后执行）

| 位置 | 变更 | 现状核对结论 |
|------|------|------|
| `mall-marketing-dev.yml` | 补 `rocketmq.producer.group: mall-marketing-producer` | ⚠️ **必需**。`application-dev.yml` 里有全局 `rocketmq.producer.group: mall-user-producer`，每个模块都会继承；`mall-marketing-dev.yml` 未覆盖，marketing 会误用 `mall-user-producer` |
| `ruoyi-gateway-dev.yml` → `mall.security.anonymous-paths` | 补 `/api/marketing/coupons`、`/api/marketing/promotions`（**仅精确路径，禁止加 `/**`**） | ⚠️ **必需**。网关路由 `mall-marketing-api`(`/api/marketing/**`) **已存在**，但白名单里没有任何 marketing 路径。⚠️ 匹配用的是 `StringUtils.isMatch()` → `AntPathMatcher.match()`（**精确匹配**），若写 `/api/marketing/coupons/**` 会把 `POST /api/marketing/coupons/{id}/claims`（领券）和 `GET /api/marketing/coupons/claims`（我的券）一并放成匿名 |
| ~~`mall-marketing-dev.yml` 补 `mall.marketing.*` 5 项~~ | ~~新增~~ | ❌ **原判断有误**：该文件 2026-06-15 已存在，且 `coupon.*` + `calculation.*` 共 5 项**已配齐**，无需新增 |
| ~~`ruoyi-gateway-dev.yml` 加 mall-marketing 路由~~ | ~~新增~~ | ❌ **原判断有误**：`mall-marketing-api` 路由（`Path=/api/marketing/**`、`StripPrefix=0`）**已存在** |

### 5.3 顺带发现的既有配置问题（非本次必须，供决策）

| # | 位置 | 问题 | 影响 |
|---|------|------|------|
| 1 | 6 个模块配置的 `sentinel...dataId` | 全部写成 `sentinel-mall-auth`（复制粘贴），仅 mall-search 是 `sentinel-mall-search` | 限流规则指向错误 dataId |
| 2 | Nacos 实际配置清单 | `sentinel-mall-auth` / `sentinel-mall-search` / `sentinel-mall-marketing` **dataId 都不存在**（只有 `sentinel-ruoyi-gateway`） | Sentinel 规则持久化未落地 |
| 3 | `mall-*-dev.yml` 的 `mybatis-plus.typeAliasesPackage` | 写的是 `com.mall.<module>.**.domain`，实际 DO 包是 `com.mall.<module>.DO`（设计文档也写 `.**.DO`） | 纯注解 Mapper 下暂不影响运行，但配置与代码不符 |
| 4 | `application-dev.yml` | 全局 `rocketmq.producer.group: mall-user-producer` 被所有模块继承 | 语义错误。✅ **已处理（2026-10-02）**：① 全局 `producer.group` 已**删除**，并在 `# RocketMQ` 处补注释防止回退；② `mall-marketing-dev.yml` 与 `mall-order-dev.yml` 各自补上 `rocketmq.producer.group`（模块级配置晚于 `application-dev.yml` 被 import，可稳定覆盖）；③ 两者 `bootstrap.yml` 的本地 rocketmq 块已删除，消除双源歧义。**保留了 `name-server`（所有模块需要）与 `consumer.group: mall-user-consumer`（mall-user 的 `UserOrderCompletedConsumer` 显式引用 `${rocketmq.consumer.group:mall-user-consumer}`）**。⚠️ 后续 `mall-product` 启用 `SearchSyncProducer` 时，必须在 `mall-product-dev.yml` 补自己的 `rocketmq.producer.group` |
| 5 | `config_info` 表 | 每个 mall 配置有**两条重复行**（`tenant_id=''` 与 `tenant_id='public'`），内容一致 | Nacos 2.x→3.x 命名空间迁移残留，可清理 |

> **写入方式提醒**：直接对 `ry-config.config_info` 执行 SQL 修改**不会通知已连接的客户端**
> （Nacos 靠配置变更 API 更新内存 md5 并唤醒长轮询），客户端最长要等 dump 周期或重启才生效。
> 正确做法是在 Nacos 控制台 `配置管理` 页修改。

---

## 6 与设计文档的差异记录（实现时须回填）

| # | 设计文档 | 实际问题 | 处理 |
|---|---------|---------|------|
| 1 | §3.3 未定义转移抛 `CouponStateException(A0702)` | `mall-common` 无 `CouponStateException`；A0702 语义是「订单状态异常」 | 暂用 `BusinessException(ErrorCode.ORDER_STATUS_ERROR)`，是否需要券专用错误码待定 |
| 2 | §3.2 `useCoupon` 由 `mall:order:paid` 触发 | 设计 §7.2 消费事件表**只列了** `mall:order:cancelled`，漏了 `paid` | 按 §3.2/§5.2 实现两个消费者 |
| 3 | §6 过期任务标注「ruoyi-job」 | mall-order 同类任务用的是模块内 `@Scheduled` | 沿用模块内 `@Scheduled`，与 mall-order 一致 |
| 4 | §1.1 提到「秒杀活动库存缓存（Redis）」 | 无对应表字段与接口设计 | 本批不实现，留待活动功能细化 |
| 5 | 设计未提及 `InnerSignatureFilter` 注册方式 | 实际全模块未注册（§0.2） | 待决策 |
| 6 | 03_04 §4.3 券状态机写「下单锁定时扣减优惠券库存、订单取消时恢复」 | 与 14 §4/§5 冲突：14 的口径是**领券时**扣减 `remain_count`，取消时回补；03_04 是锁定时扣减模型。DDL 注释 `remain_count` = 「剩余可领取数量」，支持 14 口径 | 按 14 实现（领券扣减 / 取消回补），03_04 该行描述待修订 |

| 7 | 设计 §3.2 要求 `lockCoupon` 做「①查记录归属 → non userId `A0501`」 | 🔴 **契约与设计冲突**：`RemoteMarketingService.lockCoupon(orderNo, couponClaimId)` **没有 userId 参数**，营销侧拿不到用户身份；且契约里的 `validateCoupon(couponClaimId, userId)`（本来能做归属校验）**全仓从未被调用**。结果：mall-order 下单时 `couponRecordId` 直接来自客户端请求体（`CreateOrderRequest`），**无任何归属校验** → 用户可用他人的券抵扣（越权 / IDOR） | **营销侧防线（已实施）**：`CouponClaimServiceImpl.validateCoupon` 对「不存在」与「非本人」统一抛 A0501。**待做**：Batch 4 的 `calculate` 在指定 `couponClaimId` 时必须复用同一归属校验。**根治方案**：给契约加 `userId` 参数并同步改 mall-order 调用点 —— 属跨模块改动，**待用户批准** |

| 8 | 设计 §3.4 规定配置项 `calculation.timeout`（500ms 超时兜底）与 `calculation.max-candidates`（候选券上限 20） | 两者都是为「2^N 组合搜索」设计的。§0 决策改为**单一券择优**后：超时兜底无意义（复杂度 O(n)）；候选上限 20 会**漏选**持有 20 张以上券的用户的最优券 | `timeout` 未实现；`max-candidates` 改用固定安全兜底 200。两项因此成为**无使用方的配置项**，建议后续清理或重定义 |

> **已完成的文档对齐（2026-10-02）**：`CouponRecordStatusEnum` 已改为 1~5；
> `03_01` 的 `DEFAULT 0` 笔误已改为 `DEFAULT 1`；`06_mall-common公共模块设计.md` 的枚举值表已同步。

---

## 7 执行顺序

```
§0 确认 4 个问题
  └─→ Batch 1 数据层（无 TDD，可批量）
        └─→ Batch 2 券状态机（TDD，纯逻辑，先立骨架）
              └─→ Batch 3 券定义与领券（TDD）
                    └─→ Batch 4 试算引擎（TDD，金额正确性核心）
                          └─→ Batch 5 接口层（TDD）
                                └─→ Batch 6 Outbox / MQ / 定时任务
```

TDD 三步法：主会话写测试（RED）→ 子 Agent 写最小实现（GREEN）→ 主会话验证。
**每次只做 1 个 Service，独立完成 RED→GREEN→验证**，不打包丢给子 Agent。
