package com.zhitao.securitycomplianceapprovalcenter.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication
@ComponentScan(basePackages = {
    "com.zhitao.securitycomplianceapprovalcenter.auth",
    "com.zhitao.securitycomplianceapprovalcenter.common"
})
@EnableFeignClients(basePackages = "com.zhitao.securitycomplianceapprovalcenter.common.feign")
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
