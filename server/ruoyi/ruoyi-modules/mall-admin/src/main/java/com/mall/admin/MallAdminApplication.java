package com.mall.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
import com.ruoyi.common.security.annotation.EnableCustomConfig;
import com.ruoyi.common.security.annotation.EnableRyFeignClients;

/**
 * 管理端启动类
 * 
 * @author ruoyi
 * @date 2026/05/27
 */
@EnableCustomConfig
@EnableRyFeignClients
@MapperScan("com.mall.admin.**.mapper")
@SpringBootApplication
public class MallAdminApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(MallAdminApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  管理端模块启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                " .-------.       ____     __        \n" +
                " |  _ _   \\      \\   \\   /  /    \n" +
                " | ( ' )  |       \\  _. /  '       \n" +
                " |(_ o _) /        _( )_ .'         \n" +
                " | (_,_).' __  ___(_ o _)'          \n" +
                " |  |\\ \\  |  ||   |(_,_)'         \n" +
                " |  | \\ `'   /|   `-'  /           \n" +
                " |  |  \\    /  \\      /           \n" +
                " ''-'   `'-'    `-..-'              ");
    }
}
