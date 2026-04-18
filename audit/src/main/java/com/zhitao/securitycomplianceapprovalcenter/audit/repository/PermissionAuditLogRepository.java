package com.zhitao.securitycomplianceapprovalcenter.audit.repository;

import com.zhitao.securitycomplianceapprovalcenter.audit.entity.PermissionAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 权限审计日志Repository
 */
@Repository
public interface PermissionAuditLogRepository extends JpaRepository<PermissionAuditLog, Long> {

    /**
     * 查询审计日志
     */
    @Query(
            value = "SELECT * FROM permission_audit_log log " +
                    "WHERE (:operatorName IS NULL OR log.operator_name LIKE CONCAT('%', :operatorName, '%')) " +
                    "AND (:operationType IS NULL OR log.operation_type = :operationType) " +
                    "AND (:permissionObjectType IS NULL OR log.permission_object_type = :permissionObjectType) " +
                    "AND (:auditLevel IS NULL OR log.audit_level = :auditLevel) " +
                    "AND (:startTime IS NULL OR log.operation_time >= :startTime) " +
                    "AND (:endTime IS NULL OR log.operation_time <= :endTime) " +
                    "ORDER BY log.operation_time DESC " +
                    "LIMIT :size OFFSET :offset",
            nativeQuery = true
    )
    List<PermissionAuditLog> findAuditLogs(
            @Param("operatorName") String operatorName,
            @Param("operationType") String operationType,
            @Param("permissionObjectType") String permissionObjectType,
            @Param("auditLevel") String auditLevel,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("size") int size,    // 每页条数
            @Param("offset") int offset // 偏移量 = page * size
    );

    /**
     * 根据操作人查询审计日志
     */
    List<PermissionAuditLog> findByOperatorName(String operatorName);

    /**
     * 根据操作类型查询审计日志
     */
    List<PermissionAuditLog> findByOperationType(String operationType);

    /**
     * 根据权限对象类型查询审计日志
     */
    List<PermissionAuditLog> findByPermissionObjectType(String permissionObjectType);
}