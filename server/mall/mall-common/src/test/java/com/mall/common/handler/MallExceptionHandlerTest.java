package com.mall.common.handler;

import com.mall.common.dto.MallResult;
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
            throw new CaptchaException("A0001", "验证码错误", "请重新输入验证码");
        }

        @GetMapping("/test/token")
        public void throwToken() {
            throw new TokenException("A0201", "Token无效", "请重新登录");
        }

        @GetMapping("/test/business")
        public void throwBusiness() {
            throw new BusinessException("A0101", "未同意隐私协议", "请同意隐私协议");
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
                .andExpect(jsonPath("$.errorCode").value("A0001"))
                .andExpect(jsonPath("$.errorMessage").value("验证码错误"));
    }

    @Test
    void shouldReturnBusinessErrorWhenBusinessExceptionThrown() throws Exception {
        mockMvc.perform(get("/test/business").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A0101"))
                .andExpect(jsonPath("$.errorMessage").value("未同意隐私协议"));
    }

    @Test
    void shouldReturnTokenErrorWhenTokenExceptionThrown() throws Exception {
        mockMvc.perform(get("/test/token").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("A0201"))
                .andExpect(jsonPath("$.errorMessage").value("Token无效"));
    }

    @Test
    void shouldReturnSystemErrorWhenUnknownExceptionThrown() throws Exception {
        mockMvc.perform(get("/test/exception").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("B0001"))
                .andExpect(jsonPath("$.errorMessage").value("系统繁忙，请稍后再试"));
    }
}
