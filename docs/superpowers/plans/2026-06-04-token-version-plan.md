# token_version 改造实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** accessToken 有效性从 "Redis session key" 改为 "JWT.ver == token_version"，删除 session key 机制

**Architecture:** 自底向上实施。先 mall-user 基础层（DO/Mapper/Service/Controller），再 mall-api Feign 契约，再 mall-common 常量，最后 mall-auth 核心逻辑和网关 filter

**Tech Stack:** Java 21, Spring Boot 4.0.3, MyBatis-Plus, JJWT, Redis, Feign, Nacos

---

## 任务依赖图

```
Task 1 (mall-user) ──→ Task 2 (mall-api) ──→ Task 5 (TokenServiceImpl)
                         ↓
Task 3 (CacheConstants) ─┘
Task 4 (ConfigProperties) ──→ Task 5
                                 ↓
                          Task 6 (MallAuthFilter)
                                 ↓
                          Task 7 (编译验证)
```

---

### Task 1: mall-user — DO / Mapper / Service / Controller

**模式:** TDD

**文件:**
- 修改: `server/mall/mall-user/src/main/java/com/mall/user/DO/MallUserDO.java`
- 修改: `server/mall/mall-user/src/main/java/com/mall/user/mapper/MallUserMapper.java`
- 修改: `server/mall/mall-user/src/main/java/com/mall/user/service/IMallUserService.java`
- 修改: `server/mall/mall-user/src/main/java/com/mall/user/service/impl/MallUserServiceImpl.java`
- 修改: `server/mall/mall-user/src/main/java/com/mall/user/controller/inner/RemoteUserInnerController.java`
- 新建测试: `server/mall/mall-user/src/test/java/com/mall/user/service/impl/MallUserServiceImplTest.java` (若不存在则建)

- [ ] **Step 1: MallUserDO 新增字段**

```java
/** token 版本号（递增），用于全端下线 */
@TableField("token_version")
private Integer tokenVersion;
```

> 同步手写 getter/setter（DO 不使用 Lombok @Data 的 getter/setter 自动生成，现有代码是手写的）。

- [ ] **Step 2: IMallUserService 新增接口方法**

```java
/**
 * 递增用户 token_version（原子操作，InnoDB 行锁保证）
 *
 * @param userId 用户 ID
 * @return 受影响行数（0 表示用户不存在）
 */
int incrementTokenVersion(Long userId);

/**
 * 查询用户 token_version
 *
 * @param userId 用户 ID
 * @return token_version 值，用户不存在返回 null
 */
Integer getTokenVersion(Long userId);
```

- [ ] **Step 3: MallUserMapper 新增方法**

```java
@Update("UPDATE mall_user SET token_version = token_version + 1 WHERE id = #{userId}")
int incrementTokenVersion(@Param("userId") Long userId);

@Select("SELECT token_version FROM mall_user WHERE id = #{userId}")
Integer selectTokenVersion(@Param("userId") Long userId);
```

- [ ] **Step 4: MallUserServiceImpl 新增实现**

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

- [ ] **Step 5: RemoteUserInnerController 新增端点**

在类末尾 `}` 前新增：

```java
/**
 * 递增用户 token_version（改密码 / 全端下线时调用）
 *
 * @param userId 用户 ID
 */
@PutMapping("/{userId}/token-version/increment")
public void incrementTokenVersion(@PathVariable String userId) {
    int rows = mallUserService.incrementTokenVersion(Long.parseLong(userId));
    if (rows == 0) {
        throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }
}

/**
 * 获取用户 token_version
 *
 * @param userId 用户 ID
 * @return token_version 值（用户不存在返回 null）
 */
@GetMapping("/{userId}/token-version")
public Integer getTokenVersion(@PathVariable String userId) {
    return mallUserService.getTokenVersion(Long.parseLong(userId));
}
```

> 需新增 import: `com.mall.common.enums.ErrorCode`, `com.mall.common.exception.BusinessException`

- [ ] **Step 6: 编译验证 mall-user**

```bash
mvn clean compile -f server/mall/pom.xml -pl mall-user -am -DskipTests
```

