# mall-auth MVP CAPTCHA 子系统实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 CAPTCHA 验证码子系统（图片验证码替代短信验证码），涵盖 6 个 CAPTCHA 端点（获取/注册/登录/改密/换绑手机/注销）和配套的 JWT Token 服务；同时建立 C 端公共基础设施模块 `mall-common`

**架构:** mall-common 提供全局异常处理器兜底（`MallExceptionHandler`）；mall-auth 持 CAPTCHA + Token + BCrypt，不持 MySQL 表，用户 CRUD 通过 Feign 调 mall-user；mall-api 新增 Feign 契约 + DTO 定义；mall-user 扩展 Mapper/Service + 新增 InnerController

**设计文档:** `docs/superpowers/specs/2026-05-23-mall-auth-mvp-design.md`（当前生效）

**前置条件:**
- Nacos 运行中，`mall.security.jwt-secret` 已配置到 `ruoyi-gateway-dev.yml` 和 `mall-auth-dev.yml`
- 所有设计文档已更新到最新

**技术栈:**
- JDK 17, Spring Boot 4.0.3, Spring Cloud 2025.1.0
- JJWT 0.9.1（HS512），io.jsonwebtoken（ruoyi parent 已管理版本）
- EasyCaptcha 1.6.2（`com.github.whvcse`）
- Spring Security Crypto 6.x（BCryptPasswordEncoder）
- Redis（`ruoyi-common-redis` 封装 `RedisCache`）
- 纯 MyBatis（非 MyBatis-Plus），XML mapper
- PostgreSQL（通过若依动态数据源）

---

## 文件结构总览

### 创建的文件

| # | 文件路径 | 用途 |
|---|---------|------|
| 1 | `server/mall/mall-api/src/main/java/com/mall/api/DTO/MallResult.java` | C 端统一响应体 |
| 2 | `server/mall/mall-api/src/main/java/com/mall/api/DTO/MallUserDTO.java` | 用户数据传输对象 |
| 3 | `server/mall/mall-api/src/main/java/com/mall/api/feign/RemoteUserService.java` | 用户 Feign 契约（含内部请求类） |
| 4 | `server/mall/mall-api/src/main/java/com/mall/api/feign/RemoteAuthService.java` | 认证 Feign 契约（占位） |
| 5 | `server/mall/mall-common/src/main/java/com/mall/common/exception/CaptchaException.java` | 验证码异常类 |
| 6 | `server/mall/mall-common/src/main/java/com/mall/common/exception/TokenException.java` | Token 异常类（A0231） |
| 7 | `server/mall/mall-auth/src/main/java/com/mall/auth/DTO/response/CaptchaResponse.java` | 验证码响应（captchaKey + captchaImage） |
| 8 | `server/mall/mall-auth/src/main/java/com/mall/auth/DTO/response/TokenResponse.java` | Token 响应（accessToken/refreshToken/expiresIn） |
| 9 | `server/mall/mall-auth/src/main/java/com/mall/auth/service/TokenService.java` | Token 服务接口 |
| 10 | `server/mall/mall-auth/src/main/java/com/mall/auth/service/impl/TokenServiceImpl.java` | Token 服务实现（JWT + Redis） |
| 11 | `server/mall/mall-auth/src/main/java/com/mall/auth/service/CaptchaService.java` | 验证码服务接口 |
| 12 | `server/mall/mall-auth/src/main/java/com/mall/auth/service/impl/CaptchaServiceImpl.java` | 验证码服务实现（EasyCaptcha + Redis） |
| 13 | `server/mall/mall-auth/src/main/java/com/mall/auth/DTO/request/CaptchaRegisterReq.java` | 注册请求 DTO |
| 14 | `server/mall/mall-auth/src/main/java/com/mall/auth/DTO/request/CaptchaLoginReq.java` | 登录请求 DTO |
| 15 | `server/mall/mall-auth/src/main/java/com/mall/auth/DTO/request/CaptchaResetPasswordReq.java` | 重置密码请求 DTO |
| 16 | `server/mall/mall-auth/src/main/java/com/mall/auth/DTO/request/CaptchaChangePhoneReq.java` | 换绑手机请求 DTO |
| 17 | `server/mall/mall-auth/src/main/java/com/mall/auth/DTO/request/CaptchaDeactivateReq.java` | 注销请求 DTO |
| 18 | `server/mall/mall-auth/src/main/java/com/mall/auth/controller/CaptchaController.java` | CAPTCHA 6 端点 Controller |
| 19 | `server/mall/mall-auth/src/main/java/com/mall/auth/controller/AuthController.java` | 13 端点骨架占位 Controller |
| 20 | `server/mall/mall-auth/src/main/java/com/mall/auth/service/AuthService.java` | 认证服务接口（仅签名） |
| 21 | `server/mall/mall-auth/src/main/java/com/mall/auth/service/DecryptService.java` | 解密服务接口（仅签名） |
| 22 | `server/mall/mall-auth/src/main/java/com/mall/auth/service/SmsService.java` | 短信服务接口（仅签名） |
| 23 | `server/mall/mall-auth/src/main/java/com/mall/auth/infrastructure/feign/RemoteUserAdapter.java` | Feign 调用封装骨架 |
| 24 | `server/mall/mall-user/src/main/java/com/mall/user/controller/inner/RemoteUserInnerController.java` | mall-user 内部 Feign 入口 |
| **25** | `server/mall/mall-common/pom.xml` | mall-common 模块 POM |
| **26** | `server/mall/mall-common/src/main/java/com/mall/common/handler/MallExceptionHandler.java` | 全局异常处理器（CaptchaException/TokenException/FeignException + 兜底） |

### 修改的文件

| # | 文件路径 | 修改内容 |
|---|---------|---------|
| 26 | `server/mall/pom.xml` | 注册 `<module>mall-common</module>` |
| 27 | `server/mall/mall-common/pom.xml` | 加 `feign-core` 依赖（MallExceptionHandler 需要 `FeignException`） |
| 28 | `server/mall/mall-auth/pom.xml` | 加 easy-captcha + spring-security-crypto + jjwt 依赖 |
| 29 | `server/mall/mall-auth/src/main/java/com/mall/auth/MallAuthApplication.java` | `@EnableFeignClients` + `"com.mall.api"` + `"com.mall.common"` |
| 30 | `server/mall/mall-user/pom.xml` | 加 `mall-common` 依赖 |
| 31 | `server/mall/mall-user/src/main/java/com/mall/user/MallUserApplication.java` | `@EnableFeignClients` + `"com.mall.api"` + `"com.mall.common"` |
| 32 | `server/mall/mall-user/src/main/java/com/mall/user/mapper/MallUserMapper.java` | 追加 4 个 C 端方法 |
| 33 | `server/mall/mall-user/src/main/java/com/mall/user/service/IMallUserService.java` | 追加 6 个 C 端方法 |
| 34 | `server/mall/mall-user/src/main/java/com/mall/user/service/impl/MallUserServiceImpl.java` | 追加 6 个 C 端方法实现 |
| 35 | `server/mall/mall-user/src/main/resources/mapper/mall-user/MallUserMapper.xml` | 追加 4 个 SQL 片段 |
| 36 | `server/mall/mall-product/order/payment/marketing/search/pom.xml` | 各加 `mall-common` 依赖 |

---

### Task 1: mall-common 公共基础设施 — 模块创建 + MallExceptionHandler

**设计依据:** `06_mall-common公共模块设计.md` §3（MallExceptionHandler 详细设计）

**前置说明:** mall-common 是 C 端公共基础设施模块，所有 `mall-*` 业务模块通过 Maven 依赖引入。

**文件:**
- Create: `server/mall/mall-common/pom.xml`（已在设计阶段创建）
- Create: `server/mall/mall-common/src/main/java/com/mall/common/handler/MallExceptionHandler.java`

**修改:**
- `server/mall/mall-common/pom.xml` → 追加 `feign-core` 依赖（MallExceptionHandler 需要 `FeignException`）

**修改:**
- `server/mall/pom.xml` → `<module>mall-common</module>`（已在设计阶段完成）
- 各业务模块 pom.xml → 加 `mall-common` 依赖（已在设计阶段完成）

- [ ] **Step 0: mall-common/pom.xml 追加 feign-core 依赖（MallExceptionHandler 需要 FeignException）**

在 `</dependencies>` 前插入：

```xml
        <!-- Feign Core（MallExceptionHandler 引用 FeignException）-->
        <dependency>
            <groupId>io.github.openfeign</groupId>
            <artifactId>feign-core</artifactId>
        </dependency>
```

- [ ] **Step 1: 在 MallAuthApplication 和 MallUserApplication 的 `@SpringBootApplication` 中加入 `"com.mall.common"` 扫描包**

