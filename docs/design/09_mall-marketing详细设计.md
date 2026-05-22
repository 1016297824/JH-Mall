# JH-Store mall-marketing 模块详细设计

> 基于系统详细设计 `03_系统详细设计.md` 展开。数据表 DDL 在系统设计 1.5 节统一维护，此处只引用表名和字段。

---

## 1 模块概述

### 1.1 子领域

| 子领域 | 实体 | 说明 |
|--------|------|------|
| 优惠券定义 | `mall_marketing_coupon` | 管理端创建券模板（满减/折扣/无门槛），控制发行量+有效期+限领数 |
| 优惠券记录 | `mall_marketing_coupon_record` | 用户领券→锁券→核销/释放的全生命周期 |
| 活动管理 | `mall_marketing_promotion` | 满减/满折/包邮/秒杀活动，含时间段和 Banner 图 |
| 促销规则 | `mall_marketing_promotion_rule` | 活动下多条规则（门槛+优惠+互斥/叠加+优先级） |
| 优惠试算 | — | 下单前算最优优惠组合（不锁定），返回扣减明细 |
| RocketMQ 事件 | Outbox | 生产 `mall:coupon:used`，消费 `mall:order:cancelled` |

### 1.2 依赖关系

```
mall-marketing (9306端口)
  ├── MySQL：自有表（见表系统设计第 1.5 节）
  ├── Redis：领券防并发锁、秒杀活动库存缓存
  ├── RocketMQ (Producer)：写 Outbox → 投递 mall:coupon:used
  ├── RocketMQ (Consumer)：消费 mall:order:cancelled → 释放优惠券
  ├── mall-api (Feign)：RemoteMarketingService 供 mall-order 调用试算/锁定/释放
  └── mall-order (Feign Caller)：调营销服务锁定/释放优惠券、优惠试算
```

> **关键约束**：营销服务负责优惠计算和锁定，不负责订单最终金额落库。优惠试算不改变优惠券状态，锁定才修改状态。最终优惠金额由 mall-order 落快照。

---

## 2 包结构与接口映射

### 2.1 包结构

```
server/mall/mall-marketing/
└── src/main/java/com/jhstore/mall/marketing/
    ├── MallMarketingApplication.java        # Spring Boot 启动类
    ├── controller/
    │   ├── admin/
    │   │   ├── CouponAdminController.java   # /mall-marketing/coupons/**
    │   │   └── PromotionAdminController.java # /mall-marketing/promotions/**
    │   └── api/
    │       ├── CouponApiController.java      # /api/marketing/coupons/**
    │       ├── PromotionApiController.java   # /api/marketing/promotions
    │       └── CalculationApiController.java # /api/marketing/calculations
    ├── dto/
    │   ├── request/                         → CouponClaimReq, CreateCouponReq, CreatePromotionReq,
    │   │                                       CreateRuleReq, CalculationReq
    │   └── response/                        → CouponDefResp, CouponRecordResp, CalculationResp
    ├── domain/
    │   ├── MallCouponDO.java                # 对应 mall_marketing_coupon 表
    │   ├── MallCouponRecordDO.java          # 对应 mall_marketing_coupon_record 表
    │   ├── MallPromotionDO.java             # 对应 mall_marketing_promotion 表
    │   └── MallPromotionRuleDO.java         # 对应 mall_marketing_promotion_rule 表
    ├── service/
    │   ├── coupon/
    │   │   ├── CouponDefService.java        # 接口
    │   │   ├── impl/
    │   │   │   └── CouponDefServiceImpl.java # 优惠券定义 CRUD
    │   │   ├── CouponClaimService.java      # 接口
    │   │   ├── impl/
    │   │   │   └── CouponClaimServiceImpl.java # 领取/锁定/核销/释放
    │   ├── promotion/
    │   │   ├── PromotionService.java        # 接口
    │   │   ├── impl/
    │   │   │   └── PromotionServiceImpl.java # 活动+规则 CRUD
    │   │   └── PromotionRuleMatcher.java    # 促销规则匹配引擎
    │   └── calculation/
    │       ├── CalculationService.java      # 接口
    │       └── impl/
    │           └── CalculationServiceImpl.java # 优惠试算核心
    ├── mapper/
    │   ├── MallCouponMapper.java
    │   ├── MallCouponRecordMapper.java
    │   ├── MallPromotionMapper.java
    │   └── MallPromotionRuleMapper.java
    ├── statemachine/
    │   └── CouponStateMachine.java          # 优惠券记录状态机
    ├── infrastructure/
    │   ├── mq/
    │   │   ├── CouponUsedProducer.java       # Outbox 生产 mall:coupon:used
    │   │   └── OrderCancelledConsumer.java   # 消费 mall:order:cancelled 释放券
    │   └── feign/
    │       └── RemoteOrderAdapter.java       # 调 mall-order 查询订单状态
    └── convert/
        ├── CouponConvert.java               # MallCouponDO ↔ CouponDefResp
        ├── CouponRecordConvert.java          # MallCouponRecordDO ↔ CouponRecordResp
        └── PromotionConvert.java            # MallPromotionDO ↔ PromotionResp
```

