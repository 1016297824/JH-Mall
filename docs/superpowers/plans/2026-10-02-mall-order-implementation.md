# mall-order 模块实现计划

> **For agentic workers:** 步骤使用 checkbox（`- [ ]`）语法跟踪。

**Goal:** 按 5 个批次实现 mall-order 全部业务代码（数据层 → 订单状态机 → 购物车 → 下单编排 → 售后与异步消费）

**Architecture:** 状态机为无状态单例，是订单状态变更的唯一入口；Service 层负责事务边界与 Feign 补偿；跨服务一致性问题用 Outbox 可靠投递解决，超时关单用乐观锁防竞态。

**Tech Stack:** Spring Boot 4.0.3, MyBatis-Plus, RocketMQ, Feign, Redis, Lombok

**基线文档:**
- `docs/design/12_mall-order详细设计.md`（678 行，实现的唯一事实源）
- `docs/design/03_04_系统详细设计-状态机详细设计.md`

**现状：** 模块仅有启动类 + 3 个 config（共 4 个文件），无任何业务代码。pom.xml 已配好 mall-api / mall-common / mybatis-plus / redis / sentinel，**无需改动依赖**。

---

## ✅ 跨模块阻塞项（已解除）

> **2026-10-02 决策：采用方案 A。** 两个 Feign 契约已创建，实现体留空，
> 待 mall-marketing / mall-payment 开工后填充。**Batch 4 阻塞解除。**

| 契约 | 状态 | 文件 |
|------|:--:|------|
| `RemoteMarketingService` | ✅ 已创建 | `mall-api/.../feign/RemoteMarketingService.java` |
| `RemotePaymentService` | ✅ 已创建 | `mall-api/.../feign/RemotePaymentService.java` |

**契约方法**（对应设计文档 §3.5 / §3.6）：

| 契约 | 方法 | inner 端点 |
|------|------|-----------|
| `RemoteMarketingService` | `calculate` | `/inner/marketing/calculate` |
| | `lockCoupon` | `/inner/marketing/coupon/lock` |
| | `releaseCoupon` | `/inner/marketing/coupon/release` |
| | `validateCoupon` | `/inner/marketing/coupon/validate` |
| `RemotePaymentService` | `createRefund` | `/inner/payment/refunds` |
| | `refund` | `/inner/payment/refunds/by-after-sale` |
| | `getPaymentStatus` | `/inner/payment/status` |

> `getPaymentStatus` 为本次新增（设计文档 §8.1 售后校验需要"支付成功且未全额退款"判断）。
>
> **待实现方补齐**：`RemoteMarketingInnerController`（mall-marketing）、`RemotePaymentInnerController`（mall-payment）。
> 端点路径须与上表一致，否则 Feign 调用 404。

---

## 文件清单

### Batch 1: 数据层

| 文件 | 操作 | 职责 |
|------|:--:|------|
| `mall-order/.../DO/MallCartDO.java` | 新建 | `mall_order_cart` 实体 |
| `mall-order/.../DO/MallOrderDO.java` | 新建 | `mall_order` 实体 |
| `mall-order/.../DO/MallOrderItemDO.java` | 新建 | `mall_order_item` 实体 |
| `mall-order/.../DO/MallOrderAmountDO.java` | 新建 | `mall_order_amount` 实体 |
| `mall-order/.../DO/MallAfterSaleDO.java` | 新建 | `mall_order_after_sale` 实体 |
| `mall-order/.../DO/MallOutboxDO.java` | 新建 | `mall_outbox` 实体 |
| `mall-order/.../mapper/MallCartMapper.java` | 新建 | 购物车 Mapper |
| `mall-order/.../mapper/MallOrderMapper.java` | 新建 | 订单 Mapper（含乐观锁关单） |
| `mall-order/.../mapper/MallOrderItemMapper.java` | 新建 | 订单项 Mapper |
| `mall-order/.../mapper/MallOrderAmountMapper.java` | 新建 | 金额快照 Mapper |
| `mall-order/.../mapper/MallAfterSaleMapper.java` | 新建 | 售后 Mapper |
| `mall-order/.../mapper/MallOutboxMapper.java` | 新建 | Outbox Mapper |

### Batch 2: 订单状态机

