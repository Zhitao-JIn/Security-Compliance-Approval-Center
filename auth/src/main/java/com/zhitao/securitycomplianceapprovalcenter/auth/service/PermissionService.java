package com.zhitao.securitycomplianceapprovalcenter.auth.service;

import com.zhitao.securitycomplianceapprovalcenter.auth.entity.Permission;
import com.zhitao.securitycomplianceapprovalcenter.auth.repository.PermissionRepository;
import com.zhitao.securitycomplianceapprovalcenter.common.feign.AuditFeignClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PermissionService {
    @Autowired
    private PermissionRepository permissionRepository;
    @Autowired
    private AuditFeignClient auditFeignClient;

    /**
     * 创建权限点
     */
    @Transactional
    public Permission createPermission(HttpServletRequest request,
                                       Permission permission,
                                       Long operatorId,
                                       String operatorName,
                                       String operationReason) {
        // 1. 校验权限编码唯一性
        if (permissionRepository.findByCode(permission.getCode()).isPresent()) {
            throw new RuntimeException("权限编码已存在：" + permission.getCode());
        }

        // 2. 初始化权限信息
        permission.setCreateTime(LocalDateTime.now());
        permission.setUpdateTime(LocalDateTime.now());

        // 3. 保存权限点
        Permission savedPermission = permissionRepository.save(permission);

        // 4. 记录审计日志
        String operationIp = request.getRemoteAddr();
        String operationDevice = request.getHeader("User-Agent");
        String afterChange = convertPermissionToJson(savedPermission);

        auditFeignClient.recordPermissionChange(
                operatorId,
                operatorName,
                "CREATE_PERMISSION",
                "PERMISSION",
                String.valueOf(savedPermission.getId()),
                savedPermission.getName(),
                "{}",
                afterChange,
                operationReason,
                "MEDIUM",
                "SUCCESS",
                null
        );

        return savedPermission;
    }

    /**
     * 更新权限点
     */
    @Transactional
    public Permission updatePermission(HttpServletRequest request,
                                       Long permissionId,
                                       Permission updatePermission,
                                       Long operatorId,
                                       String operatorName,
                                       String operationReason) {
        // 1. 查询原权限
        Permission oldPermission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("权限点不存在"));

        // 2. 保存变更前快照
        String beforeChange = convertPermissionToJson(oldPermission);

        // 3. 更新权限信息
        if (updatePermission.getName() != null) {
            oldPermission.setName(updatePermission.getName());
        }
        if (updatePermission.getDescription() != null) {
            oldPermission.setDescription(updatePermission.getDescription());
        }
        if (updatePermission.getResourcePath() != null) {
            oldPermission.setResourcePath(updatePermission.getResourcePath());
        }
        if (updatePermission.getOperationType() != null) {
            oldPermission.setOperationType(updatePermission.getOperationType());
        }
        oldPermission.setUpdateTime(LocalDateTime.now());

        // 4. 保存更新
        Permission updatedPermission = permissionRepository.save(oldPermission);

        // 5. 记录审计日志
        String afterChange = convertPermissionToJson(updatedPermission);
        String operationIp = request.getRemoteAddr();
        String operationDevice = request.getHeader("User-Agent");

        auditFeignClient.recordPermissionChange(
                operatorId,
                operatorName,
                "UPDATE_PERMISSION",
                "PERMISSION",
                String.valueOf(updatedPermission.getId()),
                updatedPermission.getName(),
                beforeChange,
                afterChange,
                operationReason,
                "MEDIUM",
                "SUCCESS",
                null
        );

        return updatedPermission;
    }

    /**
     * 删除权限点
     */
    @Transactional
    public void deletePermission(HttpServletRequest request,
                                 Long permissionId,
                                 Long operatorId,
                                 String operatorName,
                                 String operationReason) {
        // 1. 查询原权限
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("权限点不存在"));

        // 2. 保存变更前快照
        String beforeChange = convertPermissionToJson(permission);

        // 3. 删除权限点
        permissionRepository.delete(permission);

        // 4. 记录审计日志
        String operationIp = request.getRemoteAddr();
        String operationDevice = request.getHeader("User-Agent");

        auditFeignClient.recordPermissionChange(
                operatorId,
                operatorName,
                "DELETE_PERMISSION",
                "PERMISSION",
                String.valueOf(permissionId),
                permission.getName(),
                beforeChange,
                "{}",
                operationReason,
                "HIGH",
                "SUCCESS",
                null
        );
    }

    /**
     * 查询所有权限列表（直接查库）
     */
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    /**
     * 根据ID查询权限点
     */
    public Permission getPermissionById(Long permissionId) {
        return permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("权限点不存在"));
    }

    /**
     * 根据权限编码查询权限点
     */
    public Permission getPermissionByCode(String code) {
        return permissionRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("权限编码不存在"));
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 权限点转JSON（用于审计Diff对比）
     */
    private String convertPermissionToJson(Permission permission) {
        if (permission == null) {
            return "{}";
        }
        return String.format(
                "{\"id\":%d,\"code\":\"%s\",\"name\":\"%s\",\"description\":\"%s\",\"resourcePath\":\"%s\",\"operationType\":\"%s\"}",
                permission.getId(),
                permission.getCode(),
                permission.getName(),
                permission.getDescription() != null ? permission.getDescription() : "",
                permission.getResourcePath() != null ? permission.getResourcePath() : "",
                permission.getOperationType() != null ? permission.getOperationType() : ""
        );
    }
}
