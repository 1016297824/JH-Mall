package com.mall.user.controller;

import static com.mall.common.constant.HeaderConstants.X_USER_ID;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mall.common.DTO.MallResult;
import com.mall.user.dto.request.UpdateProfileRequest;
import com.mall.user.service.IUserProfileService;
import com.mall.user.vo.UserProfileVO;

/**
 * C 端用户资料控制器
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserProfileController {

    private final IUserProfileService userProfileService;

    /**
     * 查询用户资料
     *
     * @param request HTTP 请求
     * @return 用户资料
     */
    @GetMapping("/profile")
    public MallResult<UserProfileVO> getProfile(HttpServletRequest request) {
        String userId = request.getHeader(X_USER_ID);
        return MallResult.success(userProfileService.getProfile(Long.parseLong(userId)));
    }

    /**
     * 修改用户资料
     *
     * @param request 修改请求
     * @param httpRequest HTTP 请求
     * @return 最新用户资料
     */
    @PutMapping("/profile")
    public MallResult<UserProfileVO> updateProfile(@RequestBody UpdateProfileRequest request,
                                                   HttpServletRequest httpRequest) {
        String userId = httpRequest.getHeader(X_USER_ID);
        return MallResult.success(userProfileService.updateProfile(Long.parseLong(userId), request));
    }
}
