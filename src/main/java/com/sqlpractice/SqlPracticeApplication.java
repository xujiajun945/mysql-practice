package com.sqlpractice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.sqlpractice.mapper")
public class SqlPracticeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SqlPracticeApplication.class, args);
    }
}
