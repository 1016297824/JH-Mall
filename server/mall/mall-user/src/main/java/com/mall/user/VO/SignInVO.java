package com.mall.user.VO;

import java.util.List;

import lombok.Data;

/**
 * 签到响应
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Data
public class SignInVO {

    /** 今日签到获得积分 */
    private Integer todayPoints;

    /** 连续签到天数 */
    private Integer consecutiveDays;

    /** 当月签到日期列表 */
    private List<Integer> signInCalendar;
}
