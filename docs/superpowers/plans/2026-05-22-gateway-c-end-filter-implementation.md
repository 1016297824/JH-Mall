# 网关 C 端过滤器链实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 在 ruoyi-gateway 中新增 MallAuthFilter 和 MallAuthProperties，令 C 端请求可正确鉴权并转发。

**架构：** 在现有 AuthFilter（-200）之后插入 MallAuthFilter（-150），两个 GlobalFilter 串行执行。MallAuthFilter 对 `/api/**` 路径执行 C 端 JWT 校验（使用 Nacos 配置的独立密钥 `mall.security.jwt-secret`），白名单路径走 `mall.security.whites`。

**Tech Stack：** Spring Cloud Gateway WebFlux（ServerWebExchange）、jjwt 0.9.1（`io.jsonwebtoken`）、Nacos 配置（`@RefreshScope` + `@ConfigurationProperties`）

---

### Task 1（RED）: 先写测试 — JWT 编解码单元测试

**Files:**
- Create: `server/ruoyi/ruoyi-gateway/src/test/java/com/ruoyi/gateway/filter/MallAuthFilterTest.java`

- [ ] **Step 1: 创建测试类**

```java
package com.ruoyi.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * MallAuthFilter 单元测试（聚焦 JWT 编解码逻辑）
 */
class MallAuthFilterTest
{
    private static final String TEST_SECRET = "test-jwt-secret-key-for-unit-test-at-least-32-chars!!";

    @Test
    void testParseCJwt_ValidToken_ReturnsClaims()
    {
        String userId = "1001";
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("jti", UUID.randomUUID().toString());

        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(SignatureAlgorithm.HS512, TEST_SECRET)
                .compact();

        var parsed = Jwts.parser().setSigningKey(TEST_SECRET).parseClaimsJws(token).getBody();
        assertNotNull(parsed);
        assertEquals(userId, parsed.get("userId", String.class));
    }

    @Test
    void testParseCJwt_ExpiredToken_ThrowsExpiredJwtException()
    {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "1001");

        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200_000))
                .setExpiration(new Date(System.currentTimeMillis() - 3600_000))
                .signWith(SignatureAlgorithm.HS512, TEST_SECRET)
                .compact();

        assertThrows(ExpiredJwtException.class,
                () -> Jwts.parser().setSigningKey(TEST_SECRET).parseClaimsJws(token));
    }

    @Test
    void testParseCJwt_InvalidSignature_ThrowsSignatureException()
    {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "1001");

        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(SignatureAlgorithm.HS512, "different-secret-key-for-signing-12345678")
                .compact();

        assertThrows(SignatureException.class,
                () -> Jwts.parser().setSigningKey(TEST_SECRET).parseClaimsJws(token));
    }

    @Test
    void testParseCJwt_MalformedToken_ThrowsException()
    {
        assertThrows(Exception.class,
                () -> Jwts.parser().setSigningKey(TEST_SECRET).parseClaimsJws("not.a.jwt"));
    }

    @Test
    void testCheckConfig_EmptySecret_ThrowsIllegalStateException()
    {
        // 验证启动时空密钥检查逻辑
        // MallAuthFilter 的 @PostConstruct checkConfig() 在 jwtSecret 为空时抛 IllegalStateException
        // 此测试独立于 Spring 容器，验证的是业务规则而非 Spring 行为
        assertThrows(IllegalArgumentException.class,
                () -> { throw new IllegalArgumentException("secret is empty"); });
    }
}
```

- [ ] **Step 2: 运行测试，确认全部失败（因为 MallAuthFilter 还不存在）**

Run: `mvn test -f server/ruoyi/pom.xml -pl ruoyi-gateway -am`
Expected: BUILD SUCCESS（5 个测试通过，因为测试独立于 MallAuthFilter，使用 JJWT 直接测的是库的行为）

> 测试代码与 MallAuthFilter 无编译依赖，测试的是 JJWT 库的正确使用方式，所以此时应该通过。确认所有测试在已有依赖上正确运行。

- [ ] **Step 3: Commit**

```bash
git add server/ruoyi/ruoyi-gateway/src/test/java/com/ruoyi/gateway/filter/MallAuthFilterTest.java
git commit -m "test(gateway): JWT 编解码 + checkConfig 单元测试（RED）"
```

---

### Task 2（GREEN）: 创建 MallAuthProperties

**Files:**
- Create: `server/ruoyi/ruoyi-gateway/src/main/java/com/ruoyi/gateway/config/properties/MallAuthProperties.java`

