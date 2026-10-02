package com.mall.payment.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.payment.DO.MallPaymentDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付单 Mapper
 *
 * <p>状态推进一律走显式 CAS SQL（{@code WHERE payment_status=? AND version=?}），
 * 影响 0 行即表示被并发方抢先处理，由调用方决定是幂等返回还是抛错。</p>
 *
 * <p><b>为什么不用 {@code updateById}</b>：{@code updateById} 会把查询出来的整行写回，
 * 在「回调与定时任务并发推进同一支付单」的场景下会互相覆盖。</p>
 *
 * <p>⚠️ SQL 中的状态码使用本接口的<b>编译期常量</b>拼接，不在 SQL 里写裸数字——
 * 设计文档 §5.3 曾把 PAID 误写为 2（实际 2 是 FAILED），写死数字极易埋雷。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Mapper
public interface MallPaymentMapper extends BaseMapper<MallPaymentDO> {

    /** 支付单状态：待支付，与 {@code PaymentStatusEnum.UNPAID} 一致 */
    int STATUS_UNPAID = 0;

    /** 支付单状态：已支付，与 {@code PaymentStatusEnum.PAID} 一致 */
    int STATUS_PAID = 1;

    /** 支付单状态：支付失败，与 {@code PaymentStatusEnum.FAILED} 一致 */
    int STATUS_FAILED = 2;

    /** 支付单状态：已关闭，与 {@code PaymentStatusEnum.CLOSED} 一致 */
    int STATUS_CLOSED = 3;

    /** 支付单状态：退款中，与 {@code PaymentStatusEnum.REFUNDING} 一致 */
    int STATUS_REFUNDING = 4;

    /** 支付单状态：已退款，与 {@code PaymentStatusEnum.REFUNDED} 一致 */
    int STATUS_REFUNDED = 5;