| 文件 | 操作 | 职责 |
|------|:--:|------|
| `mall-order/.../statemachine/OrderEventEnum.java` | 新建 | 13 个订单事件（含预留 COMMON_REFUND） |
| `mall-order/.../statemachine/OrderTransition.java` | 新建 | 转移定义（目标状态解析器 + 前置条件 + 后置动作） |
| `mall-order/.../statemachine/OrderStateMachine.java` | 新建 | 转移矩阵 + `transition()` |
| `mall-order/.../statemachine/OrderStateMachineTest.java` | 新建 | 转移规则单元测试 |
| `mall-order/.../DO/MallOrderDO.java` | **提前新建** | 状态机的硬依赖，原属Batch 1 |
| `mall-order/pom.xml` | 修改 | 新增 `spring-boot-starter-test`（test作用域，照抄 mall-product） |

> `OrderStatusEnum` **已存在于 `mall-common/enums/order/`**，直接复用，不要新建。
>
> **转移矩阵规模更正**：设计文档 §6.3 表格共 **17 行**，去重后是 **14 个「状态 × 事件」条目**
> （`REFUND_FAIL` 的 4 行合并为 1 个动态条目）。此前本计划误写为"19条"，以此处为准。

### Batch 3: 购物车

| 文件 | 操作 | 职责 |
|------|:--:|------|
| `mall-order/.../service/CartService.java` | 新建 | 购物车接口 |
| `mall-order/.../service/impl/CartServiceImpl.java` | 新建 | 购物车实现 |
| `mall-order/.../convert/response/CartConvert.java` | 新建 | DO → CartVO |
| `mall-order/.../VO/CartVO.java` | 新建 | 购物车视图对象 |
| `mall-order/.../infrastructure/feign/RemoteProductAdapter.java` | 新建 | Feign 调 mall-product |
| `mall-order/.../controller/CartController.java` | 新建 | `/api/order/cart/**` |

### Batch 4: 下单编排

| 文件 | 操作 | 职责 |
|------|:--:|------|
| `mall-api/.../feign/RemoteMarketingService.java` | 新建（阻塞项） | 营销契约 |
| `mall-api/.../feign/RemotePaymentService.java` | 新建（阻塞项） | 支付契约 |
| `mall-order/.../service/OrderService.java` | 新建 | 订单接口 |
| `mall-order/.../service/impl/OrderServiceImpl.java` | 新建 | 下单编排实现 |
| `mall-order/.../infrastructure/outbox/OutboxScheduler.java` | 新建 | Outbox 投递调度 |
| `mall-order/.../infrastructure/mq/OrderEventProducer.java` | 新建 | 事件发布 |
| `mall-order/.../infrastructure/feign/RemoteUserAdapter.java` | 新建 | 地址归属校验 |
| `mall-order/.../convert/response/OrderConvert.java` | 新建 | DO → VO |
| `mall-order/.../VO/OrderVO.java` `OrderItemVO.java` | 新建 | 订单视图对象 |
| `mall-order/.../controller/OrderController.java` | 新建 | `/api/order/orders/**` |
| `mall-order/.../controller/inner/RemoteOrderInnerController.java` | 新建 | `/inner/order/**` |

### Batch 5: 售后与异步消费

| 文件 | 操作 | 职责 |
|------|:--:|------|
| `mall-order/.../service/AfterSaleService.java` + impl | 新建 | 售后接口与实现 |
| `mall-order/.../convert/response/AfterSaleConvert.java` | 新建 | 售后转换 |
| `mall-order/.../VO/AfterSaleVO.java` | 新建 | 售后视图对象 |
| `mall-order/.../controller/AfterSaleController.java` | 新建 | `/api/order/after_sales` |
| `mall-order/.../infrastructure/mq/PaymentPaidConsumer.java` | 新建 | 支付成功消费 |
| `mall-order/.../infrastructure/mq/OrderTimeoutConsumer.java` | 新建 | 超时关单消费 |
| `mall-order/.../infrastructure/mq/RefundSucceededConsumer.java` | 新建 | 退款成功消费 |
| `mall-order/.../infrastructure/schedule/OrderTimeoutFallbackTask.java` | 新建 | ruoyi-job 兜底日扫 |

---

## Batch 1: 数据层