预期: BUILD SUCCESS

---

### Task 2: mall-api — RemoteUserService Feign 接口

**模式:** 无需 TDD（纯接口定义）

**文件:**
- 修改: `server/mall/mall-api/src/main/java/com/mall/api/feign/RemoteUserService.java`

- [ ] **Step 1: 新增 Feign 方法**

在 `expirePoints()` 方法之后、第一个内部类之前新增：

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

- [ ] **Step 2: 编译验证 mall-api**

```bash
mvn clean compile -f server/mall/pom.xml -pl mall-api -am -DskipTests
```

预期: BUILD SUCCESS

---

### Task 3: mall-common — CacheConstants.Auth

**模式:** 无需 TDD（纯常量）

**文件:**
- 修改: `server/mall/mall-common/src/main/java/com/mall/common/constant/CacheConstants.java`

- [ ] **Step 1: 删除 SESSION，新增 USER_VERSION**

删除 `SESSION` 常量（约第 38 行）：

```java
// 删除以下行
public static final String SESSION = "mall:auth:session:";
```

在 `Auth` 内部类的开头（在 `REFRESH` 之前）新增 `USER_VERSION`：

```java
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

- [ ] **Step 2: 编译验证 mall-common**

```bash
mvn clean compile -f server/mall/pom.xml -pl mall-common -am -DskipTests
```

预期: BUILD SUCCESS

---

### Task 4: mall-auth — MallAuthConfigProperties

**模式:** 无需 TDD（纯配置类）

**文件:**
- 修改: `server/mall/mall-auth/src/main/java/com/mall/auth/config/MallAuthConfigProperties.java`

- [ ] **Step 1: 新增 tokenVersionCacheTtl 字段**

在 `refreshTokenTtl` 字段之后新增：

```java
/** token_version Redis 缓存 TTL（秒，默认 2592000 = 30 天） */
private long tokenVersionCacheTtl = 2592000;
```

在 `setRefreshTokenTtl` 方法之后新增 getter/setter：

```java
/**
 * 获取 token_version 缓存 TTL
 *
 * @return TTL（秒）
 */
public long getTokenVersionCacheTtl() {
    return tokenVersionCacheTtl;
}

/**
 * 设置 token_version 缓存 TTL
 *
 * @param tokenVersionCacheTtl TTL（秒）
 */