    /**
     * 按支付单号查询未删除的支付单
     *
     * @param paymentNo 支付单号
     * @return 支付单，不存在返回 null
     */
    default MallPaymentDO selectByPaymentNo(String paymentNo) {
        return selectOne(new LambdaQueryWrapper<MallPaymentDO>()
                .eq(MallPaymentDO::getPaymentNo, paymentNo)
                .eq(MallPaymentDO::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    /**
     * 按幂等键查询支付单
     *
     * @param idempotentKey 幂等键，格式 {@code userId_orderNo_channelCode}
     * @return 支付单，不存在返回 null
     */
    default MallPaymentDO selectByIdempotentKey(String idempotentKey) {
        return selectOne(new LambdaQueryWrapper<MallPaymentDO>()
                .eq(MallPaymentDO::getIdempotentKey, idempotentKey)
                .eq(MallPaymentDO::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    /**
     * 按渠道侧支付单号查询支付单
     *
     * <p><b>回调链路的唯一入口</b>：渠道回调只携带<b>它自己的</b>支付单号，
     * 必须先用本方法映射到本地支付单，才能拿到本地 {@code payment_no} 与 {@code version}
     * 去做 CAS 落库。用按本地单号查询的方法去接渠道单号是语义错位，永远查不到。</p>
     *
     * @param channelPaymentNo 渠道侧支付单号
     * @return 支付单，不存在返回 null
     */
    default MallPaymentDO selectByChannelPaymentNo(String channelPaymentNo) {
        return selectOne(new LambdaQueryWrapper<MallPaymentDO>()
                .eq(MallPaymentDO::getChannelPaymentNo, channelPaymentNo)
                .eq(MallPaymentDO::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    /**
     * 按订单号查询「实际支付成功」的支付单
     *
     * <p>供退款入口使用：mall-order 售后审核通过时只持有 {@code orderNo}，
     * 需据此反查支付单。同一订单可能有多个渠道的支付单（用户换渠道重试），
     * 但真正付款成功的只会有一张，故限定状态为
     * {@code PAID / REFUNDING / REFUNDED} 并取最新一条。</p>
     *
     * @param orderNo 订单号
     * @return 支付单，不存在返回 null
     */
    default MallPaymentDO selectPaidByOrderNo(String orderNo) {
        return selectOne(new LambdaQueryWrapper<MallPaymentDO>()
                .eq(MallPaymentDO::getOrderNo, orderNo)
                .in(MallPaymentDO::getPaymentStatus, STATUS_PAID, STATUS_REFUNDING, STATUS_REFUNDED)
                .eq(MallPaymentDO::getIsDeleted, 0)
                .orderByDesc(MallPaymentDO::getId)
                .last("LIMIT 1"));
    }

    /**
     * 扫描超时未支付的支付单
     *
     * <p>供定时任务「超时关单」使用：状态仍为 UNPAID 且已过 {@code expire_time}。</p>
     *
     * @param now   当前时间
     * @param limit 单批拉取条数
     * @return 待关闭的支付单列表
     */
    default List<MallPaymentDO> selectTimeoutUnpaid(LocalDateTime now, int limit) {
        return selectList(new LambdaQueryWrapper<MallPaymentDO>()
                .eq(MallPaymentDO::getPaymentStatus, STATUS_UNPAID)
                .lt(MallPaymentDO::getExpireTime, now)
                .eq(MallPaymentDO::getIsDeleted, 0)
                .orderByAsc(MallPaymentDO::getId)
                .last("LIMIT " + limit));
    }

    /**
     * 查询需要对账的未支付单（回调丢失补偿用）
     *
     * <p>筛选条件：仍未支付 + <b>已发起过渠道</b>（否则无从对账）+
     * 创建已超过补偿延迟（给回调留足到达时间）+ <b>尚未过期</b>。</p>
     *
     * <p>「尚未过期」是刻意加的：与 {@link #selectTimeoutUnpaid} 的扫描窗口
     * <b>互不重叠</b>，避免「对账任务刚判定未支付、关单任务随即关闭、
     * 之后回调才到达」这类竞态被两个任务同时放大。</p>
     *
     * @param now           当前时间（过期下界）
     * @param createdBefore 创建时间上界，早于此时间才纳入对账
     * @param limit         单次返回上限
     * @return 待对账支付单列表
     */
    default List<MallPaymentDO> selectReconcileCandidates(LocalDateTime now,
                                                          LocalDateTime createdBefore,
                                                          int limit) {
        return selectList(new LambdaQueryWrapper<MallPaymentDO>()
                .eq(MallPaymentDO::getPaymentStatus, STATUS_UNPAID)
                .isNotNull(MallPaymentDO::getChannelPaymentNo)
                .lt(MallPaymentDO::getCreateTime, createdBefore)
                .gt(MallPaymentDO::getExpireTime, now)
                .eq(MallPaymentDO::getIsDeleted, 0)
                .orderByAsc(MallPaymentDO::getId)
                .last("LIMIT " + limit));
    }

    /**
     * 支付成功推进（CAS）
     *
     * <p>仅当状态仍是 UNPAID 且版本未变时生效，是「先落库再应答」的落点。</p>
     *
     * @param paymentNo        支付单号
     * @param channelPayStatus 渠道侧支付状态原文
     * @param version          期望版本号
     * @return 影响行数，0 表示已被其他回调/定时任务处理
     */
    @Update("UPDATE mall_payment SET payment_status = " + STATUS_PAID + ", "
            + "channel_pay_status = #{channelPayStatus}, pay_success_time = NOW(), "
            + "version = version + 1, update_time = NOW() "
            + "WHERE payment_no = #{paymentNo} AND payment_status = " + STATUS_UNPAID + " "
            + "AND version = #{version} AND is_deleted = 0")
    int markPaid(@Param("paymentNo") String paymentNo,
                 @Param("channelPayStatus") String channelPayStatus,
                 @Param("version") Integer version);

    /**
     * 支付失败推进（CAS）
     *
     * @param paymentNo        支付单号
     * @param channelPayStatus 渠道侧失败原因/状态
     * @param version          期望版本号
     * @return 影响行数
     */
    @Update("UPDATE mall_payment SET payment_status = " + STATUS_FAILED + ", "
            + "channel_pay_status = #{channelPayStatus}, "
            + "version = version + 1, update_time = NOW() "
            + "WHERE payment_no = #{paymentNo} AND payment_status = " + STATUS_UNPAID + " "
            + "AND version = #{version} AND is_deleted = 0")
    int markFailed(@Param("paymentNo") String paymentNo,
                   @Param("channelPayStatus") String channelPayStatus,
                   @Param("version") Integer version);

    /**
     * 支付超时关闭（CAS）
     *
     * @param paymentNo 支付单号
     * @param version   期望版本号
     * @return 影响行数
     */
    @Update("UPDATE mall_payment SET payment_status = " + STATUS_CLOSED + ", "
            + "version = version + 1, update_time = NOW() "
            + "WHERE payment_no = #{paymentNo} AND payment_status = " + STATUS_UNPAID + " "
            + "AND version = #{version} AND is_deleted = 0")
    int markClosed(@Param("paymentNo") String paymentNo, @Param("version") Integer version);

    /**
     * 发起退款：PAID → REFUNDING（CAS）
     *
     * @param paymentNo 支付单号
     * @param version   期望版本号
     * @return 影响行数
     */
    @Update("UPDATE mall_payment SET payment_status = " + STATUS_REFUNDING + ", "
            + "version = version + 1, update_time = NOW() "
            + "WHERE payment_no = #{paymentNo} AND payment_status = " + STATUS_PAID + " "
            + "AND version = #{version} AND is_deleted = 0")
    int markRefunding(@Param("paymentNo") String paymentNo, @Param("version") Integer version);

    /**
     * 退款成功：REFUNDING → REFUNDED（CAS）
     *
     * @param paymentNo 支付单号
     * @param version   期望版本号
     * @return 影响行数
     */
    @Update("UPDATE mall_payment SET payment_status = " + STATUS_REFUNDED + ", "
            + "version = version + 1, update_time = NOW() "
            + "WHERE payment_no = #{paymentNo} AND payment_status = " + STATUS_REFUNDING + " "
            + "AND version = #{version} AND is_deleted = 0")
    int markRefunded(@Param("paymentNo") String paymentNo, @Param("version") Integer version);

    /**
     * 退款失败回退：REFUNDING → PAID（CAS）
     *
     * <p>渠道明确退款失败时，支付单需退回 PAID，否则会永久卡在 REFUNDING，
     * 用户既拿不到钱也无法重新发起售后。</p>
     *
     * @param paymentNo 支付单号
     * @param version   期望版本号
     * @return 影响行数
     */
    @Update("UPDATE mall_payment SET payment_status = " + STATUS_PAID + ", "
            + "version = version + 1, update_time = NOW() "
            + "WHERE payment_no = #{paymentNo} AND payment_status = " + STATUS_REFUNDING + " "
            + "AND version = #{version} AND is_deleted = 0")
    int revertToPaid(@Param("paymentNo") String paymentNo, @Param("version") Integer version);

    /**
     * 回填渠道侧支付单号与通知地址
     *
     * <p>发起支付成功后立即调用，此时状态不应发生变化，故不做状态条件。</p>
     *
     * @param paymentNo        支付单号
     * @param channelPaymentNo 渠道侧支付单号
     * @param notifyUrl        异步通知地址
     * @return 影响行数
     */
    @Update("UPDATE mall_payment SET channel_payment_no = #{channelPaymentNo}, "
            + "notify_url = #{notifyUrl}, version = version + 1, update_time = NOW() "
            + "WHERE payment_no = #{paymentNo} AND is_deleted = 0")
    int updateChannelPaymentNo(@Param("paymentNo") String paymentNo,
                               @Param("channelPaymentNo") String channelPaymentNo,
                               @Param("notifyUrl") String notifyUrl);
}
