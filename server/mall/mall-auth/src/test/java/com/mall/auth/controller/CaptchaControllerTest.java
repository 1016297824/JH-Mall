package com.mall.auth.controller;

import com.mall.common.dto.user.MallUserDTO;
import com.mall.common.enums.user.UserStatusEnum;
import com.mall.api.feign.RemoteUserService;
import com.mall.auth.dto.response.CaptchaResponse;
import com.mall.auth.dto.response.TokenResponse;
import com.mall.auth.service.CaptchaService;
import com.mall.auth.service.TokenService;
import com.mall.common.exception.CaptchaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CaptchaControllerTest {

    private MockMvc mockMvc;
    private CaptchaService captchaService;
    private TokenService tokenService;
    private RemoteUserService remoteUserService;
    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOperations;
    private BCryptPasswordEncoder passwordEncoder;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        captchaService = mock(CaptchaService.class);
        tokenService = mock(TokenService.class);
        remoteUserService = mock(RemoteUserService.class);
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        passwordEncoder = new BCryptPasswordEncoder(4);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        CaptchaController controller = new CaptchaController(
                captchaService, tokenService, remoteUserService, redisTemplate, passwordEncoder);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.mall.common.handler.MallExceptionHandler())
                .build();
    }

    // ========== GET / 获取验证码 ==========

    @Test
    void shouldReturnCaptchaWhenGet() throws Exception {
        when(captchaService.generate()).thenReturn(Map.of(
                "captchaKey", "key-123",
                "captchaImage", "data:image/png;base64,XXXX"));

        mockMvc.perform(get("/api/auth/captcha").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"))
                .andExpect(jsonPath("$.data.captchaKey").value("key-123"))
                .andExpect(jsonPath("$.data.captchaImage").value("data:image/png;base64,XXXX"));
    }

    // ========== POST /register ==========

    @Test
    void shouldRegisterSuccess() throws Exception {
        String phone = "13800138000";
        String password = "pass1234";
        String encodedPassword = passwordEncoder.encode(password);

        MallUserDTO user = new MallUserDTO();
        user.setId("user-001");
        user.setPhone(phone);

        when(remoteUserService.findByPhone(phone)).thenReturn(null);
        when(remoteUserService.register(any(RemoteUserService.RegisterRequest.class)))
                .thenReturn("user-001");
        when(tokenService.issue("user-001"))
                .thenReturn(new TokenResponse("access-token-1", "refresh-token-1", 1800L));

        mockMvc.perform(post("/api/auth/captcha/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "phone": "13800138000",
                                    "password": "pass1234",
                                    "captchaKey": "key-123",
                                    "captchaCode": "abcd",
                                    "isPrivacyAgreed": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token-1"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token-1"));

        verify(captchaService).verify(eq("key-123"), eq("abcd"), anyString());
    }

    @Test
    void shouldRegisterFailWhenPhoneExists() throws Exception {
        MallUserDTO existing = new MallUserDTO();
        existing.setId("user-002");
        existing.setPhone("13800138000");

        when(remoteUserService.findByPhone("13800138000")).thenReturn(existing);

        mockMvc.perform(post("/api/auth/captcha/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "phone": "13800138000",
                                    "password": "pass1234",
                                    "captchaKey": "key-123",
                                    "captchaCode": "abcd",
                                    "isPrivacyAgreed": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A0151"));
    }

    @Test
    void shouldRegisterFailWhenPrivacyNotAgreed() throws Exception {
        mockMvc.perform(post("/api/auth/captcha/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "phone": "13800138000",
                                    "password": "pass1234",
                                    "captchaKey": "key-123",
                                    "captchaCode": "abcd",
                                    "isPrivacyAgreed": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A0101"));
    }

    @Test
    void shouldRegisterFailWhenCaptchaError() throws Exception {
        doThrow(new CaptchaException("A0131", "验证码错误", "验证码错误，请重新输入"))
                .when(captchaService).verify(anyString(), anyString(), anyString());

        mockMvc.perform(post("/api/auth/captcha/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "phone": "13800138000",
                                    "password": "pass1234",
                                    "captchaKey": "key-123",
                                    "captchaCode": "wrong",
                                    "isPrivacyAgreed": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A0131"));
    }

    // ========== POST /login ==========

    @Test
    void shouldLoginSuccess() throws Exception {
        String phone = "13800138000";
        String rawPassword = "pass1234";

        MallUserDTO user = new MallUserDTO();
        user.setId("user-001");
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setUserStatus(String.valueOf(UserStatusEnum.NORMAL.getCode()));

        when(remoteUserService.findByPhone(phone)).thenReturn(user);
        when(valueOperations.get("mall:auth:pwd_err:user-001")).thenReturn(null);
        when(tokenService.issue("user-001"))
                .thenReturn(new TokenResponse("access-token-2", "refresh-token-2", 1800L));

        mockMvc.perform(post("/api/auth/captcha/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "phone": "13800138000",
                                    "password": "pass1234",
                                    "captchaKey": "key-123",
                                    "captchaCode": "abcd"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token-2"));

        verify(captchaService).verify(eq("key-123"), eq("abcd"), anyString());
    }

    @Test
    void shouldLoginFailWhenAccountFrozen() throws Exception {
        MallUserDTO user = new MallUserDTO();
        user.setId("user-003");
        user.setPhone("13800138000");
        user.setUserStatus(String.valueOf(UserStatusEnum.FROZEN.getCode()));

        when(remoteUserService.findByPhone("13800138000")).thenReturn(user);

        mockMvc.perform(post("/api/auth/captcha/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "phone": "13800138000",
                                    "password": "pass1234",
                                    "captchaKey": "key-123",
                                    "captchaCode": "abcd"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A0202"));
    }

    @Test
    void shouldLoginFailWhenPasswordWrong() throws Exception {
        String phone = "13800138000";

        MallUserDTO user = new MallUserDTO();
        user.setId("user-001");
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode("correct"));
        user.setUserStatus(String.valueOf(UserStatusEnum.NORMAL.getCode()));

        when(remoteUserService.findByPhone(phone)).thenReturn(user);
        when(valueOperations.get("mall:auth:pwd_err:user-001")).thenReturn(null);
        when(valueOperations.increment("mall:auth:pwd_err:user-001", 1L)).thenReturn(1L);

        mockMvc.perform(post("/api/auth/captcha/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "phone": "13800138000",
                                    "password": "wrongpass",
                                    "captchaKey": "key-123",
                                    "captchaCode": "abcd"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A0210"));
    }

    @Test
    void shouldLoginFailWhenPasswordLocked() throws Exception {
        MallUserDTO user = new MallUserDTO();
        user.setId("user-001");
        user.setPhone("13800138000");
        user.setUserStatus(String.valueOf(UserStatusEnum.NORMAL.getCode()));

        when(remoteUserService.findByPhone("13800138000")).thenReturn(user);
        when(valueOperations.get("mall:auth:pwd_err:user-001")).thenReturn(5);

        mockMvc.perform(post("/api/auth/captcha/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "phone": "13800138000",
                                    "password": "pass1234",
                                    "captchaKey": "key-123",
                                    "captchaCode": "abcd"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A0211"));
    }

    @Test
    void shouldLoginFailWhenAccountDeactivated() throws Exception {
        MallUserDTO user = new MallUserDTO();
        user.setId("user-004");
        user.setPhone("13800138000");
        user.setUserStatus(String.valueOf(UserStatusEnum.DELETED.getCode()));

        when(remoteUserService.findByPhone("13800138000")).thenReturn(user);

        mockMvc.perform(post("/api/auth/captcha/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "phone": "13800138000",
                                    "password": "pass1234",
                                    "captchaKey": "key-123",
                                    "captchaCode": "abcd"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A0203"));
    }

    // ========== POST /password/reset ==========

    @Test
    void shouldResetPasswordSuccess() throws Exception {
        String phone = "13800138000";
        String newPassword = "newPass1";

        MallUserDTO user = new MallUserDTO();
        user.setId("user-001");
        user.setPhone(phone);

        when(remoteUserService.findByPhone(phone)).thenReturn(user);

        mockMvc.perform(post("/api/auth/captcha/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "phone": "13800138000",
                                    "newPassword": "newPass1",
                                    "captchaKey": "key-123",
                                    "captchaCode": "abcd"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"));

        verify(captchaService).verify(eq("key-123"), eq("abcd"), anyString());
        verify(remoteUserService).updatePassword(eq("user-001"), any(RemoteUserService.PasswordUpdateRequest.class));
        verify(tokenService).revokeAll("user-001");
    }

    // ========== PUT /phone ==========

    @Test
    void shouldChangePhoneSuccess() throws Exception {
        String oldPhone = "13800138000";
        String newPhone = "13900139000";

        MallUserDTO user = new MallUserDTO();
        user.setId("user-001");
        user.setPhone(oldPhone);
        user.setPassword(passwordEncoder.encode("pass1234"));

        when(remoteUserService.findByPhone(oldPhone)).thenReturn(user);
        when(remoteUserService.findByPhone(newPhone)).thenReturn(null);

        mockMvc.perform(put("/api/auth/captcha/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "oldPhone": "13800138000",
                                    "password": "pass1234",
                                    "newPhone": "13900139000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"));

        verify(remoteUserService).updatePhone(eq("user-001"), any(RemoteUserService.PhoneUpdateRequest.class));
    }

    // ========== DELETE /account ==========

    @Test
    void shouldDeactivateAccountSuccess() throws Exception {
        String phone = "13800138000";

        MallUserDTO user = new MallUserDTO();
        user.setId("user-001");
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode("pass1234"));

        when(remoteUserService.findByPhone(phone)).thenReturn(user);

        mockMvc.perform(delete("/api/auth/captcha/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "phone": "13800138000",
                                    "password": "pass1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"));

        verify(remoteUserService).deactivateAccount("user-001");
        verify(tokenService).revokeAll("user-001");
    }
}
