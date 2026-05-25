# JH-Store mall-auth 模块详细设计

> 基于系统详细设计 `03_系统详细设计.md` 展开。mall-auth 不持有 MySQL 表，用户数据通过 Feign 调 mall-user 操作。

---

## 1 模块概述

### 1.1 子领域

| 子领域     | 存储  | 说明                                                     |
| ---------- | ----- | -------------------------------------------------------- |
| 用户注册   | —    | 手机号+验证码注册，调 mall-user 落库                     |
| 会话管理   | Redis | JWT access_token（30min）+ refresh_token（7d），多端共存 |
| 密码认证   | —    | 手机号+密码登录，密码哈希校验（调 mall-user 取凭证）     |
| 短信认证   | Redis | 短信验证码登录/快捷注册，code 5min TTL                   |
| 微信认证   | —    | 小程序 code→token，微信用户创建/关联                    |
| token 刷新 | Redis | refresh_token 一次性轮换，旧 token 1h 宽限期             |
| 账号安全   | Redis | 密码错误计数、账户冻结/注销、审计日志                    |
| 解密服务   | —    | AES-256-GCM 密钥持有方，其他服务 Feign 调解密            |

### 1.2 依赖关系

```
mall-auth (9301端口)
  ├── MySQL: 无自有表（用户数据通过 Feign 调用 mall-user 操作）
  ├── Redis: token/session/验证码/错误计数/黑名单
  ├── mall-common (Maven): 全局异常处理器等公共基础设施
  ├── mall-api (Feign Client): RemoteUserService → mall-user
  ├── mall-user (Feign Caller): 用户注册/查询/密码更新/注销
  ├── 短信平台: 发送验证码（第三方 SDK）
  ├── 微信开放平台: code2session / 手机号解密（第三方 SDK）
  └── 其他服务 → mall-auth (Feign): 解密手机号（RemoteAuthService.decrypt）
```

> **关键边界**：mall-auth 是认证中心，不持有用户数据表。用户 CRUD 全部通过 Feign 委托 mall-user。mall-auth 持有 JWT secret 和 AES 密钥，是唯一密钥持有方。

**MVP 范围说明：** 以下结构为完整设计。MVP 阶段仅实现 CAPTCHA 子系统（§13）+ TokenService + Feign 契约。AuthController 13 个端点返回占位"功能暂未开放"，AuthService 仅建接口，SmsService/DecryptService 仅建接口，WechatAdapter 不建。详见 `docs/superpowers/specs/2026-05-23-mall-auth-mvp-design.md`。

---

## 2 包结构与接口映射

### 2.1 包结构

```
server/mall/mall-auth/
└── src/main/java/com/mall/auth/
    ├── MallAuthApplication.java             # Spring Boot 启动类
    ├── controller/
    │   ├── AuthController.java              # 主 Controller，13 个认证接口
    │   └── CaptchaController.java           # MVP CAPTCHA 6 端点（独立实现）
    ├── dto/
    │   ├── request/                         → RegisterReq, LoginReq, SmsLoginReq, SmsCodeReq,
    │   │                                       RefreshTokenReq, WechatLoginReq, PhoneBindReq,
    │   │                                       ChangePhoneReq, ResetPasswordReq, ChangePasswordReq,
    │   │                                       DeactivateReq,
    │   │                                       CaptchaRegisterReq, CaptchaLoginReq,
    │   │                                       CaptchaResetPasswordReq, CaptchaChangePhoneReq,
    │   │                                       CaptchaDeactivateReq
    │   └── response/                        → TokenResp, SmsCodeResp, SessionInfoResp,
    │                                           CaptchaResponse
    │                                           （MallResult<T> 在 mall-api/com.mall.api.dto，非本模块）
    ├── service/
    │   ├── AuthService.java                 # 接口
    │   ├── impl/
    │   │   └── AuthServiceImpl.java         # 认证核心业务（注册/登录/刷新/注销/改密）
    │   ├── TokenService.java                # 接口
    │   ├── impl/
    │   │   └── TokenServiceImpl.java        # JWT 签发/校验/黑名单
    │   ├── SmsService.java                  # 接口
    │   ├── impl/
    │   │   └── SmsServiceImpl.java          # 短信验证码发送/校验/防刷
    │   ├── DecryptService.java              # 接口
    │   ├── impl/
    │   │   └── DecryptServiceImpl.java      # AES-256-GCM 解密（Feign 暴露给其他服务）
    │   ├── CaptchaService.java              # 接口（MVP）
    │   └── impl/
    │       └── CaptchaServiceImpl.java      # CAPTCHA 生成/校验
    └── infrastructure/
        ├── feign/
        │   └── RemoteUserAdapter.java       # 调 mall-user 用户 CRUD
        ├── sms/
        │   └── SmsProviderAdapter.java      # 短信平台适配
        └── wechat/
            └── WechatAdapter.java           # 微信 code2session + 解密
```

