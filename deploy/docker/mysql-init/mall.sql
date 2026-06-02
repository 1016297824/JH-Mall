-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: localhost    Database: mall
-- ------------------------------------------------------
-- Server version	8.0.42

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `mall`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `mall` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `mall`;

--
-- Table structure for table `mall_marketing_coupon`
--

DROP TABLE IF EXISTS `mall_marketing_coupon`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_marketing_coupon` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `coupon_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '优惠券名称',
  `coupon_type` tinyint unsigned NOT NULL COMMENT '优惠券类型',
  `face_value` bigint unsigned NOT NULL COMMENT '优惠面值（单位：分），满减/无门槛时 = 减免金额，折扣券 = 0',
  `discount_rate` tinyint unsigned DEFAULT NULL COMMENT '折扣率（百分比），折扣券专用，80=8折',
  `discount_limit` bigint unsigned DEFAULT NULL COMMENT '折扣上限（单位：分），折扣券专用',
  `min_order_amount` bigint unsigned DEFAULT '0' COMMENT '最低订单金额门槛（单位：分），0=无门槛',
  `total_count` int unsigned NOT NULL COMMENT '发行总量，0=不限量',
  `remain_count` int unsigned NOT NULL COMMENT '剩余可领取数量',
  `per_user_limit` int unsigned DEFAULT '1' COMMENT '每人限领数量',
  `use_start_time` datetime NOT NULL COMMENT '有效期开始时间',
  `use_end_time` datetime NOT NULL COMMENT '有效期截止时间',
  `coupon_status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '优惠券状态',
  `is_deleted` tinyint unsigned DEFAULT '0' COMMENT '逻辑删除标志',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `version` int unsigned DEFAULT '0' COMMENT '乐观锁版本号，控制领取并发',
  PRIMARY KEY (`id`),
  KEY `idx_coupon_status` (`coupon_status`),
  KEY `idx_use_time` (`use_start_time`,`use_end_time`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='优惠券定义表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mall_marketing_coupon`
--

