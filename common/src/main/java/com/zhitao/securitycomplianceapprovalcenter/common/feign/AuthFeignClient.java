package com.zhitao.securitycomplianceapprovalcenter.common.feign;

import com.zhitao.securitycomplianceapprovalcenter.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 认证服务 Feign 调用接口
 */
@FeignClient(name = "auth-service", url = "${service.auth.url:http://localhost:8081}")
public interface AuthFeignClient {

    @PostMapping("/api/role/assign")
    Result<Void> assignRoleToUser(@RequestParam Long userId,
                                  @RequestParam Long roleId);

    @PostMapping("/api/role/remove")
    Result<Void> removeUserRole(@RequestParam Long userId,
                                @RequestParam Long roleId,
                                @RequestParam Long operatorId,
                                @RequestParam String operatorName);

    @GetMapping("/api/permission/check")
    Result<Boolean> checkPermission(@RequestParam Long userId, @RequestParam String permissionCode);

    // ==================== 用户管理 ====================

    /**
     * 创建用户（审批回调调用）
     */
    @PostMapping("/api/user/create")
    Result<Map<String, Object>> createUser(@RequestBody Map<String, Object> userDTO);

    /**
     * 禁用/启用用户（审批回调调用）
     */
    @PostMapping("/api/user/status")
    Result<Void> updateUserStatus(@RequestParam Long userId,
                                  @RequestParam boolean enabled,
                                  @RequestParam Long operatorId,
                                  @RequestParam String operatorName);

    /**
     * 重置用户密码（审批回调调用）
     */
    @PostMapping("/api/user/reset-password")
    Result<Void> resetUserPassword(@RequestParam Long userId,
                                   @RequestParam Long operatorId,
                                   @RequestParam String operatorName);

    // ==================== 角色管理 ====================

    /**
     * 创建角色（审批回调调用）
     */
    @PostMapping("/api/role/create")
    Result<Map<String, Object>> createRole(@RequestBody Map<String, Object> roleDTO);

    /**
     * 删除角色（审批回调调用）
     */
    @PostMapping("/api/role/delete")
    Result<Void> deleteRole(@RequestParam Long roleId,
                            @RequestParam Long operatorId,
                            @RequestParam String operatorName);

    /**
     * 角色绑定权限（审批回调调用）
     */
    @PostMapping("/api/role/bind-permissions")
    Result<Void> bindPermissionsToRole(@RequestParam Long roleId,
                                       @RequestParam List<Long> permissionIds,
                                       @RequestParam Long operatorId,
                                       @RequestParam String operatorName);
}
