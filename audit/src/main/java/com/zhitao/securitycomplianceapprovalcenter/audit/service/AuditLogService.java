package com.zhitao.securitycomplianceapprovalcenter.audit.service;


import com.zhitao.securitycomplianceapprovalcenter.audit.entity.PermissionAuditLog;
import com.zhitao.securitycomplianceapprovalcenter.audit.repository.PermissionAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService{

    final  PermissionAuditLogRepository auditLogRepository;

    @Transactional(rollbackFor = Exception.class)
    public void recordAuditLog(PermissionAuditLog log) {
        // 自动补全时间
        log.setOperationTime(LocalDateTime.now());
        // 保存审计日志
        auditLogRepository.save(log);
    }
}