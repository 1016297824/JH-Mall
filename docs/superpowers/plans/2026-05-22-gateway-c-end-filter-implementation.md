# 网关 C 端过滤器链实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 在 ruoyi-gateway 中新增 MallAuthFilter 和 MallAuthProperties，修改 AuthFilter 增加请求头清洗，令 C 端请求可正确鉴权并转发。

**架构：** 在现有 AuthFilter（-200）之后插入 MallAuthFilter（-150），两个 GlobalFilter 串行执行。MallAuthFilter 对 `/api/**` 路径执行 C 端 JWT 校验（使用 Nacos 配置的独立密钥 `mall.security.jwt-secret`），白名单路径走 `mall.auth.whites`。

**Tech Stack：** Spring Cloud Gateway WebFlux（ServerWebExchange）、jjwt 0.9.1（`io.jsonwebtoken`）、Nacos 配置（`@RefreshScope` + `@ConfigurationProperties`）

---

### Task 1: 创建 MallAuthProperties

**Files:**
- Create: `server/ruoyi/ruoyi-gateway/src/main/java/com/ruoyi/gateway/config/properties/MallAuthProperties.java`

- [ ] **Step 1: 创建 MallAuthProperties.java**

```java
package com.ruoyi.gateway.config.properties;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

/**
 * C 端认证白名单配置
 */
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "mall.auth")
public class MallAuthProperties
{
    /**
     * C 端匿名白名单（MallAuthFilter 不校验的路径）
     */
    private List<String> whites = new ArrayList<>();

    public List<String> getWhites()
    {
        return whites;
    }

    public void setWhites(List<String> whites)
    {
        this.whites = whites;
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -f server/ruoyi/pom.xml -pl ruoyi-gateway -am -DskipTests`
Expected: BUILD SUCCESS（若失败因依赖缺失，需检查父 pom 中是否有 ruoyi-gateway 模块）

- [ ] **Step 3: Commit**

```bash
git add server/ruoyi/ruoyi-gateway/src/main/java/com/ruoyi/gateway/config/properties/MallAuthProperties.java
git commit -m "feat(gateway): 新增 MallAuthProperties 读取 C 端白名单配置"
```

---

### Task 2: 修改 AuthFilter — 增加请求头清洗

**Files:**
- Modify: `server/ruoyi/ruoyi-gateway/src/main/java/com/ruoyi/gateway/filter/AuthFilter.java:42-83`

- [ ] **Step 1: 在 `filter()` 方法中加入请求头清洗**

在 `String userid = JwtUtils.getUserId(claims);` 行（第 70 行）之后、注入 header 之前，增加前缀匹配的头清洗：

```java
        // 清洗请求头：删除可能被外部伪造的内部标识头
        removePrefixHeaders(mutate, "X-Admin-");
        removePrefixHeaders(mutate, "X-User-");
        removePrefixHeaders(mutate, "X-Internal-");

        // 设置用户信息到请求
        addHeader(mutate, SecurityConstants.USER_KEY, userkey);
```

然后在 `removeHeader` 方法之后新增 `removePrefixHeaders` 方法：

在 `private void removeHeader(...)` 方法（98~99行）之后：

```java
    /**
     * 批量删除指定前缀的请求头
     */
    private void removePrefixHeaders(ServerHttpRequest.Builder mutate, String prefix)
    {
        mutate.headers(httpHeaders -> {
            List<String> names = new java.util.ArrayList<>(httpHeaders.keySet());
            for (String name : names)
            {
                if (name.startsWith(prefix))
                {
                    httpHeaders.remove(name);
                }
            }
        });
    }
```

需要在类上增加 `import java.util.List;`（若原有 import 没有，加在 import 区末尾）。

- [ ] **Step 2: 编译验证**

Run: `mvn compile -f server/ruoyi/pom.xml -pl ruoyi-gateway -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add server/ruoyi/ruoyi-gateway/src/main/java/com/ruoyi/gateway/filter/AuthFilter.java
git commit -m "feat(gateway): AuthFilter 增加请求头清洗（防伪造 X-Admin-/X-User-/X-Internal-）"
```

---

### Task 3: 创建 MallAuthFilter

**Files:**
- Create: `server/ruoyi/ruoyi-gateway/src/main/java/com/ruoyi/gateway/filter/MallAuthFilter.java`
- Reference: `server/ruoyi/ruoyi-gateway/src/main/java/com/ruoyi/gateway/filter/AuthFilter.java`（模板）

- [ ] **Step 1: 创建 MallAuthFilter.java**

