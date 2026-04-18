package com.zhitao.securitycomplianceapprovalcenter.audit.controller;

import com.zhitao.securitycomplianceapprovalcenter.audit.entity.PermissionAuditLog;
import com.zhitao.securitycomplianceapprovalcenter.audit.service.AuditLogService;
import com.zhitao.securitycomplianceapprovalcenter.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit/log")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * 记录权限变更审计日志
     * 给 权限服务 / 审批服务 调用
     */
    @PostMapping("/record")
    public Result<Void> record(
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
            @RequestParam(required = false) String errorMessage,
            @RequestParam(required = false) String clientIp
    ) {
        // 构建审计日志（类名固定：PermissionAuditLog）
        PermissionAuditLog log = new PermissionAuditLog();
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setOperationType(operationType);
        log.setPermissionObjectType(permissionObjectType);
        log.setPermissionObjectId(permissionObjectId);
        log.setPermissionObjectName(permissionObjectName);
        log.setBeforeChange(beforeChange);
        log.setAfterChange(afterChange);
        log.setOperationReason(operationReason);
        log.setAuditLevel(auditLevel);
        log.setOperationResult(operationResult);
        log.setErrorMessage(errorMessage);
        log.setClientIp(clientIp);
        log.setServiceName("audit-service");

        // 保存日志
        auditLogService.recordAuditLog(log);

        return Result.success(null);
    }
}