使用示例：
```java
@SpringBootApplication(scanBasePackages = {"com.mall.auth", "com.mall.common"})
```

> 因为 `@RestControllerAdvice` 在 `com.mall.common.handler` 下，`@SpringBootApplication` 默认只扫描启动类所在包，不扫描 JAR 依赖中的 `com.mall.common`。

- [ ] **Step 2: 编写 MallExceptionHandler.java**

```java
package com.mall.common.handler;

import com.mall.api.DTO.MallResult;
import com.mall.common.exception.CaptchaException;
import com.mall.common.exception.TokenException;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MallExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(MallExceptionHandler.class);

    @ExceptionHandler(CaptchaException.class)
    public MallResult<Void> handleCaptcha(CaptchaException e) {
        return MallResult.error(e.getErrorCode(), e.getMessage(), e.getUserTip());
    }

    @ExceptionHandler(TokenException.class)
    public MallResult<Void> handleToken(TokenException e) {
        log.warn("Token 异常: {} - {}", e.getErrorCode(), e.getMessage());
        return MallResult.error(e.getErrorCode(), e.getMessage(), e.getUserTip());
    }

    @ExceptionHandler(FeignException.class)
    public MallResult<Void> handleFeign(FeignException e) {
        log.error("Feign 调用异常: {}", e.getMessage());
        return MallResult.error("B0001", "服务暂时不可用，请稍后重试");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public MallResult<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "请完整填写信息";
        return MallResult.error("A0401", msg, msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public MallResult<Void> handleConstraintViolation(ConstraintViolationException e) {
        return MallResult.error("A0401", e.getMessage(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public MallResult<Void> handleException(Exception e) {
        log.error("unhandled exception", e);
        return MallResult.error("B0001", "系统繁忙，请稍后再试");
    }
}
```

> 注：`@Order(Ordered.HIGHEST_PRECEDENCE)` 确保优先级高于若依 `GlobalExceptionHandler`，避免 C 端响应被 `AjaxResult` 污染。

- [ ] **Step 3: git commit**
```bash
git add server/mall/mall-common/ server/mall/pom.xml server/mall/mall-*/pom.xml server/mall/mall-auth/src/main/java/com/mall/auth/MallAuthApplication.java server/mall/mall-user/src/main/java/com/mall/user/MallUserApplication.java
git commit -m "feat(mall-common): add C-end common module with MallExceptionHandler"
```

---

### Task 2: mall-api 契约层 — MallResult + DTO + Feign 接口

**设计依据:** `05_mall-api契约层设计.md` §7（MallResult）+ §3.1（RemoteUserService）+ §8.1（MallUserDTO）+ spec §8

**文件:**
- Create: `server/mall/mall-api/src/main/java/com/mall/api/DTO/MallResult.java`
- Create: `server/mall/mall-api/src/main/java/com/mall/api/DTO/MallUserDTO.java`
- Create: `server/mall/mall-api/src/main/java/com/mall/api/feign/RemoteUserService.java`
- Create: `server/mall/mall-api/src/main/java/com/mall/api/feign/RemoteAuthService.java`

- [ ] **Step 1: 编写 MallResult.java**

```java
package com.mall.api.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MallResult<T> {

    private String errorCode;
    private String errorMessage;
    private String userTip;
    private T data;
    private String requestId;

    // 静态工厂方法
    public static <T> MallResult<T> success(T data) {
        MallResult<T> result = new MallResult<>();
        result.errorCode = "00000";
        result.errorMessage = "操作成功";
        result.userTip = "";
        result.data = data;
        return result;
    }

    public static <T> MallResult<T> error(String errorCode, String errorMessage) {
        MallResult<T> result = new MallResult<>();
        result.errorCode = errorCode;
        result.errorMessage = errorMessage;
        result.userTip = "";
        return result;
    }

    public static <T> MallResult<T> error(String errorCode, String errorMessage, String userTip) {
        MallResult<T> result = new MallResult<>();
        result.errorCode = errorCode;
        result.errorMessage = errorMessage;
        result.userTip = userTip;
        return result;
    }

    // getters / setters
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getUserTip() { return userTip; }
    public void setUserTip(String userTip) { this.userTip = userTip; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}
```

- [ ] **Step 2: 编写 MallUserDTO.java**

```java
package com.mall.api.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MallUserDTO {

    private String id;
    private String phone;
    private String phoneHash;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String nickname;
    private String avatar;
    private String email;
    private String emailHash;
    private String gender;
    private String userStatus;
    private String registerType;
    private String registerIp;
    private String isPrivacyAgreed;

    // getters / setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPhoneHash() { return phoneHash; }
    public void setPhoneHash(String phoneHash) { this.phoneHash = phoneHash; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getEmailHash() { return emailHash; }
    public void setEmailHash(String emailHash) { this.emailHash = emailHash; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getUserStatus() { return userStatus; }
    public void setUserStatus(String userStatus) { this.userStatus = userStatus; }
    public String getRegisterType() { return registerType; }
    public void setRegisterType(String registerType) { this.registerType = registerType; }
    public String getRegisterIp() { return registerIp; }
    public void setRegisterIp(String registerIp) { this.registerIp = registerIp; }
    public String getIsPrivacyAgreed() { return isPrivacyAgreed; }
    public void setIsPrivacyAgreed(String isPrivacyAgreed) { this.isPrivacyAgreed = isPrivacyAgreed; }
}
```

- [ ] **Step 3: 编写 RemoteUserService.java**

```java
package com.mall.api.feign;

import com.mall.api.DTO.MallUserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

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

    static class RegisterRequest {
        private String phone;
        private String phoneHash;
        private String password;
        private String registerType;

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getPhoneHash() { return phoneHash; }
        public void setPhoneHash(String phoneHash) { this.phoneHash = phoneHash; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getRegisterType() { return registerType; }
        public void setRegisterType(String registerType) { this.registerType = registerType; }
    }

    static class PasswordUpdateRequest {
        private String newPassword;

        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    static class PhoneUpdateRequest {
        private String newPhone;
        private String newPhoneHash;

        public String getNewPhone() { return newPhone; }
        public void setNewPhone(String newPhone) { this.newPhone = newPhone; }
        public String getNewPhoneHash() { return newPhoneHash; }
        public void setNewPhoneHash(String newPhoneHash) { this.newPhoneHash = newPhoneHash; }
    }
}
```

- [ ] **Step 4: 编写 RemoteAuthService.java（占位骨架）**

```java
package com.mall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(contextId = "remoteAuthService", value = "mall-auth")
public interface RemoteAuthService {
    // MVP 阶段不实现，后续用于解密手机号
}
```

- [ ] **Step 5: 编译验证 mall-api**

```bash
mvn clean install -f server/mall/pom.xml -pl mall-api -am -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add server/mall/mall-api/src/main/java/com/mall/api/DTO/MallResult.java server/mall/mall-api/src/main/java/com/mall/api/DTO/MallUserDTO.java server/mall/mall-api/src/main/java/com/mall/api/feign/RemoteUserService.java server/mall/mall-api/src/main/java/com/mall/api/feign/RemoteAuthService.java
git commit -m "feat(mall-api): add MallResult, MallUserDTO, RemoteUserService Feign contract"
```

---

### Task 3: 依赖注入 + @EnableFeignClients 修复

**设计依据:** spec §10

**文件:**
- Modify: `server/mall/mall-auth/pom.xml`
- Modify: `server/mall/mall-auth/src/main/java/com/mall/auth/MallAuthApplication.java`
- Modify: `server/mall/mall-user/src/main/java/com/mall/user/MallUserApplication.java`

- [ ] **Step 1: mall-auth/pom.xml 追加依赖**

在 `</dependencies>` 前插入：

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

        <!-- JJWT（HS512 JWT，ruoyi parent 已管理版本） -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt</artifactId>
        </dependency>

        <!-- Spring Boot Validation（@Valid + @NotBlank 等） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
