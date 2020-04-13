package com.yuhb.customer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Created by yu.hb on 2019-10-30
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients // 扫描 @FeignClient 注解
@MapperScan("com.yuhb.customer.mapper")
public class CustomerApp {

    public static void main(String[] args) {
        System.setProperty("service.disableGlobalTransaction", "false");
        SpringApplication.run(CustomerApp.class, args);
    }

}
