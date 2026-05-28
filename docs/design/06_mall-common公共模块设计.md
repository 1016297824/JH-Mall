# JH-Store mall-common 公共模块设计

> mall-common 是 C 端共享基础模块（非独立部署），提供共享 DTO、枚举、`MallResult<T>` 响应体、全局异常处理器。作为 mall 体系最底层模块，**不依赖任何 mall 模块**。`mall-api`（Feign 契约层）和所有 `mall-*` 业务模块均依赖 mall-common。
> 依据概要设计 `02_系统概要设计_补充.md` 第 2 章包结构规划及 §6 架构决策记录。

---

## 1 模块概述

### 1.1 动机

C 端各模块（auth/user/product/order/payment/marketing/search）存在以下公共需求：

1. **共享类型**：`MallResult<T>` 响应体、`MallUserDTO` 等 DTO、`UserStatusEnum` 等枚举——多个模块使用，需单一来源
2. **统一异常处理**：`MallExceptionHandler` 拦截所有未捕获异常，统一返回 `MallResult<T>`，避免各模块重复定义
3. **格式兜底**：确保 C 端 API 永远返回 `MallResult<T>` 结构，不会被若依 `GlobalExceptionHandler` 污染

### 1.2 职责

| 层 | 内容 | 约束 |
|----|------|------|
| 共享类型 | DTO、枚举、`MallResult<T>` | 仅放跨模块共享的类型，不放 VO、不放数据库实体 |
| 全局处理器 | `MallExceptionHandler`（`@RestControllerAdvice`） | 只做格式转换，不含业务逻辑 |
| 横切关注点 | 预留扩展位（如 CORS 过滤器、请求日志拦截器等） | 仅放与若依无关的 C 端公共能力 |

**禁止放入 mall-common 的：** 业务 Service、Feign 接口、数据库实体（DO/domain）、视图对象（VO）、管理端功能、对任何 mall 模块的依赖。

### 1.3 依赖关系

```
mall-common (共享基础层)
  ├── 被依赖方：mall-api + 所有 mall-* 业务模块
  ├── 依赖：
  │   └── Spring Boot Web（@RestControllerAdvice）
  └── 独立部署：否（JAR 包供各服务引用）

依赖链：mall-* → mall-api → mall-common
```

> mall-common **不能依赖任何 mall 模块**（含 mall-api），对 mall-api 是零依赖。`mall-api` 依赖 `mall-common` 使用共享类型（DTO/枚举/MallResult）。

### 1.4 与 ruoyi-common-* 的边界

| | ruoyi-common-* | mall-common |
|--|----------------|-------------|
| 服务对象 | 管理端 Controller + 全局共用能力 | C 端 Controller |
| 返回类型 | `AjaxResult` | `MallResult<T>` |
| 异常处理 | `GlobalExceptionHandler`（自动扫描） | `MallExceptionHandler`（`@Order(HIGHEST_PRECEDENCE)` 覆盖） |
| 包路径 | `com.ruoyi.common.*` | `com.mall.common.*` |

> **原则**：`ruoyi-common-*` 在 mall 模块中仅服务于管理端 Controller。C 端 Controller/Service（`.api` 包）不直接依赖 `ruoyi-common-*` 中的管理端功能。

---

## 2 包结构

```
server/mall/mall-common/
└── src/main/java/com/mall/common/
    ├── constant/                             # 跨服务共享常量（Redis Key 等）
    │   └── CacheConstants.java              # 商城统一 Redis Key 常量
    ├── DTO/                                  # 跨服务共享 DTO + 响应体
    │   ├── MallResult.java                  # C 端统一响应体（替代若依 AjaxResult）
    │   ├── user/
    │   │   └── MallUserDTO.java             # 用户数据传输对象
    │   └── product/                          # 商品 DTO（后续扩展）
    │   └── order/                            # 订单 DTO（后续扩展）
    ├── enums/                                # 跨服务共享枚举
    │   └── user/
    │       └── UserStatusEnum.java           # NORMAL(0)/FROZEN(1)/DELETED(2)
    ├── exception/
    │   ├── CaptchaException.java
    │   ├── TokenException.java
    │   └── BusinessException.java
    └── handler/
        └── MallExceptionHandler.java        # 全局异常处理器（@RestControllerAdvice）
```

> 共享 DTO 按域分包（如 `user/`、`product/`）

