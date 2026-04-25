package com.zhitao.securitycomplianceapprovalcenter.business.dto;

import lombok.Data;
import java.util.List;

/**
 * 角色创建申请 DTO
 */
@Data
public class RoleCreateRequest {
    private String roleName;
    private String roleCode;
    private String description;
    private List<Long> permissionIds;  // 初始绑定的权限 ID 列表
}
