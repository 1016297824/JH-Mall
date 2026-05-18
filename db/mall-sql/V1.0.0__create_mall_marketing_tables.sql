-- ============================================
-- mall-marketing 服务：优惠券定义、优惠券记录、活动、促销规则、Outbox
-- 版本：V1.0.0
-- 参考：docs/design/03_系统详细设计.md 1.5 节 + 1.6 节
-- ============================================

-- ----------------------------
-- 1. 优惠券定义表（商家创建的券模板）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_marketing_coupon` (
    `id`               bigint unsigned NOT NULL AUTO_INCREMENT         COMMENT '主键，自增',
    `coupon_name`      varchar(100)    NOT NULL                        COMMENT '优惠券名称',
    `coupon_type`      tinyint unsigned NOT NULL                       COMMENT '优惠券类型',
    `face_value`       bigint unsigned NOT NULL                        COMMENT '优惠面值（单位：分），满减/无门槛时 = 减免金额，折扣券 = 0',
    `discount_rate`    tinyint unsigned DEFAULT NULL                   COMMENT '折扣率（百分比），折扣券专用，80=8折',
    `discount_limit`   bigint unsigned DEFAULT NULL                    COMMENT '折扣上限（单位：分），折扣券专用',
    `min_order_amount` bigint unsigned DEFAULT 0                       COMMENT '最低订单金额门槛（单位：分），0=无门槛',
    `total_count`      int unsigned    NOT NULL                        COMMENT '发行总量，0=不限量',
    `remain_count`     int unsigned    NOT NULL                        COMMENT '剩余可领取数量',
    `per_user_limit`   int unsigned    DEFAULT 1                       COMMENT '每人限领数量',
    `use_start_time`   datetime        NOT NULL                        COMMENT '有效期开始时间',
    `use_end_time`     datetime        NOT NULL                        COMMENT '有效期截止时间',
    `coupon_status`    tinyint unsigned NOT NULL DEFAULT 0             COMMENT '优惠券状态',
    `is_deleted`       tinyint unsigned DEFAULT 0                      COMMENT '逻辑删除标志',
    `create_by`        varchar(64)     DEFAULT NULL                    COMMENT '创建人',
    `update_by`        varchar(64)     DEFAULT NULL                    COMMENT '修改人',
    `create_time`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `version`          int unsigned    DEFAULT 0                       COMMENT '乐观锁版本号，控制领取并发',
    PRIMARY KEY (`id`),
    KEY `idx_coupon_status` (`coupon_status`),
    KEY `idx_use_time` (`use_start_time`, `use_end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='优惠券定义表';

