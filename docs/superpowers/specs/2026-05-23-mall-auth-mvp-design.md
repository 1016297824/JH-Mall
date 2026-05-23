# mall-auth MVP 完整设计

> 基于 `07_mall-auth详细设计.md` 和 `03_系统详细设计.md`，限定 MVP 阶段实现范围。

---

## 1 MVP 范围

| 组件                                      |  状态  | 说明                              |
| ----------------------------------------- | :-----: | --------------------------------- |
| `CaptchaController`                     | ✅ 完整 | 6 个 CAPTCHA 端点                 |
| `CaptchaService`                        | ✅ 完整 | EasyCaptcha 生成/Redis 校验       |
| `TokenService`                          | ✅ 完整 | JWT 签发/校验/刷新/撤销           |
| `AuthController`                        | 🔷 骨架 | 13 个端点占位，返回"功能暂未开放" |
| `AuthService`                           | 🔷 接口 | 仅接口，列出全部 7 个方法签名     |
| `DecryptService`                        | 🔷 接口 | 仅接口，无实现                    |
| `SmsService`                            | 🔷 接口 | 仅接口，无实现                    |
| `RemoteUserService` (mall-api)          | ✅ 完整 | Feign 契约                        |
| `MallUserDTO` (mall-api)                | ✅ 完整 | 用户数据传输对象                  |
| `MallResult<T>` (mall-api)              | ✅ 完整 | C 端统一响应体                    |
| `RemoteUserInnerController` (mall-user) | ✅ 完整 | Feign 调用入口                    |
| mall-user Mapper/Service 扩展             | ✅ 完整 | C 端查询/注册/更新方法            |
| `@EnableFeignClients` 修复              | ✅ 完整 | 两模块加 `"com.mall.api"`       |
| 微信相关                                  |   ❌   | 全部不做                          |
| `AuthServiceImpl`                       |   ❌   | 不建                              |

---

## 2 架构边界

```
┌──────────────┐     Feign      ┌──────────────┐
│   mall-auth   │◄─────────────►│   mall-user   │
│   (9301)      │ RemoteUserSvc │   (9302)      │
│               │               │               │
│  CaptchaSvc   │               │  UserMapper   │
│  TokenService │               │  RemoteUser-  │
│  AuthCtrl(13) │               │  InnerCtrl    │
│  CaptchaCtrl  │               │               │
└──────┬───────┘                └──────────────┘
       │
       ├── Redis: captcha/token/session
       ├── EasyCaptcha (图片生成)
       └── BCrypt (密码哈希)
```

**关键边界：**

- mall-auth 不持有 MySQL 表，用户数据全部 Feign 调 mall-user
- JWT 密钥 `mall.security.jwt-secret` 独立于若依管理端 `TokenConstants.SECRET`
- userId 类型为 `String`（雪花 ID）
- C 端响应体用 `MallResult<T>`（`com.mall.api.dto`），**不**使用若依 `AjaxResult`
- **调 Feign 方式：** CaptchaController 直接 `@Autowired RemoteUserService`（`mall-api` 的 Feign 接口），`RemoteUserAdapter` 保留骨架但 MVP 不被调用，供后续 `AuthServiceImpl` 使用
- **参数校验：** 所有 Controller 统一使用 `@Validated`/`@Valid` 触发校验，DTO 字段加 `jakarta.validation.constraints` 注解，失败由 `MallExceptionHandler` 处理 → `MallResult.error("A0401", ...)`

---

## 3 包结构

### 3.1 mall-api 新增

```
server/mall/mall-api/
└── src/main/java/com/mall/api/
    ├── dto/
    │   ├── MallResult.java              # C 端统一响应体
    │   └── MallUserDTO.java             # 用户数据传输对象
    └── feign/
        ├── RemoteUserService.java       # Feign 契约（含内部请求类）
        └── RemoteAuthService.java       # Feign 契约（占位，后续实现解密）
```

### 3.2 mall-auth 结构

