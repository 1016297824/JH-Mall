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
    // 加载 mall.security.jwtSecret + mall.security.whites
}
```

**执行逻辑：**

| 步骤 | 说明 |
|------|------|
| ① 路径判定 | 非 `/api/**` → 直接放行 |
| ② 白名单检查 | 路径在 `mall.security.whites` 中 → 放行（无 token） |
| ③ 头清洗 | 剥离入站 `X-User-*` 和 `X-Mall-*` 请求头 |
| ④ TraceId | 注入 `X-Request-Id`（若不存在） |
| ⑤ Token 获取 | 从 `Authorization: Bearer <token>` 头取值 |
| ⑥ JWT 解码 | 用 `mall.security.jwt-secret`（Nacos）解码 |
| ⑦ 提取 userId | 从 payload `userId` 字段取值 |
| ⑧ 注入头 | 设置 `X-User-Id`、`X-User-Name: c_end_user` → 转发 |

**错误处理：**

| 场景 | 行为 |
|------|------|
| 无 token | 返回 401 + "C 端请求缺少 token" |
| token 过期 | 返回 401 + "C 端 token 已过期" |
| token 签名无效 | 返回 401 + "C 端 token 签名无效" |
| token 格式错误 | 返回 401 + "C 端 token 无效" |
| 白名单路径 | 放行（不校验 token） |

### 2.3 执行流示例

| 路径 | AuthFilter | MallAuthFilter | 结果 |
|------|-----------|---------------|------|
| `/mall-user/user/list` | 校验管理端 JWT | 非 `/api/` 放行 | 管理端请求 |
| `/api/auth/register` | `/api/**` 白名单 → 放行 | 在 `mall.security.whites` → 放行 | C 端注册 |
| `/api/user/profile` | `/api/**` 白名单 → 放行 | 校验 C 端 JWT | C 端认证请求 |

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

### 3.2 `ruoyi-gateway-dev.yml` C 端路由追加（MVP）

在 `spring.cloud.gateway.routes` 中追加（管理端 5 条路由保留不变）：

```yaml
        # C 端认证
        - id: mall-auth
          uri: lb://mall-auth
          predicates:
            - Path=/api/auth/**
          filters:
            - StripPrefix=1
        # C 端用户
        - id: mall-user
          uri: lb://mall-user
          predicates:
            - Path=/api/user/**
          filters:
            - StripPrefix=1
        # C 端商品
        - id: mall-product
          uri: lb://mall-product
          predicates:
            - Path=/api/product/**
          filters:
            - StripPrefix=1
```

> **注意**：`StripPrefix=1` 移除 `/api` 前缀，下游服务 Controller `@RequestMapping` 不包含 `/api`。例如 `/api/auth/login` → 转发至 mall-auth 的 `/login`。

### 3.3 `ruoyi-gateway-dev.yml` C 端白名单追加（MVP）

```yaml
# C 端安全配置
mall:
  security:
    jwt-secret: <Base64 64 字节 HS512 密钥>
    whites:
      - /api/auth/login
      - /api/auth/register
      - /api/auth/sms-code
```

> 白名单为 MVP 简化版，非 MVP 路径（如密码重置、微信登录）后续逐步加入。

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
    jwt-secret: <Base64 64 字节 HS512 密钥>
```

> 实施时自动生成 64 字节随机密钥并 Base64 编码（HS512 要求至少 64 字节），回写到两处配置。

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
| `MallAuthProperties` | 读取 `mall.security.jwtSecret` + `mall.security.whites` |
| `io.jsonwebtoken.Jwts` | C 端 JWT 解码 |
| `io.jsonwebtoken.Claims` | 提取 userId |

JWT 解码使用 `mallAuthProperties.getJwtSecret()`（非 `@Value`），通过 `@PostConstruct checkConfig()` 在启动时验证密钥已配置，未配置则抛 `IllegalStateException` 阻止启动。

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
@ConfigurationProperties(prefix = "mall.security")
public class MallAuthProperties {
    /** C 端 JWT 密钥（HS512，至少 32 字符） */
    private String jwtSecret;

    /** C 端白名单路径（逗号分隔） */
    private String[] whites = new String[] {};
}
```

---

## 五、关键约束

| 约束 | 说明 |
|------|------|
| `discovery.locator.enabled: true` 不可关闭 | 管理端前端依赖 `/{serviceId}` 自动路由 |
| AuthFilter 与 MallAuthFilter 串行 | 两者不是二选一关系 |
| `/api/**` 必须在 `security.ignore.whites` 中 | 否则 AuthFilter 拦截 C 端请求 |
| C 端 Controller `@RequestMapping` 不含 `/api` 前缀 | 网关 `StripPrefix=1` 已移除 `/api`，如 `@RequestMapping("/auth")` |
| 管理端 Controller `@RequestMapping` 不含 `/api` 前缀 | 如 `/user/list` |

---

## 六、不在此次范围内的内容

- mall-auth 模块本身的代码实现（后续任务）
- mall-user 内部 Feign 端点（mall-auth 的前置依赖）
- mall-api 契约层代码（mall-auth 的前置依赖）
- AuthFilter 修改（若依原生代码，本次不动）
- AOP 日志脱敏（后续统一处理）
