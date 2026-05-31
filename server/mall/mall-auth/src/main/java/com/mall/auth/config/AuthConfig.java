package com.mall.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * C 端认证 Bean 配置
 *
 * <p>注册 BCryptPasswordEncoder 和 RedisTemplate（key String 序列化，value JSON 序列化）</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
@Configuration
public class AuthConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder(MallAuthConfigProperties authProperties) {
        return new BCryptPasswordEncoder(authProperties.getPwdBcryptCost());
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
