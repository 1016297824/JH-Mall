package com.mall.user.service.impl;

import com.mall.common.constant.CacheConstants;
import com.mall.user.config.MallUserConfigProperties;
import com.mall.user.service.ISignInService;
import com.mall.user.vo.SignInVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * C 端签到服务实现
 *
 * <p>基于 Redis Bitmap 实现签到，支持连续签到积分递增（5~10），幂等防重</p>
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignInServiceImpl implements ISignInService {

    private static final DateTimeFormatter YM_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final RedisTemplate<String, Object> redisTemplate;

    private final MallUserConfigProperties configProperties;

    @Override
    public SignInVO signIn(Long userId) {
        LocalDate today = LocalDate.now();
        String yearMonth = today.format(YM_FORMATTER);
        String key = CacheConstants.User.SIGN + userId + ":" + yearMonth;
        int offset = today.getDayOfMonth() - 1;

        Boolean signed = redisTemplate.opsForValue().getBit(key, offset);
        if (Boolean.TRUE.equals(signed)) {
            int consecutiveDays = calcConsecutiveDays(key, offset, today);
            int points = calcPoints(consecutiveDays);
            List<Integer> calendar = buildCalendar(key, today);

            SignInVO vo = new SignInVO();
            vo.setTodayPoints(points);
            vo.setConsecutiveDays(consecutiveDays + 1);
            vo.setSignInCalendar(calendar);
            return vo;
        }

        int consecutiveDays = calcConsecutiveDays(key, offset, today);
        int points = calcPoints(consecutiveDays);

        redisTemplate.opsForValue().setBit(key, offset, true);
        redisTemplate.expire(key, 60, TimeUnit.DAYS);

        List<Integer> calendar = buildCalendar(key, today);

        SignInVO vo = new SignInVO();
        vo.setTodayPoints(points);
        vo.setConsecutiveDays(consecutiveDays + 1);
        vo.setSignInCalendar(calendar);
        return vo;
    }

    /**
     * 计算连续签到天数（从昨天向前倒推）
     */
    private int calcConsecutiveDays(String key, int offset, LocalDate today) {
        int consecutive = 0;
        LocalDate date = today.minusDays(1);
        while (date.getMonthValue() == today.getMonthValue()) {
            int off = date.getDayOfMonth() - 1;
            Boolean bit = redisTemplate.opsForValue().getBit(key, off);
            if (!Boolean.TRUE.equals(bit)) {
                break;
            }
            consecutive++;
            date = date.minusDays(1);
        }
        return consecutive;
    }

    /**
     * 计算签到积分：基础5 + 每连续一天加1，上限10
     */
    private int calcPoints(int consecutiveDays) {
        MallUserConfigProperties.Points pointsConfig = configProperties.getPoints();
        int base = pointsConfig.getSigninBase();
        int max = pointsConfig.getSigninConsecutive();
        int bonus = pointsConfig.getSigninConsecutiveBonus();
        return Math.min(base + consecutiveDays * bonus, max);
    }

    /**
     * 构建当月签到日历
     */
    private List<Integer> buildCalendar(String key, LocalDate today) {
        List<Integer> calendar = new ArrayList<>();
        int totalDays = today.lengthOfMonth();
        for (int d = 1; d <= totalDays; d++) {
            Boolean bit = redisTemplate.opsForValue().getBit(key, d - 1);
            if (Boolean.TRUE.equals(bit)) {
                calendar.add(d);
            }
        }
        if (!calendar.contains(today.getDayOfMonth())) {
            calendar.add(today.getDayOfMonth());
        }
        return calendar;
    }
}
