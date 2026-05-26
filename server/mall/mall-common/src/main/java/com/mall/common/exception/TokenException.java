package com.mall.common.exception;

import com.mall.common.enums.ErrorCode;

/**
 * C 端 Token 异常
 *
 * <p>JWT 校验失败（过期、签名错误、黑名单命中、session 缺失）时抛出，由 {@link com.mall.common.handler.MallExceptionHandler} 统一捕获</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public class TokenException extends RuntimeException {

    /** 错误码 */
    private final String errorCode;
    /** 用户提示 */
    private final String userTip;

    /**
     * 使用 ErrorCode 构造 Token 异常
     *
     * @param errorCode 错误码枚举
     */
    public TokenException(ErrorCode errorCode) {
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
