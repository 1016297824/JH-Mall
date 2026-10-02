package com.mall.marketing.controller.inner;

import com.mall.api.feign.RemoteMarketingService.CalculationReq;
import com.mall.api.feign.RemoteMarketingService.CalculationResp;
import com.mall.marketing.service.CalculationService;
import com.mall.marketing.service.CouponClaimService;
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
 * 营销内部 Feign 端点单元测试
 *
 * <p>重点验证两件事：① 路径与契约 {@code RemoteMarketingService} 完全一致（否则 Feign 404）；
 * ② <b>返回裸对象而非 {@code MallResult}</b>（包一层会导致 Feign 反序列化失败）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@ExtendWith(MockitoExtension.class)
class RemoteMarketingInnerControllerTest {

    @Mock private CalculationService calculationService;
    @Mock private CouponClaimService couponClaimService;
    @InjectMocks private RemoteMarketingInnerController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("POST /inner/marketing/calculate 返回裸 CalculationResp（无 MallResult 包装）")
    void calculateReturnsRawResp() throws Exception {
        CalculationResp resp = new CalculationResp();
        resp.setOriginalAmount(10000L);
        resp.setFinalAmount(9000L);
        when(calculationService.calculate(any(CalculationReq.class))).thenReturn(resp);

        mockMvc.perform(post("/inner/marketing/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":100,\"items\":[{\"skuId\":101,\"price\":1000,\"quantity\":10}]}"))
                .andExpect(status().isOk())
                // 字段在根上，证明没有 MallResult 包装
                .andExpect(jsonPath("$.finalAmount").value(9000))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("POST /inner/marketing/coupon/lock 返回裸 boolean")
    void lockCouponReturnsBoolean() throws Exception {
        when(couponClaimService.lockCoupon(500L, "ORDER_001")).thenReturn(true);

        mockMvc.perform(post("/inner/marketing/coupon/lock")
                        .param("orderNo", "ORDER_001")
                        .param("couponClaimId", "500"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(couponClaimService).lockCoupon(500L, "ORDER_001");
    }

    @Test
    @DisplayName("POST /inner/marketing/coupon/release 释放订单券")
    void releaseCoupon() throws Exception {
        mockMvc.perform(post("/inner/marketing/coupon/release")
                        .param("orderNo", "ORDER_001"))
                .andExpect(status().isOk());

        verify(couponClaimService).releaseCoupon("ORDER_001");
    }

    @Test
    @DisplayName("GET /inner/marketing/coupon/validate 返回裸 boolean")
    void validateCouponReturnsBoolean() throws Exception {
        when(couponClaimService.validateCoupon(500L, 100L)).thenReturn(true);

        mockMvc.perform(get("/inner/marketing/coupon/validate")
                        .param("couponClaimId", "500")
                        .param("userId", "100"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(couponClaimService).validateCoupon(500L, 100L);
    }
}
