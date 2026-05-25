package com.mall.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MallResult<T> {

    private static final String SUCCESS_CODE = "00000";
    private static final String SUCCESS_MSG = "操作成功";

    private String errorCode;
    private String errorMessage;
    private String userTip;
    private T data;
    private String requestId;

    public static <T> MallResult<T> success(T data) {
        MallResult<T> result = new MallResult<>();
        result.errorCode = SUCCESS_CODE;
        result.errorMessage = SUCCESS_MSG;
        result.userTip = null;
        result.data = data;
        return result;
    }

    public static <T> MallResult<T> error(String errorCode, String errorMessage) {
        MallResult<T> result = new MallResult<>();
        result.errorCode = errorCode;
        result.errorMessage = errorMessage;
        return result;
    }

    public static <T> MallResult<T> error(String errorCode, String errorMessage, String userTip) {
        MallResult<T> result = error(errorCode, errorMessage);
        result.userTip = userTip;
        return result;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getUserTip() {
        return userTip;
    }

    public void setUserTip(String userTip) {
        this.userTip = userTip;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

}
