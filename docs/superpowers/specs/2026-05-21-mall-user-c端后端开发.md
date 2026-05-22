# mall-user C 端后端开发规格说明

> 基于已有设计文档 `docs/design/12_mall-user详细设计.md`、`docs/design/05_mall-api契约层设计.md`、`docs/design/03_系统详细设计.md` 的实施规格。

## 架构原则

- 包名统一使用 `com.mall.*`，遵循现有代码习惯
- C 端 API 路径前缀 `/api/user/`，管理端路径前缀 `/admin/mall/user/`
- C 端认证：从请求头手动解析 JWT 获取 userId（C 端 Token 与管理端不同体系，`SecurityUtils.getUserId()` 不可用）
- 新增代码遵循阿里巴巴 Java 开发手册 + 项目 AGENTS.md 规范

## 一、mall-api 契约层

### 1.1 共享枚举

| 文件 | 路径 | 值 |
|------|------|----|
| `UserStatusEnum.java` | `com.mall.api.enums` | NORMAL(0), FROZEN(1), DELETED(2) |
| `PointsChangeTypeEnum.java` | `com.mall.api.enums` | EARN(1), CONSUME(2), EXPIRE(3), ADMIN_ADJUST(4) |
| `GrowthChangeTypeEnum.java` | `com.mall.api.enums` | EARN(1), CONSUME(2), ADMIN_ADJUST(3) |

### 1.2 共享 DTO

| 文件 | 路径 | 字段 |
|------|------|------|
| `UserDTO.java` | `com.mall.api.dto.user` | userId(Long), phoneHash(String), passwordHash(String), userStatus(Integer), nickname(String), avatar(String) |
| `AddressDTO.java` | `com.mall.api.dto.user` | addressId(Long), userId(Long), receiverName(String), receiverPhone(String), province/city/district/detail(String), isDefault(Integer) |

### 1.3 Feign 接口

**`RemoteUserService`**（提供方 mall-user，调用方 mall-auth/mall-order）：

| 方法 | 参数 | 返回 | 说明 |
|------|------|------|------|
| `register` | `phoneEncrypted`(String), `phoneHash`(String), `passwordHash`(String) | `Long` userId | 注册新用户，同时初始化会员+积分账户 |
| `findByPhone` | `phoneHash`(String) | `UserDTO` | 按手机号哈希查用户 |
| `findByWechatOpenId` | `openId`(String) | `UserDTO` | 按微信 openId 查用户 |
| `registerByWechat` | `openId`(String), `unionId`(String) | `Long` userId | 微信首次授权自动注册 |
| `updatePassword` | `userId`(Long), `newPasswordHash`(String) | `void` | 更新密码哈希 |
| `updatePhone` | `userId`(Long), `phoneEncrypted`(String), `phoneHash`(String) | `void` | 换绑手机号 |
| `deactivateAccount` | `userId`(Long) | `void` | 注销账号 |
| `validateAddress` | `userId`(Long), `addressId`(Long) | `boolean` | 校验地址归属 |
| `findById` | `userId`(Long) | `UserDTO` | 按 ID 查用户（供 mall-auth 使用） |

**`RemoteAuthService`**（提供方 mall-auth，调用方 mall-user）：

| 方法 | 参数 | 返回 | 说明 |
|------|------|------|------|
| `decrypt` | `encryptedData`(String) | `String` 明文 | 解密单条数据 |
| `batchDecrypt` | `encryptedDataList`(List\<String\>) | `List\<String\>` 明文列表 | 批量解密 |

## 二、mall-user 模块重构

### 2.1 管理端 Controller 迁移

将 7 个现有 Controller 从 `com.mall.user.controller` 移到 `com.mall.user.controller.admin`：

| 文件 | 旧 @RequestMapping | 新 @RequestMapping |
|------|-------------------|-------------------|
| MallUserController | `/user` | `/admin/mall/user/users` |
| MallUserAddressController | `/address` | `/admin/mall/user/addresses` |
| MallUserMemberController | `/member` | `/admin/mall/user/members` |
| MallUserMemberLevelController | `/level` | `/admin/mall/user/levels` |
| MallUserPointsAccountController | `/account` | `/admin/mall/user/points/accounts` |
| MallUserPointsLogController | `/points_log` | `/admin/mall/user/points/logs` |
| MallUserGrowthLogController | `/growth_log` | `/admin/mall/user/growth/logs` |

### 2.2 新增 C 端 Controller

包：`com.mall.user.controller.api`

