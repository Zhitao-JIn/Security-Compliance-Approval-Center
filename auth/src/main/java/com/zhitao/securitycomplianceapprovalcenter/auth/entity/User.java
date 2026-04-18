package com.zhitao.securitycomplianceapprovalcenter.auth.entity;


import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "auth_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String email;
    private String phone;

    /**
     * 0:禁用 1:启用
     */
    private Integer status = 1;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
