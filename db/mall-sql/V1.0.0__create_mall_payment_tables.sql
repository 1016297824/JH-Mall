-- ============================================
-- mall-payment 服务：支付单、退款单、渠道配置、回调日志、Outbox
-- 版本：V1.0.0
-- 参考：docs/design/03_系统详细设计.md 1.4 节 + 1.6 节
-- ============================================

-- ----------------------------
-- 1. 支付单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_payment` (
    `id`                 bigint unsigned NOT NULL AUTO_INCREMENT         COMMENT '主键，自增',
    `payment_no`         varchar(32)     NOT NULL                        COMMENT '支付单号，格式 PAY + 时间戳 + 随机数',
    `order_no`           varchar(32)     NOT NULL                        COMMENT '关联订单号',
    `user_id`            bigint unsigned NOT NULL                        COMMENT '付款用户 ID',
    `pay_amount`         bigint unsigned NOT NULL                        COMMENT '支付金额（单位：分）',
    `channel_code`       varchar(20)     NOT NULL                        COMMENT '支付渠道编码',
    `channel_payment_no` varchar(128)    DEFAULT NULL                    COMMENT '渠道侧支付单号，对账用',
    `channel_pay_status` varchar(20)     DEFAULT NULL                    COMMENT '渠道侧支付状态',
    `payment_status`     tinyint unsigned NOT NULL DEFAULT 0             COMMENT '支付单状态',
    `pay_success_time`   datetime        DEFAULT NULL                    COMMENT '支付成功时间',
    `expire_time`        datetime        NOT NULL                        COMMENT '支付过期时间',
    `notify_url`         varchar(500)    DEFAULT NULL                    COMMENT '异步通知地址',
    `idempotent_key`     varchar(64)     DEFAULT NULL                    COMMENT '幂等键',
    `is_deleted`         tinyint unsigned DEFAULT 0                      COMMENT '逻辑删除标志',
    `create_by`          varchar(64)     DEFAULT NULL                    COMMENT '创建人',
    `update_by`          varchar(64)     DEFAULT NULL                    COMMENT '修改人',
    `create_time`        datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `version`            int unsigned    DEFAULT 0                       COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_no` (`payment_no`),
    UNIQUE KEY `uk_idempotent_key` (`idempotent_key`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_channel_payment_no` (`channel_payment_no`(64)),
    KEY `idx_payment_status_expire` (`payment_status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='支付单表';

-- ----------------------------
-- 2. 退款单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_payment_refund` (
    `id`                    bigint unsigned NOT NULL AUTO_INCREMENT         COMMENT '主键，自增',
    `refund_no`             varchar(32)     NOT NULL                        COMMENT '退款单号，格式 REF + 时间戳 + 随机数',
    `payment_id`            bigint unsigned NOT NULL                        COMMENT '关联支付单 ID',
    `order_no`              varchar(32)     NOT NULL                        COMMENT '关联订单号',
    `after_sale_no`         varchar(32)     DEFAULT NULL                    COMMENT '关联售后单号',
    `user_id`               bigint unsigned NOT NULL                        COMMENT '退款用户 ID',
    `refund_amount`         bigint unsigned NOT NULL                        COMMENT '退款金额（单位：分）',
    `refund_reason`         varchar(200)    DEFAULT NULL                    COMMENT '退款原因',
    `channel_code`          varchar(20)     NOT NULL                        COMMENT '退款渠道编码，必须与原始支付渠道一致',
    `channel_refund_no`     varchar(128)    DEFAULT NULL                    COMMENT '渠道侧退款单号，对账用',
    `channel_refund_status` varchar(20)     DEFAULT NULL                    COMMENT '渠道侧退款状态',
    `refund_status`         tinyint unsigned NOT NULL DEFAULT 0             COMMENT '退款单状态',
    `refund_success_time`   datetime        DEFAULT NULL                    COMMENT '退款成功时间',
    `idempotent_key`        varchar(64)     DEFAULT NULL                    COMMENT '幂等键',
    `is_deleted`            tinyint unsigned DEFAULT 0                      COMMENT '逻辑删除标志',
    `create_by`             varchar(64)     DEFAULT NULL                    COMMENT '创建人',
    `update_by`             varchar(64)     DEFAULT NULL                    COMMENT '修改人',
    `create_time`           datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`           datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `version`               int unsigned    DEFAULT 0                       COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_refund_no` (`refund_no`),
    UNIQUE KEY `uk_idempotent_key` (`idempotent_key`),
    KEY `idx_payment_id` (`payment_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_after_sale_no` (`after_sale_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='退款单表';

-- ----------------------------
-- 3. 支付渠道配置表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_payment_channel` (
    `id`           bigint unsigned NOT NULL AUTO_INCREMENT         COMMENT '主键，自增',
    `channel_code` varchar(20)     NOT NULL                        COMMENT '渠道编码',
    `channel_name` varchar(50)     NOT NULL                        COMMENT '渠道展示名称',
    `channel_type` tinyint unsigned NOT NULL                       COMMENT '渠道类型',
    `config_json`  text            NOT NULL                        COMMENT '渠道配置',
    `is_enabled`   tinyint unsigned DEFAULT 1                      COMMENT '是否启用',
    `sort_order`   int unsigned    DEFAULT 0                       COMMENT '排序值',
    `is_deleted`   tinyint unsigned DEFAULT 0                      COMMENT '逻辑删除标志',
    `create_by`    varchar(64)     DEFAULT NULL                    COMMENT '创建人',
    `update_by`    varchar(64)     DEFAULT NULL                    COMMENT '修改人',
    `create_time`  datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_channel_code` (`channel_code`),
    KEY `idx_is_enabled` (`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='支付渠道配置表';

-- ----------------------------
-- 4. 支付回调记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_payment_callback_log` (
    `id`             bigint unsigned NOT NULL AUTO_INCREMENT         COMMENT '主键，自增',
    `payment_no`     varchar(32)     DEFAULT NULL                    COMMENT '关联支付单号',
    `refund_no`      varchar(32)     DEFAULT NULL                    COMMENT '关联退款单号',
    `channel_code`   varchar(20)     NOT NULL                        COMMENT '渠道编码',
    `callback_type`  varchar(20)     NOT NULL                        COMMENT '回调类型',
    `raw_body`       text            NOT NULL                        COMMENT '原始回调报文 JSON',
    `is_verified`    tinyint unsigned DEFAULT 0                      COMMENT '验签结果',
    `process_status` tinyint unsigned DEFAULT 0                      COMMENT '处理状态',
    `process_time`   datetime        DEFAULT NULL                    COMMENT '处理完成时间',
    `process_result` varchar(200)    DEFAULT NULL                    COMMENT '处理结果说明',
    `nonce`          varchar(64)     DEFAULT NULL                    COMMENT '回调防重放 nonce',
    `is_deleted`     tinyint unsigned DEFAULT 0                      COMMENT '逻辑删除标志',
    `create_time`    datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_nonce` (`nonce`(64)),
    KEY `idx_payment_no` (`payment_no`),
    KEY `idx_refund_no` (`refund_no`),
    KEY `idx_process_status` (`process_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='支付回调记录表';

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
-- 种子数据：支付渠道配置
-- ----------------------------
INSERT INTO `mall_payment_channel` (`id`, `channel_code`, `channel_name`, `channel_type`, `config_json`, `sort_order`) VALUES
(1, 'wechat', '微信支付', 1, '{}', 1),
(2, 'alipay', '支付宝',   1, '{}', 2);