LOCK TABLES `mall_marketing_coupon` WRITE;
/*!40000 ALTER TABLE `mall_marketing_coupon` DISABLE KEYS */;
INSERT INTO `mall_marketing_coupon` VALUES (1,'满 100 减 10',1,1000,NULL,NULL,10000,1000,1000,1,'2026-01-01 00:00:00','2026-12-31 23:59:59',1,0,NULL,NULL,'2026-05-19 04:15:17','2026-05-19 04:15:17',0),(2,'满 200 享 8.5 折',2,0,NULL,NULL,20000,500,500,1,'2026-01-01 00:00:00','2026-12-31 23:59:59',1,0,NULL,NULL,'2026-05-19 04:15:17','2026-05-19 04:15:17',0),(3,'无门槛减 5 元',3,500,NULL,NULL,0,2000,2000,1,'2026-01-01 00:00:00','2026-12-31 23:59:59',1,0,NULL,NULL,'2026-05-19 04:15:17','2026-05-19 04:15:17',0);
/*!40000 ALTER TABLE `mall_marketing_coupon` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mall_marketing_coupon_record`
--

DROP TABLE IF EXISTS `mall_marketing_coupon_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_marketing_coupon_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `coupon_id` bigint unsigned NOT NULL COMMENT '关联优惠券定义 ID',
  `user_id` bigint unsigned NOT NULL COMMENT '领取用户 ID',
  `coupon_code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '优惠券编码（全局唯一），格式 CPN + 时间戳 + 随机数',
  `record_status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '记录状态',
  `order_no` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '使用/锁定的订单号',
  `face_value` bigint unsigned NOT NULL COMMENT '领取时的券面值快照（单位：分）',
  `lock_time` datetime DEFAULT NULL COMMENT '锁定时间',
  `use_time` datetime DEFAULT NULL COMMENT '使用（核销）时间',
  `release_time` datetime DEFAULT NULL COMMENT '释放时间',
  `expire_time` datetime NOT NULL COMMENT '过期时间',
  `is_deleted` tinyint unsigned DEFAULT '0' COMMENT '逻辑删除标志',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupon_code` (`coupon_code`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_id_status` (`user_id`,`record_status`),
  KEY `idx_coupon_id` (`coupon_id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_status_expire` (`record_status`,`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户优惠券记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mall_marketing_coupon_record`
--

LOCK TABLES `mall_marketing_coupon_record` WRITE;
/*!40000 ALTER TABLE `mall_marketing_coupon_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `mall_marketing_coupon_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mall_marketing_promotion`
--

DROP TABLE IF EXISTS `mall_marketing_promotion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_marketing_promotion` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `promotion_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '活动名称',
  `promotion_type` tinyint unsigned NOT NULL COMMENT '活动类型',
  `start_time` datetime NOT NULL COMMENT '活动开始时间',
  `end_time` datetime NOT NULL COMMENT '活动结束时间',
  `promotion_status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '活动状态',
  `description` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '活动描述',
  `banner_image` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '活动 Banner 图 URL',
  `sort_order` int unsigned DEFAULT '0' COMMENT '排序值',
  `is_deleted` tinyint unsigned DEFAULT '0' COMMENT '逻辑删除标志',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_promotion_status` (`promotion_status`),
  KEY `idx_start_end_time` (`start_time`,`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='活动表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mall_marketing_promotion`
--

LOCK TABLES `mall_marketing_promotion` WRITE;
/*!40000 ALTER TABLE `mall_marketing_promotion` DISABLE KEYS */;
/*!40000 ALTER TABLE `mall_marketing_promotion` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mall_marketing_promotion_rule`
--

DROP TABLE IF EXISTS `mall_marketing_promotion_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_marketing_promotion_rule` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `promotion_id` bigint unsigned NOT NULL COMMENT '关联活动 ID',
  `rule_type` tinyint unsigned NOT NULL COMMENT '规则类型',
  `threshold_amount` bigint unsigned NOT NULL COMMENT '门槛金额（单位：分）',
  `benefit_amount` bigint unsigned DEFAULT NULL COMMENT '优惠金额（单位：分），满减专用',
  `benefit_rate` tinyint unsigned DEFAULT NULL COMMENT '折扣率，满折专用，80=8折',
  `is_exclusive` tinyint unsigned DEFAULT '0' COMMENT '是否互斥',
  `sort_order` int unsigned DEFAULT '0' COMMENT '优先级，越小越优先匹配',
  `is_deleted` tinyint unsigned DEFAULT '0' COMMENT '逻辑删除标志',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_promotion_id` (`promotion_id`),
  KEY `idx_promotion_sort` (`promotion_id`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='促销规则表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mall_marketing_promotion_rule`
--

LOCK TABLES `mall_marketing_promotion_rule` WRITE;
/*!40000 ALTER TABLE `mall_marketing_promotion_rule` DISABLE KEYS */;
/*!40000 ALTER TABLE `mall_marketing_promotion_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mall_order`
--

DROP TABLE IF EXISTS `mall_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_order` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `order_no` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '订单号，格式 JH + 时间戳 + 随机数',
  `user_id` bigint unsigned NOT NULL COMMENT '用户 ID',
  `order_status` tinyint unsigned NOT NULL COMMENT '订单状态',
  `total_amount` bigint unsigned NOT NULL COMMENT '商品总金额（单位：分）',
  `discount_amount` bigint unsigned DEFAULT '0' COMMENT '优惠总金额（单位：分）',
  `freight_amount` bigint unsigned DEFAULT '0' COMMENT '运费金额（单位：分）',
  `pay_amount` bigint unsigned NOT NULL COMMENT '实付金额（单位：分）',
  `pay_time` datetime DEFAULT NULL COMMENT '支付成功时间',
  `delivery_time` datetime DEFAULT NULL COMMENT '发货时间',
  `complete_time` datetime DEFAULT NULL COMMENT '交易完成时间',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `cancel_type` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '取消类型',
  `cancel_reason` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '取消原因',
  `pay_expire_time` datetime NOT NULL COMMENT '支付过期时间，默认创建后 30 分钟',
  `remark` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '买家备注',
  `idempotent_key` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '幂等键',
  `is_deleted` tinyint unsigned DEFAULT '0' COMMENT '逻辑删除标志',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人（管理员代下单时记录）',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `version` int unsigned DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_idempotent_key` (`idempotent_key`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_id_order_status` (`user_id`,`order_status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_order_status_pay_expire` (`order_status`,`pay_expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mall_order`
--

LOCK TABLES `mall_order` WRITE;
/*!40000 ALTER TABLE `mall_order` DISABLE KEYS */;
/*!40000 ALTER TABLE `mall_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mall_order_after_sale`
--

DROP TABLE IF EXISTS `mall_order_after_sale`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_order_after_sale` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `after_sale_no` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '售后单号',
  `order_id` bigint unsigned NOT NULL COMMENT '关联订单 ID',
  `order_item_id` bigint unsigned DEFAULT NULL COMMENT '关联订单项 ID，为空则整单退款',
  `user_id` bigint unsigned NOT NULL COMMENT '申请人用户 ID',
  `after_sale_type` tinyint unsigned NOT NULL COMMENT '售后类型',
  `reason` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '退款原因',
  `amount` bigint unsigned DEFAULT NULL COMMENT '退款金额（单位：分）',
  `after_sale_status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '售后状态',
  `apply_time` datetime NOT NULL COMMENT '申请时间',
  `approve_time` datetime DEFAULT NULL COMMENT '审核时间',
  `approve_remark` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审核意见',
  `return_express_company` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '退货物流公司',
  `return_express_no` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '退货物流单号',
  `receipt_time` datetime DEFAULT NULL COMMENT '商家确认收货时间',
  `is_deleted` tinyint unsigned DEFAULT '0' COMMENT '逻辑删除标志',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_after_sale_no` (`after_sale_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='售后申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mall_order_after_sale`
--

LOCK TABLES `mall_order_after_sale` WRITE;
/*!40000 ALTER TABLE `mall_order_after_sale` DISABLE KEYS */;
/*!40000 ALTER TABLE `mall_order_after_sale` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mall_order_amount`
--

DROP TABLE IF EXISTS `mall_order_amount`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_order_amount` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `order_id` bigint unsigned NOT NULL COMMENT '订单 ID，与订单一对一',
  `items_json` text COLLATE utf8mb4_general_ci NOT NULL COMMENT '商品明细快照 JSON',
  `coupon_snapshot_json` text COLLATE utf8mb4_general_ci COMMENT '优惠券使用快照 JSON',
  `promotion_snapshot_json` text COLLATE utf8mb4_general_ci COMMENT '活动优惠快照 JSON',
  `points_discount` bigint unsigned DEFAULT '0' COMMENT '积分抵扣金额（单位：分）',
  `total_amount` bigint unsigned NOT NULL COMMENT '商品总金额（单位：分）',
  `discount_amount` bigint unsigned NOT NULL COMMENT '优惠总金额（单位：分）',
  `freight_amount` bigint unsigned NOT NULL COMMENT '运费（单位：分）',
  `pay_amount` bigint unsigned NOT NULL COMMENT '实付金额（单位：分）',
  `is_deleted` tinyint unsigned DEFAULT '0' COMMENT '逻辑删除标志',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单金额快照表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mall_order_amount`
--

LOCK TABLES `mall_order_amount` WRITE;
/*!40000 ALTER TABLE `mall_order_amount` DISABLE KEYS */;
/*!40000 ALTER TABLE `mall_order_amount` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mall_order_cart`
--

DROP TABLE IF EXISTS `mall_order_cart`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_order_cart` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `user_id` bigint unsigned NOT NULL COMMENT '用户 ID',
  `sku_id` bigint unsigned NOT NULL COMMENT 'SKU ID',
  `spu_id` bigint unsigned NOT NULL COMMENT 'SPU ID',
  `sku_code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'SKU 编码（冗余，快速展示）',
  `sku_name` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'SKU 销售名称（冗余）',
  `main_image` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '商品主图 URL（冗余）',
  `price` bigint unsigned NOT NULL COMMENT '当前销售价（单位：分），加购时快照',
  `quantity` int unsigned NOT NULL DEFAULT '1' COMMENT '加入数量',
  `is_selected` tinyint unsigned DEFAULT '1' COMMENT '是否选中',
  `is_deleted` tinyint unsigned DEFAULT '0' COMMENT '逻辑删除标志',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_sku` (`user_id`,`sku_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='购物车表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mall_order_cart`
--

LOCK TABLES `mall_order_cart` WRITE;
/*!40000 ALTER TABLE `mall_order_cart` DISABLE KEYS */;
/*!40000 ALTER TABLE `mall_order_cart` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mall_order_item`
--

DROP TABLE IF EXISTS `mall_order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_order_item` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `order_id` bigint unsigned NOT NULL COMMENT '订单 ID',
  `spu_id` bigint unsigned NOT NULL COMMENT 'SPU ID（快照）',
  `sku_id` bigint unsigned NOT NULL COMMENT 'SKU ID（快照）',
  `sku_code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'SKU 编码（快照）',
  `sku_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'SKU 名称（快照）',
  `spu_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'SPU 名称（快照）',
  `main_image` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '商品主图快照 URL',
  `attrs_json` text COLLATE utf8mb4_general_ci COMMENT '销售属性 JSON 快照',
  `quantity` int unsigned NOT NULL COMMENT '购买数量',
  `price` bigint unsigned NOT NULL COMMENT '成交单价（单位：分）',
  `total_price` bigint unsigned NOT NULL COMMENT '单项总价（单位：分）= price × quantity',
  `is_deleted` tinyint unsigned DEFAULT '0' COMMENT '逻辑删除标志',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单项表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mall_order_item`
--

LOCK TABLES `mall_order_item` WRITE;
/*!40000 ALTER TABLE `mall_order_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `mall_order_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mall_outbox`
--

DROP TABLE IF EXISTS `mall_outbox`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_outbox` (
  `id` bigint NOT NULL COMMENT '主键，雪花 ID',
  `message_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息全局唯一 ID（雪花），消费方幂等去重',
  `topic` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息主题',
  `event_type` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '事件类型',
  `aggregate_type` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '聚合类型',
  `aggregate_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '聚合 ID',
  `payload` text COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息体 JSON',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'NEW' COMMENT '投递状态',
  `retry_count` int DEFAULT '0' COMMENT '已重试次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次重试时间',
  `scheduled_time` datetime DEFAULT NULL COMMENT '预约投递时间，NULL=立即投递',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_id` (`message_id`),
  KEY `idx_status_next_retry` (`status`,`next_retry_time`),
  KEY `idx_aggregate` (`aggregate_type`,`aggregate_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Outbox 消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mall_outbox`
--

LOCK TABLES `mall_outbox` WRITE;
/*!40000 ALTER TABLE `mall_outbox` DISABLE KEYS */;
/*!40000 ALTER TABLE `mall_outbox` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mall_payment`
--

DROP TABLE IF EXISTS `mall_payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_payment` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `payment_no` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '支付单号，格式 PAY + 时间戳 + 随机数',
  `order_no` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '关联订单号',
  `user_id` bigint unsigned NOT NULL COMMENT '付款用户 ID',
  `pay_amount` bigint unsigned NOT NULL COMMENT '支付金额（单位：分）',
  `channel_code` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '支付渠道编码',
  `channel_payment_no` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '渠道侧支付单号，对账用',
  `channel_pay_status` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '渠道侧支付状态',
  `payment_status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '支付单状态',
  `pay_success_time` datetime DEFAULT NULL COMMENT '支付成功时间',
  `expire_time` datetime NOT NULL COMMENT '支付过期时间',
  `notify_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '异步通知地址',
  `idempotent_key` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '幂等键',
  `is_deleted` tinyint unsigned DEFAULT '0' COMMENT '逻辑删除标志',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `version` int unsigned DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  UNIQUE KEY `uk_idempotent_key` (`idempotent_key`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_channel_payment_no` (`channel_payment_no`(64)),
  KEY `idx_payment_status_expire` (`payment_status`,`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='支付单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mall_payment`
--

LOCK TABLES `mall_payment` WRITE;
/*!40000 ALTER TABLE `mall_payment` DISABLE KEYS */;
/*!40000 ALTER TABLE `mall_payment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mall_payment_callback_log`
--

DROP TABLE IF EXISTS `mall_payment_callback_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_payment_callback_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `payment_no` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联支付单号',
  `refund_no` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联退款单号',
  `channel_code` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '渠道编码',
  `callback_type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '回调类型',
  `raw_body` text COLLATE utf8mb4_general_ci NOT NULL COMMENT '原始回调报文 JSON',
  `is_verified` tinyint unsigned DEFAULT '0' COMMENT '验签结果',
  `process_status` tinyint unsigned DEFAULT '0' COMMENT '处理状态',
  `process_time` datetime DEFAULT NULL COMMENT '处理完成时间',
  `process_result` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '处理结果说明',
  `nonce` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '回调防重放 nonce',
  `is_deleted` tinyint unsigned DEFAULT '0' COMMENT '逻辑删除标志',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_nonce` (`nonce`),
  KEY `idx_payment_no` (`payment_no`),
  KEY `idx_refund_no` (`refund_no`),
  KEY `idx_process_status` (`process_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='支付回调记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mall_payment_callback_log`
--

LOCK TABLES `mall_payment_callback_log` WRITE;
/*!40000 ALTER TABLE `mall_payment_callback_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `mall_payment_callback_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mall_payment_channel`
--

DROP TABLE IF EXISTS `mall_payment_channel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_payment_channel` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `channel_code` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '渠道编码',
  `channel_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '渠道展示名称',
  `channel_type` tinyint unsigned NOT NULL COMMENT '渠道类型',
  `config_json` text COLLATE utf8mb4_general_ci NOT NULL COMMENT '渠道配置',
  `is_enabled` tinyint unsigned DEFAULT '1' COMMENT '是否启用',
  `sort_order` int unsigned DEFAULT '0' COMMENT '排序值',
  `is_deleted` tinyint unsigned DEFAULT '0' COMMENT '逻辑删除标志',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_channel_code` (`channel_code`),
  KEY `idx_is_enabled` (`is_enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='支付渠道配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mall_payment_channel`
--

LOCK TABLES `mall_payment_channel` WRITE;
/*!40000 ALTER TABLE `mall_payment_channel` DISABLE KEYS */;
INSERT INTO `mall_payment_channel` VALUES (1,'wechat','微信支付',1,'{}',1,1,0,NULL,NULL,'2026-05-19 04:15:35','2026-05-19 04:15:35'),(2,'alipay','支付宝',1,'{}',1,2,0,NULL,NULL,'2026-05-19 04:15:35','2026-05-19 04:15:35');
/*!40000 ALTER TABLE `mall_payment_channel` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mall_payment_refund`
--

DROP TABLE IF EXISTS `mall_payment_refund`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_payment_refund` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `refund_no` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '退款单号，格式 REF + 时间戳 + 随机数',
  `payment_id` bigint unsigned NOT NULL COMMENT '关联支付单 ID',
  `order_no` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '关联订单号',
  `after_sale_no` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联售后单号',
  `user_id` bigint unsigned NOT NULL COMMENT '退款用户 ID',
  `refund_amount` bigint unsigned NOT NULL COMMENT '退款金额（单位：分）',
  `refund_reason` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '退款原因',
  `channel_code` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '退款渠道编码，必须与原始支付渠道一致',
  `channel_refund_no` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '渠道侧退款单号，对账用',
  `channel_refund_status` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '渠道侧退款状态',
  `refund_status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '退款单状态',
  `refund_success_time` datetime DEFAULT NULL COMMENT '退款成功时间',
  `idempotent_key` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '幂等键',
  `is_deleted` tinyint unsigned DEFAULT '0' COMMENT '逻辑删除标志',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `version` int unsigned DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  UNIQUE KEY `uk_idempotent_key` (`idempotent_key`),
  KEY `idx_payment_id` (`payment_id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_after_sale_no` (`after_sale_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='退款单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mall_payment_refund`
--

LOCK TABLES `mall_payment_refund` WRITE;
/*!40000 ALTER TABLE `mall_payment_refund` DISABLE KEYS */;
/*!40000 ALTER TABLE `mall_payment_refund` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mall_product_brand`
--

DROP TABLE IF EXISTS `mall_product_brand`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_product_brand` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '品牌名称',
  `logo` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '品牌 Logo URL',
  `description` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '品牌简介',
  `sort_order` int unsigned DEFAULT '0' COMMENT '排序值',
  `is_deleted` tinyint unsigned DEFAULT '0' COMMENT '逻辑删除标志',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`(30))
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='品牌表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mall_product_brand`
--

LOCK TABLES `mall_product_brand` WRITE;
/*!40000 ALTER TABLE `mall_product_brand` DISABLE KEYS */;
INSERT INTO `mall_product_brand` VALUES (1,'Apple',NULL,NULL,1,0,'2026-05-19 04:15:48','2026-05-19 04:15:48'),(2,'华为',NULL,NULL,2,0,'2026-05-19 04:15:48','2026-05-19 04:15:48'),(3,'Nike',NULL,NULL,3,0,'2026-05-19 04:15:48','2026-05-19 04:15:48'),(4,'百雀羚','https://cdn.example.com/brand/4/baiquejuan-logo.png','国货经典护肤品牌，专注草本天然护肤，产品覆盖面膜、面霜、精华等多个品类',4,0,'2026-06-01 16:31:16','2026-06-01 16:31:16');
/*!40000 ALTER TABLE `mall_product_brand` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mall_product_category`
--

DROP TABLE IF EXISTS `mall_product_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_product_category` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `parent_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '父类目 ID，0 表示顶级类目',
  `name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '类目名称',
  `level` tinyint unsigned NOT NULL COMMENT '类目层级',
  `icon` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '类目标识图标 URL',
  `sort_order` int unsigned DEFAULT '0' COMMENT '排序值，越小越靠前',
  `is_visible` tinyint unsigned DEFAULT '1' COMMENT '是否前端可见',
  `path` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '类目路径，如 /1/10/100',
  `is_deleted` tinyint unsigned DEFAULT '0' COMMENT '逻辑删除标志',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_level` (`level`),
  KEY `idx_path` (`path`(100))
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品类目表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mall_product_category`
--

LOCK TABLES `mall_product_category` WRITE;
/*!40000 ALTER TABLE `mall_product_category` DISABLE KEYS */;
INSERT INTO `mall_product_category` VALUES (1,0,'手机数码',1,NULL,1,1,'/1',0,'2026-05-19 04:15:48','2026-05-19 04:15:48'),(2,1,'手机通讯',2,NULL,1,1,'/1/2',0,'2026-05-19 04:15:48','2026-05-19 04:15:48'),(3,2,'智能手机',3,NULL,1,1,'/1/2/3',0,'2026-05-19 04:15:48','2026-05-19 04:15:48'),(4,0,'电脑办公',1,NULL,2,1,'/4',0,'2026-05-19 04:15:48','2026-05-19 04:15:48'),(5,4,'笔记本',2,NULL,1,1,'/4/5',0,'2026-05-19 04:15:48','2026-05-19 04:15:48'),(6,0,'服饰鞋包',1,NULL,3,1,'/6',0,'2026-05-19 04:15:48','2026-05-19 04:15:48'),(7,6,'电脑整机',2,NULL,1,1,'/6/7',0,'2026-05-31 18:33:43','2026-05-31 18:33:43'),(8,7,'笔记本',3,NULL,1,1,'/6/7/8',0,'2026-05-31 18:33:43','2026-05-31 18:33:43'),(9,6,'电脑配件',2,NULL,2,1,'/6/9',0,'2026-05-31 18:33:43','2026-05-31 18:33:43'),(10,6,'办公设备',2,NULL,3,1,'/6/10',0,'2026-05-31 18:33:43','2026-05-31 18:33:43'),(11,0,'家用电器',1,NULL,3,1,'/11',0,'2026-05-31 18:33:43','2026-05-31 18:33:43'),(12,11,'大家电',2,NULL,1,1,'/11/12',0,'2026-05-31 18:33:43','2026-05-31 18:33:43'),(13,12,'空调',3,NULL,1,1,'/11/12/13',0,'2026-05-31 18:33:43','2026-05-31 18:33:43'),(14,11,'生活电器',2,NULL,2,1,'/11/14',0,'2026-05-31 18:33:43','2026-05-31 18:33:43'),(15,11,'厨房电器',2,NULL,3,1,'/11/15',0,'2026-05-31 18:33:43','2026-05-31 18:33:43'),(16,0,'服饰鞋包',1,NULL,4,1,'/16',0,'2026-05-31 18:33:43','2026-05-31 18:33:43'),(17,16,'男装',2,NULL,1,1,'/16/17',0,'2026-05-31 18:33:43','2026-05-31 18:33:43'),(18,17,'上衣',3,NULL,1,1,'/16/17/18',0,'2026-05-31 18:33:43','2026-05-31 18:33:43'),(19,16,'女装',2,NULL,2,1,'/16/19',0,'2026-05-31 18:33:43','2026-05-31 18:33:43'),(20,16,'鞋靴',2,NULL,3,1,'/16/20',0,'2026-05-31 18:33:43','2026-05-31 18:33:43'),(21,0,'食品生鲜',1,NULL,5,1,'/21',0,'2026-05-31 18:33:43','2026-05-31 18:33:43'),(22,21,'休闲零食',2,NULL,1,1,'/21/22',0,'2026-05-31 18:33:43','2026-05-31 18:33:43'),(23,21,'酒水饮料',2,NULL,2,1,'/21/23',0,'2026-05-31 18:33:43','2026-05-31 18:33:43'),(24,0,'美妆个护',1,NULL,6,1,'/24',0,'2026-05-31 18:33:43','2026-05-31 18:33:43'),(25,24,'面部护理',2,NULL,1,1,'/24/25',0,'2026-05-31 18:33:43','2026-05-31 18:33:43'),(26,25,'面膜',3,NULL,1,1,'/24/25/26',0,'2026-05-31 18:33:43','2026-05-31 18:33:43'),(27,24,'彩妆',2,NULL,2,1,'/24/27',0,'2026-05-31 18:33:43','2026-05-31 18:33:43'),(28,0,'家居生活',1,NULL,7,1,'/28',0,'2026-05-31 18:33:43','2026-05-31 18:33:43'),(29,28,'家居家纺',2,NULL,1,1,'/28/29',0,'2026-05-31 18:33:43','2026-05-31 18:33:43'),(30,28,'厨具餐具',2,NULL,2,1,'/28/30',0,'2026-05-31 18:33:43','2026-05-31 18:33:43'),(31,0,'图书教育',1,NULL,8,1,'/31',0,'2026-05-31 18:33:43','2026-05-31 18:33:43'),(32,31,'图书',2,NULL,1,1,'/31/32',0,'2026-05-31 18:33:43','2026-05-31 18:33:43');
/*!40000 ALTER TABLE `mall_product_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mall_product_sku`
--

DROP TABLE IF EXISTS `mall_product_sku`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_product_sku` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `spu_id` bigint unsigned NOT NULL COMMENT '所属 SPU ID',
  `sku_code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'SKU 编码（全局唯一）',
  `sku_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'SKU 销售名称',
  `attrs_json` text COLLATE utf8mb4_general_ci COMMENT '销售属性 JSON：[{"k":"颜色","v":"蓝色"}]',
  `price` bigint unsigned NOT NULL COMMENT '销售价（单位：分）',
  `market_price` bigint unsigned DEFAULT NULL COMMENT '市场价/划线价（单位：分）',
  `cost_price` bigint unsigned DEFAULT NULL COMMENT '成本价（单位：分）',
  `image` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'SKU 级图片',
  `weight` int unsigned DEFAULT '0' COMMENT '重量（单位：克），用于运费计算',
  `sales_count` int unsigned DEFAULT '0' COMMENT '该 SKU 累计销量',
  `is_deleted` tinyint unsigned DEFAULT '0' COMMENT '逻辑删除标志',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_code` (`sku_code`),
  KEY `idx_spu_id` (`spu_id`),
  KEY `idx_spu_price` (`spu_id`,`price`)
) ENGINE=InnoDB AUTO_INCREMENT=203006 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='SKU 表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mall_product_sku`
--

LOCK TABLES `mall_product_sku` WRITE;
/*!40000 ALTER TABLE `mall_product_sku` DISABLE KEYS */;
INSERT INTO `mall_product_sku` VALUES (100101,1001,'IP15PM-256-BLUE','iPhone 15 Pro Max 256GB 蓝色','[{\"k\":\"颜色\",\"v\":\"蓝色\"},{\"k\":\"存储\",\"v\":\"256GB\"}]',899900,999900,NULL,'https://cdn.example.com/sku/100101.jpg',221,0,0,'2026-05-19 04:15:48','2026-05-19 04:15:48'),(100102,1001,'IP15PM-512-BLACK','iPhone 15 Pro Max 512GB 黑色','[{\"k\":\"颜色\",\"v\":\"黑色\"},{\"k\":\"存储\",\"v\":\"512GB\"}]',1099900,1199900,NULL,'https://cdn.example.com/sku/100102.jpg',221,0,0,'2026-05-19 04:15:48','2026-05-19 04:15:48'),(200101,2001,'AP-2001-1','Apple iPhone 16 Pro Max 沙漠色钛金属 256GB','[{\"k\":\"颜色\",\"v\":\"沙漠色钛金属\"},{\"k\":\"存储\",\"v\":\"256GB\"}]',999900,1099900,649935,'https://cdn.example.com/sku/200101.jpg',227,1568,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200102,2001,'AP-2001-2','Apple iPhone 16 Pro Max 沙漠色钛金属 512GB','[{\"k\":\"颜色\",\"v\":\"沙漠色钛金属\"},{\"k\":\"存储\",\"v\":\"512GB\"}]',1149900,1249900,747435,'https://cdn.example.com/sku/200102.jpg',227,1558,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200103,2001,'AP-2001-3','Apple iPhone 16 Pro Max 沙漠色钛金属 1TB','[{\"k\":\"颜色\",\"v\":\"沙漠色钛金属\"},{\"k\":\"存储\",\"v\":\"1TB\"}]',1299900,1399900,844935,'https://cdn.example.com/sku/200103.jpg',227,228,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200104,2001,'AP-2001-4','Apple iPhone 16 Pro Max 原色钛金属 256GB','[{\"k\":\"颜色\",\"v\":\"原色钛金属\"},{\"k\":\"存储\",\"v\":\"256GB\"}]',999900,1099900,649935,'https://cdn.example.com/sku/200104.jpg',227,241,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200105,2001,'AP-2001-5','Apple iPhone 16 Pro Max 原色钛金属 512GB','[{\"k\":\"颜色\",\"v\":\"原色钛金属\"},{\"k\":\"存储\",\"v\":\"512GB\"}]',1149900,1249900,747435,'https://cdn.example.com/sku/200105.jpg',227,104,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200106,2001,'AP-2001-6','Apple iPhone 16 Pro Max 白色钛金属 256GB','[{\"k\":\"颜色\",\"v\":\"白色钛金属\"},{\"k\":\"存储\",\"v\":\"256GB\"}]',999900,1099900,649935,'https://cdn.example.com/sku/200106.jpg',227,1486,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200201,2002,'AP-2002-1','Apple iPhone 16 Pro 沙漠色钛金属 128GB','[{\"k\":\"颜色\",\"v\":\"沙漠色钛金属\"},{\"k\":\"存储\",\"v\":\"128GB\"}]',799900,879900,519935,'https://cdn.example.com/sku/200201.jpg',199,1707,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200202,2002,'AP-2002-2','Apple iPhone 16 Pro 沙漠色钛金属 256GB','[{\"k\":\"颜色\",\"v\":\"沙漠色钛金属\"},{\"k\":\"存储\",\"v\":\"256GB\"}]',899900,979900,584935,'https://cdn.example.com/sku/200202.jpg',199,1479,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200203,2002,'AP-2002-3','Apple iPhone 16 Pro 沙漠色钛金属 512GB','[{\"k\":\"颜色\",\"v\":\"沙漠色钛金属\"},{\"k\":\"存储\",\"v\":\"512GB\"}]',1049900,1129900,682435,'https://cdn.example.com/sku/200203.jpg',199,490,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200204,2002,'AP-2002-4','Apple iPhone 16 Pro 原色钛金属 128GB','[{\"k\":\"颜色\",\"v\":\"原色钛金属\"},{\"k\":\"存储\",\"v\":\"128GB\"}]',799900,879900,519935,'https://cdn.example.com/sku/200204.jpg',199,828,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200205,2002,'AP-2002-5','Apple iPhone 16 Pro 原色钛金属 256GB','[{\"k\":\"颜色\",\"v\":\"原色钛金属\"},{\"k\":\"存储\",\"v\":\"256GB\"}]',899900,979900,584935,'https://cdn.example.com/sku/200205.jpg',199,591,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200301,2003,'AP-2003-1','Apple iPhone 16 群青色 128GB','[{\"k\":\"颜色\",\"v\":\"群青色\"},{\"k\":\"存储\",\"v\":\"128GB\"}]',599900,659900,389935,'https://cdn.example.com/sku/200301.jpg',170,1938,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200302,2003,'AP-2003-2','Apple iPhone 16 群青色 256GB','[{\"k\":\"颜色\",\"v\":\"群青色\"},{\"k\":\"存储\",\"v\":\"256GB\"}]',699900,759900,454935,'https://cdn.example.com/sku/200302.jpg',170,1748,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200303,2003,'AP-2003-3','Apple iPhone 16 深青色 128GB','[{\"k\":\"颜色\",\"v\":\"深青色\"},{\"k\":\"存储\",\"v\":\"128GB\"}]',599900,659900,389935,'https://cdn.example.com/sku/200303.jpg',170,443,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200304,2003,'AP-2003-4','Apple iPhone 16 深青色 256GB','[{\"k\":\"颜色\",\"v\":\"深青色\"},{\"k\":\"存储\",\"v\":\"256GB\"}]',699900,759900,454935,'https://cdn.example.com/sku/200304.jpg',170,1633,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200305,2003,'AP-2003-5','Apple iPhone 16 粉色 128GB','[{\"k\":\"颜色\",\"v\":\"粉色\"},{\"k\":\"存储\",\"v\":\"128GB\"}]',599900,659900,389935,'https://cdn.example.com/sku/200305.jpg',170,828,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200401,2004,'HW-2004-1','华为 Mate 70 Pro 曜石黑 256GB','[{\"k\":\"颜色\",\"v\":\"曜石黑\"},{\"k\":\"存储\",\"v\":\"256GB\"}]',699900,769900,419940,'https://cdn.example.com/sku/200401.jpg',210,777,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200402,2004,'HW-2004-2','华为 Mate 70 Pro 曜石黑 512GB','[{\"k\":\"颜色\",\"v\":\"曜石黑\"},{\"k\":\"存储\",\"v\":\"512GB\"}]',799900,869900,479940,'https://cdn.example.com/sku/200402.jpg',210,196,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200403,2004,'HW-2004-3','华为 Mate 70 Pro 雪域白 256GB','[{\"k\":\"颜色\",\"v\":\"雪域白\"},{\"k\":\"存储\",\"v\":\"256GB\"}]',699900,769900,419940,'https://cdn.example.com/sku/200403.jpg',210,1543,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200404,2004,'HW-2004-4','华为 Mate 70 Pro 雪域白 1TB','[{\"k\":\"颜色\",\"v\":\"雪域白\"},{\"k\":\"存储\",\"v\":\"1TB\"}]',949900,1019900,569940,'https://cdn.example.com/sku/200404.jpg',210,602,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200405,2004,'HW-2004-5','华为 Mate 70 Pro 罗兰紫 512GB','[{\"k\":\"颜色\",\"v\":\"罗兰紫\"},{\"k\":\"存储\",\"v\":\"512GB\"}]',799900,869900,479940,'https://cdn.example.com/sku/200405.jpg',210,1452,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200501,2005,'HW-2005-1','华为 Pura 80 Pro 罗兰紫 256GB','[{\"k\":\"颜色\",\"v\":\"罗兰紫\"},{\"k\":\"存储\",\"v\":\"256GB\"}]',599900,659900,359940,'https://cdn.example.com/sku/200501.jpg',195,598,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200502,2005,'HW-2005-2','华为 Pura 80 Pro 罗兰紫 512GB','[{\"k\":\"颜色\",\"v\":\"罗兰紫\"},{\"k\":\"存储\",\"v\":\"512GB\"}]',719900,779900,431940,'https://cdn.example.com/sku/200502.jpg',195,485,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200503,2005,'HW-2005-3','华为 Pura 80 Pro 雪域白 256GB','[{\"k\":\"颜色\",\"v\":\"雪域白\"},{\"k\":\"存储\",\"v\":\"256GB\"}]',599900,659900,359940,'https://cdn.example.com/sku/200503.jpg',195,989,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200504,2005,'HW-2005-4','华为 Pura 80 Pro 雪域白 1TB','[{\"k\":\"颜色\",\"v\":\"雪域白\"},{\"k\":\"存储\",\"v\":\"1TB\"}]',879900,939900,527940,'https://cdn.example.com/sku/200504.jpg',195,1575,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200505,2005,'HW-2005-5','华为 Pura 80 Pro 深海蓝 512GB','[{\"k\":\"颜色\",\"v\":\"深海蓝\"},{\"k\":\"存储\",\"v\":\"512GB\"}]',719900,779900,431940,'https://cdn.example.com/sku/200505.jpg',195,927,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200601,2006,'HW-2006-1','华为 Mate X6 曜石黑 256GB','[{\"k\":\"颜色\",\"v\":\"曜石黑\"},{\"k\":\"存储\",\"v\":\"256GB\"}]',1399900,1549900,769945,'https://cdn.example.com/sku/200601.jpg',243,1060,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200602,2006,'HW-2006-2','华为 Mate X6 曜石黑 512GB','[{\"k\":\"颜色\",\"v\":\"曜石黑\"},{\"k\":\"存储\",\"v\":\"512GB\"}]',1499900,1649900,824945,'https://cdn.example.com/sku/200602.jpg',243,363,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200603,2006,'HW-2006-3','华为 Mate X6 雪域白 512GB','[{\"k\":\"颜色\",\"v\":\"雪域白\"},{\"k\":\"存储\",\"v\":\"512GB\"}]',1499900,1649900,824945,'https://cdn.example.com/sku/200603.jpg',243,1271,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200604,2006,'HW-2006-4','华为 Mate X6 寰宇红 1TB','[{\"k\":\"颜色\",\"v\":\"寰宇红\"},{\"k\":\"存储\",\"v\":\"1TB\"}]',1599900,1749900,879945,'https://cdn.example.com/sku/200604.jpg',243,1008,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200701,2007,'AP-2007-1','Apple iPhone 15 Pro Max 原色钛金属 256GB','[{\"k\":\"颜色\",\"v\":\"原色钛金属\"},{\"k\":\"存储\",\"v\":\"256GB\"}]',799900,899900,479940,'https://cdn.example.com/sku/200701.jpg',221,1862,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200702,2007,'AP-2007-2','Apple iPhone 15 Pro Max 原色钛金属 512GB','[{\"k\":\"颜色\",\"v\":\"原色钛金属\"},{\"k\":\"存储\",\"v\":\"512GB\"}]',949900,1049900,569940,'https://cdn.example.com/sku/200702.jpg',221,746,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200703,2007,'AP-2007-3','Apple iPhone 15 Pro Max 蓝色钛金属 256GB','[{\"k\":\"颜色\",\"v\":\"蓝色钛金属\"},{\"k\":\"存储\",\"v\":\"256GB\"}]',799900,899900,479940,'https://cdn.example.com/sku/200703.jpg',221,979,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200704,2007,'AP-2007-4','Apple iPhone 15 Pro Max 蓝色钛金属 512GB','[{\"k\":\"颜色\",\"v\":\"蓝色钛金属\"},{\"k\":\"存储\",\"v\":\"512GB\"}]',949900,1049900,569940,'https://cdn.example.com/sku/200704.jpg',221,1075,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200705,2007,'AP-2007-5','Apple iPhone 15 Pro Max 白色钛金属 256GB','[{\"k\":\"颜色\",\"v\":\"白色钛金属\"},{\"k\":\"存储\",\"v\":\"256GB\"}]',799900,899900,479940,'https://cdn.example.com/sku/200705.jpg',221,1832,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200801,2008,'AP-2008-1','Apple MacBook Pro 16 深空黑 M4 Max/36GB/512GB','[{\"k\":\"颜色\",\"v\":\"深空黑\"},{\"k\":\"配置\",\"v\":\"M4 Max/36GB/512GB\"}]',1999900,2199900,1199940,'https://cdn.example.com/sku/200801.jpg',2140,815,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200802,2008,'AP-2008-2','Apple MacBook Pro 16 深空黑 M4 Max/48GB/1TB','[{\"k\":\"颜色\",\"v\":\"深空黑\"},{\"k\":\"配置\",\"v\":\"M4 Max/48GB/1TB\"}]',2299900,2499900,1379940,'https://cdn.example.com/sku/200802.jpg',2140,1931,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200803,2008,'AP-2008-3','Apple MacBook Pro 16 银色 M4 Max/36GB/512GB','[{\"k\":\"颜色\",\"v\":\"银色\"},{\"k\":\"配置\",\"v\":\"M4 Max/36GB/512GB\"}]',1999900,2199900,1199940,'https://cdn.example.com/sku/200803.jpg',2140,89,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200804,2008,'AP-2008-4','Apple MacBook Pro 16 银色 M4 Max/48GB/1TB','[{\"k\":\"颜色\",\"v\":\"银色\"},{\"k\":\"配置\",\"v\":\"M4 Max/48GB/1TB\"}]',2299900,2499900,1379940,'https://cdn.example.com/sku/200804.jpg',2140,168,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200901,2009,'AP-2009-1','Apple MacBook Air 15 午夜黑 M4/18GB/256GB','[{\"k\":\"颜色\",\"v\":\"午夜黑\"},{\"k\":\"配置\",\"v\":\"M4/18GB/256GB\"}]',1099900,1199900,659940,'https://cdn.example.com/sku/200901.jpg',1510,191,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200902,2009,'AP-2009-2','Apple MacBook Air 15 午夜黑 M4/24GB/512GB','[{\"k\":\"颜色\",\"v\":\"午夜黑\"},{\"k\":\"配置\",\"v\":\"M4/24GB/512GB\"}]',1299900,1399900,779940,'https://cdn.example.com/sku/200902.jpg',1510,1989,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200903,2009,'AP-2009-3','Apple MacBook Air 15 星光色 M4/18GB/256GB','[{\"k\":\"颜色\",\"v\":\"星光色\"},{\"k\":\"配置\",\"v\":\"M4/18GB/256GB\"}]',1099900,1199900,659940,'https://cdn.example.com/sku/200903.jpg',1510,1836,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200904,2009,'AP-2009-4','Apple MacBook Air 15 星光色 M4/24GB/512GB','[{\"k\":\"颜色\",\"v\":\"星光色\"},{\"k\":\"配置\",\"v\":\"M4/24GB/512GB\"}]',1299900,1399900,779940,'https://cdn.example.com/sku/200904.jpg',1510,1596,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(200905,2009,'AP-2009-5','Apple MacBook Air 15 银色 M4/18GB/256GB','[{\"k\":\"颜色\",\"v\":\"银色\"},{\"k\":\"配置\",\"v\":\"M4/18GB/256GB\"}]',1099900,1199900,659940,'https://cdn.example.com/sku/200905.jpg',1510,867,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201001,2010,'HW-2010-1','华为 MateBook X Pro 皓月银 Ultra 9/32GB/1TB','[{\"k\":\"颜色\",\"v\":\"皓月银\"},{\"k\":\"配置\",\"v\":\"Ultra 9/32GB/1TB\"}]',1299900,1429900,714945,'https://cdn.example.com/sku/201001.jpg',1290,297,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201002,2010,'HW-2010-2','华为 MateBook X Pro 皓月银 Ultra 9/32GB/2TB','[{\"k\":\"颜色\",\"v\":\"皓月银\"},{\"k\":\"配置\",\"v\":\"Ultra 9/32GB/2TB\"}]',1499900,1629900,824945,'https://cdn.example.com/sku/201002.jpg',1290,93,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201003,2010,'HW-2010-3','华为 MateBook X Pro 深空灰 Ultra 9/32GB/1TB','[{\"k\":\"颜色\",\"v\":\"深空灰\"},{\"k\":\"配置\",\"v\":\"Ultra 9/32GB/1TB\"}]',1299900,1429900,714945,'https://cdn.example.com/sku/201003.jpg',1290,501,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201004,2010,'HW-2010-4','华为 MateBook X Pro 深空灰 Ultra 9/32GB/2TB','[{\"k\":\"颜色\",\"v\":\"深空灰\"},{\"k\":\"配置\",\"v\":\"Ultra 9/32GB/2TB\"}]',1499900,1629900,824945,'https://cdn.example.com/sku/201004.jpg',1290,170,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201101,2011,'HW-2011-1','华为 MateBook 14 皓月银 Ultra 5/16GB/512GB','[{\"k\":\"颜色\",\"v\":\"皓月银\"},{\"k\":\"配置\",\"v\":\"Ultra 5/16GB/512GB\"}]',699900,769900,384945,'https://cdn.example.com/sku/201101.jpg',1380,537,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201102,2011,'HW-2011-2','华为 MateBook 14 皓月银 Ultra 5/16GB/1TB','[{\"k\":\"颜色\",\"v\":\"皓月银\"},{\"k\":\"配置\",\"v\":\"Ultra 5/16GB/1TB\"}]',799900,869900,439945,'https://cdn.example.com/sku/201102.jpg',1380,1154,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201103,2011,'HW-2011-3','华为 MateBook 14 深空灰 Ultra 5/16GB/512GB','[{\"k\":\"颜色\",\"v\":\"深空灰\"},{\"k\":\"配置\",\"v\":\"Ultra 5/16GB/512GB\"}]',699900,769900,384945,'https://cdn.example.com/sku/201103.jpg',1380,1703,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201104,2011,'HW-2011-4','华为 MateBook 14 深空灰 Ultra 5/16GB/1TB','[{\"k\":\"颜色\",\"v\":\"深空灰\"},{\"k\":\"配置\",\"v\":\"Ultra 5/16GB/1TB\"}]',799900,869900,439945,'https://cdn.example.com/sku/201104.jpg',1380,1399,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201105,2011,'HW-2011-5','华为 MateBook 14 樱语粉 Ultra 5/16GB/512GB','[{\"k\":\"颜色\",\"v\":\"樱语粉\"},{\"k\":\"配置\",\"v\":\"Ultra 5/16GB/512GB\"}]',699900,769900,384945,'https://cdn.example.com/sku/201105.jpg',1380,1006,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201201,2012,'AP-2012-1','Apple MacBook Pro 14 深空黑 M4 Pro/24GB/512GB','[{\"k\":\"颜色\",\"v\":\"深空黑\"},{\"k\":\"配置\",\"v\":\"M4 Pro/24GB/512GB\"}]',1499900,1649900,899940,'https://cdn.example.com/sku/201201.jpg',1610,874,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201202,2012,'AP-2012-2','Apple MacBook Pro 14 深空黑 M4 Pro/24GB/1TB','[{\"k\":\"颜色\",\"v\":\"深空黑\"},{\"k\":\"配置\",\"v\":\"M4 Pro/24GB/1TB\"}]',1699900,1849900,1019940,'https://cdn.example.com/sku/201202.jpg',1610,442,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201203,2012,'AP-2012-3','Apple MacBook Pro 14 银色 M4 Pro/24GB/512GB','[{\"k\":\"颜色\",\"v\":\"银色\"},{\"k\":\"配置\",\"v\":\"M4 Pro/24GB/512GB\"}]',1499900,1649900,899940,'https://cdn.example.com/sku/201203.jpg',1610,914,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201204,2012,'AP-2012-4','Apple MacBook Pro 14 银色 M4 Pro/24GB/1TB','[{\"k\":\"颜色\",\"v\":\"银色\"},{\"k\":\"配置\",\"v\":\"M4 Pro/24GB/1TB\"}]',1699900,1849900,1019940,'https://cdn.example.com/sku/201204.jpg',1610,1840,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201301,2013,'HW-2013-1','华为 MateBook 16s 深空灰 Ultra 7/32GB/1TB','[{\"k\":\"颜色\",\"v\":\"深空灰\"},{\"k\":\"配置\",\"v\":\"Ultra 7/32GB/1TB\"}]',899900,989900,494945,'https://cdn.example.com/sku/201301.jpg',1990,1157,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201302,2013,'HW-2013-2','华为 MateBook 16s 深空灰 Ultra 7/32GB/2TB','[{\"k\":\"颜色\",\"v\":\"深空灰\"},{\"k\":\"配置\",\"v\":\"Ultra 7/32GB/2TB\"}]',1049900,1139900,577445,'https://cdn.example.com/sku/201302.jpg',1990,390,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201303,2013,'HW-2013-3','华为 MateBook 16s 皓月银 Ultra 7/32GB/1TB','[{\"k\":\"颜色\",\"v\":\"皓月银\"},{\"k\":\"配置\",\"v\":\"Ultra 7/32GB/1TB\"}]',899900,989900,494945,'https://cdn.example.com/sku/201303.jpg',1990,1820,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201304,2013,'HW-2013-4','华为 MateBook 16s 皓月银 Ultra 7/32GB/2TB','[{\"k\":\"颜色\",\"v\":\"皓月银\"},{\"k\":\"配置\",\"v\":\"Ultra 7/32GB/2TB\"}]',1049900,1139900,577445,'https://cdn.example.com/sku/201304.jpg',1990,54,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201401,2014,'HW-2014-1','华为 1.5匹 新一级变频冷暖空调 白色 标准版','[{\"k\":\"颜色\",\"v\":\"白色\"},{\"k\":\"功能\",\"v\":\"标准版\"}]',299900,329900,164945,'https://cdn.example.com/sku/201401.jpg',10000,1476,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201402,2014,'HW-2014-2','华为 1.5匹 新一级变频冷暖空调 白色 Pro版(除甲醛)','[{\"k\":\"颜色\",\"v\":\"白色\"},{\"k\":\"功能\",\"v\":\"Pro版(除甲醛)\"}]',329900,359900,181445,'https://cdn.example.com/sku/201402.jpg',10000,367,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201403,2014,'HW-2014-3','华为 1.5匹 新一级变频冷暖空调 白色 旗舰版(新风)','[{\"k\":\"颜色\",\"v\":\"白色\"},{\"k\":\"功能\",\"v\":\"旗舰版(新风)\"}]',349900,379900,192445,'https://cdn.example.com/sku/201403.jpg',10000,1236,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201404,2014,'HW-2014-4','华为 1.5匹 新一级变频冷暖空调 米色 标准版','[{\"k\":\"颜色\",\"v\":\"米色\"},{\"k\":\"功能\",\"v\":\"标准版\"}]',299900,329900,164945,'https://cdn.example.com/sku/201404.jpg',10000,167,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201501,2015,'HW-2015-1','华为 3匹 新一级变频冷暖立式空调 白色 标准版','[{\"k\":\"颜色\",\"v\":\"白色\"},{\"k\":\"功能\",\"v\":\"标准版\"}]',599900,659900,329945,'https://cdn.example.com/sku/201501.jpg',28000,166,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201502,2015,'HW-2015-2','华为 3匹 新一级变频冷暖立式空调 白色 Pro版(除甲醛)','[{\"k\":\"颜色\",\"v\":\"白色\"},{\"k\":\"功能\",\"v\":\"Pro版(除甲醛)\"}]',639900,699900,351945,'https://cdn.example.com/sku/201502.jpg',28000,1268,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201503,2015,'HW-2015-3','华为 3匹 新一级变频冷暖立式空调 白色 旗舰版(新风)','[{\"k\":\"颜色\",\"v\":\"白色\"},{\"k\":\"功能\",\"v\":\"旗舰版(新风)\"}]',669900,729900,368445,'https://cdn.example.com/sku/201503.jpg',28000,295,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201504,2015,'HW-2015-4','华为 3匹 新一级变频冷暖立式空调 灰色 标准版','[{\"k\":\"颜色\",\"v\":\"灰色\"},{\"k\":\"功能\",\"v\":\"标准版\"}]',599900,659900,329945,'https://cdn.example.com/sku/201504.jpg',28000,131,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201505,2015,'HW-2015-5','华为 3匹 新一级变频冷暖立式空调 灰色 旗舰版(新风)','[{\"k\":\"颜色\",\"v\":\"灰色\"},{\"k\":\"功能\",\"v\":\"旗舰版(新风)\"}]',669900,729900,368445,'https://cdn.example.com/sku/201505.jpg',28000,1207,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201601,2016,'HW-2016-1','华为 大1匹 变频冷暖空调 白色 标准版','[{\"k\":\"颜色\",\"v\":\"白色\"},{\"k\":\"功能\",\"v\":\"标准版\"}]',259900,285900,142945,'https://cdn.example.com/sku/201601.jpg',9000,593,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201602,2016,'HW-2016-2','华为 大1匹 变频冷暖空调 白色 静音版','[{\"k\":\"颜色\",\"v\":\"白色\"},{\"k\":\"功能\",\"v\":\"静音版\"}]',274900,300900,151195,'https://cdn.example.com/sku/201602.jpg',9000,664,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201603,2016,'HW-2016-3','华为 大1匹 变频冷暖空调 白色 除湿版','[{\"k\":\"颜色\",\"v\":\"白色\"},{\"k\":\"功能\",\"v\":\"除湿版\"}]',269900,295900,148445,'https://cdn.example.com/sku/201603.jpg',9000,69,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201604,2016,'HW-2016-4','华为 大1匹 变频冷暖空调 米色 标准版','[{\"k\":\"颜色\",\"v\":\"米色\"},{\"k\":\"功能\",\"v\":\"标准版\"}]',259900,285900,142945,'https://cdn.example.com/sku/201604.jpg',9000,200,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201701,2017,'HW-2017-1','华为 2匹 变频冷暖挂式空调 白色 标准版','[{\"k\":\"颜色\",\"v\":\"白色\"},{\"k\":\"功能\",\"v\":\"标准版\"}]',449900,494900,247445,'https://cdn.example.com/sku/201701.jpg',15000,764,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201702,2017,'HW-2017-2','华为 2匹 变频冷暖挂式空调 白色 Pro版(除甲醛)','[{\"k\":\"颜色\",\"v\":\"白色\"},{\"k\":\"功能\",\"v\":\"Pro版(除甲醛)\"}]',484900,529900,266695,'https://cdn.example.com/sku/201702.jpg',15000,373,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201703,2017,'HW-2017-3','华为 2匹 变频冷暖挂式空调 白色 旗舰版(新风)','[{\"k\":\"颜色\",\"v\":\"白色\"},{\"k\":\"功能\",\"v\":\"旗舰版(新风)\"}]',504900,549900,277695,'https://cdn.example.com/sku/201703.jpg',15000,1302,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201704,2017,'HW-2017-4','华为 2匹 变频冷暖挂式空调 灰色 标准版','[{\"k\":\"颜色\",\"v\":\"灰色\"},{\"k\":\"功能\",\"v\":\"标准版\"}]',449900,494900,247445,'https://cdn.example.com/sku/201704.jpg',15000,663,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201801,2018,'HW-2018-1','华为 中央空调 一拖四 多联机 白色 标准版(4匹)','[{\"k\":\"颜色\",\"v\":\"白色\"},{\"k\":\"功能\",\"v\":\"标准版(4匹)\"}]',1299900,1429900,649950,'https://cdn.example.com/sku/201801.jpg',52000,269,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201802,2018,'HW-2018-2','华为 中央空调 一拖四 多联机 白色 Pro版(5匹)','[{\"k\":\"颜色\",\"v\":\"白色\"},{\"k\":\"功能\",\"v\":\"Pro版(5匹)\"}]',1399900,1529900,699950,'https://cdn.example.com/sku/201802.jpg',52000,1288,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201803,2018,'HW-2018-3','华为 中央空调 一拖四 多联机 白色 旗舰版(6匹+3D)','[{\"k\":\"颜色\",\"v\":\"白色\"},{\"k\":\"功能\",\"v\":\"旗舰版(6匹+3D)\"}]',1499900,1629900,749950,'https://cdn.example.com/sku/201803.jpg',52000,1457,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201804,2018,'HW-2018-4','华为 中央空调 一拖四 多联机 灰色 标准版(4匹)','[{\"k\":\"颜色\",\"v\":\"灰色\"},{\"k\":\"功能\",\"v\":\"标准版(4匹)\"}]',1299900,1429900,649950,'https://cdn.example.com/sku/201804.jpg',52000,1050,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201901,2019,'NK-2019-1','Nike Dri-FIT 女子速干运动T恤 黑色 S','[{\"k\":\"颜色\",\"v\":\"黑色\"},{\"k\":\"尺码\",\"v\":\"S\"}]',29900,32900,11960,'https://cdn.example.com/sku/201901.jpg',150,616,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201902,2019,'NK-2019-2','Nike Dri-FIT 女子速干运动T恤 黑色 M','[{\"k\":\"颜色\",\"v\":\"黑色\"},{\"k\":\"尺码\",\"v\":\"M\"}]',29900,32900,11960,'https://cdn.example.com/sku/201902.jpg',150,1354,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201903,2019,'NK-2019-3','Nike Dri-FIT 女子速干运动T恤 黑色 L','[{\"k\":\"颜色\",\"v\":\"黑色\"},{\"k\":\"尺码\",\"v\":\"L\"}]',29900,32900,11960,'https://cdn.example.com/sku/201903.jpg',150,1179,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201904,2019,'NK-2019-4','Nike Dri-FIT 女子速干运动T恤 黑色 XL','[{\"k\":\"颜色\",\"v\":\"黑色\"},{\"k\":\"尺码\",\"v\":\"XL\"}]',29900,32900,11960,'https://cdn.example.com/sku/201904.jpg',150,279,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201905,2019,'NK-2019-5','Nike Dri-FIT 女子速干运动T恤 白色 M','[{\"k\":\"颜色\",\"v\":\"白色\"},{\"k\":\"尺码\",\"v\":\"M\"}]',29900,32900,11960,'https://cdn.example.com/sku/201905.jpg',150,123,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(201906,2019,'NK-2019-6','Nike Dri-FIT 女子速干运动T恤 白色 L','[{\"k\":\"颜色\",\"v\":\"白色\"},{\"k\":\"尺码\",\"v\":\"L\"}]',29900,32900,11960,'https://cdn.example.com/sku/201906.jpg',150,353,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202001,2020,'NK-2020-1','Nike 男子套头卫衣 灰色 M','[{\"k\":\"颜色\",\"v\":\"灰色\"},{\"k\":\"尺码\",\"v\":\"M\"}]',49900,54900,19960,'https://cdn.example.com/sku/202001.jpg',350,1960,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202002,2020,'NK-2020-2','Nike 男子套头卫衣 灰色 L','[{\"k\":\"颜色\",\"v\":\"灰色\"},{\"k\":\"尺码\",\"v\":\"L\"}]',49900,54900,19960,'https://cdn.example.com/sku/202002.jpg',350,1446,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202003,2020,'NK-2020-3','Nike 男子套头卫衣 灰色 XL','[{\"k\":\"颜色\",\"v\":\"灰色\"},{\"k\":\"尺码\",\"v\":\"XL\"}]',49900,54900,19960,'https://cdn.example.com/sku/202003.jpg',350,1647,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202004,2020,'NK-2020-4','Nike 男子套头卫衣 黑色 M','[{\"k\":\"颜色\",\"v\":\"黑色\"},{\"k\":\"尺码\",\"v\":\"M\"}]',49900,54900,19960,'https://cdn.example.com/sku/202004.jpg',350,1945,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202005,2020,'NK-2020-5','Nike 男子套头卫衣 黑色 L','[{\"k\":\"颜色\",\"v\":\"黑色\"},{\"k\":\"尺码\",\"v\":\"L\"}]',49900,54900,19960,'https://cdn.example.com/sku/202005.jpg',350,100,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202006,2020,'NK-2020-6','Nike 男子套头卫衣 黑色 XL','[{\"k\":\"颜色\",\"v\":\"黑色\"},{\"k\":\"尺码\",\"v\":\"XL\"}]',49900,54900,19960,'https://cdn.example.com/sku/202006.jpg',350,1692,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202101,2021,'NK-2021-1','Nike 女子瑜伽运动上衣 紫色 S','[{\"k\":\"颜色\",\"v\":\"紫色\"},{\"k\":\"尺码\",\"v\":\"S\"}]',39900,43900,13965,'https://cdn.example.com/sku/202101.jpg',120,1662,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202102,2021,'NK-2021-2','Nike 女子瑜伽运动上衣 紫色 M','[{\"k\":\"颜色\",\"v\":\"紫色\"},{\"k\":\"尺码\",\"v\":\"M\"}]',39900,43900,13965,'https://cdn.example.com/sku/202102.jpg',120,1808,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202103,2021,'NK-2021-3','Nike 女子瑜伽运动上衣 紫色 L','[{\"k\":\"颜色\",\"v\":\"紫色\"},{\"k\":\"尺码\",\"v\":\"L\"}]',39900,43900,13965,'https://cdn.example.com/sku/202103.jpg',120,766,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202104,2021,'NK-2021-4','Nike 女子瑜伽运动上衣 粉色 S','[{\"k\":\"颜色\",\"v\":\"粉色\"},{\"k\":\"尺码\",\"v\":\"S\"}]',39900,43900,13965,'https://cdn.example.com/sku/202104.jpg',120,506,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202105,2021,'NK-2021-5','Nike 女子瑜伽运动上衣 粉色 M','[{\"k\":\"颜色\",\"v\":\"粉色\"},{\"k\":\"尺码\",\"v\":\"M\"}]',39900,43900,13965,'https://cdn.example.com/sku/202105.jpg',120,620,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202201,2022,'NK-2022-1','Nike 男子连帽夹克 黑色 M','[{\"k\":\"颜色\",\"v\":\"黑色\"},{\"k\":\"尺码\",\"v\":\"M\"}]',89900,98900,35960,'https://cdn.example.com/sku/202201.jpg',450,1093,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202202,2022,'NK-2022-2','Nike 男子连帽夹克 黑色 L','[{\"k\":\"颜色\",\"v\":\"黑色\"},{\"k\":\"尺码\",\"v\":\"L\"}]',89900,98900,35960,'https://cdn.example.com/sku/202202.jpg',450,286,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202203,2022,'NK-2022-3','Nike 男子连帽夹克 黑色 XL','[{\"k\":\"颜色\",\"v\":\"黑色\"},{\"k\":\"尺码\",\"v\":\"XL\"}]',89900,98900,35960,'https://cdn.example.com/sku/202203.jpg',450,593,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202204,2022,'NK-2022-4','Nike 男子连帽夹克 黑色 XXL','[{\"k\":\"颜色\",\"v\":\"黑色\"},{\"k\":\"尺码\",\"v\":\"XXL\"}]',89900,98900,35960,'https://cdn.example.com/sku/202204.jpg',450,757,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202205,2022,'NK-2022-5','Nike 男子连帽夹克 深蓝色 L','[{\"k\":\"颜色\",\"v\":\"深蓝色\"},{\"k\":\"尺码\",\"v\":\"L\"}]',89900,98900,35960,'https://cdn.example.com/sku/202205.jpg',450,1291,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202206,2022,'NK-2022-6','Nike 男子连帽夹克 深蓝色 XL','[{\"k\":\"颜色\",\"v\":\"深蓝色\"},{\"k\":\"尺码\",\"v\":\"XL\"}]',89900,98900,35960,'https://cdn.example.com/sku/202206.jpg',450,439,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202301,2023,'NK-2023-1','Nike 女子运动背心 黑色 S','[{\"k\":\"颜色\",\"v\":\"黑色\"},{\"k\":\"尺码\",\"v\":\"S\"}]',24900,27400,8715,'https://cdn.example.com/sku/202301.jpg',80,1945,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202302,2023,'NK-2023-2','Nike 女子运动背心 黑色 M','[{\"k\":\"颜色\",\"v\":\"黑色\"},{\"k\":\"尺码\",\"v\":\"M\"}]',24900,27400,8715,'https://cdn.example.com/sku/202302.jpg',80,795,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202303,2023,'NK-2023-3','Nike 女子运动背心 黑色 L','[{\"k\":\"颜色\",\"v\":\"黑色\"},{\"k\":\"尺码\",\"v\":\"L\"}]',24900,27400,8715,'https://cdn.example.com/sku/202303.jpg',80,1326,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202304,2023,'NK-2023-4','Nike 女子运动背心 白色 S','[{\"k\":\"颜色\",\"v\":\"白色\"},{\"k\":\"尺码\",\"v\":\"S\"}]',24900,27400,8715,'https://cdn.example.com/sku/202304.jpg',80,1088,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202305,2023,'NK-2023-5','Nike 女子运动背心 白色 M','[{\"k\":\"颜色\",\"v\":\"白色\"},{\"k\":\"尺码\",\"v\":\"M\"}]',24900,27400,8715,'https://cdn.example.com/sku/202305.jpg',80,874,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202401,2024,'NK-2024-1','Nike 男子休闲Polo衫 白色 M','[{\"k\":\"颜色\",\"v\":\"白色\"},{\"k\":\"尺码\",\"v\":\"M\"}]',59900,65900,23960,'https://cdn.example.com/sku/202401.jpg',200,1411,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202402,2024,'NK-2024-2','Nike 男子休闲Polo衫 白色 L','[{\"k\":\"颜色\",\"v\":\"白色\"},{\"k\":\"尺码\",\"v\":\"L\"}]',59900,65900,23960,'https://cdn.example.com/sku/202402.jpg',200,666,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202403,2024,'NK-2024-3','Nike 男子休闲Polo衫 白色 XL','[{\"k\":\"颜色\",\"v\":\"白色\"},{\"k\":\"尺码\",\"v\":\"XL\"}]',59900,65900,23960,'https://cdn.example.com/sku/202403.jpg',200,637,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202404,2024,'NK-2024-4','Nike 男子休闲Polo衫 黑色 M','[{\"k\":\"颜色\",\"v\":\"黑色\"},{\"k\":\"尺码\",\"v\":\"M\"}]',59900,65900,23960,'https://cdn.example.com/sku/202404.jpg',200,1292,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202405,2024,'NK-2024-5','Nike 男子休闲Polo衫 黑色 L','[{\"k\":\"颜色\",\"v\":\"黑色\"},{\"k\":\"尺码\",\"v\":\"L\"}]',59900,65900,23960,'https://cdn.example.com/sku/202405.jpg',200,955,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202406,2024,'NK-2024-6','Nike 男子休闲Polo衫 黑色 XL','[{\"k\":\"颜色\",\"v\":\"黑色\"},{\"k\":\"尺码\",\"v\":\"XL\"}]',59900,65900,23960,'https://cdn.example.com/sku/202406.jpg',200,1675,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202501,2025,'GN-2025-1',' 玻尿酸补水保湿面膜20片装 20片 盒装','[{\"k\":\"规格\",\"v\":\"20片\"},{\"k\":\"包装\",\"v\":\"盒装\"}]',12900,14200,3870,'https://cdn.example.com/sku/202501.jpg',280,1409,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202502,2025,'GN-2025-2',' 玻尿酸补水保湿面膜20片装 20片 礼盒装(含赠品)','[{\"k\":\"规格\",\"v\":\"20片\"},{\"k\":\"包装\",\"v\":\"礼盒装(含赠品)\"}]',15900,17200,4770,'https://cdn.example.com/sku/202502.jpg',280,1726,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202503,2025,'GN-2025-3',' 玻尿酸补水保湿面膜20片装 10片 盒装','[{\"k\":\"规格\",\"v\":\"10片\"},{\"k\":\"包装\",\"v\":\"盒装\"}]',12900,14200,3870,'https://cdn.example.com/sku/202503.jpg',280,510,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202504,2025,'GN-2025-4',' 玻尿酸补水保湿面膜20片装 10片 试用装','[{\"k\":\"规格\",\"v\":\"10片\"},{\"k\":\"包装\",\"v\":\"试用装\"}]',10900,12200,3270,'https://cdn.example.com/sku/202504.jpg',280,144,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202601,2026,'GN-2026-1',' 烟酰胺美白淡斑面膜10片装 10片 盒装','[{\"k\":\"规格\",\"v\":\"10片\"},{\"k\":\"包装\",\"v\":\"盒装\"}]',9900,10900,2970,'https://cdn.example.com/sku/202601.jpg',150,1864,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202602,2026,'GN-2026-2',' 烟酰胺美白淡斑面膜10片装 10片 礼盒装(含赠品)','[{\"k\":\"规格\",\"v\":\"10片\"},{\"k\":\"包装\",\"v\":\"礼盒装(含赠品)\"}]',12900,13900,3870,'https://cdn.example.com/sku/202602.jpg',150,1062,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202603,2026,'GN-2026-3',' 烟酰胺美白淡斑面膜10片装 5片 盒装','[{\"k\":\"规格\",\"v\":\"5片\"},{\"k\":\"包装\",\"v\":\"盒装\"}]',9900,10900,2970,'https://cdn.example.com/sku/202603.jpg',150,1458,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202604,2026,'GN-2026-4',' 烟酰胺美白淡斑面膜10片装 5片 试用装','[{\"k\":\"规格\",\"v\":\"5片\"},{\"k\":\"包装\",\"v\":\"试用装\"}]',8400,9400,2520,'https://cdn.example.com/sku/202604.jpg',150,1644,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202701,2027,'GN-2027-1',' 积雪草舒缓修护面膜10片装 10片 盒装','[{\"k\":\"规格\",\"v\":\"10片\"},{\"k\":\"包装\",\"v\":\"盒装\"}]',11900,13100,3570,'https://cdn.example.com/sku/202701.jpg',160,1191,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202702,2027,'GN-2027-2',' 积雪草舒缓修护面膜10片装 10片 礼盒装(含赠品)','[{\"k\":\"规格\",\"v\":\"10片\"},{\"k\":\"包装\",\"v\":\"礼盒装(含赠品)\"}]',14900,16100,4470,'https://cdn.example.com/sku/202702.jpg',160,1691,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202703,2027,'GN-2027-3',' 积雪草舒缓修护面膜10片装 5片 盒装','[{\"k\":\"规格\",\"v\":\"5片\"},{\"k\":\"包装\",\"v\":\"盒装\"}]',11900,13100,3570,'https://cdn.example.com/sku/202703.jpg',160,1269,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202704,2027,'GN-2027-4',' 积雪草舒缓修护面膜10片装 5片 试用装','[{\"k\":\"规格\",\"v\":\"5片\"},{\"k\":\"包装\",\"v\":\"试用装\"}]',10400,11600,3120,'https://cdn.example.com/sku/202704.jpg',160,1304,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202705,2027,'GN-2027-5',' 积雪草舒缓修护面膜10片装 20片 盒装','[{\"k\":\"规格\",\"v\":\"20片\"},{\"k\":\"包装\",\"v\":\"盒装\"}]',16900,18100,5070,'https://cdn.example.com/sku/202705.jpg',160,963,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202801,2028,'GN-2028-1',' 水杨酸控油祛痘面膜10片装 10片 盒装','[{\"k\":\"规格\",\"v\":\"10片\"},{\"k\":\"包装\",\"v\":\"盒装\"}]',10900,12000,3270,'https://cdn.example.com/sku/202801.jpg',155,1355,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202802,2028,'GN-2028-2',' 水杨酸控油祛痘面膜10片装 10片 礼盒装(含赠品)','[{\"k\":\"规格\",\"v\":\"10片\"},{\"k\":\"包装\",\"v\":\"礼盒装(含赠品)\"}]',13900,15000,4170,'https://cdn.example.com/sku/202802.jpg',155,1042,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202803,2028,'GN-2028-3',' 水杨酸控油祛痘面膜10片装 5片 盒装','[{\"k\":\"规格\",\"v\":\"5片\"},{\"k\":\"包装\",\"v\":\"盒装\"}]',10900,12000,3270,'https://cdn.example.com/sku/202803.jpg',155,208,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202804,2028,'GN-2028-4',' 水杨酸控油祛痘面膜10片装 5片 试用装','[{\"k\":\"规格\",\"v\":\"5片\"},{\"k\":\"包装\",\"v\":\"试用装\"}]',9400,10500,2820,'https://cdn.example.com/sku/202804.jpg',155,737,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202901,2029,'GN-2029-1',' 胶原蛋白紧致弹润面膜10片装 10片 盒装','[{\"k\":\"规格\",\"v\":\"10片\"},{\"k\":\"包装\",\"v\":\"盒装\"}]',16900,18600,5070,'https://cdn.example.com/sku/202901.jpg',180,1471,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202902,2029,'GN-2029-2',' 胶原蛋白紧致弹润面膜10片装 10片 礼盒装(含赠品)','[{\"k\":\"规格\",\"v\":\"10片\"},{\"k\":\"包装\",\"v\":\"礼盒装(含赠品)\"}]',19900,21600,5970,'https://cdn.example.com/sku/202902.jpg',180,884,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202903,2029,'GN-2029-3',' 胶原蛋白紧致弹润面膜10片装 5片 盒装','[{\"k\":\"规格\",\"v\":\"5片\"},{\"k\":\"包装\",\"v\":\"盒装\"}]',16900,18600,5070,'https://cdn.example.com/sku/202903.jpg',180,473,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(202904,2029,'GN-2029-4',' 胶原蛋白紧致弹润面膜10片装 20片 盒装','[{\"k\":\"规格\",\"v\":\"20片\"},{\"k\":\"包装\",\"v\":\"盒装\"}]',23900,25600,7170,'https://cdn.example.com/sku/202904.jpg',180,1987,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(203001,2030,'GN-2030-1',' 维C提亮抗氧面膜10片装 10片 盒装','[{\"k\":\"规格\",\"v\":\"10片\"},{\"k\":\"包装\",\"v\":\"盒装\"}]',13900,15300,4170,'https://cdn.example.com/sku/203001.jpg',170,62,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(203002,2030,'GN-2030-2',' 维C提亮抗氧面膜10片装 10片 礼盒装(含赠品)','[{\"k\":\"规格\",\"v\":\"10片\"},{\"k\":\"包装\",\"v\":\"礼盒装(含赠品)\"}]',16900,18300,5070,'https://cdn.example.com/sku/203002.jpg',170,1797,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(203003,2030,'GN-2030-3',' 维C提亮抗氧面膜10片装 5片 盒装','[{\"k\":\"规格\",\"v\":\"5片\"},{\"k\":\"包装\",\"v\":\"盒装\"}]',13900,15300,4170,'https://cdn.example.com/sku/203003.jpg',170,1688,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(203004,2030,'GN-2030-4',' 维C提亮抗氧面膜10片装 5片 试用装','[{\"k\":\"规格\",\"v\":\"5片\"},{\"k\":\"包装\",\"v\":\"试用装\"}]',12400,13800,3720,'https://cdn.example.com/sku/203004.jpg',170,608,0,'2026-06-01 08:25:42','2026-06-01 08:25:42'),(203005,2030,'GN-2030-5',' 维C提亮抗氧面膜10片装 20片 盒装','[{\"k\":\"规格\",\"v\":\"20片\"},{\"k\":\"包装\",\"v\":\"盒装\"}]',19900,21300,5970,'https://cdn.example.com/sku/203005.jpg',170,738,0,'2026-06-01 08:25:42','2026-06-01 08:25:42');
/*!40000 ALTER TABLE `mall_product_sku` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mall_product_sku_stock`
--

DROP TABLE IF EXISTS `mall_product_sku_stock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_product_sku_stock` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `sku_id` bigint unsigned NOT NULL COMMENT 'SKU ID，与 SKU 一对一',
  `total_stock` int unsigned DEFAULT '0' COMMENT '总库存 = 可用 + 锁定 + 已售 + 冻结',
  `available_stock` int unsigned DEFAULT '0' COMMENT '可用库存',
  `locked_stock` int unsigned DEFAULT '0' COMMENT '锁定库存',
  `sold_stock` int unsigned DEFAULT '0' COMMENT '已售库存',
  `frozen_stock` int unsigned DEFAULT '0' COMMENT '冻结库存',
  `is_deleted` tinyint unsigned DEFAULT '0' COMMENT '逻辑删除标志',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `version` int unsigned DEFAULT '0' COMMENT '乐观锁版本号，库存扣减防超卖',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_id` (`sku_id`)
) ENGINE=InnoDB AUTO_INCREMENT=145 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='SKU 库存表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mall_product_sku_stock`
--

LOCK TABLES `mall_product_sku_stock` WRITE;
/*!40000 ALTER TABLE `mall_product_sku_stock` DISABLE KEYS */;
INSERT INTO `mall_product_sku_stock` VALUES (1,100101,500,500,0,0,0,0,'2026-05-19 04:15:48','2026-05-19 04:15:48',0),(2,100102,300,300,0,0,0,0,'2026-05-19 04:15:48','2026-05-19 04:15:48',0),(3,200101,763,620,14,125,4,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(4,200102,409,172,47,173,17,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(5,200103,1409,975,2,432,0,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(6,200104,647,477,32,119,19,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(7,200105,1349,1081,45,203,20,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(8,200106,1316,859,14,429,14,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(9,200201,1980,1914,48,13,5,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(10,200202,1065,696,17,348,4,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(11,200203,1763,1066,6,689,2,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(12,200204,398,266,22,91,19,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(13,200205,1852,1704,46,88,14,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(14,200301,975,851,35,80,9,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(15,200302,1487,813,23,633,18,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(16,200303,1642,1562,2,71,7,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(17,200304,792,735,14,40,3,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(18,200305,769,486,40,232,11,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(19,200401,629,249,17,343,20,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(20,200402,1447,770,10,650,17,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(21,200403,701,577,29,83,12,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(22,200404,1510,764,35,704,7,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(23,200405,864,792,14,57,1,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(24,200501,335,235,36,54,10,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(25,200502,1542,986,25,511,20,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(26,200503,492,342,8,135,7,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(27,200504,1349,764,16,551,18,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(28,200505,1395,957,23,408,7,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(29,200601,386,187,3,193,3,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(30,200602,1485,1259,50,163,13,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(31,200603,330,189,24,98,19,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(32,200604,1283,991,35,257,0,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(33,200701,1299,494,17,768,20,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(34,200702,428,246,27,150,5,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(35,200703,206,30,46,122,8,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(36,200704,1760,1360,32,365,3,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(37,200705,1480,1119,40,305,16,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(38,200801,1761,1381,34,330,16,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(39,200802,201,90,20,76,15,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(40,200803,429,218,19,185,7,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(41,200804,693,396,5,290,2,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(42,200901,1757,1477,8,257,15,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(43,200902,1325,1124,16,169,16,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(44,200903,1442,979,13,433,17,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(45,200904,1694,967,12,706,9,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(46,200905,1575,873,23,665,14,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(47,201001,707,578,4,115,10,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(48,201002,1404,805,14,567,18,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(49,201003,214,131,45,18,20,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(50,201004,668,622,2,34,10,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(51,201101,770,391,31,342,6,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(52,201102,470,198,15,242,15,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(53,201103,1033,830,6,194,3,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(54,201104,1082,680,27,362,13,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(55,201105,1969,1796,43,110,20,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(56,201201,1691,1331,6,347,7,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(57,201202,589,283,28,274,4,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(58,201203,575,397,29,142,7,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(59,201204,354,203,35,113,3,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(60,201301,1912,1870,5,30,7,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(61,201302,1032,499,30,497,6,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(62,201303,1021,939,10,60,12,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(63,201304,999,664,50,271,14,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(64,201401,1696,844,35,802,15,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(65,201402,588,423,13,151,1,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(66,201403,1706,1525,47,124,10,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(67,201404,302,107,30,149,16,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(68,201501,1240,1145,11,82,2,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(69,201502,339,140,15,172,12,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(70,201503,1366,1058,37,252,19,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(71,201504,1468,1341,26,83,18,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(72,201505,1270,925,16,323,6,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(73,201601,1010,814,42,134,20,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(74,201602,1136,763,48,323,2,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(75,201603,1138,463,36,636,3,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(76,201604,1301,1043,32,218,8,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(77,201701,340,246,23,62,9,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(78,201702,1097,487,45,556,9,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(79,201703,1852,752,0,1083,17,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(80,201704,1558,1436,8,106,8,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(81,201801,1720,1376,17,318,9,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(82,201802,631,237,21,367,6,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(83,201803,1498,593,16,873,16,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(84,201804,714,663,5,26,20,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(85,201901,290,265,21,0,4,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(86,201902,736,593,47,82,14,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(87,201903,1645,1173,35,437,0,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(88,201904,354,152,9,176,17,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(89,201905,1909,1099,37,756,17,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(90,201906,1080,939,2,130,9,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(91,202001,1830,1721,22,81,6,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(92,202002,711,353,6,341,11,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(93,202003,1346,887,39,416,4,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(94,202004,684,577,11,83,13,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(95,202005,567,334,50,170,13,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(96,202006,1571,633,47,884,7,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(97,202101,1636,1501,24,110,1,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(98,202102,1163,910,12,227,14,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(99,202103,825,348,50,420,7,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(100,202104,248,164,25,49,10,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(101,202105,1970,1771,49,142,8,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(102,202201,1018,448,21,549,0,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(103,202202,1996,1433,11,534,18,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(104,202203,278,200,38,27,13,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(105,202204,1692,854,20,805,13,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(106,202205,1247,1087,24,118,18,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(107,202206,721,641,45,22,13,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(108,202301,1851,700,43,1102,6,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(109,202302,1083,960,42,71,10,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(110,202303,842,487,7,339,9,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(111,202304,833,456,26,341,10,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(112,202305,1627,1286,35,302,4,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(113,202401,976,741,39,178,18,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(114,202402,1031,461,0,561,9,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(115,202403,630,342,50,220,18,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(116,202404,1540,1168,29,329,14,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(117,202405,1583,1318,32,218,15,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(118,202406,1830,1439,42,347,2,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(119,202501,1496,839,21,634,2,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(120,202502,1738,1205,43,481,9,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(121,202503,1851,1435,9,407,0,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(122,202504,701,417,39,243,2,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(123,202601,1489,876,12,589,12,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(124,202602,1018,740,9,249,20,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(125,202603,211,46,48,114,3,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(126,202604,1070,819,11,224,16,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(127,202701,710,615,29,62,4,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(128,202702,1151,418,33,683,17,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(129,202703,849,301,48,486,14,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(130,202704,1869,792,27,1033,17,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(131,202705,525,246,28,243,8,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(132,202801,767,310,49,392,16,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(133,202802,1483,1208,17,244,14,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(134,202803,1661,1346,15,292,8,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(135,202804,854,760,8,82,4,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(136,202901,512,386,4,109,13,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(137,202902,877,373,26,477,1,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(138,202903,1905,1003,24,860,18,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(139,202904,1624,1538,48,20,18,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(140,203001,920,555,48,305,12,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(141,203002,1911,1002,34,858,17,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(142,203003,1435,1172,31,225,7,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(143,203004,1092,582,1,497,12,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0),(144,203005,1569,844,25,695,5,0,'2026-06-01 08:25:42','2026-06-01 08:25:42',0);
/*!40000 ALTER TABLE `mall_product_sku_stock` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mall_product_spu`
--

DROP TABLE IF EXISTS `mall_product_spu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_product_spu` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `category_id` bigint unsigned NOT NULL COMMENT '所属三级类目 ID',
  `brand_id` bigint unsigned DEFAULT NULL COMMENT '所属品牌 ID',
  `spu_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'SPU 名称（商品标题）',
  `spu_description` text COLLATE utf8mb4_general_ci COMMENT '商品详情描述（富文本 HTML）',
  `main_image` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '商品主图 URL',
  `images_json` text COLLATE utf8mb4_general_ci COMMENT '轮播图 JSON 数组',
  `price_min` bigint unsigned DEFAULT '0' COMMENT '最低销售价（单位：分）',
  `price_max` bigint unsigned DEFAULT '0' COMMENT '最高销售价（单位：分）',
  `sales_count` int unsigned DEFAULT '0' COMMENT '累计销量',
  `review_count` int unsigned DEFAULT '0' COMMENT '评价条数',
  `publish_status` tinyint unsigned DEFAULT '0' COMMENT '上下架状态',
  `verify_status` tinyint unsigned DEFAULT '0' COMMENT '审核状态',
  `is_deleted` tinyint unsigned DEFAULT '0' COMMENT '逻辑删除标志',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `version` int unsigned DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_brand_id` (`brand_id`),
  KEY `idx_publish_status` (`publish_status`),
  KEY `idx_category_publish` (`category_id`,`publish_status`)
) ENGINE=InnoDB AUTO_INCREMENT=2031 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品 SPU 表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mall_product_spu`
--

LOCK TABLES `mall_product_spu` WRITE;
/*!40000 ALTER TABLE `mall_product_spu` DISABLE KEYS */;
INSERT INTO `mall_product_spu` VALUES (1001,3,1,'iPhone 15 Pro Max',NULL,'http://127.0.0.1:9205/statics/2026/06/01/IPhone15 pro max_20260601155706A001.dpg',NULL,899900,1099900,0,0,1,1,0,NULL,NULL,'2026-05-19 04:15:48','2026-06-01 15:57:09',0),(2001,3,1,'iPhone 16 Pro Max','Apple A18 Pro 芯片，6.9 英寸超视网膜 XDR 显示屏，钛金属设计，4800 万像素主摄','http://127.0.0.1:9205/statics/2026/06/01/iPhone 16 Pro Max_20260601163501A002.dpg','[\"https://cdn.example.com/spu/2001/main.jpg\",\"https://cdn.example.com/spu/2001/sub1.jpg\",\"https://cdn.example.com/spu/2001/sub2.jpg\"]',999900,1299900,0,22,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2002,3,1,'iPhone 16 Pro','Apple A18 Pro 芯片，6.3 英寸超视网膜 XDR 显示屏，钛金属设计','http://127.0.0.1:9205/statics/2026/06/01/iPhone 16 Pro_20260601163512A003.dpg','[\"https://cdn.example.com/spu/2002/main.jpg\",\"https://cdn.example.com/spu/2002/sub1.jpg\",\"https://cdn.example.com/spu/2002/sub2.jpg\"]',799900,1049900,0,152,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2003,3,1,'iPhone 16','Apple A18 芯片，6.1 英寸超视网膜 XDR 显示屏，4800 万像素主摄','http://127.0.0.1:9205/statics/2026/06/01/iPhone 16_20260601163522A004.dpg','[\"https://cdn.example.com/spu/2003/main.jpg\",\"https://cdn.example.com/spu/2003/sub1.jpg\",\"https://cdn.example.com/spu/2003/sub2.jpg\"]',599900,699900,0,73,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2004,3,2,'华为 Mate 70 Pro','麒麟 9100 芯片，6.9 英寸 OLED 屏幕，5000 万像素超聚光主摄，卫星通信','http://127.0.0.1:9205/statics/2026/06/01/华为 Mate 70 Pro_20260601163754A005.dpg','[\"https://cdn.example.com/spu/2004/main.jpg\",\"https://cdn.example.com/spu/2004/sub1.jpg\",\"https://cdn.example.com/spu/2004/sub2.jpg\"]',699900,949900,6543,199,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2005,3,2,'华为 Pura 80 Pro','麒麟 9000 芯片，6.7 英寸 OLED 四曲屏，XMAGE 影像系统','http://127.0.0.1:9205/statics/2026/06/01/华为 Pura 80 Pro_20260601163805A006.dpg','[\"https://cdn.example.com/spu/2005/main.jpg\",\"https://cdn.example.com/spu/2005/sub1.jpg\",\"https://cdn.example.com/spu/2005/sub2.jpg\"]',599900,879900,5432,215,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2006,3,2,'华为 Mate X6','折叠屏旗舰，麒麟 9100 芯片，7.85 英寸柔性内屏，5000 万像素主摄','http://127.0.0.1:9205/statics/2026/06/01/华为 Mate X6_20260601163814A007.dpg','[\"https://cdn.example.com/spu/2006/main.jpg\",\"https://cdn.example.com/spu/2006/sub1.jpg\",\"https://cdn.example.com/spu/2006/sub2.jpg\"]',1399900,1599900,4321,270,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2007,3,1,'iPhone 15 Pro Max','Apple A17 Pro 芯片，6.7 英寸超视网膜 XDR 显示屏，钛金属设计','http://127.0.0.1:9205/statics/2026/06/01/iPhone 15 Pro Max_20260601163822A008.dpg','[\"https://cdn.example.com/spu/2007/main.jpg\",\"https://cdn.example.com/spu/2007/sub1.jpg\",\"https://cdn.example.com/spu/2007/sub2.jpg\"]',799900,949900,0,359,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2008,8,1,'MacBook Pro 16','Apple M4 Max 芯片，16.2 英寸 Liquid Retina XDR 显示屏，36GB 统一内存','http://127.0.0.1:9205/statics/2026/06/01/MacBook Pro 16_20260601165156A032.dpg','[\"https://cdn.example.com/spu/2008/main.jpg\",\"https://cdn.example.com/spu/2008/sub1.jpg\",\"https://cdn.example.com/spu/2008/sub2.jpg\"]',1999900,2299900,0,88,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2009,8,1,'MacBook Air 15','Apple M4 芯片，15.3 英寸 Liquid Retina 显示屏，18GB 统一内存','http://127.0.0.1:9205/statics/2026/06/01/MacBook Air 15_20260601165147A031.dpg','[\"https://cdn.example.com/spu/2009/main.jpg\",\"https://cdn.example.com/spu/2009/sub1.jpg\",\"https://cdn.example.com/spu/2009/sub2.jpg\"]',1099900,1299900,0,427,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2010,8,2,'华为 MateBook X Pro','Intel Core Ultra 9，14.2 英寸 OLED 3K 触控屏，32GB 内存，超级终端','http://127.0.0.1:9205/statics/2026/06/01/华为 MateBook X Pro_20260601165134A030.dpg','[\"https://cdn.example.com/spu/2010/main.jpg\",\"https://cdn.example.com/spu/2010/sub1.jpg\",\"https://cdn.example.com/spu/2010/sub2.jpg\"]',1299900,1499900,0,241,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2011,8,2,'华为 MateBook 14','Intel Core Ultra 5，14 英寸 2K 触控屏，16GB 内存，超级终端','http://127.0.0.1:9205/statics/2026/06/01/华为 MateBook 14_20260601165113A028.dpg','[\"https://cdn.example.com/spu/2011/main.jpg\",\"https://cdn.example.com/spu/2011/sub1.jpg\",\"https://cdn.example.com/spu/2011/sub2.jpg\"]',699900,799900,0,273,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2012,8,1,'MacBook Pro 14','Apple M4 Pro 芯片，14.2 英寸 Liquid Retina XDR 显示屏，24GB 统一内存','http://127.0.0.1:9205/statics/2026/06/01/MacBook Pro 14_20260601165122A029.dpg','[\"https://cdn.example.com/spu/2012/main.jpg\",\"https://cdn.example.com/spu/2012/sub1.jpg\",\"https://cdn.example.com/spu/2012/sub2.jpg\"]',1499900,1699900,0,41,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2013,8,2,'华为 MateBook 16s','Intel Core Ultra 7，16 英寸 2.5K 大屏，32GB 内存，数字小键盘','http://127.0.0.1:9205/statics/2026/06/01/华为 MateBook 16s_20260601165046A026.dpg','[\"https://cdn.example.com/spu/2013/main.jpg\",\"https://cdn.example.com/spu/2013/sub1.jpg\",\"https://cdn.example.com/spu/2013/sub2.jpg\"]',899900,1049900,0,343,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2014,13,2,'1.5匹 新一级变频冷暖空调','新一级能效，变频冷暖，自清洁，智能WiFi控制，适用15-22㎡','http://127.0.0.1:9205/statics/2026/06/01/1.5匹 新一级变频冷暖空调_20260601165036A025.dpg','[\"https://cdn.example.com/spu/2014/main.jpg\",\"https://cdn.example.com/spu/2014/sub1.jpg\",\"https://cdn.example.com/spu/2014/sub2.jpg\"]',299900,349900,0,226,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2015,13,2,'3匹 新一级变频冷暖立式空调','新一级能效，变频冷暖，圆柱柜机，自清洁，适用30-45㎡','http://127.0.0.1:9205/statics/2026/06/01/3匹 新一级变频冷暖立式空调_20260601165029A024.dpg','[\"https://cdn.example.com/spu/2015/main.jpg\",\"https://cdn.example.com/spu/2015/sub1.jpg\",\"https://cdn.example.com/spu/2015/sub2.jpg\"]',599900,669900,0,90,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2016,13,2,'大1匹 变频冷暖空调','新三级能效，变频冷暖，静音设计，适用10-15㎡','http://127.0.0.1:9205/statics/2026/06/01/大1匹 变频冷暖空调_20260601165018A023.dpg','[\"https://cdn.example.com/spu/2016/main.jpg\",\"https://cdn.example.com/spu/2016/sub1.jpg\",\"https://cdn.example.com/spu/2016/sub2.jpg\"]',259900,274900,0,132,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2017,13,2,'2匹 变频冷暖挂式空调','新一级能效，变频冷暖，挂机设计，自清洁，适用20-30㎡','http://127.0.0.1:9205/statics/2026/06/01/2匹 变频冷暖挂式空调_20260601165007A022.dpg','[\"https://cdn.example.com/spu/2017/main.jpg\",\"https://cdn.example.com/spu/2017/sub1.jpg\",\"https://cdn.example.com/spu/2017/sub2.jpg\"]',449900,504900,0,487,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2018,13,2,'中央空调 一拖四 多联机','全直流变频，一级能效，一拖四，智能控制，适用80-110㎡','http://127.0.0.1:9205/statics/2026/06/01/中央空调 一拖四 多联机_20260601164957A021.dpg','[\"https://cdn.example.com/spu/2018/main.jpg\",\"https://cdn.example.com/spu/2018/sub1.jpg\",\"https://cdn.example.com/spu/2018/sub2.jpg\"]',1299900,1499900,0,465,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2019,18,3,'Nike Dri-FIT 女子速干运动T恤','Dri-FIT 科技排汗速干，轻盈透气，宽松版型，运动健身必备','http://127.0.0.1:9205/statics/2026/06/01/Nike Dri-FIT 女子速干运动T恤_20260601164949A020.dpg','[\"https://cdn.example.com/spu/2019/main.jpg\",\"https://cdn.example.com/spu/2019/sub1.jpg\",\"https://cdn.example.com/spu/2019/sub2.jpg\"]',29900,29900,0,434,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2020,18,3,'Nike 男子套头卫衣','经典棉混纺面料，舒适保暖，简约Logo印花，日常休闲百搭','http://127.0.0.1:9205/statics/2026/06/01/Nike 男子套头卫衣_20260601164801A010.dpg','[\"https://cdn.example.com/spu/2020/main.jpg\",\"https://cdn.example.com/spu/2020/sub1.jpg\",\"https://cdn.example.com/spu/2020/sub2.jpg\"]',49900,49900,0,470,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2021,18,3,'Nike 女子瑜伽运动上衣','高弹力面料，速干透气，支撑性设计，适合瑜伽、普拉提等低强度运动','http://127.0.0.1:9205/statics/2026/06/01/Nike 女子瑜伽运动上衣_20260601164812A011.dpg','[\"https://cdn.example.com/spu/2021/main.jpg\",\"https://cdn.example.com/spu/2021/sub1.jpg\",\"https://cdn.example.com/spu/2021/sub2.jpg\"]',39900,39900,0,91,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2022,18,3,'Nike 男子连帽夹克','防泼水面料，连帽设计，多口袋储物，春秋季户外休闲风衣','http://127.0.0.1:9205/statics/2026/06/01/Nike 男子连帽夹克_20260601164823A012.dpg','[\"https://cdn.example.com/spu/2022/main.jpg\",\"https://cdn.example.com/spu/2022/sub1.jpg\",\"https://cdn.example.com/spu/2022/sub2.jpg\"]',89900,89900,0,338,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2023,18,3,'Nike 女子运动背心','轻盈透气面料，工字背设计，内置胸垫，适合跑步、健身等高强度运动','http://127.0.0.1:9205/statics/2026/06/01/Nike 女子运动背心_20260601164841A013.dpg','[\"https://cdn.example.com/spu/2023/main.jpg\",\"https://cdn.example.com/spu/2023/sub1.jpg\",\"https://cdn.example.com/spu/2023/sub2.jpg\"]',24900,24900,0,276,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2024,18,3,'Nike 男子休闲Polo衫','珠地棉面料，经典翻领设计，Logo刺绣，商务休闲两穿','http://127.0.0.1:9205/statics/2026/06/01/Nike 男子休闲Polo衫_20260601164852A014.dpg','[\"https://cdn.example.com/spu/2024/main.jpg\",\"https://cdn.example.com/spu/2024/sub1.jpg\",\"https://cdn.example.com/spu/2024/sub2.jpg\"]',59900,59900,0,225,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2025,26,4,'玻尿酸补水保湿面膜 20片装','三重玻尿酸精华，深层补水保湿，修护肌肤屏障，适合所有肤质','http://127.0.0.1:9205/statics/2026/06/01/玻尿酸补水保湿面膜 20片装_20260601164900A015.dpg','[\"https://cdn.example.com/spu/2025/main.jpg\",\"https://cdn.example.com/spu/2025/sub1.jpg\",\"https://cdn.example.com/spu/2025/sub2.jpg\"]',10900,15900,0,273,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2026,26,4,'烟酰胺美白淡斑面膜 10片装','5%烟酰胺精华，淡化暗沉提亮肤色，改善痘印色斑','http://127.0.0.1:9205/statics/2026/06/01/烟酰胺美白淡斑面膜 10片装_20260601164911A016.dpg','[\"https://cdn.example.com/spu/2026/main.jpg\",\"https://cdn.example.com/spu/2026/sub1.jpg\",\"https://cdn.example.com/spu/2026/sub2.jpg\"]',8400,12900,0,222,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2027,26,4,'积雪草舒缓修护面膜 10片装','积雪草提取物，舒缓敏感泛红，修护肌肤屏障，敏感肌适用','http://127.0.0.1:9205/statics/2026/06/01/积雪草舒缓修护面膜 10片装_20260601164920A017.dpg','[\"https://cdn.example.com/spu/2027/main.jpg\",\"https://cdn.example.com/spu/2027/sub1.jpg\",\"https://cdn.example.com/spu/2027/sub2.jpg\"]',10400,16900,0,35,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2028,26,4,'水杨酸控油祛痘面膜 10片装','2%水杨酸精华，控油祛痘，清洁毛孔，改善闭口粉刺','http://127.0.0.1:9205/statics/2026/06/01/水杨酸控油祛痘面膜 10片装_20260601164930A018.dpg','[\"https://cdn.example.com/spu/2028/main.jpg\",\"https://cdn.example.com/spu/2028/sub1.jpg\",\"https://cdn.example.com/spu/2028/sub2.jpg\"]',9400,13900,0,440,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2029,26,4,'胶原蛋白紧致弹润面膜 10片装','重组胶原蛋白精华，紧致提拉，淡化细纹，抗氧化抗衰老','http://127.0.0.1:9205/statics/2026/06/01/胶原蛋白紧致弹润面膜 10片装_20260601164938A019.dpg','[\"https://cdn.example.com/spu/2029/main.jpg\",\"https://cdn.example.com/spu/2029/sub1.jpg\",\"https://cdn.example.com/spu/2029/sub2.jpg\"]',16900,23900,0,206,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0),(2030,26,4,'维C提亮抗氧面膜 10片装','VC衍生物精华，抗氧提亮，改善暗沉，焕活肌肤光泽','http://127.0.0.1:9205/statics/2026/06/01/维C提亮抗氧面膜 10片装_20260601164743A009.dpg','[\"https://cdn.example.com/spu/2030/main.jpg\",\"https://cdn.example.com/spu/2030/sub1.jpg\",\"https://cdn.example.com/spu/2030/sub2.jpg\"]',12400,19900,0,254,1,1,0,'system','system','2026-06-01 08:25:42','2026-06-01 18:42:25',0);
/*!40000 ALTER TABLE `mall_product_spu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mall_user`
--

DROP TABLE IF EXISTS `mall_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_user` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `phone` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '手机号',
  `phone_hash` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '手机号哈希',
  `password` varchar(256) COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码哈希',
  `nickname` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '头像 URL',
  `email` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱',
  `email_hash` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱哈希',
  `gender` tinyint unsigned DEFAULT '0' COMMENT '性别',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `user_status` tinyint unsigned DEFAULT '0' COMMENT '用户状态',
  `register_type` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '注册方式',
  `register_ip` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '注册 IP',
  `register_time` datetime DEFAULT NULL COMMENT '注册时间',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最后登录 IP',
  `is_privacy_agreed` tinyint unsigned DEFAULT '0' COMMENT '是否同意隐私协议',
  `privacy_agreed_time` datetime DEFAULT NULL COMMENT '同意隐私协议时间',
  `wechat_openid` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '微信 OpenID',
  `wechat_unionid` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '微信 UnionID',
  `is_deleted` tinyint unsigned DEFAULT '0' COMMENT '逻辑删除标志',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `version` int unsigned DEFAULT '0' COMMENT '乐观锁版本号',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone_hash` (`phone_hash`),
  UNIQUE KEY `uk_email_hash` (`email_hash`),
  UNIQUE KEY `uk_wechat_openid` (`wechat_openid`),
  KEY `idx_user_status_register_time` (`user_status`,`register_time`),
  KEY `idx_nickname` (`nickname`(20)),
  KEY `idx_register_time` (`register_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户账号表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mall_user`
--

LOCK TABLES `mall_user` WRITE;
/*!40000 ALTER TABLE `mall_user` DISABLE KEYS */;
/*!40000 ALTER TABLE `mall_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mall_user_address`
--

DROP TABLE IF EXISTS `mall_user_address`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_user_address` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `user_id` bigint unsigned NOT NULL COMMENT '用户 ID',
  `receiver_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '收件人姓名',
  `receiver_phone` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '收件人手机号',
  `province` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '省',
  `city` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '市',
  `district` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '区',
  `detail_address` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '详细地址',
  `zip_code` varchar(10) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮编',
  `is_default` tinyint unsigned DEFAULT '0' COMMENT '是否默认地址',
  `label` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '地址标签',
  `is_deleted` tinyint unsigned DEFAULT '0' COMMENT '逻辑删除标志',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_id_is_default` (`user_id`,`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='地址簿表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mall_user_address`
--

LOCK TABLES `mall_user_address` WRITE;
/*!40000 ALTER TABLE `mall_user_address` DISABLE KEYS */;
/*!40000 ALTER TABLE `mall_user_address` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mall_user_growth_log`
--

DROP TABLE IF EXISTS `mall_user_growth_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_user_growth_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `user_id` bigint unsigned NOT NULL COMMENT '用户 ID',
  `biz_type` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务类型',
  `biz_no` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务单号',
  `change_type` tinyint unsigned NOT NULL COMMENT '变动方向',
  `growth` int unsigned NOT NULL COMMENT '本次变动成长值',
  `before_growth` int unsigned DEFAULT NULL COMMENT '变动前成长值余额',
  `after_growth` int unsigned DEFAULT NULL COMMENT '变动后成长值余额',
  `remark` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '变动原因说明',
  `is_deleted` tinyint unsigned DEFAULT '0' COMMENT '逻辑删除标志',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_id_biz_type` (`user_id`,`biz_type`),
  KEY `idx_biz_no` (`biz_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='成长值流水表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mall_user_growth_log`
--

LOCK TABLES `mall_user_growth_log` WRITE;
/*!40000 ALTER TABLE `mall_user_growth_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `mall_user_growth_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mall_user_member`
--

DROP TABLE IF EXISTS `mall_user_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_user_member` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `user_id` bigint unsigned NOT NULL COMMENT '用户 ID',
  `level_id` bigint unsigned DEFAULT NULL COMMENT '当前等级',
  `growth` int unsigned DEFAULT '0' COMMENT '当前成长值',
  `total_growth` int unsigned DEFAULT '0' COMMENT '累计成长值',
  `level_start_time` datetime DEFAULT NULL COMMENT '最近一次等级生效时间',
  `level_end_time` datetime DEFAULT NULL COMMENT '等级到期时间',
  `become_time` datetime DEFAULT NULL COMMENT '首次成为会员时间',
  `is_deleted` tinyint unsigned DEFAULT '0' COMMENT '逻辑删除标志',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_level_id` (`level_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户会员信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mall_user_member`
--

LOCK TABLES `mall_user_member` WRITE;
/*!40000 ALTER TABLE `mall_user_member` DISABLE KEYS */;
/*!40000 ALTER TABLE `mall_user_member` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mall_user_member_level`
--

DROP TABLE IF EXISTS `mall_user_member_level`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_user_member_level` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `level_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '等级名称',
  `level_value` tinyint unsigned NOT NULL COMMENT '等级值',
  `min_growth` int unsigned NOT NULL COMMENT '该等级所需的最低成长值',
  `max_growth` int unsigned NOT NULL COMMENT '该等级的最高成长值',
  `icon` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '等级图标 URL',
  `benefits_json` text COLLATE utf8mb4_general_ci COMMENT '权益',
  `is_deleted` tinyint unsigned DEFAULT '0' COMMENT '逻辑删除标志',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_level_value` (`level_value`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会员等级定义表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mall_user_member_level`
--

LOCK TABLES `mall_user_member_level` WRITE;
/*!40000 ALTER TABLE `mall_user_member_level` DISABLE KEYS */;
INSERT INTO `mall_user_member_level` VALUES (1,'普通会员',1,0,999,NULL,'{\"discountRate\":100,\"freeShipping\":false,\"pointsMultiplier\":1.0}',0,'2026-05-19 04:15:55','2026-05-19 04:15:55'),(2,'银卡会员',2,1000,2999,NULL,'{\"discountRate\":98,\"freeShipping\":false,\"freeShippingThreshold\":9900,\"pointsMultiplier\":1.1}',0,'2026-05-19 04:15:55','2026-05-19 04:15:55'),(3,'金卡会员',3,3000,8999,NULL,'{\"discountRate\":95,\"freeShipping\":true,\"pointsMultiplier\":1.2}',0,'2026-05-19 04:15:55','2026-05-19 04:15:55'),(4,'钻石会员',4,9000,99999,NULL,'{\"discountRate\":90,\"freeShipping\":true,\"pointsMultiplier\":1.5}',0,'2026-05-19 04:15:55','2026-05-19 04:15:55');
/*!40000 ALTER TABLE `mall_user_member_level` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mall_user_points_account`
--

DROP TABLE IF EXISTS `mall_user_points_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_user_points_account` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `user_id` bigint unsigned NOT NULL COMMENT '用户 ID',
  `total_points` int unsigned DEFAULT '0' COMMENT '累计积分',
  `available_points` int unsigned DEFAULT '0' COMMENT '可用积分余额',
  `used_points` int unsigned DEFAULT '0' COMMENT '已使用积分',
  `expired_points` int unsigned DEFAULT '0' COMMENT '已过期积分',
  `version` int unsigned DEFAULT '0' COMMENT '乐观锁版本号',
  `is_deleted` tinyint unsigned DEFAULT '0' COMMENT '逻辑删除标志',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='积分账户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mall_user_points_account`
--

LOCK TABLES `mall_user_points_account` WRITE;
/*!40000 ALTER TABLE `mall_user_points_account` DISABLE KEYS */;
/*!40000 ALTER TABLE `mall_user_points_account` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mall_user_points_log`
--

DROP TABLE IF EXISTS `mall_user_points_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_user_points_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `user_id` bigint unsigned NOT NULL COMMENT '用户 ID',
  `biz_type` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务类型',
  `biz_no` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务单号',
  `change_type` tinyint unsigned NOT NULL COMMENT '变动方向',
  `points` int unsigned NOT NULL COMMENT '本次变动积分值',
  `before_points` int unsigned DEFAULT NULL COMMENT '变动前积分余额',
  `after_points` int unsigned DEFAULT NULL COMMENT '变动后积分余额',
  `remark` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '变动原因说明',
  `is_deleted` tinyint unsigned DEFAULT '0' COMMENT '逻辑删除标志',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_id_biz_type` (`user_id`,`biz_type`),
  KEY `idx_biz_no` (`biz_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='积分流水表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mall_user_points_log`
--

LOCK TABLES `mall_user_points_log` WRITE;
/*!40000 ALTER TABLE `mall_user_points_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `mall_user_points_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'mall'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-02 18:35:24