> 与 mall-order/mall-payment 不同：mall-auth **无 domain/、无 mapper/、无 statemachine/** 包。用户数据通过 `infrastructure/feign/RemoteUserAdapter` 调 mall-user。所有 userId 类型为 `String`（雪花 ID）。

### 2.2 接口 → Controller 映射

| #  | 方法   | 路径                               | 方法名                     | 需登录 | 审计 |
| -- | ------ | ---------------------------------- | -------------------------- | :----: | :--: |
| 1  | POST   | `/api/auth/users`                | `register(req)`          |   否   |  —  |
| 2  | POST   | `/api/auth/sms_codes`            | `sendSmsCode(req)`       |   否   |  —  |
| 3  | POST   | `/api/auth/sessions`             | `loginByPassword(req)`   |   否   |  ✓  |
| 4  | POST   | `/api/auth/sessions/sms`         | `loginBySms(req)`        |   否   |  ✓  |
| 5  | POST   | `/api/auth/sessions/refresh`     | `refreshToken(req)`      |   否   |  —  |
| 6  | DELETE | `/api/auth/sessions/current`     | `logout()`               |   是   |  ✓  |
| 7  | POST   | `/api/auth/wechat/sessions`      | `loginByWechat(req)`     |   否   |  ✓  |
| 8  | POST   | `/api/auth/wechat/phone_binding` | `bindPhone(req)`         |   是   |  ✓  |
| 9  | PUT    | `/api/auth/phone`                | `changePhone(req)`       |   是   |  ✓  |
| 10 | PUT    | `/api/auth/password/reset`       | `resetPassword(req)`     |   否   |  —  |
| 11 | PUT    | `/api/auth/password`             | `changePassword(req)`    |   是   |  ✓  |
| 12 | DELETE | `/api/auth/account`              | `deactivateAccount(req)` |   是   |  ✓  |
| 13 | GET    | `/api/auth/sessions/current`     | `checkSession()`         |   是   |  —  |

> #10/#11 路径已从系统设计的同名 `PUT /api/auth/password` 修正为 `reset` 和 `/password` 两个不同路径（方案 A），避免 RESTful 冲突。

### 2.3 MVP CAPTCHA 接口映射（独立实现）

| #  | 方法   | 路径                               | 方法名                       | 需登录 | 审计 |
| -- | ------ | ---------------------------------- | ---------------------------- | :----: | :--: |
| 14 | GET    | `/api/auth/captcha`              | `getCaptcha(req)`          |   否   |  —  |
| 15 | POST   | `/api/auth/captcha/register`     | `registerByCaptcha(req)`   |   否   |  —  |
| 16 | POST   | `/api/auth/captcha/login`        | `loginByCaptcha(req)`      |   否   |  ✓  |
| 17 | POST   | `/api/auth/captcha/password/reset` | `resetPasswordByCaptcha(req)` | 否 |  —  |
| 18 | PUT    | `/api/auth/captcha/phone`        | `changePhoneByCaptcha(req)`|   是   |  ✓  |
| 19 | DELETE | `/api/auth/captcha/account`      | `deactivateAccount(req)`   |   是   |  ✓  |

> #14~#19 为 MVP 阶段独立端点，与原有端点完全隔离。原有端点不动，后续接入真实短信后 CAPTCHA 端点可整体下线。

### 2.4 C 端参数校验约定

所有 mall-auth Controller 统一使用 `@Validated`（或 `@Valid`）进行参数校验，禁止手动 null 检查。

**规则：**
- 请求 DTO 字段使用 `jakarta.validation.constraints`：`@NotBlank`、`@NotNull`、`@Size`、`@Pattern`
- Controller 方法参数使用 `@Valid @RequestBody` 触发校验
- 校验失败由 `MallExceptionHandler`（mall-common）统一处理 → `MallResult.error("A0401", 字段错误消息)`
- 密码字段使用 `@Size(min=8, max=32)` 覆盖长度，`validatePassword()` 仅负责字母+数字组合检查

> 管理端 Controller 不受此约束（沿用若依 `ruoyi-common-security`）。

---

## 3 核心类设计

### 3.1 AuthServiceImpl

位于 `service/impl/AuthServiceImpl.java`，认证核心业务编排。

核心依赖：`TokenService`、`SmsService`、`RemoteUserAdapter`、`WechatAdapter`、`Redis`

**register(req)**：

- ①校验 `isPrivacyAgreed=1`，否则 `A0101`
- ②校验密码复杂度（8~32 位，字母+数字），否则 `A0121`
- ③校验短信验证码：调 `SmsService.verify(phone, smsCode, "register")`
- ④调 `RemoteUserAdapter.findByPhone(phone)` → 已存在则 `A0151`
- ⑤密码 BCrypt 哈希
- ⑥调 `RemoteUserAdapter.register(phone, passwordHash)` → 返回 `userId`
- ⑦签发 token：`TokenService.issue(userId)`
- ⑧返回 `TokenResp`

**loginByPassword(req)**：

- ①调 `RemoteUserAdapter.findByPhone(phone)` → 不存在则 `A0201`
- ②校验用户状态：冻结 `A0202` / 注销 `A0203`
- ③校验密码错误次数：Redis `mall:auth:pwd_err:{userId}` 超 5 次临时锁定 `A0211`
- ④BCrypt 校验密码：不通过 → 错误计数+1 → `A0210`
- ⑤签发 token：`TokenService.issue(userId)`
- ⑥清除错误计数，记录审计日志

**loginBySms(req)**：

- ①校验短信验证码
- ②调 `RemoteUserAdapter.findByPhone(phone)` → 不存在则自动注册（调 `register` 逻辑，设 `isNewUser=true`）
- ③存在则校验状态 + 签发 token

**loginByWechat(req)**：

- ①调 `WechatAdapter.code2session(wechatCode, scene)` → 获取 `openId` + `unionId`
- ②调 `RemoteUserAdapter.findByWechatOpenId(openId)` → 存在则签发 token
- ③不存在则调 `RemoteUserAdapter.registerByWechat(openId, unionId)` 自动创建用户
- ④返回含 `isNewUser` 的 `TokenResp`

**changePassword(req)**：需登录，校验旧密码 → BCrypt 新密码 → 调 `RemoteUserAdapter.updatePassword`

**resetPassword(req)**：无需登录，校验短信验证码 → BCrypt 新密码 → 调 `RemoteUserAdapter.updatePassword`

**deactivateAccount(req)**：校验短信验证码 + 身份一致 → 调 `RemoteUserAdapter.deactivateAccount` + `TokenService.revokeAll(userId)`

### 3.2 TokenServiceImpl

位于 `service/impl/TokenServiceImpl.java`，JWT token 全生命周期管理。

**issue(userId)**：

- ①生成 `accessToken`：JWT payload `{userId, iat, exp=+30min, jti=UUID}`
- ②生成 `refreshToken`：JWT payload `{userId, iat, exp=+7d, jti=UUID, type="refresh"}`
- ③Redis 写 session：key `mall:auth:session:{userId}:{jti}` → value `{device, ip, loginTime}`，TTL 同 token 有效期
- ④Redis 写 refreshToken 映射：key `mall:auth:refresh:{jti}` → value `userId`，TTL 7d
- ⑤accessToken/refreshToken 的 issuer/secret 与管理端 ruoyi-auth 不同

**verify(accessToken)**：

- ①JWT 签名校验 + 过期校验 → 失败抛 `TokenException("A0231", "token 无效或已过期")`
- ②Redis 黑名单检查：key `mall:auth:blacklist:{jti}` 存在 → 抛 `TokenException("A0231", "token 已被撤销")`
- ③Redis session 存在性检查：key `mall:auth:session:{userId}:{jti}` 不存在 → 抛 `TokenException("A0231", "token 会话不存在或已过期")`
- ④返回 `userId`

**refresh(refreshToken)**：

- ①校验 refreshToken 有效性（同 verify 逻辑）→ 失败抛 `TokenException("A0231", ...)`
- ②校验 `type="refresh"` → 否抛 `TokenException("A0231", "refreshToken 类型错误")`
- ③黑名单检查 → 已撤销抛 `TokenException("A0231", "refreshToken 已被撤销")`
- ④Redis refresh 映射存在性 → 不存在抛 `TokenException("A0231", "refreshToken 已过期")`
- ⑤一次性轮换：旧 refreshToken jti 加入黑名单（TTL 同原 exp） + 删除旧 session
- ⑥签发新 token 对

**revoke(accessToken)**：jti 加入黑名单 `SETEX mall:auth:blacklist:{jti} {exp-now} "logout"` → 删除 session

**revokeAll(userId)**：删除 `mall:auth:session:{userId}:*` 全部 key，注销时踢所有端下线

**Token 策略**：

| 参数             |      值      | 说明                                  |
| ---------------- | :----------: | ------------------------------------- |
| accessToken TTL  |    30min    | 短期安全令牌                          |
| refreshToken TTL |      7d      | 长期刷新令牌，一次性轮换              |
| 多端共存         |      ✅      | 每端独立 jti，PC+手机+Pad 各自登录    |
| 密码修改后       | 踢其他端下线 | 修改密码/重置密码后 revokeAll(userId) |
| 注销后           |  全部踢下线  | revokeAll(userId)                     |

### 3.3 SmsServiceImpl

位于 `service/impl/SmsServiceImpl.java`，短信验证码管理。

**send(phone, scene)**：

- ①频率控制：Redis `mall:auth:sms:limit:{phone}` SETNX TTL 60s → 未过冷却期 `A0410`
- ②IP 限制：Redis `mall:auth:sms:ip:{ip}` 每日计数，超阈值 `A0410`
- ③场景白名单校验：scene 必须在 `register/login/reset_password/change_phone/bind_phone` 内
- ④生成 6 位数字 code
- ⑤Redis 存储：`SETEX mall:auth:sms:code:{phone}:{scene} 300 {code}`
- ⑥调 `SmsProviderAdapter.send(phone, code, scene)`
- ⑦返回 `expireIn=300`

**verify(phone, code, scene)**：

- ①查 Redis：`GET mall:auth:sms:code:{phone}:{scene}` → null 则 `A0132`（过期）
- ②比对 code → 不匹配 `A0131`
- ③验证成功 DELETE 该 key（一次性消费）
- ④验证尝试计数：Redis 每日 `mall:auth:sms:try:{phone}` 超 5 次则 `A0241`

### 3.4 DecryptServiceImpl

位于 `service/impl/DecryptServiceImpl.java`，手机号解密服务（暴露为 Feign 接口）。

- 持有 Nacos 密钥 `mall.security.aes-key`
- `decrypt(encryptedData)`：AES-256-GCM 解密，返回明文
- `batchDecrypt(List<encryptedData>)`：批量解密，最多 50 条
- 解密结果 Redis 缓存 1 分钟：`mall:auth:decrypt:{sha256(ciphertext)}` → 明文
- Feign 超时 2s，失败重试 1 次

---

## 4 Token 会话设计

### 4.1 Redis Key 规范

| Key 模式                                   | 用途                     | TTL                |
| ------------------------------------------ | ------------------------ | ------------------ |
| `mall:auth:session:{userId}:{jti}`       | 会话信息（设备/IP/时间） | 同 accessToken exp |
| `mall:auth:refresh:{jti}`                | refreshToken 映射        | 7d                 |
| `mall:auth:blacklist:{jti}`              | 黑名单（注销/刷新作废）  | 原 token 剩余时间  |
| `mall:auth:sms:code:{phone}:{scene}`     | 短信验证码               | 300s               |
| `mall:auth:sms:limit:{phone}`            | 发送冷却                 | 60s                |
| `mall:auth:sms:try:{phone}`              | 验证尝试计数             | 24h                |
| `mall:auth:sms:ip:{ip}`                  | IP 日发送量              | 24h                |
| `mall:auth:pwd_err:{userId}`             | 密码错误计数             | 30min              |
| `mall:auth:decrypt:{sha256(ciphertext)}` | 解密结果缓存             | 60s                |

### 4.2 Token 刷新流程

```
客户端: accessToken 过期 → 用 refreshToken 调 /refresh
  → TokenService.refresh(oldRefreshToken):
      ① 校验 refreshToken 有效性（签名+过期+黑名单+session）
      ② 该 refreshToken jti 加入黑名单（TTL=剩余有效期），删除旧 refresh session
      ③ 旧 accessToken jti 加入宽限黑名单（TTL=剩余有效期，最大1h）
      ④ 签发新 accessToken + refreshToken 对
      ⑤ 返回新 token 对
  → 客户端: 收到新 token，丢弃旧 token
```

宽限机制：旧 accessToken 在刷新后 1h 内仍可用（防止刷新与请求的并发时序问题），1h 后黑名单 TTL 过期自动失效。

### 4.3 多端管理

- 每端签发独立 jti，存储 session 为 `mall:auth:session:{userId}:{jti}`
- 查询在线设备：`SCAN mall:auth:session:{userId}:*` 获取所有活跃 session
- 踢指定端下线：删除对应 jti 的 session + 加入黑名单
- 改密/注销 → 踢全部下线：批量删除 `mall:auth:session:{userId}:*`

---

## 5 短信验证码设计

### 5.1 发送防刷

| 维度   | 机制             |  阈值  |
| ------ | ---------------- | :-----: |
| 手机号 | Redis SETNX 冷却 | 60s/次 |
| IP     | Redis 计数器     | 10次/天 |
| 手机号 | Redis 计数器     | 5次/天 |

**流程：**

1. IP 限流检查 → 超限则 `A0410`
2. 手机号冷却检查 → 冷却中 `A0410`
3. 手机号日发送量检查 → 超限 `A0410`
4. 生成 code + 存储 Redis + 调 SMS SDK
5. 异步记录发送结果

### 5.2 验证码消费

- code 5 分钟有效，验证成功立即 DELETE（一次性）
- 验证失败不删除，每日同一手机号最多尝试 5 次，超限 `A0241`
- 验证失败只计次数，不返回"还剩几次"（防攻击者探测）

---

## 6 密码安全设计

### 6.1 密码哈希

- 算法：BCrypt（cost=12）
- 哈希在 mall-auth 侧完成，调 mall-user 时传入已哈希的密码
- mall-user 只存储哈希值，不持有 BCrypt 逻辑

### 6.2 密码强度

| 规则   | 说明                                |
| ------ | ----------------------------------- |
| 长度   | 8~32 位                             |
| 字符   | 必须同时包含字母和数字              |
| 禁止   | 与手机号相同、纯数字、连续/重复字符 |
| 错误码 | 不满足 →`A0121`                  |

### 6.3 登录保护

| 机制         | 说明                                                          |
| ------------ | ------------------------------------------------------------- |
| 密码错误计数 | Redis `mall:auth:pwd_err:{userId}`，连续 5 次错误锁定 30min |
| 冻结检查     | 登录前查用户状态，冻结 `A0202`                              |
| 注销检查     | 注销用户不可登录 `A0203`                                    |
| 审计日志     | 每次登录无论成功/失败都记录                                   |

---

## 7 微信认证设计

### 7.1 微信登录流程

```
1. 客户端: 微信 wx.login() → 临时 code
2. 客户端: POST /api/auth/wechat/sessions { wechatCode, scene }
3. mall-auth: WechatAdapter.code2session(code) → 微信 API 返回 { openId, unionId, sessionKey }
4. mall-auth: RemoteUserAdapter.findByWechatOpenId(openId)
   ├─ 已有用户 → 签发 token
   └─ 新用户 → RemoteUserAdapter.registerByWechat(openId, unionId) → 签发 token { isNewUser: true }
```

### 7.2 手机号绑定

微信小程序通过 `<button open-type="getPhoneNumber">` 获取加密手机号：

```
1. 客户端: POST /api/auth/wechat/phone_binding { encryptedData, iv }
2. mall-auth: 用该用户的 sessionKey 解密 → 获取手机号
3. 校验手机号未被其他账号绑定
4. RemoteUserAdapter.updatePhone(userId, phone)
```

### 7.3 WechatAdapter 错误处理

| 微信错误码                 | 映射                        |
| -------------------------- | --------------------------- |
| `-1` 系统繁忙            | C0230（重试）               |
| `40029` code 无效        | C0230                       |
| `40163` code 已使用      | C0230                       |
| `-41003` sessionKey 过期 | C0230（客户端需重新 login） |

---

## 8 MQ 事件

mall-auth 生产 **1 个** topic，消费 **0 个**：

| Topic                    | Payload 字段                                                                 | 发布时机         |
| ------------------------ | ---------------------------------------------------------------------------- | ---------------- |
| `mall:user:registered` | `userId`、`phone`（脱敏）、`registerTime`、`channel`（PHONE/WECHAT） | 新用户注册成功后 |

> topic `mall:user:registered` 由 mall-user 消费，用于初始化积分账户、成长值等。

与 mall-order/mall-payment 不同：mall-auth **本地不维护 Outbox 表**（无 MySQL），事件由 `AuthServiceImpl` 调 `RocketMQTemplate` 直接同步发送。发送失败时记录日志 + 告警，不阻塞注册流程。

---

## 9 解密服务 Feign 接口

mall-auth 作为密钥持有方，向其他服务暴露解密接口（定义在 `mall-api`）：

| 方法                                  | 说明                                |
| ------------------------------------- | ----------------------------------- |
| `decrypt(encryptedData)`            | 解密单条 AES-256-GCM 密文，返回明文 |
| `batchDecrypt(List<encryptedData>)` | 批量解密，最多 50 条                |

调用方：

- `mall-order`：显示订单收货人手机号时调解密
- `mall-user`：管理端查看用户手机号时调解密
- `mall-search`：无（搜索不涉及 PII）

约束：

- 解密结果缓存 Redis 1 分钟（key 为 `sha256(ciphertext)`），减少重复解密
- Feign 超时 2s，失败重试 1 次
- 解密失败记录告警（非业务异常），不影响其他解密请求

---

## 10 审计日志

### 10.1 审计内容

| 操作              | 记录内容                             |
| ----------------- | ------------------------------------ |
| 登录（成功/失败） | userId、IP、设备、时间、结果         |
| 注册              | userId、IP、渠道（PHONE/WECHAT）     |
| 修改密码          | userId、IP、时间                     |
| 换绑手机号        | userId、旧号脱敏、新号脱敏、IP、时间 |
| 注销账号          | userId、IP、时间                     |
| 微信绑定手机号    | userId、绑定手机号脱敏、IP、时间     |

### 10.2 存储方式

C 端审计日志写入 `mall` 库独立表 `mall_audit_log`，Service 层直写 INSERT，不走若依 `RemoteLogService` Feign。

原因：
- `mall` 与 `ry-cloud` 分属不同数据库，跨库 Feign 写入 `sys_oper_log` 不可靠
- C 端审计与管理端审计在身份体系（userId vs adminId）、查看入口上完全独立

### 10.3 实现约定

- C 端 Controller 包路径：`com.mall.{module}.controller.api`
- C 端 Controller **禁止** `import com.ruoyi.common.log.*`（即不使用注解 `@Log`）
- 审计写入时机：登录/注册/AOP 拦截时，在本模块 Service 层直接 INSERT
- `mall_audit_log` 表 DDL 在 mall-auth 开发阶段落地（使用 Flyway 命名 `V2.0.0__mall_audit_log.sql`）

---

## 11 Nacos 配置

### 11.1 DataId: `mall-auth-dev.yml`

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
    jwt-secret: 7COWPc0I1OG/8Cby86JRsZhk6+kR3tNbKXgxwr45O1mPSZm1SqfRmXyekGo1UojKSEnjVDUSSI7a0HEVKLZcoQ==
    aes-key:

```

> 以上配置通过 Nacos 下发，支持 `@RefreshScope` 运行时动态刷新（标 \* 的需重启生效）。配置项通过 `MallAuthConfigProperties`（`@ConfigurationProperties(prefix = "mall.auth")` + `@RefreshScope`）注入，各 Service/Controller 通过构造注入获取，禁止使用 `@Value`。

### 11.2 本地配置文件 `bootstrap.yml`

```yaml
# mall-auth 认证服务
server:
  port: 9301

spring:
  application:
    name: mall-auth
  profiles:
    active: dev
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
      config:
        server-addr: 127.0.0.1:8848
  config:
    file-extension: yml
    import:
      - nacos:application-${spring.profiles.active}.${spring.config.file-extension}
      - nacos:${spring.application.name}-${spring.profiles.active}.${spring.config.file-extension}
```

> 注：`file-extension` 和 `import` 必须放在 `spring.config.*` 下（而非 `spring.cloud.nacos.config.*`），否则 Nacos 配置不会被加载。

### 11.3 配置项说明

| 配置项                            | 默认值 | 单位 | 说明                                   |
| --------------------------------- | ------ | :--: | -------------------------------------- |
| `mall.auth.access-token-ttl`    | 1800   |  秒  | accessToken 有效期（30min）            |
| `mall.auth.refresh-token-ttl`   | 604800 |  秒  | refreshToken 有效期（7d）              |
| `mall.security.jwt-secret`      | —     |  —  | JWT 签名密钥（Nacos 管理，\*）         |
| `mall.security.aes-key`         | —     |  —  | AES-256-GCM 密钥（Nacos 管理，\*）     |
| `mall.auth.sms.code-length`     | 6      |  位  | 验证码长度                             |
| `mall.auth.sms.code-ttl`        | 300    |  秒  | 验证码有效期                           |
| `mall.auth.sms.cooldown`        | 60     |  秒  | 同一手机号发送冷却                     |
| `mall.auth.sms.daily-limit`     | 5      |  次  | 同一手机号日发送上限                   |
| `mall.auth.sms.ip-daily-limit`  | 10     |  次  | 同一 IP 日发送上限                     |
| `mall.auth.pwd-err-limit`       | 5      |  次  | 密码连续错误锁定阈值                   |
| `mall.auth.pwd-err-ttl`         | 1800   |  秒  | 错误计数 TTL（30min）                  |
| `mall.auth.pwd-bcrypt-cost`     | 12     |  —  | BCrypt 哈希复杂度                      |
| `mall.auth.wechat.app-id`       | —     |  —  | 微信小程序 AppId（\*）                 |
| `mall.auth.wechat.app-secret`   | —     |  —  | 微信小程序 AppSecret（Nacos 加密，\*） |
| `mall.auth.decrypt.cache-ttl`   | 60     |  秒  | 解密结果缓存时间                       |
| `mall.auth.decrypt.batch-limit` | 50     |  条  | 批量解密上限                           |

---

## 12 错误码汇总

| 错误码 | HTTP | userTip                            | 说明                                   |
| ------ | :--: | ---------------------------------- | -------------------------------------- |
| 00000  | 200 | —                                 | 成功                                   |
| A0101  | 400 | 请同意隐私协议                     | 未同意隐私协议（isPrivacyAgreed != 1） |
| A0121  | 400 | 密码需 8~32 位且包含字母和数字     | 密码长度或复杂度不足                   |
| A0131  | 400 | 验证码错误                         | 短信验证码不匹配                       |
| A0132  | 400 | 验证码已过期                       | 验证码超过 5 分钟或已使用              |
| A0151  | 400 | 手机号已被注册                     | 注册/换绑时手机号已存在                |
| A0152  | 400 | 手机号格式错误                     | 手机号不符合格式                       |
| A0201  | 400 | 账户不存在                         | 登录时手机号未注册                     |
| A0202  | 400 | 账户已被冻结                       | 用户状态为冻结                         |
| A0203  | 400 | 账户已注销                         | 用户状态为已注销                       |
| A0210  | 400 | 密码错误                           | 密码不匹配                             |
| A0211  | 400 | 密码错误次数过多，请 30 分钟后重试 | 连续 5 次密码错误触发临时锁定          |
| A0231  | 401 | 登录已过期，请重新登录             | refresh_token 失效或 token 已注销      |
| A0241  | 400 | 验证码尝试次数过多                 | 同一手机号日验证码尝试超限             |
| A0301  | 401 | 请先登录                           | 未携带有效 token                       |
| A0310  | 401 | 登录凭证非法                       | token 被篡改或伪造                     |
| A0401  | 400 | 请完整填写信息                     | 必填参数为空                           |
| A0410  | 429 | 请求过于频繁，请稍后再试           | 短信/接口频率限制                      |
| B0001  | 500 | 系统繁忙，请稍后再试               | 未预期异常                             |
| C0110  | 500 | 服务暂时不可用                     | Redis 连接失败                         |
| C0220  | 503 | 短信服务异常                       | 短信平台调用失败                       |
| C0230  | 503 | 微信服务异常                       | 微信 code2session 或解密失败           |

> 错误码遵循阿里规范 A/B/C 格式，除 C0220 外全部来自系统设计第二章。

---

## 13 MVP CAPTCHA 图片验证码

### 13.1 背景

MVP 阶段无真实短信通道，用图片验证码代替短信验证码。原有 13 个认证端点**完全保留不动**，独立新增 6 个 `/api/auth/captcha/*` 端点。MVP 前端只调用 CAPTCHA 端点，后续接入真实短信后 CAPTCHA 端点可整体删除。

### 13.2 实现类

| 类 | 说明 |
|----|------|
| `CaptchaController` | 6 个 CAPTCHA 端点，位于 `controller/` 包 |
| `CaptchaService` | 接口 |
| `CaptchaServiceImpl` | 生成/校验逻辑 |

**generate()**：EasyCaptcha 生成 4 位字符图片（排除 0/O/1/I），JPEG Base64 返回，Redis 存储 300s。

**verify(captchaKey, code)**：Redis 比对（忽略大小写），一次性消费。失败计数按 IP 防刷。

### 13.3 端点

| 端点 | 替代说明 |
|------|---------|
| `GET /api/auth/captcha` | 获取图片验证码，返回 `{ captchaKey, captchaImage }` |
| `POST /api/auth/captcha/register` | 用 `captchaKey+captchaCode` 替换 `smsCode` |
| `POST /api/auth/captcha/login` | 密码登录 + 图片验证码防刷 |
| `POST /api/auth/captcha/password/reset` | 用 `captchaKey+captchaCode` 替换 `smsCode` |
| `PUT /api/auth/captcha/phone` | 用 `password` 替换短信验证 |
| `DELETE /api/auth/captcha/account` | 用 `password` 替换短信验证 |

### 13.4 依赖

```xml
<dependency>
    <groupId>com.github.whvcse</groupId>
    <artifactId>easy-captcha</artifactId>
    <version>1.6.2</version>
</dependency>
```

### 13.5 设计文档索引

完整设计见 `docs/superpowers/specs/2026-05-23-mall-auth-mvp-design.md`。

---
