# JH-Store 网关详细设计

> 基于系统详细设计 `03_系统详细设计.md` 展开。网关是 C 端和管理端请求的统一入口。

---

## 1 模块概述

### 1.1 职责

| 职责       | 说明                                                                                |
| ---------- | ----------------------------------------------------------------------------------- |
| 路由分发   | 管理端走 `discovery.locator` 自动路由，C 端走 `/api/**` 显式路由                |
| 认证分流   | 两个 GlobalFilter 串行执行：AuthFilter 校验管理端 JWT，MallAuthFilter 校验 C 端 JWT |
| 白名单     | `/api/**` 和 `/callback/**` 不在 AuthFilter 校验范围内                          |
| 请求头清洗 | 剥离外部注入的 `X-User-*` / `X-Admin-*` 头，注入可信内部请求头                  |
| 限流       | Sentinel 集群流控                                                                   |

### 1.2 依赖关系

```
ruoyi-gateway (8080端口)
  ├── discovery.locator: 自动发现所有注册服务
  ├── AuthFilter (order=-200): 管理端 JWT 校验
  ├── MallAuthFilter (order=-150): C 端 JWT 校验
  └── Nacos: 动态路由配置 + 白名单
```

---

## 2 过滤器链设计

### 2.1 两条过滤器串行执行

```
请求 → AuthFilter (order=-200) → MallAuthFilter (order=-150) → 路由转发
```

两个过滤器对所有路径都执行，但各自有路径判断逻辑：

#### AuthFilter（order=-200）

| 逻辑            | 说明                                                  |
| --------------- | ----------------------------------------------------- |
| 白名单检查      | 路径匹配 `security.ignore.whites` 则直接跳过        |
| 管理端 JWT 校验 | 非白名单路径校验若依 JWT，失败返回 401                |
| 内部请求头      | 校验通过后注入 `X-Admin-Id`、`X-Admin-Roles` 等头 |

#### MallAuthFilter（order=-150）

| 逻辑          | 说明                                                                  |
| ------------- | --------------------------------------------------------------------- |
| 路径判定      | 仅处理 `/api/` 开头的路径，非 `/api/` 路径直接放行                |
| C 端 JWT 校验 | 校验 mall-auth 签发的 JWT，失败返回 401                               |
| 用户状态检查  | 校验用户状态（正常/冻结/注销）                                        |
| 内部请求头    | 校验通过后注入 `X-User-Id`、`X-Member-Id` 等头                    |
| 认证失败日志  | `log.warn("[C 端鉴权失败] 请求路径:{} 原因:xxx")`，走 info.log 落盘 |

#### 执行流程示例

| 路径                            | AuthFilter 行为            | MallAuthFilter 行为                        | 结果                                                     |
| ------------------------------- | -------------------------- | ------------------------------------------ | -------------------------------------------------------- |
| `/mall-user/user/list`        | 非白名单 → 校验管理端 JWT | 非 `/api/` 开头 → 放行                  | 管理端请求                                               |
| `/api/auth/login`             | 在白名单中 → 放行         | `/api/` 开头 → 放行（登录接口无 token） | C 端登录                                                 |
| `/api/user/profile`           | 在白名单中 → 放行         | `/api/` 开头 → 校验 C 端 JWT            | C 端认证请求                                             |
| `/callback/payment/wechat`    | 在白名单中 → 放行         | 非 `/api/` 开头 → 放行                  | 支付回调                                                 |
| `/mall-user/api/user/profile` | 非白名单 → 校验管理端 JWT | `/api/` 开头 → 校验 C 端 JWT            | 管理端 JWT 校验失败（C 端用户）→ 401                    |
| `/mall-admin/user/list`       | 非白名单 → 校验管理端 JWT | 非 `/api/` 开头 → 放行                  | 管理端请求，需有效管理端 JWT + mall-admin:user:list 权限 |

> `discovery.locator` 自动路由会暴露 `/{serviceId}/api/**` 路径，这些路径不在 AuthFilter 白名单中，因此 C 端用户访问会被拦截——形成意外安全防护。

### 2.2 白名单配置

