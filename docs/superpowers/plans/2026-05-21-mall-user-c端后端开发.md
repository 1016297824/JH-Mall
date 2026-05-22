# mall-user C 端后端开发实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 mall-user 模块添加 12 个 C 端 API，包括个人资料、地址管理、会员等级、积分、成长值功能

**Architecture:** 新增 `controller/api/` 包存放 C 端 Controller，现有管理端 Controller 迁移到 `controller/admin/`（`@RequestMapping` 不变）。管理端路由由 `discovery.locator` 自动生成（`/{serviceId}/{controllerPath}`），C 端走显式 `/api/**` 路由。C 端业务逻辑在 `service/{user,address,member,points,growth}/` 子包中独立实现。跨服务调用通过 mall-api Feign 接口解耦。

## CEO Review 修复摘要

> 以下为 plan 审查（CEO Review, HOLD SCOPE 模式）发现的 7 项问题的修复清单。实施者在执行 plan 时需同步处理。

| # | 类型 | 修复内容 | 涉及位置 |
|---|------|---------|---------|
| 1 | 🟢 已更新 | Task 4 网关路由：去掉管理端显式路由（改为 discovery.locator），仅保留 C 端 `/api/user/**` | 已更新 |
| 2 | 🟢 已更新 | SecurityUtils.getUserId() 不可用，C 端 Controller 改读 `X-User-Id` 请求头（MallAuthFilter 注入） | Task 9 所有 Controller |
| 3 | 🔴 逻辑 bug | UserConvert 中 member.getLevelId() 应查等级名而非写 ID | Task 7 Step 9（已修复） |
| 4 | 🟡 设计缺陷 | Service 层抛 ErrorCode 枚举 + ServiceException 替代 RuntimeException | Task 8 所有 Service |
| 5 | 🟢 编译警告 | PointsController 删除 PageParam 无效 import | Task 9 Step 4 |
| 6 | 🟡 校验漏掉 | AddressController.create/update 补充 @Valid 注解 | Task 9 Step 2 |
| 7 | 🟡 规范问题 | register() 中 userStatus 使用 UserStatusEnum 常量 | Task 8 Step 2 |

**Tech Stack:** Spring Boot 4.0 / Spring Cloud 2025 / MyBatis / MySQL / Redis / Feign

---

## 文件结构总览

### mall-api 新增文件（8 个）

| 文件 | 职责 |
|------|------|
| `com/mall/api/enums/ErrorCode.java` | A0501/A0511 等错误码枚举 |
| `com/mall/api/enums/UserStatusEnum.java` | NORMAL/FROZEN/DELETED |
| `com/mall/api/enums/PointsChangeTypeEnum.java` | EARN/CONSUME/EXPIRE/ADMIN_ADJUST |
| `com/mall/api/enums/GrowthChangeTypeEnum.java` | EARN/CONSUME/ADMIN_ADJUST |
| `com/mall/api/dto/user/UserDTO.java` | Feign 传输用户数据 |
| `com/mall/api/dto/user/AddressDTO.java` | Feign 传输地址数据 |
| `com/mall/api/feign/RemoteUserService.java` | mall-user Feign 接口定义 |
| `com/mall/api/feign/RemoteAuthService.java` | mall-auth Feign 接口定义 |

### mall-user 新增/修改文件

**管理端 Controller 搬迁到 controller/admin/ 包（7 文件，仅改包路径）**

| 原位置 | 新位置 |
|--------|--------|
| `controller/MallUserController.java` | `controller/admin/MallUserController.java` |
| `controller/MallUserAddressController.java` | `controller/admin/MallUserAddressController.java` |
| `controller/MallUserMemberController.java` | `controller/admin/MallUserMemberController.java` |
| `controller/MallUserMemberLevelController.java` | `controller/admin/MallUserMemberLevelController.java` |
| `controller/MallUserPointsAccountController.java` | `controller/admin/MallUserPointsAccountController.java` |
| `controller/MallUserPointsLogController.java` | `controller/admin/MallUserPointsLogController.java` |
| `controller/MallUserGrowthLogController.java` | `controller/admin/MallUserGrowthLogController.java` |

**新增 VO 对象（5 个）**

| 文件 | 职责 |
|------|------|
| `com/mall/user/vo/UserProfileVO.java` | 个人资料（脱敏后） |
| `com/mall/user/vo/AddressVO.java` | 地址展示 |
| `com/mall/user/vo/MembershipVO.java` | 会员信息+进度 |
| `com/mall/user/vo/PointsRecordVO.java` | 积分流水 |
| `com/mall/user/vo/GrowthRecordVO.java` | 成长值流水 |

**新增 Request DTO（3 个）**

| 文件 | 职责 |
|------|------|
| `com/mall/user/dto/request/UpdateProfileReq.java` | 修改资料请求 |
| `com/mall/user/dto/request/CreateAddressReq.java` | 新增地址请求 |
| `com/mall/user/dto/request/UpdateAddressReq.java` | 修改地址请求 |

**新增 Convert 转换器（5 个）**

| 文件 | 职责 |
|------|------|
| `com/mall/user/convert/UserConvert.java` | MallUser→UserProfileVO |
| `com/mall/user/convert/AddressConvert.java` | MallUserAddress→AddressVO（脱敏手机号） |
| `com/mall/user/convert/MemberConvert.java` | 会员信息→MembershipVO（含进度计算） |
| `com/mall/user/convert/PointsConvert.java` | MallUserPointsLog→PointsRecordVO |
| `com/mall/user/convert/GrowthConvert.java` | MallUserGrowthLog→GrowthRecordVO |

**新增 C 端 Controller（5 个）**

| 文件 | 职责 | API 路径 |
|------|------|---------|
| `com/mall/user/controller/api/ProfileController.java` | 个人资料 | `/api/user/profile` |
| `com/mall/user/controller/api/AddressController.java` | 地址管理 | `/api/user/addresses` |
| `com/mall/user/controller/api/MembershipController.java` | 会员信息 | `/api/user/membership` |
| `com/mall/user/controller/api/PointsController.java` | 积分查询 | `/api/user/points` |
| `com/mall/user/controller/api/GrowthController.java` | 成长值查询 | `/api/user/growth` |

**新增 C 端 Service（5 接口 + 5 实现）**

| 接口 | 实现 | 子域 |
|------|------|------|
| `service/user/UserService` | `service/user/impl/UserServiceImpl` | 用户账户 |
| `service/address/AddressService` | `service/address/impl/AddressServiceImpl` | 地址簿 |
| `service/member/MemberService` | `service/member/impl/MemberServiceImpl` | 会员等级 |
| `service/points/PointsService` | `service/points/impl/PointsServiceImpl` | 积分 |
| `service/growth/GrowthService` | `service/growth/impl/GrowthServiceImpl` | 成长值 |

**Mapper XML 追加 SQL（6 文件追加）**

| 文件 | 追加内容 |
|------|---------|
| `mapper/mall-user/MallUserMapper.xml` | selectByPhoneHash, updateUserStatus, updatePassword, updatePhone, selectByIdInternal |
| `mapper/mall-user/MallUserAddressMapper.xml` | selectByUserId, selectCountByUserId, clearDefaultByUserId, selectOldestByUserId, logicDeleteById |
| `mapper/mall-user/MallUserMemberMapper.xml` | selectByUserId, updateGrowth, updateLevel |
| `mapper/mall-user/MallUserPointsAccountMapper.xml` | selectByUserId, updateAvailablePoints |
| `mapper/mall-user/MallUserPointsLogMapper.xml` | selectListByUserId |
| `mapper/mall-user/MallUserGrowthLogMapper.xml` | selectListByUserId |

---

### Task 1: mall-api 共享枚举

**Files:**
- Create: `server/mall/mall-api/src/main/java/com/mall/api/enums/UserStatusEnum.java`
- Create: `server/mall/mall-api/src/main/java/com/mall/api/enums/PointsChangeTypeEnum.java`
- Create: `server/mall/mall-api/src/main/java/com/mall/api/enums/GrowthChangeTypeEnum.java`

- [ ] **Step 1: Create UserStatusEnum.java**

```java
package com.mall.api.enums;

public enum UserStatusEnum {

    NORMAL(0, "正常"),
    FROZEN(1, "冻结"),
    DELETED(2, "注销");

    private final int code;
    private final String desc;

    UserStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static UserStatusEnum fromCode(int code) {
        for (UserStatusEnum e : values()) {
            if (e.code == code) return e;
        }
        return NORMAL;
    }
}
```

- [ ] **Step 2: Create PointsChangeTypeEnum.java**

