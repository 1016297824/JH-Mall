package com.mall.auth.controller;

import com.mall.common.dto.MallResult;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/users")
    public MallResult<Void> createUser() {
        throw new BusinessException(ErrorCode.NOT_OPEN);
    }

    @PostMapping("/sms_codes")
    public MallResult<Void> sendSmsCode() {
        throw new BusinessException(ErrorCode.NOT_OPEN);
    }

    @PostMapping("/sessions")
    public MallResult<Void> createSession() {
        throw new BusinessException(ErrorCode.NOT_OPEN);
    }

    @PostMapping("/sessions/sms")
    public MallResult<Void> createSessionBySms() {
        throw new BusinessException(ErrorCode.NOT_OPEN);
    }

    @PostMapping("/sessions/refresh")
    public MallResult<Void> refreshSession() {
        throw new BusinessException(ErrorCode.NOT_OPEN);
    }

    @GetMapping("/sessions/current")
    public MallResult<Void> getCurrentSession() {
        throw new BusinessException(ErrorCode.NOT_OPEN);
    }

    @DeleteMapping("/sessions/current")
    public MallResult<Void> deleteCurrentSession() {
        throw new BusinessException(ErrorCode.NOT_OPEN);
    }

    @PostMapping("/wechat/sessions")
    public MallResult<Void> createWechatSession() {
        throw new BusinessException(ErrorCode.NOT_OPEN);
    }

    @PostMapping("/wechat/phone_binding")
    public MallResult<Void> wechatPhoneBinding() {
        throw new BusinessException(ErrorCode.NOT_OPEN);
    }

    @PutMapping("/phone")
    public MallResult<Void> changePhone() {
        throw new BusinessException(ErrorCode.NOT_OPEN);
    }

    @PutMapping("/password/reset")
    public MallResult<Void> resetPassword() {
        throw new BusinessException(ErrorCode.NOT_OPEN);
    }

    @PutMapping("/password")
    public MallResult<Void> changePassword() {
        throw new BusinessException(ErrorCode.NOT_OPEN);
    }

    @DeleteMapping("/account")
    public MallResult<Void> deleteAccount() {
        throw new BusinessException(ErrorCode.NOT_OPEN);
    }
}
