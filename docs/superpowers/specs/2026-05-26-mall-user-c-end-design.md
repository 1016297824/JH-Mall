# mall-user C 端 API 完整开发设计

> 日期：2026-05-26
> 基线：`docs/design/08_mall-user详细设计.md` / `03_系统详细设计.md` §1.1 + §3.2
> 前置 spec：`2026-05-23-mall-auth-mvp-design.md`（mall-user 内部 Feign 端点已完成）

## 1. 范围

| 类别 | 内容 |
|------|------|
| C 端 API | 13 个接口（profile×2 / address×5 / membership×1 / points×2 / growth×2 / signin×1） |
| 签到 | Redis Bitmap，幂等，基础5分 + 连续递增加成（上限10） |
| 积分过期 | 年末清零定时任务（12月31日 00:00） |
| 会员升级 | MQ 事件驱动（消费 `mall:order:completed`） |
| 代码质量 | Service 构造注入改造 + 魔法值消除 + ErrorCodeConstants + RemoteAuthAdapter |

## 2. 包结构

```
server/mall/mall-user/src/main/java/com/mall/user/
├── MallUserApplication.java                  # 已有，不变
├── config/
│   └── MallUserConfigProperties.java         # 改造：追加签到配置项
│
├── domain/     # 7 个实体，不变
├── mapper/     # 7 个 Mapper，追加少量 C 端查询方法
│
├── service/                                   # 管理端 Service（已有）
│   ├── IMallUserService.java / impl/          # 改造：@Autowired → 构造注入
│   ├── IMallUserAddressService.java / impl/   # 改造：同上
│   ├── IMallUserMemberService.java / impl/    # 改造：同上
│   ├── IMallUserMemberLevelService.java / impl/
│   ├── IMallUserPointsAccountService.java / impl/
│   ├── IMallUserPointsLogService.java / impl/
│   └── IMallUserGrowthLogService.java / impl/
│
├── service/customer/                          # 🆕 C 端专用 Service
│   ├── UserProfileService                    # 资料查询+修改（含Redis缓存）
│   ├── AddressBookService                    # 地址CRUD+默认管理+上限检查
│   ├── MemberService                         # 会员等级查询+成长值操作+升级判断
│   ├── PointsService                         # 积分查询+流水分页+增减+签到
│   └── SignInService                         # 签到Bitmap+连续签到计算
│
├── controller/api/                            # 🆕 C 端 Controller
│   ├── UserProfileController                 # GET/PUT /api/user/profile
│   ├── AddressController                     # GET/POST/PUT/DELETE /api/user/addresses
│   ├── MemberController                      # GET /api/user/membership
│   ├── PointsController                      # GET /api/user/points + /points/records
│   ├── GrowthController                      # GET /api/user/growth + /growth/records
│   └── SignInController                      # POST /api/user/sign-in
│
├── infrastructure/feign/
│   └── RemoteAuthAdapter                     # 🆕 封装 mall-auth 手机号解密
│
├── mq/consumer/
│   └── UserOrderCompletedConsumer            # 🆕 消费 mall:order:completed
│
└── schedule/
    └── PointsExpireTask                      # 🆕 年末积分清零
```

```
server/mall/mall-common/src/main/java/com/mall/common/
├── constant/
│   └── ErrorCodeConstants.java              # 🆕 ErrorCode 常量引用
└── enums/user/
    └── BizTypeEnum.java                     # 🆕 积分/成长值共用业务类型
```

## 3. 接口契约

### 3.1 个人资料

| # | 方法 | 路径 | 认证 | 说明 |
|:-:|------|------|:---:|------|
| 1 | `GET` | `/api/user/profile` | ✅ | 获取个人资料（脱敏），含会员+积分+成长值汇总 |
| 2 | `PUT` | `/api/user/profile` | ✅ | 修改资料（nickname/avatar/gender/birthday/email 均为可选） |

**Response VO**：`UserProfileVO` — userId, nickname, avatar, gender, genderName, birthday, phone(脱敏中间4位), email, membershipLevel(名称+图标), growth, totalGrowth, points, availablePoints

### 3.2 地址簿

| # | 方法 | 路径 | 说明 |
|:-:|------|------|------|
| 3 | `GET` | `/api/user/addresses` | 列表（默认排首位） |
| 4 | `POST` | `/api/user/addresses` | 新增（上限20条，超限抛 `A0511`） |
| 5 | `PUT` | `/api/user/addresses/{addressId}` | 修改（校验归属） |
| 6 | `DELETE` | `/api/user/addresses/{addressId}` | 删除（校验归属） |
| 7 | `PUT` | `/api/user/addresses/{addressId}/default` | 设默认（原子操作） |

**Response VO**：`AddressVO` — addressId, receiverName, receiverPhone(脱敏), province, city, district, detailAddress, zipCode, isDefault, label

### 3.3 会员