```java
package com.mall.api.enums;

public enum PointsChangeTypeEnum {

    EARN(1, "获取"),
    CONSUME(2, "消耗"),
    EXPIRE(3, "过期"),
    ADMIN_ADJUST(4, "管理员调整");

    private final int code;
    private final String desc;

    PointsChangeTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static PointsChangeTypeEnum fromCode(int code) {
        for (PointsChangeTypeEnum e : values()) {
            if (e.code == code) return e;
        }
        return EARN;
    }
}
```

- [ ] **Step 3: Create GrowthChangeTypeEnum.java**

```java
package com.mall.api.enums;

public enum GrowthChangeTypeEnum {

    EARN(1, "获取"),
    CONSUME(2, "消耗"),
    ADMIN_ADJUST(3, "管理员调整");

    private final int code;
    private final String desc;

    GrowthChangeTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static GrowthChangeTypeEnum fromCode(int code) {
        for (GrowthChangeTypeEnum e : values()) {
            if (e.code == code) return e;
        }
        return EARN;
    }
}
```

- [ ] **Step 4: Create ErrorCode.java**

```java
package com.mall.api.enums;

/**
 * 业务错误码枚举，遵循阿里巴巴 A/B/C 错误码规约。
 * 配合 ServiceException（ruoyi-common-core 内置）抛出，
 * 由 GlobalExceptionHandler（ruoyi-common-security 内置）转换为 AjaxResult。
 */
public enum ErrorCode {

    SUCCESS("00000", 200, "操作成功"),
    UNAUTHORIZED("A0301", 401, "请先登录"),
    PARAM_MISSING("A0401", 400, "请完整填写信息"),
    PARAM_INVALID("A0402", 400, "参数格式错误"),
    RESOURCE_NOT_FOUND("A0501", 404, "资源不存在"),
    ADDRESS_LIMIT("A0511", 400, "地址数量已达上限");

    private final String code;
    private final int httpStatus;
    private final String userTip;

    ErrorCode(String code, int httpStatus, String userTip) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.userTip = userTip;
    }

    public String getCode() { return code; }
    public int getHttpStatus() { return httpStatus; }
    public String getUserTip() { return userTip; }
}
```

- [ ] **Step 5: 提交**

```bash
git add server/mall/mall-api/src/main/java/com/mall/api/enums/
git commit -m "feat(mall-api): add shared enums including ErrorCode for user/points/growth"
```

---

### Task 2: mall-api 共享 DTO

**Files:**
- Create: `server/mall/mall-api/src/main/java/com/mall/api/dto/user/UserDTO.java`
- Create: `server/mall/mall-api/src/main/java/com/mall/api/dto/user/AddressDTO.java`

- [ ] **Step 1: Create UserDTO.java**

```java
package com.mall.api.dto.user;

public class UserDTO {

    private Long userId;
    private String phoneHash;
    private String passwordHash;
    private Integer userStatus;
    private String nickname;
    private String avatar;
    private String wechatOpenid;
    private String wechatUnionid;

    public UserDTO() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getPhoneHash() { return phoneHash; }
    public void setPhoneHash(String phoneHash) { this.phoneHash = phoneHash; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Integer getUserStatus() { return userStatus; }
    public void setUserStatus(Integer userStatus) { this.userStatus = userStatus; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getWechatOpenid() { return wechatOpenid; }
    public void setWechatOpenid(String wechatOpenid) { this.wechatOpenid = wechatOpenid; }

    public String getWechatUnionid() { return wechatUnionid; }
    public void setWechatUnionid(String wechatUnionid) { this.wechatUnionid = wechatUnionid; }
}
```

- [ ] **Step 2: Create AddressDTO.java**

```java
package com.mall.api.dto.user;

public class AddressDTO {

    private Long addressId;
    private Long userId;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private Integer isDefault;

    public AddressDTO() {}

    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getDetailAddress() { return detailAddress; }
    public void setDetailAddress(String detailAddress) { this.detailAddress = detailAddress; }

    public Integer getIsDefault() { return isDefault; }
    public void setIsDefault(Integer isDefault) { this.isDefault = isDefault; }
}
```

- [ ] **Step 3: 提交**

```bash
git add server/mall/mall-api/src/main/java/com/mall/api/dto/user/
git commit -m "feat(mall-api): add UserDTO and AddressDTO"
```

---

### Task 3: mall-api Feign 接口

**Files:**
- Create: `server/mall/mall-api/src/main/java/com/mall/api/feign/RemoteUserService.java`
- Create: `server/mall/mall-api/src/main/java/com/mall/api/feign/RemoteAuthService.java`

- [ ] **Step 1: Create RemoteUserService.java**

```java
package com.mall.api.feign;

import com.mall.api.dto.user.AddressDTO;
import com.mall.api.dto.user.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(contextId = "remoteUserService", value = "mall-user", path = "/api/user")
public interface RemoteUserService {

    @PostMapping("/internal/register")
    Long register(@RequestParam("phoneEncrypted") String phoneEncrypted,
                  @RequestParam("phoneHash") String phoneHash,
                  @RequestParam("passwordHash") String passwordHash);

    @GetMapping("/internal/phone/{phoneHash}")
    UserDTO findByPhone(@PathVariable("phoneHash") String phoneHash);

    @GetMapping("/internal/wechat/{openId}")
    UserDTO findByWechatOpenId(@PathVariable("openId") String openId);

    @PostMapping("/internal/wechat/register")
    Long registerByWechat(@RequestParam("openId") String openId,
                          @RequestParam("unionId") String unionId);

    @PutMapping("/internal/{userId}/password")
    void updatePassword(@PathVariable("userId") Long userId,
                        @RequestParam("newPasswordHash") String newPasswordHash);

    @PutMapping("/internal/{userId}/phone")
    void updatePhone(@PathVariable("userId") Long userId,
                     @RequestParam("phoneEncrypted") String phoneEncrypted,
                     @RequestParam("phoneHash") String phoneHash);

    @PostMapping("/internal/{userId}/deactivate")
    void deactivateAccount(@PathVariable("userId") Long userId);

    @GetMapping("/internal/address/validate")
    Boolean validateAddress(@RequestParam("userId") Long userId,
                            @RequestParam("addressId") Long addressId);

    @GetMapping("/internal/{userId}")
    UserDTO findById(@PathVariable("userId") Long userId);
}
```

- [ ] **Step 2: Create RemoteAuthService.java**

```java
package com.mall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(contextId = "remoteAuthService", value = "mall-auth", path = "/api/auth")
public interface RemoteAuthService {

    @PostMapping("/internal/decrypt")
    String decrypt(@RequestParam("encryptedData") String encryptedData);

    @PostMapping("/internal/decrypt/batch")
    List<String> batchDecrypt(@RequestBody List<String> encryptedDataList);
}
```

- [ ] **Step 3: 提交**

```bash
git add server/mall/mall-api/src/main/java/com/mall/api/feign/
git commit -m "feat(mall-api): add RemoteUserService and RemoteAuthService Feign interfaces"
```

---

### Task 4: 网关路由配置

需要更新 Nacos 中的 `ruoyi-gateway-dev.yml`，新增 mall-user 模块的 C 端路由（管理端由 `discovery.locator` 自动路由 `/{serviceId}/**`，无需显式配置）。

**操作方式：** 登录 Nacos 管理台（`http://localhost:8848/nacos`）→ 配置管理 → 编辑 `ruoyi-gateway-dev.yml`，在 `spring.cloud.gateway.server.webflux.routes` 下追加 C 端路由：

```yaml
# 商城-用户模块（C 端）
- id: mall-user-api
  uri: lb://mall-user
  predicates:
    - Path=/api/user/**
  filters:
    - StripPrefix=0
```

同时在新版 `ruoyi-gateway-dev.yml` 的 `security.ignore.whites` 中添加 `/api/**`（如尚未添加），确保 C 端请求不被 AuthFilter 拦截。

- [ ] **Step 1: 更新 Nacos 网关路由**

在 `ruoyi-gateway-dev.yml` 中追加 C 端 mall-user 路由。

- [ ] **Step 2: 确认路由生效**

重启 ruoyi-gateway 后调用 `POST http://localhost:8080/api/user/profile` 验证 C 端路由。

- [ ] **Step 3: 提交**

```bash
git add -A
git commit -m "config(gateway): add mall-user C-end route to Nacos gateway config"
```

---

### Task 5: 管理端 Controller 搬迁到 controller/admin/ 包

> 管理端路由由 `discovery.locator` 自动转发（`/{serviceId}/{controllerPath}`），Controller 的 `@RequestMapping` 和 `@RequiresPermissions` **保持原值不变**。此任务仅搬迁包路径，不改路径。

