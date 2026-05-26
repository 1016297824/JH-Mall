package com.mall.common.handler;

import com.mall.common.dto.MallResult;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.common.exception.CaptchaException;
import com.mall.common.exception.TokenException;
import feign.FeignException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MallExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(MallExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public MallResult<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: {} - {}", e.getErrorCode(), e.getMessage());
        return MallResult.error(e.getErrorCode(), e.getMessage(), e.getUserTip());
    }

    @ExceptionHandler(CaptchaException.class)
    public MallResult<Void> handleCaptcha(CaptchaException e) {
        return MallResult.error(e.getErrorCode(), e.getMessage(), e.getUserTip());
    }

    @ExceptionHandler(TokenException.class)
    public MallResult<Void> handleToken(TokenException e) {
        log.warn("Token 异常: {} - {}", e.getErrorCode(), e.getMessage());
        return MallResult.error(e.getErrorCode(), e.getMessage(), e.getUserTip());
    }

    @ExceptionHandler(FeignException.class)
    public MallResult<Void> handleFeign(FeignException e) {
        log.error("Feign 调用异常: {}", e.getMessage());
        return MallResult.error(ErrorCode.SYSTEM_ERROR.getCode(), "服务暂时不可用，请稍后重试");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public MallResult<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : ErrorCode.PARAM_MISSING.getUserTip();
        return MallResult.error(ErrorCode.PARAM_MISSING.getCode(), msg, msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public MallResult<Void> handleConstraintViolation(ConstraintViolationException e) {
        return MallResult.error(ErrorCode.PARAM_MISSING.getCode(), e.getMessage(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public MallResult<Void> handleException(Exception e) {
        log.error("unhandled exception", e);
        return MallResult.error(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getUserTip());
    }
}
