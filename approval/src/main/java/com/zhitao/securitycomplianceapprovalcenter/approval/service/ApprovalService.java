package com.zhitao.securitycomplianceapprovalcenter.approval.service;

import com.zhitao.securitycomplianceapprovalcenter.approval.entity.ApprovalNode;
import com.zhitao.securitycomplianceapprovalcenter.approval.entity.ApprovalProcess;
import com.zhitao.securitycomplianceapprovalcenter.approval.entity.ApprovalRequest;
import com.zhitao.securitycomplianceapprovalcenter.approval.entity.RequestApprovalNode;
import com.zhitao.securitycomplianceapprovalcenter.approval.repository.ApprovalProcessRepository;
import com.zhitao.securitycomplianceapprovalcenter.approval.repository.ApprovalRequestRepository;
import com.zhitao.securitycomplianceapprovalcenter.approval.repository.RequestApprovalNodeRepository;
import com.zhitao.securitycomplianceapprovalcenter.common.enums.AuditLevelEnum;
import com.zhitao.securitycomplianceapprovalcenter.common.feign.AuditFeignClient;
import com.zhitao.securitycomplianceapprovalcenter.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 审批核心服务
 * 新增：全流程自动写入审计日志
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalProcessRepository processRepository;
    private final ApprovalRequestRepository requestRepository;
    private final RequestApprovalNodeRepository nodeRepository;
    // 注入审计服务Feign客户端，自动写入审计日志
    private final AuditFeignClient auditFeignClient;

    /**
     * 创建审批申请
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalRequest createRequest(Long applicantId, String applicantName, String applicantDepartment,
                                         String operationType, String operationContent, String operationReason,
                                         ApprovalProcess.RiskLevel riskLevel) {
        // 1. 根据操作类型和风险等级查找审批流程
        ApprovalProcess process = processRepository.findByOperationTypeAndRiskLevel(operationType, riskLevel);
        if (process == null || !process.getEnabled()) {
            throw new RuntimeException("未找到对应的启用审批流程");
        }

        // 2. 创建审批申请
        ApprovalRequest request = new ApprovalRequest();
        request.setRequestNo(generateRequestNo());
        request.setApprovalProcess(process);
        request.setApplicantId(applicantId);
        request.setApplicantName(applicantName);
        request.setApplicantDepartment(applicantDepartment);
        request.setOperationType(operationType);
        request.setOperationContent(operationContent);
        request.setOperationReason(operationReason);
        request.setRiskLevel(riskLevel);
        request.setApprovalLevel(process.getApprovalLevel());
        request.setCurrentNodeIndex(0);
        request.setStatus(ApprovalRequest.RequestStatus.PENDING);
        request.setCreateTime(LocalDateTime.now());
        request.setUpdateTime(LocalDateTime.now());
        request = requestRepository.save(request);

        // 3. 创建审批节点
        createApprovalNodes(request, process);

        // 4. 写入审计日志（新增联动）
        recordAuditLog(
                applicantId,
                applicantName,
                "CREATE_APPROVAL_REQUEST",
                "APPROVAL_REQUEST",
                String.valueOf(request.getId()),
                request.getRequestNo(),
                "{}",
                "{\"requestNo\":\"" + request.getRequestNo() + "\",\"operationType\":\"" + operationType + "\"}",
                operationReason,
                getAuditLevelByRisk(riskLevel),
                "SUCCESS",
                null
        );

        log.info("创建审批申请成功，申请编号：{}", request.getRequestNo());
        return request;
    }

    /**
     * 提交审批
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalRequest submitRequest(Long requestId) {
        ApprovalRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("审批申请不存在"));

        // 状态机校验：只有待提交状态可以提交
        if (request.getStatus() != ApprovalRequest.RequestStatus.PENDING) {
            throw new RuntimeException("当前状态不允许提交审批");
        }

        // 更新状态
        request.setStatus(ApprovalRequest.RequestStatus.SUBMITTED);
        request.setUpdateTime(LocalDateTime.now());
        request = requestRepository.save(request);

        // 写入审计日志
        recordAuditLog(
                request.getApplicantId(),
                request.getApplicantName(),
                "SUBMIT_APPROVAL_REQUEST",
                "APPROVAL_REQUEST",
                String.valueOf(request.getId()),
                request.getRequestNo(),
                "{\"status\":\"PENDING\"}",
                "{\"status\":\"SUBMITTED\"}",
                "提交审批申请",
                getAuditLevelByRisk(request.getRiskLevel()),
                "SUCCESS",
                null
        );

        log.info("提交审批申请成功，申请编号：{}", request.getRequestNo());
        return request;
    }

    /**
     * 审批操作
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalRequest approve(Long requestId, Long approverId, String approverRole,
                                   boolean approved, String comment) {
        ApprovalRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("审批申请不存在"));

        // 状态机校验：只有审批中/已提交状态可以审批
        if (request.getStatus() != ApprovalRequest.RequestStatus.SUBMITTED
                && request.getStatus() != ApprovalRequest.RequestStatus.APPROVING) {
            throw new RuntimeException("当前状态不允许审批操作");
        }

        // 获取当前审批节点
        RequestApprovalNode currentNode = getCurrentApprovalNode(request);

        // 验证审批人权限
        if (!canApprove(currentNode, approverId, approverRole)) {
            throw new RuntimeException("您没有权限审批此申请");
        }

        // 更新节点状态
        currentNode.setStatus(approved ? RequestApprovalNode.NodeStatus.APPROVED : RequestApprovalNode.NodeStatus.REJECTED);
        currentNode.setApprovalComment(comment);
        currentNode.setApprovalTime(LocalDateTime.now());
        currentNode.setApproverId(approverId);
        nodeRepository.save(currentNode);

        String beforeStatus = request.getStatus().name();
        String operationType;
        String auditLevel = getAuditLevelByRisk(request.getRiskLevel());

        if (approved) {
            // 审批通过，进入下一节点或完成审批
            request.setCurrentNodeIndex(request.getCurrentNodeIndex() + 1);
            if (request.getCurrentNodeIndex() >= request.getApprovalNodes().size()) {
                // 所有节点审批通过
                request.setStatus(ApprovalRequest.RequestStatus.APPROVED);
                operationType = "APPROVAL_FINISH_ALL_PASS";
            } else {
                // 进入下一审批节点
                request.setStatus(ApprovalRequest.RequestStatus.APPROVING);
                operationType = "APPROVAL_NODE_PASS";
            }
        } else {
            // 审批拒绝
            request.setStatus(ApprovalRequest.RequestStatus.REJECTED);
            operationType = "APPROVAL_REJECT";
        }

        request.setUpdateTime(LocalDateTime.now());
        request = requestRepository.save(request);

        // 写入审计日志
        recordAuditLog(
                approverId,
                approverRole,
                operationType,
                "APPROVAL_REQUEST",
                String.valueOf(request.getId()),
                request.getRequestNo(),
                "{\"status\":\"" + beforeStatus + "\"}",
                "{\"status\":\"" + request.getStatus().name() + "\"}",
                comment,
                auditLevel,
                "SUCCESS",
                null
        );

        log.info("审批操作完成，申请编号：{}，审批结果：{}", request.getRequestNo(), approved ? "通过" : "拒绝");
        return request;
    }

    /**
     * 执行审批通过的操作
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalRequest executeRequest(Long requestId) {
        ApprovalRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("审批申请不存在"));

        // 状态机校验：只有已通过状态可以执行
        if (request.getStatus() != ApprovalRequest.RequestStatus.APPROVED) {
            throw new RuntimeException("当前状态不允许执行操作");
        }

        // 更新状态为已执行
        request.setStatus(ApprovalRequest.RequestStatus.EXECUTED);
        request.setUpdateTime(LocalDateTime.now());
        request = requestRepository.save(request);

        // 写入审计日志
        recordAuditLog(
                request.getApplicantId(),
                request.getApplicantName(),
                "APPROVAL_EXECUTE",
                "APPROVAL_REQUEST",
                String.valueOf(request.getId()),
                request.getRequestNo(),
                "{\"status\":\"APPROVED\"}",
                "{\"status\":\"EXECUTED\"}",
                "执行审批通过的操作",
                getAuditLevelByRisk(request.getRiskLevel()),
                "SUCCESS",
                null
        );

        log.info("审批操作执行完成，申请编号：{}", request.getRequestNo());
        return request;
    }

    /**
     * 撤销审批申请
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalRequest cancelRequest(Long requestId, Long applicantId) {
        ApprovalRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("审批申请不存在"));

        // 权限校验：只有申请人可以撤销
        if (!request.getApplicantId().equals(applicantId)) {
            throw new RuntimeException("只有申请人可以撤销申请");
        }

        // 状态机校验：已执行的申请无法撤销
        if (request.getStatus() == ApprovalRequest.RequestStatus.EXECUTED) {
            throw new RuntimeException("已执行的申请无法撤销");
        }

        // 更新状态为已撤销
        String beforeStatus = request.getStatus().name();
        request.setStatus(ApprovalRequest.RequestStatus.CANCELLED);
        request.setUpdateTime(LocalDateTime.now());
        request = requestRepository.save(request);

        // 写入审计日志
        recordAuditLog(
                applicantId,
                request.getApplicantName(),
                "APPROVAL_CANCEL",
                "APPROVAL_REQUEST",
                String.valueOf(request.getId()),
                request.getRequestNo(),
                "{\"status\":\"" + beforeStatus + "\"}",
                "{\"status\":\"CANCELLED\"}",
                "申请人撤销审批申请",
                getAuditLevelByRisk(request.getRiskLevel()),
                "SUCCESS",
                null
        );

        log.info("审批申请撤销成功，申请编号：{}", request.getRequestNo());
        return request;
    }

    /**
     * 获取待我审批的申请列表
     */
    public List<ApprovalRequest> getPendingApprovals(Long approverId, String approverRole) {
        return requestRepository.findPendingApprovals(approverId, approverRole);
    }

    /**
     * 获取我的申请列表
     */
    public List<ApprovalRequest> getMyRequests(Long applicantId) {
        return requestRepository.findByApplicantId(applicantId);
    }

    /**
     * 获取申请详情
     */
    public ApprovalRequest getRequestDetail(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("审批申请不存在"));
    }

    /**
     * 创建审批节点
     */
    private void createApprovalNodes(ApprovalRequest request, ApprovalProcess process) {
        for (ApprovalNode configNode : process.getApprovalNodes()) {
            RequestApprovalNode node = new RequestApprovalNode();
            node.setApprovalRequest(request);
            node.setNodeName(configNode.getNodeName());
            node.setNodeOrder(configNode.getNodeOrder());
            node.setApproverId(configNode.getApproverId());
            node.setApproverRole(configNode.getApproverRole());
            node.setStatus(RequestApprovalNode.NodeStatus.PENDING);
            node.setCreateTime(LocalDateTime.now());
            nodeRepository.save(node);
        }
    }

    /**
     * 获取当前审批节点
     */
    private RequestApprovalNode getCurrentApprovalNode(ApprovalRequest request) {
        return request.getApprovalNodes().stream()
                .filter(n -> n.getNodeOrder().equals(request.getCurrentNodeIndex()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("未找到当前审批节点"));
    }

    /**
     * 校验审批人权限
     */
    private boolean canApprove(RequestApprovalNode node, Long approverId, String approverRole) {
        if (node.getApproverId() != null && node.getApproverId().equals(approverId)) {
            return true;
        }
        if (node.getApproverRole() != null && node.getApproverRole().equals(approverRole)) {
            return true;
        }
        return false;
    }

    /**
     * 生成申请编号
     */
    private String generateRequestNo() {
        return "APR" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * 风险等级映射审计级别
     */
    private String getAuditLevelByRisk(ApprovalProcess.RiskLevel riskLevel) {
        return switch (riskLevel) {
            case CRITICAL -> AuditLevelEnum.CRITICAL.getCode();
            case HIGH -> AuditLevelEnum.HIGH.getCode();
            case MEDIUM -> AuditLevelEnum.MEDIUM.getCode();
            case LOW -> AuditLevelEnum.LOW.getCode();
        };
    }

    /**
     * 写入审计日志（核心联动，失败不阻塞主流程）
     */
    private void recordAuditLog(Long operatorId, String operatorName, String operationType,
                                String permissionObjectType, String permissionObjectId, String permissionObjectName,
                                String beforeChange, String afterChange, String operationReason,
                                String auditLevel, String operationResult, String errorMessage) {
        try {
            Result<Void> result = auditFeignClient.recordPermissionChange(
                    operatorId,
                    operatorName,
                    operationType,
                    permissionObjectType,
                    permissionObjectId,
                    permissionObjectName,
                    beforeChange,
                    afterChange,
                    operationReason,
                    auditLevel,
                    operationResult,
                    errorMessage
            );
            if (!result.getCode().equals(200)) {
                log.warn("审计日志写入失败，原因：{}", result.getMessage());
            }
        } catch (Exception e) {
            // 审计服务不可用时，打印日志，不抛出异常，保证主流程稳定
            log.error("审计服务调用异常，日志写入失败", e);
        }
    }
}
