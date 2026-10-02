package com.mall.marketing.service;

import com.mall.marketing.dto.response.CouponRecordResp;

import java.util.List;

/**
 * 用户优惠券服务
 *
 * <p>覆盖券记录从领取到核销/释放的全生命周期。设计依据：
 * {@code docs/design/14_mall-marketing详细设计.md} §3.2 / §4 / §5 / §6。</p>
 *
 * <p><b>状态变更约束</b>：所有 {@code record_status} 变更必须经
 * {@code CouponStateMachine.transition()} 判定，本服务只负责补偿与落库。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
public interface CouponClaimService {

    /**
     * 领取优惠券
     *
     * <p>流程：校验券可领 → 校验每人限领 → 乐观锁扣减库存 → 落券记录。</p>
     *
     * @param userId      用户 ID
     * @param couponDefId 优惠券定义 ID
     * @return 新建的券记录 ID
     */
    Long claimCoupon(Long userId, Long couponDefId);

    /**
     * 锁定优惠券（下单时，由 mall-order 经 Feign 调用）
     *
     * <p>把券记录由 {@code AVAILABLE} 置为 {@code LOCKED}。返回 {@code false} 表示
     * 锁券不成功（券不存在/已占用/已过期），由调用方转成业务错误，<b>不抛异常</b>——
     * 契约 {@code RemoteMarketingService.lockCoupon} 的返回类型是 {@code boolean}。</p>
     *
     * @param couponClaimId 券记录 ID
     * @param orderNo       订单号
     * @return 锁定成功返回 true
     */
    boolean lockCoupon(Long couponClaimId, String orderNo);

    /**
     * 核销优惠券（订单支付成功，由 {@code mall:order:paid} 触发）
     *
     * <p>把该订单下所有 {@code LOCKED} 的券置为 {@code USED}。幂等：无可核销记录时直接返回。</p>
     *
     * @param orderNo 订单号
     */
    void useCoupon(String orderNo);

    /**
     * 释放优惠券（订单取消/超时关闭，由 {@code mall:order:cancelled} 触发）
     *
     * <p>把该订单下所有 {@code LOCKED} 的券置为 {@code RELEASED}，并回补券定义的
     * {@code remain_count}。幂等：已释放的券不重复回补库存。</p>
     *
     * @param orderNo 订单号
     */
    void releaseCoupon(String orderNo);

    /**
     * 查询用户的券记录（我的优惠券）
     *
     * @param userId       用户 ID
     * @param recordStatus 记录状态，null 表示全部
     * @param limit        单次返回上限
     * @return 券记录列表，按领取时间倒序
     */
    List<CouponRecordResp> listMyCoupons(Long userId, Integer recordStatus, int limit);

    /**
     * 批量置过期（定时任务调用）
     *
     * @return 本次置为 EXPIRED 的记录数
     */
    int expireCoupons();

    /**
     * 校验优惠券可用性（含<b>归属校验</b>）
     *
     * <p><b>安全约束</b>：本方法是「这张券是不是你的」的唯一权威判定点。
     * 契约 {@code RemoteMarketingService.lockCoupon(orderNo, couponClaimId)}
     * <b>没有 userId 参数</b>，营销侧无法在锁券时校验归属，因此归属校验必须在
     * 进入锁券之前完成——即由本方法与优惠试算共同把关。</p>
     *
     * @param couponClaimId 券记录 ID
     * @param userId        用户 ID
     * @return 券可用（归属正确 + 状态可用 + 未过期）返回 true
     * @throws com.mall.common.exception.BusinessException A0501
     *         券不存在，或不属于该用户（两种情况返回同一错误码，避免泄露归属信息）
     */
    boolean validateCoupon(Long couponClaimId, Long userId);
}
