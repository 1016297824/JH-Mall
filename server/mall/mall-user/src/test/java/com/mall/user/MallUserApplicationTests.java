package com.mall.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * MallUserApplication 启动测试
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@SpringBootTest(properties = {
    "spring.cloud.nacos.config.enabled=false",
    "spring.cloud.nacos.discovery.enabled=false",
    "rocketmq.name-server=127.0.0.1:9876"
})
class MallUserApplicationTests {

    @Test
    void contextLoads() {
    }
}
