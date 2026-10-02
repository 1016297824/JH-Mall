package com.mall.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * mall-order 订单服务
 *
 * <p>端口：9304，提供 C 端订单、购物车、售后等功能</p>
 *
 * <p><b>为何要显式扫描 {@code com.mall.common}</b>：{@code MallExceptionHandler}
 * 位于 {@code com.mall.common.handler}，不在本类的默认扫描路径
 * （{@code com.mall.order.**}）内。不显式扫描则该 {@code @RestControllerAdvice}
 * 不会注册，异常会落到 ruoyi 的全局处理器，导致响应格式非 {@code MallResult}
 * 且 HTTP 状态码错误（异常一律 200）。{@code mall-user} 采用同样写法。</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@EnableFeignClients(basePackages = {"com.ruoyi", "com.mall.api"})
@MapperScan("com.mall.order.mapper")
@EnableDiscoveryClient
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.mall.order", "com.mall.common"})
public class MallOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallOrderApplication.class, args);
    }
}
