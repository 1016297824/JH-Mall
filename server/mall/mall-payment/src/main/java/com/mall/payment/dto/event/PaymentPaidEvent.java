package com.mall.payment.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付成功事件 Payload
 *
 * <p>对应 Topic {@code mall:payment:paid}（设计文档 §8.1）。
 * mall-order 消费后推进订单 {@code WAIT_PAY → PAID}（由订单状态机保证幂等）。</p>
 *
 * <p>刻意不复用 {@code MallPaymentDO}：DO 含 {@code version}、{@code isDeleted}、
 * {@code idempotentKey} 等内部字段，一旦被序列化进消息就构成对外契约，
 * 后续 DO 改动会静默改变消息格式。</p>
 *
 * <p><b>时间字段为 ISO-8601 字符串</b>：设计文档 §8.3 的报文样例即
 * {@code "payTime": "2026-05-17T10:30:00"}；且 Outbox 用裸 {@code ObjectMapper}
 * 序列化 payload，未注册 JavaTimeModule，放 {@code LocalDateTime} 会在写 Outbox 时
 * 直接抛 {@code InvalidDefinitionException}，导致回调整体失败。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentPaidEvent {

    /** 支付单号 */
    private String paymentNo;

    /** 订单号 */
    private String orderNo;

    /** 付款用户 ID */
    private Long userId;

    /** 实付金额（单位：分） */
    private Long payAmount;

    /** 支付成功时间，ISO-8601 字符串（如 2026-05-17T10:30:00） */
    private String payTime;

    /** 渠道侧支付单号（对账用） */
    private String channelPaymentNo;

    /** 支付渠道编码 */
    private String channelCode;
}