预留 `config/`、`filter/` 等包供后续扩展。新增内容需遵循 §1.2 约束。

### 2.1.1 CacheConstants 设计

`constant/CacheConstants.java` 管理商城所有 Redis Key 常量，按模块分内部类组织。**禁止在各 Controller/Service 中 `private static final` 定义 Redis key 常量或硬编码字符串**，全部收敛到此类。若依原生 `ruoyi-common-core` 已有 `CacheConstants`，商城侧独立维护，不与之混用。

```java
package com.mall.common.constant;

public class CacheConstants {

    /** 认证模块 Redis Key — 11 个常量 */
    public static final class Auth {
        public static final String SESSION      = "mall:auth:session:";
        public static final String REFRESH      = "mall:auth:refresh:";
        public static final String BLACKLIST    = "mall:auth:blacklist:";
        public static final String SMS_CODE     = "mall:auth:sms:code:";
        public static final String SMS_LIMIT    = "mall:auth:sms:limit:";
        public static final String SMS_TRY      = "mall:auth:sms:try:";
        public static final String SMS_IP       = "mall:auth:sms:ip:";
        public static final String PWD_ERR      = "mall:auth:pwd_err:";
        public static final String DECRYPT      = "mall:auth:decrypt:";
        public static final String CAPTCHA      = "mall:auth:captcha:";
        public static final String CAPTCHA_IP   = "mall:auth:captcha:ip:";
    }

    /** 用户模块 Redis Key */
    public static final class User {
        public static final String PROFILE      = "mall:user:profile:";
        public static final String SIGN         = "mall:user:sign:";
    }

    /** 商品模块 Redis Key */
    public static final class Product {
        public static final String SKU           = "mall:product:sku:";
        public static final String CATEGORY_TREE = "mall:product:category:tree";
        public static final String CATEGORY      = "mall:product:category:";
        public static final String NEWEST_LIST   = "mall:product:newest:list";
        public static final String TAG           = "mall:product:tag:";
        public static final String HOT_RANK      = "mall:product:hot:rank";
        public static final String UV            = "mall:product:uv:";
    }

    /** 订单模块 Redis Key */
    public static final class Order {
        public static final String CART       = "mall:order:cart:";
        public static final String IDEMPOTENT = "mall:order:idempotent:";
    }

    /** 支付模块 Redis Key */
    public static final class Payment {
        public static final String CALLBACK        = "mall:payment:callback:";
        public static final String REFUND_CALLBACK = "mall:payment:refund_callback:";
        public static final String IDEMPOTENT      = "mall:payment:idempotent:";
    }

    /** 营销模块 Redis Key */
    public static final class Marketing {
        public static final String COUPON_LOCK = "mall:marketing:coupon_lock:";
    }

    /** 搜索模块 Redis Key */
    public static final class Search {
        public static final String RESULT                = "mall:search:result:";
        public static final String INDEX_REBUILD_LOCK    = "mall:search:index:rebuild_lock";
        public static final String DEDUP                 = "mall:search:dedup:";
        public static final String SUGGESTION_HOT_KEYWORDS = "mall:search:suggestion:hot_keywords";
        public static final String SUGGEST               = "mall:search:suggest:";
    }

    /** MQ 基础设施 — 消息消费幂等去重 */
    public static final class MQ {
        public static final String DEDUP = "mall:mq:dedup:";
    }

    /** 分布式任务锁 */
    public static final class Job {
        public static final String LOCK_ORDER_TIMEOUT = "mall:job:lock:order_timeout";
    }
}
```

> **命名规范**：`{mall}:{service}:{biz}:{id}`，见 `02_系统概要设计_补充.md` §6。前置部分固定为 `mall` 前缀，与若依 `ry-cloud` 库的 key 前缀隔离。
>
> **新增 Redis key 流程**：① 在 `CacheConstants` 对应内部类中新增常量声明 → ② 代码中通过 `CacheConstants.XX.YYY` 引用，禁止直接硬编码字符串 → ③ 同步更新对应模块设计文档的 Redis Key 规范表。

### 2.1.2 MqTopicConstants 设计

`constant/MqTopicConstants.java` 管理商城所有 MQ Topic 名称常量，按业务域分内部类组织。禁止在生产者/消费者中硬编码 Topic 字符串。

