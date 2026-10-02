# mall 表 → 若依管理端接入核对清单

> 生成日期：2026-10-02
> 核对范围：26 张 mall 业务表 → mall-admin Controller → ruoyi-ui 页面 → 菜单 SQL → 网关路由 → 数据源

---

## 一、结论速览

| 核对维度 | 结果 |
| --- | --- |
| 业务表归属 | ✅ 全部在 `mall` 库，未错放 `ry-cloud` |
| 配置数据源指向 | ✅ 40 处连 `mall`，11 处连 `ry-cloud`，各自正确 |
| Controller 覆盖 | ✅ 20 个 Controller 对应 20 张可管理表 |
| 前端页面覆盖 | ✅ 20 个 views 目录 + 20 个 api 文件，一一对应 |
| 菜单覆盖 | ✅ 120 条菜单（20×6），与 `ry-cloud.sys_menu` 一致 |
| 网关路由 | ✅ `/mall-admin/**` → lb://mall-admin |
| **写入并发安全** | ⚠️ **不通过**，见第二节 |

---

## 二、⚠️ 核心问题：管理端绕过了 C 端的并发控制

### 2.1 问题证据

**C 端（mall-product）库存更新 —— 乐观锁保护：**

```java
// MallSkuStockMapper.reserveStock
UPDATE mall_product_sku_stock
SET available_stock = available_stock - #{qty},
    locked_stock = locked_stock + #{qty},
    version = version + 1
WHERE sku_id = #{skuId}
  AND version = #{version}          ← 版本校验
  AND available_stock >= #{qty}     ← 库存充足校验
```

**管理端（mall-admin）库存更新 —— 无任何校验：**

```xml
<!-- MallProductSkuStockMapper.xml -->
update mall_product_sku_stock
  <trim prefix="SET" suffixOverrides=",">
    <if test="availableStock != null">available_stock = #{availableStock},</if>
    ...
    <if test="version != null">version = #{version},</if>   ← version 只是被覆盖写入
  </trim>
where id = #{id}                     ← 只有主键，无 version 条件
```

### 2.2 风险后果

管理端与 C 端**各自持有 Mapper，直连同一套表，不经过 Feign**（这是设计明确的），
但两条写入路径**不共享任何并发控制**：

| 场景 | 后果 |
| --- | --- |
| 管理员在库存页改库存，同时用户下单 | 管理端整体覆盖 → C 端刚扣的库存被回滚，**超卖** |
| 管理员改订单状态，同时 C 端状态机流转 | `order_status` 被任意覆写，**状态机被击穿** |
| 管理员改支付状态，同时回调写入 | `payment_status` 双向覆盖，**对账不一致** |

管理端 `updateMallOrder` 同样是 `where id = #{id}`（无 version），且 `MallOrderServiceImpl`
是生成器风格的纯 CRUD，**不含任何状态机校验**，可把已完成的订单直接改成待支付。

### 2.3 文档缺口

`docs/design/05_mall-admin详细设计.md`（369 行）全文未出现
「只读 / 乐观锁 / 并发 / 状态机 / 禁止覆盖」任何约束说明，
等于把「管理端直接 CRUD 覆写核心状态字段」当作正常行为放行了。

---

## 三、逐表核对明细

