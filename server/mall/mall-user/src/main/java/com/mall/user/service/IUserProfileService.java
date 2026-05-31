package com.mall.user.service;

import com.mall.user.DTO.request.UpdateProfileDTO;
import com.mall.user.VO.UserProfileVO;

/**
 * 用户资料服务接口
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
public interface IUserProfileService {

    /**
     * 查询用户资料（Redis 缓存 → DB 兜底）
     *
     * @param userId 用户ID
     * @return 用户资料VO
     */
    UserProfileVO getProfile(Long userId);

    /**
     * 修改用户资料（删缓存 → 更新 → 返回最新资料）
     *
     * @param userId  用户ID
     * @param request 修改请求
     * @return 更新后的用户资料VO
     */
    UserProfileVO updateProfile(Long userId, UpdateProfileDTO request);
}
