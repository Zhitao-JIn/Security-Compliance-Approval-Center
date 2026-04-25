package com.zhitao.securitycomplianceapprovalcenter.business.dto;

import lombok.Data;

/**
 * 用户创建申请 DTO
 */
@Data
public class UserCreateRequest {
    private String username;
    private String email;
    private String phone;
    private String department;
    private String position;
}
