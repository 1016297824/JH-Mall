-- 订单表补充字段：退款前状态 + 物流信息
-- 版本：V1.0.6
-- 背景：
--   1. pre_refund_status
--      设计文档 §6.3 中 REFUND_FAIL 需回退到「退款发生前」的状态
--      （PAID / WAIT_DELIVER / WAIT_RECEIVE / COMPLETED 之一），
--      但原表无此字段，只能靠 complete_time / delivery_time 反推，
--      导致 WAIT_DELIVER 与 WAIT_RECEIVE 无法区分（两者都只有 delivery_time）。
--      状态机进入 REFUNDING 时写入原状态，退款失败时据此精确回退。
--   2. logistics_company / logistics_no
--      设计文档 §6.3 中 SELLER_DELIVER 的前置条件是「物流单号 + 公司已填写」，
--      但原表无对应字段，该前置条件只能在 Service 层口头保证、无法校验。
--      补齐后可下沉到状态机。

ALTER TABLE `mall_order`
    ADD COLUMN `pre_refund_status` tinyint unsigned DEFAULT NULL COMMENT '退款前状态码，进入 REFUNDING 时写入原状态，取值见 OrderStatusEnum' AFTER `cancel_reason`,
    ADD COLUMN `logistics_company` varchar(50) DEFAULT NULL COMMENT '物流公司' AFTER `pre_refund_status`,
    ADD COLUMN `logistics_no` varchar(50) DEFAULT NULL COMMENT '物流单号' AFTER `logistics_company`;