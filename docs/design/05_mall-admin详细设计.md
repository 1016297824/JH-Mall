# JH-Store mall-admin 模块详细设计

> 基于系统详细设计 `03_系统详细设计.md` 展开。mall-admin 是纯管理端模块，位于 `server/ruoyi/ruoyi-modules/mall-admin/`，零依赖 mall 模块，技术栈与若依框架完全一致。

---

## 1 模块概述

### 1.1 背景

原管理端 Controller 散落在 `server/mall/` 各模块的 `controller/admin/` 包下，与 C 端 API Controller 共存在同一个微服务进程中。为达成管理端与 C 端**完全独立**的目标，将全部管理端代码剥离到独立的 `mall-admin` 模块。

mall-admin 是标准若依微服务模块，与 `ruoyi-system`、`ruoyi-gen` 平级，拥有独立的 Spring Boot 进程、独立的 Nacos 配置、独立的网关路由。

### 1.2 子领域

| 子领域   | 对应数据表（库）                                                                                                                                                                                            | 说明                               |
| -------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------- |
| 用户管理 | `mall.mall_user`, `mall.mall_user_address`, `mall.mall_user_member`, `mall.mall_user_member_level`, `mall.mall_user_points_account`, `mall.mall_user_points_log`, `mall.mall_user_growth_log` | C 端用户/会员/地址/积分/成长值管理 |
| 商品管理 | `mall.mall_product_spu`, `mall.mall_product_sku`, `mall.mall_product_category`, `mall.mall_product_brand`, `mall.mall_product_sku_stock`                                                          | SPU/SKU/类目/品牌/库存管理         |
| 订单管理 | `mall.mall_order`, `mall.mall_order_item`, `mall.mall_order_cart`, `mall.mall_order_amount`, `mall.mall_order_after_sale`                                                                         | 订单/购物车/价格快照/售后管理      |
| 支付管理 | `mall.mall_payment`, `mall.mall_payment_channel`, `mall.mall_payment_callback_log`                                                                                                                    | 支付单/支付渠道/回调记录管理       |
| 营销管理 | `mall.mall_marketing_promotion`, `mall.mall_marketing_coupon`                                                                                                                                           | 促销活动/优惠券管理                |

> 所有表在 `mall` 数据库（MySQL）中，与 C 端共享同一组表。mall-admin 通过自己的 Mapper XML 直接读写，不经过 Feign。

### 1.3 依赖关系

```
mall-admin (9207端口)
  ├── MySQL: mall 库（驱动 + Druid 连接池）
  ├── Redis: 通过 ruoyi-common-redis（传递依赖）
  ├── ruoyi-common-datasource (Maven): 多数据源 + Druid
  ├── ruoyi-common-datascope (Maven): 数据权限过滤
  ├── ruoyi-common-log (Maven): 操作日志（@Log → RemoteLogService → ry-cloud.sys_oper_log）
  ├── ruoyi-common-swagger (Maven): API 文档
  └── Gateway: 管理端前端通过网关 /mall-admin/** 访问

> **关键边界**：mall-admin **不依赖**任何 `com.mall` 模块（如 mall-common、mall-api、mall-user 等），技术栈与 `ruoyi-system` 完全一致。业务枚举和常量由各 Domain 自行维护，不引用 `com.mall.common.enums`。
```

> **关键边界**：mall-admin **不依赖**任何 `com.mall` 模块（如 mall-common、mall-api、mall-user 等），技术栈与 `ruoyi-system` 完全一致。业务枚举和常量由各 Domain 自行维护，不引用 `com.mall.common.enums`。

---

## 2 包结构与模块清单

### 2.1 包结构

