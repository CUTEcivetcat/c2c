package com.c2c;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用启动类：聚合 admin / order / product / rating 等模块，作为单体应用统一启动入口。
 */
@MapperScan("com.c2c.**.mapper")
@SpringBootApplication
public class C2cMonolithApplication {
    public static void main(String[] args) {
        SpringApplication.run(C2cMonolithApplication.class, args);
    }
}