### 2.2 接口 → Controller 映射

| # | 方法 | 路径 | Controller | 方法名 | 需登录 | 权限码 |
|---|------|------|-----------|--------|:---:|--------|
| 1 | GET | `/api/marketing/coupons` | CouponApiController | `listAvailableCoupons(params)` | 否 | — |
| 2 | POST | `/api/marketing/coupons/{couponDefId}/claims` | CouponApiController | `claimCoupon(couponDefId)` | 是 | — |
| 3 | GET | `/api/marketing/coupons/claims` | CouponApiController | `listMyCoupons(params)` | 是 | — |
| 4 | GET | `/api/marketing/promotions` | PromotionApiController | `listActivePromotions()` | 否 | — |
| 5 | POST | `/api/marketing/calculations` | CalculationApiController | `calculate(req)` | 是 | — |

管理端接口由若依代码生成器自动生成（优惠券定义/领取记录、活动/规则管理），权限码无需手动维护。

---

## 3 核心类设计

### 3.1 CouponDefServiceImpl

位于 `service/coupon/impl/CouponDefServiceImpl.java`，优惠券定义管理。

- `listAvailableCoupons()`：查 `coupon_status=1`（已发布）+ 未到 `use_end_time` + `remain_count>0`，按 `sort_order` 排序
- `createCouponDef(req)`：校验参数（满减券 face_value>0，折扣券 discount_rate 1~99），初始 `coupon_status=0`（未发布），发布需独立操作
- `updateCouponDef(req)`：已发布的券只允许修改部分字段（名称、描述），不允许改类型和面值
- `deleteCouponDef(id)`：`coupon_status=0`（未发布）可物理删除，已发布只能 `coupon_status=3`（废弃）

### 3.2 CouponClaimServiceImpl

位于 `service/coupon/impl/CouponClaimServiceImpl.java`，用户优惠券全生命周期。

**claimCoupon(couponDefId, userId)**：领取优惠券
- ①查 `mall_marketing_coupon` → coupon_status≠1 `A0610`，`use_end_time < NOW()` → `A0610`
- ②查 `mall_marketing_coupon_record` 中该用户 `COUNT WHERE coupon_id=? AND user_id=?` → 超 `per_user_limit` 则 `A0502`
- ③乐观锁扣库存：`UPDATE mall_marketing_coupon SET remain_count=remain_count-1, version=version+1 WHERE id=? AND version=? AND remain_count>0`
- ④失败（影响 0 行或版本冲突）→ `A0611`（已被领完）
- ⑤创建记录：`coupon_code` 雪花+CPN 前缀，`record_status=AVAILABLE`，`face_value` 快照，`expire_time`=use_end_time
- ⑥返回 `claimId`

**lockCoupon(couponClaimId, orderNo)**：下单时锁定（Feign 由 mall-order 调用）
- ①查记录归属 → non userId `A0501`
- ②状态机校验：`AVAILABLE → LOCKED`
- ③UPDATE：`SET record_status=2, order_no=?, lock_time=NOW() WHERE id=? AND record_status=1`
- ④同一本地事务写 Outbox `mall:coupon:used`（仅标记、不投递，等订单支付成功后才真正核销）
- ⑤失败返回 `A0612`（不满足条件）

