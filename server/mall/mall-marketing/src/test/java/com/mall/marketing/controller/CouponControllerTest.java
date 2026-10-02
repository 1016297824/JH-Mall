package com.mall.marketing.controller;

import com.mall.marketing.dto.response.CouponDefResp;
import com.mall.marketing.dto.response.CouponRecordResp;
import com.mall.marketing.service.CouponClaimService;
import com.mall.marketing.service.CouponDefService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C 端优惠券控制器单元测试
 *
 * <p>Mock Service，用 MockMvc 独立校验路径、请求头取用户身份、limit 归一化三件事。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@ExtendWith(MockitoExtension.class)
class CouponControllerTest {

    private static final String USER_ID_HEADER = "X-User-Id";

    @Mock private CouponDefService couponDefService;
    @Mock private CouponClaimService couponClaimService;
    @InjectMocks private CouponController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private CouponDefResp couponDef() {
        CouponDefResp resp = new CouponDefResp();
        resp.setId(10L);
        resp.setCouponName("满 100 减 10");
        return resp;
    }

    private CouponRecordResp couponRecord() {
        CouponRecordResp resp = new CouponRecordResp();
        resp.setId(500L);
        resp.setCouponName("满 100 减 10");
        resp.setRecordStatus(1);
        return resp;
    }

    @Test
    @DisplayName("GET /api/marketing/coupons 返回可领券列表")
    void listAvailableCoupons() throws Exception {
        when(couponDefService.listAvailableCoupons(20)).thenReturn(List.of(couponDef()));

        mockMvc.perform(get("/api/marketing/coupons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].couponName").value("满 100 减 10"));
    }

    @Test
    @DisplayName("limit 超过服务端上限时被截断到 50")
    void listAvailableCouponsCapsLimit() throws Exception {
        when(couponDefService.listAvailableCoupons(50)).thenReturn(List.of());

        mockMvc.perform(get("/api/marketing/coupons").param("limit", "999"))
                .andExpect(status().isOk());

        verify(couponDefService).listAvailableCoupons(50);
    }

    @Test
    @DisplayName("limit 非法（0 或负数）时兜底为 1")
    void listAvailableCouponsNormalizesNonPositiveLimit() throws Exception {
        when(couponDefService.listAvailableCoupons(1)).thenReturn(List.of());

        mockMvc.perform(get("/api/marketing/coupons").param("limit", "0"))
                .andExpect(status().isOk());

        verify(couponDefService).listAvailableCoupons(1);
    }

    @Test
    @DisplayName("POST 领券：用户身份取自请求头 X-User-Id")
    void claimCouponUsesUserIdFromHeader() throws Exception {
        when(couponClaimService.claimCoupon(100L, 10L)).thenReturn(500L);

        mockMvc.perform(post("/api/marketing/coupons/10/claims")
                        .header(USER_ID_HEADER, "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(500));

        verify(couponClaimService).claimCoupon(100L, 10L);
    }

    @Test
    @DisplayName("GET 我的优惠券：用户身份取自请求头，状态过滤可空")
    void listMyCouponsUsesUserIdFromHeader() throws Exception {
        when(couponClaimService.listMyCoupons(100L, null, 20)).thenReturn(List.of(couponRecord()));

        mockMvc.perform(get("/api/marketing/coupons/claims")
                        .header(USER_ID_HEADER, "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(500));

        verify(couponClaimService).listMyCoupons(100L, null, 20);
    }

    @Test
    @DisplayName("GET 我的优惠券：状态过滤参数透传")
    void listMyCouponsPassesStatusFilter() throws Exception {
        when(couponClaimService.listMyCoupons(eq(100L), eq(2), anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/marketing/coupons/claims")
                        .header(USER_ID_HEADER, "100")
                        .param("status", "2"))
                .andExpect(status().isOk());

        verify(couponClaimService).listMyCoupons(100L, 2, 20);
    }
}
