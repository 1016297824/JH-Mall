package com.mall.auth.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController();
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldReturnNotOpenWhenCreateUser() throws Exception {
        mockMvc.perform(post("/api/auth/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A9999"))
                .andExpect(jsonPath("$.errorMessage").value("该功能暂未开放，请使用 CAPTCHA 端点"));
    }

    @Test
    void shouldReturnNotOpenWhenSendSmsCode() throws Exception {
        mockMvc.perform(post("/api/auth/sms_codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A9999"))
                .andExpect(jsonPath("$.errorMessage").value("该功能暂未开放，请使用 CAPTCHA 端点"));
    }

    @Test
    void shouldReturnNotOpenWhenCreateSession() throws Exception {
        mockMvc.perform(post("/api/auth/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A9999"))
                .andExpect(jsonPath("$.errorMessage").value("该功能暂未开放，请使用 CAPTCHA 端点"));
    }

    @Test
    void shouldReturnNotOpenWhenCreateSessionBySms() throws Exception {
        mockMvc.perform(post("/api/auth/sessions/sms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A9999"))
                .andExpect(jsonPath("$.errorMessage").value("该功能暂未开放，请使用 CAPTCHA 端点"));
    }

    @Test
    void shouldReturnNotOpenWhenRefreshSession() throws Exception {
        mockMvc.perform(post("/api/auth/sessions/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A9999"))
                .andExpect(jsonPath("$.errorMessage").value("该功能暂未开放，请使用 CAPTCHA 端点"));
    }

    @Test
    void shouldReturnNotOpenWhenGetCurrentSession() throws Exception {
        mockMvc.perform(get("/api/auth/sessions/current")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A9999"))
                .andExpect(jsonPath("$.errorMessage").value("该功能暂未开放，请使用 CAPTCHA 端点"));
    }

    @Test
    void shouldReturnNotOpenWhenDeleteCurrentSession() throws Exception {
        mockMvc.perform(delete("/api/auth/sessions/current")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A9999"))
                .andExpect(jsonPath("$.errorMessage").value("该功能暂未开放，请使用 CAPTCHA 端点"));
    }

    @Test
    void shouldReturnNotOpenWhenCreateWechatSession() throws Exception {
        mockMvc.perform(post("/api/auth/wechat/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A9999"))
                .andExpect(jsonPath("$.errorMessage").value("该功能暂未开放，请使用 CAPTCHA 端点"));
    }

    @Test
    void shouldReturnNotOpenWhenWechatPhoneBinding() throws Exception {
        mockMvc.perform(post("/api/auth/wechat/phone_binding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A9999"))
                .andExpect(jsonPath("$.errorMessage").value("该功能暂未开放，请使用 CAPTCHA 端点"));
    }

    @Test
    void shouldReturnNotOpenWhenChangePhone() throws Exception {
        mockMvc.perform(put("/api/auth/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A9999"))
                .andExpect(jsonPath("$.errorMessage").value("该功能暂未开放，请使用 CAPTCHA 端点"));
    }

    @Test
    void shouldReturnNotOpenWhenResetPassword() throws Exception {
        mockMvc.perform(put("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A9999"))
                .andExpect(jsonPath("$.errorMessage").value("该功能暂未开放，请使用 CAPTCHA 端点"));
    }

    @Test
    void shouldReturnNotOpenWhenChangePassword() throws Exception {
        mockMvc.perform(put("/api/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A9999"))
                .andExpect(jsonPath("$.errorMessage").value("该功能暂未开放，请使用 CAPTCHA 端点"));
    }

    @Test
    void shouldReturnNotOpenWhenDeleteAccount() throws Exception {
        mockMvc.perform(delete("/api/auth/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A9999"))
                .andExpect(jsonPath("$.errorMessage").value("该功能暂未开放，请使用 CAPTCHA 端点"));
    }
}
