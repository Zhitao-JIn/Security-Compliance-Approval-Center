package com.zhitao.securitycomplianceapprovalcenter.business.dto;

import lombok.Data;

/**
 * 角色操作申请 DTO（删除/绑定权限）
 */
@Data
public class RoleActionRequest {
    private Long targetRoleId;
    private String targetRoleName;
    private String operationReason;
}
