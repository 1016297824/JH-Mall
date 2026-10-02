package com.mall.payment.controller;

import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.payment.dto.request.PayRequestDTO;
import com.mall.payment.dto.response.PayResultDTO;
import com.mall.payment.service.PaymentService;
import com.mall.payment.vo.PaymentVO;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C 端支付控制器单元测试
 *
 * <p>Mock Service，用 MockMvc 独立校验三件事：路径、<b>用户身份只来自 X-User-Id 请求头</b>、
 * 统一响应体包装。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private static final String USER_ID_HEADER = "X-User-Id";

    private static final Long USER_ID = 100L;

    private static final Long PAYMENT_ID = 888L;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController).build();
    }

    @Test
    @DisplayName("POST /api/payment/payments：身份取自 X-User-Id 头，返回 MallResult 包装")
    void createPaymentUsesHeaderUserId() throws Exception {
        PayResultDTO dto = new PayResultDTO();
        dto.setPaymentNo("PAY20261003001");
        when(paymentService.createPayment(eq(USER_ID), any(PayRequestDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/api/payment/payments")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNo\":\"ORD20261003001\",\"channelCode\":\"wechat\",\"openid\":\"oX-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentNo").value("PAY20261003001"));

        ArgumentCaptor<PayRequestDTO> captor = ArgumentCaptor.forClass(PayRequestDTO.class);
        verify(paymentService).createPayment(eq(USER_ID), captor.capture());
        assertThat(captor.getValue().getOrderNo()).isEqualTo("ORD20261003001");
        assertThat(captor.getValue().getChannelCode()).isEqualTo("wechat");
    }

    @Test
    @DisplayName("GET /api/payment/payments/{paymentId}：身份取自 X-User-Id 头")
    void getPaymentUsesHeaderUserId() throws Exception {
        PaymentVO vo = new PaymentVO();
        vo.setPaymentNo("PAY20261003001");
        vo.setPaymentStatus(0);
        when(paymentService.getPayment(USER_ID, PAYMENT_ID)).thenReturn(vo);

        mockMvc.perform(get("/api/payment/payments/{paymentId}", PAYMENT_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentNo").value("PAY20261003001"));

        verify(paymentService).getPayment(USER_ID, PAYMENT_ID);
    }

    @Test
    @DisplayName("Service 抛业务异常时不在控制器内被吞掉，原样向外传播")
    void createPaymentPropagatesServiceException() {
        when(paymentService.createPayment(eq(USER_ID), any(PayRequestDTO.class)))
                .thenThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // standaloneSetup 未注册统一异常处理器，异常应原样包装抛出，
        // 证明控制器没有 try/catch 吞掉业务异常导致错误被掩盖
        assertThatThrownBy(() -> mockMvc.perform(post("/api/payment/payments")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNo\":\"ORD404\",\"channelCode\":\"wechat\"}")))
                .hasRootCauseInstanceOf(BusinessException.class);
    }
}