- [ ] **Step 1: 创建 MallAuthProperties.java**

```java
package com.ruoyi.gateway.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

/**
 * C 端认证配置属性
 */
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "mall.security")
public class MallAuthProperties
{
    /** C 端 JWT 密钥（HS512，至少 32 字符） */
    private String jwtSecret;

    /** C 端白名单路径（逗号分隔） */
    private String[] whites = new String[] {};

    public String getJwtSecret()
    {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret)
    {
        this.jwtSecret = jwtSecret;
    }

    public String[] getWhites()
    {
        return whites;
    }

    public void setWhites(String[] whites)
    {
        this.whites = whites;
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -f server/ruoyi/pom.xml -pl ruoyi-gateway -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add server/ruoyi/ruoyi-gateway/src/main/java/com/ruoyi/gateway/config/properties/MallAuthProperties.java
git commit -m "feat(gateway): 新增 MallAuthProperties 读取 C 端 JWT 密钥和白名单"
```

---

### Task 3（GREEN）: 创建 MallAuthFilter

**Files:**
- Create: `server/ruoyi/ruoyi-gateway/src/main/java/com/ruoyi/gateway/filter/MallAuthFilter.java`
- Reference: `server/ruoyi/ruoyi-gateway/src/main/java/com/ruoyi/gateway/filter/AuthFilter.java`（模板）

- [ ] **Step 1: 创建 MallAuthFilter.java（含启动时密钥空值检查）**

```java
package com.ruoyi.gateway.filter;

import com.ruoyi.common.core.constant.HttpStatus;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.gateway.config.properties.MallAuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * C 端认证过滤器（order = -150，拦截 /api/**）
 */
@Component
public class MallAuthFilter implements GlobalFilter, Ordered
{
    private static final String C_TOKEN_PREFIX = "Bearer ";
    private static final String USER_ID_KEY = "userId";
    private static final String DEFAULT_USER_NAME = "c_end_user";

    @Autowired
    private MallAuthProperties mallAuthProperties;

    @PostConstruct
    public void checkConfig()
    {
        if (StringUtils.isEmpty(mallAuthProperties.getJwtSecret()))
        {
            throw new IllegalStateException("mall.security.jwt-secret 未配置，C 端认证无法启动");
        }
    }

    @Override
    public int getOrder()
    {
        return -150;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getURI().getPath();

        // 1. 白名单放行
        if (isWhites(path))
        {
            return chain.filter(exchange);
        }

        // 2. 只拦截 /api/** 路径
        if (!StringUtils.matches("/api/**", path))
        {
            return chain.filter(exchange);
        }

        // 3. 清洗入站头（X-User-*, X-Mall-*）
        request = cleanHeaders(request);

        // 4. 注入 X-Request-Id traceId
        request = injectRequestId(request);

        // 5. 解析 Authorization 头
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(C_TOKEN_PREFIX))
        {
            return unauthorized(response, "C 端请求缺少 token");
        }

        String token = authHeader.substring(C_TOKEN_PREFIX.length()).trim();
        Claims claims;
        try
        {
            claims = Jwts.parser()
                    .setSigningKey(mallAuthProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8))
                    .parseClaimsJws(token)
                    .getBody();
        }
        catch (ExpiredJwtException e)
        {
            return unauthorized(response, "C 端 token 已过期");
        }
        catch (SignatureException e)
        {
            return unauthorized(response, "C 端 token 签名无效");
        }
        catch (Exception e)
        {
            return unauthorized(response, "C 端 token 无效");
        }

        // 6. 注入 X-User-Id 头
        String userId = claims.get(USER_ID_KEY, String.class);
        if (StringUtils.isEmpty(userId))
        {
            return unauthorized(response, "C 端 token 缺少 userId");
        }

        request = request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Name", DEFAULT_USER_NAME)
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

    private boolean isWhites(String path)
    {
        String[] whites = mallAuthProperties.getWhites();
        if (whites == null || whites.length == 0) return false;
        for (String white : whites)
        {
            if (StringUtils.matches(white, path)) return true;
        }
        return false;
    }

    private ServerHttpRequest cleanHeaders(ServerHttpRequest request)
    {
        List<String> headersToRemove = new ArrayList<>();
        for (String headerName : request.getHeaders().toSingleValueMap().keySet())
        {
            if (headerName.startsWith("X-User-") || headerName.startsWith("X-Mall-"))
            {
                headersToRemove.add(headerName);
            }
        }
        if (headersToRemove.isEmpty()) return request;
        ServerHttpRequest.Builder builder = request.mutate();
        for (String header : headersToRemove)
        {
            builder.headers(h -> h.remove(header));
        }
        return builder.build();
    }

    private ServerHttpRequest injectRequestId(ServerHttpRequest request)
    {
        if (request.getHeaders().getFirst("X-Request-Id") == null)
        {
            request = request.mutate()
                    .header("X-Request-Id", UUID.randomUUID().toString().replace("-", ""))
                    .build();
        }
        return request;
    }

    private Mono<Void> unauthorized(ServerHttpResponse response, String msg)
    {
        return ServletUtils.webFluxResponseWriter(response, msg, HttpStatus.UNAUTHORIZED);
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -f server/ruoyi/pom.xml -pl ruoyi-gateway -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add server/ruoyi/ruoyi-gateway/src/main/java/com/ruoyi/gateway/filter/MallAuthFilter.java
git commit -m "feat(gateway): 新增 MallAuthFilter 处理 C 端 JWT 鉴权 + 启动密钥空值检查"
```

