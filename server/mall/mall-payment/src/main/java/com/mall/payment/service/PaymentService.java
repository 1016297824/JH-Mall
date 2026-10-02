package com.mall.payment.service;

import com.mall.payment.dto.request.PayRequestDTO;
import com.mall.payment.dto.response.PayResultDTO;
import com.mall.payment.vo.PaymentVO;

/**
 * 支付服务
 *
 * <p>对应设计文档 {@code docs/design/13_mall-payment详细设计.md} §3.2 与 §4。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
public interface PaymentService {

    /**
     * 发起支付
     *
     * <p>幂等语义：同一 {@code userId + orderNo + channelCode} 重复发起不会新建支付单。</p>
     *
     * @param userId 付款用户 ID（来自网关透传的 X-User-Id，不由请求体提供）
     * @param req    发起支付请求
     * @return 支付单号与前端调起参数
     */
    PayResultDTO createPayment(Long userId, PayRequestDTO req);

    /**
     * 查询支付单
     *
     * <p>含归属校验：只能查询本人的支付单。</p>
     *
     * @param userId    当前用户 ID
     * @param paymentId 支付单主键 ID
     * @return 支付单视图
     */
    PaymentVO getPayment(Long userId, Long paymentId);
}
