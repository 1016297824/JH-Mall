package com.mall.marketing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.mybatis.spring.annotation.MapperScan;

/**
 * mall-marketing 营销服务
 *
 * <p>端口：9306，提供 C 端优惠券、促销活动、优惠试算等功能</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@EnableFeignClients(basePackages = {"com.ruoyi", "com.mall.api"})
@MapperScan("com.mall.marketing.mapper")
@EnableDiscoveryClient
@SpringBootApplication
public class MallMarketingApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallMarketingApplication.class, args);
    }
}
