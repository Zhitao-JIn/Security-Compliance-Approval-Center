package com.zhitao.securitycomplianceapprovalcenter.auth.service;
import com.zhitao.securitycomplianceapprovalcenter.auth.entity.Role;
import com.zhitao.securitycomplianceapprovalcenter.auth.entity.UserRole;
import com.zhitao.securitycomplianceapprovalcenter.auth.repository.RoleRepository;
import com.zhitao.securitycomplianceapprovalcenter.auth.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private RoleRepository roleRepository;


    private UserRoleRepository userRoleRepository;

    @Transactional
    public Role createRole(Role role) {
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        return roleRepository.save(role);
    }

    @Transactional
    public void assignRoleToUser(Long userId, Long roleId) {
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRole.setCreateTime(LocalDateTime.now());
        userRoleRepository.save(userRole);
    }

    public List<Role> getRolesByUserId(Long userId) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        List<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());
        return roleRepository.findAllById(roleIds);
    }
}
