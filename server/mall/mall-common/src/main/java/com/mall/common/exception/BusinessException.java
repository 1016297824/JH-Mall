package com.mall.common.exception;

import com.mall.common.enums.ErrorCode;

/**
 * C 端业务异常
 *
 * <p>所有业务校验不通过时抛出此异常，由 {@link com.mall.common.handler.MallExceptionHandler} 统一捕获并转换为 MallResult</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public class BusinessException extends RuntimeException {

    /** 错误码 */
    private final String errorCode;
    /** 用户提示 */
    private final String userTip;

    /**
     * 使用 ErrorCode 构造业务异常
     *
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
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