-- ----------------------------
-- 2. 用户优惠券记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_marketing_coupon_record` (
    `id`            bigint unsigned NOT NULL AUTO_INCREMENT         COMMENT '主键，自增',
    `coupon_id`     bigint unsigned NOT NULL                        COMMENT '关联优惠券定义 ID',
    `user_id`       bigint unsigned NOT NULL                        COMMENT '领取用户 ID',
    `coupon_code`   varchar(64)     NOT NULL                        COMMENT '优惠券编码（全局唯一），格式 CPN + 时间戳 + 随机数',
    `record_status` tinyint unsigned NOT NULL DEFAULT 1             COMMENT '记录状态',
    `order_no`      varchar(32)     DEFAULT NULL                    COMMENT '使用/锁定的订单号',
    `face_value`    bigint unsigned NOT NULL                        COMMENT '领取时的券面值快照（单位：分）',
    `lock_time`     datetime        DEFAULT NULL                    COMMENT '锁定时间',
    `use_time`      datetime        DEFAULT NULL                    COMMENT '使用（核销）时间',
    `release_time`  datetime        DEFAULT NULL                    COMMENT '释放时间',
    `expire_time`   datetime        NOT NULL                        COMMENT '过期时间',
    `is_deleted`    tinyint unsigned DEFAULT 0                      COMMENT '逻辑删除标志',
    `create_time`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_coupon_code` (`coupon_code`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_id_status` (`user_id`, `record_status`),
    KEY `idx_coupon_id` (`coupon_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_status_expire` (`record_status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户优惠券记录表';

-- ----------------------------
-- 3. 活动表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_marketing_promotion` (
    `id`               bigint unsigned NOT NULL AUTO_INCREMENT         COMMENT '主键，自增',
    `promotion_name`   varchar(100)    NOT NULL                        COMMENT '活动名称',
    `promotion_type`   tinyint unsigned NOT NULL                       COMMENT '活动类型',
    `start_time`       datetime        NOT NULL                        COMMENT '活动开始时间',
    `end_time`         datetime        NOT NULL                        COMMENT '活动结束时间',
    `promotion_status` tinyint unsigned NOT NULL DEFAULT 0             COMMENT '活动状态',
    `description`      varchar(500)    DEFAULT NULL                    COMMENT '活动描述',
    `banner_image`     varchar(500)    DEFAULT NULL                    COMMENT '活动 Banner 图 URL',
    `sort_order`       int unsigned    DEFAULT 0                       COMMENT '排序值',
    `is_deleted`       tinyint unsigned DEFAULT 0                      COMMENT '逻辑删除标志',
    `create_by`        varchar(64)     DEFAULT NULL                    COMMENT '创建人',
    `update_by`        varchar(64)     DEFAULT NULL                    COMMENT '修改人',
    `create_time`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_promotion_status` (`promotion_status`),
    KEY `idx_start_end_time` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='活动表';

-- ----------------------------
-- 4. 促销规则表（一个活动可有多条规则）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_marketing_promotion_rule` (
    `id`               bigint unsigned NOT NULL AUTO_INCREMENT         COMMENT '主键，自增',
    `promotion_id`     bigint unsigned NOT NULL                        COMMENT '关联活动 ID',
    `rule_type`        tinyint unsigned NOT NULL                       COMMENT '规则类型',
    `threshold_amount` bigint unsigned NOT NULL                        COMMENT '门槛金额（单位：分）',
    `benefit_amount`   bigint unsigned DEFAULT NULL                    COMMENT '优惠金额（单位：分），满减专用',
    `benefit_rate`     tinyint unsigned DEFAULT NULL                   COMMENT '折扣率，满折专用，80=8折',
    `is_exclusive`     tinyint unsigned DEFAULT 0                      COMMENT '是否互斥',
    `sort_order`       int unsigned    DEFAULT 0                       COMMENT '优先级，越小越优先匹配',
    `is_deleted`       tinyint unsigned DEFAULT 0                      COMMENT '逻辑删除标志',
    `create_time`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_promotion_id` (`promotion_id`),
    KEY `idx_promotion_sort` (`promotion_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='促销规则表';

-- ----------------------------
-- 5. Outbox 消息表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_outbox` (
    `id`              bigint       NOT NULL                            COMMENT '主键，雪花 ID',
    `message_id`      varchar(64)  NOT NULL                            COMMENT '消息全局唯一 ID（雪花），消费方幂等去重',
    `topic`           varchar(128) NOT NULL                            COMMENT '消息主题',
    `event_type`      varchar(64)  NOT NULL                            COMMENT '事件类型',
    `aggregate_type`  varchar(64)  NOT NULL                            COMMENT '聚合类型',
    `aggregate_id`    varchar(64)  NOT NULL                            COMMENT '聚合 ID',
    `payload`         text         NOT NULL                            COMMENT '消息体 JSON',
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

-- ----------------------------
-- 种子数据：测试优惠券
-- ----------------------------
INSERT INTO `mall_marketing_coupon` (`id`, `coupon_name`, `coupon_type`, `face_value`, `min_order_amount`, `total_count`, `remain_count`, `per_user_limit`, `use_start_time`, `use_end_time`, `coupon_status`) VALUES
(1, '满 100 减 10',      1, 1000,  10000, 1000, 1000, 1,  '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1),
(2, '满 200 享 8.5 折',  2, 0,     20000, 500,  500,  1,  '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1),
(3, '无门槛减 5 元',      3, 500,   0,     2000, 2000, 1,  '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1);
