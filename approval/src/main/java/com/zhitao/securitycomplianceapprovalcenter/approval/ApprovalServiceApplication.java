package com.zhitao.securitycomplianceapprovalcenter.approval;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * 审批服务独立启动类
 * 开启 Feign 客户端，用于调用审计服务
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.zhitao.securitycomplianceapprovalcenter.approval",
    "com.zhitao.securitycomplianceapprovalcenter.common"
})
@EnableFeignClients(basePackages = "com.zhitao.securitycomplianceapprovalcenter.common.feign")
public class ApprovalServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApprovalServiceApplication.class, args);
    }
}
