-- ============================================
-- mall-order 服务：购物车、订单、订单项、金额快照、售后、Outbox
-- 版本：V1.0.0
-- 参考：docs/design/03_系统详细设计.md 1.3 节 + 1.6 节
-- ============================================

-- ----------------------------
-- 1. 购物车表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_order_cart` (
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT         COMMENT '主键，自增',
    `user_id`     bigint unsigned NOT NULL                        COMMENT '用户 ID',
    `sku_id`      bigint unsigned NOT NULL                        COMMENT 'SKU ID',
    `spu_id`      bigint unsigned NOT NULL                        COMMENT 'SPU ID',
    `sku_code`    varchar(64)     NOT NULL                        COMMENT 'SKU 编码（冗余，快速展示）',
    `sku_name`    varchar(200)    DEFAULT NULL                    COMMENT 'SKU 销售名称（冗余）',
    `main_image`  varchar(500)    DEFAULT NULL                    COMMENT '商品主图 URL（冗余）',
    `price`       bigint unsigned NOT NULL                        COMMENT '当前销售价（单位：分），加购时快照',
    `quantity`    int unsigned    NOT NULL DEFAULT 1              COMMENT '加入数量',
    `is_selected` tinyint unsigned DEFAULT 1                      COMMENT '是否选中',
    `is_deleted`  tinyint unsigned DEFAULT 0                      COMMENT '逻辑删除标志',
    `create_time` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    UNIQUE KEY `uk_user_sku` (`user_id`, `sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='购物车表';

