package com.zhitao.securitycomplianceapprovalcenter.auth.repository;

import com.zhitao.securitycomplianceapprovalcenter.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
