package com.mall.order.infrastructure.feign;

import com.mall.api.feign.RemoteMarketingService;
import com.mall.api.feign.RemoteMarketingService.CalculationReq;
import com.mall.api.feign.RemoteMarketingService.CalculationReq.CalculationItem;
import com.mall.api.feign.RemoteMarketingService.CalculationResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * mall-marketing Feign 适配器
 *
 * <p>刻意不降级：营销调用失败必须让下单流程感知，否则会用错误金额成交。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteMarketingAdapter {

    private final RemoteMarketingService remoteMarketingService;

    /**
     * 优惠试算（不锁定资源）
     *
     * @param userId         用户 ID
     * @param items          商品明细
     * @param couponRecordId 指定优惠券记录 ID，可空
     * @return 试算结果
     */
    public CalculationResp calculate(Long userId,
                                    List<CalculationItem> items,
                                    Long couponRecordId) {
        CalculationReq req = new CalculationReq();
        req.setUserId(userId);
        req.setItems(items);
        req.setCouponClaimId(couponRecordId);
        return remoteMarketingService.calculate(req);
    }

    /**
     * 锁定优惠券
     *
     * @param orderNo        订单号
     * @param couponRecordId 优惠券记录 ID
     * @return 锁定成功返回 true
     */
    public boolean lockCoupon(String orderNo, Long couponRecordId) {
        return remoteMarketingService.lockCoupon(orderNo, couponRecordId);
    }

    /**
     * 释放订单已锁定的全部优惠券（取消 / 下单失败补偿）
     *
     * @param orderNo 订单号
     */
    public void releaseCoupon(String orderNo) {
        remoteMarketingService.releaseCoupon(orderNo);
        log.info("releaseCoupon 已调用, orderNo={}", orderNo);
    }

    /**
     * 校验优惠券可用性
     *
     * @param couponRecordId 优惠券记录 ID
     * @param userId         用户 ID
     * @return 可用返回 true
     */
    public boolean validateCoupon(Long couponRecordId, Long userId) {
        return remoteMarketingService.validateCoupon(couponRecordId, userId);
    }
}