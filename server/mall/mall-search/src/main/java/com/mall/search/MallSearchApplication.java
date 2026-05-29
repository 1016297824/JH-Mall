package com.mall.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * mall-search 搜索服务
 *
 * <p>端口：9307，提供 C 端商品搜索、搜索建议、索引重建等功能</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@EnableFeignClients(basePackages = "com.mall.api")
@EnableDiscoveryClient
@SpringBootApplication
public class MallSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallSearchApplication.class, args);
    }
}