```
server/mall/mall-auth/
└── src/main/java/com/mall/auth/
    ├── MallAuthApplication.java         # @EnableFeignClients({"com.ruoyi","com.mall.api"})
    ├── controller/
    │   ├── AuthController.java          # 13 端点占位，返回"功能暂未开放"
    │   └── CaptchaController.java       # 6 CAPTCHA 端点，完整实现
    ├── dto/
    │   ├── request/
    │   │   ├── CaptchaRegisterReq.java
    │   │   ├── CaptchaLoginReq.java
    │   │   ├── CaptchaResetPasswordReq.java
    │   │   ├── CaptchaChangePhoneReq.java
    │   │   └── CaptchaDeactivateReq.java
    │   └── response/
    │       ├── CaptchaResponse.java
    │       └── TokenResponse.java
    ├── service/
    │   ├── AuthService.java             # 接口，7 方法签名
    │   ├── TokenService.java            # 接口
    │   ├── impl/TokenServiceImpl.java   # 完整实现
    │   ├── CaptchaService.java          # 接口
    │   ├── impl/CaptchaServiceImpl.java # 完整实现
    │   ├── DecryptService.java          # 仅接口
    │   └── SmsService.java              # 仅接口
    └── infrastructure/
        ├── feign/
        │   └── RemoteUserAdapter.java   # Feign 调用封装
        └── exception/
            └── CaptchaException.java    # 验证码异常
```

### 3.3 mall-user 修改

```
server/mall/mall-user/
└── src/main/java/com/mall/user/
    ├── MallUserApplication.java          # @EnableFeignClients({"com.ruoyi","com.mall.api"})
    ├── controller/
    │   └── RemoteUserInnerController.java # FEIGN 调用入口，路径 /inner/user/*
    ├── mapper/
    │   └── MallUserMapper.java           # 追加 selectByPhoneHash / updatePassword 等
    └── service/
        ├── IMallUserService.java         # 追加 C 端方法接口
        └── impl/MallUserServiceImpl.java # 追加 C 端方法实现
```

---

## 4 TokenService 详细设计

### 4.1 接口

```java
public interface TokenService {
    TokenResponse issue(String userId);
    String verify(String accessToken);       // 返回 userId
    TokenResponse refresh(String refreshToken);
    void revoke(String accessToken);
    void revokeAll(String userId);
}
```

### 4.2 JWT Payload

| 字段       | 类型   | 说明                          |
| ---------- | ------ | ----------------------------- |
| `userId` | String | 用户 ID                       |
| `jti`    | UUID   | 唯一 token 标识               |
| `iat`    | long   | 签发时间戳                    |
| `exp`    | long   | 过期时间戳                    |
| `type`   | String | `"access"` 或 `"refresh"` |

### 4.3 Token 策略

| 参数             |      值      | 说明                                               |
| ---------------- | :-----------: | -------------------------------------------------- |
| accessToken TTL  |     30min     | 短期安全令牌                                       |
| refreshToken TTL |      7d      | 长期刷新令牌，一次性轮换                           |
| 多端共存         |      ✅      | 每端独立 jti                                       |
| 密钥             |     HS512     | 64 字节 Base64，Nacos `mall.security.jwt-secret` |
| issuer           | `mall-auth` | 与管理端 ruoyi-auth 不同                           |

### 4.4 Redis Key

| Key                                  |        TTL        | 用途              |
| ------------------------------------ | :----------------: | ----------------- |
| `mall:auth:session:{userId}:{jti}` | 同 accessToken exp | 会话信息          |
| `mall:auth:refresh:{jti}`          |         7d         | refreshToken 映射 |
| `mall:auth:blacklist:{jti}`        | 原 token 剩余时间 | 注销/刷新作废     |

### 4.5 流程

**issue(userId)：**

1. 生成 accessToken JWT `{userId, jti=UUID, iat, exp=+30min, type="access"}`
2. 生成 refreshToken JWT `{userId, jti=UUID, iat, exp=+7d, type="refresh"}`
3. Redis SETEX session，TTL 同 accessToken
4. Redis SETEX refresh 映射，TTL 7d
5. 返回 TokenResponse

**verify(accessToken)：**

1. JWT 签名校验 + 过期校验 → 不通过抛异常
2. Redis 黑名单检查 → 存在抛 A0231
3. Redis session 存在性检查 → 不存在抛 A0231
4. 返回 userId

**refresh(refreshToken)：**