```

- [ ] **Step 2: MallAuthApplication.java 修复 @EnableFeignClients**

```java
@EnableFeignClients(basePackages = {"com.ruoyi", "com.mall.api"})
```

- [ ] **Step 3: MallUserApplication.java 修复 @EnableFeignClients**

```java
@EnableFeignClients(basePackages = {"com.ruoyi", "com.mall.api"})
```

- [ ] **Step 4: 编译验证**

```bash
mvn clean install -f server/mall/pom.xml -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add server/mall/mall-auth/pom.xml server/mall/mall-auth/src/main/java/com/mall/auth/MallAuthApplication.java server/mall/mall-user/src/main/java/com/mall/user/MallUserApplication.java
git commit -m "chore: add easy-captcha/jjwt/validation deps, fix @EnableFeignClients scan to include com.mall.api"
```

---

### Task 4: mall-user — Mapper + Service 扩展（C 端方法）

**设计依据:** spec §9.1 + §9.2

**现有代码上下文:**
- `MallUserMapper.java` — 纯 MyBatis 接口，`selectMallUserById`, `insertMallUser`, `updateMallUser`, `deleteMallUserById`, `deleteMallUserByIds`
- `MallUserMapper.xml` — 定义 `MallUserResult` resultMap + `selectMallUserVo` SQL 片段 + 上述 CRUD
- `IMallUserService.java` — 对应 mapper 的标准调用接口
- `MallUserServiceImpl.java` — 自动注入 `MallUserMapper`，若依 `DateUtils.getNowDate()` 设置时间
- 表结构：`mall_user`，主键自增 `useGeneratedKeys="true" keyProperty="id"`

**文件:**
- Modify: `server/mall/mall-user/src/main/java/com/mall/user/mapper/MallUserMapper.java`
- Modify: `server/mall/mall-user/src/main/java/com/mall/user/service/IMallUserService.java`
- Modify: `server/mall/mall-user/src/main/java/com/mall/user/service/impl/MallUserServiceImpl.java`
- Modify: `server/mall/mall-user/src/main/resources/mapper/mall-user/MallUserMapper.xml`

- [ ] **Step 1: MallUserMapper.java 追加 4 个方法**

在 `int deleteMallUserById(String id);` 后插入：

```java
    MallUser selectByPhoneHash(@Param("phoneHash") String phoneHash);

    int updatePassword(@Param("id") String id, @Param("password") String password);

    int updatePhone(@Param("id") String id, @Param("phone") String phone, @Param("phoneHash") String phoneHash);

    int updateUserStatus(@Param("id") String id, @Param("userStatus") String userStatus);
```

- [ ] **Step 2: MallUserMapper.xml 追加 4 个 SQL 片段**

在 `</mapper>` 前插入：

```xml
    <select id="selectByPhoneHash" parameterType="String" resultMap="MallUserResult">
        <include refid="selectMallUserVo"/>
        where phone_hash = #{phoneHash}
        limit 1
    </select>

    <update id="updatePassword">
        update mall_user
        set password = #{password},
            update_time = sysdate()
        where id = #{id}
    </update>

    <update id="updatePhone">
        update mall_user
        set phone = #{phone},
            phone_hash = #{phoneHash},
            update_time = sysdate()
        where id = #{id}
    </update>

    <update id="updateUserStatus">
        update mall_user
        set user_status = #{userStatus},
            update_time = sysdate()
        where id = #{id}
    </update>
```

注意：若依使用 MySQL `sysdate()`，如果实际数据库是 PostgreSQL 需适配。但目前 mapper 已有 `DateUtils.getNowDate()` 在 Service 层手动设时间字段。但这里 update 直接写 SQL，`update_time` 用 Java 端更可靠。改为：

```xml
    <update id="updatePassword">
        update mall_user
        set password = #{password},
            update_time = now()
        where id = #{id}
    </update>

    <update id="updatePhone">
        update mall_user
        set phone = #{phone},
            phone_hash = #{phoneHash},
            update_time = now()
        where id = #{id}
    </update>

    <update id="updateUserStatus">
        update mall_user
        set user_status = #{userStatus},
            update_time = now()
        where id = #{id}
    </update>
```

- [ ] **Step 3: IMallUserService.java 追加 6 个 C 端方法**

在 `int deleteMallUserById(String id);` 后插入：

```java
    MallUser selectByPhoneHash(String phoneHash);

    MallUser selectByPhone(String phone);

    MallUser selectByWechatOpenId(String openId);

    String registerByPhone(String phone, String phoneHash, String passwordHash);

    int updatePasswordById(String id, String newPasswordHash);

    int updatePhoneById(String id, String newPhone, String newPhoneHash);

    int updateUserStatusById(String id, String userStatus);
```

- [ ] **Step 4: MallUserServiceImpl.java 追加实现**

在最后一个方法（`deleteMallUserById`）后插入：

```java
    @Override
    public MallUser selectByPhoneHash(String phoneHash) {
        return mallUserMapper.selectByPhoneHash(phoneHash);
    }

    @Override
    public MallUser selectByPhone(String phone) {
        String phoneHash = DigestUtils.sha256Hex(phone);
        return mallUserMapper.selectByPhoneHash(phoneHash);
    }

    @Override
    public MallUser selectByWechatOpenId(String openId) {
        // MVP 不实现微信登录
        return null;
    }

    @Override
    public String registerByPhone(String phone, String phoneHash, String passwordHash) {
        MallUser user = new MallUser();
        user.setPhone(phone);
        user.setPhoneHash(phoneHash);
        user.setPassword(passwordHash);
        user.setNickname("用户" + phone.substring(phone.length() - 4));
        user.setUserStatus("0");
        user.setRegisterType("phone");
        user.setIsPrivacyAgreed("1");
        user.setPrivacyAgreedTime(DateUtils.getNowDate());
        user.setCreateTime(DateUtils.getNowDate());
        mallUserMapper.insertMallUser(user);
        return user.getId();
    }

    @Override
    public int updatePasswordById(String id, String newPasswordHash) {
        return mallUserMapper.updatePassword(id, newPasswordHash);
    }

    @Override
    public int updatePhoneById(String id, String newPhone, String newPhoneHash) {
        return mallUserMapper.updatePhone(id, newPhone, newPhoneHash);
    }

    @Override
    public int updateUserStatusById(String id, String userStatus) {
        return mallUserMapper.updateUserStatus(id, userStatus);
    }
```

需要引入 `org.apache.commons.codec.digest.DigestUtils`：

```java
import org.apache.commons.codec.digest.DigestUtils;
```

检查文件头是否已有该 import，没有则加。

- [ ] **Step 5: 编译验证**

```bash
mvn clean install -f server/mall/pom.xml -pl mall-user -am -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add server/mall/mall-user/src/main/java/com/mall/user/mapper/MallUserMapper.java server/mall/mall-user/src/main/java/com/mall/user/service/IMallUserService.java server/mall/mall-user/src/main/java/com/mall/user/service/impl/MallUserServiceImpl.java server/mall/mall-user/src/main/resources/mapper/mall-user/MallUserMapper.xml
git commit -m "feat(mall-user): add C-end mapper methods and service extensions for phone-based operations"
```

---

### Task 5: mall-user — RemoteUserInnerController

**设计依据:** spec §9.3，路径前缀 `/inner/user`，Feign 直连不走网关

**文件:**
- Create: `server/mall/mall-user/src/main/java/com/mall/user/controller/inner/RemoteUserInnerController.java`

- [ ] **Step 1: 创建 RemoteUserInnerController.java**

```java
package com.mall.user.controller.inner;

import com.mall.api.DTO.MallUserDTO;
import com.mall.api.feign.RemoteUserService;
import com.mall.user.DO.MallUserDO;
import com.mall.user.service.IMallUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inner/user")
public class RemoteUserInnerController {

    @Autowired
    private IMallUserService mallUserService;

    @GetMapping("/phone/{phone}")
    public MallUserDTO findByPhone(@PathVariable("phone") String phone) {
        MallUser user = mallUserService.selectByPhone(phone);
        if (user == null) {
            return null;
        }
        return toDTO(user);
    }

    @PostMapping("/register")
    public String register(@RequestBody RemoteUserService.RegisterRequest request) {
        return mallUserService.registerByPhone(
                request.getPhone(),
                request.getPhoneHash(),
                request.getPassword()
        );
    }

    @PutMapping("/{userId}/password")
    public void updatePassword(@PathVariable("userId") String userId,
                               @RequestBody RemoteUserService.PasswordUpdateRequest request) {
        mallUserService.updatePasswordById(userId, request.getNewPassword());
    }

    @PutMapping("/{userId}/phone")
    public void updatePhone(@PathVariable("userId") String userId,
                            @RequestBody RemoteUserService.PhoneUpdateRequest request) {
        mallUserService.updatePhoneById(userId, request.getNewPhone(), request.getNewPhoneHash());
    }

    @DeleteMapping("/{userId}/account")
    public void deactivateAccount(@PathVariable("userId") String userId) {
        mallUserService.updateUserStatusById(userId, "2");
    }

