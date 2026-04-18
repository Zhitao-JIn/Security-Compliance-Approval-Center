package com.zhitao.securitycomplianceapprovalcenter.audit.repository;

import com.zhitao.securitycomplianceapprovalcenter.audit.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Permission findByCode(String code);
}