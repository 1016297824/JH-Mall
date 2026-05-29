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
    "rocketmq.name-server=127.0.0.1:9876",
    "spring.datasource.dynamic.primary=master",
    "spring.datasource.dynamic.datasource.master.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.datasource.dynamic.datasource.master.url=jdbc:mysql://localhost:3306/mall?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=GMT%2B8",
    "spring.datasource.dynamic.datasource.master.username=root",
    "spring.datasource.dynamic.datasource.master.password=138992"
})
class MallUserApplicationTests {

    @Test
    void contextLoads() {
    }
}