```java
package com.mall.common.constant;

public class MqTopicConstants {

    /** 订单域 — 7 个 Topic */
    public static final class Order {
        public static final String CREATED   = "mall:order:created";
        public static final String PAID      = "mall:order:paid";
        public static final String CANCELLED = "mall:order:cancelled";
        public static final String DELIVERED = "mall:order:delivered";
        public static final String COMPLETED = "mall:order:completed";
        public static final String REFUNDED  = "mall:order:refunded";
        public static final String TIMEOUT   = "mall:order:timeout";
    }

    /** 支付/退款域 — 5 个 Topic */
    public static final class Payment {
        public static final String CREATED          = "mall:payment:created";
        public static final String PAID             = "mall:payment:paid";
        public static final String FAILED           = "mall:payment:failed";
        public static final String REFUND_CREATED   = "mall:refund:created";
        public static final String REFUND_SUCCEEDED = "mall:refund:succeeded";
    }

    /** 用户域 — 1 个 Topic */
    public static final class User {
        public static final String REGISTERED = "mall:user:registered";
    }

    /** 库存域 — 2 个 Topic */
    public static final class Stock {
        public static final String RESERVED = "mall:stock:reserved";
        public static final String RELEASED = "mall:stock:released";
    }

    /** 营销域 — 3 个 Topic */
    public static final class Coupon {
        public static final String LOCKED   = "mall:coupon:locked";
        public static final String USED     = "mall:coupon:used";
        public static final String RELEASED = "mall:coupon:released";
    }

    /** 搜索域 — 2 个 Topic */
    public static final class Search {
        public static final String SYNC    = "mall:search:sync";
        public static final String REBUILD = "mall:search:rebuild";
    }
}
```

> **命名规范**：`{mall}:{domain}:{action}`，共 20 个 Topic。新增 Topic 必须先在此类声明。

### 2.1.3 HeaderConstants 设计

`constant/HeaderConstants.java` 管理 C 端 HTTP 请求头名称常量。网关 MallAuthFilter 注入的 Header 和 mall 模块消费的 Header 使用同名常量，禁止硬编码字符串。

```java
package com.mall.common.constant;

public final class HeaderConstants {

    /** C端 JWT Token */
    public static final String AUTHORIZATION = "Authorization";

    /** C端用户ID（网关注入） */
    public static final String X_USER_ID = "X-User-Id";

    /** C端认证用户名（网关注入） */
    public static final String X_USER_NAME = "X-User-Name";

    /** 全链路追踪ID */
    public static final String X_REQUEST_ID = "X-Request-Id";

    /** 下单幂等键（客户端生成 UUID v4） */
    public static final String IDEMPOTENT_KEY = "Idempotent-Key";

    /** 客户端真实IP（代理透传） */
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";

    /** 客户端真实IP（Nginx） */
    public static final String X_REAL_IP = "X-Real-IP";

    /** 内部服务签名头 */
    public static final String X_INTERNAL_TIMESTAMP = "X-Internal-Timestamp";
    public static final String X_INTERNAL_NONCE     = "X-Internal-Nonce";
    public static final String X_INTERNAL_SIGNATURE = "X-Internal-Signature";
    public static final String X_INTERNAL_VERIFIED  = "X-Internal-Verified";

    /** 网关透传前缀 */
    public static final String X_USER_PREFIX = "X-User-";
    public static final String X_MALL_PREFIX = "X-Mall-";
}
```

> **注意**：`ruoyi-gateway` 模块不依赖 `mall-common`，MallAuthFilter 中 Header 常量以 `private static final` 形式本地定义，无需跨模块同步。

### 2.2 共享枚举

数据库字段类型分三类，枚举策略按类区分：

| 分类 | 数据库类型 | 枚举策略 | 典型字段 |
|------|:---:|------|------|
| 核心业务枚举 | `tinyint unsigned` | Java `int` 枚举，直接映射 | 状态/类型字段，共 19 个 |
| 外部渠道值 | `varchar(20)` | 不定义枚举，保留原始值 | `channel_pay_status`、`channel_refund_status` |
| 业务类型标识 | `varchar(20~64)` | 用 `String` 枚举 | `register_type`、`cancel_type` |

#### 2.2.1 用户域

