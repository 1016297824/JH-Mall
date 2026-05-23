# JH-Store mall-api 契约层设计

> mall-api 是跨服务共享契约模块（非独立部署），定义 Feign 接口、共享 DTO、枚举。所有 `server/mall/mall-*` 服务通过 `mall-api` 进行服务间调用。
> 依据概要设计 `02_系统概要设计_补充.md` 第 2 章包结构规划。

---

## 1 模块概述

### 1.1 职责

| 层 | 内容 | 约束 |
|----|------|------|
| Feign 接口 | `Remote*Service` 7 个接口 | 只定义接口签名 + `@FeignClient` + fallback |
| 共享 DTO | 请求/响应 DTO | 不含 VO、不含数据库实体 |
| 共享枚举 | 跨服务必须一致的枚举 | 如订单状态、支付状态、优惠券状态 |
| C 端响应体 | `MallResult<T>` | 所有 C 端模块统一使用，替代若依 `AjaxResult` |

**禁止放入 mall-api 的：** VO（视图对象）、数据库实体（DO/domain）、Mapper、Service 实现、constant 常量类。

### 1.2 依赖关系

```
mall-api (共享库)
  ├── 被依赖方：所有 mall-* 服务通过 maven 依赖引入
  ├── 依赖：ruoyi-common（复用若依返回结构、异常、Feign 基础配置）
  └── 独立部署：否（JAR 包供各服务引用）
```

---

## 2 包结构

```
server/mall/mall-api/
└── src/main/java/com/mall/api/
    ├── feign/                                # Feign 远程调用接口
    │   ├── RemoteUserService.java            # 用户服务（提供方：mall-user）
    │   ├── RemoteProductService.java         # 商品服务（提供方：mall-product）
    │   ├── RemoteOrderService.java           # 订单服务（提供方：mall-order）
    │   ├── RemotePaymentService.java         # 支付服务（提供方：mall-payment）
    │   ├── RemoteMarketingService.java       # 营销服务（提供方：mall-marketing）
    │   ├── RemoteSearchService.java          # 搜索服务（提供方：mall-search）
    │   └── RemoteAuthService.java            # 认证服务（提供方：mall-auth）
    ├── dto/                                  # 跨服务共享 DTO（按域分包）
    │   ├── user/                             → AddressDTO ...
    │   ├── product/                          → ProductSkuDTO, SpuDTO ...
    │   ├── order/                            → OrderDTO ...
    │   ├── payment/                          → RefundDTO, RefundResultDTO ...
    │   ├── marketing/                        → CalculationReq, CalculationResp ...
    │   └── search/                           → ProductIndex ...
    └── dto/
        ├── MallResult.java                  # C 端统一响应体（替代若依 AjaxResult）
        └── MallUserDTO.java                 # 用户数据传输对象（userId 为 String）
    └── enums/                                # 跨服务必须共享的枚举
        ├── OrderStatusEnum.java              # WAIT_PAY/PAID/WAIT_DELIVER/WAIT_RECEIVE/COMPLETED/CANCELLED/CLOSED/REFUNDING/REFUNDED
        ├── PaymentStatusEnum.java            # UNPAID/PAID/FAILED/CLOSED/REFUNDING/REFUNDED
        ├── RefundStatusEnum.java             # PROCESSING/SUCCESS/FAILED
        ├── CouponRecordStatusEnum.java       # AVAILABLE/LOCKED/USED/RELEASED/EXPIRED
        ├── CouponTypeEnum.java               # FULL_REDUCE/DISCOUNT/NO_THRESHOLD
        ├── PromotionTypeEnum.java            # FULL_REDUCE/FULL_DISCOUNT/FREE_SHIPPING/SECKILL
        └── UserStatusEnum.java               # NORMAL/FROZEN/DELETED
```

---

## 3 Feign 接口定义

### 3.1 RemoteUserService

**提供方**：`mall-user`（9302）  
**调用方**：`mall-auth`（登录/注册）、`mall-order`（校验地址）

