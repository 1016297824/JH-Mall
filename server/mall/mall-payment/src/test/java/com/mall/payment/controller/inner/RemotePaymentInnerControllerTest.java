package com.mall.payment.controller.inner;

import com.mall.api.feign.RemotePaymentService.PaymentStatusDTO;
import com.mall.api.feign.RemotePaymentService.RefundDTO;
import com.mall.api.feign.RemotePaymentService.RefundResultDTO;
import com.mall.payment.service.RefundService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 支付模块内部端点单元测试
 *
 * <p>重点校验两件事：<b>路径与契约字符级一致</b>（否则 Feign 调用 404）、
 * <b>返回裸对象</b>（包一层 {@code MallResult} 会让 Feign 反序列化失败）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class RemotePaymentInnerControllerTest {

    private static final String PAYMENT_NO = "PAY20261003001";

    private static final String ORDER_NO = "ORD20261003001";

    @Mock
    private RefundService refundService;

    @InjectMocks
    private RemotePaymentInnerController innerController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(innerController).build();
    }

    @Test
    @DisplayName("POST /inner/payment/refunds：返回裸对象，根级即 refundNo")
    void createRefundReturnsBareObject() throws Exception {
        RefundResultDTO dto = new RefundResultDTO("REF20261003001", 0, "MOCKREF20261003001");
        when(refundService.createRefund(any(RefundDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/inner/payment/refunds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentNo\":\"PAY20261003001\",\"refundAmount\":1000,"
                                + "\"afterSaleNo\":\"AS001\",\"channelCode\":\"wechat\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundNo").value("REF20261003001"))
                .andExpect(jsonPath("$.refundStatus").value(0))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(refundService).createRefund(any(RefundDTO.class));
    }

    @Test
    @DisplayName("POST /inner/payment/refunds/by-after-sale：传 orderNo 而非 paymentNo")
    void refundByOrderNoUsesOrderNo() throws Exception {
        RefundResultDTO dto = new RefundResultDTO("REF20261003002", 0, null);
        when(refundService.refundByOrderNo(ORDER_NO, 1000L, 66L)).thenReturn(dto);

        mockMvc.perform(post("/inner/payment/refunds/by-after-sale")
                        .param("orderNo", ORDER_NO)
                        .param("refundAmount", "1000")
                        .param("afterSaleId", "66"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundNo").value("REF20261003002"))
                .andExpect(jsonPath("$.code").doesNotExist());

        verify(refundService).refundByOrderNo(ORDER_NO, 1000L, 66L);
    }

    @Test
    @DisplayName("GET /inner/payment/status：返回裸对象，含累计已退款金额")
    void getPaymentStatusReturnsBareObject() throws Exception {
        PaymentStatusDTO dto = new PaymentStatusDTO(PAYMENT_NO, ORDER_NO, 1, 89900L, 1000L);
        when(refundService.getPaymentStatus(PAYMENT_NO)).thenReturn(dto);

        mockMvc.perform(get("/inner/payment/status").param("payOrderNo", PAYMENT_NO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentNo").value(PAYMENT_NO))
                .andExpect(jsonPath("$.paymentStatus").value(1))
                .andExpect(jsonPath("$.refundedAmount").value(1000))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(refundService).getPaymentStatus(PAYMENT_NO);
    }

    @Test
    @DisplayName("GET /inner/payment/status：支付单不存在时返回 200 + 空体（调用方按 null 判断）")
    void getPaymentStatusReturnsEmptyWhenNotFound() throws Exception {
        when(refundService.getPaymentStatus(PAYMENT_NO)).thenReturn(null);

        mockMvc.perform(get("/inner/payment/status").param("payOrderNo", PAYMENT_NO))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }
}
