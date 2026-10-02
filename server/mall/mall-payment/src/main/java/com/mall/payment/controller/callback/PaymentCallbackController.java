package com.mall.payment.controller.callback;

import com.mall.payment.dto.response.CallbackResult;
import com.mall.payment.service.CallbackService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付渠道回调控制器
 *
 * <p>对应设计文档 {@code docs/design/13_mall-payment详细设计.md} §2.2 端点 10、11 与 §10 回调安全。</p>
 *
 * <p><b>三条约束</b>：</p>
 * <ol>
 *   <li><b>无认证</b>：支付平台无法携带我方 token，身份由适配器层验签保证
 *       （网关不可能实现微信 SHA256-RSA / 支付宝 RSA2 验签，故验签在适配器层）。</li>
 *   <li><b>原始报文不解析直接用</b>：{@code rawBody} 原样透传给适配器，
 *       因为验签必须基于原始字节，任何预处理都会破坏签名。</li>
 *   <li><b>验签失败返回 HTTP 400 且无业务错误码</b>（设计 §12）：
 *       应答方是支付平台而非浏览器，它只看 HTTP 状态码与应答体，
 *       包装成 {@code MallResult} 反而会让平台误判为「处理成功」。</li>
 * </ol>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Slf4j
@RestController
@RequestMapping("/callback/payment")
@RequiredArgsConstructor
public class PaymentCallbackController {

    /** 平台应答体统一按 UTF-8 输出，避免失败原因中的中文被默认字符集损坏 */
    private static final MediaType PLAIN_TEXT_UTF8 =
            new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);

    private final CallbackService callbackService;

    /**
     * 支付结果回调
     *
     * @param channel 渠道编码（路径变量）
     * @param rawBody 回调原始报文
     * @param request HTTP 请求（取回调头，供适配器验签）
     * @return 支付平台要求的应答体
     */
    @PostMapping("/{channel}")
    public ResponseEntity<String> payCallback(@PathVariable("channel") String channel,
                                              @RequestBody(required = false) String rawBody,
                                              HttpServletRequest request) {
        CallbackResult result = callbackService.processPayCallback(channel, rawBody, extractHeaders(request));
        return toResponse(result);
    }

    /**
     * 退款结果回调
     *
     * @param channel 渠道编码（路径变量）
     * @param rawBody 回调原始报文
     * @param request HTTP 请求（取回调头，供适配器验签）
     * @return 支付平台要求的应答体
     */
    @PostMapping("/{channel}/refund")
    public ResponseEntity<String> refundCallback(@PathVariable("channel") String channel,
                                                 @RequestBody(required = false) String rawBody,
                                                 HttpServletRequest request) {
        CallbackResult result = callbackService.processRefundCallback(channel, rawBody, extractHeaders(request));
        return toResponse(result);
    }

    /**
     * 把处理结果转为 HTTP 应答
     *
     * @param result 回调处理结果
     * @return 成功 200 + 平台要求的应答体；失败 400 + 失败原因
     */
    private ResponseEntity<String> toResponse(CallbackResult result) {
        if (result.isSuccess()) {
            return ResponseEntity.ok()
                    .contentType(PLAIN_TEXT_UTF8)
                    .body(result.getResponseBody());
        }
        return ResponseEntity.badRequest()
                .contentType(PLAIN_TEXT_UTF8)
                .body(result.getFailReason());
    }

    /**
     * 提取全部请求头
     *
     * @param request HTTP 请求
     * @return 请求头键值对（无头时为空 Map）
     */
    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames == null) {
            return headers;
        }
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }
}
