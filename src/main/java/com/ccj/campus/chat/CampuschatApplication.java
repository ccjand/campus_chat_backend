package com.ccj.campus.chat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 校园即时通讯系统启动入口。
 * 对齐论文 5.1 开发环境与架构。
 */
@SpringBootApplication
@MapperScan("com.ccj.campus.chat.mapper")
@EnableScheduling
public class CampuschatApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampuschatApplication.class, args);
    }

}

