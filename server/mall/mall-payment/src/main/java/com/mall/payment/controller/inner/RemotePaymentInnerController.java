package com.mall.payment.controller.inner;

import com.mall.api.feign.RemotePaymentService.PaymentStatusDTO;
import com.mall.api.feign.RemotePaymentService.RefundDTO;
import com.mall.api.feign.RemotePaymentService.RefundResultDTO;
import com.mall.payment.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付模块内部端点（仅供服务间 Feign 调用）
 *
 * <p>对应契约 {@code com.mall.api.feign.RemotePaymentService}，
 * <b>路径必须与契约字符级一致</b>，否则调用方得到 404。</p>
 *
 * <p><b>返回裸对象而非 {@code MallResult}</b>：契约方法签名直接是 DTO，
 * 包一层会让 Feign 反序列化失败。两者不一致是刻意的 —— C 端面向浏览器，
 * 需要统一响应体；内部端点面向服务，直返数据。</p>
 *
 * <p>本控制器不做用户身份校验：调用方是 mall-order，属于服务间信任边界，
 * 其入参由售后单推导而来。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Slf4j
@RestController
@RequestMapping("/inner/payment")
@RequiredArgsConstructor
public class RemotePaymentInnerController {

    private final RefundService refundService;

    /**
     * 创建退款单并调支付渠道
     *
     * @param refundDTO 退款请求（含支付单号、金额、售后单号、渠道编码）
     * @return 退款结果（含退款单号）
     */
    @PostMapping("/refunds")
    public RefundResultDTO createRefund(@RequestBody RefundDTO refundDTO) {
        return refundService.createRefund(refundDTO);
    }

    /**
     * 售后审核通过后按订单号发起退款
     *
     * <p>mall-order 侧只持有 {@code orderNo}，支付单号由本模块内部解析。</p>
     *
     * @param orderNo      订单号
     * @param refundAmount 退款金额（单位：分）
     * @param afterSaleNo  售后单业务单号（非主键 id）
     * @return 退款结果（含退款单号）
     */
    @PostMapping("/refunds/by-after-sale")
    public RefundResultDTO refundByOrderNo(@RequestParam("orderNo") String orderNo,
                                           @RequestParam("refundAmount") Long refundAmount,
                                           @RequestParam("afterSaleNo") String afterSaleNo) {
        return refundService.refundByOrderNo(orderNo, refundAmount, afterSaleNo);
    }

    /**
     * 查询支付单当前状态
     *
     * <p>供 mall-order 售后校验：仅支付成功且未全额退款的订单才可退款。</p>
     *
     * @param payOrderNo 支付单号
     * @return 支付状态快照，不存在返回 {@code null}
     */
    @GetMapping("/status")
    public PaymentStatusDTO getPaymentStatus(@RequestParam("payOrderNo") String payOrderNo) {
        return refundService.getPaymentStatus(payOrderNo);
    }
}