**Files:**
- Modify (move): 7 controller files from `controller/` to `controller/admin/`

For each controller:
1. Change package from `com.mall.user.controller` to `com.mall.user.controller.admin`
2. `@RequestMapping` **保持不变**（如 `/user`、`/address`）
3. `@RequiresPermissions` **保持不变**（已为 `mall-user:*` 格式）

- [ ] **Step 1: Move MallUserController**

Move file to `server/mall/mall-user/src/main/java/com/mall/user/controller/admin/MallUserController.java` with:

```java
package com.mall.user.controller.admin;

// ... imports unchanged ...

@RestController
@RequestMapping("/user")
public class MallUserController extends BaseController
{
    @Autowired
    private IMallUserService mallUserService;

    @RequiresPermissions("mall-user:user:list")
    @GetMapping("/list")
    public TableDataInfo list(MallUser mallUser)
    {
        startPage();
        List<MallUser> list = mallUserService.selectMallUserList(mallUser);
        return getDataTable(list);
    }

    @RequiresPermissions("mall-user:user:export")
    @Log(title = "用户账号", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MallUser mallUser)
    {
        List<MallUser> list = mallUserService.selectMallUserList(mallUser);
        ExcelUtil<MallUser> util = new ExcelUtil<MallUser>(MallUser.class);
        util.exportExcel(response, list, "用户账号数据");
    }

    @RequiresPermissions("mall-user:user:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(mallUserService.selectMallUserById(id));
    }

    @RequiresPermissions("mall-user:user:add")
    @Log(title = "用户账号", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MallUser mallUser)
    {
        return toAjax(mallUserService.insertMallUser(mallUser));
    }

    @RequiresPermissions("mall-user:user:edit")
    @Log(title = "用户账号", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MallUser mallUser)
    {
        return toAjax(mallUserService.updateMallUser(mallUser));
    }

    @RequiresPermissions("mall-user:user:remove")
    @Log(title = "用户账号", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(mallUserService.deleteMallUserByIds(ids));
    }
}
```

- [ ] **Step 2: Move MallUserAddressController**

Move to `server/mall/mall-user/src/main/java/com/mall/user/controller/admin/MallUserAddressController.java` with:

```java
package com.mall.user.controller.admin;

// ... imports unchanged (change import for MallUserAddress to com.mall.user.domain) ...

@RestController
@RequestMapping("/address")
public class MallUserAddressController extends BaseController
{
    @Autowired
    private IMallUserAddressService mallUserAddressService;

    @RequiresPermissions("mall-user:address:list")
    @GetMapping("/list")
    public TableDataInfo list(MallUserAddress mallUserAddress)
    {
        startPage();
        List<MallUserAddress> list = mallUserAddressService.selectMallUserAddressList(mallUserAddress);
        return getDataTable(list);
    }

    @RequiresPermissions("mall-user:address:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, MallUserAddress mallUserAddress)
    {
        List<MallUserAddress> list = mallUserAddressService.selectMallUserAddressList(mallUserAddress);
        ExcelUtil<MallUserAddress> util = new ExcelUtil<MallUserAddress>(MallUserAddress.class);
        util.exportExcel(response, list, "地址簿数据");
    }

    @RequiresPermissions("mall-user:address:query")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(mallUserAddressService.selectMallUserAddressById(id));
    }

    @RequiresPermissions("mall-user:address:add")
    @Log(title = "地址簿", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MallUserAddress mallUserAddress)
    {
        return toAjax(mallUserAddressService.insertMallUserAddress(mallUserAddress));
    }

    @RequiresPermissions("mall-user:address:edit")
    @Log(title = "地址簿", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MallUserAddress mallUserAddress)
    {
        return toAjax(mallUserAddressService.updateMallUserAddress(mallUserAddress));
    }

    @RequiresPermissions("mall-user:address:remove")
    @Log(title = "地址簿", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(mallUserAddressService.deleteMallUserAddressByIds(ids));
    }
}
```

- [ ] **Step 3: Move remaining 5 admin controllers**

Same pattern for: `MallUserMemberController`, `MallUserMemberLevelController`, `MallUserPointsAccountController`, `MallUserPointsLogController`, `MallUserGrowthLogController`.

Each:
- New package: `com.mall.user.controller.admin`
- `@RequestMapping` and `@RequiresPermissions` unchanged

- [ ] **Step 4: 删除旧文件**

After confirming all 7 files compile at new locations:

```bash
rm server/mall/mall-user/src/main/java/com/mall/user/controller/MallUserController.java
rm server/mall/mall-user/src/main/java/com/mall/user/controller/MallUserAddressController.java
rm server/mall/mall-user/src/main/java/com/mall/user/controller/MallUserMemberController.java
rm server/mall/mall-user/src/main/java/com/mall/user/controller/MallUserMemberLevelController.java
rm server/mall/mall-user/src/main/java/com/mall/user/controller/MallUserPointsAccountController.java
rm server/mall/mall-user/src/main/java/com/mall/user/controller/MallUserPointsLogController.java
rm server/mall/mall-user/src/main/java/com/mall/user/controller/MallUserGrowthLogController.java
```

- [ ] **Step 5: 构建验证**

```bash
mvn clean compile -f server/mall/pom.xml -pl mall-user -am -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add server/mall/mall-user/src/main/java/com/mall/user/controller/
git commit -m "refactor(mall-user): move admin controllers to controller/admin subpackage"
```

---

### Task 6: Mapper XML 新增 SQL

**Files:**
- Modify: `server/mall/mall-user/src/main/resources/mapper/mall-user/MallUserMapper.xml`
- Modify: `server/mall/mall-user/src/main/resources/mapper/mall-user/MallUserAddressMapper.xml`
- Modify: `server/mall/mall-user/src/main/resources/mapper/mall-user/MallUserMemberMapper.xml`
- Modify: `server/mall/mall-user/src/main/resources/mapper/mall-user/MallUserMemberLevelMapper.xml`
- Modify: `server/mall/mall-user/src/main/resources/mapper/mall-user/MallUserPointsAccountMapper.xml`
- Modify: `server/mall/mall-user/src/main/resources/mapper/mall-user/MallUserPointsLogMapper.xml`
- Modify: `server/mall/mall-user/src/main/resources/mapper/mall-user/MallUserGrowthLogMapper.xml`
- Modify: `server/mall/mall-user/src/main/java/com/mall/user/mapper/` (6 Mapper interfaces)

- [ ] **Step 1: Append to MallUserMapper.xml** (before closing `</mapper>`)

```xml
    <!-- C端 API: 按 phone_hash 精确查询（Feign 调用用，不检查 is_deleted） -->
    <select id="selectByPhoneHash" parameterType="String" resultMap="MallUserResult">
        <include refid="selectMallUserVo"/>
        where phone_hash = #{phoneHash}
    </select>

    <!-- C端 API: 按 ID 查询（不过滤 is_deleted，Feign 内部调用用） -->
    <select id="selectByIdInternal" parameterType="Long" resultMap="MallUserResult">
        <include refid="selectMallUserVo"/>
        where id = #{id}
    </select>

    <!-- C端 API: 更新用户状态 -->
    <update id="updateUserStatus">
        update mall_user set user_status = #{status}, update_time = now()
        where id = #{userId}
    </update>

    <!-- C端 API: 更新密码 -->
    <update id="updatePassword">
        update mall_user set password = #{password}, update_time = now()
        where id = #{userId}
    </update>

    <!-- C端 API: 更新手机号 -->
    <update id="updatePhone">
        update mall_user set phone = #{phone}, phone_hash = #{phoneHash}, update_time = now()
        where id = #{userId}
    </update>
```

Add to `MallUserMapper.java`:

```java
    MallUser selectByPhoneHash(String phoneHash);
    MallUser selectByIdInternal(@Param("id") Long id);
    int updateUserStatus(@Param("userId") Long userId, @Param("status") Integer status);
    int updatePassword(@Param("userId") Long userId, @Param("password") String password);
    int updatePhone(@Param("userId") Long userId, @Param("phone") String phone, @Param("phoneHash") String phoneHash);
```

- [ ] **Step 2: Append to MallUserAddressMapper.xml** (before closing `</mapper>`)

