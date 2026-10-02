package com.mall.payment.controller;

import static com.mall.common.constant.HeaderConstants.X_USER_ID;

import com.mall.common.DTO.MallResult;
import com.mall.payment.dto.request.PayRequestDTO;
import com.mall.payment.dto.response.PayResultDTO;
import com.mall.payment.service.PaymentService;
import com.mall.payment.vo.PaymentVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端支付控制器
 *
 * <p>对应设计文档 {@code docs/design/13_mall-payment详细设计.md} §2.2 端点 1、2（均需登录）。</p>
 *
 * <p><b>安全约束</b>：付款用户身份只能来自网关注入的 {@code X-User-Id} 请求头。
 * 请求体 {@link PayRequestDTO} 刻意不含 {@code userId}，避免客户端伪造身份
 * 去支付他人订单 —— 服务层会校验订单归属，但前提是身份本身可信。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Slf4j
@RestController
@RequestMapping("/api/payment/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 发起支付
     *
     * @param req     发起支付请求（<b>不含 userId</b>，身份取自请求头）
     * @param request HTTP 请求（取 X-User-Id）
     * @return 支付单号与前端调起参数
     */
    @PostMapping
    public MallResult<PayResultDTO> createPayment(@RequestBody PayRequestDTO req,
                                                  HttpServletRequest request) {
        Long userId = currentUserId(request);
        return MallResult.success(paymentService.createPayment(userId, req));
    }

    /**
     * 查询支付单
     *
     * @param paymentId 支付单主键 ID
     * @param request   HTTP 请求（取 X-User-Id）
     * @return 支付单视图
     */
    @GetMapping("/{paymentId}")
    public MallResult<PaymentVO> getPayment(@PathVariable("paymentId") Long paymentId,
                                            HttpServletRequest request) {
        Long userId = currentUserId(request);
        return MallResult.success(paymentService.getPayment(userId, paymentId));
    }

    /**
     * 从请求头解析当前用户 ID
     *
     * @param request HTTP 请求
     * @return 用户 ID
     */
    private Long currentUserId(HttpServletRequest request) {
        return Long.parseLong(request.getHeader(X_USER_ID));
    }
}
