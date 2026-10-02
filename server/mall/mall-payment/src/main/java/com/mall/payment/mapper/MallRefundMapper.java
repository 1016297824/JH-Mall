package com.mall.payment.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.payment.DO.MallRefundDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 退款单 Mapper
 *
 * <p>状态推进走显式 CAS SQL，理由同 {@link MallPaymentMapper}。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Mapper
public interface MallRefundMapper extends BaseMapper<MallRefundDO> {

    /** 退款单状态：处理中，与 {@code RefundStatusEnum.PROCESSING} 一致 */
    int STATUS_PROCESSING = 0;

    /** 退款单状态：退款成功，与 {@code RefundStatusEnum.SUCCESS} 一致 */
    int STATUS_SUCCESS = 1;

    /** 退款单状态：退款失败，与 {@code RefundStatusEnum.FAILED} 一致 */
    int STATUS_FAILED = 2;

    /**
     * 按退款单号查询未删除的退款单
     *
     * @param refundNo 退款单号
     * @return 退款单，不存在返回 null
     */
    default MallRefundDO selectByRefundNo(String refundNo) {
        return selectOne(new LambdaQueryWrapper<MallRefundDO>()
                .eq(MallRefundDO::getRefundNo, refundNo)
                .eq(MallRefundDO::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    /**
     * 按幂等键查询退款单
     *
     * @param idempotentKey 幂等键，格式 {@code afterSaleNo_channelCode}
     * @return 退款单，不存在返回 null
     */
    default MallRefundDO selectByIdempotentKey(String idempotentKey) {
        return selectOne(new LambdaQueryWrapper<MallRefundDO>()
                .eq(MallRefundDO::getIdempotentKey, idempotentKey)
                .eq(MallRefundDO::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    /**
     * 按订单号查询退款单列表
     *
     * <p>一个订单可能有多笔部分退款（多次售后），故返回列表。</p>
     *
     * @param orderNo 订单号
     * @return 退款单列表，按主键升序
     */
    default List<MallRefundDO> selectByOrderNo(String orderNo) {
        return selectList(new LambdaQueryWrapper<MallRefundDO>()
                .eq(MallRefundDO::getOrderNo, orderNo)
                .eq(MallRefundDO::getIsDeleted, 0)
                .orderByAsc(MallRefundDO::getId));
    }

    /**
     * 按渠道侧退款单号查询退款单
     *
     * <p>退款回调只携带渠道侧退款单号，据此定位本地退款单。依赖
     * {@code channel_refund_no} 在「渠道受理时」就已回填
     * （见 {@link #updateChannelRefundNo}）。</p>
     *
     * @param channelRefundNo 渠道侧退款单号
     * @return 退款单，不存在返回 null
     */
    default MallRefundDO selectByChannelRefundNo(String channelRefundNo) {
        return selectOne(new LambdaQueryWrapper<MallRefundDO>()
                .eq(MallRefundDO::getChannelRefundNo, channelRefundNo)
                .eq(MallRefundDO::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    /**
     * 统计某支付单的累计占用退款金额
     *
     * <p><b>为何 PROCESSING 也要计入</b>：退款是异步的，发起后到渠道回调前状态一直是
     * PROCESSING。若只统计 SUCCESS，同一支付单的并发退款请求会各自看到「未超额」，
     * 导致累计退款超过支付金额（超退）。故取 {@code refund_status IN (0, 1)}。</p>
     *
     * @param paymentId 支付单 ID
     * @return 累计退款金额（单位：分），无记录返回 0
     */
    @Select("SELECT COALESCE(SUM(refund_amount), 0) FROM mall_payment_refund "
            + "WHERE payment_id = #{paymentId} AND refund_status IN (" + STATUS_PROCESSING + ", "
            + STATUS_SUCCESS + ") AND is_deleted = 0")
    Long sumRefundedAmount(@Param("paymentId") Long paymentId);

    /**
     * 回填渠道侧退款单号
     *
     * <p><b>为何必须在「渠道受理后」立即回填</b>：渠道的退款回调<b>只携带渠道侧退款单号</b>，
     * 若等到退款成功才写 {@code channel_refund_no}，那么处于 PROCESSING 的退款单在收到
     * 回调时将无法被定位（渠道返回「处理中」是常态，终态由回调推进）。</p>
     *
     * <p>本方法<b>不改 {@code version}</b>：它只是字段回填而非状态推进，
     * 且同一笔退款只可能对应一个渠道退款单号，并发下无覆盖风险；
     * 若在此递增 version，反而会让后续 {@code markSuccess} 的 CAS 条件失配。</p>
     *
     * @param refundNo            退款单号
     * @param channelRefundNo     渠道侧退款单号
     * @param channelRefundStatus 渠道侧退款状态原文
     * @return 影响行数
     */
    @Update("UPDATE mall_payment_refund SET channel_refund_no = #{channelRefundNo}, "
            + "channel_refund_status = #{channelRefundStatus}, update_time = NOW() "
            + "WHERE refund_no = #{refundNo} AND is_deleted = 0")
    int updateChannelRefundNo(@Param("refundNo") String refundNo,
                              @Param("channelRefundNo") String channelRefundNo,
                              @Param("channelRefundStatus") String channelRefundStatus);

    /**
     * 退款成功推进（CAS）
     *
     * @param refundNo            退款单号
     * @param channelRefundNo     渠道侧退款单号
     * @param channelRefundStatus 渠道侧退款状态原文
     * @param version             期望版本号
     * @return 影响行数
     */
    @Update("UPDATE mall_payment_refund SET refund_status = " + STATUS_SUCCESS + ", "
            + "channel_refund_no = #{channelRefundNo}, "
            + "channel_refund_status = #{channelRefundStatus}, refund_success_time = NOW(), "
            + "version = version + 1, update_time = NOW() "
            + "WHERE refund_no = #{refundNo} AND refund_status = " + STATUS_PROCESSING + " "
            + "AND version = #{version} AND is_deleted = 0")
    int markSuccess(@Param("refundNo") String refundNo,
                    @Param("channelRefundNo") String channelRefundNo,
                    @Param("channelRefundStatus") String channelRefundStatus,
                    @Param("version") Integer version);

    /**
     * 退款失败推进（CAS）
     *
     * @param refundNo            退款单号
     * @param channelRefundStatus 渠道侧失败原因/状态
     * @param version             期望版本号
     * @return 影响行数
     */
    @Update("UPDATE mall_payment_refund SET refund_status = " + STATUS_FAILED + ", "
            + "channel_refund_status = #{channelRefundStatus}, "
            + "version = version + 1, update_time = NOW() "
            + "WHERE refund_no = #{refundNo} AND refund_status = " + STATUS_PROCESSING + " "
            + "AND version = #{version} AND is_deleted = 0")
    int markFailed(@Param("refundNo") String refundNo,
                   @Param("channelRefundStatus") String channelRefundStatus,
                   @Param("version") Integer version);

    /**
     * 重试退款：FAILED → PROCESSING（CAS）
     *
     * <p>操作员对失败退款单重新发起时调用。</p>
     *
     * @param refundNo 退款单号
     * @param version  期望版本号
     * @return 影响行数
     */
    @Update("UPDATE mall_payment_refund SET refund_status = " + STATUS_PROCESSING + ", "
            + "version = version + 1, update_time = NOW() "
            + "WHERE refund_no = #{refundNo} AND refund_status = " + STATUS_FAILED + " "
            + "AND version = #{version} AND is_deleted = 0")
    int markRetrying(@Param("refundNo") String refundNo, @Param("version") Integer version);
}
