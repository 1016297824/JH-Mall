package com.mall.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.mybatis.spring.annotation.MapperScan;

/**
 * mall-payment 支付服务
 *
 * <p>端口：9305，提供 C 端支付、退款、支付回调等功能</p>
 *
 * <p><b>scanBasePackages 含 com.mall.common</b>：{@code MallExceptionHandler}
 * 等公共组件在 mall-common 下，不扫描则异常响应不是 {@code MallResult} 格式、
 * 且 HTTP 状态码一律 200。</p>
 *
 * <p><b>@EnableScheduling</b>：超时关单（{@code PaymentTimeoutTask}）与
 * 回调丢失补偿（{@code PaymentCompensateTask}）依赖定时任务生效。</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@EnableFeignClients(basePackages = {"com.ruoyi", "com.mall.api"})
@MapperScan("com.mall.payment.mapper")
@EnableDiscoveryClient
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.mall.payment", "com.mall.common"})
public class MallPaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallPaymentApplication.class, args);
    }
}
