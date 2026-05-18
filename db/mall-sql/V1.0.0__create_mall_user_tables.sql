-- ============================================
-- mall-user 服务：用户、会员、地址、积分、成长值
-- 版本：V1.0.0
-- 参考：docs/design/03_系统详细设计.md 1.1 节
-- ============================================

-- ----------------------------
-- 1. 用户账号表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_user` (
    `id`                  bigint unsigned NOT NULL AUTO_INCREMENT              COMMENT '主键，自增',
    `phone`               varchar(20)     NOT NULL                             COMMENT '手机号',
    `phone_hash`          varchar(64)     NOT NULL                             COMMENT '手机号哈希',
    `password`            varchar(256)    NOT NULL                             COMMENT '密码哈希',
    `nickname`            varchar(50)     DEFAULT NULL                         COMMENT '昵称',
    `avatar`              varchar(500)    DEFAULT NULL                         COMMENT '头像 URL',
    `email`               varchar(100)    DEFAULT NULL                         COMMENT '邮箱',
    `email_hash`          varchar(64)     DEFAULT NULL                         COMMENT '邮箱哈希',
    `gender`              tinyint unsigned DEFAULT 0                           COMMENT '性别',
    `birthday`            date            DEFAULT NULL                         COMMENT '生日',
    `user_status`         tinyint unsigned DEFAULT 0                           COMMENT '用户状态',
    `register_type`       varchar(20)     DEFAULT NULL                         COMMENT '注册方式',
    `register_ip`         varchar(50)     DEFAULT NULL                         COMMENT '注册 IP',
    `register_time`       datetime        DEFAULT NULL                         COMMENT '注册时间',
    `last_login_time`     datetime        DEFAULT NULL                         COMMENT '最后登录时间',
    `last_login_ip`       varchar(50)     DEFAULT NULL                         COMMENT '最后登录 IP',
    `is_privacy_agreed`   tinyint unsigned DEFAULT 0                           COMMENT '是否同意隐私协议',
    `privacy_agreed_time` datetime        DEFAULT NULL                         COMMENT '同意隐私协议时间',
    `wechat_openid`       varchar(100)    DEFAULT NULL                         COMMENT '微信 OpenID',
    `wechat_unionid`      varchar(100)    DEFAULT NULL                         COMMENT '微信 UnionID',
    `is_deleted`          tinyint unsigned DEFAULT 0                           COMMENT '逻辑删除标志',
    `create_by`           varchar(64)     DEFAULT NULL                         COMMENT '创建人',
    `update_by`           varchar(64)     DEFAULT NULL                         COMMENT '修改人',
    `create_time`         datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    `update_time`         datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `version`             int unsigned    DEFAULT 0                           COMMENT '乐观锁版本号',
    `remark`              varchar(500)    DEFAULT NULL                         COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone_hash` (`phone_hash`),
    UNIQUE KEY `uk_email_hash` (`email_hash`),
    UNIQUE KEY `uk_wechat_openid` (`wechat_openid`),
    KEY `idx_user_status_register_time` (`user_status`, `register_time`),
    KEY `idx_nickname` (`nickname`(20)),
    KEY `idx_register_time` (`register_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户账号表';

-- ----------------------------
-- 2. 会员等级定义表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_user_member_level` (
    `id`            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    `level_name`    varchar(50)     NOT NULL                COMMENT '等级名称',
    `level_value`   tinyint unsigned NOT NULL               COMMENT '等级值',
    `min_growth`    int unsigned    NOT NULL                COMMENT '该等级所需的最低成长值',
    `max_growth`    int unsigned    NOT NULL                COMMENT '该等级的最高成长值',
    `icon`          varchar(500)    DEFAULT NULL            COMMENT '等级图标 URL',
    `benefits_json` text            DEFAULT NULL            COMMENT '权益',
    `is_deleted`    tinyint unsigned DEFAULT 0              COMMENT '逻辑删除标志',
    `create_time`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_level_value` (`level_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会员等级定义表';

-- ----------------------------
-- 3. 用户会员信息表（与用户 1:1）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_user_member` (
    `id`               bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    `user_id`          bigint unsigned NOT NULL                COMMENT '用户 ID',
    `level_id`         bigint unsigned DEFAULT NULL            COMMENT '当前等级',
    `growth`           int unsigned    DEFAULT 0               COMMENT '当前成长值',
    `total_growth`     int unsigned    DEFAULT 0               COMMENT '累计成长值',
    `level_start_time` datetime        DEFAULT NULL            COMMENT '最近一次等级生效时间',
    `level_end_time`   datetime        DEFAULT NULL            COMMENT '等级到期时间',
    `become_time`      datetime        DEFAULT NULL            COMMENT '首次成为会员时间',
    `is_deleted`       tinyint unsigned DEFAULT 0              COMMENT '逻辑删除标志',
    `create_time`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_level_id` (`level_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户会员信息表';

-- ----------------------------
-- 4. 地址簿表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_user_address` (
    `id`             bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    `user_id`        bigint unsigned NOT NULL                COMMENT '用户 ID',
    `receiver_name`  varchar(50)     NOT NULL                COMMENT '收件人姓名',
    `receiver_phone` varchar(20)     NOT NULL                COMMENT '收件人手机号',
    `province`       varchar(50)     DEFAULT NULL            COMMENT '省',
    `city`           varchar(50)     DEFAULT NULL            COMMENT '市',
    `district`       varchar(50)     DEFAULT NULL            COMMENT '区',
    `detail_address` varchar(200)    DEFAULT NULL            COMMENT '详细地址',
    `zip_code`       varchar(10)     DEFAULT NULL            COMMENT '邮编',
    `is_default`     tinyint unsigned DEFAULT 0              COMMENT '是否默认地址',
    `label`          varchar(20)     DEFAULT NULL            COMMENT '地址标签',
    `is_deleted`     tinyint unsigned DEFAULT 0              COMMENT '逻辑删除标志',
    `create_time`    datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_id_is_default` (`user_id`, `is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='地址簿表';

-- ----------------------------
-- 5. 积分账户表（与用户 1:1）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_user_points_account` (
    `id`               bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    `user_id`          bigint unsigned NOT NULL                COMMENT '用户 ID',
    `total_points`     int unsigned    DEFAULT 0               COMMENT '累计积分',
    `available_points` int unsigned    DEFAULT 0               COMMENT '可用积分余额',
    `used_points`      int unsigned    DEFAULT 0               COMMENT '已使用积分',
    `expired_points`   int unsigned    DEFAULT 0               COMMENT '已过期积分',
    `is_deleted`       tinyint unsigned DEFAULT 0              COMMENT '逻辑删除标志',
    `create_time`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='积分账户表';

-- ----------------------------
-- 6. 积分流水表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_user_points_log` (
    `id`            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    `user_id`       bigint unsigned NOT NULL                COMMENT '用户 ID',
    `biz_type`      varchar(50)     NOT NULL                COMMENT '业务类型',
    `biz_no`        varchar(64)     DEFAULT NULL            COMMENT '业务单号',
    `change_type`   tinyint unsigned NOT NULL               COMMENT '变动方向',
    `points`        int unsigned    NOT NULL                COMMENT '本次变动积分值',
    `before_points` int unsigned    DEFAULT NULL            COMMENT '变动前积分余额',
    `after_points`  int unsigned    DEFAULT NULL            COMMENT '变动后积分余额',
    `remark`        varchar(200)    DEFAULT NULL            COMMENT '变动原因说明',
    `is_deleted`    tinyint unsigned DEFAULT 0              COMMENT '逻辑删除标志',
    `create_time`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_id_biz_type` (`user_id`, `biz_type`),
    KEY `idx_biz_no` (`biz_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='积分流水表';

-- ----------------------------
-- 7. 成长值流水表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_user_growth_log` (
    `id`             bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    `user_id`        bigint unsigned NOT NULL                COMMENT '用户 ID',
    `biz_type`       varchar(50)     NOT NULL                COMMENT '业务类型',
    `biz_no`         varchar(64)     DEFAULT NULL            COMMENT '业务单号',
    `change_type`    tinyint unsigned NOT NULL               COMMENT '变动方向',
    `growth`         int unsigned    NOT NULL                COMMENT '本次变动成长值',
    `before_growth`  int unsigned    DEFAULT NULL            COMMENT '变动前成长值余额',
    `after_growth`   int unsigned    DEFAULT NULL            COMMENT '变动后成长值余额',
    `remark`         varchar(200)    DEFAULT NULL            COMMENT '变动原因说明',
    `is_deleted`     tinyint unsigned DEFAULT 0              COMMENT '逻辑删除标志',
    `create_time`    datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_id_biz_type` (`user_id`, `biz_type`),
    KEY `idx_biz_no` (`biz_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='成长值流水表';

-- ----------------------------
-- 种子数据：会员等级定义
-- ----------------------------
INSERT INTO `mall_user_member_level` (`id`, `level_name`, `level_value`, `min_growth`, `max_growth`, `benefits_json`) VALUES
(1, '普通会员', 1, 0,     999,  '{"discountRate":100,"freeShipping":false,"pointsMultiplier":1.0}'),
(2, '银卡会员', 2, 1000,  2999, '{"discountRate":98,"freeShipping":false,"freeShippingThreshold":9900,"pointsMultiplier":1.1}'),
(3, '金卡会员', 3, 3000,  8999, '{"discountRate":95,"freeShipping":true,"pointsMultiplier":1.2}'),
(4, '钻石会员', 4, 9000,  99999,'{"discountRate":90,"freeShipping":true,"pointsMultiplier":1.5}');
