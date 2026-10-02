package com.mall.payment.controller.callback;

import com.mall.payment.dto.response.CallbackResult;
import com.mall.payment.service.CallbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 支付渠道回调控制器单元测试
 *
 * <p>重点校验三件事：验签失败返回 <b>HTTP 400 且无 MallResult 包装</b>（设计 §12）、
 * 成功时返回支付平台要求的应答体、<b>原始报文与请求头原样透传</b>给适配器（验签须基于原始字节）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class PaymentCallbackControllerTest {

    private static final String CHANNEL = "wechat";

    /** 模拟渠道回调的原始 JSON 报文 */
    private static final String RAW_BODY =
            "{\"paymentNo\":\"PAY20261003001\",\"tradeStatus\":\"SUCCESS\",\"sign\":\"MOCK_SIGN\"}";

    @Mock
    private CallbackService callbackService;

    @InjectMocks
    private PaymentCallbackController callbackController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(callbackController).build();
    }

    @Test
    @DisplayName("支付回调成功 → HTTP 200 + 平台应答体")
    void payCallbackSuccessReturns200() throws Exception {
        when(callbackService.processPayCallback(eq(CHANNEL), eq(RAW_BODY), anyMap()))
                .thenReturn(new CallbackResult(true, "success", null));

        mockMvc.perform(post("/callback/payment/{channel}", CHANNEL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RAW_BODY))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));
    }

    @Test
    @DisplayName("验签失败 → HTTP 400（无业务错误码，平台只看 HTTP 码与应答体）")
    void payCallbackVerifyFailedReturns400() throws Exception {
        when(callbackService.processPayCallback(eq(CHANNEL), eq(RAW_BODY), anyMap()))
                .thenReturn(new CallbackResult(false, null, "验签失败"));

        mockMvc.perform(post("/callback/payment/{channel}", CHANNEL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RAW_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("验签失败"));
    }

    @Test
    @DisplayName("原始报文与请求头原样透传（验签必须基于原始字节）")
    void payCallbackPassesRawBodyAndHeaders() throws Exception {
        when(callbackService.processPayCallback(eq(CHANNEL), anyString(), anyMap()))
                .thenReturn(new CallbackResult(true, "success", null));

        mockMvc.perform(post("/callback/payment/{channel}", CHANNEL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Wechatpay-Signature", "SIG-123")
                        .content(RAW_BODY))
                .andExpect(status().isOk());

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> headerCaptor = ArgumentCaptor.forClass(Map.class);
        verify(callbackService).processPayCallback(eq(CHANNEL), bodyCaptor.capture(), headerCaptor.capture());

        // 报文字节级一致：任何裁剪/重排都会破坏签名校验
        assertThat(bodyCaptor.getValue()).isEqualTo(RAW_BODY);
        assertThat(headerCaptor.getValue()).containsEntry("Wechatpay-Signature", "SIG-123");
    }

    @Test
    @DisplayName("退款回调路径 /callback/payment/{channel}/refund")
    void refundCallbackPath() throws Exception {
        when(callbackService.processRefundCallback(eq(CHANNEL), eq(RAW_BODY), anyMap()))
                .thenReturn(new CallbackResult(true, "success", null));

        mockMvc.perform(post("/callback/payment/{channel}/refund", CHANNEL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RAW_BODY))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));

        verify(callbackService).processRefundCallback(eq(CHANNEL), eq(RAW_BODY), anyMap());
    }

    @Test
    @DisplayName("退款回调失败 → HTTP 400")
    void refundCallbackFailedReturns400() throws Exception {
        when(callbackService.processRefundCallback(eq(CHANNEL), eq(RAW_BODY), anyMap()))
                .thenReturn(new CallbackResult(false, null, "退款单不存在"));

        mockMvc.perform(post("/callback/payment/{channel}/refund", CHANNEL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RAW_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("退款单不存在"));
    }
}
