package com.mall.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 模板配置
 *
 * <p>配置 String 序列化的 RedisTemplate Bean</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Configuration
public class UserConfig {

    /**
     * 配置 String 序列化的 RedisTemplate Bean
     *
     * <p>key 使用 StringRedisSerializer，value 使用 JDK 序列化</p>
     *
     * @param connectionFactory Redis 连接工厂
     * @return RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        return template;
    }
}
