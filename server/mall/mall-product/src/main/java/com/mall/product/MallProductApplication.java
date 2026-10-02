package com.mall.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.mybatis.spring.annotation.MapperScan;

/**
 * mall-product 商品服务
 *
 * <p>端口：9302，提供 C 端商品、类目、SKU、搜索、库存等功能</p>
 *
 * <p><b>为何要显式扫描 {@code com.mall.common}</b>：{@code MallExceptionHandler}
 * 位于 {@code com.mall.common.handler}，不在默认扫描路径内。不显式扫描则该
 * {@code @RestControllerAdvice} 不会注册，异常响应将不是 {@code MallResult}
 * 格式且 HTTP 状态码错误（异常一律 200）。</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@EnableFeignClients(basePackages = {"com.ruoyi", "com.mall.api"})
@MapperScan("com.mall.product.mapper")
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.mall.product", "com.mall.common"})
public class MallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallProductApplication.class, args);
    }
}
