package com.zhitao.securitycomplianceapprovalcenter.approval.controller;

import com.zhitao.securitycomplianceapprovalcenter.approval.entity.ApprovalProcess;
import com.zhitao.securitycomplianceapprovalcenter.approval.entity.ApprovalRequest;
import com.zhitao.securitycomplianceapprovalcenter.approval.service.ApprovalService;
import com.zhitao.securitycomplianceapprovalcenter.common.enums.RiskLevel;
import com.zhitao.securitycomplianceapprovalcenter.common.result.Result;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审批控制器
 */
@RestController
@RequestMapping("/api/approval")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    /**
     * 创建审批申请（Feign 接口 - 接收 Map 返回 Result<Map>）
     */
    @PostMapping("/request")
    public Result<Map<String, Object>> createApprovalRequest(@RequestBody Map<String, Object> requestDTO) {
        ApprovalRequest approvalRequest = approvalService.createRequest(
                Long.valueOf(requestDTO.get("applicantId").toString()),
                requestDTO.get("applicantName").toString(),
                requestDTO.get("applicantDepartment").toString(),
                requestDTO.get("operationType").toString(),
                requestDTO.get("operationContent").toString(),
                requestDTO.get("operationReason").toString(),
                RiskLevel.valueOf(requestDTO.get("riskLevel").toString())
        );

        Map<String, Object> response = new HashMap<>();
        response.put("requestNo", approvalRequest.getRequestNo());
        response.put("status", approvalRequest.getStatus().name());
        return Result.success("审批申请创建成功", response);
    }

    /**
     * 获取我的申请列表（Feign 接口 - 返回 Result<List<Map>>）
     */
    @GetMapping("/my-requests")
    public Result<List<Map<String, Object>>> getMyRequests(@RequestParam Long applicantId) {
        List<ApprovalRequest> list = approvalService.getMyRequests(applicantId);
        List<Map<String, Object>> response = list.stream().map(request -> {
            Map<String, Object> map = new HashMap<>();
            map.put("requestNo", request.getRequestNo());
            map.put("status", request.getStatus().name());
            map.put("operationType", request.getOperationType());
            map.put("operationContent", request.getOperationContent());
            map.put("createTime", request.getCreateTime());
            return map;
        }).collect(Collectors.toList());
        return Result.success(response);
    }

    /**
     * 获取待我审批列表（Feign 接口 - 返回 Result<List<Map>>）
     */
    @GetMapping("/pending")
    public Result<List<Map<String, Object>>> getPendingApprovals(
            @RequestParam Long approverId,
            @RequestParam String approverRole) {
        List<ApprovalRequest> list = approvalService.getPendingApprovals(approverId, approverRole);
        List<Map<String, Object>> response = list.stream().map(request -> {
            Map<String, Object> map = new HashMap<>();
            map.put("requestNo", request.getRequestNo());
            map.put("status", request.getStatus().name());
            map.put("operationType", request.getOperationType());
            map.put("operationContent", request.getOperationContent());
            map.put("applicantName", request.getApplicantName());
            map.put("createTime", request.getCreateTime());
            return map;
        }).collect(Collectors.toList());
        return Result.success(response);
    }

    /**
     * 获取申请详情
     */
    @GetMapping("/request/{id}")
    public Result<ApprovalRequest> getRequestDetail(@PathVariable Long id) {
        ApprovalRequest request = approvalService.getRequestDetail(id);
        return Result.success(request);
    }

    // ------------------------------ DTO类 ------------------------------
    @Data
    public static class ApprovalRequestDTO {
        private Long applicantId;
        private String applicantName;
        private String applicantDepartment;
        private String operationType;
        private String operationContent;
        private String operationReason;
        private RiskLevel riskLevel;
    }

    @Data
    public static class ApprovalDTO {
        private Long approverId;
        private String approverRole;
        private boolean approved;
        private String comment;
    }
}
