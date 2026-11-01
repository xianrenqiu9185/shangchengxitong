package com.linli.mall;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = "com.linli.mall", annotationClass = Mapper.class)
public class LinliMallApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinliMallApplication.class, args);
    }
}
