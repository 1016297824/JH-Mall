package com.mall.common.handler;

import com.mall.common.dto.MallResult;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.common.exception.CaptchaException;
import com.mall.common.exception.TokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MallExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new MallExceptionHandler())
                .build();
    }

    @RestController
    static class TestController {
        @GetMapping("/test/captcha")
        public void throwCaptcha() {
            throw new CaptchaException(ErrorCode.CAPTCHA_VERIFY_ERROR);
        }

        @GetMapping("/test/token")
        public void throwToken() {
            throw new TokenException(ErrorCode.TOKEN_INVALID);
        }

        @GetMapping("/test/business")
        public void throwBusiness() {
            throw new BusinessException(ErrorCode.PRIVACY_NOT_AGREED);
        }

        @GetMapping("/test/exception")
        public void throwException() {
            throw new RuntimeException("未知错误");
        }
    }

    @Test
    void shouldReturnCaptchaErrorWhenCaptchaExceptionThrown() throws Exception {
        mockMvc.perform(get("/test/captcha").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A0240"))
                .andExpect(jsonPath("$.errorMessage").value("验证码错误"));
    }

    @Test
    void shouldReturnBusinessErrorWhenBusinessExceptionThrown() throws Exception {
        mockMvc.perform(get("/test/business").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A0101"))
                .andExpect(jsonPath("$.errorMessage").value("未同意隐私协议（isPrivacyAgreed != 1）"));
    }

    @Test
    void shouldReturnTokenErrorWhenTokenExceptionThrown() throws Exception {
        mockMvc.perform(get("/test/token").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A0231"))
                .andExpect(jsonPath("$.errorMessage").value("refresh_token 失效或 token 已注销"));
    }

    @Test
    void shouldReturnSystemErrorWhenUnknownExceptionThrown() throws Exception {
        mockMvc.perform(get("/test/exception").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("B0001"))
                .andExpect(jsonPath("$.errorMessage").value("系统繁忙，请稍后再试"));
    }
}
