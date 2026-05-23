package com.ruoyi.gateway.config.properties;

import java.util.Arrays;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

/**
 * C 端认证配置属性
 *
 * @author ruoyi
 * @date 2026/05/22
 */
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "mall.security")
public class MallAuthProperties
{
    /** C 端 JWT 密钥（HS512，至少 32 字符） */
    private String jwtSecret;

    /** C 端匿名路径（不需 token 即可访问） */
    private String[] anonymousPaths = new String[] {};

    public String getJwtSecret()
    {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret)
    {
        this.jwtSecret = jwtSecret;
    }

    public String[] getAnonymousPaths()
    {
        return anonymousPaths;
    }

    public void setAnonymousPaths(String[] anonymousPaths)
    {
        this.anonymousPaths = anonymousPaths;
    }

    @Override
    public String toString()
    {
        return "MallAuthProperties{jwtSecret='***', anonymousPaths=" + Arrays.toString(anonymousPaths) + "}";
    }
}
