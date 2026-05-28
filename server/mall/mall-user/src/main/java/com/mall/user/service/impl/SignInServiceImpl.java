package com.mall.user.service.impl;

import com.mall.common.constant.CacheConstants;
import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.user.BizTypeEnum;
import com.mall.common.exception.BusinessException;
import com.mall.user.config.MallUserConfigProperties;
import com.mall.user.service.IPointsService;
import com.mall.user.service.ISignInService;
import com.mall.user.VO.SignInVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * C 端签到服务实现类
 *
 * <p>基于 Redis Bitmap + Lua 脚本实现原子签到，支持连续签到积分递增（5~10），幂等防重。
 * 签到后自动调用积分服务发放积分</p>
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignInServiceImpl implements ISignInService {

    /** 月份格式化器：yyyyMM */
    private static final DateTimeFormatter YM_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    /**
     * Lua 脚本：原子检查并设置签到位
     *
     * <p>ARGS[1] = offset（日偏移量），KEYS[1] = Redis key。
     * 返回值：0 = 已签到，1 = 签到成功</p>
     */
    private static final DefaultRedisScript<Long> SIGN_IN_SCRIPT;

    static {
        SIGN_IN_SCRIPT = new DefaultRedisScript<>();
        SIGN_IN_SCRIPT.setScriptText(
                "local offset = tonumber(ARGV[1]) " +
                "if redis.call('GETBIT', KEYS[1], offset) == 1 then return 0 end " +
                "redis.call('SETBIT', KEYS[1], offset, 1) " +
                "return 1");
        SIGN_IN_SCRIPT.setResultType(Long.class);
    }

    private final RedisTemplate<String, Object> redisTemplate;

    private final MallUserConfigProperties configProperties;

    private final IPointsService pointsService;

    /**
     * 执行签到
     *
     * <p>执行流程：
     * <ol>
     *   <li>Lua 原子检查并设置当天的签到位</li>
     *   <li>已签到则抛出 RESOURCE_EXISTS 异常（幂等防重）</li>
     *   <li>计算连续签到天数，按规则计算积分（基础 5 + 连续加成，上限 10）</li>
     *   <li>设置 Redis key 过期时间为 60 天</li>
     *   <li>调用积分服务发放积分</li>
     *   <li>构建当月签到日历返回</li>
     * </ol>
     * </p>
     *
     * @param userId 用户 ID
     * @return 签到结果 VO，包含本次积分、连续天数、签到日历
     * @throws BusinessException 当天已签到时抛出 RESOURCE_EXISTS
     */
    @Override
    public SignInVO signIn(Long userId) {
        LocalDate today = LocalDate.now();
        String yearMonth = today.format(YM_FORMATTER);
        String key = CacheConstants.User.SIGN + userId + ":" + yearMonth;
        int offset = today.getDayOfMonth() - 1;

        Long result = redisTemplate.execute(SIGN_IN_SCRIPT,
                Collections.singletonList(key), String.valueOf(offset));
        if (result == null || result == 0) {
            throw new BusinessException(ErrorCode.RESOURCE_EXISTS);
        }

        int consecutiveDays = calcConsecutiveDays(key, offset, today);
        int points = calcPoints(consecutiveDays);

        redisTemplate.expire(key, 60, TimeUnit.DAYS);

        pointsService.addPoints(userId, points, BizTypeEnum.SIGN_IN, null);

        List<Integer> calendar = buildCalendar(key, today);

        SignInVO vo = new SignInVO();
        vo.setTodayPoints(points);
        vo.setConsecutiveDays(consecutiveDays + 1);
        vo.setSignInCalendar(calendar);
        return vo;
    }

    /**
     * 计算连续签到天数
     *
     * <p>从昨天开始向前倒推，统计连续为 1 的 bit 位数，遇 0 或跨月则停止</p>
     *
     * @param key    Redis 签到 key
     * @param offset 今天的日偏移量
     * @param today  当天日期
     * @return 连续签到天数（不含今天）
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
     * 计算签到所得积分
     *
     * <p>规则：基础积分 + 连续天数 × 加成积分，上限为连续签到最大积分</p>
     *
     * @param consecutiveDays 连续签到天数（不含今天）
     * @return 本次签到积分
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
     *
     * <p>遍历当月每一天，已签到的日期加入列表，确保当天一定在列表中</p>
     *
     * @param key   Redis 签到 key
     * @param today 当天日期
     * @return 当月已签到日期列表
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
        // 确保今天一定在日历中
        if (!calendar.contains(today.getDayOfMonth())) {
            calendar.add(today.getDayOfMonth());
        }
        return calendar;
    }
}
