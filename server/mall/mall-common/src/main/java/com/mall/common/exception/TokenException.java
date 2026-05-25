package com.mall.common.exception;

public class TokenException extends RuntimeException {

    private final String errorCode;
    private final String userTip;

    public TokenException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.userTip = message;
    }

    public TokenException(String errorCode, String message, String userTip) {
        super(message);
        this.errorCode = errorCode;
        this.userTip = userTip;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getUserTip() {
        return userTip;
    }
}
