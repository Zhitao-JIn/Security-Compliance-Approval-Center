package com.zhitao.securitycomplianceapprovalcenter.auth.repository;

import com.zhitao.securitycomplianceapprovalcenter.auth.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findByUserId(Long userId);
    List<UserRole> findByRoleId(Long roleId);
    List<UserRole> findByUserIdAndRoleId(Long userId, Long roleId);
    void deleteByUserIdAndRoleId(Long userId, Long roleId);
}