    private MallUserDTO toDTO(MallUser user) {
        MallUserDTO dto = new MallUserDTO();
        BeanUtils.copyProperties(user, dto);
        dto.setPassword(null); // 不传输密码哈希
        return dto;
    }
}
```

注意：需要新增 `com.mall.user.controller.inner` 包。这个包是内部 Feign Controller 包。

- [ ] **Step 2: 编译验证**

```bash
mvn clean install -f server/mall/pom.xml -pl mall-user -am -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add server/mall/mall-user/src/main/java/com/mall/user/controller/inner/RemoteUserInnerController.java
git commit -m "feat(mall-user): add RemoteUserInnerController for Feign access to C-end user operations"
```

---

### Task 6: mall-common/mall-auth — 异常类 + 响应 DTO + Redis Key 常量

**设计依据:** spec §3.2 包结构、§5.4 Redis Key、§11 错误码

CaptchaException 和 TokenException 放 mall-common（避免循环依赖），CaptchaResponse、TokenResponse 放 mall-auth。

**文件:**
- Create: `server/mall/mall-common/src/main/java/com/mall/common/exception/CaptchaException.java`
- Create: `server/mall/mall-common/src/main/java/com/mall/common/exception/TokenException.java`
- Create: `server/mall/mall-auth/src/main/java/com/mall/auth/DTO/response/CaptchaResponse.java`
- Create: `server/mall/mall-auth/src/main/java/com/mall/auth/DTO/response/TokenResponse.java`

- [ ] **Step 1: 创建 CaptchaException.java（mall-common）**

```java
package com.mall.common.exception;

public class CaptchaException extends RuntimeException {

    private final String errorCode;
    private final String userTip;

    public CaptchaException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.userTip = message;
    }

    public CaptchaException(String errorCode, String message, String userTip) {
        super(message);
        this.errorCode = errorCode;
        this.userTip = userTip;
    }

    public String getErrorCode() { return errorCode; }
    public String getUserTip() { return userTip; }
}
```

- [ ] **Step 1b: 创建 TokenException.java（mall-common）**

```java
package com.mall.common.exception;

public class TokenException extends RuntimeException {

    private final String errorCode;
    private final String userTip;

    public TokenException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.userTip = message;
    }

    public TokenException(String errorCode, String message, String userTip) {
        super(message);
        this.errorCode = errorCode;
        this.userTip = userTip;
    }

    public String getErrorCode() { return errorCode; }
    public String getUserTip() { return userTip; }
}
```

- [ ] **Step 2: 创建 CaptchaResponse.java（mall-auth）**

```java
package com.mall.auth.DTO.response;

public class CaptchaResponse {

    private String captchaKey;
    private String captchaImage;

    public CaptchaResponse() {
    }

    public CaptchaResponse(String captchaKey, String captchaImage) {
        this.captchaKey = captchaKey;
        this.captchaImage = captchaImage;
    }

    public String getCaptchaKey() {
        return captchaKey;
    }

    public void setCaptchaKey(String captchaKey) {
        this.captchaKey = captchaKey;
    }

    public String getCaptchaImage() {
        return captchaImage;
    }

    public void setCaptchaImage(String captchaImage) {
        this.captchaImage = captchaImage;
    }
}
```

- [ ] **Step 3: 创建 TokenResponse.java（mall-auth）**

```java
package com.mall.auth.DTO.response;

public class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private long expiresIn;

    public TokenResponse() {
    }

    public TokenResponse(String accessToken, String refreshToken, long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}
```

- [ ] **Step 4: 编译验证**

```bash
# 先编译 mall-common（含 CaptchaException + TokenException）
mvn clean install -f server/mall/pom.xml -pl mall-common -am -DskipTests
# 再编译 mall-auth
mvn clean install -f server/mall/pom.xml -pl mall-auth -am -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add server/mall/mall-common/src/main/java/com/mall/common/exception/CaptchaException.java server/mall/mall-common/src/main/java/com/mall/common/exception/TokenException.java server/mall/mall-auth/src/main/java/com/mall/auth/DTO/response/CaptchaResponse.java server/mall/mall-auth/src/main/java/com/mall/auth/DTO/response/TokenResponse.java
git commit -m "feat(mall-common): add CaptchaException and TokenException; feat(mall-auth): add CaptchaResponse and TokenResponse"
```

---

### Task 7: mall-auth — TokenService 完整实现

**设计依据:** spec §4（完整设计）

**JJWT API 说明（基于 ruoyi 用的 jjwt 0.9.1）：**
- `Jwts.builder().setClaims(map).signWith(SignatureAlgorithm.HS512, secretBytes).compact()`
- `Jwts.parser().setSigningKey(secretBytes).parseClaimsJws(token).getBody()`
- 密钥为 `mall.security.jwt-secret` 的 UTF-8 字节数组（与 `MallAuthFilter` 一致）

**RedisCache API（ruoyi-common-redis 封装）：**
- `redisCache.setCacheObject(key, value)` — 无 TTL
- `redisCache.setCacheObject(key, value, timeout, TimeUnit)` — 带 TTL
- `redisCache.getCacheObject(key)` — 返回 Object
- `redisCache.deleteObject(key)` — 删除
- `redisCache.scan(pattern)` — 使用 SCAN 非阻塞迭代（替代 KEYS）

**文件:**
- Create: `server/mall/mall-auth/src/main/java/com/mall/auth/service/TokenService.java`
- Create: `server/mall/mall-auth/src/main/java/com/mall/auth/service/impl/TokenServiceImpl.java`

- [ ] **Step 1: 创建 TokenService.java 接口**

```java
package com.mall.auth.service;

import com.mall.auth.DTO.response.TokenResponse;

public interface TokenService {

    TokenResponse issue(String userId);

    String verify(String accessToken);

    TokenResponse refresh(String refreshToken);

    void revoke(String accessToken);

    void revokeAll(String userId);
}
```

- [ ] **Step 2: 创建 TokenServiceImpl.java**

```java
package com.mall.auth.service.impl;

import com.mall.auth.DTO.response.TokenResponse;
import com.mall.auth.service.TokenService;
import com.mall.common.exception.TokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

