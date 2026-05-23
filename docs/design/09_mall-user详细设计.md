# JH-Store mall-user 模块详细设计

> 基于系统详细设计 `03_系统详细设计.md` 展开。数据表 DDL 在系统设计 1.1 节统一维护，此处只引用表名和字段。

---

## 1 模块概述

### 1.1 子领域

| 子领域 | 实体 | 说明 |
|--------|------|------|
| 用户账号 | `mall_user` | 基本信息（加密手机号/密码哈希/昵称/头像/状态） |
| 会员等级 | `mall_user_member_level` | 等级定义（成长值区间/折扣率/包邮/积分倍数） |
| 用户会员 | `mall_user_member` | 用户当前等级+成长值，与用户 1:1 |
| 地址簿 | `mall_user_address` | 收货地址（最多 20 条），手机号加密存储 |
| 积分账户 | `mall_user_points_account` | 积分余额+累计，与用户 1:1 |
| 积分流水 | `mall_user_points_log` | 积分变动记录（获取/消耗/过期/调整） |
| 成长值流水 | `mall_user_growth_log` | 成长值变动记录（获取/消耗） |

### 1.2 依赖关系

```
mall-user (9302端口)
  ├── MySQL：自有表（见表系统设计第 1.1 节）
  ├── Redis：用户资料缓存
  ├── mall-auth (Feign Client)：调解密手机号
  ├── mall-auth (Feign Caller)：用户注册/登录时调 mall-user CRUD
  └── mall-api (Feign)：RemoteUserService 供 mall-auth/mall-order 调用
```

> **关键约束**：手机号/邮箱 AES-256-GCM 加密存储，等值查询通过 SHA256 哈希辅助列。mall-user 是用户数据唯一持有方，其他服务通过 Feign 调 RemoteUserService 操作。

---

## 2 包结构与接口映射

### 2.1 包结构

```
server/mall/mall-user/
└── src/main/java/com/mall/user/
    ├── MallUserApplication.java            # Spring Boot 启动类
    ├── controller/
    │   ├── admin/
    │   │   ├── UserAdminController.java    # /mall-user/users/**
    │   │   ├── MemberAdminController.java  # /mall-user/members/**
    │   │   └── PointsAdminController.java  # /mall-user/points/**
    │   └── api/
    │       ├── ProfileController.java      # /api/user/profile
    │       ├── AddressController.java      # /api/user/addresses/**
    │       ├── MembershipController.java   # /api/user/membership
    │       ├── PointsController.java       # /api/user/points/**
    │       └── GrowthController.java       # /api/user/growth/**
    ├── dto/
    │   ├── request/                        → UpdateProfileReq, CreateAddressReq, AdjustPointsReq,
    │   │                                      CreateMemberLevelReq ...
    │   └── response/                       → PointsResp, GrowthResp
    ├── vo/                                  # 视图对象，前端展示（脱敏/单位转换）
    │   ├── UserProfileVO.java              # C 端资料（手机号/邮箱脱敏）
    │   ├── AddressVO.java                  # 地址展示（手机号脱敏）
    │   ├── MembershipVO.java               # 会员等级+权益+进度
    │   ├── PointsRecordVO.java             # 积分流水单条
    │   └── UserAdminVO.java               # 管理端用户详情（全字段）
    ├── domain/
    │   ├── MallUser.java                   # 对应 mall_user 表
    │   ├── MallMemberLevel.java            # 对应 mall_user_member_level 表
    │   ├── MallUserMember.java             # 对应 mall_user_member 表
    │   ├── MallUserAddress.java            # 对应 mall_user_address 表
    │   ├── MallPointsAccount.java          # 对应 mall_user_points_account 表
    │   ├── MallPointsLog.java              # 对应 mall_user_points_log 表
    │   └── MallGrowthLog.java              # 对应 mall_user_growth_log 表
    ├── service/
    │   ├── user/
    │   │   ├── UserService.java
    │   │   └── impl/UserServiceImpl.java   # 用户 CRUD + 状态管理
    │   ├── address/
    │   │   ├── AddressService.java
    │   │   └── impl/AddressServiceImpl.java
    │   ├── member/
    │   │   ├── MemberService.java
    │   │   └── impl/MemberServiceImpl.java # 会员等级+成长值
    │   ├── points/
    │   │   ├── PointsService.java
    │   │   └── impl/PointsServiceImpl.java # 积分账户+流水
    │   └── growth/
    │       ├── GrowthService.java
    │       └── impl/GrowthServiceImpl.java # 成长值流水
    ├── mapper/
    │   ├── MallUserMapper.java
    │   ├── MallMemberLevelMapper.java
    │   ├── MallUserMemberMapper.java
    │   ├── MallUserAddressMapper.java
    │   ├── MallPointsAccountMapper.java
    │   ├── MallPointsLogMapper.java
    │   └── MallGrowthLogMapper.java
    ├── infrastructure/
    │   └── feign/
    │       └── RemoteAuthAdapter.java       # 调 mall-auth 解密手机号
    └── convert/
        ├── UserConvert.java                # MallUser → UserProfileVO / UserAdminVO
        ├── AddressConvert.java             # MallUserAddress → AddressVO
        ├── MemberConvert.java              # MallUserMember → MembershipVO
        ├── PointsConvert.java              # MallPointsLog → PointsRecordVO
        └── GrowthConvert.java
```

