package com.mall.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.mybatis.spring.annotation.MapperScan;

/**
 * mall-auth 认证服务
 * 端口：9210
 */
@EnableFeignClients(basePackages = {"com.ruoyi", "com.mall.api"})
@MapperScan("com.mall.auth.mapper")
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.mall.auth", "com.mall.common"})
public class MallAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallAuthApplication.class, args);
    }
}