```
server/ruoyi/ruoyi-modules/mall-admin/
├── pom.xml                                     # parent=ruoyi-modules:3.6.8
└── src/
    ├── main/
    │   ├── java/com/mall/admin/
    │   │   ├── MallAdminApplication.java       # Spring Boot 启动类
    │   │   ├── user/
    │   │   │   ├── controller/MallUserController.java
    │   │   │   ├── controller/MallUserAddressController.java
    │   │   │   ├── controller/MallUserMemberController.java
    │   │   │   ├── controller/MallUserMemberLevelController.java
    │   │   │   ├── controller/MallUserPointsAccountController.java
    │   │   │   ├── controller/MallUserPointsLogController.java
    │   │   │   ├── controller/MallUserGrowthLogController.java
    │   │   │   ├── service/IMallUserService.java
    │   │   │   ├── service/impl/MallUserServiceImpl.java
    │   │   │   ├── domain/MallUser.java
    │   │   │   └── mapper/MallUserMapper.java
    │   │   ├── product/
    │   │   │   ├── controller/MallProductSpuController.java
    │   │   │   ├── controller/MallProductCategoryController.java
    │   │   │   ├── controller/MallProductBrandController.java
    │   │   │   ├── controller/MallProductSkuStockController.java
    │   │   │   ├── service/...
    │   │   │   ├── domain/...
    │   │   │   └── mapper/...
    │   │   ├── order/
    │   │   │   ├── controller/MallOrderController.java
    │   │   │   ├── controller/MallOrderAfterSaleController.java
    │   │   │   ├── controller/MallOrderAmountController.java
    │   │   │   ├── controller/MallOrderCartController.java
    │   │   │   ├── service/...
    │   │   │   ├── domain/...
    │   │   │   └── mapper/...
    │   │   ├── payment/
    │   │   │   ├── controller/MallPaymentController.java
    │   │   │   ├── controller/MallPaymentChannelController.java
    │   │   │   ├── controller/MallPaymentCallbackLogController.java
    │   │   │   ├── service/...
    │   │   │   ├── domain/...
    │   │   │   └── mapper/...
    │   │   └── marketing/
    │   │       ├── controller/MallMarketingPromotionController.java
    │   │       ├── controller/MallMarketingCouponController.java
    │   │       ├── service/...
    │   │       ├── domain/...
    │   │       └── mapper/...
    │   └── resources/
    │       ├── mapper/
    │       │   ├── user/MallUserMapper.xml
    │       │   ├── user/MallUserAddressMapper.xml
    │       │   ├── user/MallUserMemberMapper.xml
    │       │   ├── user/MallUserMemberLevelMapper.xml
    │       │   ├── user/MallUserPointsAccountMapper.xml
    │       │   ├── user/MallUserPointsLogMapper.xml
    │       │   ├── user/MallUserGrowthLogMapper.xml
    │       │   ├── product/MallProductSpuMapper.xml
    │       │   ├── product/MallProductCategoryMapper.xml
    │       │   ├── product/MallProductBrandMapper.xml
    │       │   ├── product/MallProductSkuStockMapper.xml
    │       │   ├── order/MallOrderMapper.xml
    │       │   ├── order/MallOrderAfterSaleMapper.xml
    │       │   ├── order/MallOrderAmountMapper.xml
    │       │   ├── order/MallOrderCartMapper.xml
    │       │   ├── payment/MallPaymentMapper.xml
    │       │   ├── payment/MallPaymentChannelMapper.xml
    │       │   ├── payment/MallPaymentCallbackLogMapper.xml
    │       │   ├── marketing/MallMarketingPromotionMapper.xml
    │       │   └── marketing/MallMarketingCouponMapper.xml
    │       ├── logback.xml                      # 独立日志配置
    │       └── bootstrap.yml                    # Nacos 配置引用
```

> 共 **20 个 Controller**，对应 7 个 user + 4 个 product + 4 个 order + 3 个 payment + 2 个 marketing。包名从原 `com.mall.{module}.controller.admin` 调整为 `com.mall.admin.{domain}.controller`，`@RequestMapping` 路径不变。

### 2.2 技术栈

| 维度            | 说明                                                                        |
| --------------- | --------------------------------------------------------------------------- |
| 基础框架        | Spring Boot 4.0.3 + Spring Cloud 2025.1.0                                   |
| Web             | `spring-boot-starter-web`（传递依赖）                                     |
| ORM             | MyBatis（`mybatis-spring-boot-starter` 4.0.1），**非** MyBatis-Plus |
| 分页            | PageHelper（2.1.0，通过 `ruoyi-common-core` 传递）                        |
| 连接池          | Druid 1.2.28（通过 `ruoyi-common-datasource`）                            |
| 多数据源        | dynamic-ds 4.5.0（通过 `ruoyi-common-datasource`）                        |
| 鉴权            | `@RequiresPermissions`（通过 `ruoyi-common-security` 传递）             |
| 日志            | `@Log`（`ruoyi-common-log`）+ `logback.xml` 独立文件                  |
| 数据权限        | `@DataScope`（`ruoyi-common-datascope`）                                |
| 注册中心        | Nacos Discovery 2025.1.0.0                                                  |
| 配置中心        | Nacos Config 2025.1.0.0                                                     |
| 响应体          | `AjaxResult` + `TableDataInfo`（`ruoyi-common-core`）                 |
| Controller 基类 | `BaseController`（`ruoyi-common-core`）                                 |
| Excel 导入导出  | `ExcelUtil`（`ruoyi-common-core`，基于 poi-ooxml）                      |