**useCoupon(orderNo)**：支付成功核销（消费 `mall:order:paid` 事件触发）
- ①查 `WHERE order_no=? AND record_status=2` →
- ②状态机：`LOCKED → USED`，记录 `use_time=NOW()`
- ③写 Outbox `mall:coupon:used` → 发布核销事件

**releaseCoupon(orderNo)**：订单取消释放（消费 `mall:order:cancelled` 事件触发）
- ①查 `WHERE order_no=? AND record_status=2` →
- ②状态机：`LOCKED → RELEASED`，清除 `order_no`，记录 `release_time`
- ③回补优惠券库存：`UPDATE mall_marketing_coupon SET remain_count=remain_count+1 WHERE id=?`
- ④幂等去重：`orderNo + couponRecordId` 维度，防止重复释放

**expireBatch()**：定时任务过期扫描
- ①`WHERE record_status IN (1, 4) AND expire_time < NOW()` → AVAILABLE 和 RELEASED 的过期
- ②状态机：`→ EXPIRED`
- ③分页批量处理，单次上限 500 条

### 3.3 CouponStateMachine

位于 `statemachine/CouponStateMachine.java`，优惠券记录状态转移。

| 状态 | 含义 | 终态 |
|------|------|:---:|
| AVAILABLE | 可用，已领取未使用 | 否 |
| LOCKED | 已锁定，下单占用中 | 否 |
| USED | 已使用，订单支付成功 | 是 |
| RELEASED | 已释放，订单取消回退 | 否 |
| EXPIRED | 已过期，超过有效期 | 是 |

| 当前状态 | 触发事件 | 事件源 | 目标状态 | 后置动作 |
|---------|---------|:-----:|---------|---------|
| AVAILABLE | 下单锁定 | mall-order | LOCKED | ①记录 order_no+lock_time |
| LOCKED | 订单支付成功 | RocketMQ | USED | ①记录 use_time ②写 Outbox `mall:coupon:used` |
| LOCKED | 订单取消 | RocketMQ | RELEASED | ①清除 order_no ②回补 remain_count |
| AVAILABLE | 超时过期 | 定时任务 | EXPIRED | — |
| RELEASED | 超时过期 | 定时任务 | EXPIRED | — |

转移矩阵中未定义的转移 → 抛 `CouponStateException(A0702)`。
释放后的券在原有效期到期后自动过期（通过定时任务扫描 `expire_time < NOW()`）。

### 3.4 CalculationServiceImpl

位于 `service/calculation/impl/CalculationServiceImpl.java`，优惠试算引擎。

**calculate(req)**：下单前调用，不锁定任何资源，只计算最优优惠组合。

输入：
| 字段 | 说明 |
|------|------|
| userId | 用户 ID |
| items | `[{skuId, price, quantity}]` 商品明细 |
| couponClaimId | 指定使用的优惠券（可选，传了只算这一张券） |

计算逻辑（两阶段过滤+择优）：
1. 根据 items 算订单总原价 `totalAmount`
2. **阶段一：门槛过滤**。查该用户所有 `AVAILABLE` 优惠券 → 过滤掉 `min_order_amount > totalAmount` 的不适用券
3. **阶段二：面值择优**。过滤后的券按 `face_value DESC` 排序，取前 20 张
4. 查当前进行中的促销活动 + 规则：`promotion_status=1` + 规则匹配
5. **组合枚举**：对 ≤20 张候选券做 2^N 全组合搜索，剪枝：
   - 门槛不满足 → 跳过
   - 互斥冲突（`is_exclusive=1` 的规则间） → 只保留最大优惠的一条
6. **择优返回**：`{ originalAmount, couponDiscount, promotionDiscount, finalAmount, appliedCoupons[], appliedPromotions[] }`

