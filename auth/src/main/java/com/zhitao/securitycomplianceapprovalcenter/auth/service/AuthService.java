package com.zhitao.securitycomplianceapprovalcenter.auth.service;


import com.zhitao.securitycomplianceapprovalcenter.auth.entity.Permission;
import com.zhitao.securitycomplianceapprovalcenter.auth.entity.Role;
import com.zhitao.securitycomplianceapprovalcenter.auth.entity.User;
import com.zhitao.securitycomplianceapprovalcenter.auth.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {
    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public String login(String username, String password) {
        User user = userService.getUserByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new RuntimeException("用户已被禁用");
        }
        return jwtUtil.generateToken(user);
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