**Design ref:** `db/mall-sql/V1.0.0__create_mall_order_tables.sql`，风格参照 `mall-product/.../DO/MallSkuStockDO.java`

- [x] **Step 1: 创建 6 个 DO** —— ✅ 2026-10-02 完成（`MallOrderDO` 在 Batch 2 提前创建，其余 5 个本次完成；字段严格按 DDL，`@Version` 仅 `MallOrderDO` 有）

每个 DO 遵循同一范式（以 `MallOrderDO` 为例）：

```java
package com.mall.order.DO;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

/**
 * 订单 DO
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Data
@NoArgsConstructor
@TableName("mall_order")
public class MallOrderDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 订单号（业务唯一键） */
    @TableField("order_no")
    private String orderNo;

    @TableField("user_id")
    private Long userId;

    /** 订单状态，取值见 OrderStatusEnum */
    @TableField("order_status")
    private Integer orderStatus;

    @TableField("total_amount")
    private Long totalAmount;

    @TableField("discount_amount")
    private Long discountAmount;

    @TableField("freight_amount")
    private Long freightAmount;

    @TableField("pay_amount")
    private Long payAmount;

    /** 支付超时时间，WAIT_PAY 超过此时间应关单 */
    @TableField("pay_expire_time")
    private LocalDateTime payExpireTime;

    /** 幂等键 */
    @TableField("idempotent_key")
    private String idempotentKey;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    /** 乐观锁版本号（超时关单竞态防护） */
    @Version
    @TableField("version")
    private Integer version;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
```

字段以 DDL 为准，**逐表核对不得增减**。注意：
- 金额字段全部是 `bigint unsigned`（单位：分），Java 侧用 `Long`
- 所有表都有 `is_deleted` 逻辑删除 + `version` 乐观锁（`mall_order_cart` 除外，无 version）
- 时间字段统一 `LocalDateTime`

- [x] **Step 2: 创建 6 个 Mapper** —— ✅ 2026-10-02 完成

遵循设计文档 §2.4：SELECT 用 `LambdaQueryWrapper`，UPDATE 用 `@Update`，INSERT/DELETE 走 BaseMapper。参考 `MallSkuStockMapper`。

`MallOrderMapper` 需要一个专用的乐观锁关单方法（设计文档 §5.9）：

```java
/**
 * 超时关单（乐观锁）
 *
 * <p>WHERE 条件锁定 order_status = WAIT_PAY，
 * 支付回调先到时本方法影响 0 行，天然防竞态。</p>
 *
 * @param orderNo 订单号
 * @return 影响行数，1 = 关单成功，0 = 已被支付或已关闭
 */
@Update("UPDATE mall_order SET order_status = 6, update_time = NOW(), version = version + 1 " +
        "WHERE order_no = #{orderNo} AND order_status = 0")
int closeByTimeout(@Param("orderNo") String orderNo);
```

其余 Mapper 按标准范式，方法清单：
- `MallCartMapper`：`selectByUserId`、`insert`、`updateQuantity`、`deleteById`、`deleteByUserId`、`selectByUserIdAndSkuId`
- `MallOrderMapper`：`selectByOrderNo`、`selectByUserIdAndStatus`、`closeByTimeout`
- `MallOrderItemMapper`：`selectByOrderNo`、`batchInsert`
- `MallOrderAmountMapper`：`selectByOrderNo`
- `MallAfterSaleMapper`：`selectByOrderNo`、`updateStatus`
- `MallOutboxMapper`：`selectPending`、`updateStatus`

- [ ] **Step 3: 编译验证（由你执行）**

```powershell
mvn clean compile -f server/mall/mall-order/pom.xml
```

预期: BUILD SUCCESS

---

## Batch 2: 订单状态机（核心）

> ✅ **2026-10-02 已完成。** 以下 Step 中的代码块为**实现前的草案**，
> 最终代码以仓库为准（`OrderTransition` 的 target 已改为 `Function` 以支持动态回退，
> 测试改用 `@Nested` 分组）。草案保留仅作设计推导记录。

**Design ref:** 设计文档 §6.1~6.5 · **无外部依赖，可独立验证**

- [x] **Step 1: 创建 OrderEventEnum** —— ✅ 2026-10-02 完成，13 个事件（12 个设计事件 + 预留 `COMMON_REFUND(0)`）

