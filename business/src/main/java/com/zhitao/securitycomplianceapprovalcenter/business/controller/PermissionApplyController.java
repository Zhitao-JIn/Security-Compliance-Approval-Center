package com.zhitao.securitycomplianceapprovalcenter.business.controller;


import com.zhitao.securitycomplianceapprovalcenter.business.dto.PermissionRemoveRequest;
import com.zhitao.securitycomplianceapprovalcenter.common.feign.ApprovalFeignClient;
import com.zhitao.securitycomplianceapprovalcenter.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限分配审批控制器
 * 负责发起权限分配/移除的审批申请
 */
@RestController
@RequestMapping("/api/business/permission")
@RequiredArgsConstructor
public class PermissionApplyController {

    private final ApprovalFeignClient approvalFeignClient;

    /**
     * 发起权限变更审批申请（分配角色）
     */
    @PostMapping("/apply")
    public String applyPermissionChange(HttpServletRequest request,
                                        @RequestParam Long targetUserId,
                                        @RequestParam Long targetRoleId,
                                        @RequestParam String operationReason) {
        Long operatorId = Long.valueOf(request.getHeader("X-User-Id"));
        String operatorName = request.getHeader("X-User-Name");

        String operationContent = String.format("给用户 ID:%s 分配角色 ID:%s", targetUserId, targetRoleId);
        Map<String, Object> requestDTO = new HashMap<>();
        requestDTO.put("applicantId", operatorId);
        requestDTO.put("applicantName", operatorName);
        requestDTO.put("applicantDepartment", operatorName);
        requestDTO.put("operationType", "MODIFY_PERMISSION");
        requestDTO.put("operationContent", operationContent);
        requestDTO.put("operationReason", operationReason);
        requestDTO.put("riskLevel", "HIGH");

        Result<Map<String, Object>> response = approvalFeignClient.createApprovalRequest(requestDTO);
        return "审批申请提交成功，申请编号：" + response.getData().get("requestNo");
    }

    /**
     * 发起移除权限审批申请
     */
    @PostMapping("/remove")
    public String applyRemovePermission(HttpServletRequest request,
                                        @RequestBody PermissionRemoveRequest removeRequest) {
        Long operatorId = Long.valueOf(request.getHeader("X-User-Id"));
        String operatorName = request.getHeader("X-User-Name");

        String operationContent = String.format("移除用户 ID:%s 的角色 ID:%s",
                removeRequest.getTargetUserId(), removeRequest.getTargetRoleId());

        Map<String, Object> requestDTO = new HashMap<>();
        requestDTO.put("applicantId", operatorId);
        requestDTO.put("applicantName", operatorName);
        requestDTO.put("applicantDepartment", operatorName);
        requestDTO.put("operationType", "REMOVE_PERMISSION");
        requestDTO.put("operationContent", operationContent);
        requestDTO.put("operationReason", removeRequest.getOperationReason());
        requestDTO.put("riskLevel", "HIGH");

        Map<String, Object> removeData = new HashMap<>();
        removeData.put("targetUserId", removeRequest.getTargetUserId());
        removeData.put("targetUsername", removeRequest.getTargetUsername());
        removeData.put("targetRoleId", removeRequest.getTargetRoleId());
        removeData.put("targetRoleName", removeRequest.getTargetRoleName());
        requestDTO.put("removeData", removeData);

        Result<Map<String, Object>> response = approvalFeignClient.createApprovalRequest(requestDTO);
        return "权限移除审批申请提交成功，申请编号：" + response.getData().get("requestNo");
    }

    /**
     * 查询我的权限变更申请列表
     */
    @GetMapping("/my-apply")
    public List<Map<String, Object>> getMyApplyList(HttpServletRequest request) {
        Long userId = Long.valueOf(request.getHeader("X-User-Id"));
        Result<List<Map<String, Object>>> response = approvalFeignClient.getMyRequests(userId);
        return response.getData();
    }
}
