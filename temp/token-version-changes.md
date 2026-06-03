# token_version 改造改动清单（修正版）

> 目标：accessToken 有效性从"Redis session key 存在性"改为"JWT.ver == Redis token_version"
> 
> 核心思路：
> - **token_version**（DB + Redis 缓存）→ 负责全端下线（改密码 / 踢人）
> - **JTI 黑名单 + refresh mapping**（Redis TTL）→ 负责 refreshToken 一次性轮换
> - **不再写 session key** → 删除 `mall:auth:session:{userId}:{jti}` 相关逻辑

---

## A. 数据库（1 项）

### 1. 新增 Flyway 迁移脚本

**文件：** `db/mall-sql/V1.0.4__add_token_version.sql`

```sql
ALTER TABLE mall_user ADD COLUMN token_version INT NOT NULL DEFAULT 1;
```

---

## B. mall-common（1 项）

### 2. CacheConstants.Auth

**文件：** `server/mall/mall-common/src/main/java/com/mall/common/constant/CacheConstants.java`

改动：
- 新增 `USER_VERSION = "mall:auth:user_version:"`
- 删除 `SESSION = "mall:auth:session:"` 常量

```
/**
 * 用户 token 版本号
 *
 * <p>Key 模式：{@code mall:auth:user_version:{userId}}</p>
 * <ul>
 *   <li>TTL：Nacos {@code mall.auth.token-version-cache-ttl} 配置（默认 30d）</li>
 *   <li>数据结构：Integer</li>
 *   <li>缓存 miss 时回源 DB 兜底</li>
 * </ul>
 */
public static final String USER_VERSION = "mall:auth:user_version:";
```

---

## C. mall-auth — MallAuthConfigProperties（1 项）

**文件：** `server/mall/mall-auth/src/main/java/com/mall/auth/config/MallAuthConfigProperties.java`

### 3. 新增 Nacos 配置项

新增字段 `tokenVersionCacheTtl`，默认 2592000（30d），支持 Nacos 热刷新。

```java
/** token_version Redis 缓存 TTL（秒，默认 2592000 = 30 天） */
private long tokenVersionCacheTtl = 2592000;
```

> 对应对应的 getter/setter。

---

## D. mall-auth — TokenServiceImpl

**文件：** `server/mall/mall-auth/src/main/java/com/mall/auth/service/impl/TokenServiceImpl.java`

### 3. 新增私有方法

**说明：** `getTokenVersion()` 优先 Redis，miss 则回源 DB 并写回缓存。统一返回 Integer，避免类型转换错误。`parseUserId()` 统一处理 userId 转 Long 的异常防护。

```java
/**
 * 获取用户 token_version（优先 Redis，miss 则查 DB 并写缓存）
 */
private Integer getTokenVersion(Long userId) {
    String key = CacheConstants.Auth.USER_VERSION + userId;
    Integer version = (Integer) redisTemplate.opsForValue().get(key);
    if (version != null) {
        return version;
    }
    // 缓存 miss → 查 DB 并写回缓存（TTL 通过 Nacos mall.auth.token-version-cache-ttl 配置）
    version = remoteUserService.getTokenVersion(String.valueOf(userId));
    if (version != null) {
        redisTemplate.opsForValue().set(key, version, authProperties.getTokenVersionCacheTtl(), TimeUnit.SECONDS);
    } else {
        log.warn("getTokenVersion: DB 未查询到用户版本, userId={}", userId);
    }
    return version;
}

/**
 * 更新 Redis 缓存中的 token_version（TTL 由 Nacos 配置）
 */
private void updateVersionCache(Long userId, Integer version) {
    String key = CacheConstants.Auth.USER_VERSION + userId;
    redisTemplate.opsForValue().set(key, version, authProperties.getTokenVersionCacheTtl(), TimeUnit.SECONDS);
}

/**
 * userId 转 Long（统一异常防护）
 */
private Long parseUserId(String userId) {
    try {
        return Long.parseLong(userId);
    } catch (NumberFormatException e) {
        log.error("parseUserId: userId 转换失败, userId={}", userId, e);
        throw new TokenException(ErrorCode.TOKEN_INVALID);
    }
}
```

### 4. `issue()` — JWT 新增 ver claim + 删除 session key

