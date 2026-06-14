package com.mall.auth.controller;

import com.mall.auth.DTO.request.*;
import com.mall.auth.DTO.response.TokenRespDTO;
import com.mall.common.DTO.user.response.MallUserDTO;
import com.mall.common.DTO.MallResult;
import com.mall.api.feign.RemoteUserService;
import com.mall.auth.config.MallAuthConfigProperties;
import com.mall.auth.DTO.request.CaptchaChangePhoneReqDTO;
import com.mall.auth.DTO.response.CaptchaRespDTO;
import com.mall.auth.service.ICaptchaService;
import com.mall.auth.service.ITokenService;
import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.user.RegisterTypeEnum;
import com.mall.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.apache.commons.codec.digest.DigestUtils;

import com.mall.common.constant.CacheConstants;
import com.mall.common.constant.HeaderConstants;
import com.mall.common.enums.user.UserStatusEnum;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * C 端认证控制器（验证码注册/登录/密码重置/换绑手机/注销）
 *
 * <p>所有接口通过图形验证码校验后执行，防止自动化攻击。
 * 密码使用 BCrypt 哈希，三次失败计入错误计数。</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
@RestController
@RequestMapping("/api/auth/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    /** 密码正则：必须包含字母和数字 */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-zA-Z])(?=.*\\d).+$");

    /** 验证码服务 */
    private final ICaptchaService captchaService;
    /** Token 服务 */
    private final ITokenService tokenService;
    /** 用户服务 Feign 接口 */
    private final RemoteUserService remoteUserService;
    /** Redis 模板 */
    private final RedisTemplate<String, Object> redisTemplate;
    /** 密码编码器 */
    private final BCryptPasswordEncoder passwordEncoder;
    /** 认证配置属性 */
    private final MallAuthConfigProperties authProperties;

    /**
     * 获取图形验证码
     *
     * @return 验证码响应
     */
    @GetMapping
    public MallResult<CaptchaRespDTO> getCaptcha() {
        Map<String, String> result = captchaService.generate();
        CaptchaRespDTO response = new CaptchaRespDTO(
                result.get("captchaKey"), result.get("captchaImage"));
        return MallResult.success(response);
    }

    /**
     * 验证码注册
     *
     * @param req     注册请求
     * @param request HTTP 请求（用于获取客户端 IP）
     * @return Token 响应
     */
    @PostMapping("/register")
    public MallResult<TokenRespDTO> register(@Valid @RequestBody CaptchaRegisterReqDTO req,
                                             HttpServletRequest request) {
        String clientIp = getClientIp(request);
        captchaService.verify(req.getCaptchaKey(), req.getCaptchaCode(), clientIp);

        if (!Boolean.TRUE.equals(req.getIsPrivacyAgreed())) {
            throw new BusinessException(ErrorCode.PRIVACY_NOT_AGREED);
        }

        validatePassword(req.getPassword());

        MallUserDTO existing = remoteUserService.findByPhone(req.getPhone());
        if (existing != null) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
        }

        String passwordHash = passwordEncoder.encode(req.getPassword());
        String phoneHash = sha256(req.getPhone());

        RemoteUserService.RegisterRequest registerReq = new RemoteUserService.RegisterRequest();
        registerReq.setPhone(req.getPhone());
        registerReq.setPhoneHash(phoneHash);
        registerReq.setPassword(passwordHash);
        registerReq.setRegisterType(RegisterTypeEnum.PHONE.getCode());

        String userId = remoteUserService.register(registerReq);
        TokenRespDTO token = tokenService.issue(userId);
        return MallResult.success(token);
    }

    /**
     * 验证码密码登录
     *
     * @param req     登录请求
     * @param request HTTP 请求（用于获取客户端 IP）
     * @return Token 响应
     */
    @PostMapping("/login")
    public MallResult<TokenRespDTO> login(@Valid @RequestBody CaptchaLoginReqDTO req,
                                          HttpServletRequest request) {
        String clientIp = getClientIp(request);
        captchaService.verify(req.getCaptchaKey(), req.getCaptchaCode(), clientIp);

        MallUserDTO user = remoteUserService.findByPhone(req.getPhone());
        if (user == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        if (String.valueOf(UserStatusEnum.FROZEN.getCode()).equals(user.getUserStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_FROZEN);
        }
        if (String.valueOf(UserStatusEnum.DELETED.getCode()).equals(user.getUserStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DELETED);
        }

        // 读取 Redis 中的密码错误计数，超过上限则临时锁定
        String pwdErrKey = CacheConstants.Auth.PWD_ERR + user.getId();
        Object errCountObj = redisTemplate.opsForValue().get(pwdErrKey);
        int errCount = errCountObj instanceof Number ? ((Number) errCountObj).intValue() : 0;
        if (errCount >= authProperties.getPwdErrLimit()) {
            throw new BusinessException(ErrorCode.PASSWORD_LOCKED);
        }

        // 密码不匹配时递增错误计数
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            incrementPwdErrCount(pwdErrKey);
            throw new BusinessException(ErrorCode.PASSWORD_WRONG);
        }

        // 密码正确时清除错误计数
        redisTemplate.delete(pwdErrKey);
        TokenRespDTO token = tokenService.issue(user.getId());
        return MallResult.success(token);
    }

    /**
     * 验证码重置密码
     *
     * @param req     重置密码请求
     * @param request HTTP 请求（用于获取客户端 IP）
     * @return 成功响应
     */
    @PostMapping("/password/reset")
    public MallResult<Void> resetPassword(@Valid @RequestBody CaptchaResetPasswordReqDTO req,
                                          HttpServletRequest request) {
        String clientIp = getClientIp(request);
        captchaService.verify(req.getCaptchaKey(), req.getCaptchaCode(), clientIp);

        MallUserDTO user = remoteUserService.findByPhone(req.getPhone());
        if (user == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        validatePassword(req.getNewPassword());

        String passwordHash = passwordEncoder.encode(req.getNewPassword());

        RemoteUserService.PasswordUpdateRequest updateReq = new RemoteUserService.PasswordUpdateRequest();
        updateReq.setNewPassword(passwordHash);
        remoteUserService.updatePassword(user.getId(), updateReq);

        tokenService.revokeAll(user.getId());
        return MallResult.success(null);
    }

    /**
     * 换绑手机号（图形验证码已在前端校验）
     *
     * @param req 换绑手机请求
     * @return 成功响应
     */
    @PutMapping("/phone")
    public MallResult<Void> changePhone(@Valid @RequestBody CaptchaChangePhoneReqDTO req) {
        MallUserDTO user = remoteUserService.findByPhone(req.getOldPhone());
        if (user == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_WRONG);
        }

        MallUserDTO newPhoneUser = remoteUserService.findByPhone(req.getNewPhone());
        if (newPhoneUser != null) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
        }

        String newPhoneHash = sha256(req.getNewPhone());

        RemoteUserService.PhoneUpdateRequest updateReq = new RemoteUserService.PhoneUpdateRequest();
        updateReq.setNewPhone(req.getNewPhone());
        updateReq.setNewPhoneHash(newPhoneHash);
        remoteUserService.updatePhone(user.getId(), updateReq);

        return MallResult.success(null);
    }

    /**
     * 注销账户
     *
     * @param req 注销账户请求
     * @return 成功响应
     */
    @DeleteMapping("/account")
    public MallResult<Void> deactivateAccount(@Valid @RequestBody CaptchaDeactivateReqDTO req) {
        MallUserDTO user = remoteUserService.findByPhone(req.getPhone());
        if (user == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_WRONG);
        }

        remoteUserService.deactivateAccount(user.getId());
        tokenService.revokeAll(user.getId());

        return MallResult.success(null);
    }

    /**
     * 校验密码复杂度
     *
     * @param password 明文密码
     */
    private void validatePassword(String password) {
        if (password.length() < 8 || password.length() > 32 || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException(ErrorCode.PASSWORD_WEAK);
        }
    }

    /**
     * 获取客户端真实 IP
     *
     * <p>优先级：X-Forwarded-For &gt; X-Real-IP &gt; RemoteAddr</p>
     *
     * @param request HTTP 请求
     * @return 客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader(HeaderConstants.X_FORWARDED_FOR);
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader(HeaderConstants.X_REAL_IP);
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 递增密码错误计数（首次设置 TTL）
     *
     * @param key 密码错误计数 Key
     */
    private void incrementPwdErrCount(String key) {
        // 真正做计数 +1 的是这一行
        Long count = redisTemplate.opsForValue().increment(key, 1L);
        // expire 是首次创建 key 时设置过期时间（30 分钟自动解锁）
        if (count != null && count == 1) {
            redisTemplate.expire(key, authProperties.getPwdErrTtl(), TimeUnit.SECONDS);
        }
    }

    /**
     * SHA-256 哈希
     *
     * @param input 输入字符串
     * @return 哈希值（Hex）
     */
    private String sha256(String input) {
        return DigestUtils.sha256Hex(input);
    }
}
