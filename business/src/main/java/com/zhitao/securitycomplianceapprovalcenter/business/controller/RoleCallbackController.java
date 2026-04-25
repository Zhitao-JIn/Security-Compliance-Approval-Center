package com.zhitao.securitycomplianceapprovalcenter.business.controller;

import com.zhitao.securitycomplianceapprovalcenter.common.feign.AuthFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色管理回调控制器
 * 当审批流程通过后，执行实际的角色管理操作
 */
@RestController
@RequestMapping("/api/business/callback/role")
@RequiredArgsConstructor
public class RoleCallbackController {

    private final AuthFeignClient authFeignClient;

    /**
     * 角色创建审批通过回调，执行实际的角色创建
     */
    @PostMapping("/create-exec")
    public String execRoleCreate(@RequestBody Map<String, Object> roleData) {
        authFeignClient.createRole(roleData);
        return "角色创建执行成功";
    }

    /**
     * 角色删除审批通过回调，执行实际的角色删除
     */
    @PostMapping("/delete-exec")
    public String execRoleDelete(@RequestParam Long targetRoleId,
                                 @RequestParam Long operatorId,
                                 @RequestParam String operatorName) {
        authFeignClient.deleteRole(targetRoleId, operatorId, operatorName);
        return "角色删除执行成功";
    }

    /**
     * 角色绑定权限审批通过回调，执行实际的权限绑定
     */
    @PostMapping("/bind-permission-exec")
    public String execRoleBindPermission(@RequestParam Long targetRoleId,
                                         @RequestParam List<Long> permissionIds,
                                         @RequestParam Long operatorId,
                                         @RequestParam String operatorName) {
        authFeignClient.bindPermissionsToRole(targetRoleId, permissionIds, operatorId, operatorName);
        return "角色权限绑定执行成功";
    }
}