| 方法 | 参数 | 返回 | 说明 |
|------|------|------|------|
| `register` | `RegisterRequest`(phone, phoneHash, password, registerType) | `String userId` | 注册新用户，同时初始化会员+积分账户 |
| `findByPhone` | `phone`(String) | `MallUserDTO` | 按手机号查用户，供 mall-auth 登录校验 |
| `findByWechatOpenId` | `openId`(String) | `MallUserDTO` | 按微信 openId 查用户 |
| `registerByWechat` | `openId`(String), `unionId`(String) | `String userId` | 微信首次授权自动注册 |
| `updatePassword` | `userId`(String), `PasswordUpdateRequest`(newPassword) | `void` | 更新密码哈希（改密/重置密码） |
| `updatePhone` | `userId`(String), `PhoneUpdateRequest`(newPhone, newPhoneHash) | `void` | 换绑手机号 |
| `deactivateAccount` | `userId`(String) | `void` | 注销账号（userStatus→2） |
| `validateAddress` | `userId`(String), `addressId`(Long) | `boolean` | 校验收货地址归属，供 mall-order 下单时校验 |

> 请求对象 RegisterRequest / PasswordUpdateRequest / PhoneUpdateRequest 定义为 RemoteUserService 内部类。

### 3.2 RemoteAuthService

**提供方**：`mall-auth`（9301）  
**调用方**：`mall-order`（解密手机号）、`mall-user`（管理端查手机号）

| 方法 | 参数 | 返回 | 说明 |
|------|------|------|------|
| `decrypt` | `encryptedData`(String, AES-256-GCM密文) | `String` 明文 | 解密单条，结果缓存 Redis 1min |
| `batchDecrypt` | `encryptedDataList`(List\<String\>, 最多50条) | `List\<String\>` 明文列表 | 批量解密，超时 2s，失败重试 1 次 |

### 3.3 RemoteProductService

**提供方**：`mall-product`（9303）  
**调用方**：`mall-order`（购物车/下单）、`mall-search`（全量重建）

| 方法 | 参数 | 返回 | 说明 |
|------|------|------|------|
| `batchGetSku` | `skuIds`(List\<Long\>) | `List\<ProductSkuDTO\>`(含 isOnSale, availableQty, price) | 批量查 SKU 实时信息 |
| `reserveStock` | `orderNo`(String), `items`(List\<{skuId, qty}\>) | `boolean` | 下单锁库存，乐观锁防超卖 |
| `releaseStock` | `orderNo`(String) | `void` | 取消订单释放库存 |
| `restock` | `skuId`(Long), `qty`(Integer) | `void` | 售后退货回补可用库存 |
| `fetchAllSpus` | `page`(int), `size`(int) | `PageResult\<SpuDTO\>` | 全量重建时分批拉取 SPU 数据 |

### 3.4 RemoteOrderService

**提供方**：`mall-order`（9304）  
**调用方**：`mall-payment`（发起支付校验）、`mall-marketing`（校验订单状态）

| 方法 | 参数 | 返回 | 说明 |
|------|------|------|------|
| `queryOrder` | `orderNo`(String) | `OrderDTO`(含 status, payAmount, payExpireTime) | 查询订单当前状态和快照金额 |

### 3.5 RemotePaymentService

**提供方**：`mall-payment`（9305）  
**调用方**：`mall-order`（售后发起退款）

| 方法 | 参数 | 返回 | 说明 |
|------|------|------|------|
| `createRefund` | `RefundDTO`(含 paymentNo, refundAmount分, afterSaleNo, channelCode) | `RefundResultDTO`(含 refundNo) | 创建退款单并调支付渠道 |
| `refund` | `payOrderNo`(String), `refundAmount`(Long分), `afterSaleId`(Long) | `RefundResultDTO` | 售后审核通过后发起退款 |

### 3.6 RemoteMarketingService

**提供方**：`mall-marketing`（9306）  
**调用方**：`mall-order`（下单锁券/试算）

| 方法 | 参数 | 返回 | 说明 |
|------|------|------|------|
| `calculate` | `CalculationReq`(含 userId, items, couponClaimId可选) | `CalculationResp`(含各优惠明细) | 优惠试算，不锁定 |
| `lockCoupon` | `orderNo`(String), `couponClaimId`(Long) | `boolean` | 下单锁定优惠券 |
| `releaseCoupon` | `orderNo`(String) | `void` | 订单取消释放全部已锁券 |
| `validateCoupon` | `couponClaimId`(Long), `userId`(Long) | `boolean` | 校验券可用性 |

### 3.7 RemoteSearchService

**提供方**：`mall-search`（9307）  
**调用方**：`mall-product`（商品变更同步）

| 方法 | 参数 | 返回 | 说明 |
|------|------|------|------|
| `syncProduct` | `ProductIndex`(含 spuId, spuName, price, operation:UPSERT/DELETE) | `void` | 实时同步 ES 索引（失败不阻塞，Outbox 兜底） |