public void setTokenVersionCacheTtl(long tokenVersionCacheTtl) {
    this.tokenVersionCacheTtl = tokenVersionCacheTtl;
}
```

- [ ] **Step 2: 编译验证 mall-auth**

```bash
mvn clean compile -f server/mall/pom.xml -pl mall-auth -am -DskipTests
```

预期: BUILD SUCCESS (若因 ITokenService.verify 签名变更导致编译错误，属于 Task 5 的范畴，此处可忽略)

---

### Task 5: mall-auth — TokenServiceImpl（核心改造）

**模式:** TDD

**文件:**
- 修改: `server/mall/mall-auth/src/main/java/com/mall/auth/service/impl/TokenServiceImpl.java`
- 新建测试: `server/mall/mall-auth/src/test/java/com/mall/auth/service/impl/TokenServiceImplTest.java`

**改动范围:**
1. 新增依赖 `RemoteUserService`
2. 新增 `@Slf4j` 注解
3. 新增私有方法: `getTokenVersion()`, `updateVersionCache()`, `parseUserId()`
4. 修改 `issue()`: 新增 ver claim，删除 session key 写入
5. 修改 `verify()`: session 检查 → version 比对
6. 修改 `refresh()`: 新增 version 比对
7. 修改 `revoke()`: 删除 session key 删除
8. 修改 `revokeAll()`: Redis SCAN → Feign incrementTokenVersion

- [ ] **Step 1: 删除 `issue()` 中 session key 写入（第 84-86 行）**

删除或注释：
```java
// accessToken 会话缓存，用于快速校验 token 有效性
String sessionKey = CacheConstants.Auth.SESSION + userId + ":" + accessJti;
redisTemplate.opsForValue().set(sessionKey, "1", authProperties.getAccessTokenTtl(), TimeUnit.SECONDS);
```

- [ ] **Step 2: 修改 `verify()` — session 检查 → version 比对**

将第 114-118 行的 session 检查替换为 version 比对：

```java
// 检查会话缓存：无 session 说明 token 未签发或已被删除
String sessionKey = CacheConstants.Auth.SESSION + userId + ":" + jti;
if (Boolean.FALSE.equals(redisTemplate.hasKey(sessionKey))) {
    throw new TokenException(ErrorCode.TOKEN_INVALID);
}
```

替换为：

```java
// token_version 比对（cache miss 回源 DB）
Integer jwtVersion = claims.get("ver", Integer.class);
if (jwtVersion == null) {
    log.warn("verify: jwt 中 ver 为空, userId={}, jti={}", userId, jti);
    throw new TokenException(ErrorCode.TOKEN_INVALID);
}
Integer currentVersion = getTokenVersion(parseUserId(userId));
if (!jwtVersion.equals(currentVersion)) {
    throw new TokenException(ErrorCode.TOKEN_INVALID);
}
```

> 需新增 import: `com.mall.api.feign.RemoteUserService`, `lombok.extern.slf4j.Slf4j`, `feign.FeignException`

- [ ] **Step 3: 修改 `revoke()` — 删除 session 删除**

将第 186-187 行的 session 删除移除：

```java
// 删除以下两行
String sessionKey = CacheConstants.Auth.SESSION + userId + ":" + jti;
redisTemplate.delete(sessionKey);
```

- [ ] **Step 4: 修改 `revokeAll()` — Redis SCAN → Feign incrementTokenVersion**

将整个 `revokeAll` 方法替换为：

```java
@Override
public void revokeAll(String userId) {
    Long uid;
    try {
        uid = Long.parseLong(userId);
    } catch (NumberFormatException e) {
        log.error("revokeAll: userId 转换失败, userId={}", userId, e);
        return;
    }
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

- [ ] **Step 5: 新增依赖、注解和私有方法**

将类注解从 `@RequiredArgsConstructor` 改为 `@RequiredArgsConstructor` + 新增 `@Slf4j`：

```java
@Slf4j
@Service
@RequiredArgsConstructor
```

新增字段 `RemoteUserService`：

```java
private final RemoteUserService remoteUserService;
```

新增私有方法（放在 `parseToken` 方法之前）：

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
    // 缓存 miss → 查 DB 并写回缓存
    version = remoteUserService.getTokenVersion(String.valueOf(userId));
    if (version != null) {
        redisTemplate.opsForValue().set(key, version, authProperties.getTokenVersionCacheTtl(), TimeUnit.SECONDS);
    } else {
        log.warn("getTokenVersion: DB 未查询到用户版本, userId={}", userId);
    }
    return version;
}

/**
 * 更新 Redis 缓存中的 token_version
 */
private void updateVersionCache(Long userId, Integer version) {
    String key = CacheConstants.Auth.USER_VERSION + userId;
    redisTemplate.opsForValue().set(key, version, authProperties.getTokenVersionCacheTtl(), TimeUnit.SECONDS);
}
```

- [ ] **Step 6: 修改 `issue()` — JWT 新增 ver claim**

在 accessToken 构建的 `.claim("userId", userId)` 之后新增：

```java
.claim("ver", version)       // 新增
```

在 refreshToken 构建同样新增。

在方法开头获取 version（替换原来的 session key 逻辑）：

```java
// 获取当前 token_version
Long uid = parseUserId(userId);
Integer version = getTokenVersion(uid);
if (version == null) {
    throw new TokenException(ErrorCode.TOKEN_INVALID);
}
```

> `parseUserId` 方法定义见 step 7。

- [ ] **Step 7: 修改 `refresh()` — 新增 version 比对**

在 refresh mapping 检查之后、黑名单写入之前插入 version 比对：

```java
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
```

同时在解析 claims 时新增获取 `jwtVersion`：

```java
Integer jwtVersion = claims.get("ver", Integer.class);
```

> 在现有的 `String jwtVersion = claims.get("ver", String.class)` 位置改为上述类型。

- [ ] **Step 8: 新增 `parseUserId()` 私有方法**

```java
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