1. 校验 refreshToken（签名+过期+黑名单+session）
2. 旧 refreshToken jti 加入黑名单，删除旧 session
3. 旧 accessToken jti 加入宽限黑名单（TTL 最小化）
4. 签发新 token 对

**revoke(accessToken)：** jti 加入黑名单，删除 session

**revokeAll(userId)：** 遍历删除该用户全部 session key

---

## 5 CaptchaService 详细设计

### 5.1 接口

```java
public interface CaptchaService {
    Map<String, String> generate();
    void verify(String captchaKey, String captchaCode, String clientIp);
}
```

### 5.2 generate()

1. EasyCaptcha `SpecCaptcha(130, 48, 4)` 生成 4 位字符
2. 手动排除 `0/O/1/I`（替换为随机字母数字）
3. JPEG 质量 0.7，转 Base64 (`data:image/jpeg;base64,...`)
4. UUID 生成 captchaKey
5. Redis SETEX `mall:auth:captcha:{captchaKey}` 300s `{text_lowercase}`
6. 返回 `{ captchaKey, captchaImage }`

### 5.3 verify(captchaKey, code, clientIp)

| 步骤 | 逻辑                                              | 错误码 |
| :--: | ------------------------------------------------- | :----: |
|  ①  | 参数空检查                                        | A0401 |
|  ②  | IP 防刷：`mall:auth:captcha:ip:{ip}` 日超 10 次 | A0241 |
|  ③  | Redis GET key → null                             | A0132 |
|  ④  | 比对（忽略大小写）→ 不匹配，IP 失败计数+1        | A0131 |
|  ⑤  | 匹配 → DELETE key（一次性消费）                  |   —   |

### 5.4 Redis Key

| Key                                |  TTL  | 用途               |
| ---------------------------------- | :----: | ------------------ |
| `mall:auth:captcha:{captchaKey}` |  300s  | 验证码文本（小写） |
| `mall:auth:captcha:ip:{ip}`      | 86400s | IP 验证失败计数器  |

---

## 6 CAPTCHA 端点详细流程

所有端点在 `CaptchaController`，`@RequestMapping("/api/auth/captcha")`。

### 6.1 GET /api/auth/captcha

```
① CaptchaService.generate()
② 返回 { captchaKey, captchaImage }
```

> `clientIp` 从请求头 `X-Forwarded-For` 获取，无代理时取 `request.getRemoteAddr()`。后续公共 `RequestContext` 接入后统一。MVP 阶段如果 IP 获取不到兜底用 `"unknown"`。

### 6.2 POST /api/auth/captcha/register

```java
CaptchaRegisterReq { phone, password, captchaKey, captchaCode, isPrivacyAgreed }
```

```
① CaptchaService.verify(key, code, ip)
② isPrivacyAgreed != 1 → A0101
③ 密码 8~32 位 + 字母+数字 → 否则 A0121
④ RemoteUserService.findByPhone(phone) → 已存在 A0151
⑤ BCrypt 哈希密码
⑥ SHA256 phoneHash
⑦ RemoteUserService.register(RegisterRequest) → userId
⑧ TokenService.issue(userId) → 返回 TokenResponse
```

### 6.3 POST /api/auth/captcha/login

```java
CaptchaLoginReq { phone, password, captchaKey, captchaCode }
```

```
① CaptchaService.verify(key, code, ip)
② RemoteUserService.findByPhone(phone) → null → A0201
③ userStatus=1(冻结) → A0202, =2(注销) → A0203
④ Redis mall:auth:pwd_err:{userId} 超 5 次 → A0211
⑤ BCrypt 校验 → 不匹配，计数+1 → A0210
⑥ 登录成功 → 清除错误计数
⑦ TokenService.issue(userId) → 返回 TokenResponse
```

### 6.4 POST /api/auth/captcha/password/reset

```java
CaptchaResetPasswordReq { phone, newPassword, captchaKey, captchaCode }
```

```
① CaptchaService.verify(key, code, ip)
② RemoteUserService.findByPhone(phone) → null → A0201
③ 密码校验 8~32 位 + 字母+数字 → 否则 A0121
④ BCrypt 哈希新密码
⑤ RemoteUserService.updatePassword(userId, request)
⑥ TokenService.revokeAll(userId)
⑦ 返回成功
```

