package com.zhitao.securitycomplianceapprovalcenter.approval.service;

import com.zhitao.securitycomplianceapprovalcenter.approval.entity.ApprovalNode;
import com.zhitao.securitycomplianceapprovalcenter.approval.entity.ApprovalProcess;
import com.zhitao.securitycomplianceapprovalcenter.approval.entity.ApprovalRequest;
import com.zhitao.securitycomplianceapprovalcenter.approval.entity.RequestApprovalNode;
import com.zhitao.securitycomplianceapprovalcenter.approval.repository.ApprovalProcessRepository;
import com.zhitao.securitycomplianceapprovalcenter.approval.repository.ApprovalRequestRepository;
import com.zhitao.securitycomplianceapprovalcenter.approval.repository.RequestApprovalNodeRepository;
import com.zhitao.securitycomplianceapprovalcenter.common.enums.RiskLevel;
import com.zhitao.securitycomplianceapprovalcenter.common.feign.AuditFeignClient;
import com.zhitao.securitycomplianceapprovalcenter.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ApprovalService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    @InjectMocks
    private ApprovalService approvalService;

    @Mock
    private ApprovalProcessRepository processRepository;

    @Mock
    private ApprovalRequestRepository requestRepository;

    @Mock
    private RequestApprovalNodeRepository nodeRepository;

    @Mock
    private AuditFeignClient auditFeignClient;

    private ApprovalProcess testProcess;
    private ApprovalRequest testRequest;
    private RequestApprovalNode testNode;

    @BeforeEach
    void setUp() {
        // 准备测试数据 - 审批流程
        testProcess = new ApprovalProcess();
        testProcess.setId(1L);
        testProcess.setProcessName("权限变更审批流程");
        testProcess.setOperationType("MODIFY_PERMISSION");
        testProcess.setRiskLevel(RiskLevel.HIGH);
        testProcess.setApprovalLevel(2);
        testProcess.setEnabled(true);

        List<ApprovalNode> nodes = new ArrayList<>();
        ApprovalNode node1 = new ApprovalNode();
        node1.setId(1L);
        node1.setNodeName("直接上级审批");
        node1.setNodeOrder(0);
        node1.setApproverRole("MANAGER");
        nodes.add(node1);

        ApprovalNode node2 = new ApprovalNode();
        node2.setId(2L);
        node2.setNodeName("安全管理员审批");
        node2.setNodeOrder(1);
        node2.setApproverRole("SECURITY_ADMIN");
        nodes.add(node2);

        testProcess.setApprovalNodes(nodes);

        // 准备测试数据 - 审批申请
        testRequest = new ApprovalRequest();
        testRequest.setId(1L);
        testRequest.setRequestNo("APR1234567890");
        testRequest.setApprovalProcess(testProcess);
        testRequest.setApplicantId(100L);
        testRequest.setApplicantName("张三");
        testRequest.setOperationType("MODIFY_PERMISSION");
        testRequest.setRiskLevel(RiskLevel.HIGH);
        testRequest.setStatus(ApprovalRequest.RequestStatus.PENDING);
        testRequest.setCurrentNodeIndex(0);

        List<RequestApprovalNode> requestNodes = new ArrayList<>();
        RequestApprovalNode rn1 = new RequestApprovalNode();
        rn1.setId(1L);
        rn1.setNodeOrder(0);
        rn1.setApproverRole("MANAGER");
        rn1.setStatus(RequestApprovalNode.NodeStatus.PENDING);
        requestNodes.add(rn1);

        RequestApprovalNode rn2 = new RequestApprovalNode();
        rn2.setId(2L);
        rn2.setNodeOrder(1);
        rn2.setApproverRole("SECURITY_ADMIN");
        rn2.setStatus(RequestApprovalNode.NodeStatus.PENDING);
        requestNodes.add(rn2);

        testRequest.setApprovalNodes(requestNodes);

        // 准备测试数据 - 节点
        testNode = new RequestApprovalNode();
        testNode.setId(1L);
        testNode.setNodeOrder(0);
        testNode.setStatus(RequestApprovalNode.NodeStatus.PENDING);
    }

    // ===== 辅助方法：mock 审计服务 =====
    private void mockAuditSuccess() {
        when(auditFeignClient.recordPermissionChange(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Result.success(null));
    }

    @Test
    @DisplayName("创建审批申请成功")
    void createRequest_Success() {
        // Given
        mockAuditSuccess();
        when(processRepository.findByOperationTypeAndRiskLevel("MODIFY_PERMISSION", RiskLevel.HIGH))
                .thenReturn(testProcess);
        when(requestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> {
            ApprovalRequest saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(nodeRepository.save(any(RequestApprovalNode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ApprovalRequest result = approvalService.createRequest(
                100L, "张三", "技术部",
                "MODIFY_PERMISSION", "修改权限", "工作需要",
                RiskLevel.HIGH
        );

        // Then
        assertNotNull(result);
        assertTrue(result.getRequestNo().startsWith("APR"), "申请编号应以 APR 开头");
        assertEquals(ApprovalRequest.RequestStatus.PENDING, result.getStatus());
        verify(processRepository).findByOperationTypeAndRiskLevel("MODIFY_PERMISSION", RiskLevel.HIGH);
        verify(requestRepository).save(any(ApprovalRequest.class));
        verify(nodeRepository, times(2)).save(any(RequestApprovalNode.class));
    }

    @Test
    @DisplayName("创建审批申请 - 未找到审批流程")
    void createRequest_ProcessNotFound_ThrowsException() {
        // Given
        when(processRepository.findByOperationTypeAndRiskLevel("MODIFY_PERMISSION", RiskLevel.HIGH))
                .thenReturn(null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            approvalService.createRequest(100L, "张三", "技术部", "MODIFY_PERMISSION", "内容", "原因", RiskLevel.HIGH);
        });
        assertEquals("未找到对应的启用审批流程", exception.getMessage());
    }

    @Test
    @DisplayName("创建审批申请 - 流程未启用")
    void createRequest_ProcessNotEnabled_ThrowsException() {
        // Given
        testProcess.setEnabled(false);
        when(processRepository.findByOperationTypeAndRiskLevel("MODIFY_PERMISSION", RiskLevel.HIGH))
                .thenReturn(testProcess);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            approvalService.createRequest(100L, "张三", "技术部", "MODIFY_PERMISSION", "内容", "原因", RiskLevel.HIGH);
        });
        assertEquals("未找到对应的启用审批流程", exception.getMessage());
    }

    @Test
    @DisplayName("提交审批申请成功")
    void submitRequest_Success() {
        // Given
        mockAuditSuccess();
        when(requestRepository.findById(1L)).thenReturn(Optional.of(testRequest));
        when(requestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ApprovalRequest result = approvalService.submitRequest(1L);

        // Then
        assertEquals(ApprovalRequest.RequestStatus.SUBMITTED, result.getStatus());
        verify(requestRepository).save(testRequest);
    }

    @Test
    @DisplayName("提交审批申请 - 申请不存在")
    void submitRequest_NotFound_ThrowsException() {
        // Given
        when(requestRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            approvalService.submitRequest(1L);
        });
        assertEquals("审批申请不存在", exception.getMessage());
    }

    @Test
    @DisplayName("提交审批申请 - 状态不允许")
    void submitRequest_InvalidStatus_ThrowsException() {
        // Given
        testRequest.setStatus(ApprovalRequest.RequestStatus.SUBMITTED); // 已经是已提交状态
        when(requestRepository.findById(1L)).thenReturn(Optional.of(testRequest));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            approvalService.submitRequest(1L);
        });
        assertEquals("当前状态不允许提交审批", exception.getMessage());
    }

    @Test
    @DisplayName("审批通过 - 进入下一节点")
    void approve_NodePass_GoToNextNode() {
        // Given
        mockAuditSuccess();
        testRequest.setStatus(ApprovalRequest.RequestStatus.SUBMITTED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(testRequest));
        when(requestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(nodeRepository.save(any(RequestApprovalNode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ApprovalRequest result = approvalService.approve(1L, 200L, "MANAGER", true, "同意");

        // Then
        assertEquals(ApprovalRequest.RequestStatus.APPROVING, result.getStatus());
        assertEquals(1, result.getCurrentNodeIndex()); // 进入下一节点
        verify(nodeRepository).save(any(RequestApprovalNode.class));
    }

    @Test
    @DisplayName("审批通过 - 所有节点通过")
    void approve_AllNodesPassed_Finish() {
        // Given
        mockAuditSuccess();
        testRequest.setStatus(ApprovalRequest.RequestStatus.SUBMITTED); // 设置为已提交状态
        testRequest.setCurrentNodeIndex(1); // 最后一个节点
        when(requestRepository.findById(1L)).thenReturn(Optional.of(testRequest));
        when(requestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(nodeRepository.save(any(RequestApprovalNode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ApprovalRequest result = approvalService.approve(1L, 300L, "SECURITY_ADMIN", true, "同意");

        // Then
        assertEquals(ApprovalRequest.RequestStatus.APPROVED, result.getStatus());
        verify(nodeRepository).save(any(RequestApprovalNode.class));
    }

    @Test
    @DisplayName("审批拒绝 - 申请已提交后拒绝")
    void approve_Reject() {
        // Given
        mockAuditSuccess();
        testRequest.setStatus(ApprovalRequest.RequestStatus.SUBMITTED); // 设置为已提交状态，可审批
        when(requestRepository.findById(1L)).thenReturn(Optional.of(testRequest));
        when(requestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(nodeRepository.save(any(RequestApprovalNode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ApprovalRequest result = approvalService.approve(1L, 200L, "MANAGER", false, "不同意");

        // Then
        assertEquals(ApprovalRequest.RequestStatus.REJECTED, result.getStatus());
        verify(nodeRepository).save(any(RequestApprovalNode.class));
    }

    @Test
    @DisplayName("审批 - 申请不存在")
    void approve_NotFound_ThrowsException() {
        // Given
        when(requestRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            approvalService.approve(1L, 200L, "MANAGER", true, "同意");
        });
        assertEquals("审批申请不存在", exception.getMessage());
    }

    @Test
    @DisplayName("审批 - 状态不允许")
    void approve_InvalidStatus_ThrowsException() {
        // Given
        testRequest.setStatus(ApprovalRequest.RequestStatus.APPROVED); // 已通过状态
        when(requestRepository.findById(1L)).thenReturn(Optional.of(testRequest));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            approvalService.approve(1L, 200L, "MANAGER", true, "同意");
        });
        assertEquals("当前状态不允许审批操作", exception.getMessage());
    }

    @Test
    @DisplayName("审批 - 审批人无权限")
    void approve_NoPermission_ThrowsException() {
        // Given
        testRequest.setStatus(ApprovalRequest.RequestStatus.SUBMITTED); // 设置为已提交状态
        when(requestRepository.findById(1L)).thenReturn(Optional.of(testRequest));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            approvalService.approve(1L, 999L, "WRONG_ROLE", true, "同意");
        });
        assertEquals("您没有权限审批此申请", exception.getMessage());
    }

    @Test
    @DisplayName("执行审批通过的操作成功")
    void executeRequest_Success() {
        // Given
        mockAuditSuccess();
        testRequest.setStatus(ApprovalRequest.RequestStatus.APPROVED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(testRequest));
        when(requestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ApprovalRequest result = approvalService.executeRequest(1L);

        // Then
        assertEquals(ApprovalRequest.RequestStatus.EXECUTED, result.getStatus());
    }

    @Test
    @DisplayName("执行审批 - 状态不允许")
    void executeRequest_InvalidStatus_ThrowsException() {
        // Given
        testRequest.setStatus(ApprovalRequest.RequestStatus.SUBMITTED); // 未通过状态
        when(requestRepository.findById(1L)).thenReturn(Optional.of(testRequest));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            approvalService.executeRequest(1L);
        });
        assertEquals("当前状态不允许执行操作", exception.getMessage());
    }

    @Test
    @DisplayName("撤销审批申请成功")
    void cancelRequest_Success() {
        // Given
        mockAuditSuccess();
        when(requestRepository.findById(1L)).thenReturn(Optional.of(testRequest));
        when(requestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ApprovalRequest result = approvalService.cancelRequest(1L, 100L);

        // Then
        assertEquals(ApprovalRequest.RequestStatus.CANCELLED, result.getStatus());
    }

    @Test
    @DisplayName("撤销审批 - 非申请人撤销")
    void cancelRequest_NotApplicant_ThrowsException() {
        // Given
        when(requestRepository.findById(1L)).thenReturn(Optional.of(testRequest));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            approvalService.cancelRequest(1L, 999L); // 不是申请人 ID
        });
        assertEquals("只有申请人可以撤销申请", exception.getMessage());
    }

    @Test
    @DisplayName("撤销审批 - 已执行无法撤销")
    void cancelRequest_AlreadyExecuted_ThrowsException() {
        // Given
        testRequest.setStatus(ApprovalRequest.RequestStatus.EXECUTED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(testRequest));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            approvalService.cancelRequest(1L, 100L);
        });
        assertEquals("已执行的申请无法撤销", exception.getMessage());
    }

    @Test
    @DisplayName("获取我的申请列表")
    void getMyRequests_Success() {
        // Given
        List<ApprovalRequest> mockList = List.of(testRequest);
        when(requestRepository.findByApplicantId(100L)).thenReturn(mockList);

        // When
        List<ApprovalRequest> result = approvalService.getMyRequests(100L);

        // Then
        assertEquals(1, result.size());
        verify(requestRepository).findByApplicantId(100L);
    }

    @Test
    @DisplayName("获取申请详情")
    void getRequestDetail_Success() {
        // Given
        when(requestRepository.findById(1L)).thenReturn(Optional.of(testRequest));

        // When
        ApprovalRequest result = approvalService.getRequestDetail(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("获取申请详情 - 不存在")
    void getRequestDetail_NotFound_ThrowsException() {
        // Given
        when(requestRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            approvalService.getRequestDetail(1L);
        });
        assertEquals("审批申请不存在", exception.getMessage());
    }
}
