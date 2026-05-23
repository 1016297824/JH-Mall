# JH-Store mall-common 公共模块设计

> mall-common 是 C 端公共基础设施模块（非独立部署），提供全局异常处理器等横切关注点。与 `mall-api`（契约层）职责互补，两者共同构成 C 端的基础支撑。
> 依据概要设计 `02_系统概要设计_补充.md` 第 2 章包结构规划及 §6 架构决策记录。

---

## 1 模块概述

### 1.1 动机

C 端各模块（auth/user/product/order/payment/marketing/search）存在以下公共需求：

1. **统一异常处理**：`MallExceptionHandler` 拦截所有未捕获异常，统一返回 `MallResult<T>`，避免各模块重复定义
2. **格式兜底**：确保 C 端 API 永远返回 `MallResult<T>` 结构，不会被若依 `GlobalExceptionHandler` 污染

### 1.2 职责

| 层 | 内容 | 约束 |
|----|------|------|
| 全局处理器 | `MallExceptionHandler`（`@RestControllerAdvice`） | 只做格式转换，不含业务逻辑 |
| 横切关注点 | 预留扩展位（如 CORS 过滤器、请求日志拦截器等） | 仅放与若依无关的 C 端公共能力 |

**禁止放入 mall-common 的：** 业务 Service、Feign 接口、DTO、数据库实体、业务枚举、管理端功能。

### 1.3 依赖关系

```
mall-common (公共基础设施层)
  ├── 被依赖方：所有 mall-* 业务模块
  ├── 依赖：
  │   ├── mall-api（复用 MallResult、Feign 接口等契约）
  │   └── Spring Boot Web（@RestControllerAdvice）
  └── 独立部署：否（JAR 包供各服务引用）

业务模块依赖链：mall-* → mall-common → mall-api
```

> mall-common 不能反向依赖业务模块，业务模块之间通过 `mall-api` 的 Feign 接口通信。

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
    ├── handler/
    │   └── MallExceptionHandler.java   # 全局异常处理器（@RestControllerAdvice）
    └── ...
```

预留 `config/`、`filter/` 等包供后续扩展。新增内容需遵循 §1.2 约束。

---

## 3 MallExceptionHandler 详细设计

### 3.1 定位

C 端全局异常兜底。用 `@Order(Ordered.HIGHEST_PRECEDENCE)` 确保优先级高于若依 `GlobalExceptionHandler`，避免响应用于被 `AjaxResult` 污染。

### 3.2 异常处理策略

| 异常类型 | 处理器 | 返回 | 说明 |
|----------|--------|------|------|
| `CaptchaException` | 精确匹配 | `MallResult.error(errorCode, message, userTip)` | 500 错误码、不含堆栈 |
| `MethodArgumentNotValidException` | 精确匹配 | `MallResult.error("A0401", 字段错误消息)` | `@Validated` 参数校验失败时触发 |
| `Exception.class` | 兜底 | `MallResult.error("B0001", "系统繁忙，请稍后再试")` | 日志记录完整堆栈 |

### 3.3 代码骨架

```java
package com.mall.common.handler;

import com.mall.api.dto.MallResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MallExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(MallExceptionHandler.class);

    @ExceptionHandler(com.mall.auth.infrastructure.exception.CaptchaException.class)
    public MallResult<Void> handleCaptcha(com.mall.auth.infrastructure.exception.CaptchaException e) {
        return MallResult.error(e.getErrorCode(), e.getMessage(), e.getUserTip());
    }

    @ExceptionHandler(Exception.class)
    public MallResult<Void> handleException(Exception e) {
        log.error("unhandled exception", e);
        return MallResult.error("B0001", "系统繁忙，请稍后再试");
    }
}
```

> 注：`CaptchaException` 在 `mall-auth` 中定义，`MallExceptionHandler` 通过精确类型捕获。若有多个模块定义业务异常，后续可考虑抽取异常基类到 `mall-common`。

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

`MallResult` 的错误码定义在 `05_mall-api契约层设计.md` §7 中，`mall-common` 不定义错误码。

---

## 7 设计文档索引

| 文档 | 说明 |
|------|------|
| `02_系统概要设计_补充.md` §2 包结构、§6 架构决策 | mall-common 在整体架构中的位置 |
| `05_mall-api契约层设计.md` | 契约层（MallResult、Feign 接口） |
| `07_mall-auth详细设计.md` | 首个依赖 mall-common 的业务模块 |