@Service
public class TokenServiceImpl implements TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenServiceImpl.class);

    private static final long ACCESS_TOKEN_TTL = 30;      // 30 分钟
    private static final long REFRESH_TOKEN_TTL = 7 * 24; // 7 天（小时）
    private static final String ISSUER = "mall-auth";

    private static final String KEY_SESSION = "mall:auth:session:";
    private static final String KEY_REFRESH = "mall:auth:refresh:";
    private static final String KEY_BLACKLIST = "mall:auth:blacklist:";

    private final byte[] jwtSecret;
    private final RedisTemplate<String, Object> redisTemplate;

    public TokenServiceImpl(
            @Value("${mall.security.jwt-secret}") String jwtSecret,
            RedisTemplate<String, Object> redisTemplate) {
        this.jwtSecret = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public TokenResponse issue(String userId) {
        String accessJti = UUID.randomUUID().toString().replace("-", "");
        String refreshJti = UUID.randomUUID().toString().replace("-", "");

        long now = System.currentTimeMillis();
        long accessExp = now + ACCESS_TOKEN_TTL * 60 * 1000;
        long refreshExp = now + REFRESH_TOKEN_TTL * 3600 * 1000;

        String accessToken = createJwt(userId, accessJti, now, accessExp, "access");
        String refreshToken = createJwt(userId, refreshJti, now, refreshExp, "refresh");

        // 存 session（access TTL）
        redisTemplate.opsForValue().set(
                KEY_SESSION + userId + ":" + accessJti,
                "1",
                ACCESS_TOKEN_TTL,
                TimeUnit.MINUTES);

        // 存 refresh 映射（refresh TTL）
        redisTemplate.opsForValue().set(
                KEY_REFRESH + refreshJti,
                userId,
                REFRESH_TOKEN_TTL,
                TimeUnit.HOURS);

        return new TokenResponse(accessToken, refreshToken, ACCESS_TOKEN_TTL * 60);
    }

    @Override
    public String verify(String accessToken) {
        Claims claims = parseJwt(accessToken);
        if (claims == null) {
            throw new TokenException("A0231", "token 无效或已过期");
        }
        if (!"access".equals(claims.get("type"))) {
            throw new TokenException("A0231", "token 类型错误");
        }

        String jti = claims.getId();
        String userId = claims.get("userId", String.class);

        Boolean blacklisted = redisTemplate.hasKey(KEY_BLACKLIST + jti);
        if (Boolean.TRUE.equals(blacklisted)) {
            throw new TokenException("A0231", "token 已被撤销");
        }

        Boolean sessionExists = redisTemplate.hasKey(KEY_SESSION + userId + ":" + jti);
        if (!Boolean.TRUE.equals(sessionExists)) {
            throw new TokenException("A0231", "token 会话不存在或已过期");
        }

        return userId;
    }

    @Override
    public TokenResponse refresh(String refreshToken) {
        Claims claims = parseJwt(refreshToken);
        if (claims == null) {
            throw new TokenException("A0231", "refreshToken 无效或已过期");
        }
        if (!"refresh".equals(claims.get("type"))) {
            throw new TokenException("A0231", "refreshToken 类型错误");
        }

        String jti = claims.getId();
        String userId = claims.get("userId", String.class);

        Boolean blacklisted = redisTemplate.hasKey(KEY_BLACKLIST + jti);
        if (Boolean.TRUE.equals(blacklisted)) {
            throw new TokenException("A0231", "refreshToken 已被撤销");
        }

        String storedUserId = (String) redisTemplate.opsForValue().get(KEY_REFRESH + jti);
        if (storedUserId == null) {
            throw new TokenException("A0231", "refreshToken 已过期");
        }

        long ttl = getRemainingTtl(KEY_REFRESH + jti);
        redisTemplate.opsForValue().set(KEY_BLACKLIST + jti, "1", ttl, TimeUnit.SECONDS);
        redisTemplate.delete(KEY_REFRESH + jti);

        return issue(userId);
    }

    @Override
    public void revoke(String accessToken) {
        Claims claims = parseJwt(accessToken);
        if (claims == null) return;

        String jti = claims.getId();
        String userId = claims.get("userId", String.class);

        long ttl = getRemainingTtl(KEY_SESSION + userId + ":" + jti);
        if (ttl > 0) {
            redisTemplate.opsForValue().set(KEY_BLACKLIST + jti, "1", ttl, TimeUnit.SECONDS);
        }
        redisTemplate.delete(KEY_SESSION + userId + ":" + jti);
    }

    @Override
    public void revokeAll(String userId) {
        String pattern = KEY_SESSION + userId + ":*";
        List<String> keysToDelete = new ArrayList<>();
        try (Cursor<byte[]> cursor = redisTemplate.getConnectionFactory().getConnection()
                .scan(ScanOptions.scanOptions().match(pattern).count(100).build())) {
            while (cursor.hasNext()) {
                String key = new String(cursor.next(), StandardCharsets.UTF_8);
                keysToDelete.add(key);
                String jti = key.substring(key.lastIndexOf(":") + 1);
                long ttl = getRemainingTtl(key);
                if (ttl > 0) {
                    redisTemplate.opsForValue().set(KEY_BLACKLIST + jti, "1", ttl, TimeUnit.SECONDS);
                }
            }
        } catch (Exception e) {
            log.error("SCAN 查找用户 {} session 失败", userId, e);
        }
        if (!keysToDelete.isEmpty()) {
            redisTemplate.delete(keysToDelete);
        }
    }

    // ========== 私有方法 ==========

    private String createJwt(String userId, String jti, long now, long exp, String type) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("jti", jti);
        claims.put("iat", now);
        claims.put("exp", exp);
        claims.put("type", type);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuer(ISSUER)
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    private Claims parseJwt(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("JWT 已过期");
            return null;
        } catch (SignatureException e) {
            log.warn("JWT 签名无效");
            return null;
        } catch (Exception e) {
            log.warn("JWT 解析异常: {}", e.getMessage());
            return null;
        }
    }

    private long getRemainingTtl(String key) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null ? ttl : 0;
    }

}
```

- [ ] **Step 3: 编译验证**

```bash
mvn clean install -f server/mall/pom.xml -pl mall-auth -am -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add server/mall/mall-auth/src/main/java/com/mall/auth/service/TokenService.java server/mall/mall-auth/src/main/java/com/mall/auth/service/impl/TokenServiceImpl.java
git commit -m "feat(mall-auth): add TokenService with JWT issue/verify/refresh/revoke"
```

---

### Task 8: mall-auth — CaptchaService 完整实现

**设计依据:** spec §5（完整设计）

**EasyCaptcha API:**
```java
SpecCaptcha captcha = new SpecCaptcha(130, 48, 4);  // 宽, 高, 字符数
captcha.text();        // 获取验证码文本（字符串）
captcha.toBase64();    // 返回 data:image/jpeg;base64,...
```

**文件:**
- Create: `server/mall/mall-auth/src/main/java/com/mall/auth/service/CaptchaService.java`
- Create: `server/mall/mall-auth/src/main/java/com/mall/auth/service/impl/CaptchaServiceImpl.java`

- [ ] **Step 1: 创建 CaptchaService.java 接口**

```java
package com.mall.auth.service;

import java.util.Map;

public interface CaptchaService {

    Map<String, String> generate();

    void verify(String captchaKey, String captchaCode, String clientIp);
}
```

- [ ] **Step 2: 创建 CaptchaServiceImpl.java**

```java
package com.mall.auth.service.impl;

import com.mall.common.exception.CaptchaException;
import com.mall.auth.service.CaptchaService;
import com.wf.captcha.SpecCaptcha;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CaptchaServiceImpl implements CaptchaService {

    private static final String KEY_CAPTCHA = "mall:auth:captcha:";
    private static final String KEY_IP = "mall:auth:captcha:ip:";

    private static final long CAPTCHA_TTL = 300; // 5 分钟
    private static final long IP_TTL = 86400;    // 24 小时
    private static final int IP_MAX_ATTEMPTS = 10;

    private final RedisTemplate<String, Object> redisTemplate;

    public CaptchaServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Map<String, String> generate() {
        SpecCaptcha captcha = new SpecCaptcha(130, 48, 4);
        String text = captcha.text().toLowerCase();

        // 重试直到生成不含歧义字符 0/O/1/I 的文本，避免用户混淆
        while (text.contains("0") || text.contains("o") || text.contains("1") || text.contains("i")) {
            captcha = new SpecCaptcha(130, 48, 4);
            text = captcha.text().toLowerCase();
        }
        String captchaKey = UUID.randomUUID().toString().replace("-", "");

        redisTemplate.opsForValue().set(
                KEY_CAPTCHA + captchaKey,
                text,
                CAPTCHA_TTL,
                TimeUnit.SECONDS);

        Map<String, String> result = new HashMap<>();
        result.put("captchaKey", captchaKey);
        result.put("captchaImage", captcha.toBase64());
        return result;
    }

    @Override
    public void verify(String captchaKey, String captchaCode, String clientIp) {
        // ① 参数空检查
        if (captchaKey == null || captchaCode == null || captchaKey.isEmpty() || captchaCode.isEmpty()) {
            throw new CaptchaException("A0401", "请完整填写信息");
        }

        // ② IP 防刷
        String ipKey = KEY_IP + clientIp;
        Integer ipCount = (Integer) redisTemplate.opsForValue().get(ipKey);
        if (ipCount != null && ipCount >= IP_MAX_ATTEMPTS) {
            throw new CaptchaException("A0241", "验证码尝试次数过多", "验证码尝试次数过多，请 24 小时后重试");
        }

        // ③ 验证码是否存在
        String redisKey = KEY_CAPTCHA + captchaKey;
        String storedCode = (String) redisTemplate.opsForValue().get(redisKey);
        if (storedCode == null) {
            incrementIpCount(ipKey);
            throw new CaptchaException("A0132", "验证码已过期", "验证码已过期，请重新获取");
        }

        // ④ 比对（忽略大小写）
        if (!storedCode.equalsIgnoreCase(captchaCode)) {
            incrementIpCount(ipKey);
            throw new CaptchaException("A0131", "验证码错误", "验证码错误，请重新输入");
        }

        // ⑤ 匹配 → 删除 key（一次性消费）
        redisTemplate.delete(redisKey);
    }

    private void incrementIpCount(String ipKey) {
        Long count = redisTemplate.opsForValue().increment(ipKey);
        if (count != null && count == 1) {
            redisTemplate.expire(ipKey, IP_TTL, TimeUnit.SECONDS);
        }
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
mvn clean install -f server/mall/pom.xml -pl mall-auth -am -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add server/mall/mall-auth/src/main/java/com/mall/auth/service/CaptchaService.java server/mall/mall-auth/src/main/java/com/mall/auth/service/impl/CaptchaServiceImpl.java
git commit -m "feat(mall-auth): add CaptchaService with EasyCaptcha generate/verify and IP rate limiting"
```

---

### Task 9: mall-auth — Request DTOs + CaptchaController 完整实现

**设计依据:** spec §6（CAPTCHA 端点详细流程）+ §3.2（包结构）

**CaptchaController 调用关系：**
- Customize `@Autowired RemoteUserService`（直接调用 Feign，不走 Adapter）
- `@Autowired CaptchaService`（生成/校验验证码）
- `@Autowired TokenService`（签发/撤销 Token）
- `BCryptPasswordEncoder`（密码哈希 + 校验）
- `org.apache.commons.codec.digest.DigestUtils`（SHA256 phoneHash）

**文件:**
- Create: `server/mall/mall-auth/src/main/java/com/mall/auth/DTO/request/CaptchaRegisterReq.java`
- Create: `server/mall/mall-auth/src/main/java/com/mall/auth/DTO/request/CaptchaLoginReq.java`
- Create: `server/mall/mall-auth/src/main/java/com/mall/auth/DTO/request/CaptchaResetPasswordReq.java`
- Create: `server/mall/mall-auth/src/main/java/com/mall/auth/DTO/request/CaptchaChangePhoneReq.java`
- Create: `server/mall/mall-auth/src/main/java/com/mall/auth/DTO/request/CaptchaDeactivateReq.java`
- Create: `server/mall/mall-auth/src/main/java/com/mall/auth/controller/CaptchaController.java`

- [ ] **Step 1: 创建 5 个 Request DTO**

CaptchaRegisterReq.java:

```java
package com.mall.auth.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CaptchaRegisterReq {
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码需 8~32 位")
    private String password;

    @NotBlank(message = "验证码 Key 不能为空")
    private String captchaKey;

    @NotBlank(message = "验证码不能为空")
    private String captchaCode;

    @NotNull(message = "请同意隐私协议")
    private Integer isPrivacyAgreed;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCaptchaKey() {
        return captchaKey;
    }

    public void setCaptchaKey(String captchaKey) {
        this.captchaKey = captchaKey;
    }

    public String getCaptchaCode() {
        return captchaCode;
    }

    public void setCaptchaCode(String captchaCode) {
        this.captchaCode = captchaCode;
    }

    public Integer getIsPrivacyAgreed() {
        return isPrivacyAgreed;
    }

    public void setIsPrivacyAgreed(Integer isPrivacyAgreed) {
        this.isPrivacyAgreed = isPrivacyAgreed;
    }
}
```

CaptchaLoginReq.java:

```java
package com.mall.auth.DTO.request;

import jakarta.validation.constraints.NotBlank;

public class CaptchaLoginReq {
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "验证码 Key 不能为空")
    private String captchaKey;

    @NotBlank(message = "验证码不能为空")
    private String captchaCode;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCaptchaKey() {
        return captchaKey;
    }

    public void setCaptchaKey(String captchaKey) {
        this.captchaKey = captchaKey;
    }

    public String getCaptchaCode() {
        return captchaCode;
    }

    public void setCaptchaCode(String captchaCode) {
        this.captchaCode = captchaCode;
    }
}
```

CaptchaResetPasswordReq.java:

```java
package com.mall.auth.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CaptchaResetPasswordReq {
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 32, message = "密码需 8~32 位")
    private String newPassword;

    @NotBlank(message = "验证码 Key 不能为空")
    private String captchaKey;

    @NotBlank(message = "验证码不能为空")
    private String captchaCode;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getCaptchaKey() {
        return captchaKey;
    }

    public void setCaptchaKey(String captchaKey) {
        this.captchaKey = captchaKey;
    }

    public String getCaptchaCode() {
        return captchaCode;
    }

    public void setCaptchaCode(String captchaCode) {
        this.captchaCode = captchaCode;
    }
}
```

CaptchaChangePhoneReq.java:

```java
package com.mall.auth.DTO.request;

