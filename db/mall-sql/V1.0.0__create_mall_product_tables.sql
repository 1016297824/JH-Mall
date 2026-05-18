-- ============================================
-- mall-product 服务：类目、品牌、SPU、SKU、库存
-- 版本：V1.0.0
-- 参考：docs/design/03_系统详细设计.md 1.2 节
-- ============================================

-- ----------------------------
-- 1. 商品类目表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_product_category` (
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT         COMMENT '主键，自增',
    `parent_id`   bigint unsigned NOT NULL DEFAULT 0              COMMENT '父类目 ID，0 表示顶级类目',
    `name`        varchar(50)     NOT NULL                        COMMENT '类目名称',
    `level`       tinyint unsigned NOT NULL                       COMMENT '类目层级：1=一级 2=二级 3=三级',
    `icon`        varchar(500)    DEFAULT NULL                    COMMENT '类目标识图标 URL',
    `sort_order`  int unsigned    DEFAULT 0                       COMMENT '排序值，越小越靠前',
    `is_visible`  tinyint unsigned DEFAULT 1                      COMMENT '是否前端可见：1=可见 0=隐藏',
    `path`        varchar(255)    DEFAULT NULL                    COMMENT '类目路径，如 /1/10/100',
    `is_deleted`  tinyint unsigned DEFAULT 0                      COMMENT '逻辑删除标志',
    `create_time` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_level` (`level`),
    KEY `idx_path` (`path`(100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品类目表';

-- ----------------------------
-- 2. 品牌表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_product_brand` (
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT         COMMENT '主键，自增',
    `name`        varchar(100)    NOT NULL                        COMMENT '品牌名称',
    `logo`        varchar(500)    DEFAULT NULL                    COMMENT '品牌 Logo URL',
    `description` varchar(500)    DEFAULT NULL                    COMMENT '品牌简介',
    `sort_order`  int unsigned    DEFAULT 0                       COMMENT '排序值',
    `is_deleted`  tinyint unsigned DEFAULT 0                      COMMENT '逻辑删除标志',
    `create_time` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`(30))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='品牌表';

-- ----------------------------
-- 3. 商品 SPU 表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_product_spu` (
    `id`              bigint unsigned NOT NULL AUTO_INCREMENT         COMMENT '主键，自增',
    `category_id`     bigint unsigned NOT NULL                        COMMENT '所属三级类目 ID',
    `brand_id`        bigint unsigned DEFAULT NULL                    COMMENT '所属品牌 ID',
    `spu_name`        varchar(200)    NOT NULL                        COMMENT 'SPU 名称（商品标题）',
    `spu_description` text            DEFAULT NULL                    COMMENT '商品详情描述（富文本 HTML）',
    `main_image`      varchar(500)    DEFAULT NULL                    COMMENT '商品主图 URL',
    `images_json`     text            DEFAULT NULL                    COMMENT '轮播图 JSON 数组：["url1","url2"]',
    `price_min`       bigint unsigned DEFAULT 0                       COMMENT '最低销售价（单位：分）',
    `price_max`       bigint unsigned DEFAULT 0                       COMMENT '最高销售价（单位：分）',
    `sales_count`     int unsigned    DEFAULT 0                       COMMENT '累计销量',
    `review_count`    int unsigned    DEFAULT 0                       COMMENT '评价条数',
    `publish_status`  tinyint unsigned DEFAULT 0                      COMMENT '上下架状态：0=下架 1=上架',
    `verify_status`   tinyint unsigned DEFAULT 0                      COMMENT '审核状态：0=待审核 1=审核通过 2=审核驳回',
    `is_deleted`      tinyint unsigned DEFAULT 0                      COMMENT '逻辑删除标志',
    `create_by`       varchar(64)     DEFAULT NULL                    COMMENT '创建人',
    `update_by`       varchar(64)     DEFAULT NULL                    COMMENT '修改人',
    `create_time`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `version`         int unsigned    DEFAULT 0                       COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_brand_id` (`brand_id`),
    KEY `idx_publish_status` (`publish_status`),
    KEY `idx_category_publish` (`category_id`, `publish_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品 SPU 表';

-- ----------------------------
-- 4. SKU 表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_product_sku` (
    `id`           bigint unsigned NOT NULL AUTO_INCREMENT         COMMENT '主键，自增',
    `spu_id`       bigint unsigned NOT NULL                        COMMENT '所属 SPU ID',
    `sku_code`     varchar(64)     NOT NULL                        COMMENT 'SKU 编码（全局唯一）',
    `sku_name`     varchar(200)    NOT NULL                        COMMENT 'SKU 销售名称',
    `attrs_json`   text            DEFAULT NULL                    COMMENT '销售属性 JSON：[{"k":"颜色","v":"蓝色"}]',
    `price`        bigint unsigned NOT NULL                        COMMENT '销售价（单位：分）',
    `market_price` bigint unsigned DEFAULT NULL                    COMMENT '市场价/划线价（单位：分）',
    `cost_price`   bigint unsigned DEFAULT NULL                    COMMENT '成本价（单位：分）',
    `image`        varchar(500)    DEFAULT NULL                    COMMENT 'SKU 级图片',
    `weight`       int unsigned    DEFAULT 0                       COMMENT '重量（单位：克），用于运费计算',
    `sales_count`  int unsigned    DEFAULT 0                       COMMENT '该 SKU 累计销量',
    `is_deleted`   tinyint unsigned DEFAULT 0                      COMMENT '逻辑删除标志',
    `create_time`  datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sku_code` (`sku_code`),
    KEY `idx_spu_id` (`spu_id`),
    KEY `idx_spu_price` (`spu_id`, `price`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='SKU 表';

-- ----------------------------
-- 5. SKU 库存表（与 SKU 1:1）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `mall_product_sku_stock` (
    `id`              bigint unsigned NOT NULL AUTO_INCREMENT         COMMENT '主键，自增',
    `sku_id`          bigint unsigned NOT NULL                        COMMENT 'SKU ID，与 SKU 一对一',
    `total_stock`     int unsigned    DEFAULT 0                       COMMENT '总库存 = 可用 + 锁定 + 已售 + 冻结',
    `available_stock` int unsigned    DEFAULT 0                       COMMENT '可用库存：当前可下单购买的数量',
    `locked_stock`    int unsigned    DEFAULT 0                       COMMENT '锁定库存：下单未支付时锁定',
    `sold_stock`      int unsigned    DEFAULT 0                       COMMENT '已售库存：已支付完成的累计销量',
    `frozen_stock`    int unsigned    DEFAULT 0                       COMMENT '冻结库存：售后申请中暂冻',
    `is_deleted`      tinyint unsigned DEFAULT 0                      COMMENT '逻辑删除标志',
    `create_time`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `version`         int unsigned    DEFAULT 0                       COMMENT '乐观锁版本号，库存扣减防超卖',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='SKU 库存表';

-- ----------------------------
-- 种子数据：示例类目 + 品牌
-- ----------------------------
INSERT INTO `mall_product_category` (`id`, `parent_id`, `name`, `level`, `sort_order`, `path`) VALUES
(1,  0, '手机数码', 1, 1, '/1'),
(2,  1, '手机通讯', 2, 1, '/1/2'),
(3,  2, '智能手机', 3, 1, '/1/2/3'),
(4,  0, '电脑办公', 1, 2, '/4'),
(5,  4, '笔记本',   2, 1, '/4/5'),
(6,  0, '服饰鞋包', 1, 3, '/6');

INSERT INTO `mall_product_brand` (`id`, `name`, `sort_order`) VALUES
(1, 'Apple',  1),
(2, '华为',   2),
(3, 'Nike',   3);
