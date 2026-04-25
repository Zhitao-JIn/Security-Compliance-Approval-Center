package com.zhitao.securitycomplianceapprovalcenter.business.controller;

import com.zhitao.securitycomplianceapprovalcenter.common.feign.AuthFeignClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户管理回调控制器
 * 当审批流程通过后，执行实际的用户管理操作
 */
@RestController
@RequestMapping("/api/business/callback/user")
@RequiredArgsConstructor
public class UserCallbackController {

    private final AuthFeignClient authFeignClient;

    /**
     * 用户创建审批通过回调，执行实际的用户创建
     */
    @PostMapping("/create-exec")
    public String execUserCreate(@RequestBody Map<String, Object> userData) {
        authFeignClient.createUser(userData);
        return "用户创建执行成功";
    }

    /**
     * 用户禁用审批通过回调，执行实际的用户禁用
     */
    @PostMapping("/disable-exec")
    public String execUserDisable(@RequestParam Long targetUserId,
                                  @RequestParam Long operatorId,
                                  @RequestParam String operatorName) {
        authFeignClient.updateUserStatus(targetUserId, false, operatorId, operatorName);
        return "用户禁用执行成功";
    }

    /**
     * 用户启用审批通过回调，执行实际的用户启用
     */
    @PostMapping("/enable-exec")
    public String execUserEnable(@RequestParam Long targetUserId,
                                 @RequestParam Long operatorId,
                                 @RequestParam String operatorName) {
        authFeignClient.updateUserStatus(targetUserId, true, operatorId, operatorName);
        return "用户启用执行成功";
    }

    /**
     * 用户重置密码审批通过回调，执行实际的密码重置
     */
    @PostMapping("/reset-password-exec")
    public String execUserResetPassword(@RequestParam Long targetUserId,
                                        @RequestParam Long operatorId,
                                        @RequestParam String operatorName) {
        authFeignClient.resetUserPassword(targetUserId, operatorId, operatorName);
        return "用户密码重置执行成功";
    }
}
