package com.mall.common.exception;

import com.mall.common.enums.ErrorCode;

/**
 * C 端验证码异常
 *
 * <p>图形验证码校验失败时抛出，由 {@link com.mall.common.handler.MallExceptionHandler} 统一捕获</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public class CaptchaException extends RuntimeException {

    /** 错误码 */
    private final String errorCode;
    /** 用户提示 */
    private final String userTip;

    /**
     * 使用 ErrorCode 构造验证码异常
     *
     * @param errorCode 错误码枚举
     */
    public CaptchaException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode.getCode();
        this.userTip = errorCode.getUserTip();
    }

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 获取用户提示
     *
     * @return 用户提示
     */
    public String getUserTip() {
        return userTip;
    }
}
