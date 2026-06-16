package com.mall.common.handler;

import com.mall.common.DTO.MallResult;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * C 端全局异常处理器
 *
 * <p>最高优先级（{@code @Order(HIGHEST_PRECEDENCE)}），统一拦截 C 端（mall-*）的所有异常并转换为 {@link MallResult}。
 * 包含业务异常、验证码异常、Token 异常、Feign 调用异常、参数校验异常及兜底异常的处理。</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MallExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(MallExceptionHandler.class);

    /**
     * 处理业务异常
     *
     * @param e 业务异常
     * @return 错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<MallResult<Void>> handleBusiness(BusinessException e) {
        log.warn("业务异常: {} - {}", e.getErrorCode(), e.getMessage());
        MallResult<Void> body = MallResult.error(e.getErrorCode(), e.getMessage(), e.getUserTip());
        return ResponseEntity.status(e.getErrorCodeEnum().getHttpStatus()).body(body);
    }

    /**
     * 处理验证码异常
     *
     * @param e 验证码异常
     * @return 错误响应
     */
    @ExceptionHandler(CaptchaException.class)
    public ResponseEntity<MallResult<Void>> handleCaptcha(CaptchaException e) {
        MallResult<Void> body = MallResult.error(e.getErrorCode(), e.getMessage(), e.getUserTip());
        return ResponseEntity.status(e.getErrorCodeEnum().getHttpStatus()).body(body);
    }

    /**
     * 处理 Token 异常
     *
     * @param e Token 异常
     * @return 错误响应
     */
    @ExceptionHandler(TokenException.class)
    public ResponseEntity<MallResult<Void>> handleToken(TokenException e) {
        log.warn("Token 异常: {} - {}", e.getErrorCode(), e.getMessage());
        MallResult<Void> body = MallResult.error(e.getErrorCode(), e.getMessage(), e.getUserTip());
        return ResponseEntity.status(e.getErrorCodeEnum().getHttpStatus()).body(body);
    }

    /**
     * 处理 Feign 调用异常
     *
     * @param e Feign 异常
     * @return 错误响应
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<MallResult<Void>> handleFeign(FeignException e) {
        log.error("Feign 调用异常: {}", e.getMessage());
        MallResult<Void> body = MallResult.error(ErrorCode.SYSTEM_ERROR.getCode(), "服务暂时不可用，请稍后重试");
        return ResponseEntity.status(ErrorCode.SYSTEM_ERROR.getHttpStatus()).body(body);
    }

    /**
     * 处理 @Valid 参数校验异常
     *
     * @param e 参数校验异常
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MallResult<Void>> handleValidation(MethodArgumentNotValidException e) {
        // 优先取 @NotBlank/@NotNull 等注解的 message，无具体字段错误时使用默认
        String msg = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : ErrorCode.PARAM_MISSING.getUserTip();
        MallResult<Void> body = MallResult.error(ErrorCode.PARAM_MISSING.getCode(), msg, msg);
        return ResponseEntity.status(ErrorCode.PARAM_MISSING.getHttpStatus()).body(body);
    }

    /**
     * 处理方法级 ConstraintViolation 异常
     *
     * @param e 约束违反异常
     * @return 错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<MallResult<Void>> handleConstraintViolation(ConstraintViolationException e) {
        MallResult<Void> body = MallResult.error(ErrorCode.PARAM_MISSING.getCode(), e.getMessage(), e.getMessage());
        return ResponseEntity.status(ErrorCode.PARAM_MISSING.getHttpStatus()).body(body);
    }

    /**
     * 兜底异常处理
     *
     * @param e 未预期的异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<MallResult<Void>> handleException(Exception e) {
        log.error("unhandled exception", e);
        MallResult<Void> body = MallResult.error(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getUserTip());
        return ResponseEntity.status(ErrorCode.SYSTEM_ERROR.getHttpStatus()).body(body);
    }
}