- [ ] **Step 9: 更新类 Javadoc**

将类级 Javadoc 中的 "通过 Redis 维护会话（session Key）" 改为 "通过 token_version + JTI 黑名单维护令牌有效性"。

- [ ] **Step 10: 编译验证 mall-auth**

```bash
mvn clean compile -f server/mall/pom.xml -pl mall-auth -am -DskipTests
```

预期: BUILD SUCCESS

---

### Task 6: ruoyi-gateway — MallAuthFilter

**模式:** 无需 TDD（网关层不走 TDD）

**文件:**
- 修改: `server/ruoyi/ruoyi-gateway/src/main/java/com/ruoyi/gateway/filter/MallAuthFilter.java`

- [ ] **Step 1: 删除 session key 常量**

删除第 65 行：

```java
// 删除以下行
private static final String REDIS_SESSION_PREFIX = "mall:auth:session:";
```

新增 `USER_VERSION_PREFIX` 常量（与 `REDIS_BLACKLIST_PREFIX` 同级）：

```java
/** C 端 token_version Redis Key 前缀 — 与 mall-common CacheConstants.Auth.USER_VERSION 一致 */
private static final String USER_VERSION_PREFIX = "mall:auth:user_version:";
```

- [ ] **Step 2: 删除 session key 检查，改为 version 比对**

删除第 162-167 行：

```java
// 删除以下代码块
String sessionKey = REDIS_SESSION_PREFIX + userId + ":" + jti;
if (Boolean.FALSE.equals(redisService.hasKey(sessionKey))) {
    log.warn("[C 端鉴权失败] 请求路径:{} userId:{} 原因:会话不存在或已被删除", path, userId);
    return unauthorized(response, "C 端 token 会话不存在");
}
```

替换为：

```java
// token_version 校验（cache miss 直接放行，由下游 verify() 兜底回源 DB）
Integer jwtVersion = claims.get("ver", Integer.class);
String versionKey = USER_VERSION_PREFIX + userId;
Object cachedVersion = redisService.getCacheObject(versionKey);
if (jwtVersion != null && cachedVersion != null) {
    try {
        int currentVer = Integer.parseInt(cachedVersion.toString());
        if (jwtVersion != currentVer) {
            log.warn("[C 端鉴权失败] 请求路径:{} userId:{} jwtVer:{} curVer:{} 原因:token_version 不匹配",
                    path, userId, jwtVersion, currentVer);
            return unauthorized(response, "C 端 token 已失效");
        }
    } catch (NumberFormatException e) {
        log.error("[C 端鉴权] 缓存 version 转换失败, userId={}, cachedVersion={}", userId, cachedVersion, e);
    }
}
if (cachedVersion == null) {
    log.info("[C 端鉴权] 缓存未命中, userId={}, 放行下游兜底", userId);
}
```

- [ ] **Step 3: 编译验证 ruoyi-gateway**

```bash
mvn clean compile -f server/ruoyi/pom.xml -pl ruoyi-gateway -am -DskipTests
```

预期: BUILD SUCCESS

---

### Task 7: 全量编译验证

- [ ] **Step 1: 编译 server/mall 所有模块**

```bash
mvn clean install -f server/mall/pom.xml -DskipTests
```

预期: BUILD SUCCESS

- [ ] **Step 2: 编译 server/ruoyi 所有模块**

```bash
mvn clean install -f server/ruoyi/pom.xml -DskipTests
```

预期: BUILD SUCCESS

---

## 验证清单

完成后手动验证：

1. 启动 mall-auth + mall-user + gateway
2. 调用登录接口 → 获取 accessToken → 解码 JWT 确认包含 `ver` claim
3. 用 accessToken 调需鉴权接口 → 200 OK
4. 调 revokeAll → Redis `mall:auth:user_version:{userId}` 值递增
5. 用旧 accessToken 调接口 → 401
6. 用旧 refreshToken 刷新 → 401
