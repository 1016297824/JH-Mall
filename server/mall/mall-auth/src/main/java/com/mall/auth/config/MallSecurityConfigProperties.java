package com.mall.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * C 端安全配置属性
 *
 * <p>对应 Nacos mall-auth-dev.yml 中 mall.security.* 配置</p>
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "mall.security")
public class MallSecurityConfigProperties {

    /** JWT 签名密钥（HS512，64 字节 Base64，* 需重启） */
    private String jwtSecret;

    /** AES-256-GCM 密钥（* 需重启） */
    private String aesKey;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public String getAesKey() {
        return aesKey;
    }

    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }
}
