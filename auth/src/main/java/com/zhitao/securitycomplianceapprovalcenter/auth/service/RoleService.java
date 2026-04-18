package com.zhitao.securitycomplianceapprovalcenter.auth.service;
import com.zhitao.securitycomplianceapprovalcenter.auth.entity.Permission;
import com.zhitao.securitycomplianceapprovalcenter.auth.entity.Role;
import com.zhitao.securitycomplianceapprovalcenter.auth.entity.UserRole;
import com.zhitao.securitycomplianceapprovalcenter.auth.repository.PermissionRepository;
import com.zhitao.securitycomplianceapprovalcenter.auth.repository.RoleRepository;
import com.zhitao.securitycomplianceapprovalcenter.auth.repository.UserRoleRepository;
import com.zhitao.securitycomplianceapprovalcenter.common.feign.AuditFeignClient;
import com.zhitao.securitycomplianceapprovalcenter.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private RoleRepository roleRepository;

    private PermissionRepository permissionRepository;

    private UserRoleRepository userRoleRepository;

    private AuditFeignClient auditFeignClient;

    private RoleService roleService;

    @Transactional
    public Role createRole(Role role) {
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        return roleRepository.save(role);
    }

    @Transactional
    public void assignRoleToUser(Long userId, Long roleId) {
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRole.setCreateTime(LocalDateTime.now());
        userRoleRepository.save(userRole);
    }

    public List<Role> getRolesByUserId(Long userId) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        List<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());
        return roleRepository.findAllById(roleIds);
    }

    @Transactional
    public Role bindPermissionsToRole(HttpServletRequest request,
                                      Long roleId,
                                      List<Long> permissionIds,
                                      Long operatorId,
                                      String operatorName,
                                      String operationReason) {
        // 查询角色
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("角色不存在"));

        // 保存变更前的权限快照
        String beforeChange = convertPermissionsToJson(role.getPermissions());

        // 查询权限列表并绑定
        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        role.setPermissions(permissions);
        role.setUpdateTime(LocalDateTime.now());

        // 保存角色
        Role updatedRole = roleRepository.save(role);

        // 变更后的权限快照
        String afterChange = convertPermissionsToJson(updatedRole.getPermissions());

        // 调用审计服务，记录权限变更日志
        Result<Void> auditResult = auditFeignClient.recordPermissionChange(
                operatorId,
                operatorName,
                "BIND_ROLE_PERMISSION",
                "ROLE_PERMISSION",
                String.valueOf(roleId),
                role.getName(),
                beforeChange,
                afterChange,
                operationReason,
                "HIGH",
                "SUCCESS",
                null
        );

        return updatedRole;
    }

    /**
     * 查询用户的所有权限列表（核心：鉴权的依据）
     */
    public List<String> getUserPermissionCodes(Long userId) {
        // 查询用户的所有角色
        List<Role> userRoles = getRolesByUserId(userId);
        // 提取所有角色的权限编码，去重
        return userRoles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 权限列表转JSON（用于审计Diff对比）
     */
    private String convertPermissionsToJson(List<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return "[]";
        }
        return permissions.stream()
                .map(p -> "{\"id\":" + p.getId() + ",\"code\":\"" + p.getCode() + "\",\"name\":\"" + p.getName() + "\"}")
                .collect(Collectors.joining(",", "[", "]"));
    }

    /**
     * 校验用户是否拥有指定权限
     * @param userId 用户ID
     * @param permissionCode 目标权限编码
     * @return 是否拥有权限
     */
    public boolean hasPermission(Long userId, String permissionCode) {
        // 1. 获取用户的所有权限编码列表
        List<String> userPermissionCodes = roleService.getUserPermissionCodes(userId);

        // 2. 校验：要么拥有目标权限，要么是超管（*:*:*）
        return userPermissionCodes.contains(permissionCode)
                || userPermissionCodes.contains("*:*:*");
    }
}
