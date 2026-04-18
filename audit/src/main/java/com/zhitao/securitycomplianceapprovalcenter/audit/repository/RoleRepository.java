package com.zhitao.securitycomplianceapprovalcenter.audit.repository;

import com.zhitao.securitycomplianceapprovalcenter.audit.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByName(String name);
}
