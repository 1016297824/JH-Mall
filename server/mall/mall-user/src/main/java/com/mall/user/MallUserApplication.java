package com.mall.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * mall-user 用户服务
 *
 * <p>端口：9302，提供 C 端用户注册、资料、地址、会员、积分、签到等功能</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@EnableScheduling
@EnableFeignClients(basePackages = {"com.ruoyi", "com.mall.api"})
@MapperScan("com.mall.user.mapper")
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.mall.user", "com.mall.common"})
public class MallUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallUserApplication.class, args);
    }
}
