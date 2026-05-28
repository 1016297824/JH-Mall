package com.mall.user.service.impl;

import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.user.BizTypeEnum;
import com.mall.common.exception.BusinessException;
import com.mall.user.config.MallUserConfigProperties;
import com.mall.user.service.IPointsService;
import com.mall.user.VO.SignInVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

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

    @Mock
    private IPointsService pointsService;

    @InjectMocks
    private SignInServiceImpl signInService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(configProperties.getPoints()).thenReturn(pointsConfig);
        lenient().when(pointsConfig.getSigninBase()).thenReturn(5);
        lenient().when(pointsConfig.getSigninConsecutive()).thenReturn(10);
        lenient().when(pointsConfig.getSigninConsecutiveBonus()).thenReturn(1);
        lenient().when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(1L);
        lenient().when(valueOperations.getBit(anyString(), anyLong())).thenReturn(false);
    }

    @Test
    void signIn_shouldReturnPointsAndCalendar_whenFirstSignIn() {
        SignInVO result = signInService.signIn(1L);

        assertNotNull(result);
        assertEquals(5, result.getTodayPoints());
        assertEquals(1, result.getConsecutiveDays());
        assertNotNull(result.getSignInCalendar());
        verify(pointsService).addPoints(eq(1L), eq(5), eq(BizTypeEnum.SIGN_IN), isNull());
    }

    @Test
    void signIn_shouldThrowWhenAlreadySigned() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(0L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> signInService.signIn(1L));
        assertEquals(ErrorCode.RESOURCE_EXISTS.getCode(), ex.getErrorCode());
    }

    @Test
    void signIn_shouldCalculateConsecutiveDays() {
        SignInVO result = signInService.signIn(1L);

        assertNotNull(result);
        assertEquals(1, result.getConsecutiveDays());
    }

    @Test
    void signIn_shouldCapPointsAtMax() {
        SignInVO result = signInService.signIn(1L);

        assertTrue(result.getTodayPoints() <= 10);
    }

    @Test
    void signIn_shouldReturnCalendar() {
        SignInVO result = signInService.signIn(1L);

        List<Integer> calendar = result.getSignInCalendar();
        assertNotNull(calendar);
        assertTrue(calendar.contains(LocalDate.now().getDayOfMonth()));
    }
}