### 2.2 接口 → Controller 映射

| # | 方法 | 路径 | Controller | 方法名 | 需登录 | 权限码 |
|---|------|------|-----------|--------|:---:|--------|
| 1 | GET | `/api/user/profile` | ProfileController | `getProfile()` | 是 | — |
| 2 | PUT | `/api/user/profile` | ProfileController | `updateProfile(req)` | 是 | — |
| 3 | GET | `/api/user/addresses` | AddressController | `list()` | 是 | — |
| 4 | POST | `/api/user/addresses` | AddressController | `create(req)` | 是 | — |
| 5 | PUT | `/api/user/addresses/{addressId}` | AddressController | `update(addressId, req)` | 是 | — |
| 6 | DELETE | `/api/user/addresses/{addressId}` | AddressController | `delete(addressId)` | 是 | — |
| 7 | PUT | `/api/user/addresses/{addressId}/default` | AddressController | `setDefault(addressId)` | 是 | — |
| 8 | GET | `/api/user/membership` | MembershipController | `getMembership()` | 是 | — |
| 9 | GET | `/api/user/points` | PointsController | `getPoints()` | 是 | — |
| 10 | GET | `/api/user/points/records` | PointsController | `listRecords(params)` | 是 | — |
| 11 | GET | `/api/user/growth` | GrowthController | `getGrowth()` | 是 | — |
| 12 | GET | `/api/user/growth/records` | GrowthController | `listRecords(params)` | 是 | — |

管理端接口由若依代码生成器自动生成（用户/会员/积分 CRUD + 冻结解冻、积分调整），权限码无需手动维护。

---

## 3 核心类设计

### 3.1 UserServiceImpl

位于 `service/user/impl/UserServiceImpl.java`，用户账号管理。

**getProfile(userId)**：
- 查 `mall_user` + `mall_user_member` + `mall_user_points_account` LEFT JOIN
- 手机号/邮箱脱敏后返回（通过 Jackson `@Sensitive` 注解）
- 组合为 `UserProfileVO` 返回

**updateProfile(userId, req)**：更新昵称/头像/性别/生日，全部可选

**updateStatus(userId, userStatus)**：管理端冻结/解冻。已注销用户不可操作 → `A0503`

**findByPhone(phoneHash)**（Feign 接口，供 mall-auth 调用）：
- 查询：`WHERE phone_hash = SHA2(CONCAT(?, salt), 256)`
- 返回 `MallUser`（含密码哈希、状态），用于登录校验

**register(phoneEncrypted, phoneHash, passwordHash)**（Feign 接口，供 mall-auth 调用）：
- 插入 `mall_user` + 初始化 `mall_user_member` + `mall_user_points_account`
- 手机号唯一约束：`uk_phone_hash`

**Deactivate(userId)**：更新 `user_status=2`（注销），不物理删除

### 3.2 AddressServiceImpl

位于 `service/address/impl/AddressServiceImpl.java`，地址簿管理。

- `list(userId)`：查默认地址排在首位，phone 脱敏返回
- `create(userId, req)`：地址数检查（≤20），`is_default=true` 时先取消其他默认
- `update(userId, addressId, req)`：校验地址归属
- `delete(userId, addressId)`：若删除默认地址，自动指定最早创建的地址为默认
- `setDefault(userId, addressId)`：原子操作，先取消旧默认再设新默认（同一事务）

### 3.3 MemberServiceImpl

位于 `service/member/impl/MemberServiceImpl.java`，会员成长值。

**getMembership(userId)**：查 `mall_user_member` + `mall_user_member_level`，返回当前等级、权益、到下一级的进度

**addGrowth(userId, growth, bizType, bizNo)**（Feign 接口，消费 `mall:user:registered` 事件时调用）：
- `UPDATE mall_user_member SET growth=growth+#{growth}, total_growth=total_growth+#{growth}`
- 判断是否升级：查当前等级 `max_growth`，超则升到下一级
- 记录 `mall_user_growth_log`

**expireGrowth()**（定时任务）：每年 1 月 1 日扣除上上年的成长值（预留，初期不做）

### 3.4 PointsServiceImpl

位于 `service/points/impl/PointsServiceImpl.java`，积分管理。

**addPoints(userId, points, bizType, bizNo)**：
- `UPDATE mall_user_points_account SET available_points=available_points+#{points}, total_points=total_points+#{points} WHERE user_id=? AND version=?`
- 乐观锁防并发
- 记录 `mall_user_points_log`（change_type=1）

**consumePoints(userId, points, bizNo)**：
- 校验 `available_points >= points`
- `UPDATE ... SET available_points=available_points-#{points}, used_points=used_points+#{points}`
- 记录流水（change_type=2）

