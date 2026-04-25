package com.zhitao.securitycomplianceapprovalcenter.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.zhitao.securitycomplianceapprovalcenter.audit",
    "com.zhitao.securitycomplianceapprovalcenter.common"
})
@EnableFeignClients(basePackages = "com.zhitao.securitycomplianceapprovalcenter.common.feign")
public class AuditServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
    }
}