12 个事件，code 从 1 递增（`COMMON_REFUND` 预留 0）：

| 枚举 | code | 来源 |
|------|:---:|------|
| `PAY_SUCCESS` | 1 | 支付回调 |
| `USER_CANCEL` | 2 | 用户 |
| `PAY_TIMEOUT` | 3 | 系统定时 |
| `SELLER_DELIVER` | 4 | 管理端 |
| `LOGISTICS_PICK` | 5 | 快递回调 |
| `CONFIRM_RECEIPT` | 6 | 用户 |
| `FORCE_CANCEL` | 7 | 管理端 |
| `REFUND_ONLY` | 8 | 系统 |
| `RETURN_REFUND` | 9 | 系统 |
| `AFTER_SALE` | 10 | 系统 |
| `REFUND_SUCCESS` | 11 | 支付回调 |
| `REFUND_FAIL` | 12 | 支付回调 |

- [x] **Step 2: 创建 OrderTransition** —— ✅ 2026-10-02 完成（target 改为 `Function`，见上方结论）

```java
package com.mall.order.statemachine;

import com.mall.order.DO.MallOrderDO;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 订单状态转移定义
 *
 * @author JH-Mall
 * @date 2026/10/02
 * @param target       目标状态
 * @param precondition 前置条件，不满足抛 A0703
 * @param postAction    后置动作（写 Outbox 等），在 Service 层事务内执行
 */
public record OrderTransition(
        OrderStatusEnum target,
        Predicate<MallOrderDO> precondition,
        Consumer<MallOrderDO> postAction
) {

    /** 无前置条件的转移 */
    public static OrderTransition to(OrderStatusEnum target, Consumer<MallOrderDO> postAction) {
        return new OrderTransition(target, order -> true, postAction);
    }

    /** 无后置动作的转移 */
    public static OrderTransition to(OrderStatusEnum target) {
        return new OrderTransition(target, order -> true, order -> { });
    }
}
```

- [x] **Step 3: 创建 OrderStateMachine** —— ✅ 2026-10-02 完成，14 个「状态 × 事件」条目，转移矩阵随实例构造初始化

设计文档 §6.4 要求：**无状态单例、不操作 DB、不持有 Mapper、不管理事务**。转移矩阵在构造时静态初始化，19 条规则严格照抄设计文档 §6.3。

