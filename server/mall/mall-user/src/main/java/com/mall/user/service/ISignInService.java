package com.mall.user.service;

import com.mall.user.vo.SignInVO;

/**
 * C 端签到服务接口
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
public interface ISignInService {

    /**
     * 当日签到
     *
     * @param userId 用户ID
     * @return 签到结果（今日积分、连续天数、签到日历）
     */
    SignInVO signIn(Long userId);
}