### 6.5 PUT /api/auth/captcha/phone

```java
CaptchaChangePhoneReq { oldPhone, password, newPhone }
```

```
① RemoteUserService.findByPhone(oldPhone) → null → A0201
② BCrypt 校验 password → 不匹配 A0210
③ RemoteUserService.findByPhone(newPhone) → 已存在 A0151
④ SHA256 newPhoneHash
⑤ RemoteUserService.updatePhone(userId, request)
⑥ 返回成功
```

### 6.6 DELETE /api/auth/captcha/account

```java
CaptchaDeactivateReq { phone, password }
```

```
① RemoteUserService.findByPhone(phone) → null → A0201
② BCrypt 校验 password → 不匹配 A0210
③ RemoteUserService.deactivateAccount(userId)
④ TokenService.revokeAll(userId)
⑤ 返回成功
```

---

## 7 AuthController 骨架

13 个端点全部返回：

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    // 全部 13 个端点，统一返回:
    // MallResult.error("A9999", "该功能暂未开放，请使用 CAPTCHA 端点")
}
```

保留路径占位：

| 路径                               |     方法     |
| ---------------------------------- | :----------: |
| `/api/auth/users`                |     POST     |
| `/api/auth/sms_codes`            |     POST     |
| `/api/auth/sessions`             |     POST     |
| `/api/auth/sessions/sms`         |     POST     |
| `/api/auth/sessions/refresh`     |     POST     |
| `/api/auth/sessions/current`     | DELETE / GET |
| `/api/auth/wechat/sessions`      |     POST     |
| `/api/auth/wechat/phone_binding` |     POST     |
| `/api/auth/phone`                |     PUT     |
| `/api/auth/password/reset`       |     PUT     |
| `/api/auth/password`             |     PUT     |
| `/api/auth/account`              |    DELETE    |

---

## 8 mall-api 契约

### 8.1 MallUserDTO

| 字段         | 类型   | 说明                 |
| ------------ | ------ | -------------------- |
| id           | String | 用户 ID              |
| phone        | String | 手机号               |
| phoneHash    | String | SHA256 哈希          |
| password     | String | BCrypt 哈希          |
| nickname     | String | 昵称                 |
| userStatus   | String | 0=正常 1=冻结 2=注销 |
| registerType | String | phone/wechat         |

### 8.2 RemoteUserService Feign

```java
@FeignClient(contextId = "remoteUserService", value = "mall-user")
public interface RemoteUserService {

    @GetMapping("/inner/user/phone/{phone}")
    MallUserDTO findByPhone(@PathVariable("phone") String phone);

    @PostMapping("/inner/user/register")
    String register(@RequestBody RegisterRequest request);

    @PutMapping("/inner/user/{userId}/password")
    void updatePassword(@PathVariable("userId") String userId,
                        @RequestBody PasswordUpdateRequest request);

    @PutMapping("/inner/user/{userId}/phone")
    void updatePhone(@PathVariable("userId") String userId,
                     @RequestBody PhoneUpdateRequest request);

    @DeleteMapping("/inner/user/{userId}/account")
    void deactivateAccount(@PathVariable("userId") String userId);

    // 内部请求类
    static class RegisterRequest { ... }
    static class PasswordUpdateRequest { ... }
    static class PhoneUpdateRequest { ... }
}
```

### 8.3 MallResult`<T>`

| 字段         | 类型   | 说明                       |
| ------------ | ------ | -------------------------- |
| errorCode    | String | 错误码                     |
| errorMessage | String | 错误信息                   |
| userTip      | String | 用户提示（成功时空字符串） |
| data         | T      | 数据（错误时为 null）      |
| requestId    | String | 请求 ID                    |

静态工厂方法：`success(data)`、`error(code, message)`、`error(code, message, userTip)`

---

## 9 mall-user 扩展

### 9.1 Mapper 追加

```java
MallUser selectByPhoneHash(@Param("phoneHash") String phoneHash);
int updatePassword(@Param("id") String id, @Param("password") String password);
int updatePhone(@Param("id") String id, @Param("phone") String phone, @Param("phoneHash") String phoneHash);
int updateUserStatus(@Param("id") String id, @Param("userStatus") String userStatus);
```

### 9.2 Service 追加

```java
MallUser selectByPhone(String phone);                    // 先 SHA256 再查 phoneHash
MallUser selectByWechatOpenId(String openId);
String registerByPhone(String phone, String phoneHash, String passwordHash);
int updatePasswordById(String id, String newPasswordHash);
int updatePhoneById(String id, String newPhone, String newPhoneHash);
int updateUserStatusById(String id, String userStatus);
```

### 9.3 RemoteUserInnerController

```
路径前缀: /inner/user  (Feign 直连，不走 gateway)

