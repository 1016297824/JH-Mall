package com.mall.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 渠道回调处理结果
 *
 * <p>注意：<b>验签失败不抛业务异常</b> —— 设计 §12 明确「回调验签失败 → HTTP 400（无业务错误码）」，
 * 且支付平台只看 HTTP 状态码与应答体，不认我们的 MallResult 结构。
 * 因此用本对象把「处理是否成功」与「该回什么应答体」一并交给 Controller，
 * 由 Controller 决定 HTTP 状态码。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallbackResult {

    /** 处理是否成功（验签通过且状态推进完成）；false 时 Controller 应返回 HTTP 400 */
    private boolean success;

    /** 返回给支付平台的应答体（如模拟渠道的 {@code success}） */
    private String responseBody;

    /** 失败原因，{@code success=false} 时非空 */
    private String failReason;
}