import jakarta.validation.constraints.NotBlank;

public class CaptchaChangePhoneReq {
    @NotBlank(message = "原手机号不能为空")
    private String oldPhone;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "新手机号不能为空")
    private String newPhone;

    public String getOldPhone() {
        return oldPhone;
    }

    public void setOldPhone(String oldPhone) {
        this.oldPhone = oldPhone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNewPhone() {
        return newPhone;
    }

    public void setNewPhone(String newPhone) {
        this.newPhone = newPhone;
    }
}
```

CaptchaDeactivateReq.java:

```java
package com.mall.auth.DTO.request;

import jakarta.validation.constraints.NotBlank;

public class CaptchaDeactivateReq {
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "密码不能为空")
    private String password;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
```

- [ ] **Step 2: 创建 CaptchaController.java**

```java
package com.mall.auth.controller;

import com.mall.api.DTO.MallResult;
import com.mall.api.DTO.MallUserDTO;
import com.mall.api.feign.RemoteUserService;
import com.mall.auth.DTO.request.CaptchaChangePhoneReq;
import com.mall.auth.DTO.request.CaptchaDeactivateReq;
import com.mall.auth.DTO.request.CaptchaLoginReq;
import com.mall.auth.DTO.request.CaptchaRegisterReq;
import com.mall.auth.DTO.request.CaptchaResetPasswordReq;
import com.mall.auth.DTO.response.CaptchaResponse;
import com.mall.auth.DTO.response.TokenResponse;
import com.mall.auth.service.CaptchaService;
import com.mall.auth.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/captcha")
public class CaptchaController {

    private static final Logger log = LoggerFactory.getLogger(CaptchaController.class);

    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_LENGTH = 32;
    private static final int PWD_ERR_MAX = 5;
    private static final long PWD_ERR_LOCK_MINUTES = 30;
    private static final String KEY_PWD_ERR = "mall:auth:pwd_err:";

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private RemoteUserService remoteUserService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    /**
     * 6.1 获取验证码
     * GET /api/auth/captcha
     */
    @GetMapping
    public MallResult<CaptchaResponse> getCaptcha() {
        Map<String, String> captcha = captchaService.generate();
        CaptchaResponse resp = new CaptchaResponse(
                captcha.get("captchaKey"),
                captcha.get("captchaImage"));
        return MallResult.success(resp);
    }

    /**
     * 6.2 手机号+验证码注册
     * POST /api/auth/captcha/register
     */
    @PostMapping("/register")
    public MallResult<TokenResponse> register(@Valid @RequestBody CaptchaRegisterReq req,
                                              HttpServletRequest request) {
        String clientIp = getClientIp(request);

        // ① 校验验证码
        captchaService.verify(req.getCaptchaKey(), req.getCaptchaCode(), clientIp);

        // ② 隐私协议检查（@NotNull 已确保非 null）
        if (req.getIsPrivacyAgreed() != 1) {
            return MallResult.error("A0101", "请同意隐私协议", "请同意隐私协议");
        }

        // ③ 密码复杂度（@Size 已确保长度，此方法仅检查字母+数字）
        String pwdError = validatePassword(req.getPassword());
        if (pwdError != null) {
            return MallResult.error("A0121", pwdError, pwdError);
        }

        // ④ 手机号是否已注册
        MallUserDTO existing = remoteUserService.findByPhone(req.getPhone());
        if (existing != null) {
            return MallResult.error("A0151", "手机号已被注册", "手机号已被注册");
        }

        // ⑤ BCrypt 哈希密码
        String passwordHash = passwordEncoder.encode(req.getPassword());

        // ⑥ SHA256 phoneHash
        String phoneHash = DigestUtils.sha256Hex(req.getPhone());

        // ⑦ 注册
        RemoteUserService.RegisterRequest registerReq = new RemoteUserService.RegisterRequest();
        registerReq.setPhone(req.getPhone());
        registerReq.setPhoneHash(phoneHash);
        registerReq.setPassword(passwordHash);
        registerReq.setRegisterType("phone");
        String userId = remoteUserService.register(registerReq);

        // ⑧ 签发 token
        TokenResponse token = tokenService.issue(userId);
        return MallResult.success(token);
    }

    /**
     * 6.3 手机号+密码登录（带验证码）
     * POST /api/auth/captcha/login
     */
    @PostMapping("/login")
    public MallResult<TokenResponse> login(@Valid @RequestBody CaptchaLoginReq req,
                                           HttpServletRequest request) {
        String clientIp = getClientIp(request);

        // ① 校验验证码
        captchaService.verify(req.getCaptchaKey(), req.getCaptchaCode(), clientIp);

        // ② 查用户
        MallUserDTO user = remoteUserService.findByPhone(req.getPhone());
        if (user == null) {
            return MallResult.error("A0201", "账户不存在", "该手机号未注册，请先注册");
        }

        // ③ 状态检查
        if ("1".equals(user.getUserStatus())) {
            return MallResult.error("A0202", "账户已被冻结", "账户已被冻结，请联系客服");
        }
        if ("2".equals(user.getUserStatus())) {
            return MallResult.error("A0203", "账户已注销", "账户已注销");
        }

        // ④ 错误次数检查
        String pwdErrKey = KEY_PWD_ERR + user.getId();
        Integer errCount = (Integer) redisTemplate.opsForValue().get(pwdErrKey);
        if (errCount != null && errCount >= PWD_ERR_MAX) {
            return MallResult.error("A0211", "密码错误次数过多，请 30 分钟后重试", "密码错误次数过多，请 30 分钟后重试");
        }

        // ⑤ 密码校验
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            errCount = incrementAndGet(pwdErrKey);
            if (errCount >= PWD_ERR_MAX) {
                log.warn("用户 {} 登录密码错误超限，锁定 30 分钟", user.getId());
            }
            return MallResult.error("A0210", "密码错误", "密码错误，还可重试 " + (PWD_ERR_MAX - errCount) + " 次");
        }

