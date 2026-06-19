# JH-Store mall-api 契约层设计

> mall-api 是跨服务共享契约模块（非独立部署），**仅定义 Feign 接口**。共享 DTO、枚举、`MallResult<T>` 响应体位于兄弟模块 `mall-common`。
> `mall-common` 作为基础层（不依赖任何 mall 模块），`mall-api` 依赖 `mall-common` 使用共享类型。
> 依据概要设计 `02_系统概要设计_补充.md` 第 2 章包结构规划。

---

## 1 模块概述

### 1.1 职责

| 内容 | 约束 |
|------|------|
| Feign 接口 `Remote*Service` | **仅**定义接口签名 + `@FeignClient` + fallback |

**禁止放入 mall-api 的：** DTO、枚举、VO、数据库实体、Mapper、Service 实现、global constant 常量类、异常处理器。共享类型全部在 `mall-common`（详见 `06_mall-common公共模块设计.md`）。

### 1.2 依赖关系

```
mall-api (Feign 契约层)
  ├── 被依赖方：所有 mall-* 服务（通过 Feign 接口调用）
  ├── 依赖：mall-common（共享类型）、spring-cloud-starter-openfeign（@FeignClient）
  └── 独立部署：否（JAR 包供各服务引用）
```

> mall-api 不定义任何业务类型，DTO/枚举/MallResult 等共享类型一律在 `mall-common`。

---

## 2 包结构

```
server/mall/mall-api/
└── src/main/java/com/mall/api/
    └── feign/                                # Feign 远程调用接口
        ├── RemoteUserService.java            # 用户服务（提供方：mall-user）
        ├── RemoteProductService.java         # 商品服务（提供方：mall-product）
        ├── RemoteOrderService.java           # 订单服务（提供方：mall-order）
        ├── RemotePaymentService.java         # 支付服务（提供方：mall-payment）
        ├── RemoteMarketingService.java       # 营销服务（提供方：mall-marketing）
        ├── RemoteSearchService.java          # 搜索服务（提供方：mall-search）
        └── RemoteAuthService.java            # 认证服务（提供方：mall-auth）
```

> DTO、枚举、`MallResult<T>` 响应体等共享类型在 `mall-common`，详见 `06_mall-common公共模块设计.md`。

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
| `fetchAllSpus` | `page`(int), `size`(int) | `PageResult\<SpuDTO\>` | 全量重建时分批拉取 SPU 基础数据 |
| `fetchAllSpusForSearch` | `page`(int), `size`(int) | `PageResult\<SpuSearchDTO\>` | 全量重建时分批拉取含类目名/品牌名/SKU规格的富 DTO |

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
| 错误码 | C 端被调方异常通过 `MallResult<T>` 统一返回，使用系统设计第二章错误码 |
| 日志 | Feign 请求/响应 debug 级别，异常 error 级别 |
| 禁止 | Feign 接口不放 VO、不引用 domain 实体、不包含业务逻辑 |

---

## 6 共享枚举

> 跨服务枚举定义统一在 `mall-common`，详见 `06_mall-common公共模块设计.md` §2.2。

---

## 6 C 端统一响应体 MallResult<T>

所有 C 端模块使用 `MallResult<T>`（`com.mall.common.DTO`）替代若依管理端的 `AjaxResult`。

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

## 7 关键约束

| 约束 | 说明 |
|------|------|
| mall-api 不放 DTO/枚举 | 共享类型全部在 mall-common |
| mall-common 不放 Feign 接口 | 契约接口只在 mall-api |
| mall-common 不依赖 mall-api | 最底层不能依赖上层 |
| 不循环依赖 | Feign 调用链必须单向，检查：mall-order→mall-payment→mall-order（OK，不同方法） |

---