```java
@Override
public TokenRespDTO issue(String userId) {
    byte[] key = securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
    Date now = new Date();
    Date accessExp = new Date(now.getTime() + authProperties.getAccessTokenTtl() * 1000);
    Date refreshExp = new Date(now.getTime() + authProperties.getRefreshTokenTtl() * 1000);

    String accessJti = UUID.randomUUID().toString();
    String refreshJti = UUID.randomUUID().toString();

    // 获取当前 token_version
    Long uid = parseUserId(userId);
    Integer version = getTokenVersion(uid);
    if (version == null) {
        throw new TokenException(ErrorCode.TOKEN_INVALID);
    }

    // 构建 accessToken（新增 ver claim）
    String accessToken = Jwts.builder()
            .setId(accessJti)
            .setSubject(userId)
            .setIssuedAt(now)
            .setExpiration(accessExp)
            .claim("type", "access")
            .claim("userId", userId)
            .claim("ver", version)       // 新增
            .setIssuer("mall-auth")
            .signWith(SignatureAlgorithm.HS512, key)
            .compact();

    // 构建 refreshToken（新增 ver claim）
    String refreshToken = Jwts.builder()
            .setId(refreshJti)
            .setSubject(userId)
            .setIssuedAt(now)
            .setExpiration(refreshExp)
            .claim("type", "refresh")
            .claim("userId", userId)
            .claim("ver", version)       // 新增
            .setIssuer("mall-auth")
            .signWith(SignatureAlgorithm.HS512, key)
            .compact();

    // 删除：不再写 session key

    // refreshToken 映射缓存
    String refreshKey = CacheConstants.Auth.REFRESH + refreshJti;
    redisTemplate.opsForValue().set(refreshKey, userId, authProperties.getRefreshTokenTtl(), TimeUnit.SECONDS);

    return new TokenRespDTO(accessToken, refreshToken, authProperties.getAccessTokenTtl());
}
```

### 5. `verify()` — 改为 token_version 比对 + JTI 黑名单检查保留

**说明：** 保留黑名单检查（内部服务调用也可能走 verify），session 检查替换为 version 比对。version 取 Integer 直接比对，避免类型转换错误。

```java
@Override
public String verify(String accessToken) {
    Claims claims = parseToken(accessToken);
    String jti = claims.getId();
    String userId = claims.getSubject();
    Integer jwtVersion = claims.get("ver", Integer.class);

    // JTI 黑名单检查（保留：内部调用 verify 的场景也需拦截）
    String blacklistKey = CacheConstants.Auth.BLACKLIST + jti;
    if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
        throw new TokenException(ErrorCode.TOKEN_INVALID);
    }

    // token_version 比对（cache miss 回源 DB）
    Long uid = parseUserId(userId);
    Integer currentVersion = getTokenVersion(uid);
    if (jwtVersion == null) {
        log.warn("verify: jwt 中 ver 为空, userId={}, jti={}", userId, jti);
        throw new TokenException(ErrorCode.TOKEN_INVALID);
    }
    if (!jwtVersion.equals(currentVersion)) {
        throw new TokenException(ErrorCode.TOKEN_INVALID);
    }

    return userId;
}
```

### 6. `refresh()` — 新增 token_version 比对

**说明：** 全端下线后，旧 refreshToken 的 ver 与 DB 不一致，刷新时必须拒绝。

```java
@Override
public TokenRespDTO refresh(String refreshToken) {
    Claims claims = parseToken(refreshToken);

    String type = claims.get("type", String.class);
    if (!"refresh".equals(type)) {
        throw new TokenException(ErrorCode.TOKEN_INVALID);
    }

    String jti = claims.getId();
    String userId = claims.getSubject();
    Date expiration = claims.getExpiration();
    Integer jwtVersion = claims.get("ver", Integer.class);

    // JTI 黑名单检查
    String blacklistKey = CacheConstants.Auth.BLACKLIST + jti;
    if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
        throw new TokenException(ErrorCode.TOKEN_INVALID);
    }

    // refresh 映射检查
    String refreshMappingKey = CacheConstants.Auth.REFRESH + jti;
    if (Boolean.FALSE.equals(redisTemplate.hasKey(refreshMappingKey))) {
        throw new TokenException(ErrorCode.TOKEN_INVALID);
    }

    // token_version 比对（全端下线后旧 refreshToken 拒绝）
    Long uid = parseUserId(userId);
    Integer currentVersion = getTokenVersion(uid);
    if (jwtVersion == null) {
        log.warn("refresh: jwt 中 ver 为空, userId={}, jti={}", userId, jti);
        throw new TokenException(ErrorCode.TOKEN_INVALID);
    }
    if (!jwtVersion.equals(currentVersion)) {
        throw new TokenException(ErrorCode.TOKEN_INVALID);
    }

    // 旧 refreshToken 一次性使用
    long remainingSeconds = Math.max(0,
            (expiration.getTime() - System.currentTimeMillis()) / 1000);
    redisTemplate.opsForValue().set(blacklistKey, "revoked", remainingSeconds, TimeUnit.SECONDS);
    redisTemplate.delete(refreshMappingKey);

    return issue(userId);
}
```

