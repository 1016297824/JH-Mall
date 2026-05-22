# 网关 C 端过滤器链设计

> 基于 `docs/design/04_gateway详细设计.md` 的实施规格。
> 目标：在 ruoyi-gateway 中新增 MallAuthFilter，打通 C 端 API 请求通路。

---

## 一、背景

当前网关仅 `AuthFilter`（order=-200）一枚 GlobalFilter，校验管理端 JWT。C 端 API（`/api/**`）不在其白名单中，任何 C 端请求都会被校验管理端 JWT 后返回 401。需要新增一条 C 端过滤器并配置路由，使 C 端请求能正确鉴权并转发。

---

## 二、过滤器链

```
请求 → AuthFilter(-200) → MallAuthFilter(-150) → 路由转发
```

两个过滤器对所有路径都执行，各自按路径判定放行或校验。

### 2.1 AuthFilter（order=-200，已存在，本次不变）

**保留全部已有逻辑**：
- 白名单检查 → 校验管理端 JWT → 注入 X-Admin-* 头 / 清除 `from-source`

### 2.2 MallAuthFilter（order=-150，新建）

```java
@Component
public class MallAuthFilter implements GlobalFilter, Ordered {
    // ORDER = -150
    // 加载 mall.auth.whites 作为匿名路径白名单
}
```

**执行逻辑：**

| 步骤 | 说明 |
|------|------|
| ① 路径判定 | 非 `/api/` 开头 → 直接放行 |
| ② 匿名检查 | 路径在 `mall.auth.whites` 中 → 放行（无 token） |
| ③ Token 获取 | 从 `Authorization: Bearer <token>` 头取值 |
| ④ JWT 解码 | 用 `mall.security.jwt-secret`（Nacos）解码 |
| ⑤ 提取 userId | 从 payload `userId` 字段取值 |
| ⑥ 头清洗 | 剥离 `X-User-*` 请求头 |
| ⑦ 注入头 | 设置 `X-User-Id` → 转发 |

**错误处理：**

| 场景 | 行为 |
|------|------|
| 无 token | 返回 401 + `A0301`（请先登录） |
| token 解码失败 | 返回 401 + `A0310`（登录凭证非法） |
| token 过期 | 返回 401 + `A0231`（登录已过期） |
| 匿名路径带 token | 忽略 token，继续放行 |

### 2.3 执行流示例

| 路径 | AuthFilter | MallAuthFilter | 结果 |
|------|-----------|---------------|------|
| `/mall-user/user/list` | 校验管理端 JWT | 非 `/api/` 放行 | 管理端请求 |
| `/api/auth/users` | `/api/**` 白名单 → 放行 | 在 `mall.auth.whites` → 放行 | C 端注册 |
| `/api/user/profile` | `/api/**` 白名单 → 放行 | 校验 C 端 JWT | C 端认证请求 |
| `/callback/payment/wechat` | `/callback/**` 白名单 → 放行 | 非 `/api/` 放行 | 支付回调 |

---

## 三、Nacos 配置变更

### 3.1 `ruoyi-gateway-dev.yml` 白名单追加

在 `security.ignore.whites` 中添加：

```yaml
      # C 端接口（不在 AuthFilter 校验范围，由 MallAuthFilter 处理）
      - /api/**
      # 支付回调（完全放行，签名验签由 mall-payment 自行处理）
      - /callback/**
```

### 3.2 `ruoyi-gateway-dev.yml` C 端路由追加

在 `spring.cloud.gateway.routes` 中追加（管理端 5 条路由保留不变）：

```yaml
            # ─── C 端路由（StripPrefix=0，保留 /api 前缀） ───
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
```

### 3.3 `ruoyi-gateway-dev.yml` C 端匿名白名单追加

```yaml
# MallAuthFilter 匿名白名单（C 端不需要 token 的路径）
mall:
  auth:
    whites:
      - /api/auth/users
      - /api/auth/sms_codes
      - /api/auth/sessions
      - /api/auth/sessions/sms
      - /api/auth/sessions/refresh
      - /api/auth/password/reset
```

### 3.4 C 端 JWT 密钥配置

`mall.security.jwt-secret` 需要在 **两处 Nacos 配置中保持相同**：

| Nacos DataId | 用途 |
|---|---|
| `ruoyi-gateway-dev.yml` | MallAuthFilter 解码 C 端 JWT |
| `mall-auth-dev.yml` | mall-auth 签名 C 端 JWT |

```yaml
# 两处都要加，值必须一致
mall:
  security:
    jwt-secret: <自动生成的 32+ 字符随机串>
```

`mall.security.aes-key` 仅 `mall-auth-dev.yml` 需要（网关不解密）。

> 实施时自动生成 JWT secret（UUID）和 AES-256 key（Base64 32字节），回写到两处配置。

---

## 四、新增 Java 类

### 4.1 MallAuthFilter

```
位置: server/ruoyi/ruoyi-gateway/src/main/java/com/ruoyi/gateway/filter/MallAuthFilter.java
包:   com.ruoyi.gateway.filter
```

关键依赖：

| 依赖 | 用途 |
|------|------|
| `MallAuthProperties` | 读取 `mall.auth.whites` 配置 |
| `io.jsonwebtoken.Jwts` | C 端 JWT 解码 |
| `io.jsonwebtoken.Claims` | 提取 userId |
| `@Value("${mall.security.jwt-secret}")` | C 端 JWT 签名密钥 |

JWT 载荷结构（与 mall-auth 签发一致）：

```json
{
  "userId": 1001,
  "iat": 1747881600,
  "exp": 1747883400,
  "jti": "uuid"
}
```

### 4.2 MallAuthProperties

```
位置: server/ruoyi/ruoyi-gateway/src/main/java/com/ruoyi/gateway/config/properties/MallAuthProperties.java
```

```java
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "mall.auth")
public class MallAuthProperties {
    private List<String> whites = new ArrayList<>();
}
```

---

## 五、关键约束

| 约束 | 说明 |
|------|------|
| `discovery.locator.enabled: true` 不可关闭 | 管理端前端依赖 `/{serviceId}` 自动路由 |
| AuthFilter 与 MallAuthFilter 串行 | 两者不是二选一关系 |
| `/api/**` 必须在 `security.ignore.whites` 中 | 否则 AuthFilter 拦截 C 端请求 |
| C 端 Controller `@RequestMapping` 含 `/api` 前缀 | 如 `/api/user/profile` |
| 管理端 Controller `@RequestMapping` 不含 `/api` 前缀 | 如 `/user/list` |

---

## 六、不在此次范围内的内容

- mall-auth 模块本身的代码实现（后续任务）
- mall-user 内部 Feign 端点（mall-auth 的前置依赖）
- mall-api 契约层代码（mall-auth 的前置依赖）
- AuthFilter 修改（若依原生代码，本次不动，MallAuthFilter 中已有 X-User-* 清洗）
- AOP 日志脱敏（后续统一处理）
