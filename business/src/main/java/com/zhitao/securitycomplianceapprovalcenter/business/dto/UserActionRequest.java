package com.zhitao.securitycomplianceapprovalcenter.business.dto;

import lombok.Data;

/**
 * 用户操作申请 DTO（禁用/启用/重置密码）
 */
@Data
public class UserActionRequest {
    private Long targetUserId;
    private String targetUsername;
    private String operationReason;
}
