package com.mall.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * mall-user 用户服务
 * 端口：9301
 */
@EnableFeignClients(basePackages = {"com.ruoyi", "com.mall.api"})
@MapperScan("com.mall.user.mapper")
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.mall.user", "com.mall.common"})
public class MallUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallUserApplication.class, args);
    }
}