        // ⑥ 成功 → 清错误计数
        redisTemplate.delete(pwdErrKey);

        // ⑦ 签发 token
        TokenResponse token = tokenService.issue(user.getId());
        return MallResult.success(token);
    }

    /**
     * 6.4 验证码重置密码
     * POST /api/auth/captcha/password/reset
     */
    @PostMapping("/password/reset")
    public MallResult<Void> resetPassword(@Valid @RequestBody CaptchaResetPasswordReq req,
                                          HttpServletRequest request) {
        String clientIp = getClientIp(request);

        // ① 校验验证码
        captchaService.verify(req.getCaptchaKey(), req.getCaptchaCode(), clientIp);

        // ② 查用户
        MallUserDTO user = remoteUserService.findByPhone(req.getPhone());
        if (user == null) {
            return MallResult.error("A0201", "账户不存在", "该手机号未注册");
        }

        // ③ 密码校验
        String pwdError = validatePassword(req.getNewPassword());
        if (pwdError != null) {
            return MallResult.error("A0121", pwdError, pwdError);
        }

        // ④ BCrypt 哈希
        String passwordHash = passwordEncoder.encode(req.getNewPassword());

        // ⑤ 更新密码
        RemoteUserService.PasswordUpdateRequest updateReq =
                new RemoteUserService.PasswordUpdateRequest();
        updateReq.setNewPassword(passwordHash);
        remoteUserService.updatePassword(user.getId(), updateReq);

        // ⑥ 撤销所有 token
        tokenService.revokeAll(user.getId());

        return MallResult.success(null);
    }

    /**
     * 6.5 换绑手机号
     * PUT /api/auth/captcha/phone
     */
    @PutMapping("/phone")
    public MallResult<Void> changePhone(@Valid @RequestBody CaptchaChangePhoneReq req) {
        // ① 查旧手机号用户
        MallUserDTO oldUser = remoteUserService.findByPhone(req.getOldPhone());
        if (oldUser == null) {
            return MallResult.error("A0201", "原手机号未注册");
        }

        // ② 校验密码
        if (!passwordEncoder.matches(req.getPassword(), oldUser.getPassword())) {
            return MallResult.error("A0210", "密码错误", "密码错误");
        }

        // ③ 查新手机号是否已被注册
        MallUserDTO newUser = remoteUserService.findByPhone(req.getNewPhone());
        if (newUser != null) {
            return MallResult.error("A0151", "新手机号已被注册", "新手机号已被注册");
        }

        // ④ SHA256 newPhoneHash
        String newPhoneHash = DigestUtils.sha256Hex(req.getNewPhone());

        // ⑤ 更新
        RemoteUserService.PhoneUpdateRequest updateReq =
                new RemoteUserService.PhoneUpdateRequest();
        updateReq.setNewPhone(req.getNewPhone());
        updateReq.setNewPhoneHash(newPhoneHash);
        remoteUserService.updatePhone(oldUser.getId(), updateReq);

        return MallResult.success(null);
    }

    /**
     * 6.6 注销账号
     * DELETE /api/auth/captcha/account
     */
    @DeleteMapping("/account")
    public MallResult<Void> deactivateAccount(@Valid @RequestBody CaptchaDeactivateReq req) {
        // ① 查用户
        MallUserDTO user = remoteUserService.findByPhone(req.getPhone());
        if (user == null) {
            return MallResult.error("A0201", "账户不存在");
        }

        // ② 校验密码
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            return MallResult.error("A0210", "密码错误", "密码错误");
        }

        // ③ 注销
        remoteUserService.deactivateAccount(user.getId());

        // ④ 撤销所有 token
        tokenService.revokeAll(user.getId());

        return MallResult.success(null);
    }

    // ========== 私有方法 ==========

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }

    private String validatePassword(String password) {
        if (password == null || password.length() < PASSWORD_MIN_LENGTH
                || password.length() > PASSWORD_MAX_LENGTH) {
            return "密码需 " + PASSWORD_MIN_LENGTH + "~" + PASSWORD_MAX_LENGTH + " 位";
        }
        if (!password.matches(".*[a-zA-Z].*") || !password.matches(".*[0-9].*")) {
            return "密码需包含字母和数字";
        }
        return null;
    }

    private int incrementAndGet(String key) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, PWD_ERR_LOCK_MINUTES, TimeUnit.MINUTES);
        }
        return count != null ? count.intValue() : 0;
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
mvn clean install -f server/mall/pom.xml -pl mall-auth -am -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add server/mall/mall-auth/src/main/java/com/mall/auth/DTO/request server/mall/mall-auth/src/main/java/com/mall/auth/controller/CaptchaController.java
git commit -m "feat(mall-auth): add CaptchaController with 6 CAPTCHA endpoints and request DTOs"
```

---

### Task 10: mall-auth — AuthController 骨架 + 剩余接口 + RemoteUserAdapter 骨架

**设计依据:** spec §3.2（包结构）+ §7（AuthController 骨架）

**文件:**
- Create: `server/mall/mall-auth/src/main/java/com/mall/auth/controller/AuthController.java`
- Create: `server/mall/mall-auth/src/main/java/com/mall/auth/service/AuthService.java`
- Create: `server/mall/mall-auth/src/main/java/com/mall/auth/service/DecryptService.java`
- Create: `server/mall/mall-auth/src/main/java/com/mall/auth/service/SmsService.java`
- Create: `server/mall/mall-auth/src/main/java/com/mall/auth/infrastructure/feign/RemoteUserAdapter.java`

- [ ] **Step 1: 创建 AuthController.java（13 端点占位）**

```java
package com.mall.auth.controller;

import com.mall.api.DTO.MallResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String NOT_OPEN = "该功能暂未开放，请使用 CAPTCHA 端点";

    @PostMapping("/users")
    public MallResult<Void> register() {
        return MallResult.error("A9999", NOT_OPEN);
    }

    @PostMapping("/sms_codes")
    public MallResult<Void> sendSms() {
        return MallResult.error("A9999", NOT_OPEN);
    }

    @PostMapping("/sessions")
    public MallResult<Void> loginByPassword() {
        return MallResult.error("A9999", NOT_OPEN);
    }

    @PostMapping("/sessions/sms")
    public MallResult<Void> loginBySms() {
        return MallResult.error("A9999", NOT_OPEN);
    }

    @PostMapping("/sessions/refresh")
    public MallResult<Void> refresh() {
        return MallResult.error("A9999", NOT_OPEN);
    }

    @GetMapping("/sessions/current")
    public MallResult<Void> getCurrentSession() {
        return MallResult.error("A9999", NOT_OPEN);
    }

    @DeleteMapping("/sessions/current")
    public MallResult<Void> logout() {
        return MallResult.error("A9999", NOT_OPEN);
    }

    @PostMapping("/wechat/sessions")
    public MallResult<Void> wechatLogin() {
        return MallResult.error("A9999", NOT_OPEN);
    }

    @PostMapping("/wechat/phone_binding")
    public MallResult<Void> bindWechatPhone() {
        return MallResult.error("A9999", NOT_OPEN);
    }

    @PutMapping("/phone")
    public MallResult<Void> changePhone() {
        return MallResult.error("A9999", NOT_OPEN);
    }

    @PutMapping("/password/reset")
    public MallResult<Void> resetPassword() {
        return MallResult.error("A9999", NOT_OPEN);
    }

    @PutMapping("/password")
    public MallResult<Void> changePassword() {
        return MallResult.error("A9999", NOT_OPEN);
    }

    @DeleteMapping("/account")
    public MallResult<Void> deactivate() {
        return MallResult.error("A9999", NOT_OPEN);
    }
}
```

- [ ] **Step 2: 创建 AuthService.java 接口（仅签名）**

```java
package com.mall.auth.service;

public interface AuthService {
    String register(String phone, String password);
    String loginByPassword(String phone, String password);
    String loginBySms(String phone, String smsCode);
    TokenService.TokenPair refresh(String refreshToken);
    void logout(String accessToken);
    void resetPassword(String phone, String newPassword);
    void changePassword(String userId, String oldPassword, String newPassword);
}
```

这里 `TokenService.TokenPair` 未定义。由于 AuthService 是占位接口，简化不引入 TokenPair 类型，改为直接返回 String 占位：

```java
package com.mall.auth.service;

public interface AuthService {
    String register(String phone, String password);
    String loginByPassword(String phone, String password);
    String loginBySms(String phone, String smsCode);
    String refresh(String refreshToken);
    void logout(String accessToken);
    void resetPassword(String phone, String newPassword);
    void changePassword(String userId, String oldPassword, String newPassword);
}
```

- [ ] **Step 3: 创建 DecryptService.java 接口**

```java
package com.mall.auth.service;