| # | 方法 | 路径 | 说明 |
|:-:|------|------|------|
| 8 | `GET` | `/api/user/membership` | 会员等级+权益+成长值进度 |

**Response VO**：`MembershipVO` — currentLevel(名称+图标+levelValue), growth, totalGrowth, nextLevel(名称+所需成长值), benefits(List)

### 3.4 积分

| # | 方法 | 路径 | 说明 |
|:-:|------|------|------|
| 9 | `GET` | `/api/user/points` | 积分余额 |
| 10 | `GET` | `/api/user/points/records?page=1&size=20&bizType=signin` | 流水分页，bizType 可选筛选 |

**Response VO**：
- `PointsVO` — totalPoints, availablePoints, usedPoints, expiredPoints
- `PointsRecordVO` — id, bizType, bizTypeName, changeType(1增/2减), points, beforePoints, afterPoints, remark, createTime

### 3.5 成长值

| # | 方法 | 路径 | 说明 |
|:-:|------|------|------|
| 11 | `GET` | `/api/user/growth` | 成长值余额+下一级进度 |
| 12 | `GET` | `/api/user/growth/records?page=1&size=20&bizType=order` | 流水分页，bizType 可选筛选 |

**Response VO**：
- `GrowthVO` — growth, totalGrowth, currentLevel(名称+图标), nextLevel(名称+所需成长值), progressPercent
- `GrowthRecordVO` — id, bizType, bizTypeName, changeType, growth, beforeGrowth, afterGrowth, remark, createTime

### 3.6 签到

| # | 方法 | 路径 | 说明 |
|:-:|------|------|------|
| 13 | `POST` | `/api/user/sign-in` | 当日签到 |

**Response VO**：`SignInVO` — todayPoints, consecutiveDays, signInCalendar(List\<Integer\>: 当月已签到日期列表)

**幂等**：重复签到返回 `A0502 "今日已签到"`，不重复加积分。

## 4. 数据流

### 4.1 用户资料

```
网关 MallAuthFilter(-150) → 解析 JWT → 注入 X-User-Id header
                                                │
GET /api/user/profile                           ▼
  UserProfileController.getProfile()       从 X-User-Id 取 userId
    ↓
  UserProfileService.getProfile(userId)
    ① Redis GET mall:user:profile:{userId}
       ├─ 命中 → 直接返回
       └─ 未命中 ↓
    ② DB: mall_user LEFT JOIN mall_user_member
       ├─ userStatus = FROZEN/DELETED → throw A0503
       └─ 正常 → 组装 VO → Redis SETEX(TTL=600s) → 返回

PUT /api/user/profile
  UserProfileService.updateProfile(userId, request)
    ① UPDATE mall_user SET (可选字段)
    ② DEL mall:user:profile:{userId}   # 删除缓存
    ③ 重新调用 getProfile(userId) → 返回最新数据
```

### 4.2 地址簿

```
GET    /api/user/addresses               SELECT WHERE user_id=? ORDER BY is_default DESC
POST   /api/user/addresses               先 COUNT 检查上限 → INSERT
PUT    /api/user/addresses/{id}          SELECT 校验归属 → 校验通过 → UPDATE
DELETE /api/user/addresses/{id}          SELECT 校验归属 → DELETE
PUT    /api/user/addresses/{id}/default  BEGIN → UPDATE SET is_default=0 WHERE user_id=? → UPDATE SET is_default=1 WHERE id=? → COMMIT
```

### 4.3 签到

```
POST /api/user/sign-in
  SignInService.signIn(userId)
    ① Key: mall:user:sign:{userId}:{yyyyMM}
    ② GETBIT key today
       └─ =1 → throw A0502 "今日已签到"
    ③ BITCOUNT key → 当月签到总天数
    ④ 计算连续天数（从 today-1 倒推 BITFIELD GET）
    ⑤ 积分 = min(5 + 连续天数 - 1, 10)
    ⑥ SETBIT key today 1 → EXPIRE key 60天
    ⑦ PointsService.addPoints(userId, points, SIGN_IN, null)
       ├─ UPDATE mall_user_points_account SET version=version+1 WHERE user_id=? AND version=?
       │   (乐观锁，失败重试3次)
       ├─ INSERT mall_user_points_log
       └─ 返回 {todayPoints, consecutiveDays, calendar}
```

### 4.4 会员升级（MQ）

```
MQ Consumer: UserOrderCompletedConsumer
  消费 topic: mall:order:completed
  消息体: {userId, orderNo, orderAmount, points}
  │
  ├─ ① PointsService.addPoints(userId, points, ORDER, orderNo)
  │
  └─ ② MemberService.addGrowth(userId, growth=orderAmount/100, ORDER, orderNo)
        ├─ UPDATE mall_user_member SET growth=growth+?, total_growth=total_growth+?
        ├─ INSERT mall_user_growth_log
        └─ ③ 查询当前等级 → 查询所有等级 ORDER BY level_value
            遍历，找到 growth 所属区间
            └─ level_id 变化 → UPDATE mall_user_member SET level_id=?, level_start_time=NOW()
```

