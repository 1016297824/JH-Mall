package com.mall.marketing.controller;

import static com.mall.common.constant.HeaderConstants.X_USER_ID;

import com.mall.api.feign.RemoteMarketingService.CalculationReq;
import com.mall.api.feign.RemoteMarketingService.CalculationResp;
import com.mall.common.DTO.MallResult;
import com.mall.marketing.dto.request.CalculationRequest;
import com.mall.marketing.service.CalculationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端优惠试算控制器
 *
 * <p>对应设计文档 {@code docs/design/14_mall-marketing详细设计.md} §2.2 端点 5（需登录）。</p>
 *
 * <p>响应直接复用契约 DTO {@code CalculationResp}：其字段（原价 / 券优惠 / 促销优惠 / 应付 /
 * 命中明细）正是前端下单前需要展示的全部内容，其中 {@code appliedCoupons[].couponRecordId}
 * 还需回传给下单接口。另建平行 VO 只会带来两处漂移风险。</p>
 *
 * <p><b>安全约束</b>：请求 DTO {@link CalculationRequest} 刻意不含 {@code userId}，
 * 身份只能由网关注入的 {@code X-User-Id} 请求头决定，再从请求头取值拼装内部契约
 * {@link CalculationReq}，天然杜绝请求体伪造身份越权用他人券。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@RestController
@RequestMapping("/api/marketing/calculations")
@RequiredArgsConstructor
public class CalculationController {

    private final CalculationService calculationService;

    /**
     * 优惠试算（不锁定任何资源）
     *
     * @param req     试算请求（<b>不含 userId</b>，身份取自请求头）
     * @param request HTTP 请求（取 X-User-Id）
     * @return 试算结果
     */
    @PostMapping
    public MallResult<CalculationResp> calculate(@RequestBody CalculationRequest req,
                                                 HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader(X_USER_ID));
        CalculationReq calculationReq = new CalculationReq();
        calculationReq.setUserId(userId);
        calculationReq.setItems(req.getItems());
        calculationReq.setCouponClaimId(req.getCouponClaimId());
        return MallResult.success(calculationService.calculate(calculationReq));
    }
}
