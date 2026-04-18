package com.zhitao.securitycomplianceapprovalcenter.auth.controller;

import com.zhitao.securitycomplianceapprovalcenter.auth.entity.Permission;
import com.zhitao.securitycomplianceapprovalcenter.auth.entity.Role;
import com.zhitao.securitycomplianceapprovalcenter.auth.service.PermissionService;
import com.zhitao.securitycomplianceapprovalcenter.auth.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permission")
@RequiredArgsConstructor
public class PermissionController {

    private PermissionService permissionService;

    private RoleService roleService;

    /**
     * 创建权限点
     */
    @PostMapping
    public Permission createPermission(HttpServletRequest request,
                                       @RequestBody Permission permission,
                                       @RequestParam Long operatorId,
                                       @RequestParam String operatorName,
                                       @RequestParam String operationReason) {
        return permissionService.createPermission(request, permission, operatorId, operatorName, operationReason);
    }

    /**
     * 查询所有权限列表
     */
    @GetMapping("/list")
    public List<Permission> getAllPermissions() {
        return permissionService.getAllPermissions();
    }

    /**
     * 给角色绑定权限
     */
    @PostMapping("/role/{roleId}/bind")
    public Role bindPermissionsToRole(HttpServletRequest request,
                                      @PathVariable Long roleId,
                                      @RequestBody List<Long> permissionIds,
                                      @RequestParam Long operatorId,
                                      @RequestParam String operatorName,
                                      @RequestParam String operationReason) {
        return roleService.bindPermissionsToRole(request, roleId, permissionIds, operatorId, operatorName, operationReason);
    }

    /**
     * 查询用户的权限列表
     */
    @GetMapping("/user/{userId}")
    public List<String> getUserPermissions(@PathVariable Long userId) {
        return roleService.getUserPermissionCodes(userId);
    }

    /**
     * 校验用户是否拥有指定权限（给网关调用）
     */
    @GetMapping("/check")
    public boolean checkPermission(@RequestParam Long userId, @RequestParam String permissionCode) {
        return roleService.hasPermission(userId, permissionCode);
    }
}