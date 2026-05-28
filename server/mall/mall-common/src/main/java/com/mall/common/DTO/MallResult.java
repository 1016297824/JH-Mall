package com.mall.common.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * C 端统一响应体
 *
 * <p>成功时不返回 {@code errorCode} 和 {@code userTip}（{@code @JsonInclude(NON_NULL)}）。
 * 失败时 {@code errorCode} 不为 {@code "00000"}。</p>
 *
 * @param <T> 响应数据泛型
 * @author JH-Mall
 * @date 2026/05/26
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MallResult<T> {

    private static final String SUCCESS_CODE = "00000";
    private static final String SUCCESS_MSG = "操作成功";

    /** 错误码（成功时为 "00000"） */
    private String errorCode;
    /** 错误消息（日志用） */
    private String errorMessage;
    /** 用户提示（仅失败时填充） */
    private String userTip;
    /** 响应数据 */
    private T data;
    /** 全链路请求 ID */
    private String requestId;

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @param <T>  泛型类型
     * @return 成功 MallResult
     */
    public static <T> MallResult<T> success(T data) {
        MallResult<T> result = new MallResult<>();
        result.errorCode = SUCCESS_CODE;
        result.errorMessage = SUCCESS_MSG;
        result.userTip = null;
        result.data = data;
        return result;
    }

    /**
     * 错误响应（双参）
     *
     * @param errorCode    错误码
     * @param errorMessage 错误消息
     * @param <T>          泛型类型
     * @return 错误 MallResult
     */
    public static <T> MallResult<T> error(String errorCode, String errorMessage) {
        MallResult<T> result = new MallResult<>();
        result.errorCode = errorCode;
        result.errorMessage = errorMessage;
        return result;
    }

    /**
     * 错误响应（三参，含用户提示）
     *
     * @param errorCode    错误码
     * @param errorMessage 错误消息
     * @param userTip      用户提示
     * @param <T>          泛型类型
     * @return 错误 MallResult
     */
    public static <T> MallResult<T> error(String errorCode, String errorMessage, String userTip) {
        MallResult<T> result = error(errorCode, errorMessage);
        result.userTip = userTip;
        return result;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

}
