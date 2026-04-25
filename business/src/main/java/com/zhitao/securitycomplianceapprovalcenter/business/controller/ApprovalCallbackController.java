package com.zhitao.securitycomplianceapprovalcenter.business.controller;

import com.zhitao.securitycomplianceapprovalcenter.common.feign.AuthFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 审批回调执行控制器
 * 当审批流程通过后，调用相应的回调接口执行实际业务操作
 */
@RestController
@RequestMapping("/api/business/callback")
public class ApprovalCallbackController {

    @Autowired
    private AuthFeignClient authFeignClient;

    /**
     * 权限变更审批通过回调，执行实际的权限分配
     */
    @PostMapping("/permission/exec")
    public String execPermissionChange(@RequestParam Long targetUserId,
                                       @RequestParam Long targetRoleId,
                                       @RequestParam Long operatorId,
                                       @RequestParam String operatorName) {
        // 远程调用 Auth Service 执行权限变更
        authFeignClient.assignRoleToUser(targetUserId, targetRoleId);
        return "权限变更执行成功";
    }

    /**
     * 权限移除审批通过回调，执行实际的权限移除
     */
    @PostMapping("/permission/remove-exec")
    public String execPermissionRemove(@RequestParam Long targetUserId,
                                       @RequestParam Long targetRoleId,
                                       @RequestParam Long operatorId,
                                       @RequestParam String operatorName) {
        // 远程调用 Auth Service 执行权限移除
        authFeignClient.removeUserRole(targetUserId, targetRoleId, operatorId, operatorName);
        return "权限移除执行成功";
    }

    /**
     * 用户创建审批通过回调，执行实际的用户创建
     */
    @PostMapping("/user/create-exec")
    public String execUserCreate(@RequestBody Map<String, Object> userData) {
        // 远程调用 Auth Service 执行用户创建
        authFeignClient.createUser(userData);
        return "用户创建执行成功";
    }

    /**
     * 用户禁用审批通过回调，执行实际的用户禁用
     */
    @PostMapping("/user/disable-exec")
    public String execUserDisable(@RequestParam Long targetUserId,
                                  @RequestParam Long operatorId,
                                  @RequestParam String operatorName) {
        // 远程调用 Auth Service 执行用户禁用
        authFeignClient.updateUserStatus(targetUserId, false, operatorId, operatorName);
        return "用户禁用执行成功";
    }

    /**
     * 用户启用审批通过回调，执行实际的用户启用
     */
    @PostMapping("/user/enable-exec")
    public String execUserEnable(@RequestParam Long targetUserId,
                                 @RequestParam Long operatorId,
                                 @RequestParam String operatorName) {
        // 远程调用 Auth Service 执行用户启用
        authFeignClient.updateUserStatus(targetUserId, true, operatorId, operatorName);
        return "用户启用执行成功";
    }

    /**
     * 用户重置密码审批通过回调，执行实际的密码重置
     */
    @PostMapping("/user/reset-password-exec")
    public String execUserResetPassword(@RequestParam Long targetUserId,
                                        @RequestParam Long operatorId,
                                        @RequestParam String operatorName) {
        // 远程调用 Auth Service 执行密码重置
        authFeignClient.resetUserPassword(targetUserId, operatorId, operatorName);
        return "用户密码重置执行成功";
    }

    /**
     * 角色创建审批通过回调，执行实际的角色创建
     */
    @PostMapping("/role/create-exec")
    public String execRoleCreate(@RequestBody Map<String, Object> roleData) {
        // 远程调用 Auth Service 执行角色创建
        authFeignClient.createRole(roleData);
        return "角色创建执行成功";
    }

    /**
     * 角色删除审批通过回调，执行实际的角色删除
     */
    @PostMapping("/role/delete-exec")
    public String execRoleDelete(@RequestParam Long targetRoleId,
                                 @RequestParam Long operatorId,
                                 @RequestParam String operatorName) {
        // 远程调用 Auth Service 执行角色删除
        authFeignClient.deleteRole(targetRoleId, operatorId, operatorName);
        return "角色删除执行成功";
    }

    /**
     * 角色绑定权限审批通过回调，执行实际的权限绑定
     */
    @PostMapping("/role/bind-permission-exec")
    public String execRoleBindPermission(@RequestParam Long targetRoleId,
                                         @RequestParam List<Long> permissionIds,
                                         @RequestParam Long operatorId,
                                         @RequestParam String operatorName) {
        // 远程调用 Auth Service 执行权限绑定
        authFeignClient.bindPermissionsToRole(targetRoleId, permissionIds, operatorId, operatorName);
        return "角色权限绑定执行成功";
    }
}