| Controller | @RequestMapping | 方法 |
|-----------|----------------|------|
| `ProfileController` | `/api/user/profile` | `getProfile()`, `updateProfile(req)` |
| `AddressController` | `/api/user/addresses` | `list()`, `create(req)`, `update(addressId, req)`, `delete(addressId)`, `setDefault(addressId)` |
| `MembershipController` | `/api/user/membership` | `getMembership()` |
| `PointsController` | `/api/user/points` | `getPoints()`, `listRecords(params)` |
| `GrowthController` | `/api/user/growth` | `getGrowth()`, `listRecords(params)` |

所有 C 端 Controller：
- 继承 `BaseController`，使用 `success()` / `error()` / `toAjax()` / `getDataTable()` 返回
- 不继承 `@RequiresPermissions`（C 端 Token 认证由网关统一处理）
- 通过手动解析 JWT 获取当前用户 ID（从 `HttpServletRequest` 提取 Token → 解析出 userId）

### 2.3 Request DTO（`com.mall.user.dto.request`）

| 类 | 字段 | 校验 |
|----|------|------|
| `UpdateProfileReq` | nickname(String), avatar(String), gender(Integer), birthday(String) | 全部可选 |
| `CreateAddressReq` | receiverName, receiverPhone, province, city, district, detailAddress, zipCode(可选), isDefault(可选), label(可选) | receiverName/receiverPhone 必填 |
| `UpdateAddressReq` | 同 CreateAddressReq（全部可选） | — |

### 2.4 VO 视图对象（`com.mall.user.vo`）

| 类 | 字段 | 说明 |
|----|------|------|
| `UserProfileVO` | userId, nickname, avatar, phone(脱敏), gender(字典值), birthday, memberLevelName, availablePoints | 联合查询 3 表 |
| `AddressVO` | id, receiverName, receiverPhone(脱敏), province, city, district, detailAddress, zipCode, isDefault, label | 默认地址排首位 |
| `MembershipVO` | levelName, levelValue, icon, benefitsJson, growth, nextLevelName, nextMinGrowth, progressPercent | 进度百分比 = growth / nextMinGrowth × 100 |
| `PointsRecordVO` | id, bizType, bizTypeDesc, points, changeType(1增/2减/3过期), beforePoints, afterPoints, remark, createTime | — |
| `GrowthRecordVO` | id, bizType, bizTypeDesc, growth, changeType(1增/2减), beforeGrowth, afterGrowth, remark, createTime | — |

### 2.5 C 端业务 Service（`com.mall.user.service` 子包）

**`service/user/UserService`**：
- `getProfile(userId)`: 联合查询用户表+会员表+积分表，组装 UserProfileVO
- `updateProfile(userId, req)`: 按需更新非空字段
- `findByPhone(phoneHash)`: Feign 远程调用用
- `register(phoneEncrypted, phoneHash, passwordHash)`: 事务中创建用户+初始化会员+积分账户
- `deactivateAccount(userId)`: 设置 user_status=2

**`service/address/AddressService`**：
- `listByUser(userId)`: 默认地址排首位，手机号脱敏
- `create(userId, req)`: 检查 ≤20 条 → 若 isDefault 先取消旧默认 → INSERT
- `update(userId, addressId, req)`: 校验归属 → 更新
- `delete(userId, addressId)`: 校验归属 → 若删默认则自动指定最旧地址为新默认 → 逻辑删除
- `setDefault(userId, addressId)`: 事务中两步操作（先全取消→再设指定）
- `validateAddress(userId, addressId)`: Feign 调用用，校验地址归属

**`service/member/MemberService`**：
- `getMembership(userId)`: 查用户会员 → 查当前等级(含权益) → 查下一级 → 计算进度
- `addGrowth(userId, growth, bizType, bizNo)`: 乐观锁更新成长值 → 检查升降级 → 记录流水

**`service/points/PointsService`**：
- `getPoints(userId)`: 查积分账户
- `listRecords(userId, page, size)`: 积分流水分页
- `addPoints(userId, points, bizType, bizNo)`: 乐观锁累加
- `consumePoints(userId, points, bizNo)`: 校验余额 → 乐观锁扣减

**`service/growth/GrowthService`**：
- `getGrowth(userId)`: 查当前成长值+下一级进度
- `listRecords(userId, page, size)`: 成长值流水分页

### 2.6 Convert 转换层（`com.mall.user.convert`）

