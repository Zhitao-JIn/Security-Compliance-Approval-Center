package com.zhitao.securitycomplianceapprovalcenter.auth.repository;

import com.zhitao.securitycomplianceapprovalcenter.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByName(String name);

    Role findByCode(String code);
}