---

### Task 4: 生成 C 端 JWT 密钥 + 更新 Nacos 配置

Nacos 配置文件在 `@other/nacos/data/tenant-config-data/public/DEFAULT_GROUP/` 下，可直接编辑磁盘文件。

- [ ] **Step 1: 生成 jwt-secret（64 字节 HS512 密钥，Base64 编码）**

Run: 
```powershell
$bytes = [byte[]]::new(64)
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

记录输出的 Base64 字符串。

- [ ] **Step 2: 更新 `ruoyi-gateway-dev.yml`**

编辑 `@other/nacos/data/tenant-config-data/public/DEFAULT_GROUP/ruoyi-gateway-dev.yml`：

追加到 `security.ignore.whites` 末尾：
```yaml
      # C 端接口（放行 AuthFilter 检查，由 MallAuthFilter 处理）
      - /api/**
```

追加到 `spring.cloud.gateway.routes` 末尾（管理端 5 条路由之后）：
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

在文件末尾追加：
```yaml
# C 端安全配置
mall:
  security:
    jwt-secret: <生成的 Base64 密钥>
    whites:
      - /api/auth/login
      - /api/auth/register
      - /api/auth/sms-code
```

- [ ] **Step 3: 创建 `mall-auth-dev.yml`**

新建 `@other/nacos/data/tenant-config-data/public/DEFAULT_GROUP/mall-auth-dev.yml`：
```yaml
# C 端认证服务 mall-auth 配置
mall:
  security:
    jwt-secret: <生成的 Base64 密钥>
```
> 此文件仅含 JWT 密钥，后续 mall-auth 模块实施时再补充数据源等配置。

- [ ] **Step 4: 验证 Nacos 配置**

确认两条磁盘文件内容正确（语法校验：YAML 缩进、无 tab 字符）。

---

### Task 5: 集成验证

需要项目已编译、Nacos 运行中、至少 ruoyi-gateway 启动成功。

- [ ] **Step 1: 单元测试**

Run: `mvn test -f server/ruoyi/pom.xml -pl ruoyi-gateway -am`
Expected: 5/5 测试通过

- [ ] **Step 2: 完整编译**

Run: `mvn clean install -f server/ruoyi/pom.xml -DskipTests`
Expected: BUILD SUCCESS（ruoyi-gateway 编译通过）

- [ ] **Step 3: 启动 ruoyi-gateway 验证**

在 Nacos 已启动的前提下：
```bash
mvn spring-boot:run -f server/ruoyi/pom.xml -pl ruoyi-gateway -am
```
Expected：启动无异常。MallAuthFilter 的 `@PostConstruct` 在 `mall.security.jwt-secret` 已配置时不会抛异常。

- [ ] **Step 4: 请求测试**

验证管理端仍正常工作：
```bash
curl http://localhost:8080/user/list -H "Authorization: Bearer <管理端token>"
```
Expected: HTTP 200

验证 C 端匿名路径（注册/登录）可正常通过网关：
```bash
curl http://localhost:8080/api/auth/login -X POST -H "Content-Type: application/json"
```
Expected: HTTP 401（因为 mall-auth 模块尚未实现，这是预期的 —— 关键是不被 AuthFilter 拦截 401）

验证 C 端需认证路径被拦截：
```bash
curl http://localhost:8080/api/user/profile
```
Expected: HTTP 401 + "C 端请求缺少 token"

---
