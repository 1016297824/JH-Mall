package com.mall.marketing.infrastructure.schedule;

import com.mall.marketing.service.CouponClaimService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 优惠券过期定时任务
 *
 * <p>按 {@code mall.marketing.coupon.expire-scan-interval}（单位<strong>秒</strong>，默认 3600）
 * 周期性把 {@code record_status IN (AVAILABLE, RELEASED) AND expire_time < NOW()}
 * 的券批量置为 EXPIRED（设计文档 §6）。</p>
 *
 * <p><b>关于扫描间隔的写法</b>：{@code @Scheduled} 的 {@code fixedDelay} 单位是<strong>毫秒</strong>，
 * 而配置项单位是<strong>秒</strong>，故在占位符后拼接 {@code "000"} 完成换算——
 * 这样既不必改配置语义，也避免误把 3600 当毫秒（那会变成 3.6 秒扫一次）。
 * 注意该换算仅适用于整数秒配置。</p>
 *
 * <p><b>为何不做库存回补</b>：过期的是「已领出但没用掉」的券，配额在领取时已扣减，
 * 过期不返还（设计文档 §6 仅置状态，无回补动作）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponExpireTask {

    private final CouponClaimService couponClaimService;

    /**
     * 批量置过期
     *
     * <p>单次处理上限由 {@code expire-batch-size}（默认 500）在 Service 层控制；
     * 异常在此吞掉并记录，避免调度线程被中断导致后续周期不再执行。</p>
     */
    @Scheduled(fixedDelayString = "${mall.marketing.coupon.expire-scan-interval:3600}000")
    public void expireCoupons() {
        try {
            int affected = couponClaimService.expireCoupons();
            log.info("优惠券过期扫描完成: 置过期 {} 条", affected);
        } catch (Exception e) {
            log.error("优惠券过期扫描失败", e);
        }
    }
}
