package com.zhitao.securitycomplianceapprovalcenter.auth.controller;

import com.zhitao.securitycomplianceapprovalcenter.auth.entity.User;
import com.zhitao.securitycomplianceapprovalcenter.auth.service.AuthService;
import com.zhitao.securitycomplianceapprovalcenter.auth.service.UserService;
import com.zhitao.securitycomplianceapprovalcenter.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password) {
        return authService.login(username, password);
    }

    /**
     * 创建用户（直接调用）
     */
    @PostMapping("/user")
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    /**
     * 创建用户（Feign 回调接口，审批通过后调用）
     */
    @PostMapping("/user/create")
    public Result<Map<String, Object>> createUserByFeign(@RequestBody Map<String, Object> userDTO) {
        Map<String, Object> response = userService.createUserByMap(userDTO);
        return Result.success("用户创建成功", response);
    }

    /**
     * 更新用户状态（Feign 回调接口）
     */
    @PostMapping("/user/status")
    public Result<Void> updateUserStatus(@RequestParam Long userId,
                                         @RequestParam boolean enabled,
                                         @RequestParam Long operatorId,
                                         @RequestParam String operatorName) {
        userService.updateUserStatus(userId, enabled, operatorId, operatorName);
        return Result.success(null);
    }

    /**
     * 重置用户密码（Feign 回调接口）
     */
    @PostMapping("/user/reset-password")
    public Result<Void> resetUserPassword(@RequestParam Long userId,
                                          @RequestParam Long operatorId,
                                          @RequestParam String operatorName) {
        userService.resetUserPassword(userId, operatorId, operatorName);
        return Result.success(null);
    }
}
