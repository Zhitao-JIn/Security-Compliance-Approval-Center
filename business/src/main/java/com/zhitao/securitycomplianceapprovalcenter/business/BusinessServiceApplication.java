package com.zhitao.securitycomplianceapprovalcenter.business;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.zhitao.securitycomplianceapprovalcenter.business",
    "com.zhitao.securitycomplianceapprovalcenter.common"
})
@EnableFeignClients(basePackages = "com.zhitao.securitycomplianceapprovalcenter.common.feign")
public class BusinessServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BusinessServiceApplication.class, args);
    }
}
