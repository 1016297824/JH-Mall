package com.mall.auth.service.impl;

import com.mall.auth.config.MallAuthConfigProperties;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.CaptchaException;
import com.wf.captcha.SpecCaptcha;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaptchaServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private CaptchaServiceImpl captchaService;

    @Mock
    private MallAuthConfigProperties authProperties;

    @BeforeEach
    void setUp() {
        MallAuthConfigProperties.Sms sms = new MallAuthConfigProperties.Sms();
        sms.setCodeTtl(300);
        sms.setIpDailyLimit(10);
        MallAuthConfigProperties.Captcha captcha = new MallAuthConfigProperties.Captcha();
        captcha.setIpTtl(86400);
        lenient().when(authProperties.getSms()).thenReturn(sms);
        lenient().when(authProperties.getCaptcha()).thenReturn(captcha);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ========== generate() 测试 ==========

    @Test
    void shouldGenerateReturnCaptchaKeyAndImage() {
        try (MockedConstruction<SpecCaptcha> mocked = mockConstruction(SpecCaptcha.class,
                (mock, context) -> {
                    when(mock.text()).thenReturn("abcd");
                    when(mock.toBase64()).thenReturn("data:image/png;base64,XXXX");
                })) {

            Map<String, String> result = captchaService.generate();

            assertNotNull(result.get("captchaKey"));
            assertFalse(result.get("captchaKey").isEmpty());
            assertNotNull(result.get("captchaImage"));
            assertEquals("data:image/png;base64,XXXX", result.get("captchaImage"));
            verify(valueOperations).set(
                    contains("mall:auth:captcha:"), eq("abcd"), eq(300L), eq(TimeUnit.SECONDS));
        }
    }

    @Test
    void shouldRetryWhenAmbiguousCharactersPresent() {
        AtomicInteger counter = new AtomicInteger(0);
        try (MockedConstruction<SpecCaptcha> mocked = mockConstruction(SpecCaptcha.class,
                (mock, context) -> {
                    int c = counter.incrementAndGet();
                    if (c <= 2) {
                        when(mock.text()).thenReturn("0o1i");
                    } else {
                        when(mock.text()).thenReturn("abcd");
                        when(mock.toBase64()).thenReturn("data:image/png;base64,Good");
                    }
                })) {

            Map<String, String> result = captchaService.generate();

            assertNotNull(result.get("captchaImage"));
            assertEquals("data:image/png;base64,Good", result.get("captchaImage"));
            assertEquals(3, mocked.constructed().size());
            verify(valueOperations).set(
                    contains("mall:auth:captcha:"), eq("abcd"), eq(300L), eq(TimeUnit.SECONDS));
        }
    }

    @Test
    void shouldGenerateUniqueCaptchaKeyEachTime() {
        try (MockedConstruction<SpecCaptcha> mocked = mockConstruction(SpecCaptcha.class,
                (mock, context) -> {
                    when(mock.text()).thenReturn("abcd");
                    when(mock.toBase64()).thenReturn("data:image/png;base64,Key");
                })) {

            Map<String, String> result1 = captchaService.generate();
            Map<String, String> result2 = captchaService.generate();

            assertNotEquals(result1.get("captchaKey"), result2.get("captchaKey"));
        }
    }

    // ========== verify() 参数空检查测试 ==========

    @Test
    void shouldVerifyThrowWhenCaptchaKeyIsNull() {
        CaptchaException exception = assertThrows(CaptchaException.class,
                () -> captchaService.verify(null, "1234", "127.0.0.1"));
        assertEquals(ErrorCode.PARAM_MISSING.getCode(), exception.getErrorCode());
        assertEquals("请完整填写信息", exception.getUserTip());
    }

    @Test
    void shouldVerifyThrowWhenCaptchaCodeIsNull() {
        CaptchaException exception = assertThrows(CaptchaException.class,
                () -> captchaService.verify("key-123", null, "127.0.0.1"));
        assertEquals(ErrorCode.PARAM_MISSING.getCode(), exception.getErrorCode());
    }

    @Test
    void shouldVerifyThrowWhenCaptchaKeyIsEmpty() {
        CaptchaException exception = assertThrows(CaptchaException.class,
                () -> captchaService.verify("", "1234", "127.0.0.1"));
        assertEquals(ErrorCode.PARAM_MISSING.getCode(), exception.getErrorCode());
    }

    @Test
    void shouldVerifyThrowWhenCaptchaCodeIsEmpty() {
        CaptchaException exception = assertThrows(CaptchaException.class,
                () -> captchaService.verify("key-123", "", "127.0.0.1"));
        assertEquals(ErrorCode.PARAM_MISSING.getCode(), exception.getErrorCode());
    }

    // ========== verify() IP 防刷测试 ==========

    @Test
    void shouldVerifyThrowWhenIpExceedsLimit() {
        when(valueOperations.get(eq("mall:auth:captcha:ip:127.0.0.1"))).thenReturn(10);

        CaptchaException exception = assertThrows(CaptchaException.class,
                () -> captchaService.verify("key-123", "abcd", "127.0.0.1"));
        assertEquals(ErrorCode.CAPTCHA_RETRY_LIMIT.getCode(), exception.getErrorCode());
        assertEquals("验证码尝试次数过多", exception.getUserTip());
    }

    // ========== verify() 验证码过期测试 ==========

    @Test
    void shouldVerifyThrowWhenCaptchaExpired() {
        when(valueOperations.get(eq("mall:auth:captcha:ip:127.0.0.1"))).thenReturn(3);
        when(valueOperations.get(eq("mall:auth:captcha:key-123"))).thenReturn(null);
        when(valueOperations.increment(anyString(), eq(1L))).thenReturn(4L);

        CaptchaException exception = assertThrows(CaptchaException.class,
                () -> captchaService.verify("key-123", "abcd", "127.0.0.1"));
        assertEquals(ErrorCode.CAPTCHA_EXPIRED.getCode(), exception.getErrorCode());
        assertEquals("验证码已过期", exception.getUserTip());
        verify(valueOperations).increment(eq("mall:auth:captcha:ip:127.0.0.1"), eq(1L));
    }

    // ========== verify() 验证码不匹配测试 ==========

    @Test
    void shouldVerifyThrowWhenCodeMismatchAndIncrementIp() {
        when(valueOperations.get(eq("mall:auth:captcha:ip:127.0.0.1"))).thenReturn(3);
        when(valueOperations.get(eq("mall:auth:captcha:key-123"))).thenReturn("correct");
        when(valueOperations.increment(anyString(), eq(1L))).thenReturn(4L);

        CaptchaException exception = assertThrows(CaptchaException.class,
                () -> captchaService.verify("key-123", "wrong", "127.0.0.1"));
        assertEquals(ErrorCode.CAPTCHA_WRONG.getCode(), exception.getErrorCode());
        assertEquals("验证码错误", exception.getUserTip());
        verify(valueOperations).increment(eq("mall:auth:captcha:ip:127.0.0.1"), eq(1L));
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void shouldVerifySuccessAndDeleteKey() {
        when(valueOperations.get(eq("mall:auth:captcha:ip:127.0.0.1"))).thenReturn(0);
        when(valueOperations.get(eq("mall:auth:captcha:key-123"))).thenReturn("abcd");
        when(redisTemplate.delete(eq("mall:auth:captcha:key-123"))).thenReturn(true);

        assertDoesNotThrow(() -> captchaService.verify("key-123", "abcd", "127.0.0.1"));

        verify(redisTemplate).delete(eq("mall:auth:captcha:key-123"));
        verify(valueOperations, never()).increment(anyString(), anyLong());
    }

    @Test
    void shouldVerifyCaseInsensitiveMatch() {
        when(valueOperations.get(eq("mall:auth:captcha:ip:127.0.0.1"))).thenReturn(0);
        when(valueOperations.get(eq("mall:auth:captcha:key-123"))).thenReturn("abcd");
        when(redisTemplate.delete(eq("mall:auth:captcha:key-123"))).thenReturn(true);

        assertDoesNotThrow(() -> captchaService.verify("key-123", "ABCD", "127.0.0.1"));

        verify(redisTemplate).delete(eq("mall:auth:captcha:key-123"));
    }

    // ========== verify() IP 计数细节测试 ==========

    @Test
    void shouldIpCountSetExpireOnlyOnFirstIncrement() {
        when(valueOperations.get(eq("mall:auth:captcha:ip:127.0.0.1"))).thenReturn(0);
        when(valueOperations.get(eq("mall:auth:captcha:key-123"))).thenReturn("stored");
        when(valueOperations.increment(eq("mall:auth:captcha:ip:127.0.0.1"), eq(1L))).thenReturn(1L);
        when(redisTemplate.expire(eq("mall:auth:captcha:ip:127.0.0.1"), eq(86400L), eq(TimeUnit.SECONDS))).thenReturn(true);

        assertThrows(CaptchaException.class,
                () -> captchaService.verify("key-123", "wrong", "127.0.0.1"));
        verify(redisTemplate).expire(eq("mall:auth:captcha:ip:127.0.0.1"), eq(86400L), eq(TimeUnit.SECONDS));
    }

    @Test
    void shouldIpCountNotSetExpireOnSubsequentIncrements() {
        when(valueOperations.get(eq("mall:auth:captcha:ip:127.0.0.1"))).thenReturn(5);
        when(valueOperations.get(eq("mall:auth:captcha:key-123"))).thenReturn("stored");
        when(valueOperations.increment(eq("mall:auth:captcha:ip:127.0.0.1"), eq(1L))).thenReturn(6L);

        assertThrows(CaptchaException.class,
                () -> captchaService.verify("key-123", "wrong", "127.0.0.1"));
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }
}
