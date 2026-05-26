package com.mall.common.exception;

import com.mall.common.enums.ErrorCode;

public class CaptchaException extends RuntimeException {

    private final String errorCode;
    private final String userTip;

    public CaptchaException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode.getCode();
        this.userTip = errorCode.getUserTip();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getUserTip() {
        return userTip;
    }
}
