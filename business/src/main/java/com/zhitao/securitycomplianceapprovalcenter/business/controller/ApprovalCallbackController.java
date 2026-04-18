package com.zhitao.securitycomplianceapprovalcenter.business.controller;

import com.zhitao.securitycomplianceapprovalcenter.common.feign.AuthFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
        // 远程调用Auth Service执行权限变更
        authFeignClient.assignRoleToUser(targetUserId, targetRoleId, operatorId, operatorName);
        return "权限变更执行成功";
    }
}
