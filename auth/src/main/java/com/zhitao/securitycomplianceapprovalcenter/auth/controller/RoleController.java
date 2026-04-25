package com.zhitao.securitycomplianceapprovalcenter.auth.controller;

import com.zhitao.securitycomplianceapprovalcenter.auth.entity.Role;
import com.zhitao.securitycomplianceapprovalcenter.auth.service.RoleService;
import com.zhitao.securitycomplianceapprovalcenter.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 创建角色（直接调用）
     */
    @PostMapping
    public Result<Role> createRole(@RequestBody Role role) {
        Role createdRole = roleService.createRole(role);
        return Result.success("角色创建成功", createdRole);
    }

    /**
     * 创建角色（Feign 回调接口，审批通过后调用）
     */
    @PostMapping("/create")
    public Result<Map<String, Object>> createRoleByFeign(@RequestBody Map<String, Object> roleDTO) {
        Role role = new Role();
        role.setName(roleDTO.get("roleName").toString());
        role.setCode(roleDTO.get("roleCode").toString());
        role.setDescription(roleDTO.get("description").toString());

        Role createdRole = roleService.createRole(role);

        // 如果有初始权限 ID，绑定权限
        if (roleDTO.containsKey("permissionIds")) {
            List<Long> permissionIds = (List<Long>) roleDTO.get("permissionIds");
            Long operatorId = Long.valueOf(roleDTO.get("operatorId").toString());
            String operatorName = roleDTO.get("operatorName").toString();
            roleService.bindPermissionsToRole(createdRole.getId(), permissionIds, operatorId, operatorName);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("roleId", createdRole.getId());
        response.put("roleCode", createdRole.getCode());
        return Result.success("角色创建成功", response);
    }

    /**
     * 给用户分配角色（Feign 接口）
     */
    @PostMapping("/assign")
    public Result<Void> assignRoleToUser(@RequestParam Long userId,
                                         @RequestParam Long roleId) {
        roleService.assignRoleToUser(userId, roleId);
        return Result.success(null);
    }

    /**
     * 移除用户角色（Feign 接口）
     */
    @PostMapping("/remove")
    public Result<Void> removeUserRole(@RequestParam Long userId,
                                       @RequestParam Long roleId,
                                       @RequestParam Long operatorId,
                                       @RequestParam String operatorName) {
        roleService.removeUserRole(userId, roleId, operatorId, operatorName);
        return Result.success(null);
    }

    /**
     * 删除角色（Feign 回调接口）
     */
    @PostMapping("/delete")
    public Result<Void> deleteRole(@RequestParam Long roleId,
                                   @RequestParam Long operatorId,
                                   @RequestParam String operatorName) {
        roleService.deleteRole(roleId, operatorId, operatorName);
        return Result.success(null);
    }

    /**
     * 角色绑定权限（Feign 回调接口）
     */
    @PostMapping("/bind-permissions")
    public Result<Void> bindPermissionsToRole(@RequestParam Long roleId,
                                              @RequestParam List<Long> permissionIds,
                                              @RequestParam Long operatorId,
                                              @RequestParam String operatorName) {
        roleService.bindPermissionsToRole(roleId, permissionIds, operatorId, operatorName);
        return Result.success(null);
    }
}
