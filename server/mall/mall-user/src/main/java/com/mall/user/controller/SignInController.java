package com.mall.user.controller;

import static com.mall.common.constant.HeaderConstants.X_USER_ID;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mall.common.DTO.MallResult;
import com.mall.user.service.ISignInService;
import com.mall.user.VO.SignInVO;

/**
 * C 端签到控制器
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class SignInController {

    private final ISignInService signInService;

    /**
     * 执行每日签到
     *
     * @param request HTTP 请求
     * @return 签到结果
     */
    @PostMapping("/sign-in")
    public MallResult<SignInVO> signIn(HttpServletRequest request) {
        String userId = request.getHeader(X_USER_ID);
        return MallResult.success(signInService.signIn(Long.parseLong(userId)));
    }
}