```xml
    <!-- C端 API: 按用户 ID 查地址列表，默认地址排首位 -->
    <select id="selectByUserId" parameterType="String" resultMap="MallUserAddressResult">
        <include refid="selectMallUserAddressVo"/>
        where user_id = #{userId} and is_deleted = 0
        order by is_default desc, create_time asc
    </select>

    <!-- C端 API: 统计用户地址数 -->
    <select id="selectCountByUserId" parameterType="String" resultType="int">
        select count(*) from mall_user_address
        where user_id = #{userId} and is_deleted = 0
    </select>

    <!-- C端 API: 取消用户所有地址的默认状态 -->
    <update id="clearDefaultByUserId">
        update mall_user_address set is_default = 0
        where user_id = #{userId} and is_deleted = 0
    </update>

    <!-- C端 API: 查询用户最早创建的地址（删除默认时用） -->
    <select id="selectOldestByUserId" parameterType="String" resultMap="MallUserAddressResult">
        <include refid="selectMallUserAddressVo"/>
        where user_id = #{userId} and is_deleted = 0
        order by create_time asc limit 1
    </select>

    <!-- C端 API: 逻辑删除地址 -->
    <update id="logicDeleteById">
        update mall_user_address set is_deleted = 1, update_time = now()
        where id = #{id} and user_id = #{userId}
    </update>

    <!-- C端 API: 校验地址归属 -->
    <select id="validateBelongsToUser" resultType="int">
        select count(*) from mall_user_address
        where id = #{addressId} and user_id = #{userId} and is_deleted = 0
    </select>
```

Add to `MallUserAddressMapper.java`:

```java
    List<MallUserAddress> selectByUserId(String userId);
    int selectCountByUserId(String userId);
    int clearDefaultByUserId(String userId);
    MallUserAddress selectOldestByUserId(String userId);
    int logicDeleteById(@Param("id") Long id, @Param("userId") Long userId);
    int validateBelongsToUser(@Param("addressId") Long addressId, @Param("userId") Long userId);
```

- [ ] **Step 3: Append to MallUserMemberMapper.xml**

```xml
    <!-- C端 API: 按用户 ID 查会员信息 -->
    <select id="selectByUserId" parameterType="String" resultMap="MallUserMemberResult">
        <include refid="selectMallUserMemberVo"/>
        where user_id = #{userId} and is_deleted = 0
    </select>

    <!-- C端 API: 乐观锁更新成长值 -->
    <update id="updateGrowth">
        update mall_user_member
        set growth = growth + #{growth},
            total_growth = total_growth + #{totalGrowth},
            update_time = now(),
            version = version + 1
        where user_id = #{userId} and version = #{version}
    </update>

    <!-- C端 API: 更新会员等级 -->
    <update id="updateLevel">
        update mall_user_member
        set level_id = #{levelId}, update_time = now()
        where user_id = #{userId}
    </update>
```

Add to `MallUserMemberMapper.java`:

```java
    MallUserMember selectByUserId(String userId);
    int updateGrowth(@Param("userId") Long userId, @Param("growth") Long growth,
                     @Param("totalGrowth") Long totalGrowth, @Param("version") Long version);
    int updateLevel(@Param("userId") Long userId, @Param("levelId") Long levelId);
```

- [ ] **Step 4: Append to MallUserMemberLevelMapper.xml**

```xml
    <!-- C端 API: 查下一级等级 -->
    <select id="selectNextLevel" parameterType="Long" resultMap="MallUserMemberLevelResult">
        <include refid="selectMallUserMemberLevelVo"/>
        where level_value > #{currentLevelValue} and is_deleted = 0
        order by level_value asc limit 1
    </select>

    <!-- C端 API: 查所有等级 -->
    <select id="selectAll" resultMap="MallUserMemberLevelResult">
        <include refid="selectMallUserMemberLevelVo"/>
        where is_deleted = 0 order by level_value asc
    </select>
```

Add to `MallUserMemberLevelMapper.java`:

```java
    MallUserMemberLevel selectNextLevel(Long currentLevelValue);
    List<MallUserMemberLevel> selectAll();
```

- [ ] **Step 5: Append to MallUserPointsAccountMapper.xml**

```xml
    <!-- C端 API: 按用户 ID 查积分账户 -->
    <select id="selectByUserId" parameterType="String" resultMap="MallUserPointsAccountResult">
        <include refid="selectMallUserPointsAccountVo"/>
        where user_id = #{userId} and is_deleted = 0
    </select>

    <!-- C端 API: 乐观锁更新可用积分 -->
    <update id="updateAvailablePoints">
        update mall_user_points_account
        set available_points = available_points + #{deltaPoints},
            total_points = total_points + #{deltaTotal},
            used_points = used_points + #{deltaUsed},
            update_time = now(),
            version = version + 1
        where user_id = #{userId} and version = #{version}
    </update>
```

Add to `MallUserPointsAccountMapper.java`:

```java
    MallUserPointsAccount selectByUserId(String userId);
    int updateAvailablePoints(@Param("userId") Long userId,
                              @Param("deltaPoints") Long deltaPoints,
                              @Param("deltaTotal") Long deltaTotal,
                              @Param("deltaUsed") Long deltaUsed,
                              @Param("version") Long version);
```

- [ ] **Step 6: Append to MallUserPointsLogMapper.xml**

```xml
    <!-- C端 API: 按用户 ID 分页查询积分流水 -->
    <select id="selectListByUserId" resultMap="MallUserPointsLogResult">
        <include refid="selectMallUserPointsLogVo"/>
        where user_id = #{userId} and is_deleted = 0
        order by create_time desc
    </select>
```

Add to `MallUserPointsLogMapper.java`:

```java
    List<MallUserPointsLog> selectListByUserId(String userId);
```

- [ ] **Step 7: Append to MallUserGrowthLogMapper.xml**

```xml
    <!-- C端 API: 按用户 ID 分页查询成长值流水 -->
    <select id="selectListByUserId" resultMap="MallUserGrowthLogResult">
        <include refid="selectMallUserGrowthLogVo"/>
        where user_id = #{userId} and is_deleted = 0
        order by create_time desc
    </select>
```

Add to `MallUserGrowthLogMapper.java`:

```java
    List<MallUserGrowthLog> selectListByUserId(String userId);
```

- [ ] **Step 8: 构建验证**

```bash
mvn clean compile -f server/mall/pom.xml -pl mall-user -am -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 9: 提交**

```bash
git add server/mall/mall-user/src/main/resources/mapper/mall-user/
git add server/mall/mall-user/src/main/java/com/mall/user/mapper/
git commit -m "feat(mall-user): add C-end API SQL queries to Mapper XML files"
```

---

### Task 7: VO + Request DTO + Convert 对象

**Files:**
- Create: 5 VO files under `com/mall/user/vo/`
- Create: 3 DTO request files under `com/mall/user/dto/request/`
- Create: 5 Convert files under `com/mall/user/convert/`

- [ ] **Step 1: Create UserProfileVO.java**

```java
package com.mall.user.vo;

import java.util.Date;

public class UserProfileVO {

    private Long userId;
    private String nickname;
    private String avatar;
    private String phone;
    private Integer gender;
    private Date birthday;
    private String memberLevelName;
    private Long availablePoints;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }

    public Date getBirthday() { return birthday; }
    public void setBirthday(Date birthday) { this.birthday = birthday; }

    public String getMemberLevelName() { return memberLevelName; }
    public void setMemberLevelName(String memberLevelName) { this.memberLevelName = memberLevelName; }

    public Long getAvailablePoints() { return availablePoints; }
    public void setAvailablePoints(Long availablePoints) { this.availablePoints = availablePoints; }
}
```

- [ ] **Step 2: Create AddressVO.java**

```java
package com.mall.user.vo;

public class AddressVO {

