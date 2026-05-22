package com.ruoyi.gateway.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

/**
 * C 端认证配置属性
 *
 * @author ruoyi
 */
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "mall.security")
public class MallAuthProperties
{
    /** C 端 JWT 密钥（HS512，至少 32 字符） */
    private String jwtSecret;

    /** C 端白名单路径（逗号分隔） */
    private String[] whites = new String[] {};

    public String getJwtSecret()
    {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret)
    {
        this.jwtSecret = jwtSecret;
    }

    public String[] getWhites()
    {
        return whites;
    }

    public void setWhites(String[] whites)
    {
        this.whites = whites;
    }
}
