package com.mall.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * mall-auth 认证服务
 *
 * <p>端口：9301</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
@EnableFeignClients(basePackages = "com.mall.api")
//@MapperScan("com.mall.auth.mapper")
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.mall.auth", "com.mall.common"})
public class MallAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallAuthApplication.class, args);
    }
}