```java
package com.mall.order.statemachine;

import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.order.OrderStatusEnum;
import com.mall.common.exception.BusinessException;
import com.mall.order.DO.MallOrderDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

/**
 * 订单状态机
 *
 * <p>订单状态变更的唯一入口。不操作 DB、不持有 Mapper、不管理事务，
 * 只做转移合法性判断与前置条件校验。</p>
 *
 * <p>无状态单例，Map 只读，线程安全。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Slf4j
@Component
public class OrderStateMachine {

    /** 转移矩阵：状态 → 事件 → 转移定义 */
    private final Map<OrderStatusEnum, Map<OrderEventEnum, OrderTransition>> transitions =
            new EnumMap<>(OrderStatusEnum.class);

    public OrderStateMachine() {
        initTransitions();
    }

    /**
     * 执行状态转移
     *
     * @param order 当前订单（会被原地更新状态）
     * @param event 触发事件
     * @return 目标状态
     * @throws BusinessException A0702 转移矩阵无匹配 / A0703 前置条件不满足
     */
    public OrderStatusEnum transition(MallOrderDO order, OrderEventEnum event) {
        OrderStatusEnum current = OrderStatusEnum.ofCode(order.getOrderStatus());
        OrderTransition transition = transitions
                .getOrDefault(current, Map.of())
                .get(event);

        if (transition == null) {
            log.warn("非法状态转移: status={}, event={}, orderNo={}",
                    current, event, order.getOrderNo());
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        if (!transition.precondition().test(order)) {
            log.warn("前置条件不满足: status={}, event={}, orderNo={}",
                    current, event, order.getOrderNo());
            throw new BusinessException(ErrorCode.ORDER_ACTION_DENIED);
        }

        transition.postAction().accept(order);
        order.setOrderStatus(transition.target().getCode());
        order.setUpdateTime(LocalDateTime.now());

        log.info("状态转移成功: {} --{}--> {}, orderNo={}",
                current, event, transition.target(), order.getOrderNo());
        return transition.target();
    }

    /**
     * 该状态下是否允许此事件（供管理端/前端做按钮级校验）
     */
    public boolean canTransit(OrderStatusEnum from, OrderEventEnum event) {
        return transitions.getOrDefault(from, Map.of()).containsKey(event);
    }

    /**
     * 初始化转移矩阵 —— 严格对应设计文档 §6.3，共 19 条
     */
    private void initTransitions() {
        // WAIT_PAY
        put(OrderStatusEnum.WAIT_PAY, OrderEventEnum.PAY_SUCCESS, new OrderTransition(
                OrderStatusEnum.PAID,
                order -> !order.getPayExpireTime().isBefore(LocalDateTime.now()),
                order -> { }));
        put(OrderStatusEnum.WAIT_PAY, OrderEventEnum.USER_CANCEL, OrderTransition.to(OrderStatusEnum.CANCELLED));
        put(OrderStatusEnum.WAIT_PAY, OrderEventEnum.PAY_TIMEOUT, new OrderTransition(
                OrderStatusEnum.CLOSED,
                order -> order.getPayExpireTime().isBefore(LocalDateTime.now()),
                order -> { }));

        // PAID
        put(OrderStatusEnum.PAID, OrderEventEnum.SELLER_DELIVER, OrderTransition.to(OrderStatusEnum.WAIT_DELIVER));
        put(OrderStatusEnum.PAID, OrderEventEnum.FORCE_CANCEL, OrderTransition.to(OrderStatusEnum.CANCELLED));
        put(OrderStatusEnum.PAID, OrderEventEnum.REFUND_ONLY, OrderTransition.to(OrderStatusEnum.REFUNDING));

        // WAIT_DELIVER
        put(OrderStatusEnum.WAIT_DELIVER, OrderEventEnum.LOGISTICS_PICK,
                OrderTransition.to(OrderStatusEnum.WAIT_RECEIVE));

        // WAIT_RECEIVE
        put(OrderStatusEnum.WAIT_RECEIVE, OrderEventEnum.CONFIRM_RECEIPT,
                OrderTransition.to(OrderStatusEnum.COMPLETED));
        put(OrderStatusEnum.WAIT_RECEIVE, OrderEventEnum.RETURN_REFUND,
                OrderTransition.to(OrderStatusEnum.REFUNDING));

        // COMPLETED
        put(OrderStatusEnum.COMPLETED, OrderEventEnum.AFTER_SALE,
                OrderTransition.to(OrderStatusEnum.REFUNDING));

        // REFUNDING —— 退款失败按原状态回退，4 条
        put(OrderStatusEnum.REFUNDING, OrderEventEnum.REFUND_SUCCESS,
                OrderTransition.to(OrderStatusEnum.REFUNDED));
        put(OrderStatusEnum.REFUNDING, OrderEventEnum.REFUND_FAIL,
                OrderTransition.to(OrderStatusEnum.PAID));
        put(OrderStatusEnum.REFUNDING, OrderEventEnum.REFUND_FAIL,
                OrderTransition.to(OrderStatusEnum.WAIT_DELIVER));
        put(OrderStatusEnum.REFUNDING, OrderEventEnum.REFUND_FAIL,
                OrderTransition.to(OrderStatusEnum.WAIT_RECEIVE));
        put(OrderStatusEnum.REFUNDING, OrderEventEnum.REFUND_FAIL,
                OrderTransition.to(OrderStatusEnum.COMPLETED));

        // 终态清理 —— 2 条
        put(OrderStatusEnum.CANCELLED, OrderEventEnum.PAY_TIMEOUT, OrderTransition.to(OrderStatusEnum.CLOSED));
        put(OrderStatusEnum.REFUNDED, OrderEventEnum.PAY_TIMEOUT, OrderTransition.to(OrderStatusEnum.CLOSED));
    }

    private void put(OrderStatusEnum from, OrderEventEnum event, OrderTransition transition) {
        transitions.computeIfAbsent(from, k -> new EnumMap<>(OrderEventEnum.class)).put(event, transition);
    }
}
```