| 枚举 | 值 | 使用方 |
|------|----|--------|
| `UserStatusEnum` | NORMAL(0) / FROZEN(1) / DELETED(2) | mall-user, mall-auth |
| `GenderEnum` | UNKNOWN(0) / MALE(1) / FEMALE(2) | mall-user |
| `RegisterTypeEnum` | PHONE("phone") / WECHAT("wechat") / EMAIL("email") | mall-user, mall-auth |
| `PointsChangeTypeEnum` | INCREASE(1) / DECREASE(2) | mall-user |
| `GrowthChangeTypeEnum` | INCREASE(1) / DECREASE(2) | mall-user |

#### 2.2.2 商品域

| 枚举 | 值 | 使用方 |
|------|----|--------|
| `PublishStatusEnum` | OFFLINE(0) / ONLINE(1) | mall-product, mall-search |
| `VerifyStatusEnum` | PENDING(0) / APPROVED(1) / REJECTED(2) | mall-product |

#### 2.2.3 订单域

| 枚举 | 值 | 使用方 |
|------|----|--------|
| `OrderStatusEnum` | WAIT_PAY(0) / PAID(1) / WAIT_DELIVER(2) / WAIT_RECEIVE(3) / COMPLETED(4) / CANCELLED(5) / CLOSED(6) / REFUNDING(7) / REFUNDED(8) | mall-order, mall-payment, mall-marketing |
| `AfterSaleTypeEnum` | REFUND_ONLY(1) / RETURN_REFUND(2) / EXCHANGE(3) | mall-order |
| `AfterSaleStatusEnum` | PENDING(0) / APPROVED(1) / REJECTED(2) / RETURNED(3) / RECEIVED(4) / REFUNDING(5) / COMPLETED(6) / CLOSED(7) | mall-order, mall-payment |
| `CancelTypeEnum` | USER_CANCEL("user_cancel") / TIMEOUT_CANCEL("timeout_cancel") / ADMIN_CANCEL("admin_cancel") | mall-order |

#### 2.2.4 支付域

| 枚举 | 值 | 使用方 |
|------|----|--------|
| `PaymentStatusEnum` | UNPAID(0) / PAID(1) / FAILED(2) / CLOSED(3) / REFUNDING(4) / REFUNDED(5) | mall-payment, mall-order |
| `RefundStatusEnum` | PROCESSING(0) / SUCCESS(1) / FAILED(2) | mall-payment, mall-order |
| `ChannelTypeEnum` | PAY(1) / REFUND(2) | mall-payment |
| `CallbackProcessStatusEnum` | PENDING(0) / SUCCESS(1) / FAILED(2) / DUPLICATE(3) | mall-payment |

#### 2.2.5 营销域

| 枚举 | 值 | 使用方 |
|------|----|--------|
| `CouponTypeEnum` | FULL_REDUCE(1) / DISCOUNT(2) / NO_THRESHOLD(3) | mall-marketing |
| `CouponStatusEnum` | DRAFT(0) / PUBLISHED(1) / ENDED(2) / DISCARDED(3) | mall-marketing |
| `CouponRecordStatusEnum` | AVAILABLE(0) / LOCKED(1) / USED(2) / RELEASED(3) / EXPIRED(4) | mall-marketing, mall-order |
| `PromotionTypeEnum` | FULL_REDUCE(1) / FULL_DISCOUNT(2) / FREE_SHIPPING(3) / SECKILL(4) | mall-marketing |
| `PromotionStatusEnum` | PENDING(0) / ACTIVE(1) / ENDED(2) / CLOSED(3) | mall-marketing |
| `RuleTypeEnum` | FULL_REDUCE(1) / FULL_DISCOUNT(2) / FREE_SHIPPING(3) | mall-marketing |

#### 2.2.6 通用

| 枚举 | 值 | 使用方 |
|------|----|--------|
| `OutboxStatusEnum` | NEW("NEW") / PENDING("PENDING") / SENT("SENT") / FAILED("FAILED") | 各服务 |

> 核心业务枚举（`tinyint unsigned`）共 17 个字段，Java 用 `int` 编码直接对应，MyBatis 可直接映射，无需 TypeHandler。
> `RegisterTypeEnum`、`CancelTypeEnum`、`OutboxStatusEnum` 使用 `String` 值，对应数据库 `varchar` 字段。
> 以上 20 个枚举已全部在 `com.mall.common.enums` 中实现，按域分包。

---

## 3 MallExceptionHandler 详细设计

### 3.1 定位