```yaml
security:
  ignore:
    whites:
      # 静态资源
      - /favicon.ico
      - /css/**
      - /js/**
      - /fonts/**
      # 健康检查
      - /actuator/**
      # 验证码
      - /captcha/**
      # 管理端登录
      - /auth/**
      # C 端接口（不在 AuthFilter 校验范围，由 MallAuthFilter 处理）
      - /api/**
      # 支付回调（完全放行，签名验签由下游服务自行处理）
      - /callback/**
```

---

## 3 路由策略

### 3.1 管理端路由

由 `discovery.locator.enabled: true` 自动生成，不配显式路由。

```
/{serviceId}/{controllerPath}?{args}
```

例如：

- `/mall-user/user/list` → discovery.locator → mall-user(9302) → `@RequestMapping("/user")` → list 方法
- `/mall-order/order/list` → discovery.locator → mall-order(9304) → `@RequestMapping("/order")` → list 方法
- `/mall-admin/user/list` → discovery.locator → mall-admin(9207) → `@RequestMapping("/user")` → MallUserController.list

> mall-admin 的管理端 Controller 位于 `com.mall.admin.{domain}.controller` 包下，`@RequestMapping` 路径按业务域命名（如 `/user`、`/product`）。

### 3.2 C 端路由

显式配置，StripPrefix=0（保留 `/api` 前缀），路径与 Controller 的 `@RequestMapping` 一致。

```yaml
spring:
  cloud:
    gateway:
      routes:
        # mall-auth C 端
        - id: mall-auth-api
          uri: lb://mall-auth
          predicates:
            - Path=/api/auth/**
          filters:
            - StripPrefix=0
        # mall-user C 端
        - id: mall-user-api
          uri: lb://mall-user
          predicates:
            - Path=/api/user/**
          filters:
            - StripPrefix=0
        # mall-product C 端
        - id: mall-product-api
          uri: lb://mall-product
          predicates:
            - Path=/api/product/**
          filters:
            - StripPrefix=0
        # mall-order C 端（含购物车）
        - id: mall-order-api
          uri: lb://mall-order
          predicates:
            - Path=/api/order/**
          filters:
            - StripPrefix=0
        # mall-payment C 端
        - id: mall-payment-api
          uri: lb://mall-payment
          predicates:
            - Path=/api/payment/**
          filters:
            - StripPrefix=0
        # mall-marketing C 端
        - id: mall-marketing-api
          uri: lb://mall-marketing
          predicates:
            - Path=/api/marketing/**
          filters:
            - StripPrefix=0
        # mall-search C 端
        - id: mall-search-api
          uri: lb://mall-search
          predicates:
            - Path=/api/search/**
          filters:
            - StripPrefix=0
```

> C 端 Controller 放在 `controller/` 包下，`@RequestMapping` 路径**含 `/api` 前缀**（如 `/api/user/profile`）。

---

## 4 请求头清洗与注入

网关在转发前按以下顺序处理：

```
① 剥离：删除请求中所有 X-User-*、X-Admin-*、X-Internal-*
② 认证：
    ├─ 非白名单路径 → AuthFilter 校验管理端 JWT → 解析 adminId + 角色 + 权限码
    └─ /api/** → MallAuthFilter 校验 C 端 JWT → 解析 userId + memberId
③ 注入：写入可信内部请求头
    ├─ X-Request-Id: traceId
    ├─ (管理端) X-Admin-Id: xxx, X-Admin-Roles: [...]
    └─ (C端) X-User-Id: xxx, X-Member-Id: xxx
④ 签名：生成 X-Internal-Timestamp / X-Internal-Nonce / X-Internal-Signature
⑤ 转发：路由到目标服务
```

---

## 5 Nacos 配置

