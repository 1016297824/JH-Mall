package com.mall.marketing.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mall.api.feign.RemoteMarketingService.CalculationResp.AppliedPromotion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;

/**
 * 营销模块 Redis / 本地缓存配置
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Configuration
public class MarketingConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    /**
     * 促销规则匹配结果本地缓存
     *
     * <p>同一「活动集合 + 订单金额」在 TTL 内不重复计算，对应设计文档 §3.4 的
     * 「促销规则匹配结果缓存」（配置项 {@code mall.marketing.calculation.rule-cache-ttl}）。</p>
     *
     * <p><b>注意</b>：TTL 在容器启动时读取一次。{@code MallMarketingConfigProperties}
     * 虽标注 {@code @RefreshScope}，但本缓存的过期时间<b>不会</b>随之热更新——
     * 该配置项变更需重启生效。缓存键已包含活动 ID 集合，故活动上下线不会读到脏数据。</p>
     *
     * @param config 营销模块配置属性
     * @return Caffeine 缓存实例（key = 订单金额 + 活动 ID 集合，value = 命中的优惠明细）
     */
    @Bean
    public Cache<String, List<AppliedPromotion>> promotionMatchCache(MallMarketingConfigProperties config) {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofSeconds(config.getCalculation().getRuleCacheTtl()))
                .recordStats()
                .build();
    }
}