**性能约束**：
- 试算不调远程服务、不做 DB 写操作、超时 500ms
- 过滤后 ≤20 张 → 全组合搜索（最坏 2^20≈100 万次，现代 JVM <10ms）
- 过滤后 >20 张 → 按面值取前 20 张做全组合
- 超时兜底：500ms 到期则返回当前找到的最优解
- 促销规则匹配结果缓存：同一 `promotionId + totalAmount` 1 分钟内不重复计算（Caffeine 本地缓存）

### 3.5 PromotionRuleMatcher

位于 `service/promotion/PromotionRuleMatcher.java`，促销规则匹配引擎。

按优先级排序（`sort_order ASC`），逐条匹配：
1. 订单总金额 ≥ `threshold_amount`
2. 商品在规则适用范围内（如果规则限定了类目/品牌）
3. 活动时间段内（现在时间在 `start_time` 和 `end_time` 之间）

匹配到的第一条互斥规则直接返回；非互斥规则可叠加。

---

## 4 领取优惠券流程

```
用户 POST /api/marketing/coupons/{couponDefId}/claims
  → CouponClaimServiceImpl.claimCoupon(couponDefId, userId):
      ① 查优惠券定义 → 状态≠已发布或已过期 → A0610
      ② 查用户已领数量 → 超 per_user_limit → A0502
      ③ 乐观锁扣库存 remain_count
         UPDATE mall_marketing_coupon SET remain_count=remain_count-1, version=version+1
         WHERE id=? AND version=? AND remain_count>0
         → 失败 A0611
      ④ INSERT mall_marketing_coupon_record (record_status=AVAILABLE)
      ⑤ 返回 claimId
```

| 防护 | 机制 |
|------|------|
| 并发超领 | 乐观锁 `remain_count>0` + `version` |
| 重复领取 | `user_id + coupon_id` 去重，DB 唯一约束 |
| 过期领取 | 查时校验 `use_end_time < NOW()` |

---

## 5 优惠锁定与释放

### 5.1 下单锁券

mall-order 调 `RemoteMarketingService.lockCoupon(orderNo, couponClaimId)`：

```
CouponClaimServiceImpl.lockCoupon(couponClaimId, orderNo):
  ① 查 couponRecord → 归属不匹配 A0501
  ② 状态机 AVAILABLE → LOCKED
  ③ UPDATE SET record_status=2, order_no=?, lock_time=NOW() WHERE id=? AND record_status=1
  ④ 失败 → A0612
```

### 5.2 支付核销

消费 `mall:order:paid` → `CouponClaimServiceImpl.useCoupon(orderNo)`：
- 查 `WHERE order_no=? AND record_status=2` → 逐条 `LOCKED → USED`
- 写 Outbox `mall:coupon:used` → 记录核销事实

### 5.3 取消释放

消费 `mall:order:cancelled` → `CouponClaimServiceImpl.releaseCoupon(orderNo)`：
- 查 `WHERE order_no=? AND record_status=2` → 逐条 `LOCKED → RELEASED`
- 清除 `order_no`，记录 `release_time`
- 回补 `remain_count`：`UPDATE mall_marketing_coupon SET remain_count=remain_count+1 WHERE id=?`
- 幂等：已释放的不重复处理

---

## 6 优惠券过期处理

定时任务 `CouponExpireTask`（ruoyi-job）：

| 参数 | 值 | 说明 |
|------|:--:|------|
| 扫描间隔 | 每小时 | 精度要求不高 |
| 单次上限 | 500 条 | 防内存溢出 |
| 扫描条件 | `record_status IN (1,4) AND expire_time < NOW()` | AVAILABLE + RELEASED |

- 批量 UPDATE `SET record_status=5` → EXPIRED
- 状态机保证：已 EXPIRED 的券不再被锁定

---

## 7 RocketMQ 事件

### 7.1 发布的事件

| Topic | Payload 字段 | 发布时机 |
|-------|-------------|---------|
| `mall:coupon:used` | `couponRecordId`、`couponId`、`userId`、`orderNo`、`faceValue`（分）、`useTime` | 订单支付成功核销时 |

### 7.2 消费的事件

