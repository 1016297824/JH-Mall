package com.mall.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * C 端认证配置属性（热更新）
 *
 * <p>对应 Nacos mall-auth-dev.yml 中 mall.auth.* 配置</p>
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "mall.auth")
public class MallAuthConfigProperties {

    /** Access Token 有效期（秒，默认 1800 = 30 分钟） */
    private long accessTokenTtl = 1800;

    /** Refresh Token 有效期（秒，默认 604800 = 7 天） */
    private long refreshTokenTtl = 604800;

    /** 密码连续错误次数上限 */
    private int pwdErrLimit = 5;

    /** 密码错误计数重置时间（秒） */
    private long pwdErrTtl = 1800;

    /** BCrypt 哈希强度（2^N 轮迭代） */
    private int pwdBcryptCost = 12;

    private final Captcha captcha = new Captcha();

    private final Sms sms = new Sms();

    public long getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(long accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public long getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(long refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public int getPwdErrLimit() {
        return pwdErrLimit;
    }

    public void setPwdErrLimit(int pwdErrLimit) {
        this.pwdErrLimit = pwdErrLimit;
    }

    public long getPwdErrTtl() {
        return pwdErrTtl;
    }

    public void setPwdErrTtl(long pwdErrTtl) {
        this.pwdErrTtl = pwdErrTtl;
    }

    public int getPwdBcryptCost() {
        return pwdBcryptCost;
    }

    public void setPwdBcryptCost(int pwdBcryptCost) {
        this.pwdBcryptCost = pwdBcryptCost;
    }

    public Captcha getCaptcha() {
        return captcha;
    }

    public Sms getSms() {
        return sms;
    }

    /** 验证码配置 */
    public static class Captcha {

        /** IP 计数 TTL（秒，默认 86400 = 24 小时） */
        private long ipTtl = 86400;

        public long getIpTtl() {
            return ipTtl;
        }

        public void setIpTtl(long ipTtl) {
            this.ipTtl = ipTtl;
        }
    }

    /** 短信验证码配置 */
    public static class Sms {

        /** 验证码长度 */
        private int codeLength = 6;

        /** 验证码有效期（秒） */
        private long codeTtl = 300;

        /** 发送冷却时间（秒） */
        private long cooldown = 60;

        /** 单手机号日发送上限 */
        private int dailyLimit = 5;

        /** 单 IP 日发送上限 */
        private int ipDailyLimit = 10;

        public int getCodeLength() {
            return codeLength;
        }

        public void setCodeLength(int codeLength) {
            this.codeLength = codeLength;
        }

        public long getCodeTtl() {
            return codeTtl;
        }

        public void setCodeTtl(long codeTtl) {
            this.codeTtl = codeTtl;
        }

        public long getCooldown() {
            return cooldown;
        }

        public void setCooldown(long cooldown) {
            this.cooldown = cooldown;
        }

        public int getDailyLimit() {
            return dailyLimit;
        }

        public void setDailyLimit(int dailyLimit) {
            this.dailyLimit = dailyLimit;
        }

        public int getIpDailyLimit() {
            return ipDailyLimit;
        }

        public void setIpDailyLimit(int ipDailyLimit) {
            this.ipDailyLimit = ipDailyLimit;
        }
    }
}
