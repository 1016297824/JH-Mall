package com.mall.order.infrastructure.feign;

import com.mall.api.feign.RemotePaymentService;
import com.mall.api.feign.RemotePaymentService.PaymentStatusDTO;
import com.mall.api.feign.RemotePaymentService.RefundResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * mall-payment Feign 适配器
 *
 * <p>退款属于写操作，<b>刻意不降级</b>：失败必须让售后流程感知，
 * 否则会出现「售后单已通过但退款没发起」的资金损失。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemotePaymentAdapter {

    private final RemotePaymentService remotePaymentService;

    /**
     * 售后审核通过后发起退款
     *
     * <p>传订单号而非支付单号——支付单号只有 mall-payment 自己知道
     * （{@code mall_payment} 表），由它内部做 orderNo → paymentNo 解析。</p>
     *
     * @param orderNo      订单号
     * @param refundAmount 退款金额（单位：分）
     * @param afterSaleId  售后单 ID
     * @return 退款结果（含退款单号）
     */
    public RefundResultDTO refundByOrderNo(String orderNo, Long refundAmount, Long afterSaleId) {
        RefundResultDTO result = remotePaymentService.refundByOrderNo(orderNo, refundAmount, afterSaleId);
        log.info("发起退款: orderNo={}, refundAmount={}, afterSaleId={}, refundNo={}",
                orderNo, refundAmount, afterSaleId, result == null ? null : result.getRefundNo());
        return result;
    }

    /**
     * 查询支付单状态（售后提交时校验是否可退）
     *
     * @param payOrderNo 支付单号
     * @return 支付状态，不存在返回 null
     */
    public PaymentStatusDTO getPaymentStatus(String payOrderNo) {
        return remotePaymentService.getPaymentStatus(payOrderNo);
    }
}