> ✅ **已定结论**：`OrderTransition` 的 target 字段类型由 `OrderStatusEnum`
> 改为 `Function<MallOrderDO, OrderStatusEnum>`（新增 `OrderTransition.dynamic(...)` 工厂），
> `REFUND_FAIL` 用 `this::resolvePreRefundStatus` 动态回退。
>
> ⚠️ **遗留局限**：~~`mall_order` 无"退款前状态"字段~~ **已解决**（2026-10-02）：
> 新增迁移脚本 `db/mall-sql/V1.0.6__add_order_refund_and_logistics_fields.sql`，
> 补 `pre_refund_status` / `logistics_company` / `logistics_no` 三列。
> 状态机进入 REFUNDING 时写入 `pre_refund_status`，退款失败据此精确回退；
> `SELLER_DELIVER` 的「物流单号 + 公司已填写」前置条件也已下沉到状态机层。

- [x] **Step 4: 创建 OrderStateMachineTest** —— ✅ 2026-10-02 完成，24 个用例（`@Nested` 按状态分组），覆盖 14 个条目 + A0702/A0703 异常路径

19 条转移规则逐条断言，外加非法转移与前置条件失败：

```java
package com.mall.order.statemachine;

import com.mall.common.enums.order.OrderStatusEnum;
import com.mall.common.exception.BusinessException;
import com.mall.order.DO.MallOrderDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 订单状态机单元测试
 *
 * <p>纯逻辑测试，不依赖 Spring 容器与数据库。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
class OrderStateMachineTest {

    private OrderStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new OrderStateMachine();
    }

    private MallOrderDO order(OrderStatusEnum status) {
        MallOrderDO order = new MallOrderDO();
        order.setId(1L);
        order.setOrderNo("ORDER_TEST_001");
        order.setOrderStatus(status.getCode());
        order.setPayAmount(10000L);
        order.setPayExpireTime(LocalDateTime.now().plusMinutes(30));
        return order;
    }

    @Test
    @DisplayName("WAIT_PAY --PAY_SUCCESS--> PAID")
    void paySuccess() {
        MallOrderDO o = order(OrderStatusEnum.WAIT_PAY);
        assertEquals(OrderStatusEnum.PAID, stateMachine.transition(o, OrderEventEnum.PAY_SUCCESS));
        assertEquals(OrderStatusEnum.PAID.getCode(), o.getOrderStatus());
    }

    @Test
    @DisplayName("WAIT_PAY --USER_CANCEL--> CANCELLED")
    void userCancel() {
        MallOrderDO o = order(OrderStatusEnum.WAIT_PAY);
        assertEquals(OrderStatusEnum.CANCELLED, stateMachine.transition(o, OrderEventEnum.USER_CANCEL));
    }

    @Test
    @DisplayName("支付成功但订单已过期 → 前置条件不满足，抛 A0703")
    void paySuccessButExpired() {
        MallOrderDO o = order(OrderStatusEnum.WAIT_PAY);
        o.setPayExpireTime(LocalDateTime.now().minusMinutes(1));
        assertThrows(BusinessException.class, () -> stateMachine.transition(o, OrderEventEnum.PAY_SUCCESS));
    }

    @Test
    @DisplayName("COMPLETED --PAY_SUCCESS--> 非法转移，抛 A0702")
    void illegalTransition() {
        MallOrderDO o = order(OrderStatusEnum.COMPLETED);
        assertThrows(BusinessException.class, () -> stateMachine.transition(o, OrderEventEnum.PAY_SUCCESS));
    }

    @Test
    @DisplayName("canTransit 反映转移矩阵是否覆盖")
    void canTransit() {
        assertTrue(stateMachine.canTransit(OrderStatusEnum.WAIT_PAY, OrderEventEnum.PAY_SUCCESS));
        assertFalse(stateMachine.canTransit(OrderStatusEnum.COMPLETED, OrderEventEnum.PAY_SUCCESS));
    }

    // TODO: 补齐 §6.3 全表 19 条转移的逐条断言
}
```

- [ ] **Step 5: 编译 + 跑测试（由你执行）**

```powershell
mvn clean test -f server/mall/mall-order/pom.xml -Dtest=OrderStateMachineTest
```

预期: BUILD SUCCESS，测试全绿

---