### 4.5 积分过期

```
@Scheduled(cron = "0 0 0 31 12 ?")
PointsExpireTask.execute()
  ① SELECT user_id, available_points FROM mall_user_points_account WHERE available_points > 0
  ② 分批处理（每批500条）：
     FOR EACH record:
       ├─ UPDATE available_points=0, expired_points=expired_points+?, version=version+1
       │   WHERE user_id=? AND version=?
       ├─ INSERT mall_user_points_log (biz_type=EXPIRE, change_type=DECREASE)
       └─ 写入 mall.mall_user_audit_log (审计)
```

## 5. 错误码

| 常量 | ErrorCode | msg | userTip |
|------|-----------|-----|---------|
| `ErrorCodeConstants.User.PHONE_EXISTS` | `A0151` | 手机号已存在 | 手机号已注册 |
| `ErrorCodeConstants.User.EMAIL_EXISTS` | `A0154` | 邮箱已存在 | 邮箱已注册 |
| `ErrorCodeConstants.User.ACCOUNT_NOT_FOUND` | `A0201` | 用户不存在 | 用户不存在 |
| `ErrorCodeConstants.User.ACCOUNT_FROZEN` | `A0202` | 用户已冻结 | 账户已冻结 |
| `ErrorCodeConstants.User.ACCOUNT_DELETED` | `A0203` | 用户已注销 | 账户已注销 |
| `ErrorCodeConstants.User.NOT_AUTHENTICATED` | `A0301` | 未登录 | 请先登录 |
| `ErrorCodeConstants.User.RESOURCE_NOT_FOUND` | `A0501` | 资源不存在 | 资源不存在 |
| `ErrorCodeConstants.User.RESOURCE_EXISTS` | `A0502` | 已签到 | 今日已签到 |
| `ErrorCodeConstants.User.RESOURCE_STATUS_ERROR` | `A0503` | 资源状态错误 | 账户状态异常 |
| `ErrorCodeConstants.User.ADDRESS_LIMIT` | `A0511` | 地址数量超限 | 最多添加20个地址 |

## 6. 缓存 Key & MQ Topic

| 类型 | 常量 | 值 | TTL |
|------|------|----|:--:|
| Redis | `CacheConstants.User.PROFILE` | `mall:user:profile:{userId}` | 600s（可配） |
| Redis | `CacheConstants.User.SIGN` | `mall:user:sign:{userId}:{yyyyMM}` | 60天 |
| MQ | `MqTopicConstants.User.REGISTERED` | `mall:user:registered` | — |
| MQ | `MqTopicConstants.Order.COMPLETED` | `mall:order:completed` | — |

> `MqTopicConstants.Order.COMPLETED` 需在 mall-common 中新增。

## 7. 新增枚举

### BizTypeEnum（`com.mall.common.enums.user`）

| code | name | 适用场景 |
|------|------|---------|
| `order` | 下单赠送 | 积分 + 成长值 |
| `signin` | 签到 | 积分 + 成长值 |
| `review` | 评价 | 积分 + 成长值 |
| `refund` | 退款扣除 | 积分 |
| `admin` | 管理员调整 | 积分 + 成长值 |
| `expire` | 积分过期 | 积分 |

## 8. Nacos 配置变更

在 `mall-user-dev.yml` 的 `mall.user.points` 段追加：

```yaml
mall:
  user:
    points:
      signin-base: 5              # 签到基础积分（已有）
      signin-consecutive: 10       # 签到积分上限（已有）
      signin-consecutive-bonus: 1  # 🆕 每连续一天额外加积分
      review: 10                   # 评价积分（已有）
      review-with-photo: 20        # 带图评价积分（已有）
```

> 仅新增 `signin-consecutive-bonus`，其余为已有配置。

## 9. 依赖新增

- **RocketMQ 客户端**：引入 `org.apache.rocketmq:rocketmq-spring-boot-starter:2.4.2`
  - Docker 已部署 RocketMQ 5.5.0 单容器（`deploy/docker/docker-compose.yml`，端口 9876/10911/10909）
  - mall-user 仅需**消费者**能力（消费 `mall:order:completed`）
- Spring Boot 自带 `@EnableScheduling`（无需额外依赖），需在 `MallUserApplication` 上加注解

## 10. 自查

| 检查项 | 状态 |
|--------|:--:|
| 无 TBD/TODO 占位符 | ✅ |
| 接口与设计文档 `03_系统详细设计.md` §3.2 一致 | ✅ |
| 错误码与 `ErrorCode` 枚举对齐 | ✅ |
| 缓存 Key 与 `CacheConstants` 约定一致 | ✅ |
| 范围未膨胀（无额外需求） | ✅ |
| 未引入循环依赖 | ✅ |
