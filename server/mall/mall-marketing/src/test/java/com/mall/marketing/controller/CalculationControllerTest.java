package com.mall.marketing.controller;

import com.mall.api.feign.RemoteMarketingService.CalculationReq;
import com.mall.api.feign.RemoteMarketingService.CalculationResp;
import com.mall.marketing.service.CalculationService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C 端优惠试算控制器单元测试
 *
 * <p><b>核心安全用例</b>：请求体里塞 {@code userId} 必须被忽略，用户身份只能来自
 * {@code X-User-Id} 请求头——否则「券归属校验」可被绕过（越权用他人券）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@ExtendWith(MockitoExtension.class)
class CalculationControllerTest {

    private static final String USER_ID_HEADER = "X-User-Id";

    @Mock private CalculationService calculationService;
    @InjectMocks private CalculationController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("POST /api/marketing/calculations 返回试算结果")
    void calculateReturnsResult() throws Exception {
        CalculationResp resp = new CalculationResp();
        resp.setOriginalAmount(10000L);
        resp.setCouponDiscount(1000L);
        resp.setPromotionDiscount(0L);
        resp.setFinalAmount(9000L);
        when(calculationService.calculate(any(CalculationReq.class))).thenReturn(resp);

        mockMvc.perform(post("/api/marketing/calculations")
                        .header(USER_ID_HEADER, "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"skuId\":101,\"price\":1000,\"quantity\":10}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.originalAmount").value(10000))
                .andExpect(jsonPath("$.data.finalAmount").value(9000));
    }

    @Test
    @DisplayName("🔴 请求体里的 userId 被忽略，身份只认 X-User-Id 请求头")
    void calculateIgnoresUserIdInBody() throws Exception {
        when(calculationService.calculate(any(CalculationReq.class))).thenReturn(new CalculationResp());

        // 攻击者把自己伪装成 999 号用户
        mockMvc.perform(post("/api/marketing/calculations")
                        .header(USER_ID_HEADER, "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":999,\"items\":[{\"skuId\":101,\"price\":1000,\"quantity\":1}]}"))
                .andExpect(status().isOk());

        ArgumentCaptor<CalculationReq> captor = ArgumentCaptor.forClass(CalculationReq.class);
        verify(calculationService).calculate(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("商品明细与指定券 ID 原样透传给 Service")
    void calculatePassesItemsAndCouponClaimId() throws Exception {
        when(calculationService.calculate(any(CalculationReq.class))).thenReturn(new CalculationResp());

        mockMvc.perform(post("/api/marketing/calculations")
                        .header(USER_ID_HEADER, "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"skuId\":101,\"price\":1000,\"quantity\":2},"
                                + "{\"skuId\":102,\"price\":500,\"quantity\":1}],\"couponClaimId\":500}"))
                .andExpect(status().isOk());

        ArgumentCaptor<CalculationReq> captor = ArgumentCaptor.forClass(CalculationReq.class);
        verify(calculationService).calculate(captor.capture());
        CalculationReq captured = captor.getValue();
        assertThat(captured.getCouponClaimId()).isEqualTo(500L);
        assertThat(captured.getItems()).hasSize(2);
        assertThat(captured.getItems().get(0).getSkuId()).isEqualTo(101L);
        assertThat(captured.getItems().get(0).getPrice()).isEqualTo(1000L);
        assertThat(captured.getItems().get(0).getQuantity()).isEqualTo(2);
    }
}