### 7. `revoke()` — 只加 JTI 黑名单

```java
@Override
public void revoke(String accessToken) {
    Claims claims = parseToken(accessToken);
    String jti = claims.getId();
    Date expiration = claims.getExpiration();

    long remainingSeconds = Math.max(0,
            (expiration.getTime() - System.currentTimeMillis()) / 1000);

    String blacklistKey = CacheConstants.Auth.BLACKLIST + jti;
    redisTemplate.opsForValue().set(blacklistKey, "revoked", remainingSeconds, TimeUnit.SECONDS);
}
```

### 8. `revokeAll()` — 改为 version++，加异常处理

**说明：** 调用 mall-user 递增 token_version，校验返回值，细化 Feign 异常处理。

```java
@Override
public void revokeAll(String userId) {
    Long uid = parseUserId(userId);
    try {
        remoteUserService.incrementTokenVersion(String.valueOf(uid));
        // 读取新 version 并刷新 Redis 缓存
        Integer newVersion = getTokenVersion(uid);
        if (newVersion != null) {
            updateVersionCache(uid, newVersion);
        } else {
            log.warn("revokeAll: 获取新 version 失败, userId={}", userId);
        }
    } catch (FeignException e) {
        log.error("revokeAll: Feign 调用失败, userId={}", userId, e);
    } catch (Exception e) {
        log.error("revokeAll: 未知异常, userId={}", userId, e);
    }
}
```

> 需要注入 `RemoteUserService`（mall-api 中的 Feign 接口），并添加 `@Slf4j`。

---

## D. ruoyi-gateway — MallAuthFilter（1 项）

**文件：** `server/ruoyi/ruoyi-gateway/src/main/java/com/ruoyi/gateway/filter/MallAuthFilter.java`

### 9. 删除 session key 检查，改为 version 比对

- 删除常量 `REDIS_SESSION_PREFIX`
- 删除 session key 检查（原 162-167 行）
- 用 `CacheConstants.Auth.USER_VERSION`（通过 Nacos 共享配置引入，或在本类定义同值常量）
- 新增 version 比对 + null 保护：

```java
// 6. token_version 比对
String userId = claims.get(USER_ID_KEY, String.class);
if (StringUtils.isEmpty(userId)) {
    log.warn("[C 端鉴权失败] 请求路径:{} 原因:token 中缺少 userId", path);
    return unauthorized(response, "C 端 token 缺少 userId");
}

// JTI 黑名单检查
String blacklistKey = REDIS_BLACKLIST_PREFIX + jti;
if (Boolean.TRUE.equals(redisService.hasKey(blacklistKey))) {
    log.warn("[C 端鉴权失败] 请求路径:{} jti:{} 原因:token 已被吊销", path, jti);
    return unauthorized(response, "C 端 token 已被吊销");
}

// token_version 校验（cache miss 直接放行，由下游 verify() 兜底回源 DB）
Integer jwtVersion = claims.get("ver", Integer.class);
String versionKey = USER_VERSION_PREFIX + userId;
Object cachedVersion = redisService.getCacheObject(versionKey);
if (jwtVersion != null && cachedVersion != null) {
    int currentVer = Integer.parseInt(cachedVersion.toString());
    if (jwtVersion != currentVer) {
        log.warn("[C 端鉴权失败] 请求路径:{} userId:{} jwtVer:{} curVer:{} 原因:token_version 不匹配",
                path, userId, jwtVersion, currentVer);
        return unauthorized(response, "C 端 token 已失效");
    }
}
// 若 cachedVersion 为 null（首次/缓存失效），不拦截，由下游 verify() 回源 DB
if (cachedVersion == null) {
    log.info("[C 端鉴权] 缓存未命中, userId={}, 放行下游兜底", userId);
}
```