---

## 3 过滤器链与路由

### 3.1 网关处理流程

mall-admin 的管理端 Controller 通过服务发现路径访问：`/mall-admin/{domain}/{action}`。

请求经过网关时的处理：

| Order | 过滤器            | 对 `/mall-admin/**` 的行为                                                                    |
| :---: | ----------------- | ----------------------------------------------------------------------------------------------- |
| -200 | AuthFilter        | 路径**不在**白名单 `security.ignore.whites` 中 → 校验管理端 JWT，无有效 token 返回 401 |
| -150 | MallAuthFilter    | 路径非 `/api/**` 开头 → **直接放行**                                                   |
| 路由 | discovery.locator | `mall-admin` 注册到 Nacos，自动路由到 mall-admin(9207)                                        |

> mall-admin 纯属管理端体系，不走 MallAuthFilter。管理端 JWT 由 ruoyi-auth（9201）签发。

### 3.2 网关路由配置变更

`ruoyi-gateway-dev.yml` 需新增：

```yaml
spring:
  cloud:
    gateway:
      routes:
        # 管理端 - mall-admin（通过发现服务自动路由，无需显式配置）
```

由于 `discovery.locator.enabled: true`，mall-admin 在 Nacos 注册后，网关自动将 `/mall-admin/**` 路由到 `mall-admin` 服务，**无需显式路由**。

### 3.3 白名单说明

AuthFilter 白名单 `security.ignore.whites` 中：

| 路径               | 用途                                                 |
| ------------------ | ---------------------------------------------------- |
| `/api/**`        | C 端接口，放行 AuthFilter，由 MallAuthFilter 处理    |
| `/mall-admin/**` | **不在白名单中**，由 AuthFilter 校验管理端 JWT |

---

## 4 接口清单

接口风格与若依 CRUD 生成代码完全一致，每个 Controller 包含：

| 方法   | 路径                 | 权限标识                 | 说明           |
| ------ | -------------------- | ------------------------ | -------------- |
| GET    | `/{domain}/list`   | `{模块}:{实体}:list`   | 分页查询列表   |
| GET    | `/{domain}/export` | `{模块}:{实体}:export` | Excel 导出     |
| GET    | `/{domain}/{id}`   | `{模块}:{实体}:query`  | 按 ID 查询详情 |
| POST   | `/{domain}`        | `{模块}:{实体}:add`    | 新增           |
| PUT    | `/{domain}`        | `{模块}:{实体}:edit`   | 修改           |
| DELETE | `/{domain}/{ids}`  | `{模块}:{实体}:remove` | 批量删除       |

以用户管理为例：

| 方法 | 路径                 | 权限标识                       | Controller                      |
| ---- | -------------------- | ------------------------------ | ------------------------------- |
| GET  | `/user/list`       | `mall-admin:user:list`       | MallUserController              |
| GET  | `/address/list`    | `mall-admin:address:list`    | MallUserAddressController       |
| GET  | `/member/list`     | `mall-admin:member:list`     | MallUserMemberController        |
| GET  | `/level/list`      | `mall-admin:level:list`      | MallUserMemberLevelController   |
| GET  | `/account/list`    | `mall-admin:account:list`    | MallUserPointsAccountController |
| GET  | `/points_log/list` | `mall-admin:points_log:list` | MallUserPointsLogController     |
| GET  | `/growth_log/list` | `mall-admin:growth_log:list` | MallUserGrowthLogController     |

> 权限标识统一使用 `mall-admin:` 前缀，与原 `mall-{模块}:` 前缀不同，需要在管理端权限管理模块中更新。

---

## 5 Nacos 配置

### 5.1 新建配置 `mall-admin-dev.yml`

通过 Nacos 控制台新建（DataId: `mall-admin-dev.yml`，Group: `DEFAULT_GROUP`）：

