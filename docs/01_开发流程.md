# JH-Mall 开发流程

> 基于若依代码生成器 + 设计文档的开发工作流。

---

## 一、整体流程

```
DDL 脚本 → 导入 MySQL → 若依代码生成 → 手写业务逻辑 → 前端页面
  ①          ②             ③               ④             ⑤
```

---

## 二、分步说明

### ① 生成 DDL 脚本

来源：`docs/design/03_系统详细设计.md` 第一章（1.1~1.6 节，共 25+ 张表）。

- 输出到 `db/{service}/` 目录
- 按服务拆分（每个 mall-* 一个子目录）
- 命名：`V1.0.0__create_{service}_tables.sql`
- 按模块先后：mall-user → mall-product → mall-order → mall-payment → mall-marketing

### ② 导入 MySQL

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS mall DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 按顺序执行 DDL 脚本
source db/mall-user/V1.0.0__create_mall_user_tables.sql;
source db/mall-product/V1.0.0__create_mall_product_tables.sql;
source db/mall-order/V1.0.0__create_mall_order_tables.sql;
source db/mall-payment/V1.0.0__create_mall_payment_tables.sql;
source db/mall-marketing/V1.0.0__create_mall_marketing_tables.sql;
```

### ③ 若依代码生成

对每张表，在若依「系统工具 → 代码生成」导入表结构，生成：

| 生成内容 | 管理端 | 后端服务 |
|---------|:---:|:---:|
| domain/DO | — | ✅ |
| Mapper + XML | — | ✅ |
| Service + Impl | — | ✅ |
| Controller | — | ✅（标准 CRUD） |
| Vue 页面 + API | ✅ | — |

**注意**：搜索模块 `mall-search` 不从数据库生成（无 MySQL 表）。

### ④ 手写业务逻辑

代码生成器覆盖标准 CRUD，以下需手写：

| 模块 | 手写内容 | 参考设计文档 |
|------|---------|------------|
| mall-auth | TokenService、SmsService、WechatAdapter、BCrypt 校验、RemoteUserAdapter | 06 mall-auth 3.1~3.4 |
| mall-order | OrderStateMachine、下单编排（幂等/锁库存/锁券/补偿）、Outbox 投递、MQ 消费 | 04 mall-order 5~7 |
| mall-payment | PaymentStateMachine、渠道适配器（Wechat/Alipay）、回调验签+防重放、Outbox | 05 mall-payment 3~8 |
| mall-marketing | CouponStateMachine、优惠试算引擎（两阶段过滤+组合择优）、规则匹配 | 07 mall-marketing 3~5 |
| mall-product | StockServiceImpl（乐观锁库存操作）、SkuCacheService、搜索同步双通道 | 08 mall-product 3~5 |
| mall-search | ES 索引管理、全文搜索、聚合、降级兜底 | 09 mall-search 3~5 |
| mall-user | 积分操作（乐观锁）、成长值计算、手机号加密查询（SHA256 哈希） | 10 mall-user 3~5 |
| mall-api | Feign 接口 + 共享 DTO + 枚举 | 11 mall-api 3~7 |

### ⑤ 前端页面

- **管理端**：若依代码生成覆盖标准 CRUD 页面，不需额外开发
- **C 端**：`client/mall-ui/` 手写开发，参考 `docs/design/12_前端详细设计-C端.md`

---

## 三、开发顺序

```
第一阶段：基础设施
  1. DDL 脚本 + 导入 MySQL
  2. mall-api 契约层（DTO + Feign 接口 + 枚举）
  3. 各模块代码生成（标准 CRUD）

第二阶段：核心业务（按依赖顺序）
  4. mall-user     → 用户/地址/会员/积分
  5. mall-auth     → 认证中心（依赖 mall-user Feign）
  6. mall-product  → 商品/类目/品牌/库存
  7. mall-marketing → 优惠券/活动/促销
  8. mall-order    → 订单/购物车/售后（依赖 product + marketing + payment Feign）
  9. mall-payment  → 支付/退款/回调（依赖 order Feign）
  10. mall-search  → 搜索索引（无 MySQL，依赖 product Feign 取数据）

第三阶段：前端
  11. C 端 mall-ui（按模块逐页开发）
```

---

## 四、编码规范

- 后端：遵循 `docs/design/03_系统详细设计.md` + 阿里巴巴 Java 开发手册（嵩山版）
- 前端 C 端：遵循 `client/mall-ui/docs/CODING.md`
- Feign 接口：遵循 `docs/design/11_mall-api契约层设计.md` 第 7 节关键约束
- 命名：`docs/design/` 各模块设计文档中的包名/类名/方法名

---
