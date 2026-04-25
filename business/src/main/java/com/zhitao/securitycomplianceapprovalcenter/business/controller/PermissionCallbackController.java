package com.zhitao.securitycomplianceapprovalcenter.business.controller;

import com.zhitao.securitycomplianceapprovalcenter.common.feign.AuthFeignClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 权限变更回调控制器
 * 当审批流程通过后，执行实际的权限变更操作
 */
@RestController
@RequestMapping("/api/business/callback/permission")
@RequiredArgsConstructor
public class PermissionCallbackController {

    private final AuthFeignClient authFeignClient;

    /**
     * 权限分配审批通过回调，执行实际的权限分配
     */
    @PostMapping("/exec")
    public String execPermissionChange(@RequestParam Long targetUserId,
                                       @RequestParam Long targetRoleId,
                                       @RequestParam Long operatorId,
                                       @RequestParam String operatorName) {
        authFeignClient.assignRoleToUser(targetUserId, targetRoleId);
        return "权限变更执行成功";
    }

    /**
     * 权限移除审批通过回调，执行实际的权限移除
     */
    @PostMapping("/remove-exec")
    public String execPermissionRemove(@RequestParam Long targetUserId,
                                       @RequestParam Long targetRoleId,
                                       @RequestParam Long operatorId,
                                       @RequestParam String operatorName) {
        authFeignClient.removeUserRole(targetUserId, targetRoleId, operatorId, operatorName);
        return "权限移除执行成功";
    }
}
