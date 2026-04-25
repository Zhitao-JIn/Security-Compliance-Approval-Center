package com.zhitao.securitycomplianceapprovalcenter.business.dto;

import lombok.Data;

/**
 * 权限移除申请 DTO
 */
@Data
public class PermissionRemoveRequest {
    private Long targetUserId;
    private String targetUsername;
    private Long targetRoleId;
    private String targetRoleName;
    private String operationReason;
}
