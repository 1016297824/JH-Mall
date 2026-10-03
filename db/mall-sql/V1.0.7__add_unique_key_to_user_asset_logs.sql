-- 用户资产流水表加唯一键：把并发重复发放的窗口从根上关掉
-- 版本：V1.0.7
-- 背景：
--   积分/成长值的幂等原先靠「先查流水是否存在、再改余额」。
--   真机实测同一订单被投递 3 次时，三次查询都在任何插入提交之前完成，
--   于是三次全部通过校验：积分与成长值各被发放 3 次（17998 × 3），
--   且成长值出现丢失更新——两条流水的 before_growth 都是 0。
--   结论：check-then-act 挡不住并发，必须以数据库唯一键作为幂等锚点。
--
-- 配套代码改动：
--   PointsServiceImpl#addPoints / MemberServiceImpl#addGrowth 改为
--   「先插流水占位 → 再改余额 → 回填快照」，撞唯一键即视为已发放并跳过；
--   余额变更失败时硬删除占位行（软删除仍占用唯一键，会让该业务单永远无法重试补发）。
--
-- 唯一性前提（已核对全部调用方）：
--   积分：ORDER+orderNo（订单完成）、SIGN_IN+bizNo=NULL（签到）、EXPIRE+bizNo=NULL（年度清零）
--   成长值：ORDER+orderNo（订单完成）
--   MySQL 唯一索引不约束 NULL，故 bizNo 为空的写入不受影响。

-- 1) 防御性清理历史重复：同一 (user_id, biz_type, biz_no) 只保留 id 最小的一条。
--    注意：本语句只去重流水，不会修正已被多发的余额——存量数据需按业务单人工核对后调整。
DELETE l FROM `mall_user_points_log` l
JOIN (
    SELECT `user_id`, `biz_type`, `biz_no`, MIN(`id`) AS keep_id
    FROM `mall_user_points_log`
    WHERE `biz_no` IS NOT NULL
    GROUP BY `user_id`, `biz_type`, `biz_no`
    HAVING COUNT(*) > 1
) d ON l.`user_id` = d.`user_id` AND l.`biz_type` = d.`biz_type` AND l.`biz_no` = d.`biz_no`
WHERE l.`id` <> d.keep_id;

DELETE l FROM `mall_user_growth_log` l
JOIN (
    SELECT `user_id`, `biz_type`, `biz_no`, MIN(`id`) AS keep_id
    FROM `mall_user_growth_log`
    WHERE `biz_no` IS NOT NULL
    GROUP BY `user_id`, `biz_type`, `biz_no`
    HAVING COUNT(*) > 1
) d ON l.`user_id` = d.`user_id` AND l.`biz_type` = d.`biz_type` AND l.`biz_no` = d.`biz_no`
WHERE l.`id` <> d.keep_id;

-- 2) 幂等锚点
ALTER TABLE `mall_user_points_log`
    ADD UNIQUE KEY `uk_user_biz` (`user_id`, `biz_type`, `biz_no`);

ALTER TABLE `mall_user_growth_log`
    ADD UNIQUE KEY `uk_user_biz` (`user_id`, `biz_type`, `biz_no`);
