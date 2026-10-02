package com.mall.marketing.controller.inner;

import com.mall.api.feign.RemoteMarketingService.CalculationReq;
import com.mall.api.feign.RemoteMarketingService.CalculationResp;
import com.mall.marketing.service.CalculationService;
import com.mall.marketing.service.CouponClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 营销内部 Feign 端点
 *
 * <p>路径 {@code /inner/marketing/**} 仅供服务间调用，不可从网关外部直达，
 * 由 {@code mall-api} 的 {@code InnerSignatureFilter} 校验 {@code X-Internal-*} 签名头。</p>
 *
 * <p><b>路径必须与契约 {@code RemoteMarketingService} 完全一致</b>，否则 Feign 调用 404：
 * {@code /inner/marketing/calculate}、{@code /inner/marketing/coupon/lock}、
 * {@code /inner/marketing/coupon/release}、{@code /inner/marketing/coupon/validate}。</p>
 *
 * <p><b>返回裸对象而非 {@code MallResult}</b>：契约声明的是 {@code CalculationResp} /
 * {@code boolean}，包一层会导致 Feign 反序列化失败。与 {@code RemoteOrderInnerController} 保持一致。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@RestController
@RequestMapping("/inner/marketing")
@RequiredArgsConstructor
public class RemoteMarketingInnerController {

    private final CalculationService calculationService;

    private final CouponClaimService couponClaimService;

    /**
     * 优惠试算（供 mall-order 下单前调用）
     *
     * @param req 试算请求（含 userId，由调用方从下单用户传入）
     * @return 试算结果
     */
    @PostMapping("/calculate")
    public CalculationResp calculate(@RequestBody CalculationReq req) {
        return calculationService.calculate(req);
    }

    /**
     * 锁定优惠券（供 mall-order 下单时调用）
     *
     * @param orderNo       订单号
     * @param couponClaimId 券记录 ID
     * @return 锁定成功返回 true
     */
    @PostMapping("/coupon/lock")
    public boolean lockCoupon(@RequestParam("orderNo") String orderNo,
                              @RequestParam("couponClaimId") Long couponClaimId) {
        return couponClaimService.lockCoupon(couponClaimId, orderNo);
    }

    /**
     * 释放订单已锁定的全部优惠券（供 mall-order 取消/补偿时调用）
     *
     * @param orderNo 订单号
     */
    @PostMapping("/coupon/release")
    public void releaseCoupon(@RequestParam("orderNo") String orderNo) {
        couponClaimService.releaseCoupon(orderNo);
    }

    /**
     * 校验优惠券可用性（含归属校验）
     *
     * <p><b>语义说明</b>：券不存在或不属于该用户时，底层会抛 {@code A0501}（两种情况同码，
     * 不泄露归属信息），异常经统一处理器转为业务错误响应；其余「不可用」情形返回 {@code false}。</p>
     *
     * @param couponClaimId 券记录 ID
     * @param userId        用户 ID
     * @return 可用返回 true
     */
    @GetMapping("/coupon/validate")
    public boolean validateCoupon(@RequestParam("couponClaimId") Long couponClaimId,
                                  @RequestParam("userId") Long userId) {
        return couponClaimService.validateCoupon(couponClaimId, userId);
    }
}
