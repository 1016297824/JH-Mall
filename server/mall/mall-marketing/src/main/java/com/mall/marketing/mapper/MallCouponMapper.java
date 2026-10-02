package com.mall.marketing.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.marketing.DO.MallCouponDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 优惠券定义 Mapper
 *
 * <p>领券扣减走显式乐观锁 SQL（{@code version} CAS），不依赖 MyBatis-Plus 的
 * {@code @Version} 自动乐观锁（项目未注册 {@code OptimisticLockerInnerInterceptor}）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Mapper
public interface MallCouponMapper extends BaseMapper<MallCouponDO> {

    /** 券状态：已发布，与 {@code CouponStatusEnum.PUBLISHED} 保持一致 */
    int STATUS_PUBLISHED = 1;

    /**
     * 扣减剩余可领数量（乐观锁）
     *
     * <p>{@code remain_count>0} 与 {@code version} 双重条件同时满足才成功，
     * 返回 0 表示已被领完或版本冲突（调用方抛 A0611）。</p>
     *
     * @param id      券定义 ID
     * @param version 期望的版本号
     * @return 影响行数，0 表示扣减失败
     */
    @Update("UPDATE mall_marketing_coupon SET remain_count = remain_count - 1, version = version + 1, "
            + "update_time = NOW() WHERE id = #{id} AND version = #{version} "
            + "AND remain_count > 0 AND is_deleted = 0")
    int decreaseRemainCount(@Param("id") Long id, @Param("version") Integer version);

    /**
     * 回补剩余可领数量
     *
     * <p>订单取消/超时释放券时调用。此处是补偿动作，不再做乐观锁校验，
     * 幂等由调用方（`order_no + couponRecordId` 维度）保证。</p>
     *
     * @param id 券定义 ID
     * @return 影响行数
     */
    @Update("UPDATE mall_marketing_coupon SET remain_count = remain_count + 1, version = version + 1, "
            + "update_time = NOW() WHERE id = #{id} AND is_deleted = 0")
    int increaseRemainCount(@Param("id") Long id);

    /**
     * 查询可领取的优惠券
     *
     * <p>条件：已发布 + 未过有效期 + 仍有剩余量，按主键升序。</p>
     *
     * <p><b>与设计文档的差异</b>：设计 §3.1 要求「按 {@code sort_order} 排序」，
     * 但 {@code mall_marketing_coupon} 表<b>没有</b> {@code sort_order} 列
     * （该列只存在于活动表与规则表），故此处退化为按 {@code id} 升序。</p>
     *
     * @param now   当前时间，用于判断 {@code use_end_time}
     * @param limit 单次返回上限
     * @return 可领券列表
     */
    default List<MallCouponDO> selectAvailable(LocalDateTime now, int limit) {
        return selectList(new LambdaQueryWrapper<MallCouponDO>()
                .eq(MallCouponDO::getCouponStatus, STATUS_PUBLISHED)
                .gt(MallCouponDO::getUseEndTime, now)
                .gt(MallCouponDO::getRemainCount, 0)
                .eq(MallCouponDO::getIsDeleted, 0)
                .orderByAsc(MallCouponDO::getId)
                .last("LIMIT " + limit));
    }

    /**
     * 按主键查询未删除的券定义
     *
     * @param id 券定义 ID
     * @return 券定义，不存在返回 null
     */
    default MallCouponDO selectByIdNotDeleted(Long id) {
        return selectOne(new LambdaQueryWrapper<MallCouponDO>()
                .eq(MallCouponDO::getId, id)
                .eq(MallCouponDO::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    /**
     * 按 ID 集合批量查询未删除的券定义
     *
     * <p>「我的优惠券」列表批量补券名/类型用，避免逐条查库。</p>
     *
     * @param ids 券定义 ID 集合，空集合直接返回空列表
     * @return 券定义列表
     */
    default List<MallCouponDO> selectByIdsNotDeleted(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapper<MallCouponDO>()
                .in(MallCouponDO::getId, ids)
                .eq(MallCouponDO::getIsDeleted, 0));
    }

    /**
     * 仅更新券名称
     *
     * <p><b>为什么不用 {@code updateById}</b>：{@code updateById} 会把查询出来的整行
     * 字段一并写回，其中包含 {@code remain_count}。已发布的券随时可能被用户领取，
     * 若「管理端改名」与「用户领券」交叉执行，改名操作会用陈旧快照覆盖掉刚扣减的库存，
     * 造成超发。故字段级定向更新。</p>
     *
     * @param id         券定义 ID
     * @param couponName 新名称
     * @return 影响行数
     */
    @Update("UPDATE mall_marketing_coupon SET coupon_name = #{couponName}, update_time = NOW() "
            + "WHERE id = #{id} AND is_deleted = 0")
    int updateNameById(@Param("id") Long id, @Param("couponName") String couponName);

    /**
     * 仅更新券状态
     *
     * <p>理由同 {@link #updateNameById}：避免把整行快照写回而覆盖 {@code remain_count}。
     * 用于「已发布的券置为废弃」（草稿态才走物理删除）。</p>
     *
     * @param id           券定义 ID
     * @param couponStatus 新状态，取值见 {@code CouponStatusEnum}
     * @return 影响行数
     */
    @Update("UPDATE mall_marketing_coupon SET coupon_status = #{couponStatus}, update_time = NOW() "
            + "WHERE id = #{id} AND is_deleted = 0")
    int updateStatusById(@Param("id") Long id, @Param("couponStatus") Integer couponStatus);
}
