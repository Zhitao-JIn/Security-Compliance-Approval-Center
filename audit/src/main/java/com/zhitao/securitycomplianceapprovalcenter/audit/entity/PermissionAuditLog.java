package com.zhitao.securitycomplianceapprovalcenter.audit.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限变更审计日志实体
 */
@Data
@Entity
@Table(name = "permission_audit_log")
public class PermissionAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 操作IP
     */
    private String operationIp;

    /**
     * 操作设备
     */
    private String operationDevice;

    /**
     * 权限对象类型
     */
    private String permissionObjectType;

    /**
     * 权限对象ID
     */
    private String permissionObjectId;

    /**
     * 权限对象名称
     */
    private String permissionObjectName;

    /**
     * 变更前状态
     */
    @Column(columnDefinition = "TEXT")
    private String beforeChange;

    /**
     * 变更后状态
     */
    @Column(columnDefinition = "TEXT")
    private String afterChange;

    /**
     * 操作原因
     */
    private String operationReason;

    /**
     * 审计级别
     */
    private String auditLevel;

    /**
     * 操作结果
     */
    private String operationResult;

    /**
     * 错误信息
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}