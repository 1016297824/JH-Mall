package com.mall.order.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.order.DO.MallOrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 订单 Mapper
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Mapper
public interface MallOrderMapper extends BaseMapper<MallOrderDO> {

    /**
     * 按订单号查询
     *
     * @param orderNo 订单号
     * @return 订单，不存在返回 null
     */
    default MallOrderDO selectByOrderNo(String orderNo) {
        return selectOne(new LambdaQueryWrapper<MallOrderDO>()
                .eq(MallOrderDO::getOrderNo, orderNo)
                .eq(MallOrderDO::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    /**
     * 按幂等键查询
     *
     * <p>{@code uk_idempotent_key} 是数据库级唯一约束，Redis 幂等失效时
     * 靠它兜底：插入撞唯一键后用本方法回查已存在的订单号返回给客户端。</p>
     *
     * @param idempotentKey 幂等键（形如 {@code userId:clientRequestNo}）
     * @return 订单，不存在返回 null
     */
    default MallOrderDO selectByIdempotentKey(String idempotentKey) {
        return selectOne(new LambdaQueryWrapper<MallOrderDO>()
                .eq(MallOrderDO::getIdempotentKey, idempotentKey)
                .last("LIMIT 1"));
    }

    /**
     * 超时关单（乐观锁）
     *
     * <p>SQL 中的状态码对应 {@code OrderStatusEnum}：0 = WAIT_PAY，6 = CLOSED；
     * {@code cancel_type} 取值对应 {@code CancelTypeEnum.TIMEOUT_CANCEL}（{@code timeout_cancel}）。
     * {@code WHERE order_status = 0} 是竞态防护核心：</p>
     * <ul>
     *   <li>支付回调先到 → 状态已变 1（PAID），本方法影响 0 行，MQ 消费与兜底任务据此跳过</li>
     *   <li>本方法先到 → 状态变 6（CLOSED），随后支付回调的状态机会报 A0702</li>
     * </ul>
     *
     * @param orderNo 订单号
     * @return 影响行数，1=关单成功；0=已支付或已关闭
     */
    @Update("UPDATE mall_order SET order_status = 6, cancel_type = 'timeout_cancel', "
            + "cancel_time = NOW(), update_time = NOW(), version = version + 1 "
            + "WHERE order_no = #{orderNo} AND order_status = 0 AND is_deleted = 0")
    int closeByTimeout(@Param("orderNo") String orderNo);

    /**
     * 扫描超时未支付订单（兜底日扫用）
     *
     * <p>依赖 {@code idx_order_status_pay_expire} 索引。</p>
     *
     * @param limit 单批条数
     * @return 超时订单列表
     */
    default java.util.List<MallOrderDO> selectTimeoutOrders(int limit) {
        return selectList(new LambdaQueryWrapper<MallOrderDO>()
                .eq(MallOrderDO::getOrderStatus, 0)
                .isNotNull(MallOrderDO::getPayExpireTime)
                .le(MallOrderDO::getPayExpireTime, java.time.LocalDateTime.now())
                .eq(MallOrderDO::getIsDeleted, 0)
                .orderByAsc(MallOrderDO::getPayExpireTime)
                .last("LIMIT " + limit));
    }

    /**
     * 扫描达到自动确认收货时限的订单
     *
     * <p>发货后 {@code days} 天用户未手动确认则自动完成（设计文档 §5.10）。</p>
     *
     * @param days    自动确认天数
     * @param limit   单批条数
     * @return 待自动确认的订单列表
     */
    default java.util.List<MallOrderDO> selectAutoConfirmOrders(int days, int limit) {
        return selectList(new LambdaQueryWrapper<MallOrderDO>()
                .eq(MallOrderDO::getOrderStatus, 3)
                .isNotNull(MallOrderDO::getDeliveryTime)
                .le(MallOrderDO::getDeliveryTime,
                        java.time.LocalDateTime.now().minusDays(days))
                .eq(MallOrderDO::getIsDeleted, 0)
                .orderByAsc(MallOrderDO::getDeliveryTime)
                .last("LIMIT " + limit));
    }

    /**
     * 带乐观锁的状态更新
     *
     * <p>状态变更必须先经 {@code OrderStateMachine.transition()} 校验，
     * 再调用本方法落库。{@code WHERE order_status = #{originStatus}} 兜底防并发覆盖。</p>
     *
     * <p>{@code preRefundStatus} 仅在进入 REFUNDING 时由状态机写入，
     * 非退款流转时传 null，该列保持原值不变。</p>
     *
     * @param orderNo         订单号
     * @param targetStatus   目标状态码
     * @param originStatus 转移前状态码
     * @param version      乐观锁版本号
     * @param preRefundStatus 退款前状态码，非退款流转传 null
     * @return 影响行数，1=成功；0=并发冲突
     */
    @Update("<script>UPDATE mall_order SET order_status = #{targetStatus}, update_time = NOW(), " +
            "version = version + 1 " +
            "<if test='preRefundStatus != null'>" +
            ", pre_refund_status = #{preRefundStatus} " +
            "</if>" +
            "WHERE order_no = #{orderNo} AND order_status = #{originStatus} " +
            "AND version = #{version} AND is_deleted = 0</script>")
    int updateStatusCas(@Param("orderNo") String orderNo,
                        @Param("targetStatus") Integer targetStatus,
                        @Param("originStatus") Integer originStatus,
                        @Param("version") Integer version,
                        @Param("preRefundStatus") Integer preRefundStatus);

    /**
     * 填写物流信息
     *
     * <p>需在调用 {@code OrderStateMachine.transition(SELLER_DELIVER)} <b>之前</b>执行，
     * 否则状态机会因物流信息缺失抛 A0703。</p>
     *
     * @param orderNo         订单号
     * @param logisticsCompany 物流公司
     * @param logisticsNo     物流单号
     * @return 影响行数
     */
    @Update("UPDATE mall_order SET logistics_company = #{logisticsCompany}, " +
            "logistics_no = #{logisticsNo}, delivery_time = NOW(), update_time = NOW() " +
            "WHERE order_no = #{orderNo} AND order_status = 1 AND is_deleted = 0")
    int updateLogistics(@Param("orderNo") String orderNo,
                        @Param("logisticsCompany") String logisticsCompany,
                        @Param("logisticsNo") String logisticsNo);

    /**
     * 记录支付时间
     *
     * <p>{@link #updateStatusCas} 只推进状态、不写时间线字段，故由调用方在同一事务内单独补齐。
     * 不加状态前置条件：CAS 已经保证只有合法流转才会走到这里。</p>
     *
     * @param orderNo 订单号
     * @return 影响行数
     */
    @Update("UPDATE mall_order SET pay_time = NOW(), update_time = NOW() "
            + "WHERE order_no = #{orderNo} AND is_deleted = 0")
    int markPayTime(@Param("orderNo") String orderNo);

    /**
     * 记录订单完成时间
     *
     * @param orderNo 订单号
     * @return 影响行数
     */
    @Update("UPDATE mall_order SET complete_time = NOW(), update_time = NOW() "
            + "WHERE order_no = #{orderNo} AND is_deleted = 0")
    int markCompleteTime(@Param("orderNo") String orderNo);

    /**
     * 记录取消时间与取消类型
     *
     * <p>{@code cancelType} 取值见 {@code CancelTypeEnum}。</p>
     *
     * @param orderNo    订单号
     * @param cancelType 取消类型码
     * @return 影响行数
     */
    @Update("UPDATE mall_order SET cancel_time = NOW(), cancel_type = #{cancelType}, "
            + "update_time = NOW() WHERE order_no = #{orderNo} AND is_deleted = 0")
    int markCancelTime(@Param("orderNo") String orderNo,
                       @Param("cancelType") String cancelType);
}