public interface DecryptService {
    String decrypt(String encryptedData);
}
```

- [ ] **Step 4: 创建 SmsService.java 接口**

```java
package com.mall.auth.service;

public interface SmsService {
    void send(String phone, String code);
}
```

- [ ] **Step 5: 创建 RemoteUserAdapter.java 骨架**

```java
package com.mall.auth.infrastructure.feign;

import com.mall.api.DTO.MallUserDTO;
import com.mall.api.feign.RemoteUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * RemoteUserService Feign 调用封装
 * MVP 阶段不被调用，CaptchaController 直接 @Autowired RemoteUserService
 * 后续 AuthServiceImpl 使用此 Adapter 统一错误处理
 */
@Component
public class RemoteUserAdapter {

    private static final Logger log = LoggerFactory.getLogger(RemoteUserAdapter.class);

    @Autowired
    private RemoteUserService remoteUserService;

    public MallUserDTO findByPhone(String phone) {
        try {
            return remoteUserService.findByPhone(phone);
        } catch (Exception e) {
            log.error("Feign 调用 findByPhone 失败: {}", e.getMessage());
            return null;
        }
    }

    public String register(RemoteUserService.RegisterRequest request) {
        try {
            return remoteUserService.register(request);
        } catch (Exception e) {
            log.error("Feign 调用 register 失败: {}", e.getMessage());
            throw new RuntimeException("注册失败，请稍后重试", e);
        }
    }

    public void updatePassword(String userId, RemoteUserService.PasswordUpdateRequest request) {
        try {
            remoteUserService.updatePassword(userId, request);
        } catch (Exception e) {
            log.error("Feign 调用 updatePassword 失败: {}", e.getMessage());
            throw new RuntimeException("修改密码失败，请稍后重试", e);
        }
    }

    public void updatePhone(String userId, RemoteUserService.PhoneUpdateRequest request) {
        try {
            remoteUserService.updatePhone(userId, request);
        } catch (Exception e) {
            log.error("Feign 调用 updatePhone 失败: {}", e.getMessage());
            throw new RuntimeException("修改手机号失败，请稍后重试", e);
        }
    }

    public void deactivateAccount(String userId) {
        try {
            remoteUserService.deactivateAccount(userId);
        } catch (Exception e) {
            log.error("Feign 调用 deactivateAccount 失败: {}", e.getMessage());
            throw new RuntimeException("注销失败，请稍后重试", e);
        }
    }
}
```

- [ ] **Step 6: 编译验证**

```bash
mvn clean install -f server/mall/pom.xml -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add server/mall/mall-auth/src/main/java/com/mall/auth/controller/AuthController.java server/mall/mall-auth/src/main/java/com/mall/auth/service/AuthService.java server/mall/mall-auth/src/main/java/com/mall/auth/service/DecryptService.java server/mall/mall-auth/src/main/java/com/mall/auth/service/SmsService.java server/mall/mall-auth/src/main/java/com/mall/auth/infrastructure/feign/RemoteUserAdapter.java
git commit -m "feat(mall-auth): add AuthController stub, AuthService/DecryptService/SmsService interfaces, RemoteUserAdapter skeleton"
```

---

## 自检

### 1. Spec 覆盖检查

| Spec 章节 | 实现任务 | 状态 |
|-----------|---------|------|
| `06_mall-common公共模块设计.md` §3 MallExceptionHandler | Task 1 | ✅ |
| §1 MVP 范围（CaptchaController 完整） | Task 9 | ✅ |
| §1 MVP 范围（CaptchaService 完整） | Task 8 | ✅ |
| §1 MVP 范围（TokenService 完整） | Task 7 | ✅ |
| §1 MVP 范围（AuthController 骨架） | Task 10 | ✅ |
| §1 MVP 范围（AuthService 接口） | Task 10 | ✅ |
| §1 MVP 范围（DecryptService/SmsService 接口） | Task 10 | ✅ |
| §1 MVP 范围（RemoteUserService Feign） | Task 2 | ✅ |
| §1 MVP 范围（MallUserDTO） | Task 2 | ✅ |
| §1 MVP 范围（MallResult） | Task 2 | ✅ |
| §1 MVP 范围（RemoteUserInnerController） | Task 5 | ✅ |
| §1 MVP 范围（mall-user Mapper/Service 扩展） | Task 4 | ✅ |
| §1 MVP 范围（@EnableFeignClients 修复） | Task 3 | ✅ |
| §3.2 包结构（mall-auth） | Task 6-10 | ✅ |
| §3.3 包结构（mall-user） | Task 4-5 | ✅ |
| §4 TokenService（issue/verify/refresh/revoke/revokeAll） | Task 7 | ✅ |
| §4.4 Redis Key 命名 | Task 7 | ✅ |
| §5 CaptchaService（generate/verify） | Task 8 | ✅ |
| §5.3 校验流程（5 步验证） | Task 8 | ✅ |
| §6.1 GET /api/auth/captcha | Task 9 | ✅ |
| §6.2 POST /api/auth/captcha/register | Task 9 | ✅ |
| §6.3 POST /api/auth/captcha/login | Task 9 | ✅ |
| §6.4 POST /api/auth/captcha/password/reset | Task 9 | ✅ |
| §6.5 PUT /api/auth/captcha/phone | Task 9 | ✅ |
| §6.6 DELETE /api/auth/captcha/account | Task 9 | ✅ |
| §7 AuthController 13 端点占位 | Task 10 | ✅ |
| §8 mall-api 契约 | Task 2 | ✅ |
| §9.1 Mapper 追加 | Task 4 | ✅ |
| §9.2 Service 追加 | Task 4 | ✅ |
| §9.3 RemoteUserInnerController | Task 5 | ✅ |
| §10.1 pom.xml 新增依赖 | Task 3 | ✅ |
| §10.2 @EnableFeignClients 修复 | Task 3 | ✅ |
| §11 错误码（CaptchaController 全部使用） | Task 9 | ✅ |
| `@Validated` 参数校验（DTO + Controller + ExceptionHandler） | Task 6/9/1 | ✅ |

### 2. 占位符检查

无 TBD/TODO/placeholder 残留。所有代码块包含完整实现。

### 3. 类型一致性检查

- `userId`: String 类型（Task 2 MallUserDTO → Task 7 TokenService → Task 9 CaptchaController），一致 ✅
- `MallResult<T>`: 所有 Controller 返回类型统一 ✅
- `RemoteUserService.RegisterRequest`: Task 2 定义内部类 → Task 5 引用 → Task 9 构造使用，一致 ✅
- `CaptchaException`: Task 6（mall-common）定义 → Task 8 CaptchaServiceImpl 抛出 → Task 1 MallExceptionHandler 捕获转 MallResult ✅
- `TokenException`: Task 6（mall-common）定义 → Task 7 TokenServiceImpl 抛出 → Task 1 MallExceptionHandler 捕获转 MallResult 带 A0231 ✅
- `CaptchaResponse`: Task 6 定义 → Task 9 CaptchaController 返回，一致 ✅
- `TokenResponse`: Task 6 定义 → Task 7 TokenService 返回 → Task 9 CaptchaController 返回，一致 ✅
- `BCryptPasswordEncoder`: Task 9 直接 new，12 rounds，一致 ✅
- `DigestUtils.sha256Hex`: `org.apache.commons.codec.digest.DigestUtils`，Task 4 Service 和 Task 9 Controller 均使用，一致 ✅
- `@Valid` 参数校验：DTO 使用 `@NotBlank/@Size/@NotNull`（Task 6）→ Controller 使用 `@Valid @RequestBody`（Task 9）→ MallExceptionHandler 处理 `MethodArgumentNotValidException`（Task 1），链路完整 ✅
- `MallUserDTO.password`: Task 2 `@JsonProperty(WRITE_ONLY)` + Task 5 `toDTO()` 置空，防泄露 ✅

### 4. 全局异常处理器

MallExceptionHandler 位于 Task 1（mall-common），`@Order(HIGHEST_PRECEDENCE)` 优先于若依 GlobalExceptionHandler。覆盖：
- `CaptchaException` → 精确匹配，返回带错误码的 `MallResult`
- `TokenException` → 精确匹配，返回 A0231 `MallResult`
- `FeignException` → 友好提示"服务暂时不可用"
- 参数校验异常 → A0401
- 兜底 Exception → `B0001`
不再需要各 Controller 单独 try-catch。

---

## 执行交接

计划已保存到 `docs/superpowers/plans/2026-05-23-mall-auth-mvp-implementation.md`

**两种执行方式：**

1. **Subagent-Driven（推荐）** — 我派发一个子 agent 执行每个 Task，task 之间 review 检查点，快速迭代

2. **Inline Execution** — 在当前 session 中逐个 Task 执行，批处理 + 检查点

**选哪种？**
