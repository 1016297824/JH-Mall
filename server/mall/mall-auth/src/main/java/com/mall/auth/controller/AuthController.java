package com.mall.auth.controller;

import com.mall.common.DTO.MallResult;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端认证接口（占位，尚未开放）
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

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
     * 刷新 Token（暂未开放）
     *
     * @return 错误响应
     */
    @PostMapping("/sessions/refresh")
    public MallResult<Void> refreshSession() {
        throw new BusinessException(ErrorCode.NOT_OPEN);
    }

    /**
     * 获取当前会话（暂未开放）
     *
     * @return 错误响应
     */
    @GetMapping("/sessions/current")
    public MallResult<Void> getCurrentSession() {
        throw new BusinessException(ErrorCode.NOT_OPEN);
    }

    /**
     * 登出（暂未开放）
     *
     * @return 错误响应
     */
    @DeleteMapping("/sessions/current")
    public MallResult<Void> deleteCurrentSession() {
        throw new BusinessException(ErrorCode.NOT_OPEN);
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
}