| 类 | 方法 | 说明 |
|----|------|------|
| `UserConvert` | `toProfileVO(MallUser, MallUserMember, MallUserPointsAccount)` | 组合多表数据 |
| `AddressConvert` | `toAddressVO(MallUserAddress)` | DO → VO，手机号脱敏 |
| `MemberConvert` | `toMembershipVO(MallUserMember, MemberLevel current, MemberLevel next)` | 含进度计算 |
| `PointsConvert` | `toRecordVO(MallUserPointsLog)` | bizType → bizTypeDesc 映射 |
| `GrowthConvert` | `toRecordVO(MallUserGrowthLog)` | 同上 |

### 2.7 Feign 适配器（`com.mall.user.infrastructure.feign`）

**`RemoteAuthAdapter`**：封装调 mall-auth 的解密调用
- `decryptPhone(String encryptedPhone)`: 调 `RemoteAuthService.decrypt()` 返回明文
- `batchDecryptPhone(List<String>)`: 批量解密

### 2.8 新增 Mapper XML 方法

在现有 Mapper XML 中追加以下 SQL：

**MallUserMapper.xml 新增**：
- `selectByIdInternal(Long id)`: 不检查 is_deleted=0（Feign 内部调用需要）
- `updateUserStatus(@Param("userId") Long id, @Param("status") Integer status)`: 更新状态
- `selectByPhoneHash(String phoneHash)`: 按 phone_hash 精确查询
- `updatePassword(@Param("userId") Long id, @Param("password") String passwordHash)`
- `updatePhone(@Param("userId") Long id, @Param("phone") String encrypted, @Param("phoneHash") String hash)`

**MallUserAddressMapper.xml 新增**：
- `selectByUserId(String userId)`: 按用户 ID 查地址列表（is_deleted=0, is_default DESC）
- `selectCountByUserId(String userId)`: 计数
- `clearDefaultByUserId(String userId)`: 取消用户所有地址的默认状态
- `selectOldestByUserId(String userId)`: 查最早创建的地址（删除默认时用）
- `validateBelongsToUser(@Param("addressId") Long id, @Param("userId") Long userId)`: 校验归属

**MallUserMemberMapper.xml 新增**：
- `selectByUserId(String userId)`: 按用户 ID 查会员信息
- `updateGrowth(@Param("userId") Long userId, @Param("growth") Long growth, @Param("totalGrowth") Long totalGrowth, @Param("version") Long version)`: 乐观锁更新成长值
- `updateLevel(@Param("userId") Long userId, @Param("levelId") Long levelId)`: 更新会员等级

**MallUserPointsAccountMapper.xml 新增**：
- `selectByUserId(String userId)`: 按用户 ID 查积分账户
- `updateAvailablePoints(...)`: 乐观锁更新可用积分

**MallUserPointsLogMapper.xml / MallUserGrowthLogMapper.xml 新增**：
- `selectListByUserId(@Param("userId") String userId, ...)`: 按用户 ID 分页查询（is_deleted=0）

**MallUserMemberLevelMapper.xml 新增**：
- `selectNextLevel(Long currentLevelValue)`: 查下一个等级的级别信息
- `selectAll()`: 查全部分级列表

## 三、错误码处理

遵循系统设计第二章错误码体系，C 端 API 统一返回 `AjaxResult`。错误码以 `ErrorCode` 枚举（位于 `com.mall.api.enums`）统一管理，Service 层通过 `ServiceException`（ruoyi-common-core 内置）抛出，由 `GlobalExceptionHandler`（ruoyi-common-security 内置）转换为响应。

| errorCode | HTTP | userTip | 场景 |
|-----------|:----:|---------|------|
| 00000 | 200 | — | 成功 |
| A0301 | 401 | 请先登录 | 未登录 |
| A0401 | 400 | 请完整填写信息 | 必填参数为空 |
| A0402 | 400 | 参数格式错误 | 参数格式错误 |
| A0501 | 404 | 资源不存在 | 资源不存在（地址/用户不存在） |
| A0511 | 400 | 地址数量已达上限 | 地址数量超过上限 |

## 四、实施顺序

1. mall-api：5 个枚举（+ErrorCode）+ 2 个 DTO + 2 个 Feign 接口
2. mall-user 包结构调整：admin 子包 + api 子包 + vo/dto/convert/infrastructure 包
3. 管理端 Controller 迁移（7 个文件改包 + 改路径）
4. 网关路由配置（ruoyi-gateway：更新管理端 7 条路由 + 新增 C 端 5 条路由）
5. 新增 Mapper XML SQL
6. Convert + VO + Request DTO 转换层实现
7. C 端业务 Service 实现（5 个子域）
8. C 端 Controller 实现（5 个）
9. 构建验证 `mvn clean install -f server/mall/pom.xml -DskipTests`
