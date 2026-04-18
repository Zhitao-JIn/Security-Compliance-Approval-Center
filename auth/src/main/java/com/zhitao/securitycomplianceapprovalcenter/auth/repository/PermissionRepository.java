package com.zhitao.securitycomplianceapprovalcenter.auth.repository;

import com.zhitao.securitycomplianceapprovalcenter.auth.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Permission findByCode(String code);
}