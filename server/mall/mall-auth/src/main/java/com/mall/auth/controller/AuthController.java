package com.mall.auth.controller;

import com.mall.api.dto.MallResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String NOT_OPEN_CODE = "A9999";
    private static final String NOT_OPEN_MSG = "该功能暂未开放，请使用 CAPTCHA 端点";

    @PostMapping("/users")
    public MallResult<Void> createUser() {
        return MallResult.error(NOT_OPEN_CODE, NOT_OPEN_MSG);
    }

    @PostMapping("/sms_codes")
    public MallResult<Void> sendSmsCode() {
        return MallResult.error(NOT_OPEN_CODE, NOT_OPEN_MSG);
    }

    @PostMapping("/sessions")
    public MallResult<Void> createSession() {
        return MallResult.error(NOT_OPEN_CODE, NOT_OPEN_MSG);
    }

    @PostMapping("/sessions/sms")
    public MallResult<Void> createSessionBySms() {
        return MallResult.error(NOT_OPEN_CODE, NOT_OPEN_MSG);
    }

    @PostMapping("/sessions/refresh")
    public MallResult<Void> refreshSession() {
        return MallResult.error(NOT_OPEN_CODE, NOT_OPEN_MSG);
    }

    @GetMapping("/sessions/current")
    public MallResult<Void> getCurrentSession() {
        return MallResult.error(NOT_OPEN_CODE, NOT_OPEN_MSG);
    }

    @DeleteMapping("/sessions/current")
    public MallResult<Void> deleteCurrentSession() {
        return MallResult.error(NOT_OPEN_CODE, NOT_OPEN_MSG);
    }

    @PostMapping("/wechat/sessions")
    public MallResult<Void> createWechatSession() {
        return MallResult.error(NOT_OPEN_CODE, NOT_OPEN_MSG);
    }

    @PostMapping("/wechat/phone_binding")
    public MallResult<Void> wechatPhoneBinding() {
        return MallResult.error(NOT_OPEN_CODE, NOT_OPEN_MSG);
    }

    @PutMapping("/phone")
    public MallResult<Void> changePhone() {
        return MallResult.error(NOT_OPEN_CODE, NOT_OPEN_MSG);
    }

    @PutMapping("/password/reset")
    public MallResult<Void> resetPassword() {
        return MallResult.error(NOT_OPEN_CODE, NOT_OPEN_MSG);
    }

    @PutMapping("/password")
    public MallResult<Void> changePassword() {
        return MallResult.error(NOT_OPEN_CODE, NOT_OPEN_MSG);
    }

    @DeleteMapping("/account")
    public MallResult<Void> deleteAccount() {
        return MallResult.error(NOT_OPEN_CODE, NOT_OPEN_MSG);
    }
}
