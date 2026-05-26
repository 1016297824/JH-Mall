package com.mall.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * C 端认证 Bean 配置
 *
 * <p>注册 BCryptPasswordEncoder（cost 从 Nacos 配置读取）和 RedisTemplate（key 使用 String 序列化）</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
@Configuration
public class AuthConfig {

    /**
     * BCrypt 密码编码器
     *
     * @param authProperties 认证配置属性（用于读取 bcryptCost）
     * @return BCryptPasswordEncoder
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder(MallAuthConfigProperties authProperties) {
        return new BCryptPasswordEncoder(authProperties.getPwdBcryptCost());
    }

    /**
     * Redis 模板（Key 使用 String 序列化）
     *
     * @param connectionFactory Redis 连接工厂
     * @return RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        return template;
    }
}