**expirePoints()**（定时任务）：扫描有效期到期的积分，自动扣减

**adjustPoints(userId, points, reason)**：管理端手动调整，审计日志记录操作人

---

## 4 会员成长值设计

### 4.1 成长值获取

| 来源 | 成长值 | 说明 |
|------|:---:|------|
| 订单完成 | 订单金额/100 | 订单确认收货后发放 |
| 每日签到 | 5~10 | 连续签到递增 |
| 评价商品 | 10 | 带图+20 |

### 4.2 升级/降级

| 操作 | 时机 | 说明 |
|------|------|------|
| 升级 | 成长值达到上级 `min_growth` | 实时触发，消费积分/成长值事件后检查 |
| 降级 | 年度到期扣除后低于本级 `min_growth` | 每年 1 月 1 日执行（初期不做） |

### 4.3 会员权益

| 等级 | 折扣率 | 包邮 | 积分倍数 |
|------|:---:|:---:|:---:|
| 普通会员 | 100% | 否 | 1.0x |
| 银卡会员 | 98% | 满 99 包邮 | 1.1x |
| 金卡会员 | 95% | 包邮 | 1.2x |
| 钻石会员 | 90% | 包邮 | 1.5x |

> 会员权益在 `mall_user_member_level.benefits_json` 中以 JSON 存储，前端根据此字段展示权益明细。

---

## 5 手机号加密存储

### 5.1 加密方案

遵循系统设计 7.1 节：

| 方面 | 方案 |
|------|------|
| 算法 | AES-256-GCM（认证加密） |
| 密钥持有 | mall-auth（Nacos `mall.security.aes-key`） |
| 查询方式 | SHA256 哈希辅助列 `phone_hash`（等值匹配） |
| 解密 | mall-user 调 `RemoteAuthAdapter.decrypt(encryptedPhone)` |

### 5.2 解密调用链

```
mall-user 需要展示手机号
  → 调 RemoteAuthAdapter.decrypt(encryptedPhone)
    → mall-auth 用 Nacos 密钥解密
    → 返回明文 → 脱敏后返回前端
```

管理端用户详情可查看完整手机号（权限控制），C 端始终脱敏展示。

---

## 6 Nacos 配置

### 6.1 DataId: `mall-user-dev.yml`

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
  typeAliasesPackage: com.mall.user.**.domain
  mapperLocations: classpath:mapper/**/*.xml

# springdoc配置
springdoc:
  gatewayUrl: http://localhost:8080/${spring.application.name}
  api-docs:
    enabled: true
  info:
    title: '用户模块接口文档'
    description: '用户模块接口描述'
    contact:
      name: RuoYi
      url: https://ruoyi.vip

# mall-user 业务配置
mall:
  user:
    address:
      max-count: 20
    profile:
      cache-ttl: 600
    member:
      default-level: 1
    points:
      signin-base: 5
      signin-consecutive: 10
      review: 10
      review-with-photo: 20
```

> 以上配置通过 Nacos 下发，支持 `@RefreshScope` 运行时动态刷新。

### 6.2 本地配置文件 `bootstrap.yml`

```yaml
# mall-user 用户服务
server:
  port: 9302

spring:
  application:
    name: mall-user
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

### 6.3 配置项说明

| 配置项 | 默认值 | 单位 | 说明 |
|--------|--------|:---:|------|
| `mall.user.address.max-count` | 20 | 条 | 地址簿上限 |
| `mall.user.profile.cache-ttl` | 600 | 秒 | 用户资料缓存时间 |
| `mall.user.member.default-level` | 1 | — | 新用户默认会员等级 |
| `mall.user.points.signin-base` | 5 | 分 | 每日签到基础积分 |
| `mall.user.points.signin-consecutive` | 10 | 分 | 连续签到上限积分 |
| `mall.user.points.review` | 10 | 分 | 评价奖励积分 |
| `mall.user.points.review-with-photo` | 20 | 分 | 带图评价积分 |

---

## 7 错误码汇总

| 错误码 | HTTP | userTip | 说明 |
|--------|:----:|---------|------|
| 00000 | 200 | — | 成功 |
| A0301 | 401 | 请先登录 | 未登录 |
| A0320 | 403 | 无权限访问 | 权限不足 |
| A0401 | 400 | 请完整填写信息 | 必填参数为空 |
| A0402 | 400 | 参数格式错误 | 参数格式不符合要求 |
| A0501 | 404 | 资源不存在 | 用户/地址/成员不存在 |
| A0502 | 400 | 资源已存在 | 等级数值已存在 |
| A0503 | 400 | 资源不可操作 | 用户已注销不可操作 |
| A0511 | 400 | 地址数量已达上限 | 地址簿已满 20 条 |
| B0001 | 500 | 系统繁忙，请稍后再试 | 未预期异常 |
| C0110 | 500 | 服务暂时不可用 | Redis 连接失败 |
| C0120 | 500 | 数据库异常 | MySQL 连接失败 |

> 全部错误码来自系统设计第二章。

---