| # | 表 | Controller | 页面(views) | api | 菜单SQL | 网关 | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | mall_user | MallUserController | user/ | user.ts | userMenu.sql | /mall-admin/user | |
| 2 | mall_user_address | MallUserAddressController | address/ | address.ts | addressMenu.sql | | |
| 3 | mall_user_member | MallUserMemberController | member/ | member.ts | memberMenu.sql | | |
| 4 | mall_user_member_level | MallUserMemberLevelController | level/ | level.ts | levelMenu.sql | | |
| 5 | mall_user_points_account | MallUserPointsAccountController | account/ | account.ts | accountMenu.sql | | |
| 6 | mall_user_points_log | MallUserPointsLogController | points_log/ | points_log.ts | points_logMenu.sql | | |
| 7 | mall_user_growth_log | MallUserGrowthLogController | growth_log/ | growth_log.ts | growth_logMenu.sql | | |
| 8 | mall_product_category | MallProductCategoryController | category/ | category.ts | categoryMenu.sql | | 树表 |
| 9 | mall_product_brand | MallProductBrandController | brand/ | brand.ts | brandMenu.sql | | |
| 10 | mall_product_spu | MallProductSpuController | spu/ | spu.ts | spuMenu.sql | | 主子表 |
| 11 | mall_product_sku | （作为 SPU 子表） | （嵌 SPU 弹窗） | — | — | | 不单独管理页 |
| 12 | mall_product_sku_stock | MallProductSkuStockController | stock/ | stock.ts | stockMenu.sql | | ⚠️ 风险点 |
| 13 | mall_order | MallOrderController | order/ | order.ts | orderMenu.sql | | ⚠️ 风险点 |
| 14 | mall_order_item | （作为 Order 子表） | （嵌 Order 弹窗） | — | — | | 不单独管理页 |
| 15 | mall_order_amount | MallOrderAmountController | amount/ | amount.ts | amountMenu.sql | | |
| 16 | mall_order_cart | MallOrderCartController | cart/ | cart.ts | cartMenu.sql | | |
| 17 | mall_order_after_sale | MallOrderAfterSaleController | after_sale/ | after_sale.ts | after_saleMenu.sql | | |
| 18 | mall_payment | MallPaymentController | payment/ | payment.ts | paymentMenu.sql | | ⚠️ 风险点 |
| 19 | mall_payment_refund | （作为 Payment 子表） | （嵌 Payment 弹窗） | — | — | | 不单独管理页 |
| 20 | mall_payment_channel | MallPaymentChannelController | channel/ | channel.ts | channelMenu.sql | | |
| 21 | mall_payment_callback_log | MallPaymentCallbackLogController | log/ | log.ts | logMenu.sql | | |
| 22 | mall_marketing_coupon | MallMarketingCouponController | coupon/ | coupon.ts | couponMenu.sql | | 主子表 |
| 23 | mall_marketing_coupon_record | （作为 Coupon 子表） | （嵌 Coupon 弹窗） | — | — | | 不单独管理页 |
| 24 | mall_marketing_promotion | MallMarketingPromotionController | promotion/ | promotion.ts | promotionMenu.sql | | 主子表 |
| 25 | mall_marketing_promotion_rule | （作为 Promotion 子表） | （嵌 Promotion 弹窗） | — | — | | 不单独管理页 |
| 26 | mall_outbox | — | — | — | — | | 设计即"不生成" |

**统计**：26 张表 = 20 张独立管理页 + 5 张主子表内嵌 + 1 张（mall_outbox）无页面。

---

## 四、另一处冗余：ry-cloud 初始化脚本含全套 mall 表

`deploy/docker/mysql-init/ry-cloud.sql` 中**同时存在 26 张 mall 表的完整建表 + 8 张表的种子数据**，
内容与 `mall.sql` 完全一致。

| 文件 | 含 mall 表 | 说明 |
| --- | --- | --- |
| `ry-cloud.sql` | ✅ 26 张建表 + 8 张种子 | ← 冗余，易误导 |
| `mall.sql` | ✅ 26 张建表 + 8 张种子 | ← 正常业务库 |

这是为「代码生成器读表结构」留下的历史痕迹。**不影响运行**（运行期数据源明确指向 `mall`），
但会让后续维护者误以为业务表归属 `ry-cloud`。建议清理 `ry-cloud.sql` 中的 mall 表段落。

---

## 五、建议整改方向（待决策，未实施）

针对第二节的并发风险，三个方案：

| 方案 | 做法 | 优点 | 代价 |
| --- | --- | --- | --- |
| **A. 走 Feign** | 管理端写操作调 C 端内部接口 | 完全复用状态机+乐观锁 | 改动大，管理端需依赖 mall-api（违背"零依赖"决策） |
| **B. 补校验** | 管理端写 SQL 加 version 条件 + 状态机校验 | 保持零依赖 | 需逐表改生成代码，状态机逻辑重复一份 |
| **C. 降级只读** | 库存/订单状态/支付状态在管理端只读 | 改动最小、风险最低 | 失去部分管理能力 |

> 推荐组合：**C 为基础 + B 补充**——高风险字段（库存数量、order_status、payment_status）降级只读；
> 确需人工干预的（如强制退款、下架商品）走 B 专用接口 + 状态机校验。