-- ----------------------------
-- 2. 订单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_order` (
    `id`              bigint unsigned NOT NULL AUTO_INCREMENT         COMMENT '主键，自增',
    `order_no`        varchar(32)     NOT NULL                        COMMENT '订单号，格式 JH + 时间戳 + 随机数',
    `user_id`         bigint unsigned NOT NULL                        COMMENT '用户 ID',
    `order_status`    tinyint unsigned NOT NULL                       COMMENT '订单状态',
    `total_amount`    bigint unsigned NOT NULL                        COMMENT '商品总金额（单位：分）',
    `discount_amount` bigint unsigned DEFAULT 0                       COMMENT '优惠总金额（单位：分）',
    `freight_amount`  bigint unsigned DEFAULT 0                       COMMENT '运费金额（单位：分）',
    `pay_amount`      bigint unsigned NOT NULL                        COMMENT '实付金额（单位：分）',
    `pay_time`        datetime        DEFAULT NULL                    COMMENT '支付成功时间',
    `delivery_time`   datetime        DEFAULT NULL                    COMMENT '发货时间',
    `complete_time`   datetime        DEFAULT NULL                    COMMENT '交易完成时间',
    `cancel_time`     datetime        DEFAULT NULL                    COMMENT '取消时间',
    `cancel_type`     varchar(20)     DEFAULT NULL                    COMMENT '取消类型',
    `cancel_reason`   varchar(200)    DEFAULT NULL                    COMMENT '取消原因',
    `pay_expire_time` datetime        NOT NULL                        COMMENT '支付过期时间，默认创建后 30 分钟',
    `remark`          varchar(200)    DEFAULT NULL                    COMMENT '买家备注',
    `idempotent_key`  varchar(64)     DEFAULT NULL                    COMMENT '幂等键',
    `is_deleted`      tinyint unsigned DEFAULT 0                      COMMENT '逻辑删除标志',
    `create_by`       varchar(64)     DEFAULT NULL                    COMMENT '创建人（管理员代下单时记录）',
    `update_by`       varchar(64)     DEFAULT NULL                    COMMENT '修改人',
    `create_time`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `version`         int unsigned    DEFAULT 0                       COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    UNIQUE KEY `uk_idempotent_key` (`idempotent_key`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_id_order_status` (`user_id`, `order_status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_order_status_pay_expire` (`order_status`, `pay_expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单表';

-- ----------------------------
-- 3. 订单项表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_order_item` (
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT         COMMENT '主键，自增',
    `order_id`    bigint unsigned NOT NULL                        COMMENT '订单 ID',
    `spu_id`      bigint unsigned NOT NULL                        COMMENT 'SPU ID（快照）',
    `sku_id`      bigint unsigned NOT NULL                        COMMENT 'SKU ID（快照）',
    `sku_code`    varchar(64)     NOT NULL                        COMMENT 'SKU 编码（快照）',
    `sku_name`    varchar(200)    NOT NULL                        COMMENT 'SKU 名称（快照）',
    `spu_name`    varchar(200)    NOT NULL                        COMMENT 'SPU 名称（快照）',
    `main_image`  varchar(500)    DEFAULT NULL                    COMMENT '商品主图快照 URL',
    `attrs_json`  text            DEFAULT NULL                    COMMENT '销售属性 JSON 快照',
    `quantity`    int unsigned    NOT NULL                        COMMENT '购买数量',
    `price`       bigint unsigned NOT NULL                        COMMENT '成交单价（单位：分）',
    `total_price` bigint unsigned NOT NULL                        COMMENT '单项总价（单位：分）= price × quantity',
    `is_deleted`  tinyint unsigned DEFAULT 0                      COMMENT '逻辑删除标志',
    `create_time` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单项表';

-- ----------------------------
-- 4. 订单金额快照表（与订单 1:1）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_order_amount` (
    `id`                      bigint unsigned NOT NULL AUTO_INCREMENT         COMMENT '主键，自增',
    `order_id`                bigint unsigned NOT NULL                        COMMENT '订单 ID，与订单一对一',
    `items_json`              text            NOT NULL                        COMMENT '商品明细快照 JSON',
    `coupon_snapshot_json`    text            DEFAULT NULL                    COMMENT '优惠券使用快照 JSON',
    `promotion_snapshot_json` text            DEFAULT NULL                    COMMENT '活动优惠快照 JSON',
    `points_discount`         bigint unsigned DEFAULT 0                       COMMENT '积分抵扣金额（单位：分）',
    `total_amount`            bigint unsigned NOT NULL                        COMMENT '商品总金额（单位：分）',
    `discount_amount`         bigint unsigned NOT NULL                        COMMENT '优惠总金额（单位：分）',
    `freight_amount`          bigint unsigned NOT NULL                        COMMENT '运费（单位：分）',
    `pay_amount`              bigint unsigned NOT NULL                        COMMENT '实付金额（单位：分）',
    `is_deleted`              tinyint unsigned DEFAULT 0                      COMMENT '逻辑删除标志',
    `create_time`             datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单金额快照表';

-- ----------------------------
-- 5. 售后申请表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_order_after_sale` (
    `id`                     bigint unsigned NOT NULL AUTO_INCREMENT         COMMENT '主键，自增',
    `after_sale_no`          varchar(32)     NOT NULL                        COMMENT '售后单号',
    `order_id`               bigint unsigned NOT NULL                        COMMENT '关联订单 ID',
    `order_item_id`          bigint unsigned DEFAULT NULL                    COMMENT '关联订单项 ID，为空则整单退款',
    `user_id`                bigint unsigned NOT NULL                        COMMENT '申请人用户 ID',
    `after_sale_type`        tinyint unsigned NOT NULL                       COMMENT '售后类型',
    `reason`                 varchar(200)    DEFAULT NULL                    COMMENT '退款原因',
    `amount`                 bigint unsigned DEFAULT NULL                    COMMENT '退款金额（单位：分）',
    `after_sale_status`      tinyint unsigned NOT NULL DEFAULT 0             COMMENT '售后状态',
    `apply_time`             datetime        NOT NULL                        COMMENT '申请时间',
    `approve_time`           datetime        DEFAULT NULL                    COMMENT '审核时间',
    `approve_remark`         varchar(200)    DEFAULT NULL                    COMMENT '审核意见',
    `return_express_company` varchar(50)     DEFAULT NULL                    COMMENT '退货物流公司',
    `return_express_no`      varchar(50)     DEFAULT NULL                    COMMENT '退货物流单号',
    `receipt_time`           datetime        DEFAULT NULL                    COMMENT '商家确认收货时间',
    `is_deleted`             tinyint unsigned DEFAULT 0                      COMMENT '逻辑删除标志',
    `create_by`              varchar(64)     DEFAULT NULL                    COMMENT '创建人',
    `update_by`              varchar(64)     DEFAULT NULL                    COMMENT '修改人',
    `create_time`            datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`            datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_after_sale_no` (`after_sale_no`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='售后申请表';

-- ----------------------------
-- 6. Outbox 消息表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_outbox` (
    `id`              bigint       NOT NULL                            COMMENT '主键，雪花 ID',
    `message_id`      varchar(64)  NOT NULL                            COMMENT '消息全局唯一 ID（雪花），消费方幂等去重',
    `topic`           varchar(128) NOT NULL                            COMMENT '消息主题',
    `event_type`      varchar(64)  NOT NULL                            COMMENT '事件类型，如 OrderCreatedEvent',
    `aggregate_type`  varchar(64)  NOT NULL                            COMMENT '聚合类型，如 order',
    `aggregate_id`    varchar(64)  NOT NULL                            COMMENT '聚合 ID，如订单号',
    `payload`         text         NOT NULL                            COMMENT '消息体 JSON，使用稳定 DTO 序列化',
    `status`          varchar(16)  NOT NULL DEFAULT 'NEW'              COMMENT '投递状态',
    `retry_count`     int          DEFAULT 0                           COMMENT '已重试次数',
    `next_retry_time` datetime     DEFAULT NULL                        COMMENT '下次重试时间',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_id` (`message_id`),
    KEY `idx_status_next_retry` (`status`, `next_retry_time`),
    KEY `idx_aggregate` (`aggregate_type`, `aggregate_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Outbox 消息表';
