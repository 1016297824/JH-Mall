package com.mall.auth.controller;

import com.mall.common.dto.user.MallUserDTO;
import com.mall.common.dto.MallResult;
import com.mall.api.feign.RemoteUserService;
import com.mall.auth.dto.request.CaptchaChangePhoneReq;
import com.mall.auth.dto.request.CaptchaDeactivateReq;
import com.mall.auth.dto.request.CaptchaLoginReq;
import com.mall.auth.dto.request.CaptchaRegisterReq;
import com.mall.auth.dto.request.CaptchaResetPasswordReq;
import com.mall.auth.dto.response.CaptchaResponse;
import com.mall.auth.dto.response.TokenResponse;
import com.mall.auth.service.CaptchaService;
import com.mall.auth.service.TokenService;
import com.mall.common.exception.BusinessException;
import com.mall.common.exception.CaptchaException;
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

import com.mall.common.enums.user.UserStatusEnum;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth/captcha")
public class CaptchaController {

    private static final String KEY_PWD_ERR = "mall:auth:pwd_err:";
    private static final long PWD_ERR_TTL = 1800L;
    private static final int PWD_ERR_MAX = 5;
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-zA-Z])(?=.*\\d).+$");

    private final CaptchaService captchaService;
    private final TokenService tokenService;
    private final RemoteUserService remoteUserService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder;

    public CaptchaController(CaptchaService captchaService, TokenService tokenService,
                             RemoteUserService remoteUserService, RedisTemplate<String, Object> redisTemplate,
                             BCryptPasswordEncoder passwordEncoder) {
        this.captchaService = captchaService;
        this.tokenService = tokenService;
        this.remoteUserService = remoteUserService;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public MallResult<CaptchaResponse> getCaptcha() {
        Map<String, String> result = captchaService.generate();
        CaptchaResponse response = new CaptchaResponse(
                result.get("captchaKey"), result.get("captchaImage"));
        return MallResult.success(response);
    }

    @PostMapping("/register")
    public MallResult<TokenResponse> register(@Valid @RequestBody CaptchaRegisterReq req,
                                              HttpServletRequest request) {
        String clientIp = getClientIp(request);
        captchaService.verify(req.getCaptchaKey(), req.getCaptchaCode(), clientIp);

        if (!Boolean.TRUE.equals(req.getIsPrivacyAgreed())) {
            throw new BusinessException("A0101", "请同意隐私协议", "请同意隐私协议");
        }

        validatePassword(req.getPassword());

        MallUserDTO existing = remoteUserService.findByPhone(req.getPhone());
        if (existing != null) {
            throw new BusinessException("A0151", "手机号已被注册", "手机号已被注册");
        }

        String passwordHash = passwordEncoder.encode(req.getPassword());
        String phoneHash = sha256(req.getPhone());

        RemoteUserService.RegisterRequest registerReq = new RemoteUserService.RegisterRequest();
        registerReq.setPhone(req.getPhone());
        registerReq.setPhoneHash(phoneHash);
        registerReq.setPassword(passwordHash);
        registerReq.setRegisterType("PHONE");

        String userId = remoteUserService.register(registerReq);
        TokenResponse token = tokenService.issue(userId);
        return MallResult.success(token);
    }

    @PostMapping("/login")
    public MallResult<TokenResponse> login(@Valid @RequestBody CaptchaLoginReq req,
                                           HttpServletRequest request) {
        String clientIp = getClientIp(request);
        captchaService.verify(req.getCaptchaKey(), req.getCaptchaCode(), clientIp);

        MallUserDTO user = remoteUserService.findByPhone(req.getPhone());
        if (user == null) {
            throw new BusinessException("A0201", "账户不存在", "账户不存在");
        }

        if (String.valueOf(UserStatusEnum.FROZEN.getCode()).equals(user.getUserStatus())) {
            throw new BusinessException("A0202", "账户已被冻结", "账户已被冻结");
        }
        if (String.valueOf(UserStatusEnum.DELETED.getCode()).equals(user.getUserStatus())) {
            throw new BusinessException("A0203", "账户已注销", "账户已注销");
        }

        String pwdErrKey = KEY_PWD_ERR + user.getId();
        Object errCountObj = redisTemplate.opsForValue().get(pwdErrKey);
        int errCount = errCountObj instanceof Number ? ((Number) errCountObj).intValue() : 0;
        if (errCount >= PWD_ERR_MAX) {
            throw new BusinessException("A0211", "密码错误次数过多",
                    "密码错误次数过多，请30分钟后重试");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            incrementPwdErrCount(pwdErrKey);
            throw new BusinessException("A0210", "密码错误", "密码错误");
        }

        redisTemplate.delete(pwdErrKey);
        TokenResponse token = tokenService.issue(user.getId());
        return MallResult.success(token);
    }

    @PostMapping("/password/reset")
    public MallResult<Void> resetPassword(@Valid @RequestBody CaptchaResetPasswordReq req,
                                          HttpServletRequest request) {
        String clientIp = getClientIp(request);
        captchaService.verify(req.getCaptchaKey(), req.getCaptchaCode(), clientIp);

        MallUserDTO user = remoteUserService.findByPhone(req.getPhone());
        if (user == null) {
            throw new BusinessException("A0201", "账户不存在", "账户不存在");
        }

        validatePassword(req.getNewPassword());

        String passwordHash = passwordEncoder.encode(req.getNewPassword());

        RemoteUserService.PasswordUpdateRequest updateReq = new RemoteUserService.PasswordUpdateRequest();
        updateReq.setNewPassword(passwordHash);
        remoteUserService.updatePassword(user.getId(), updateReq);

        tokenService.revokeAll(user.getId());
        return MallResult.success(null);
    }

    @PutMapping("/phone")
    public MallResult<Void> changePhone(@Valid @RequestBody CaptchaChangePhoneReq req) {
        MallUserDTO user = remoteUserService.findByPhone(req.getOldPhone());
        if (user == null) {
            throw new BusinessException("A0201", "账户不存在", "账户不存在");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException("A0210", "密码错误", "密码错误");
        }

        MallUserDTO newPhoneUser = remoteUserService.findByPhone(req.getNewPhone());
        if (newPhoneUser != null) {
            throw new BusinessException("A0151", "手机号已被注册", "手机号已被注册");
        }

        String newPhoneHash = sha256(req.getNewPhone());

        RemoteUserService.PhoneUpdateRequest updateReq = new RemoteUserService.PhoneUpdateRequest();
        updateReq.setNewPhone(req.getNewPhone());
        updateReq.setNewPhoneHash(newPhoneHash);
        remoteUserService.updatePhone(user.getId(), updateReq);

        return MallResult.success(null);
    }

    @DeleteMapping("/account")
    public MallResult<Void> deactivateAccount(@Valid @RequestBody CaptchaDeactivateReq req) {
        MallUserDTO user = remoteUserService.findByPhone(req.getPhone());
        if (user == null) {
            throw new BusinessException("A0201", "账户不存在", "账户不存在");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException("A0210", "密码错误", "密码错误");
        }

        remoteUserService.deactivateAccount(user.getId());
        tokenService.revokeAll(user.getId());

        return MallResult.success(null);
    }

    private void validatePassword(String password) {
        if (password.length() < 8 || password.length() > 32 || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException("A0121", "密码需8~32位且包含字母和数字",
                    "密码需8~32位且包含字母和数字");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    private void incrementPwdErrCount(String key) {
        Long count = redisTemplate.opsForValue().increment(key, 1L);
        if (count != null && count == 1) {
            redisTemplate.expire(key, PWD_ERR_TTL, TimeUnit.SECONDS);
        }
    }

    private String sha256(String input) {
        return DigestUtils.sha256Hex(input);
    }
}
