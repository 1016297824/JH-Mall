-- 归一化订单取消类型的历史取值
-- 版本：V1.0.8
-- 背景：
--   cancel_type 一列原本并存三套词汇——
--     ① CancelTypeEnum（mall-common，权威）：user_cancel / timeout_cancel / admin_cancel
--     ② MallOrderMapper.closeByTimeout 的 SQL 字面量：'PAY_TIMEOUT'
--     ③ MallOrderDO / OrderVO 的注释：USER_CANCEL / PAY_TIMEOUT / FORCE_CANCEL
--   2026-10-03 已把代码统一收敛到 ①（超时关单改写 'timeout_cancel'，
--   强制取消写 'admin_cancel'，用户取消写 'user_cancel'），
--   但在此之前落库的行仍是 ② 的 'PAY_TIMEOUT'，需要一次性订正。
--
--   注意：仓库内没有任何逻辑按 cancel_type 分支（前端也未展示该字段），
--   故本迁移只影响数据一致性，不改变行为。

UPDATE `mall_order`
SET `cancel_type` = 'timeout_cancel'
WHERE `cancel_type` = 'PAY_TIMEOUT';

-- 兜底：早期若还写过其它非枚举取值，统一归到管理员取消，避免留下无法解释的值
UPDATE `mall_order`
SET `cancel_type` = 'admin_cancel'
WHERE `cancel_type` IS NOT NULL
  AND `cancel_type` NOT IN ('user_cancel', 'timeout_cancel', 'admin_cancel');
