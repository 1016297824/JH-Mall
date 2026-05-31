package com.mall.auth.controller;

import com.mall.auth.DTO.request.RefreshTokenReqDTO;
import com.mall.auth.DTO.response.SessionInfoRespDTO;
import com.mall.auth.DTO.response.TokenRespDTO;
import com.mall.auth.service.ITokenService;
import com.mall.common.DTO.MallResult;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端认证接口
 *
 * <p>提供注册、登录、Token 管理、密码管理、账号管理能力。短信/微信登录暂未开放（占位返回 NOT_OPEN）。</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    /** Token 服务 */
    private final ITokenService tokenService;

    /** Authorization Header 前缀 */
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 创建用户（暂未开放）
     *
     * @return 错误响应
     */
    @PostMapping("/users")
    public MallResult<Void> createUser() {
        throw new BusinessException(ErrorCode.NOT_OPEN);
    }

    /**
     * 发送短信验证码（暂未开放）
     *
     * @return 错误响应
     */
    @PostMapping("/sms_codes")
    public MallResult<Void> sendSmsCode() {
        throw new BusinessException(ErrorCode.NOT_OPEN);
    }

    /**
     * 密码登录（暂未开放）
     *
     * @return 错误响应
     */
    @PostMapping("/sessions")
    public MallResult<Void> createSession() {
        throw new BusinessException(ErrorCode.NOT_OPEN);
    }

    /**
     * 短信登录（暂未开放）
     *
     * @return 错误响应
     */
    @PostMapping("/sessions/sms")
    public MallResult<Void> createSessionBySms() {
        throw new BusinessException(ErrorCode.NOT_OPEN);
    }

    /**
     * 刷新 Token（refreshToken 一次性轮换）
     *
     * @param req 刷新请求
     * @return 新的 Token 响应
     */
    @PostMapping("/sessions/refresh")
    public MallResult<TokenRespDTO> refreshSession(@Valid @RequestBody RefreshTokenReqDTO req) {
        TokenRespDTO token = tokenService.refresh(req.getRefreshToken());
        return MallResult.success(token);
    }

    /**
     * 获取当前会话（校验 Token 有效性）
     *
     * @param request HTTP 请求
     * @return 会话信息
     */
    @GetMapping("/sessions/current")
    public MallResult<SessionInfoRespDTO> getCurrentSession(HttpServletRequest request) {
        String accessToken = extractBearerToken(request);
        String userId = tokenService.verify(accessToken);
        return MallResult.success(new SessionInfoRespDTO(userId));
    }

    /**
     * 登出
     *
     * @param request HTTP 请求
     * @return 成功响应
     */
    @DeleteMapping("/sessions/current")
    public MallResult<Void> deleteCurrentSession(HttpServletRequest request) {
        String accessToken = extractBearerToken(request);
        tokenService.revoke(accessToken);
        return MallResult.success(null);
    }

    /**
     * 微信登录（暂未开放）
     *
     * @return 错误响应
     */
    @PostMapping("/wechat/sessions")
    public MallResult<Void> createWechatSession() {
        throw new BusinessException(ErrorCode.NOT_OPEN);
    }

    /**
     * 微信手机号绑定（暂未开放）
     *
     * @return 错误响应
     */
    @PostMapping("/wechat/phone_binding")
    public MallResult<Void> wechatPhoneBinding() {
        throw new BusinessException(ErrorCode.NOT_OPEN);
    }

    /**
     * 更换手机号（暂未开放）
     *
     * @return 错误响应
     */
    @PutMapping("/phone")
    public MallResult<Void> changePhone() {
        throw new BusinessException(ErrorCode.NOT_OPEN);
    }

    /**
     * 重置密码（暂未开放）
     *
     * @return 错误响应
     */
    @PutMapping("/password/reset")
    public MallResult<Void> resetPassword() {
        throw new BusinessException(ErrorCode.NOT_OPEN);
    }

    /**
     * 修改密码（暂未开放）
     *
     * @return 错误响应
     */
    @PutMapping("/password")
    public MallResult<Void> changePassword() {
        throw new BusinessException(ErrorCode.NOT_OPEN);
    }

    /**
     * 注销账户（暂未开放）
     *
     * @return 错误响应
     */
    @DeleteMapping("/account")
    public MallResult<Void> deleteAccount() {
        throw new BusinessException(ErrorCode.NOT_OPEN);
    }

    /**
     * 从 Authorization Header 中提取 Bearer Token
     *
     * @param request HTTP 请求
     * @return accessToken
     */
    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        return header.substring(BEARER_PREFIX.length());
    }
}