`ruoyi-gateway-dev.yml` 完整配置（保留若依原有结构，新增 C 端路由和白名单）：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:
  cloud:
    gateway:
      server:
        webflux:
          discovery:
            locator:
              lowerCaseServiceId: true
              enabled: true
          routes:
            # ─── 若依管理端路由 ───
            # 认证中心
            - id: ruoyi-auth
              uri: lb://ruoyi-auth
              predicates:
                - Path=/auth/**
              filters:
                # 验证码处理
                - name: CacheRequestBody
                  args:
                    bodyClass: java.lang.String
                - ValidateCodeFilter
                - StripPrefix=1
            # 代码生成
            - id: ruoyi-gen
              uri: lb://ruoyi-gen
              predicates:
                - Path=/code/**
              filters:
                - StripPrefix=1
            # 定时任务
            - id: ruoyi-job
              uri: lb://ruoyi-job
              predicates:
                - Path=/schedule/**
              filters:
                - StripPrefix=1
            # 系统模块
            - id: ruoyi-system
              uri: lb://ruoyi-system
              predicates:
                - Path=/system/**
              filters:
                - StripPrefix=1
            # 文件服务
            - id: ruoyi-file
              uri: lb://ruoyi-file
              predicates:
                - Path=/file/**
              filters:
                - StripPrefix=1
            # 商城管理端
            - id: mall-admin
              uri: lb://mall-admin
              predicates:
                - Path=/mall-admin/**
              filters:
                - StripPrefix=1

            # ─── C 端路由（显式路由，StripPrefix=0，保留 /api 前缀） ───
            - id: mall-auth-api
              uri: lb://mall-auth
              predicates:
                - Path=/api/auth/**
              filters:
                - StripPrefix=0
            - id: mall-user-api
              uri: lb://mall-user
              predicates:
                - Path=/api/user/**
              filters:
                - StripPrefix=0
            - id: mall-product-api
              uri: lb://mall-product
              predicates:
                - Path=/api/product/**
              filters:
                - StripPrefix=0
            - id: mall-order-api
              uri: lb://mall-order
              predicates:
                - Path=/api/order/**
              filters:
                - StripPrefix=0
            - id: mall-payment-api
              uri: lb://mall-payment
              predicates:
                - Path=/api/payment/**
              filters:
                - StripPrefix=0
            - id: mall-marketing-api
              uri: lb://mall-marketing
              predicates:
                - Path=/api/marketing/**
              filters:
                - StripPrefix=0
            - id: mall-search-api
              uri: lb://mall-search
              predicates:
                - Path=/api/search/**
              filters:
                - StripPrefix=0

# 安全配置
security:
  # 验证码
  captcha:
    enabled: true
    type: math
  # 防止XSS攻击
  xss:
    enabled: true
    excludeUrls:
      - /system/notice
  # 不校验白名单
  ignore:
    whites:
      - /auth/logout
      - /auth/login
      - /auth/register
      - /*/v2/api-docs
      - /*/v3/api-docs
      - /csrf
      # C 端接口（不在 AuthFilter 校验范围，由 MallAuthFilter 处理）
      - /api/**

# springdoc配置
springdoc:
  webjars:
    # 访问前缀
    prefix:

# C 端安全配置（jwt-secret 从 application-dev.yml 共享配置继承，网关不再单独配置）
mall:
  security:
    anonymous-paths:
      - /api/auth/sessions
      - /api/auth/sessions/sms
      - /api/auth/users
      - /api/auth/sms_codes
      - /api/auth/captcha
      - /api/auth/captcha/register
      - /api/auth/captcha/login
      - /api/auth/captcha/password/reset
      - /api/auth/sessions/refresh
      - /api/product/categories
      - /api/product/categories/**
      - /api/product/spus
      - /api/product/spus/**
      - /api/product/skus/**
      - /api/product/brands
      - /api/product/search/fallback
      - /api/search/**
```

---

## 6 关键约束

| 约束                              | 说明                                                                                                     |
| --------------------------------- | -------------------------------------------------------------------------------------------------------- |
| `discovery.locator` 不可关闭    | 管理端前端依赖 `/{serviceId}/**` 自动路由                                                              |
| AuthFilter 与 MallAuthFilter 串行 | 两条过滤器不是二选一，MallAuthFilter 在前者通过后二次校验                                                |
| 白名单顺序                        | `/api/**` 必须放入 `security.ignore.whites`，否则 AuthFilter 拦截 C 端请求                           |
| Controller 包路径                 | 管理端 `controller/admin/` + 不含 `/admin` 的路由前缀；C 端 `controller/` + 含 `/api` 的路由前缀 |
