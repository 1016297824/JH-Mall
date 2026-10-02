package com.mall.marketing.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.marketing.DO.MallCouponRecordDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户优惠券记录 Mapper
 *
 * <p>状态字段的变更一律走 {@code CouponStateMachine} 判定，本接口只负责 CAS 落库：
 * 每条 UPDATE 都带原状态条件，返回 0 行即代表竞态失败，由 Service 层抛错。</p>
 *
 * <p><b>状态码常量</b>：注解中的 SQL 需要编译期字面量，故此处以 {@code int} 常量声明，
 * 取值必须与 {@code CouponRecordStatusEnum} 保持一致。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Mapper
public interface MallCouponRecordMapper extends BaseMapper<MallCouponRecordDO> {

    /** 记录状态：可用（CouponRecordStatusEnum.AVAILABLE） */
    int STATUS_AVAILABLE = 1;

    /** 记录状态：已锁定（CouponRecordStatusEnum.LOCKED） */
    int STATUS_LOCKED = 2;

    /** 记录状态：已使用（CouponRecordStatusEnum.USED） */
    int STATUS_USED = 3;

    /** 记录状态：已释放（CouponRecordStatusEnum.RELEASED） */
    int STATUS_RELEASED = 4;

    /** 记录状态：已过期（CouponRecordStatusEnum.EXPIRED） */
    int STATUS_EXPIRED = 5;

    /**
     * 锁定优惠券（AVAILABLE → LOCKED）
     *
     * <p>CAS 条件带原状态 {@code record_status = 1}，并发下只有一个请求能成功。</p>
     *
     * @param id      券记录 ID
     * @param orderNo 锁定到的订单号
     * @return 影响行数，0 表示券不处于可用态（调用方抛 A0612）
     */
    @Update("UPDATE mall_marketing_coupon_record SET record_status = " + STATUS_LOCKED + ", "
            + "order_no = #{orderNo}, lock_time = NOW(), update_time = NOW() "
            + "WHERE id = #{id} AND record_status = " + STATUS_AVAILABLE + " AND is_deleted = 0")
    int lockById(@Param("id") Long id, @Param("orderNo") String orderNo);

    /**
     * 核销优惠券（LOCKED → USED）
     *
     * @param id      券记录 ID
     * @param orderNo 订单号，作为归属校验
     * @return 影响行数，0 表示竞态失败
     */
    @Update("UPDATE mall_marketing_coupon_record SET record_status = " + STATUS_USED + ", "
            + "use_time = NOW(), update_time = NOW() "
            + "WHERE id = #{id} AND order_no = #{orderNo} "
            + "AND record_status = " + STATUS_LOCKED + " AND is_deleted = 0")
    int markUsedById(@Param("id") Long id, @Param("orderNo") String orderNo);

    /**
     * 释放优惠券（LOCKED → RELEASED）
     *
     * <p>清除 {@code order_no} 并记录释放时间；库存回补由 Service 层另行调用
     * {@code MallCouponMapper.increaseRemainCount}。</p>
     *
     * @param id      券记录 ID
     * @param orderNo 订单号，作为归属校验
     * @return 影响行数，0 表示已释放或竞态失败（幂等场景可忽略）
     */
    @Update("UPDATE mall_marketing_coupon_record SET record_status = " + STATUS_RELEASED + ", "
            + "order_no = NULL, release_time = NOW(), update_time = NOW() "
            + "WHERE id = #{id} AND order_no = #{orderNo} "
            + "AND record_status = " + STATUS_LOCKED + " AND is_deleted = 0")
    int releaseById(@Param("id") Long id, @Param("orderNo") String orderNo);

    /**
     * 批量置过期（AVAILABLE / RELEASED → EXPIRED）
     *
     * <p>定时任务扫描 {@code expire_time < NOW()}，单次上限由配置控制（设计 §6）。
     * 该操作不涉及库存回补——未领到的券仍在 {@code remain_count} 里。</p>
     *
     * @param batchSize 单次处理上限
     * @return 影响行数
     */
    @Update("UPDATE mall_marketing_coupon_record SET record_status = " + STATUS_EXPIRED + ", "
            + "update_time = NOW() WHERE record_status IN (" + STATUS_AVAILABLE + ", " + STATUS_RELEASED + ") "
            + "AND expire_time < NOW() AND is_deleted = 0 LIMIT #{batchSize}")
    int expireBatch(@Param("batchSize") int batchSize);

    /**
     * 统计用户对某张券的已领数量（每人限领校验）
     *
     * @param userId   用户 ID
     * @param couponId 券定义 ID
     * @return 已领数量
     */
    default long countClaimed(Long userId, Long couponId) {
        return selectCount(new LambdaQueryWrapper<MallCouponRecordDO>()
                .eq(MallCouponRecordDO::getUserId, userId)
                .eq(MallCouponRecordDO::getCouponId, couponId)
                .eq(MallCouponRecordDO::getIsDeleted, 0));
    }

    /**
     * 按订单号 + 状态查询券记录
     *
     * @param orderNo 订单号
     * @param status  记录状态
     * @return 券记录列表
     */
    default List<MallCouponRecordDO> selectByOrderNoAndStatus(String orderNo, int status) {
        return selectList(new LambdaQueryWrapper<MallCouponRecordDO>()
                .eq(MallCouponRecordDO::getOrderNo, orderNo)
                .eq(MallCouponRecordDO::getRecordStatus, status)
                .eq(MallCouponRecordDO::getIsDeleted, 0)
                .orderByAsc(MallCouponRecordDO::getId));
    }

    /**
     * 查询用户的券记录（我的优惠券）
     *
     * @param userId       用户 ID
     * @param recordStatus 记录状态，null 表示全部
     * @param limit        单次返回上限
     * @return 券记录列表，按领取时间倒序
     */
    default List<MallCouponRecordDO> selectUserRecords(Long userId, Integer recordStatus, int limit) {
        return selectList(new LambdaQueryWrapper<MallCouponRecordDO>()
                .eq(MallCouponRecordDO::getUserId, userId)
                .eq(recordStatus != null, MallCouponRecordDO::getRecordStatus, recordStatus)
                .eq(MallCouponRecordDO::getIsDeleted, 0)
                .orderByDesc(MallCouponRecordDO::getCreateTime)
                .last("LIMIT " + limit));
    }

    /**
     * 按主键查询未删除的券记录
     *
     * @param id 券记录 ID
     * @return 券记录，不存在返回 null
     */
    default MallCouponRecordDO selectByIdNotDeleted(Long id) {
        return selectOne(new LambdaQueryWrapper<MallCouponRecordDO>()
                .eq(MallCouponRecordDO::getId, id)
                .eq(MallCouponRecordDO::getIsDeleted, 0)
                .last("LIMIT 1"));
    }
}
