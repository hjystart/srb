package com.atguigu.srb.acl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author hjystart
 * @create 2020-12-17 16:47
 */
@SpringBootApplication
@ComponentScan({"com.atguigu.srb"})
public class ServiceAclApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceAclApplication.class, args);
    }
}