| Topic | 消费者类 | 处理流程 |
|-------|---------|---------|
| `mall:order:cancelled` | `OrderCancelledConsumer` | ①幂等去重 `mall:mq:dedup:{messageId}:mall-marketing` ②查 `WHERE order_no=? AND record_status=2` ③逐条释放优惠券 ④回补库存 |

### 7.3 重试策略

| 重试次数 | 间隔 | next_retry_time |
|:-------:|------|----------------|
| 第 1 次 | 10s | NOW() + 10s |
| 第 2 次 | 30s | NOW() + 30s |
| 第 3 次 | 60s | NOW() + 60s |
| 超过 3 次 | — | status = FAILED，死信处理 |

---

## 8 优惠试算 Feign 接口

定义在 `mall-api` 的 `RemoteMarketingService`，供 mall-order 调用：

| 方法 | 说明 |
|------|------|
| `calculate(CalculationReq)` | 优惠试算，返回可用优惠组合 |
| `lockCoupon(orderNo, couponClaimId)` | 锁定优惠券 |
| `releaseCoupon(orderNo)` | 释放订单所有锁定券 |
| `validateCoupon(couponClaimId, userId)` | 校验优惠券可用性（下单参数校验用）|

---

## 9 Nacos 配置

### 9.1 DataId: `mall-marketing-dev.yml`

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
  typeAliasesPackage: com.mall.marketing.**.domain
  mapperLocations: classpath:mapper/**/*.xml

springdoc:
  gatewayUrl: http://localhost:8080/${spring.application.name}
  api-docs:
    enabled: true
  info:
    title: '营销模块接口文档'
    description: '营销模块接口描述'
    contact:
      name: RuoYi
      url: https://ruoyi.vip

mall:
  marketing:
    coupon:
      expire-scan-interval: 3600
      expire-batch-size: 500
    calculation:
      timeout: 500
      max-candidates: 20
      rule-cache-ttl: 60
```

> 以上配置通过 Nacos 下发，支持 `@RefreshScope` 运行时动态刷新。

### 9.2 本地配置文件 `bootstrap.yml`

```yaml
# mall-marketing 营销服务
server:
  port: 9306

spring:
  application:
    name: mall-marketing
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
|--------|--------|:---:|------|
| `mall.marketing.coupon.expire-scan-interval` | 3600 | 秒 | 优惠券过期扫描间隔（每小时） |
| `mall.marketing.coupon.expire-batch-size` | 500 | 条 | 单次过期处理上限 |
| `mall.marketing.calculation.timeout` | 500 | ms | 优惠试算超时时间 |
| `mall.marketing.calculation.max-candidates` | 20 | 张 | 过滤后参与全组合搜索的最大候选券数 |
| `mall.marketing.calculation.rule-cache-ttl` | 60 | 秒 | 促销规则匹配结果本地缓存 TTL |

---

## 10 错误码汇总

| 错误码 | HTTP | userTip | 说明 |
|--------|:----:|---------|------|
| 00000 | 200 | — | 成功 |
| A0301 | 401 | 请先登录 | 未登录 |
| A0320 | 403 | 无权限访问 | 权限不足 |
| A0501 | 404 | 优惠券不存在 | 优惠券定义或记录不存在 |
| A0502 | 400 | 已达限领数量 | 每人限领 N 张已达上限 |
| A0503 | 400 | 活动中不可删除 | 进行中活动不允许删除 |
| A0610 | 400 | 优惠券已过期 | 优惠券超过有效期 |
| A0611 | 400 | 优惠券已被领完 | remain_count=0 |
| A0612 | 400 | 不满足优惠券使用条件 | 锁定失败（状态不为可用/订单金额不满足门槛） |
| A0613 | 400 | 优惠券已使用 | 券状态为 USED |
| B0001 | 500 | 系统繁忙，请稍后再试 | 未预期异常 |
| C0110 | 500 | 服务暂时不可用 | Redis 连接失败 |
| C0120 | 500 | 数据库异常 | MySQL 连接失败 |

> 全部错误码来自系统设计第二章。

---