C 端全局异常兜底。用 `@Order(Ordered.HIGHEST_PRECEDENCE)` 确保优先级高于若依 `GlobalExceptionHandler`，避免响应用于被 `AjaxResult` 污染。

### 3.2 异常处理策略

| 异常类型 | 处理器 | 返回 | 说明 |
|----------|--------|------|------|
| `CaptchaException` | 精确匹配 | `MallResult.error(errorCode, message, userTip)` | 验证码异常，携带错误码 |
| `TokenException` | 精确匹配 | `MallResult.error("A0231", message, userTip)` | Token 异常，携带 A0231 |
| `FeignException` | 精确匹配 | `MallResult.error("B0001", "服务暂时不可用")` | Feign 调用异常，统一友好提示 |
| `MethodArgumentNotValidException` | 精确匹配 | `MallResult.error("A0401", 字段错误消息)` | `@Validated` 参数校验失败时触发 |
| `Exception.class` | 兜底 | `MallResult.error("B0001", "系统繁忙，请稍后再试")` | 日志记录完整堆栈 |

### 3.3 代码骨架

```java
package com.mall.common.handler;

import com.mall.common.DTO.MallResult;
import com.mall.common.exception.CaptchaException;
import com.mall.common.exception.TokenException;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MallExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(MallExceptionHandler.class);

    @ExceptionHandler(CaptchaException.class)
    public MallResult<Void> handleCaptcha(CaptchaException e) {
        return MallResult.error(e.getErrorCode(), e.getMessage(), e.getUserTip());
    }

    @ExceptionHandler(TokenException.class)
    public MallResult<Void> handleToken(TokenException e) {
        log.warn("Token 异常: {} - {}", e.getErrorCode(), e.getMessage());
        return MallResult.error(e.getErrorCode(), e.getMessage(), e.getUserTip());
    }

    @ExceptionHandler(FeignException.class)
    public MallResult<Void> handleFeign(FeignException e) {
        log.error("Feign 调用异常: {}", e.getMessage());
        return MallResult.error("B0001", "服务暂时不可用，请稍后重试");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public MallResult<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "请完整填写信息";
        return MallResult.error("A0401", msg, msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public MallResult<Void> handleConstraintViolation(ConstraintViolationException e) {
        return MallResult.error("A0401", e.getMessage(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public MallResult<Void> handleException(Exception e) {
        log.error("unhandled exception", e);
        return MallResult.error("B0001", "系统繁忙，请稍后再试");
    }
}
```

> `CaptchaException` 和 `TokenException` 均定义在 `mall-common`（`com.mall.common.exception`），避免循环依赖。`MallExceptionHandler` 与异常类同模块，无需跨模块引用。

### 3.4 自动注册机制

`MallExceptionHandler` 在 `mall-common` JAR 中，不会被 `@SpringBootApplication` 默认包扫描覆盖。需要各模块 `Application.java` 显式引入：

```java
@SpringBootApplication(scanBasePackages = {"com.mall.auth", "com.mall.common"})
// 或通过 @ComponentScan + @Import
```

MVP 阶段推荐在 `@SpringBootApplication` 的 `scanBasePackages` 中加入 `"com.mall.common"`。后续可改为 Spring Boot AutoConfiguration 实现零侵入注册。

---

## 4 审计日志说明

审计日志（`mall_audit_log`）由各 C 端模块 Service 层直写，不在 `mall-common` 中实现。原因：

- 审计逻辑与业务操作耦合度高（需要确定写入时机、内容裁剪）
- 若抽出公共审计组件，反而增加调用链复杂度

---

## 5 日志配置

`mall-common` 纯依赖库，**不包含 `logback.xml`**。遵循 `02_系统概要设计_补充.md` §16.1.1 约定。

---

## 6 错误码

`MallResult` 的错误码定义在 `MallResult` 类自身中（`SUCCESS_CODE = "00000"`），扩展错误码由各业务模块按 `02_系统概要设计.md` 第 2 章错误码规范自行定义。

---

## 7 设计文档索引

| 文档 | 说明 |
|------|------|
| `02_系统概要设计_补充.md` §2 包结构、§6 架构决策 | mall-common 在整体架构中的位置 |
| `05_mall-api契约层设计.md` | Feign 接口契约（依赖 mall-common 使用 DTO 类型） |
| `07_mall-auth详细设计.md` | 首个依赖 mall-common 的业务模块 |