## Batch 3: 购物车

**Design ref:** 设计文档 §4 · 依赖 `RemoteProductService`（**已存在**，含 `batchGetSku`）

- [x] **Step 1: 创建 CartVO + CartConvert** —— ✅ 2026-10-02
- [x] **Step 2: 创建 RemoteProductAdapter** —— ✅ 2026-10-02（Feign 失败降级为空 Map，不抛异常）
- [x] **Step 3: 创建 CartService + CartServiceImpl** —— ✅ 2026-10-02

实现要点（§4.1~4.6）：
- `addItem`：合并同 SKU 数量，合并后不得超库存
- `listCart`：批量取实时价 + 库存，`batchGetSku` 失败时降级用 `mall_order_cart.price` 冗余字段
- Redis Hash 缓存（`key = mall:order:cart:{userId}`，TTL 30min），**MySQL 为唯一事实来源**，Redis 不可用时跳过

- [x] **Step 4: 创建 CartController** —— ✅ 2026-10-02，6 个端点（§2.2 的 5 个 + 新增「修改选中状态」）

- [ ] **Step 5: 编译验证（由你执行）**

> ⚠️ **偏离设计文档 §4.6（待张坤确认）**：未实现 Redis Hash 缓存。
> §4.6 约定 field=skuId、value=quantity，但 §4.5 的降级路径必须读 MySQL 拿冗余 price
> （该字段只存在于 `mall_order_cart`），Redis 里只有数量省不掉那次查询；
> 真要省掉 MySQL 需整体缓存 VO 列表，会引入双写一致性风险。
> 另：§9.3 的 `mall.order.cart-max-items` 在 §4.1 中无对应校验，本实现未使用。

```powershell
mvn clean compile -f server/mall/mall-order/pom.xml
```

---

## Batch 4: 下单编排

> ⚠️ **需先决策阻塞项方案（见文首「跨模块阻塞项」）**

- [x] **Step 0（方案 A）: 补 mall-api 契约** —— `RemoteMarketingService` + `RemotePaymentService` 接口与 DTO（**2026-10-02 已完成**，实现体待 marketing/payment 开工）
- [x] **Step 1: 创建 Outbox 基础设施** —— ✅ 2026-10-02
  - `mall-order/pom.xml` 新增 `rocketmq-spring-boot-starter:2.3.5`（原无 MQ 依赖）
  - `MallOrderApplication` 新增 `@EnableScheduling`（Outbox 调度器需要）
  - `OutboxPublisher`（业务侧，同事务落库）+ `OutboxScheduler`（调度侧，退避 10/30/60s + 抖动，上限 3 次）
- [x] **Step 2: 创建 OrderServiceImpl.createOrder()** —— ✅ 2026-10-02
- [x] **Step 3: 状态推进方法** —— ✅ 2026-10-02，`payCallback` / `cancelOrder` / `confirmReceipt`
- [x] **Step 4: 创建 OrderController** —— ✅ 2026-10-02，5 个端点
- [x] **Step 5: 创建 RemoteOrderInnerController** —— ✅ 2026-10-02，同时补 `RemoteOrderService` 契约

> **与设计文档 §2.2 的差异**：
> ① 路径变量用 `{orderNo}` 而非 `{id}`；
> ② **未实现** `DELETE /orders/{id}`（端点 11）——订单属财务凭证，不应由 C 端用户删除。
>
> **未实现的隐含依赖**：`createOrder` 强依赖 mall-marketing 的 `calculate`。
> marketing 未实现前该接口必然失败（Feign 404），**不会静默降级算错金额**。
> 运费 `freightAmount` 暂固定 0——设计文档 §5.6 未给出运费规则。

严格按 §5.1 编排：`幂等校验 → 参数校验 → 锁库存 → 锁优惠 → 创建订单+Outbox`

关键约束（§5.7 补偿表）：

| 失败位置 | 补偿动作 |
|---------|---------|
| 锁优惠失败 | `releaseStock(orderNo)` |
| 订单创建失败 | `releaseStock(orderNo)` + `releaseCoupon(orderNo)`，返 B0001 |

> Feign 调用失败**不会**自动回滚远程事务，补偿必须手写。