```java
package com.ruoyi.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import java.util.ArrayList;
import java.util.List;
import com.ruoyi.common.core.constant.HttpStatus;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.gateway.config.properties.MallAuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import reactor.core.publisher.Mono;

/**
 * C 端网关鉴权过滤器
 * 仅处理 /api/** 路径的 C 端请求，校验 JWT 并注入 X-User-Id
 */
@Component
public class MallAuthFilter implements GlobalFilter, Ordered
{
    private static final Logger log = LoggerFactory.getLogger(MallAuthFilter.class);

    @Autowired
    private MallAuthProperties mallAuthProperties;

    @Value("${mall.security.jwt-secret:}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpRequest.Builder mutate = request.mutate();
        String url = request.getURI().getPath();

        // 仅处理 /api/ 开头的 C 端请求
        if (!url.startsWith("/api/"))
        {
            return chain.filter(exchange);
        }

        // 匿名白名单检查
        if (StringUtils.matches(url, mallAuthProperties.getWhites()))
        {
            return chain.filter(exchange);
        }

        // 获取 token
        String token = getToken(request);
        if (StringUtils.isEmpty(token))
        {
            log.warn("[C端鉴权] 缺少令牌，路径:{}", url);
            return unauthorizedResponse(exchange, "请先登录");
        }

        // 解码 JWT
        Claims claims = parseCJwt(token);
        if (claims == null)
        {
            return unauthorizedResponse(exchange, "登录凭证已过期或无效，请重新登录");
        }

        Object userIdObj = claims.get("userId");
        if (userIdObj == null)
        {
            return unauthorizedResponse(exchange, "令牌验证失败");
        }

        // 清洗可能被外部伪造的 X-User-* 头
        removePrefixHeaders(mutate, "X-User-");

        // 注入 C 端用户标识
        addHeader(mutate, "X-User-Id", userIdObj.toString());

        return chain.filter(exchange.mutate().request(mutate.build()).build());
    }

    /**
     * 解析 C 端 JWT（使用独立的密钥）
     */
    private Claims parseCJwt(String token)
    {
        if (StringUtils.isEmpty(jwtSecret))
        {
            log.error("[C端鉴权] jwt-secret 未配置");
            return null;
        }
        try
        {
            return Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody();
        }
        catch (ExpiredJwtException e)
        {
            log.warn("[C端鉴权] 令牌已过期");
        }
        catch (SignatureException | MalformedJwtException e)
        {
            log.warn("[C端鉴权] 令牌签名无效或格式错误");
        }
        catch (Exception e)
        {
            log.error("[C端鉴权] 令牌解析异常: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取请求 token（从 Authorization: Bearer xxx）
     */
    private String getToken(ServerHttpRequest request)
    {
        String token = request.getHeaders().getFirst("Authorization");
        if (StringUtils.isNotEmpty(token) && token.startsWith("Bearer "))
        {
            token = token.substring(7);
        }
        return token;
    }

    private void addHeader(ServerHttpRequest.Builder mutate, String name, String value)
    {
        if (value == null)
        {
            return;
        }
        mutate.header(name, value);
    }

    private void removePrefixHeaders(ServerHttpRequest.Builder mutate, String prefix)
    {
        mutate.headers(httpHeaders -> {
            List<String> names = new ArrayList<>(httpHeaders.keySet());
            for (String name : names)
            {
                if (name.startsWith(prefix))
                {
                    httpHeaders.remove(name);
                }
            }
        });
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String msg)
    {
        log.warn("[C端鉴权异常] 路径:{}, 信息:{}", exchange.getRequest().getPath(), msg);
        return ServletUtils.webFluxResponseWriter(exchange.getResponse(), msg, HttpStatus.UNAUTHORIZED);
    }

    @Override
    public int getOrder()
    {
        return -150;
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -f server/ruoyi/pom.xml -pl ruoyi-gateway -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add server/ruoyi/ruoyi-gateway/src/main/java/com/ruoyi/gateway/filter/MallAuthFilter.java
git commit -m "feat(gateway): 新增 MallAuthFilter 处理 C 端 JWT 鉴权"
```

---

### Task 4: 生成 C 端 JWT 密钥 + 更新 Nacos 配置

Nacos 配置不在代码仓库中，需手动登录 Nacos 控制台（127.0.0.1:8848）或在 Nacos 运行时 API 中更新。

- [ ] **Step 1: 生成 jwt-secret**

Run: 
```powershell
# 生成 64 字符随机 HEX 作为 JWT secret
$jwtSecret = -join ((48..57) + (97..102) | Get-Random -Count 64 | % {[char]$_})
Write-Output "JWT Secret: $jwtSecret"

# 生成 32 字节 AES key（Base64）
$aesKeyBytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($aesKeyBytes)
$aesKey = [Convert]::ToBase64String($aesKeyBytes)
Write-Output "AES Key: $aesKey"
```

记录生成的 `$jwtSecret` 和 `$aesKey` 值。

- [ ] **Step 2: 更新 `ruoyi-gateway-dev.yml`（Nacos 控制台）**