```yaml
# spring 配置
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:
  datasource:
    druid:
      stat-view-servlet:
        enabled: true
        loginUsername: ruoyi
        loginPassword: 123456
    dynamic:
      druid:
        initial-size: 5
        min-idle: 5
        maxActive: 20
        maxWait: 60000
        connectTimeout: 30000
        socketTimeout: 60000
        timeBetweenEvictionRunsMillis: 60000
        minEvictableIdleTimeMillis: 300000
        validationQuery: SELECT 1 FROM DUAL
        testWhileIdle: true
        testOnBorrow: false
        testOnReturn: false
        poolPreparedStatements: true
        maxPoolPreparedStatementPerConnectionSize: 20
        filters: stat,slf4j
        connectionProperties: druid.stat.mergeSql\=true;druid.stat.slowSqlMillis\=5000
      datasource:
        # 主库数据源
        master:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://localhost:3306/mall?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8
          username: root
          password: 138992
        # 从库数据源
        # slave:
          # username:
          # password:
          # url:
          # driver-class-name:

# mybatis 配置
mybatis:
  # 搜索指定包别名
  typeAliasesPackage: com.mall.admin
  # 配置 mapper 的扫描，找到所有 mapper.xml 映射文件
  mapperLocations: classpath:mapper/**/*.xml

# springdoc 配置
springdoc:
  gatewayUrl: http://localhost:8080/${spring.application.name}
  api-docs:
    # 是否开启接口文档
    enabled: true
  info:
    # 标题
    title: '管理端模块接口文档'
    # 描述
    description: '管理端模块接口描述'
    # 作者信息
    contact:
      name: RuoYi
      url: https://ruoyi.vip

# mall-admin 业务配置
mall:
  admin:
```

### 5.2 本地配置文件 `bootstrap.yml`

```yaml
# mall-admin 管理端
server:
  port: 9207

spring:
  application:
    name: mall-admin
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

---

## 6 设计决策

| 决策           | 结论                                                         | 原因                                                               |
| -------------- | ------------------------------------------------------------ | ------------------------------------------------------------------ |
| 包命名         | `com.mall.admin.{domain}.controller/service/domain/mapper` | 按业务域分包，清晰区分 5 个子领域                                  |
| ORM 框架       | 原生 MyBatis，非 MyBatis-Plus                                | 与若依框架一致，生成代码即原生 MyBatis 风格                        |
| 依赖 mall 模块 | **零依赖**                                             | 管理端与 C 端完全独立，技术栈与 ruoyi-system 一致                  |
| 业务枚举       | 各 Domain 自行维护 `Integer`/`String` 字段               | 不引用 `com.mall.common.enums`，避免对 mall-common 的依赖        |
| 网关路由       | `discovery.locator` 自动发现，不配显式路由                 | 与管理端其他模块（ruoyi-system）一致                               |
| 鉴权           | AuthFilter（管理端 JWT）+`@RequiresPermissions`            | 与若依管理端体系一致                                               |
| 日志审计       | `@Log` → RemoteLogService → `ry-cloud.sys_oper_log`    | 与若依管理端体系一致，不走 `mall.mall_audit_log`                 |
| 响应体         | `AjaxResult` / `TableDataInfo`（`ruoyi-common-core`）  | 与若依管理端体系一致，不用 `MallResult<T>`                       |
| 端口 | 9207 | 在若依管理端端口段（9201-9206）之后分配 |
| Java 版本      | 17                                                           | 与若依模块一致（`server/ruoyi/pom.xml` 定义），不使用 mall 的 21 |
| 启动顺序       | `mall-admin(9207)` 放在所有 mall 模块之后启动              | 无依赖关系，但为清晰排在 C 端之后                                  |

---

## 7 启动顺序（更新）

```bash
# 必须先启动 Nacos（依赖 MySQL）
@other/nacos/bin/startup.cmd -m standalone

# 网关 + 若依基础服务
ruoyi-gateway(8080) → ruoyi-auth(9201) → ruoyi-system(9202) → ruoyi-gen(9203)

# 商城 C 端服务
mall-auth(9301) → mall-user(9302) → mall-product(9303) → mall-order(9304) → mall-payment(9305) → mall-marketing(9306) → mall-search(9307)

# 管理端服务
mall-admin(9207)
```

---

## 8 文档变更清单

以下文档需要同步更新：

| 文档                                   | 变更内容                                                                                                                                    |
| -------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| `AGENTS.md`                          | 模块列表追加 mall-admin(9207)；@EnableFeignClients 规则更新（去掉"双端"描述）；Nacos 配置表追加 mall-admin-dev.yml；Controller 隔离说明更新 |
| `docs/design/04_gateway详细设计.md`  | 路由策略 §3.1 管理端路由示例追加 `/mall-admin/*`；过滤器链表格追加 mall-admin 行；白名单配置确认                                         |
| `docs/开发/01_管理端代码生成流程.md` | 输出目录改为 `ruoyi-modules/mall-admin/`                                                                                                  |
