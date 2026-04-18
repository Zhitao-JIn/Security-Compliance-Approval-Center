package com.zhitao.securitycomplianceapprovalcenter.auth.service;


import com.zhitao.securitycomplianceapprovalcenter.auth.entity.Permission;
import com.zhitao.securitycomplianceapprovalcenter.auth.entity.Role;
import com.zhitao.securitycomplianceapprovalcenter.auth.entity.User;
import com.zhitao.securitycomplianceapprovalcenter.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private UserService userService;

    private RoleService roleService;

    private PasswordEncoder passwordEncoder;

    private JwtUtil jwtUtil;

    public String login(String username, String password) {
        User user = userService.getUserByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new RuntimeException("用户已被禁用");
        }

        // 核心：查询用户的所有权限编码
        List<String> permissionCodes = roleService.getUserPermissionCodes(user.getId());

        // 把用户ID、用户名、权限列表都放入JWT载荷
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("permissions", permissionCodes); // 权限列表注入Token

        return jwtUtil.generateToken(claims,user);
    }


    public boolean hasPermission(Long userId, String permissionCode) {
        List<Role> roles = roleService.getRolesByUserId(userId);
        for (Role role : roles) {
            for (Permission permission : role.getPermissions()) {
                if (permission.getCode().equals(permissionCode)) {
                    return true;
                }
            }
        }
        return false;
    }
}
