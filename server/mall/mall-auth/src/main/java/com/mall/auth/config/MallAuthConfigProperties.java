package com.mall.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * C 端认证配置属性（热更新）
 *
 * <p>对应 Nacos mall-auth-dev.yml 中 mall.auth.* 配置</p>
 * @author JH-Mall
 * @date 2026/05/26
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "mall.auth")
public class MallAuthConfigProperties {

    /** Access Token 有效期（秒，默认 1800 = 30 分钟） */
    private long accessTokenTtl = 1800;

    /** Refresh Token 有效期（秒，默认 604800 = 7 天） */
    private long refreshTokenTtl = 604800;

    /** token_version Redis 缓存 TTL（秒，默认 2592000 = 30 天） */
    private long tokenVersionCacheTtl = 2592000;

    /** 密码连续错误次数上限 */
    private int pwdErrLimit = 5;

    /** 密码错误计数重置时间（秒） */
    private long pwdErrTtl = 1800;

    /** BCrypt 哈希强度（2^N 轮迭代） */
    private int pwdBcryptCost = 12;

    private final Captcha captcha = new Captcha();

    private final Sms sms = new Sms();

    /**
     * 获取 Access Token 有效期
     *
     * @return 有效期（秒）
     */
    public long getAccessTokenTtl() {
        return accessTokenTtl;
    }

    /**
     * 设置 Access Token 有效期
     *
     * @param accessTokenTtl 有效期（秒）
     */
    public void setAccessTokenTtl(long accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    /**
     * 获取 Refresh Token 有效期
     *
     * @return 有效期（秒）
     */
    public long getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    /**
     * 设置 Refresh Token 有效期
     *
     * @param refreshTokenTtl 有效期（秒）
     */
    public void setRefreshTokenTtl(long refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }

    /**
     * 获取 token_version 缓存 TTL
     *
     * @return TTL（秒）
     */
    public long getTokenVersionCacheTtl() {
        return tokenVersionCacheTtl;
    }

    /**
     * 设置 token_version 缓存 TTL
     *
     * @param tokenVersionCacheTtl TTL（秒）
     */
    public void setTokenVersionCacheTtl(long tokenVersionCacheTtl) {
        this.tokenVersionCacheTtl = tokenVersionCacheTtl;
    }

    /**
     * 获取密码连续错误次数上限
     *
     * @return 错误次数上限
     */
    public int getPwdErrLimit() {
        return pwdErrLimit;
    }

    /**
     * 设置密码连续错误次数上限
     *
     * @param pwdErrLimit 错误次数上限
     */
    public void setPwdErrLimit(int pwdErrLimit) {
        this.pwdErrLimit = pwdErrLimit;
    }

    /**
     * 获取密码错误计数重置时间
     *
     * @return 重置时间（秒）
     */
    public long getPwdErrTtl() {
        return pwdErrTtl;
    }

    /**
     * 设置密码错误计数重置时间
     *
     * @param pwdErrTtl 重置时间（秒）
     */
    public void setPwdErrTtl(long pwdErrTtl) {
        this.pwdErrTtl = pwdErrTtl;
    }

    /**
     * 获取 BCrypt 哈希强度
     *
     * @return 哈希强度（2^N 轮迭代）
     */
    public int getPwdBcryptCost() {
        return pwdBcryptCost;
    }

    /**
     * 设置 BCrypt 哈希强度
     *
     * @param pwdBcryptCost 哈希强度（2^N 轮迭代）
     */
    public void setPwdBcryptCost(int pwdBcryptCost) {
        this.pwdBcryptCost = pwdBcryptCost;
    }

    /**
     * 获取验证码配置
     *
     * @return 验证码配置
     */
    public Captcha getCaptcha() {
        return captcha;
    }

    /**
     * 获取短信配置
     *
     * @return 短信配置
     */
    public Sms getSms() {
        return sms;
    }

    /** 验证码配置 */
    public static class Captcha {

        /** IP 计数 TTL（秒，默认 86400 = 24 小时） */
        private long ipTtl = 86400;

        /**
         * 获取 IP 计数 TTL
         *
         * @return TTL（秒）
         */
        public long getIpTtl() {
            return ipTtl;
        }

        /**
         * 设置 IP 计数 TTL
         *
         * @param ipTtl TTL（秒）
         */
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

        /**
         * 获取验证码长度
         *
         * @return 验证码长度
         */
        public int getCodeLength() {
            return codeLength;
        }

        /**
         * 设置验证码长度
         *
         * @param codeLength 验证码长度
         */
        public void setCodeLength(int codeLength) {
            this.codeLength = codeLength;
        }

        /**
         * 获取验证码有效期
         *
         * @return 有效期（秒）
         */
        public long getCodeTtl() {
            return codeTtl;
        }

        /**
         * 设置验证码有效期
         *
         * @param codeTtl 有效期（秒）
         */
        public void setCodeTtl(long codeTtl) {
            this.codeTtl = codeTtl;
        }

        /**
         * 获取发送冷却时间
         *
         * @return 冷却时间（秒）
         */
        public long getCooldown() {
            return cooldown;
        }

        /**
         * 设置发送冷却时间
         *
         * @param cooldown 冷却时间（秒）
         */
        public void setCooldown(long cooldown) {
            this.cooldown = cooldown;
        }

        /**
         * 获取单手机号日发送上限
         *
         * @return 日发送上限
         */
        public int getDailyLimit() {
            return dailyLimit;
        }

        /**
         * 设置单手机号日发送上限
         *
         * @param dailyLimit 日发送上限
         */
        public void setDailyLimit(int dailyLimit) {
            this.dailyLimit = dailyLimit;
        }

        /**
         * 获取单 IP 日发送上限
         *
         * @return 日发送上限
         */
        public int getIpDailyLimit() {
            return ipDailyLimit;
        }

        /**
         * 设置单 IP 日发送上限
         *
         * @param ipDailyLimit 日发送上限
         */
        public void setIpDailyLimit(int ipDailyLimit) {
            this.ipDailyLimit = ipDailyLimit;
        }
    }
}
