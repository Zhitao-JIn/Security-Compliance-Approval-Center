package com.zhitao.securitycomplianceapprovalcenter.common.feign;


import com.zhitao.securitycomplianceapprovalcenter.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 审计服务Feign调用接口
 */
@FeignClient(name = "audit", url = "${feign.client.audit.url:http://localhost:8081}")
public interface AuditFeignClient {

    /**
     * 写入权限变更审计日志
     */
    @PostMapping("/api/audit/log/record")
    Result<Void> recordPermissionChange(
            @RequestParam Long operatorId,
            @RequestParam String operatorName,
            @RequestParam String operationType,
            @RequestParam String permissionObjectType,
            @RequestParam String permissionObjectId,
            @RequestParam String permissionObjectName,
            @RequestParam String beforeChange,
            @RequestParam String afterChange,
            @RequestParam String operationReason,
            @RequestParam String auditLevel,
            @RequestParam String operationResult,
            @RequestParam(required = false) String errorMessage
    );
}