package com.taxi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 网约车运营系统启动类
 */
@SpringBootApplication
@MapperScan("com.taxi.mapper")
public class TaxiOperationSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaxiOperationSystemApplication.class, args);
        System.out.println("网约车运营系统启动成功！");
        System.out.println("访问地址: http://localhost:8080/api");
    }
} 