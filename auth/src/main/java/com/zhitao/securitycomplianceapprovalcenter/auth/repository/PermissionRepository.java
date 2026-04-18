package com.zhitao.securitycomplianceapprovalcenter.auth.repository;

import com.zhitao.securitycomplianceapprovalcenter.auth.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByCode(String code);
}