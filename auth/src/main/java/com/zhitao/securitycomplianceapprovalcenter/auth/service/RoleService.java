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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    private final PermissionRepository permissionRepository;

    private final UserRoleRepository userRoleRepository;

    private final AuditFeignClient auditFeignClient;

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

    /**
     * 删除角色
     */
    @Transactional
    public void deleteRole(Long roleId, Long operatorId, String operatorName) {
        // 先删除该角色关联的所有用户角色关系
        List<UserRole> userRoles = userRoleRepository.findByRoleId(roleId);
        for (UserRole userRole : userRoles) {
            userRoleRepository.delete(userRole);
        }
        // 删除角色
        roleRepository.deleteById(roleId);
    }

    /**
     * 角色绑定权限（不带 HttpServletRequest 版本，供 Feign 调用）
     */
    @Transactional
    public void bindPermissionsToRole(Long roleId, List<Long> permissionIds, Long operatorId, String operatorName) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("角色不存在"));

        String beforeChange = convertPermissionsToJson(role.getPermissions());

        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        role.setPermissions(permissions);
        role.setUpdateTime(LocalDateTime.now());

        roleRepository.save(role);

        String afterChange = convertPermissionsToJson(role.getPermissions());

        // 写入审计日志
        try {
            auditFeignClient.recordPermissionChange(
                    operatorId,
                    operatorName,
                    "BIND_ROLE_PERMISSION",
                    "ROLE_PERMISSION",
                    String.valueOf(roleId),
                    role.getName(),
                    beforeChange,
                    afterChange,
                    "角色绑定权限",
                    "HIGH",
                    "SUCCESS",
                    null
            );
        } catch (Exception e) {
            // 审计服务调用失败不影响主流程
        }
    }

    @Transactional
    public void removeUserRole(Long userId, Long roleId, Long operatorId, String operatorName) {
        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
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
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("角色不存在"));

        String beforeChange = convertPermissionsToJson(role.getPermissions());

        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        role.setPermissions(permissions);
        role.setUpdateTime(LocalDateTime.now());

        Role updatedRole = roleRepository.save(role);

        String afterChange = convertPermissionsToJson(updatedRole.getPermissions());

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

    public List<String> getUserPermissionCodes(Long userId) {
        List<Role> userRoles = getRolesByUserId(userId);
        return userRoles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .distinct()
                .collect(Collectors.toList());
    }

    private String convertPermissionsToJson(List<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return "[]";
        }
        return permissions.stream()
                .map(p -> "{\"id\":" + p.getId() + ",\"code\":\"" + p.getCode() + "\",\"name\":\"" + p.getName() + "\"}")
                .collect(Collectors.joining(",", "[", "]"));
    }

    public boolean hasPermission(Long userId, String permissionCode) {
        List<String> userPermissionCodes = getUserPermissionCodes(userId);
        return userPermissionCodes.contains(permissionCode)
                || userPermissionCodes.contains("*:*:*");
    }

    /**
     * 判断用户是否有指定角色（通过角色编码判断）
     */
    public boolean userHasRole(Long userId, String roleCode) {
        List<Role> userRoles = getRolesByUserId(userId);
        return userRoles.stream()
                .anyMatch(role -> roleCode.equals(role.getCode()));
    }
}