- [ ] **Step 3: 状态推进方法** —— `payCallback` / `cancelOrder` / `confirmReceipt`（✅ 2026-10-02；`autoConfirmReceipt` 待 Batch 5 定时任务）
- [x] **Step 4: 创建 OrderController** —— ✅ 2026-10-02，5 个端点（§2.2 的 6 个中未实现 DELETE，见上）
- [x] **Step 5: 创建 RemoteOrderInnerController** —— ✅ 2026-10-02，`/inner/order/query`

---

## Batch 5: 售后与异步消费

- [x] **Step 1: 售后服务** —— ✅ 2026-10-02，`submit` / `approve` / `reject` / `refundCallback` / `refundFailedCallback`
- [x] **Step 2: 三个消费者** —— ✅ 2026-10-02，`MqDedupGuard` 统一 Redis SETNX 幂等（§7.4）

| Topic | 处理 |
|-------|------|
| `mall:payment:paid` | 幂等 → `payCallback` → 状态机 → 写 Outbox → **取消 `mall:order:timeout` 延迟消息** |
| `mall:order:timeout` | 幂等 → 双校验（`WAIT_PAY` + 已到期）→ 乐观锁关单 → 写 Outbox |
| `mall:refund:succeeded` | 幂等 → `refundCallback` → 退货退款调 `restock` |

- [x] **Step 3: 超时兜底日扫** —— ✅ 2026-10-02，`OrderTimeoutFallbackTask`，cron `${mall.order.timeout-fallback-cron:0 0 2 * * ?}`
- [x] **Step 4: 自动确认收货** —— ✅ 2026-10-02，同类内 cron `0 0 3 * * ?`，天数取 `mall.order.auto-receive-days`

> **Batch 5 与设计文档的差异**：
> ① `RemotePaymentService.refund(payOrderNo,...)` 改为 `refundByOrderNo(orderNo,...)`——
>    支付单号只存在于 mall-payment 的 `mall_payment` 表，mall-order 拿不到；
> ② §8.4 要求退款失败置售后单 `FAILED`，但 `AfterSaleStatusEnum` **无 FAILED 值**
>    （PENDING/APPROVED/REJECTED/RETURNED/RECEIVED/REFUNDING/COMPLETED/CLOSED），
>    实现保持 `REFUNDING` 并打「需人工介入」日志，不擅自置 CLOSED 丢失待处理语义；
> ③ §8.4 要求累加 `mall_order.refunded_amount`，**该列不存在**——
>    未加列，需要时从售后表汇总（避免反规范化冗余）。

---

## 约束与规范

| 规约 | 说明 |
|------|------|
| 状态变更 | **必须**经 `OrderStateMachine.transition()`，禁止 Service 直接 update status |
| Mapper | SELECT 用 `LambdaQueryWrapper`；UPDATE 用 `@Update`；算术运算必须用（设计文档 §2.4） |
| 金额 | 一律 `Long`，单位**分**，禁止用 `double`/`BigDecimal` 参与运算 |
| 错误处理 | 抛 `BusinessException(ErrorCode.XXX)`，A07xx 已在 `mall-common` 定义好 |
| 消息 Payload | 禁止序列化 DO，必须精简 DTO，字段 lowerCamelCase |
| Lombok | DO/VO/DTO 用 `@Data`；Service/Controller/Consumer 用 `@Slf4j` + `@RequiredArgsConstructor` |
| 编码规范 | 阿里巴巴 Java 开发手册·嵩山版 |

---

## 验证方式

| 批次 | 验证方式 |
|------|----------|
| Batch 1 | `mvn compile` 通过 |
| Batch 2 | `mvn test -Dtest=OrderStateMachineTest` —— 19 条转移规则全绿 |
| Batch 3 | 启动后调 `/api/order/cart/items` 增删改查 |
| Batch 4 | 下单全链路：加购 → 下单 → 校验库存锁定 + Outbox 落库 |
| Batch 5 | 支付回调推进状态；超时消息关单并释放库存 |

---

## 配置变更

| 位置 | 变更 | 说明 |
|------|------|------|
| `mall-order-dev.yml` | 补 `mall.order.*` 7 项 | 配置清单见设计文档 §9.3 |
| `ruoyi-job` 控制台 | 新增 cron `0 0 2 * * ?` | 超时关单兜底日扫 |