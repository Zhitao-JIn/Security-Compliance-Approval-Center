package com.zhitao.securitycomplianceapprovalcenter.approval.controller;

import com.zhitao.securitycomplianceapprovalcenter.approval.entity.ApprovalProcess;
import com.zhitao.securitycomplianceapprovalcenter.approval.entity.ApprovalRequest;
import com.zhitao.securitycomplianceapprovalcenter.approval.service.ApprovalService;
import com.zhitao.securitycomplianceapprovalcenter.common.result.Result;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审批控制器
 */
@RestController
@RequestMapping("/api/approval")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    /**
     * 创建审批申请
     */
    @PostMapping("/request")
    public Result<ApprovalRequest> createRequest(@RequestBody ApprovalRequestDTO request) {
        ApprovalRequest approvalRequest = approvalService.createRequest(
                request.getApplicantId(),
                request.getApplicantName(),
                request.getApplicantDepartment(),
                request.getOperationType(),
                request.getOperationContent(),
                request.getOperationReason(),
                request.getRiskLevel()
        );
        return Result.success("审批申请创建成功", approvalRequest);
    }

    /**
     * 提交审批
     */
    @PostMapping("/request/{id}/submit")
    public Result<ApprovalRequest> submitRequest(@PathVariable Long id) {
        ApprovalRequest request = approvalService.submitRequest(id);
        return Result.success("审批提交成功", request);
    }

    /**
     * 审批操作
     */
    @PostMapping("/request/{id}/approve")
    public Result<ApprovalRequest> approve(@PathVariable Long id, @RequestBody ApprovalDTO approval) {
        ApprovalRequest request = approvalService.approve(
                id,
                approval.getApproverId(),
                approval.getApproverRole(),
                approval.isApproved(),
                approval.getComment()
        );
        return Result.success(approval.isApproved() ? "审批通过" : "审批拒绝", request);
    }

    /**
     * 执行审批通过的操作
     */
    @PostMapping("/request/{id}/execute")
    public Result<ApprovalRequest> executeRequest(@PathVariable Long id) {
        ApprovalRequest request = approvalService.executeRequest(id);
        return Result.success("操作执行成功", request);
    }

    /**
     * 撤销审批申请
     */
    @PostMapping("/request/{id}/cancel")
    public Result<ApprovalRequest> cancelRequest(@PathVariable Long id, @RequestParam Long applicantId) {
        ApprovalRequest request = approvalService.cancelRequest(id, applicantId);
        return Result.success("申请撤销成功", request);
    }

    /**
     * 获取待我审批的申请列表
     */
    @GetMapping("/pending")
    public Result<List<ApprovalRequest>> getPendingApprovals(
            @RequestParam Long approverId,
            @RequestParam String approverRole) {
        List<ApprovalRequest> list = approvalService.getPendingApprovals(approverId, approverRole);
        return Result.success(list);
    }

    /**
     * 获取我的申请列表
     */
    @GetMapping("/my-requests")
    public Result<List<ApprovalRequest>> getMyRequests(@RequestParam Long applicantId) {
        List<ApprovalRequest> list = approvalService.getMyRequests(applicantId);
        return Result.success(list);
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
        private ApprovalProcess.RiskLevel riskLevel;
    }

    @Data
    public static class ApprovalDTO {
        private Long approverId;
        private String approverRole;
        private boolean approved;
        private String comment;
    }
}
