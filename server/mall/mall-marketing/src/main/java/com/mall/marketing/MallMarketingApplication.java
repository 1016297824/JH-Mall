package com.mall.marketing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * mall-marketing 营销服务
 *
 * <p>端口：9306，提供 C 端优惠券、促销活动、优惠试算等功能</p>
 *
 * <p><b>为何要显式扫描 {@code com.mall.common}</b>：{@code MallExceptionHandler}
 * 位于 {@code com.mall.common.handler}，不在本类的默认扫描路径内。不显式扫描则该
 * {@code @RestControllerAdvice} 不会注册，异常响应将不是 {@code MallResult}
 * 格式且 HTTP 状态码错误（异常一律 200）。mall-order / mall-user 采用同样写法。</p>
 *
 * <p><b>为何要 {@code @EnableScheduling}</b>：{@code OutboxScheduler}（Outbox 消息投递）
 * 与 {@code CouponExpireTask}（券批量过期）都基于 {@code @Scheduled}，
 * 不开则定时任务静默不执行。</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@EnableFeignClients(basePackages = {"com.ruoyi", "com.mall.api"})
@MapperScan("com.mall.marketing.mapper")
@EnableDiscoveryClient
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.mall.marketing", "com.mall.common"})
public class MallMarketingApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallMarketingApplication.class, args);
    }
}
