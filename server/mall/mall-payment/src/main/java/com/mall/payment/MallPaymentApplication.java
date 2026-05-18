package com.mall.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.mybatis.spring.annotation.MapperScan;

/**
 * mall-payment 支付服务
 * 端口：9304
 */
@EnableFeignClients(basePackages = "com.ruoyi")
@MapperScan("com.mall.payment.mapper")
@EnableDiscoveryClient
@SpringBootApplication
public class MallPaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallPaymentApplication.class, args);
    }
}
