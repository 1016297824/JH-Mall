package com.mall.user.service.impl;

import com.mall.user.config.MallUserConfigProperties;
import com.mall.user.vo.SignInVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SignInServiceImpl 单元测试
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@ExtendWith(MockitoExtension.class)
class SignInServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock(lenient = true)
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private MallUserConfigProperties configProperties;

    @Mock
    private MallUserConfigProperties.Points pointsConfig;

    @InjectMocks
    private SignInServiceImpl signInService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(configProperties.getPoints()).thenReturn(pointsConfig);
        when(pointsConfig.getSigninBase()).thenReturn(5);
        when(pointsConfig.getSigninConsecutive()).thenReturn(10);
        when(pointsConfig.getSigninConsecutiveBonus()).thenReturn(1);
    }

    @Test
    void signIn_shouldReturnPointsAndCalendar_whenFirstSignIn() {
        lenient().when(valueOperations.getBit(anyString(), anyLong())).thenReturn(false);

        SignInVO result = signInService.signIn(1L);

        assertNotNull(result);
        assertEquals(5, result.getTodayPoints());
        assertEquals(1, result.getConsecutiveDays());
        assertNotNull(result.getSignInCalendar());
    }

    @Test
    void signIn_shouldCalculateConsecutiveDays() {
        int todayOffset = LocalDate.now().getDayOfMonth() - 1;
        lenient().when(valueOperations.getBit(anyString(), anyLong())).thenReturn(false);

        SignInVO result = signInService.signIn(1L);

        assertNotNull(result);
        assertEquals(1, result.getConsecutiveDays());
    }

    @Test
    void signIn_shouldCapPointsAtMax() {
        lenient().when(valueOperations.getBit(anyString(), anyLong())).thenReturn(false);

        SignInVO result = signInService.signIn(1L);

        assertTrue(result.getTodayPoints() <= 10);
    }

    @Test
    void signIn_shouldReturnCalendar() {
        lenient().when(valueOperations.getBit(anyString(), anyLong())).thenReturn(false);

        SignInVO result = signInService.signIn(1L);

        List<Integer> calendar = result.getSignInCalendar();
        assertNotNull(calendar);
        assertTrue(calendar.contains(LocalDate.now().getDayOfMonth()));
    }
}
