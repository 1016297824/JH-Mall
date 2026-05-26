ALTER TABLE `mall_user_points_account`
    ADD COLUMN `version` int unsigned DEFAULT 0 COMMENT '乐观锁版本号' AFTER `expired_points`;