登录 Nacos 控制台 → 配置管理 → 选择 `ruoyi-gateway-dev.yml` → 编辑。

追加到 `security.ignore.whites` 末尾：
```yaml
      # C 端接口（不在 AuthFilter 校验范围，由 MallAuthFilter 处理）
      - /api/**
      # 支付回调（完全放行，签名验签由 mall-payment 自行处理）
      - /callback/**
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
# C 端认证配置
mall:
  auth:
    whites:
      - /api/auth/users
      - /api/auth/sms_codes
      - /api/auth/sessions
      - /api/auth/sessions/sms
      - /api/auth/sessions/refresh
      - /api/auth/password/reset
  security:
    jwt-secret: <生成的 jwtSecret>
```

- [ ] **Step 3: 更新 `mall-auth-dev.yml`（Nacos 控制台）**

若此配置不存在，则在 Nacos 控制台创建。内容基础可参考：
```yaml
# spring配置
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
# mybatis配置
mybatis:
  typeAliasesPackage: com.mall.auth.**.domain
  mapperLocations: classpath:mapper/**/*.xml
# springdoc配置
springdoc:
  gatewayUrl: http://localhost:8080/${spring.application.name}
  api-docs:
    enabled: true
  info:
    title: '认证模块接口文档'
    description: '认证模块接口描述'
    contact:
      name: RuoYi
      url: https://ruoyi.vip
# mall-auth 业务配置
mall:
  auth:
    access-token-ttl: 1800
    refresh-token-ttl: 604800
    sms:
      code-length: 6
      code-ttl: 300
      cooldown: 60
      daily-limit: 5
      ip-daily-limit: 10
    pwd-err-limit: 5
    pwd-err-ttl: 1800
    pwd-bcrypt-cost: 12
    wechat:
      app-id:
      app-secret:
    decrypt:
      cache-ttl: 60
      batch-limit: 50
  security:
    jwt-secret: <生成的 jwtSecret>
    aes-key: <生成的 aesKey>
```

- [ ] **Step 4: 验证 Nacos 配置**

用浏览器访问 Nacos 控制台（http://127.0.0.1:8848/nacos），确认两条配置都已保存且内容正确。

---

### Task 5: 集成验证

需要项目已编译、Nacos 运行中、至少 ruoyi-gateway 启动成功。

- [ ] **Step 1: 完整编译**

Run: `mvn clean install -f server/ruoyi/pom.xml -DskipTests`
Expected: BUILD SUCCESS（ruoyi-gateway 编译通过）

- [ ] **Step 2: 启动 ruoyi-gateway 验证**

在 Nacos 已启动的前提下：
```bash
mvn spring-boot:run -f server/ruoyi/pom.xml -pl ruoyi-gateway -am
```
Expected：启动无异常，日志中出现 `MallAuthFilter` 相关的初始化信息。

- [ ] **Step 3: 请求测试**

验证管理端仍正常工作：
```bash
curl http://localhost:8080/user/list -H "Authorization: Bearer <管理端token>"
```
Expected: HTTP 200

验证 C 端匿名路径（注册）可正常通过网关：
```bash
curl http://localhost:8080/api/auth/users -X POST -H "Content-Type: application/json" -d '{}'
```
Expected: HTTP 401（因为 mall-auth 模块尚未实现，这是预期的 —— 关键是不被 AuthFilter 拦截 401）

验证 C 端需认证路径被拦截：
```bash
curl http://localhost:8080/api/user/profile
```
Expected: HTTP 401 + "请先登录"

---

### Task 6: （可选）单元测试 MallAuthFilter

- [ ] **Step 1: 创建测试类**

`server/ruoyi/ruoyi-gateway/src/test/java/com/ruoyi/gateway/filter/MallAuthFilterTest.java`

```java
package com.ruoyi.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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
    void testParseCJwt_ExpiredToken_ReturnsNull()
    {
        String userId = "1001";
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);

        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200_000))
                .setExpiration(new Date(System.currentTimeMillis() - 3600_000))
                .signWith(SignatureAlgorithm.HS512, TEST_SECRET)
                .compact();

        try
        {
            Jwts.parser().setSigningKey(TEST_SECRET).parseClaimsJws(token).getBody();
        }
        catch (Exception e)
        {
            assertNotNull(e);
        }
    }
}
```

- [ ] **Step 2: 运行测试**

Run: `mvn test -f server/ruoyi/pom.xml -pl ruoyi-gateway -am`
Expected: BUILD SUCCESS（2 个测试通过）

- [ ] **Step 3: Commit**

```bash
git add server/ruoyi/ruoyi-gateway/src/test/java/com/ruoyi/gateway/filter/MallAuthFilterTest.java
git commit -m "test(gateway): MallAuthFilter JWT 编解码单元测试"
```
