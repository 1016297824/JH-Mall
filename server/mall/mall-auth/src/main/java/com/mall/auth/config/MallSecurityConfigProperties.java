package com.mall.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * C 端安全配置属性
 *
 * <p>对应 Nacos mall-auth-dev.yml 中 mall.security.* 配置</p>
 * @author JH-Mall
 * @date 2026/05/26
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "mall.security")
public class MallSecurityConfigProperties {

    /** JWT 签名密钥（HS512，64 字节 Base64，* 需重启） */
    private String jwtSecret;

    /** AES-256-GCM 密钥（* 需重启） */
    private String aesKey;

    /**
     * 获取 JWT 签名密钥
     *
     * @return JWT 签名密钥（HS512，64 字节 Base64）
     */
    public String getJwtSecret() {
        return jwtSecret;
    }

    /**
     * 设置 JWT 签名密钥
     *
     * @param jwtSecret JWT 签名密钥（HS512，64 字节 Base64）
     */
    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    /**
     * 获取 AES 加密密钥
     *
     * @return AES-256-GCM 密钥
     */
    public String getAesKey() {
        return aesKey;
    }

    /**
     * 设置 AES 加密密钥
     *
     * @param aesKey AES-256-GCM 密钥
     */
    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }
}