GET    /inner/user/phone/{phone}
POST   /inner/user/register
PUT    /inner/user/{userId}/password
PUT    /inner/user/{userId}/phone
DELETE /inner/user/{userId}/account
```

---

## 10 依赖变更

### 10.1 mall-auth/pom.xml 新增

```xml
<!-- EasyCaptcha 图片验证码 -->
<dependency>
    <groupId>com.github.whvcse</groupId>
    <artifactId>easy-captcha</artifactId>
    <version>1.6.2</version>
</dependency>

<!-- Spring Security Crypto（BCrypt） -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```

### 10.2 @EnableFeignClients 修复

mall-auth 和 mall-user 的 Application.java 均改为：

```java
@EnableFeignClients(basePackages = {"com.ruoyi", "com.mall.api"})
```

---

## 11 错误码

| 错误码 | HTTP | userTip                            | 触发条件                            |
| ------ | :--: | ---------------------------------- | ----------------------------------- |
| 00000  | 200 | —                                 | 成功                                |
| A0101  | 400 | 请同意隐私协议                     | isPrivacyAgreed != 1                |
| A0121  | 400 | 密码需 8~32 位且包含字母和数字     | 密码复杂度不足                      |
| A0131  | 400 | 验证码错误                         | 图片验证码不匹配                    |
| A0132  | 400 | 验证码已过期                       | 验证码超 5 分钟或已使用             |
| A0151  | 400 | 手机号已被注册                     | 注册/换绑时手机号已存在             |
| A0201  | 400 | 账户不存在                         | 登录/改密时手机号未注册             |
| A0202  | 400 | 账户已被冻结                       | 用户状态为冻结                      |
| A0203  | 400 | 账户已注销                         | 用户状态为已注销                    |
| A0210  | 400 | 密码错误                           | 密码不匹配                          |
| A0211  | 400 | 密码错误次数过多，请 30 分钟后重试 | 连续 5 次密码错误锁定               |
| A0241  | 400 | 验证码尝试次数过多                 | IP 日验证失败超 10 次               |
| A0401  | 400 | 请完整填写信息                     | 必填参数为空                        |
| A0410  | 429 | 请求过于频繁，请稍后再试           | 频率限制                            |
| A9999  | 200 | —                                 | 功能暂未开放（AuthController 占位） |
| B0001  | 500 | 系统繁忙，请稍后再试               | 未预期异常                          |

---

## 12 设计文档关联

已更新的文档：

| 文档                         | 更新内容                                   |
| ---------------------------- | ------------------------------------------ |
| `07_mall-auth详细设计.md`  | §13 MVP CAPTCHA 图片验证码                |
| `03_系统详细设计.md`       | §3.1.14~3.1.19 CAPTCHA 端点小节（待更新） |
| `05_mall-api契约层设计.md` | Feign 接口签名 + MallResult 定义（待更新） |

---

## 13 不做的内容（明确排除）

| 功能                             | 原因                                      |
| -------------------------------- | ----------------------------------------- |
| 真实短信通道                     | 需要第三方 SMS SDK，MVP 无                |
| 微信登录/绑定                    | 需要微信开放平台，MVP 无                  |
| AuthServiceImpl                  | MVP 无编排需求                            |
| SmsServiceImpl                   | 仅有接口，无实现                          |
| DecryptServiceImpl               | 仅有接口，无实现                          |
| 审计日志 mall_audit_log          | MVP 不在 auth 范围（在 mall-user 侧处理） |
| 原始 13 端点实现                 | MVP 前端只调 CAPTCHA 端点                 |
| Domain DO 后缀                   | 全 C 端统一无 DO 后缀                     |
| MQ 事件 `mall:user:registered` | MVP 不涉及                                |
