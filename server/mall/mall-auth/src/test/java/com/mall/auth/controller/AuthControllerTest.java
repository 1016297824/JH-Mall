package com.mall.auth.controller;

import com.mall.auth.DTO.response.TokenRespDTO;
import com.mall.auth.service.ITokenService;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.TokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private MockMvc mockMvc;
    private ITokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = mock(ITokenService.class);
        AuthController controller = new AuthController(tokenService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.mall.common.handler.MallExceptionHandler())
                .build();
    }

    // ========== 占位端点（暂未开放）==========

    @Test
    void shouldReturnNotOpenWhenCreateUser() throws Exception {
        mockMvc.perform(post("/api/auth/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A9999"))
                .andExpect(jsonPath("$.errorMessage").value("该功能暂未开放"));
    }

    @Test
    void shouldReturnNotOpenWhenSendSmsCode() throws Exception {
        mockMvc.perform(post("/api/auth/sms_codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A9999"))
                .andExpect(jsonPath("$.errorMessage").value("该功能暂未开放"));
    }

    @Test
    void shouldReturnNotOpenWhenCreateSession() throws Exception {
        mockMvc.perform(post("/api/auth/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A9999"))
                .andExpect(jsonPath("$.errorMessage").value("该功能暂未开放"));
    }

    @Test
    void shouldReturnNotOpenWhenCreateSessionBySms() throws Exception {
        mockMvc.perform(post("/api/auth/sessions/sms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A9999"))
                .andExpect(jsonPath("$.errorMessage").value("该功能暂未开放"));
    }

    @Test
    void shouldReturnNotOpenWhenCreateWechatSession() throws Exception {
        mockMvc.perform(post("/api/auth/wechat/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A9999"))
                .andExpect(jsonPath("$.errorMessage").value("该功能暂未开放"));
    }

    @Test
    void shouldReturnNotOpenWhenWechatPhoneBinding() throws Exception {
        mockMvc.perform(post("/api/auth/wechat/phone_binding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A9999"))
                .andExpect(jsonPath("$.errorMessage").value("该功能暂未开放"));
    }

    @Test
    void shouldReturnNotOpenWhenChangePhone() throws Exception {
        mockMvc.perform(put("/api/auth/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A9999"))
                .andExpect(jsonPath("$.errorMessage").value("该功能暂未开放"));
    }

    @Test
    void shouldReturnNotOpenWhenResetPassword() throws Exception {
        mockMvc.perform(put("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A9999"))
                .andExpect(jsonPath("$.errorMessage").value("该功能暂未开放"));
    }

    @Test
    void shouldReturnNotOpenWhenChangePassword() throws Exception {
        mockMvc.perform(put("/api/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A9999"))
                .andExpect(jsonPath("$.errorMessage").value("该功能暂未开放"));
    }

    @Test
    void shouldReturnNotOpenWhenDeleteAccount() throws Exception {
        mockMvc.perform(delete("/api/auth/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A9999"))
                .andExpect(jsonPath("$.errorMessage").value("该功能暂未开放"));
    }

    // ========== POST /sessions/refresh ==========

    @Test
    void shouldRefreshTokenSuccess() throws Exception {
        TokenRespDTO tokenResp = new TokenRespDTO("new-access", "new-refresh", 1800L);
        when(tokenService.refresh("old-refresh-token")).thenReturn(tokenResp);

        mockMvc.perform(post("/api/auth/sessions/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"old-refresh-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"))
                .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh"))
                .andExpect(jsonPath("$.data.expiresIn").value(1800));
    }

    @Test
    void shouldRefreshTokenFailWhenTokenInvalid() throws Exception {
        when(tokenService.refresh("bad-token"))
                .thenThrow(new TokenException(ErrorCode.TOKEN_INVALID));

        mockMvc.perform(post("/api/auth/sessions/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"bad-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A0231"));
    }

    @Test
    void shouldRefreshTokenFailWhenRefreshTokenBlank() throws Exception {
        mockMvc.perform(post("/api/auth/sessions/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A0401"));
    }

    // ========== GET /sessions/current ==========

    @Test
    void shouldCheckSessionSuccess() throws Exception {
        when(tokenService.verify("valid-access-token")).thenReturn("user-001");

        mockMvc.perform(get("/api/auth/sessions/current")
                        .header("Authorization", "Bearer valid-access-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"))
                .andExpect(jsonPath("$.data.userId").value("user-001"));
    }

    @Test
    void shouldCheckSessionFailWhenNoAuthHeader() throws Exception {
        mockMvc.perform(get("/api/auth/sessions/current")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A0231"));
    }

    @Test
    void shouldCheckSessionFailWhenTokenInvalid() throws Exception {
        when(tokenService.verify("expired-token"))
                .thenThrow(new TokenException(ErrorCode.TOKEN_INVALID));

        mockMvc.perform(get("/api/auth/sessions/current")
                        .header("Authorization", "Bearer expired-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A0231"));
    }

    // ========== DELETE /sessions/current ==========

    @Test
    void shouldLogoutSuccess() throws Exception {
        mockMvc.perform(delete("/api/auth/sessions/current")
                        .header("Authorization", "Bearer valid-access-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"));

        verify(tokenService).revoke("valid-access-token");
    }

    @Test
    void shouldLogoutFailWhenNoAuthHeader() throws Exception {
        mockMvc.perform(delete("/api/auth/sessions/current")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A0231"));
    }
}
