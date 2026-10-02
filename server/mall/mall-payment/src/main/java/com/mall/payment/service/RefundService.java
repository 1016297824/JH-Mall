package com.mall.payment.service;

import com.mall.api.feign.RemotePaymentService.PaymentStatusDTO;
import com.mall.api.feign.RemotePaymentService.RefundDTO;
import com.mall.api.feign.RemotePaymentService.RefundResultDTO;

/**
 * 退款服务
 *
 * <p>对应设计文档 {@code docs/design/13_mall-payment详细设计.md} §3.3 与 §6。</p>
 *
 * <p><b>两个入口的差别</b>：mall-order 售后审核通过时只持有 {@code orderNo}
 * （支付单号只存在于 mall-payment 自己的表里），故主用
 * {@link #refundByOrderNo}；已知支付单号的场景（内部 / 管理端）用 {@link #createRefund}。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
public interface RefundService {

    /**
     * 创建退款单并调渠道退款
     *
     * <p>幂等语义：{@code idempotent_key = afterSaleNo + "_" + channelCode}（DB 唯一约束），
     * 重复调用返回已有退款单，不重复扣款。</p>
     *
     * @param refundDTO 退款请求（含支付单号与渠道编码）
     * @return 退款结果（含退款单号与退款状态）
     */
    RefundResultDTO createRefund(RefundDTO refundDTO);

    /**
     * 售后审核通过后按订单号发起退款
     *
     * <p>内部先完成 {@code orderNo → paymentNo} 解析，再复用 {@link #createRefund} 主流程。</p>
     *
     * @param orderNo      订单号
     * @param refundAmount 退款金额（单位：分）
     * @param afterSaleId  售后单 ID
     * @return 退款结果（含退款单号与退款状态）
     */
    RefundResultDTO refundByOrderNo(String orderNo, Long refundAmount, Long afterSaleId);

    /**
     * 查询支付单的可退款状态快照
     *
     * <p>供 mall-order 售后校验使用：仅「支付成功且未全额退款」的订单才允许发起退款。
     * 返回的 {@code refundedAmount} 为累计已退款金额（<b>含处理中</b>），
     * 与 {@link #createRefund} 的超额校验口径一致，避免并发退款时各自看到「未超额」。</p>
     *
     * @param paymentNo 支付单号
     * @return 支付状态快照，支付单不存在返回 {@code null}
     */
    PaymentStatusDTO getPaymentStatus(String paymentNo);
}