---

## E. mall-api + mall-user（2 项）

### 10. Feign 接口（mall-api）

**文件：** `server/mall/mall-api/src/main/java/com/mall/api/feign/RemoteUserService.java`

新增方法：

```java
/**
 * 递增用户 token_version（改密码 / 全端下线时调用）
 *
 * @param userId 用户 ID
 */
@PutMapping("/inner/user/{userId}/token-version/increment")
void incrementTokenVersion(@PathVariable("userId") String userId);

/**
 * 获取用户 token_version
 *
 * @param userId 用户 ID
 * @return token_version 值（用户不存在返回 null）
 */
@GetMapping("/inner/user/{userId}/token-version")
Integer getTokenVersion(@PathVariable("userId") String userId);
```

### 11. 实现（mall-user）

**Controller：** `server/mall/mall-user/src/main/java/com/mall/user/controller/inner/RemoteUserInnerController.java`

```java
@PutMapping("/inner/user/{userId}/token-version/increment")
public void incrementTokenVersion(@PathVariable String userId) {
    int rows = userService.incrementTokenVersion(Long.parseLong(userId));
    if (rows == 0) {
        throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }
}

@GetMapping("/inner/user/{userId}/token-version")
public Integer getTokenVersion(@PathVariable String userId) {
    return userService.getTokenVersion(Long.parseLong(userId));
}
```

**Service：** `MallUserServiceImpl` 新增：

```java
@Override
public int incrementTokenVersion(Long userId) {
    return mallUserMapper.incrementTokenVersion(userId);
}

@Override
public Integer getTokenVersion(Long userId) {
    return mallUserMapper.selectTokenVersion(userId);
}
```

**Mapper：**

```java
@Update("UPDATE mall_user SET token_version = token_version + 1 WHERE id = #{userId}")
int incrementTokenVersion(@Param("userId") Long userId);

@Select("SELECT token_version FROM mall_user WHERE id = #{userId}")
Integer selectTokenVersion(@Param("userId") Long userId);
```

**DO：** `MallUserDO` 新增字段：

```java
/** token 版本号（递增），用于全端下线 */
private Integer tokenVersion;
```

---

## F. 设计文档（3 项）

### 12. `08_mall-auth详细设计.md`

- §3.2 TokenServiceImpl — 更新 `issue()`/`verify()`/`revoke()`/`revokeAll()` 流程：session key → token_version
- §4.1 Redis Key 规范 — 删除 `mall:auth:session:{userId}:{jti}`，新增 `mall:auth:user_version:{userId}`（TTL 30d）
- §4.2 Token 刷新流程 — 删除"删除旧 refresh session"描述
- §4.3 多端管理 — `SCAN` + 批量删除改为 token_version 递增机制说明

### 13. `03_07_系统详细设计-安全详细设计.md`

- 更新 JWT 校验流程图（网关 filter 和 verify 流程）
- 新增 Redis key 说明：`mall:auth:user_version:{userId}`

### 14. `06_mall-common公共模块设计.md`

- `CacheConstants.Auth`：注释从"11 个常量"更新，删除 `SESSION`，新增 `USER_VERSION`

---

## Redis Key 变更汇总

| Key | 变更 |
|-----|:---:|
| `mall:auth:session:{userId}:{jti}` | **删除** |
| `mall:auth:refresh:{jti}` | 保留 |
| `mall:auth:blacklist:{jti}` | 保留 |
| `mall:auth:user_version:{userId}` | **新增**（TTL 由 Nacos 配置，默认 30d，miss 时 DB 回源兜底） |

---

## 不改的

- `client.ts` 前端刷新逻辑 — 不变
- 登录/注册流程 — 不变
- `refreshToken TTL`（7d）/ `accessToken TTL`（30min）— 不变
- JWT 签名算法（HS512）— 不变
- `InnoDB` 行锁保证 `UPDATE SET token_version = token_version + 1` 原子性 — 不变
