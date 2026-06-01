package com.mall.product.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mall.product.VO.SpuVO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * 商品模块 Redis 模板配置
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Configuration
public class ProductConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    /**
     * 热点商品 Caffeine 本地缓存
     *
     * <p>热点排行接口中加速 C 端查询，减少 Redis 压力。
     * 最大 500 条，5min 无访问过期，启用统计用于监控。</p>
     *
     * @return Caffeine 缓存实例（key = spuId, value = SpuVO）
     */
    @Bean
    public Cache<Long, SpuVO> hotProductCache() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterAccess(Duration.ofMinutes(5))
                .recordStats()
                .build();
    }
}