    private Long id;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private String zipCode;
    private Integer isDefault;
    private String label;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getDetailAddress() { return detailAddress; }
    public void setDetailAddress(String detailAddress) { this.detailAddress = detailAddress; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public Integer getIsDefault() { return isDefault; }
    public void setIsDefault(Integer isDefault) { this.isDefault = isDefault; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
```

- [ ] **Step 3: Create MembershipVO.java**

```java
package com.mall.user.vo;

public class MembershipVO {

    private String levelName;
    private Integer levelValue;
    private String icon;
    private String benefitsJson;
    private Long growth;
    private Long totalGrowth;
    private String nextLevelName;
    private Long nextMinGrowth;
    private Integer progressPercent;

    public String getLevelName() { return levelName; }
    public void setLevelName(String levelName) { this.levelName = levelName; }

    public Integer getLevelValue() { return levelValue; }
    public void setLevelValue(Integer levelValue) { this.levelValue = levelValue; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getBenefitsJson() { return benefitsJson; }
    public void setBenefitsJson(String benefitsJson) { this.benefitsJson = benefitsJson; }

    public Long getGrowth() { return growth; }
    public void setGrowth(Long growth) { this.growth = growth; }

    public Long getTotalGrowth() { return totalGrowth; }
    public void setTotalGrowth(Long totalGrowth) { this.totalGrowth = totalGrowth; }

    public String getNextLevelName() { return nextLevelName; }
    public void setNextLevelName(String nextLevelName) { this.nextLevelName = nextLevelName; }

    public Long getNextMinGrowth() { return nextMinGrowth; }
    public void setNextMinGrowth(Long nextMinGrowth) { this.nextMinGrowth = nextMinGrowth; }

    public Integer getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }
}
```

- [ ] **Step 4: Create PointsRecordVO.java**

```java
package com.mall.user.vo;

import java.util.Date;

public class PointsRecordVO {

    private Long id;
    private String bizType;
    private String bizTypeDesc;
    private Long points;
    private Integer changeType;
    private Long beforePoints;
    private Long afterPoints;
    private String remark;
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }

    public String getBizTypeDesc() { return bizTypeDesc; }
    public void setBizTypeDesc(String bizTypeDesc) { this.bizTypeDesc = bizTypeDesc; }

    public Long getPoints() { return points; }
    public void setPoints(Long points) { this.points = points; }

    public Integer getChangeType() { return changeType; }
    public void setChangeType(Integer changeType) { this.changeType = changeType; }

    public Long getBeforePoints() { return beforePoints; }
    public void setBeforePoints(Long beforePoints) { this.beforePoints = beforePoints; }

    public Long getAfterPoints() { return afterPoints; }
    public void setAfterPoints(Long afterPoints) { this.afterPoints = afterPoints; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
```

- [ ] **Step 5: Create GrowthRecordVO.java**

```java
package com.mall.user.vo;

import java.util.Date;

public class GrowthRecordVO {

    private Long id;
    private String bizType;
    private String bizTypeDesc;
    private Long growth;
    private Integer changeType;
    private Long beforeGrowth;
    private Long afterGrowth;
    private String remark;
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }

    public String getBizTypeDesc() { return bizTypeDesc; }
    public void setBizTypeDesc(String bizTypeDesc) { this.bizTypeDesc = bizTypeDesc; }

    public Long getGrowth() { return growth; }
    public void setGrowth(Long growth) { this.growth = growth; }

    public Integer getChangeType() { return changeType; }
    public void setChangeType(Integer changeType) { this.changeType = changeType; }

    public Long getBeforeGrowth() { return beforeGrowth; }
    public void setBeforeGrowth(Long beforeGrowth) { this.beforeGrowth = beforeGrowth; }

    public Long getAfterGrowth() { return afterGrowth; }
    public void setAfterGrowth(Long afterGrowth) { this.afterGrowth = afterGrowth; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
```

- [ ] **Step 6: Create UpdateProfileReq.java**

```java
package com.mall.user.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

public class UpdateProfileReq {

    private String nickname;
    private String avatar;
    private Integer gender;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date birthday;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }

    public Date getBirthday() { return birthday; }
    public void setBirthday(Date birthday) { this.birthday = birthday; }
}
```

- [ ] **Step 7: Create CreateAddressReq.java**

```java
package com.mall.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public class CreateAddressReq {

    @NotBlank(message = "收件人姓名不能为空")
    private String receiverName;

    @NotBlank(message = "收件人手机号不能为空")
    private String receiverPhone;

    @NotBlank(message = "省不能为空")
    private String province;

    @NotBlank(message = "市不能为空")
    private String city;

    @NotBlank(message = "区不能为空")
    private String district;

    @NotBlank(message = "详细地址不能为空")
    private String detailAddress;

    private String zipCode;
    private Integer isDefault;
    private String label;

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getDetailAddress() { return detailAddress; }
    public void setDetailAddress(String detailAddress) { this.detailAddress = detailAddress; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public Integer getIsDefault() { return isDefault; }
    public void setIsDefault(Integer isDefault) { this.isDefault = isDefault; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
```

- [ ] **Step 8: Create UpdateAddressReq.java**

```java
package com.mall.user.dto.request;

public class UpdateAddressReq {

    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private String zipCode;
    private Integer isDefault;
    private String label;

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getDetailAddress() { return detailAddress; }
    public void setDetailAddress(String detailAddress) { this.detailAddress = detailAddress; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public Integer getIsDefault() { return isDefault; }
    public void setIsDefault(Integer isDefault) { this.isDefault = isDefault; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
```

- [ ] **Step 9: Create UserConvert.java**

```java
package com.mall.user.convert;

import com.mall.user.domain.MallUser;
import com.mall.user.domain.MallUserMember;
import com.mall.user.domain.MallUserMemberLevel;
import com.mall.user.domain.MallUserPointsAccount;
import com.mall.user.mapper.MallUserMemberLevelMapper;
import com.mall.user.vo.UserProfileVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserConvert {

    @Autowired
    private MallUserMemberLevelMapper mallUserMemberLevelMapper;

    public UserProfileVO toProfileVO(MallUser user, MallUserMember member, MallUserPointsAccount pointsAccount) {
        UserProfileVO vo = new UserProfileVO();
        vo.setUserId(Long.valueOf(user.getId()));
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(maskPhone(user.getPhone()));
        if (user.getGender() != null) {
            vo.setGender(Integer.valueOf(user.getGender()));
        }
        vo.setBirthday(user.getBirthday());

        if (member != null && member.getLevelId() != null) {
            MallUserMemberLevel level = mallUserMemberLevelMapper.selectMallUserMemberLevelById(member.getLevelId());
            vo.setMemberLevelName(level != null ? level.getLevelName() : null);
        }
        if (pointsAccount != null) {
            vo.setAvailablePoints(pointsAccount.getAvailablePoints());
        }
        return vo;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
```

- [ ] **Step 10: Create AddressConvert.java**

```java
package com.mall.user.convert;

import com.mall.user.domain.MallUserAddress;
import com.mall.user.vo.AddressVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AddressConvert {

    public AddressVO toAddressVO(MallUserAddress address) {
        if (address == null) return null;
        AddressVO vo = new AddressVO();
        vo.setId(Long.valueOf(address.getId()));
        vo.setReceiverName(address.getReceiverName());
        vo.setReceiverPhone(maskPhone(address.getReceiverPhone()));
        vo.setProvince(address.getProvince());
        vo.setCity(address.getCity());
        vo.setDistrict(address.getDistrict());
        vo.setDetailAddress(address.getDetailAddress());
        vo.setZipCode(address.getZipCode());
        if (address.getIsDefault() != null) {
            vo.setIsDefault(Integer.valueOf(address.getIsDefault()));
        }
        vo.setLabel(address.getLabel());
        return vo;
    }

    public List<AddressVO> toAddressVOList(List<MallUserAddress> addresses) {
        return addresses.stream()
            .map(this::toAddressVO)
            .collect(Collectors.toList());
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
```

- [ ] **Step 11: Create MemberConvert.java**

```java
package com.mall.user.convert;

import com.mall.user.domain.MallUserMember;
import com.mall.user.domain.MallUserMemberLevel;
import com.mall.user.vo.MembershipVO;
import org.springframework.stereotype.Component;

@Component
public class MemberConvert {

    public MembershipVO toMembershipVO(MallUserMember member, MallUserMemberLevel currentLevel, MallUserMemberLevel nextLevel) {
        MembershipVO vo = new MembershipVO();
        if (currentLevel != null) {
            vo.setLevelName(currentLevel.getLevelName());
            if (currentLevel.getLevelValue() != null) {
                vo.setLevelValue(Integer.valueOf(currentLevel.getLevelValue()));
            }
            vo.setIcon(currentLevel.getIcon());
            vo.setBenefitsJson(currentLevel.getBenefitsJson());
        }

        if (member != null) {
            vo.setGrowth(member.getGrowth());
            vo.setTotalGrowth(member.getTotalGrowth());
        }

        if (nextLevel != null && member != null && member.getGrowth() != null && nextLevel.getMinGrowth() != null) {
            vo.setNextLevelName(nextLevel.getLevelName());
            vo.setNextMinGrowth(nextLevel.getMinGrowth());
            int progress = (int) (member.getGrowth() * 100L / nextLevel.getMinGrowth());
            vo.setProgressPercent(Math.min(progress, 100));
        } else if (currentLevel != null) {
            vo.setProgressPercent(100);
        }

        return vo;
    }
}
```

- [ ] **Step 12: Create PointsConvert.java**

```java
package com.mall.user.convert;

import com.mall.user.domain.MallUserPointsLog;
import com.mall.user.vo.PointsRecordVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PointsConvert {

    public PointsRecordVO toRecordVO(MallUserPointsLog log) {
        PointsRecordVO vo = new PointsRecordVO();
        vo.setId(Long.valueOf(log.getId()));
        vo.setBizType(log.getBizType());
        vo.setBizTypeDesc(getBizTypeDesc(log.getBizType()));
        vo.setPoints(log.getPoints());
        if (log.getChangeType() != null) {
            vo.setChangeType(Integer.valueOf(log.getChangeType()));
        }
        vo.setBeforePoints(log.getBeforePoints());
        vo.setAfterPoints(log.getAfterPoints());
        vo.setRemark(log.getRemark());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }

    public List<PointsRecordVO> toRecordVOList(List<MallUserPointsLog> logs) {
        return logs.stream().map(this::toRecordVO).collect(Collectors.toList());
    }

    private String getBizTypeDesc(String bizType) {
        if (bizType == null) return "";
        switch (bizType) {
            case "order": return "下单赠送";
            case "signin": return "每日签到";
            case "refund": return "退款扣除";
            case "admin": return "管理员调整";
            case "review": return "评价奖励";
            case "expire": return "积分过期";
            default: return bizType;
        }
    }
}
```

- [ ] **Step 13: Create GrowthConvert.java**

```java
package com.mall.user.convert;

import com.mall.user.domain.MallUserGrowthLog;
import com.mall.user.vo.GrowthRecordVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class GrowthConvert {

    public GrowthRecordVO toRecordVO(MallUserGrowthLog log) {
        GrowthRecordVO vo = new GrowthRecordVO();
        vo.setId(Long.valueOf(log.getId()));
        vo.setBizType(log.getBizType());
        vo.setBizTypeDesc(getBizTypeDesc(log.getBizType()));
        vo.setGrowth(log.getGrowth());
        if (log.getChangeType() != null) {
            vo.setChangeType(Integer.valueOf(log.getChangeType()));
        }
        vo.setBeforeGrowth(log.getBeforeGrowth());
        vo.setAfterGrowth(log.getAfterGrowth());
        vo.setRemark(log.getRemark());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }

    public List<GrowthRecordVO> toRecordVOList(List<MallUserGrowthLog> logs) {
        return logs.stream().map(this::toRecordVO).collect(Collectors.toList());
    }

    private String getBizTypeDesc(String bizType) {
        if (bizType == null) return "";
        switch (bizType) {
            case "order": return "订单完成";
            case "signin": return "每日签到";
            case "review": return "商品评价";
            case "admin": return "管理员调整";
            case "refund": return "退款扣除";
            default: return bizType;
        }
    }
}
```

- [ ] **Step 14: 构建验证**

```bash
mvn clean compile -f server/mall/pom.xml -pl mall-user -am -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 15: 提交**

```bash
git add server/mall/mall-user/src/main/java/com/mall/user/vo/
git add server/mall/mall-user/src/main/java/com/mall/user/dto/
git add server/mall/mall-user/src/main/java/com/mall/user/convert/
git commit -m "feat(mall-user): add VO, request DTO and Convert classes for C-end API"
```

---

### Task 8: C 端业务 Service

**Files:**
- Create: 5 Service interfaces under `com/mall/user/service/{user,address,member,points,growth}/`
- Create: 5 Service implementations under `com/mall/user/service/{user,address,member,points,growth}/impl/`

- [ ] **Step 1: Create UserService interface**

File: `server/mall/mall-user/src/main/java/com/mall/user/service/user/UserService.java`

```java
package com.mall.user.service.user;

import com.mall.user.domain.MallUser;
import com.mall.user.vo.UserProfileVO;

public interface UserService {

    UserProfileVO getProfile(Long userId);

    void updateProfile(Long userId, String nickname, String avatar, Integer gender, java.util.Date birthday);

    MallUser findByPhoneHash(String phoneHash);

    Long register(String phoneEncrypted, String phoneHash, String passwordHash);

    void updatePassword(Long userId, String newPasswordHash);

    void updatePhone(Long userId, String phoneEncrypted, String phoneHash);

    void deactivateAccount(Long userId);

    MallUser findByIdInternal(Long userId);
}
```

- [ ] **Step 2: Create UserServiceImpl**

File: `server/mall/mall-user/src/main/java/com/mall/user/service/user/impl/UserServiceImpl.java`

```java
package com.mall.user.service.user.impl;

import com.mall.user.convert.UserConvert;
import com.mall.user.domain.MallUser;
import com.mall.user.domain.MallUserMember;
import com.mall.user.domain.MallUserPointsAccount;
import com.mall.user.mapper.MallUserMapper;
import com.mall.user.mapper.MallUserMemberMapper;
import com.mall.user.mapper.MallUserPointsAccountMapper;
import com.mall.user.service.user.UserService;
import com.mall.user.vo.UserProfileVO;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private MallUserMapper mallUserMapper;

    @Autowired
    private MallUserMemberMapper mallUserMemberMapper;

    @Autowired
    private MallUserPointsAccountMapper mallUserPointsAccountMapper;

    @Autowired
    private UserConvert userConvert;

    @Override
    public UserProfileVO getProfile(Long userId) {
        MallUser user = mallUserMapper.selectByIdInternal(userId);
        if (user == null) return null;

        MallUserMember member = mallUserMemberMapper.selectByUserId(String.valueOf(userId));
        MallUserPointsAccount points = mallUserPointsAccountMapper.selectByUserId(String.valueOf(userId));
        return userConvert.toProfileVO(user, member, points);
    }

    @Override
    public void updateProfile(Long userId, String nickname, String avatar, Integer gender, Date birthday) {
        MallUser user = new MallUser();
        user.setId(String.valueOf(userId));
        user.setNickname(nickname);
        user.setAvatar(avatar);
        if (gender != null) user.setGender(String.valueOf(gender));
        user.setBirthday(birthday);
        user.setUpdateTime(DateUtils.getNowDate());
        mallUserMapper.updateMallUser(user);
    }

    @Override
    public MallUser findByPhoneHash(String phoneHash) {
        return mallUserMapper.selectByPhoneHash(phoneHash);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(String phoneEncrypted, String phoneHash, String passwordHash) {
        MallUser user = new MallUser();
        user.setPhone(phoneEncrypted);
        user.setPhoneHash(phoneHash);
        user.setPassword(passwordHash);
        user.setRegisterTime(DateUtils.getNowDate());
        user.setCreateTime(DateUtils.getNowDate());
        user.setUserStatus("0");
        mallUserMapper.insertMallUser(user);

        Long userId = Long.valueOf(user.getId());

        MallUserMember member = new MallUserMember();
        member.setUserId(String.valueOf(userId));
        member.setLevelId("1");
        member.setGrowth(0L);
        member.setTotalGrowth(0L);
        member.setCreateTime(DateUtils.getNowDate());
        mallUserMemberMapper.insertMallUserMember(member);

        MallUserPointsAccount points = new MallUserPointsAccount();
        points.setUserId(String.valueOf(userId));
        points.setTotalPoints(0L);
        points.setAvailablePoints(0L);
        points.setUsedPoints(0L);
        points.setExpiredPoints(0L);
        points.setCreateTime(DateUtils.getNowDate());
        mallUserPointsAccountMapper.insertMallUserPointsAccount(points);

        return userId;
    }

    @Override
    public void updatePassword(Long userId, String newPasswordHash) {
        mallUserMapper.updatePassword(userId, newPasswordHash);
    }

    @Override
    public void updatePhone(Long userId, String phoneEncrypted, String phoneHash) {
        mallUserMapper.updatePhone(userId, phoneEncrypted, phoneHash);
    }

    @Override
    public void deactivateAccount(Long userId) {
        mallUserMapper.updateUserStatus(userId, 2);
    }

    @Override
    public MallUser findByIdInternal(Long userId) {
        return mallUserMapper.selectByIdInternal(userId);
    }
}
```

- [ ] **Step 3: Create AddressService interface + impl**

File: `server/mall/mall-user/src/main/java/com/mall/user/service/address/AddressService.java`

```java
package com.mall.user.service.address;

import com.mall.user.dto.request.CreateAddressReq;
import com.mall.user.dto.request.UpdateAddressReq;
import com.mall.user.vo.AddressVO;

import java.util.List;

public interface AddressService {

    List<AddressVO> listByUser(Long userId);

    void create(Long userId, CreateAddressReq req);

    void update(Long userId, Long addressId, UpdateAddressReq req);

    void delete(Long userId, Long addressId);

    void setDefault(Long userId, Long addressId);

    boolean validateAddress(Long userId, Long addressId);
}
```

File: `server/mall/mall-user/src/main/java/com/mall/user/service/address/impl/AddressServiceImpl.java`

```java
package com.mall.user.service.address.impl;

import com.mall.user.convert.AddressConvert;
import com.mall.user.domain.MallUserAddress;
import com.mall.user.dto.request.CreateAddressReq;
import com.mall.user.dto.request.UpdateAddressReq;
import com.mall.user.mapper.MallUserAddressMapper;
import com.mall.user.service.address.AddressService;
import com.mall.user.vo.AddressVO;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private MallUserAddressMapper mallUserAddressMapper;

    @Autowired
    private AddressConvert addressConvert;

    @Value("${mall.user.address.max-count:20}")
    private int maxAddressCount;

    @Override
    public List<AddressVO> listByUser(Long userId) {
        List<MallUserAddress> addresses = mallUserAddressMapper.selectByUserId(String.valueOf(userId));
        return addressConvert.toAddressVOList(addresses);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(Long userId, CreateAddressReq req) {
        int count = mallUserAddressMapper.selectCountByUserId(String.valueOf(userId));
        if (count >= maxAddressCount) {
            throw new RuntimeException("A0511|地址数量已达上限");
        }

        if (Boolean.TRUE.equals(req.getIsDefault())) {
            mallUserAddressMapper.clearDefaultByUserId(String.valueOf(userId));
        }

        MallUserAddress address = new MallUserAddress();
        address.setUserId(String.valueOf(userId));
        address.setReceiverName(req.getReceiverName());
        address.setReceiverPhone(req.getReceiverPhone());
        address.setProvince(req.getProvince());
        address.setCity(req.getCity());
        address.setDistrict(req.getDistrict());
        address.setDetailAddress(req.getDetailAddress());
        address.setZipCode(req.getZipCode());
        address.setIsDefault(Boolean.TRUE.equals(req.getIsDefault()) ? "1" : "0");
        address.setLabel(req.getLabel());
        address.setIsDeleted("0");
        address.setCreateTime(DateUtils.getNowDate());
        mallUserAddressMapper.insertMallUserAddress(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Long addressId, UpdateAddressReq req) {
        MallUserAddress address = mallUserAddressMapper.selectMallUserAddressById(String.valueOf(addressId));
        if (address == null || !String.valueOf(userId).equals(address.getUserId())) {
            throw new RuntimeException("A0501|地址不存在");
        }

        if (Boolean.TRUE.equals(req.getIsDefault())) {
            mallUserAddressMapper.clearDefaultByUserId(String.valueOf(userId));
        }

        address.setReceiverName(req.getReceiverName());
        address.setReceiverPhone(req.getReceiverPhone());
        address.setProvince(req.getProvince());
        address.setCity(req.getCity());
        address.setDistrict(req.getDistrict());
        address.setDetailAddress(req.getDetailAddress());
        address.setZipCode(req.getZipCode());
        if (req.getIsDefault() != null) {
            address.setIsDefault(req.getIsDefault() == 1 ? "1" : "0");
        }
        address.setLabel(req.getLabel());
        address.setUpdateTime(DateUtils.getNowDate());
        mallUserAddressMapper.updateMallUserAddress(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long addressId) {
        MallUserAddress address = mallUserAddressMapper.selectMallUserAddressById(String.valueOf(addressId));
        if (address == null || !String.valueOf(userId).equals(address.getUserId())) {
            throw new RuntimeException("A0501|地址不存在");
        }

        mallUserAddressMapper.logicDeleteById(addressId, userId);

        if ("1".equals(address.getIsDefault())) {
            MallUserAddress oldest = mallUserAddressMapper.selectOldestByUserId(String.valueOf(userId));
            if (oldest != null) {
                oldest.setIsDefault("1");
                oldest.setUpdateTime(DateUtils.getNowDate());
                mallUserAddressMapper.updateMallUserAddress(oldest);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long userId, Long addressId) {
        int count = mallUserAddressMapper.validateBelongsToUser(addressId, userId);
        if (count == 0) {
            throw new RuntimeException("A0501|地址不存在");
        }

        mallUserAddressMapper.clearDefaultByUserId(String.valueOf(userId));
        MallUserAddress address = mallUserAddressMapper.selectMallUserAddressById(String.valueOf(addressId));
        if (address != null) {
            address.setIsDefault("1");
            address.setUpdateTime(DateUtils.getNowDate());
            mallUserAddressMapper.updateMallUserAddress(address);
        }
    }

    @Override
    public boolean validateAddress(Long userId, Long addressId) {
        return mallUserAddressMapper.validateBelongsToUser(addressId, userId) > 0;
    }
}
```

- [ ] **Step 4: Create MemberService interface + impl**

File: `server/mall/mall-user/src/main/java/com/mall/user/service/member/MemberService.java`

```java
package com.mall.user.service.member;

import com.mall.user.vo.MembershipVO;

public interface MemberService {

    MembershipVO getMembership(Long userId);
}
```

File: `server/mall/mall-user/src/main/java/com/mall/user/service/member/impl/MemberServiceImpl.java`

```java
package com.mall.user.service.member.impl;

import com.mall.user.convert.MemberConvert;
import com.mall.user.domain.MallUserMember;
import com.mall.user.domain.MallUserMemberLevel;
import com.mall.user.mapper.MallUserMemberLevelMapper;
import com.mall.user.mapper.MallUserMemberMapper;
import com.mall.user.service.member.MemberService;
import com.mall.user.vo.MembershipVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MemberServiceImpl implements MemberService {

    @Autowired
    private MallUserMemberMapper mallUserMemberMapper;

    @Autowired
    private MallUserMemberLevelMapper mallUserMemberLevelMapper;

    @Autowired
    private MemberConvert memberConvert;

    @Override
    public MembershipVO getMembership(Long userId) {
        MallUserMember member = mallUserMemberMapper.selectByUserId(String.valueOf(userId));
        if (member == null) return null;

        MallUserMemberLevel currentLevel = null;
        if (member.getLevelId() != null) {
            currentLevel = mallUserMemberLevelMapper.selectMallUserMemberLevelById(member.getLevelId());
        }

        MallUserMemberLevel nextLevel = null;
        if (currentLevel != null && currentLevel.getLevelValue() != null) {
            nextLevel = mallUserMemberLevelMapper.selectNextLevel(Long.valueOf(currentLevel.getLevelValue()));
        }

        return memberConvert.toMembershipVO(member, currentLevel, nextLevel);
    }
}
```

- [ ] **Step 5: Create PointsService interface + impl**

File: `server/mall/mall-user/src/main/java/com/mall/user/service/points/PointsService.java`

```java
package com.mall.user.service.points;

import com.mall.user.vo.PointsRecordVO;

import java.util.List;

public interface PointsService {

    Long getPoints(Long userId);

    List<PointsRecordVO> listRecords(Long userId);
}
```

File: `server/mall/mall-user/src/main/java/com/mall/user/service/points/impl/PointsServiceImpl.java`

```java
package com.mall.user.service.points.impl;

import com.mall.user.convert.PointsConvert;
import com.mall.user.domain.MallUserPointsAccount;
import com.mall.user.domain.MallUserPointsLog;
import com.mall.user.mapper.MallUserPointsAccountMapper;
import com.mall.user.mapper.MallUserPointsLogMapper;
import com.mall.user.service.points.PointsService;
import com.mall.user.vo.PointsRecordVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointsServiceImpl implements PointsService {

    @Autowired
    private MallUserPointsAccountMapper mallUserPointsAccountMapper;

    @Autowired
    private MallUserPointsLogMapper mallUserPointsLogMapper;

    @Autowired
    private PointsConvert pointsConvert;

    @Override
    public Long getPoints(Long userId) {
        MallUserPointsAccount account = mallUserPointsAccountMapper.selectByUserId(String.valueOf(userId));
        return account != null ? account.getAvailablePoints() : 0L;
    }

    @Override
    public List<PointsRecordVO> listRecords(Long userId) {
        List<MallUserPointsLog> logs = mallUserPointsLogMapper.selectListByUserId(String.valueOf(userId));
        return pointsConvert.toRecordVOList(logs);
    }
}
```

- [ ] **Step 6: Create GrowthService interface + impl**

File: `server/mall/mall-user/src/main/java/com/mall/user/service/growth/GrowthService.java`

```java
package com.mall.user.service.growth;

import com.mall.user.vo.GrowthRecordVO;

import java.util.List;

public interface GrowthService {

    Long getGrowth(Long userId);

    List<GrowthRecordVO> listRecords(Long userId);
}
```

File: `server/mall/mall-user/src/main/java/com/mall/user/service/growth/impl/GrowthServiceImpl.java`

```java
package com.mall.user.service.growth.impl;

import com.mall.user.convert.GrowthConvert;
import com.mall.user.domain.MallUserMember;
import com.mall.user.domain.MallUserGrowthLog;
import com.mall.user.mapper.MallUserGrowthLogMapper;
import com.mall.user.mapper.MallUserMemberMapper;
import com.mall.user.service.growth.GrowthService;
import com.mall.user.vo.GrowthRecordVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GrowthServiceImpl implements GrowthService {

    @Autowired
    private MallUserMemberMapper mallUserMemberMapper;

    @Autowired
    private MallUserGrowthLogMapper mallUserGrowthLogMapper;

    @Autowired
    private GrowthConvert growthConvert;

    @Override
    public Long getGrowth(Long userId) {
        MallUserMember member = mallUserMemberMapper.selectByUserId(String.valueOf(userId));
        return member != null ? member.getGrowth() : 0L;
    }

    @Override
    public List<GrowthRecordVO> listRecords(Long userId) {
        List<MallUserGrowthLog> logs = mallUserGrowthLogMapper.selectListByUserId(String.valueOf(userId));
        return growthConvert.toRecordVOList(logs);
    }
}
```

- [ ] **Step 7: 构建验证**

```bash
mvn clean compile -f server/mall/pom.xml -pl mall-user -am -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 8: 提交**

```bash
git add server/mall/mall-user/src/main/java/com/mall/user/service/user/
git add server/mall/mall-user/src/main/java/com/mall/user/service/address/
git add server/mall/mall-user/src/main/java/com/mall/user/service/member/
git add server/mall/mall-user/src/main/java/com/mall/user/service/points/
git add server/mall/mall-user/src/main/java/com/mall/user/service/growth/
git commit -m "feat(mall-user): add C-end business services for profile/address/member/points/growth"
```

---

### Task 9: C 端 Controller

**Files:**
- Create: `server/mall/mall-user/src/main/java/com/mall/user/controller/api/ProfileController.java`
- Create: `server/mall/mall-user/src/main/java/com/mall/user/controller/api/AddressController.java`
- Create: `server/mall/mall-user/src/main/java/com/mall/user/controller/api/MembershipController.java`
- Create: `server/mall/mall-user/src/main/java/com/mall/user/controller/api/PointsController.java`
- Create: `server/mall/mall-user/src/main/java/com/mall/user/controller/api/GrowthController.java`

- [ ] **Step 1: Create ProfileController.java**

Note: C 端 Controller 不使用 `@RequiresPermissions`（Token 认证由网关统一处理）。通过 `X-User-Id` 请求头（MallAuthFilter 注入）获取当前用户 ID。

```java
package com.mall.user.controller.api;

import com.mall.user.dto.request.UpdateProfileReq;
import com.mall.user.service.user.UserService;
import com.mall.user.vo.UserProfileVO;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/profile")
public class ProfileController extends BaseController {

    @Autowired
    private UserService userService;

    @Autowired
    private HttpServletRequest request;

    @GetMapping
    public AjaxResult getProfile() {
        Long userId = Long.valueOf(request.getHeader("X-User-Id"));
        UserProfileVO profile = userService.getProfile(userId);
        if (profile == null) {
            return error("A0501|用户不存在");
        }
        return success(profile);
    }

    @PutMapping
    public AjaxResult updateProfile(@RequestBody UpdateProfileReq req) {
        Long userId = Long.valueOf(request.getHeader("X-User-Id"));
        userService.updateProfile(userId, req.getNickname(), req.getAvatar(),
                req.getGender(), req.getBirthday());
        return success();
    }
}
```

- [ ] **Step 2: Create AddressController.java**

```java
package com.mall.user.controller.api;

import com.mall.user.dto.request.CreateAddressReq;
import com.mall.user.dto.request.UpdateAddressReq;
import com.mall.user.service.address.AddressService;
import com.mall.user.vo.AddressVO;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/addresses")
public class AddressController extends BaseController {

    @Autowired
    private AddressService addressService;

    @Autowired
    private HttpServletRequest request;

    private Long currentUserId() {
        return Long.valueOf(request.getHeader("X-User-Id"));
    }

    @GetMapping
    public AjaxResult list() {
        List<AddressVO> list = addressService.listByUser(currentUserId());
        return success(list);
    }

    @PostMapping
    public AjaxResult create(@RequestBody CreateAddressReq req) {
        addressService.create(currentUserId(), req);
        return success();
    }

    @PutMapping("/{addressId}")
    public AjaxResult update(@PathVariable Long addressId, @RequestBody UpdateAddressReq req) {
        addressService.update(currentUserId(), addressId, req);
        return success();
    }

    @DeleteMapping("/{addressId}")
    public AjaxResult delete(@PathVariable Long addressId) {
        addressService.delete(currentUserId(), addressId);
        return success();
    }

    @PutMapping("/{addressId}/default")
    public AjaxResult setDefault(@PathVariable Long addressId) {
        addressService.setDefault(currentUserId(), addressId);
        return success();
    }
}
```

- [ ] **Step 3: Create MembershipController.java**

```java
package com.mall.user.controller.api;

import com.mall.user.service.member.MemberService;
import com.mall.user.vo.MembershipVO;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/membership")
public class MembershipController extends BaseController {

    @Autowired
    private MemberService memberService;

    @Autowired
    private HttpServletRequest request;

    @GetMapping
    public AjaxResult getMembership() {
        Long userId = Long.valueOf(request.getHeader("X-User-Id"));
        MembershipVO membership = memberService.getMembership(userId);
        if (membership == null) {
            return error("A0501|会员信息不存在");
        }
        return success(membership);
    }
}
```

- [ ] **Step 4: Create PointsController.java**

```java
package com.mall.user.controller.api;

import com.mall.user.service.points.PointsService;
import com.mall.user.vo.PointsRecordVO;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/points")
public class PointsController extends BaseController {

    @Autowired
    private PointsService pointsService;

    @Autowired
    private HttpServletRequest request;

    @GetMapping
    public AjaxResult getPoints() {
        Long userId = Long.valueOf(request.getHeader("X-User-Id"));
        Long points = pointsService.getPoints(userId);
        return success(points);
    }

    @GetMapping("/records")
    public AjaxResult listRecords() {
        Long userId = Long.valueOf(request.getHeader("X-User-Id"));
        startPage();
        List<PointsRecordVO> records = pointsService.listRecords(userId);
        return success(records);
    }
}
```

- [ ] **Step 5: Create GrowthController.java**

```java
package com.mall.user.controller.api;

import com.mall.user.service.growth.GrowthService;
import com.mall.user.vo.GrowthRecordVO;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/growth")
public class GrowthController extends BaseController {

    @Autowired
    private GrowthService growthService;

    @Autowired
    private HttpServletRequest request;

    @GetMapping
    public AjaxResult getGrowth() {
        Long userId = Long.valueOf(request.getHeader("X-User-Id"));
        Long growth = growthService.getGrowth(userId);
        return success(growth);
    }

    @GetMapping("/records")
    public AjaxResult listRecords() {
        Long userId = Long.valueOf(request.getHeader("X-User-Id"));
        startPage();
        List<GrowthRecordVO> records = growthService.listRecords(userId);
        return success(records);
    }
}
```

- [ ] **Step 6: 构建验证**

```bash
mvn clean compile -f server/mall/pom.xml -pl mall-user -am -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add server/mall/mall-user/src/main/java/com/mall/user/controller/api/
git commit -m "feat(mall-user): add C-end API controllers for profile/address/member/points/growth"
```

---

### Task 10: 完整构建验证

**Files:** No new files — build all modules

- [ ] **Step 1: 完整 Maven 构建**

```bash
mvn clean install -f server/mall/pom.xml -DskipTests
```

Expected: BUILD SUCCESS (all 8 modules compile)

- [ ] **Step 2: 最终提交**

```bash
git add -A
git commit -m "feat(mall-user): complete C-end API implementation with restructured packages"
```

---

## 自我检查

- [ ] **Spec coverage**: 覆盖了 mall-api 枚举/DTO/Feign（Task 1-3）、网关路由（Task 4）、管理端 Controller 迁移（Task 5）、Mapper SQL（Task 6）、VO/DTO/Convert（Task 7）、C 端 Service（Task 8）、C 端 Controller（Task 9）
- [ ] **Placeholder scan**: 无 TBD/TODO/「类似」引用/空占位
- [ ] **Type consistency**: 所有 `userId` 为 `Long`，`id` 为 `String`（匹配现有 Domain 的 `String id`），VO/DTO/Convert 间类型一致
