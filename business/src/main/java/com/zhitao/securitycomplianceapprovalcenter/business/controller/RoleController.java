package com.zhitao.securitycomplianceapprovalcenter.business.controller;

import com.zhitao.securitycomplianceapprovalcenter.business.dto.RoleCreateRequest;
import com.zhitao.securitycomplianceapprovalcenter.business.dto.RoleActionRequest;
import com.zhitao.securitycomplianceapprovalcenter.common.feign.ApprovalFeignClient;
import com.zhitao.securitycomplianceapprovalcenter.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色管理审批控制器
 */
@RestController
@RequestMapping("/api/business/role")
public class RoleController {

    @Autowired
    private ApprovalFeignClient approvalFeignClient;

    /**
     * 发起创建角色审批申请
     */
    @PostMapping("/create")
    public String applyCreateRole(HttpServletRequest request,
                                  @RequestBody RoleCreateRequest createRequest) {
        // 从请求 Header 中获取当前登录用户信息（网关透传）
        Long operatorId = Long.valueOf(request.getHeader("X-User-Id"));
        String operatorName = request.getHeader("X-User-Name");

        // 封装审批申请内容
        String permissionDesc = createRequest.getPermissionIds() != null
            ? "绑定权限 ID: " + createRequest.getPermissionIds()
            : "无初始权限";
        String operationContent = String.format("创建角色：%s (%s) - %s, %s",
                createRequest.getRoleName(), createRequest.getRoleCode(),
                createRequest.getDescription(), permissionDesc);

        Map<String, Object> requestDTO = new HashMap<>();
        requestDTO.put("applicantId", operatorId);
        requestDTO.put("applicantName", operatorName);
        requestDTO.put("applicantDepartment", operatorName);
        requestDTO.put("operationType", "CREATE_ROLE");
        requestDTO.put("operationContent", operationContent);
        requestDTO.put("operationReason", "新增系统角色");
        requestDTO.put("riskLevel", "HIGH");

        // 存储额外的角色信息，供回调时使用
        requestDTO.put("roleData", createRequest);

        // 远程调用审批服务创建申请
        Result<Map<String, Object>> response = approvalFeignClient.createApprovalRequest(requestDTO);

        return "角色创建审批申请提交成功，申请编号：" + response.getData().get("requestNo");
    }

    /**
     * 发起删除角色审批申请
     */
    @PostMapping("/delete")
    public String applyDeleteRole(HttpServletRequest request,
                                  @RequestBody RoleActionRequest actionRequest) {
        Long operatorId = Long.valueOf(request.getHeader("X-User-Id"));
        String operatorName = request.getHeader("X-User-Name");

        String operationContent = String.format("删除角色：%s (ID:%s)",
                actionRequest.getTargetRoleName(), actionRequest.getTargetRoleId());

        Map<String, Object> requestDTO = new HashMap<>();
        requestDTO.put("applicantId", operatorId);
        requestDTO.put("applicantName", operatorName);
        requestDTO.put("applicantDepartment", operatorName);
        requestDTO.put("operationType", "DELETE_ROLE");
        requestDTO.put("operationContent", operationContent);
        requestDTO.put("operationReason", actionRequest.getOperationReason());
        requestDTO.put("riskLevel", "HIGH");

        // 存储额外的操作信息，供回调时使用
        requestDTO.put("actionData", actionRequest);

        Result<Map<String, Object>> response = approvalFeignClient.createApprovalRequest(requestDTO);

        return "角色删除审批申请提交成功，申请编号：" + response.getData().get("requestNo");
    }

    /**
     * 发起角色绑定权限审批申请
     */
    @PostMapping("/bind-permission")
    public String applyBindRolePermission(HttpServletRequest request,
                                          @RequestBody RoleActionRequest actionRequest,
                                          @RequestParam List<Long> permissionIds) {
        Long operatorId = Long.valueOf(request.getHeader("X-User-Id"));
        String operatorName = request.getHeader("X-User-Name");

        String operationContent = String.format("为角色：%s (ID:%s) 绑定权限：%s",
                actionRequest.getTargetRoleName(), actionRequest.getTargetRoleId(),
                permissionIds);

        Map<String, Object> requestDTO = new HashMap<>();
        requestDTO.put("applicantId", operatorId);
        requestDTO.put("applicantName", operatorName);
        requestDTO.put("applicantDepartment", operatorName);
        requestDTO.put("operationType", "BIND_ROLE_PERMISSION");
        requestDTO.put("operationContent", operationContent);
        requestDTO.put("operationReason", actionRequest.getOperationReason());
        requestDTO.put("riskLevel", "HIGH");

        // 存储额外的操作信息，供回调时使用
        Map<String, Object> bindData = new HashMap<>();
        bindData.put("roleId", actionRequest.getTargetRoleId());
        bindData.put("roleName", actionRequest.getTargetRoleName());
        bindData.put("permissionIds", permissionIds);
        requestDTO.put("bindData", bindData);

        Result<Map<String, Object>> response = approvalFeignClient.createApprovalRequest(requestDTO);

        return "角色绑定权限审批申请提交成功，申请编号：" + response.getData().get("requestNo");
    }

    /**
     * 查询我的申请列表
     */
    @GetMapping("/my-apply")
    public List<Map<String, Object>> getMyRoleApplyList(HttpServletRequest request) {
        Long userId = Long.valueOf(request.getHeader("X-User-Id"));
        Result<List<Map<String, Object>>> response = approvalFeignClient.getMyRequests(userId);
        return response.getData();
    }
}
