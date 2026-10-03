package com.mall.api.feign;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * C 端支付服务 Feign 接口
 *
 * <p>提供给 mall-order 调用（售后审核通过后发起退款）</p>
 *
 * <p>金额字段一律为 {@code Long}，单位<strong>分</strong>。</p>
 *
 * <p>对应设计文档 {@code docs/design/07_mall-api契约层设计.md} §3.5</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@FeignClient(contextId = "mall-payment", value = "mall-payment")
public interface RemotePaymentService {

    /**
     * 创建退款单并调支付渠道
     *
     * <p>提供方保证幂等：{@code idempotentKey = afterSaleNo + ":" + channelCode}
     * （DB 唯一约束），重复调用返回已有退款单。</p>
     *
     * <p>渠道侧结果分三种：{@code SUCCESS} 立即终态、{@code PROCESSING} 等回调、
     * {@code FAIL} 需人工介入。均通过 {@link RefundResultDTO#getRefundStatus()} 返回。</p>
     *
     * @param refundDTO 退款请求
     * @return 退款结果（含退款单号）
     */
    @PostMapping("/inner/payment/refunds")
    RefundResultDTO createRefund(@RequestBody RefundDTO refundDTO);

    /**
     * 售后审核通过后发起退款
     *
     * <p><b>与设计文档 §3.5 的差异</b>：文档写的是 {@code refund(payOrderNo, ...)}，
     * 但支付单号只存在于 mall-payment 自己的 {@code mall_payment} 表，
     * mall-order 无法获取。因此这里传 {@code orderNo}，
     * 由 mall-payment 内部完成 orderNo → paymentNo 的解析。</p>
     *
     * @param orderNo      订单号
     * @param refundAmount 退款金额（单位：分）
     * @param afterSaleNo  售后单<b>业务单号</b>（非主键 id）——回调会原样带回，
     *                     mall-order 需据此按业务单号查回售后单
     * @return 退款结果（含退款单号）
     */
    @PostMapping("/inner/payment/refunds/by-after-sale")
    RefundResultDTO refundByOrderNo(@RequestParam("orderNo") String orderNo,
                                    @RequestParam("refundAmount") Long refundAmount,
                                    @RequestParam("afterSaleNo") String afterSaleNo);

    /**
     * 查询支付单当前状态
     *
     * <p>供 mall-order 售后校验：仅支付成功且未全额退款的订单才可退款。</p>
     *
     * @param payOrderNo 支付单号
     * @return 支付状态快照，不存在返回 null
     */
    @GetMapping("/inner/payment/status")
    PaymentStatusDTO getPaymentStatus(@RequestParam("payOrderNo") String payOrderNo);

    /**
     * 退款请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class RefundDTO {

        /** 支付单号（关联原支付） */
        private String paymentNo;

        /** 退款金额（单位：分） */
        private Long refundAmount;

        /** 售后单号，用作幂等键组成部分 */
        private String afterSaleNo;

        /** 支付渠道编码，必须与原始支付渠道一致 */
        private String channelCode;
    }

    /**
     * 退款结果
     *
     * <p>{@code refundStatus} 取值见 {@code mall-common} 的 {@code RefundStatusEnum}。</p>
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class RefundResultDTO {

        /** 退款单号（格式 REF + 时间戳 + 随机数），对账用 */
        private String refundNo;

        /** 退款状态，取值见 RefundStatusEnum */
        private Integer refundStatus;

        /** 渠道侧退款单号 */
        private String channelRefundNo;
    }

    /**
     * 支付状态快照
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class PaymentStatusDTO {

        /** 支付单号 */
        private String paymentNo;

        /** 订单号 */
        private String orderNo;

        /** 支付状态，取值见 {@code mall-common} 的 {@code PaymentStatusEnum} */
        private Integer paymentStatus;

        /** 实付金额（单位：分） */
        private Long payAmount;

        /** 累计已退款金额（单位：分） */
        private Long refundedAmount;
    }
}