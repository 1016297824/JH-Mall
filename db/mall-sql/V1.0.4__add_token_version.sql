-- ============================================
-- mall-user 服务：新增 token_version 字段
-- 版本：V1.0.4
-- 说明：用于全端下线（改密码 / 踢人），JWT.ver 与此字段比对
-- ============================================

ALTER TABLE mall_user ADD COLUMN token_version INT NOT NULL DEFAULT 1;