---

## 4 调用关系矩阵

| 调用方↓ / 提供方→ | mall-user | mall-auth | mall-product | mall-order | mall-payment | mall-marketing | mall-search |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **mall-order** | validateAddress | decrypt, batchDecrypt | batchGetSku, reserveStock, releaseStock, restock | — | createRefund | calculate, lockCoupon, releaseCoupon, validateCoupon | — |
| **mall-auth** | register, findByPhone, findByWechatOpenId, registerByWechat, updatePassword, deactivateAccount, updatePhone | — | — | — | — | — | — |
| **mall-payment** | — | — | — | queryOrder | — | — | — |
| **mall-marketing** | — | — | — | queryOrder | — | — | — |
| **mall-product** | — | — | — | — | — | — | syncProduct |
| **mall-search** | — | — | fetchAllSpus | — | — | — | — |
| **mall-user** | — | decrypt, batchDecrypt | — | — | — | — | — |

---

## 5 Feign 通用约束

| 约束项 | 配置 |
|--------|------|
| 序列化 | JSON，lowerCamelCase |
| 超时 | connect 2s，read 5s |
| 重试 | 默认 1 次（仅幂等接口开启） |
| 降级 | 每个接口建议配 fallback，避免调用方阻塞 |
| 错误码 | 被调方异常通过若依 `R<T>` 统一返回，使用系统设计第二章错误码 |
| 日志 | Feign 请求/响应 debug 级别，异常 error 级别 |
| 禁止 | Feign 接口不放 VO、不引用 domain 实体、不包含业务逻辑 |

---

## 6 共享枚举

| 枚举 | 值 | 使用方 |
|------|----|--------|
| `OrderStatusEnum` | WAIT_PAY / PAID / WAIT_DELIVER / WAIT_RECEIVE / COMPLETED / CANCELLED / CLOSED / REFUNDING / REFUNDED | mall-order, mall-payment, mall-marketing |
| `PaymentStatusEnum` | UNPAID / PAID / FAILED / CLOSED / REFUNDING / REFUNDED | mall-payment, mall-order |
| `RefundStatusEnum` | PROCESSING / SUCCESS / FAILED | mall-payment, mall-order |
| `CouponRecordStatusEnum` | AVAILABLE / LOCKED / USED / RELEASED / EXPIRED | mall-marketing, mall-order |
| `CouponTypeEnum` | FULL_REDUCE(1) / DISCOUNT(2) / NO_THRESHOLD(3) | mall-marketing |
| `PromotionTypeEnum` | FULL_REDUCE(1) / FULL_DISCOUNT(2) / FREE_SHIPPING(3) / SECKILL(4) | mall-marketing |
| `UserStatusEnum` | NORMAL(0) / FROZEN(1) / DELETED(2) | mall-user, mall-auth |

> 枚举统一使用字符串值（如 `WAIT_PAY`），DB 存储数字编码由各服务 domain 层自行映射。

---

## 7 C 端统一响应体 MallResult<T>

所有 C 端模块使用 `MallResult<T>`（`com.mall.api.dto`）替代若依管理端的 `AjaxResult`。

```json
{
  "errorCode": "00000",
  "errorMessage": "操作成功",
  "userTip": "",
  "data": {},
  "requestId": "..."
}
```

静态工厂方法：

| 方法 | 说明 |
|------|------|
| `success(data)` | 成功返回，errorCode=00000，userTip="" |
| `error(errorCode, errorMessage)` | 错误返回，data=null |
| `error(errorCode, errorMessage, userTip)` | 带用户提示的错误返回 |

注解 `@JsonInclude(NON_NULL)`：成功时 userTip 序列化为 `""` 而非 `null`，错误时 data=null 不序列化。

---

## 8 关键约束

| 约束 | 说明 |
|------|------|
| mall-api 不放 VO | VO 属各模块视图层，不放共享库 |
| mall-api 不放 domain/DO | 数据库实体各模块私有 |
| mall-api 不放 Service 实现 | 只放接口签名 |
| 不循环依赖 | Feign 调用链必须单向，检查：mall-order→mall-payment→mall-order（OK，不同方法） |
| DTO 命名 | 请求 `*Req`、响应 `*Resp` 或 `*DTO`，统一 lowerCamelCase |
| 版本兼容 | 新增字段向后兼容（可选），不删字段 |

---
