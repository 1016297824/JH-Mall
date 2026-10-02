package com.mall.marketing.controller;

import static com.mall.common.constant.HeaderConstants.X_USER_ID;

import com.mall.common.DTO.MallResult;
import com.mall.marketing.dto.response.CouponDefResp;
import com.mall.marketing.dto.response.CouponRecordResp;
import com.mall.marketing.service.CouponClaimService;
import com.mall.marketing.service.CouponDefService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端优惠券控制器
 *
 * <p>对应设计文档 {@code docs/design/14_mall-marketing详细设计.md} §2.2 端点 1~3。
 * 其中「可领券列表」无需登录，其余两个需登录。</p>
 *
 * <p>需登录的端点：用户身份一律取自网关注入的 {@code X-User-Id} 请求头，
 * 绝不从请求体读取，避免越权。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@RestController
@RequestMapping("/api/marketing/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponDefService couponDefService;

    private final CouponClaimService couponClaimService;

    /** 单次返回上限的下界，非正数兜底为该值 */
    private static final int MIN_LIMIT = 1;

    /** 单次返回上限的上界，超过则截断为该值 */
    private static final int MAX_LIMIT = 50;

    /**
     * 查询可领取的优惠券列表（无需登录）
     *
     * @param limit 单次返回上限，归一化到 {@code [1, 50]} 后透传
     * @return 可领券列表
     */
    @GetMapping
    public MallResult<List<CouponDefResp>> listAvailableCoupons(
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return MallResult.success(couponDefService.listAvailableCoupons(normalizeLimit(limit)));
    }

    /**
     * 领取优惠券
     *
     * @param couponDefId 优惠券定义 ID
     * @param request     HTTP 请求（取 X-User-Id）
     * @return 新建的券记录 ID
     */
    @PostMapping("/{couponDefId}/claims")
    public MallResult<Long> claimCoupon(@PathVariable("couponDefId") Long couponDefId,
                                        HttpServletRequest request) {
        Long userId = currentUserId(request);
        return MallResult.success(couponClaimService.claimCoupon(userId, couponDefId));
    }

    /**
     * 查询我的优惠券
     *
     * @param status  记录状态过滤，可空
     * @param limit   单次返回上限，归一化到 {@code [1, 50]} 后透传
     * @param request HTTP 请求（取 X-User-Id）
     * @return 券记录列表
     */
    @GetMapping("/claims")
    public MallResult<List<CouponRecordResp>> listMyCoupons(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            HttpServletRequest request) {
        Long userId = currentUserId(request);
        return MallResult.success(couponClaimService.listMyCoupons(userId, status, normalizeLimit(limit)));
    }

    /**
     * 归一化单次返回上限
     *
     * <p>下界 {@value #MIN_LIMIT}、上界 {@value #MAX_LIMIT}：小于下界的兜底为 1，
     * 超过上界的截断为 50，其余原样透传。</p>
     *
     * @param limit 原始 limit 参数
     * @return 落在 {@code [1, 50]} 区间内的 limit
     */
    private int normalizeLimit(int limit) {
        if (limit < MIN_LIMIT) {
            return MIN_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * 从请求头 {@code X-User-Id} 取当前登录用户 ID
     *
     * @param request HTTP 请求
     * @return 当前用户 ID
     */
    private Long currentUserId(HttpServletRequest request) {
        return Long.parseLong(request.getHeader(X_USER_ID));
    }
}
