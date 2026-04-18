package com.zhitao.securitycomplianceapprovalcenter.approval.service;

import com.zhitao.securitycomplianceapprovalcenter.approval.entity.ApprovalProcess;
import com.zhitao.securitycomplianceapprovalcenter.approval.entity.ApprovalRequest;
import com.zhitao.securitycomplianceapprovalcenter.approval.entity.RequestApprovalNode;
import com.zhitao.securitycomplianceapprovalcenter.approval.repository.ApprovalProcessRepository;
import com.zhitao.securitycomplianceapprovalcenter.approval.repository.ApprovalRequestRepository;
import com.zhitao.securitycomplianceapprovalcenter.common.feign.AuditFeignClient;
import com.zhitao.securitycomplianceapprovalcenter.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Rollback;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 审批服务完整单元测试
 * @MockBean：Mock掉审计服务Feign调用，避免依赖外部服务
 */
@SpringBootTest
public class ApprovalServiceTest {

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private ApprovalRequestRepository requestRepository;

    @Autowired
    private ApprovalProcessRepository processRepository;

    // Mock掉审计服务Feign客户端，避免依赖外部服务
    @MockBean
    private AuditFeignClient auditFeignClient;

    @BeforeEach
    public void setUp() {
        // Mock审计服务调用，永远返回成功
        when(auditFeignClient.recordPermissionChange(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Result.success(null));
    }

    // ------------------------------ 核心流程测试 ------------------------------

    /**
     * 测试：完整审批流程（创建→提交→一级审批→二级审批→执行）
     */
    @Test

    public void testCompleteApprovalProcess_权限变更二级审批() {
        // ------------------------------ 步骤1：创建审批申请 ------------------------------
        ApprovalRequest request = approvalService.createRequest(
                1L,
                "张三",
                "技术部",
                "MODIFY_PERMISSION",
                "给用户李四分配系统管理员角色",
                "新员工入职",
                ApprovalProcess.RiskLevel.HIGH
        );
        request = requestRepository.findById(request.getId()).orElseThrow(()->new RuntimeException("request 不存在"));

        // 验证创建结果
        assertNotNull(request.getId());
        assertNotNull(request.getRequestNo());
        assertTrue(request.getRequestNo().startsWith("APR"));
        assertEquals(ApprovalRequest.RequestStatus.PENDING, request.getStatus());
        assertEquals(2, request.getApprovalLevel()); // 二级审批
        assertEquals(2, request.getApprovalNodes().size()); // 2个审批节点

        // ------------------------------ 步骤2：提交审批 ------------------------------
        request = approvalService.submitRequest(request.getId());
        assertEquals(ApprovalRequest.RequestStatus.SUBMITTED, request.getStatus());

        // ------------------------------ 步骤3：一级审批通过（MANAGER角色） ------------------------------
        request = approvalService.approve(
                request.getId(),
                2L,
                "MANAGER",
                true,
                "同意申请，权限调整合理"
        );

        // 验证一级审批结果
        assertEquals(ApprovalRequest.RequestStatus.APPROVING, request.getStatus());
        assertEquals(1, request.getCurrentNodeIndex()); // 进入第二个节点
        RequestApprovalNode firstNode = request.getApprovalNodes().get(0);
        assertEquals(RequestApprovalNode.NodeStatus.APPROVED, firstNode.getStatus());
        assertEquals("MANAGER", firstNode.getApproverRole());
        assertNotNull(firstNode.getApprovalTime());

        // ------------------------------ 步骤4：二级审批通过（SECURITY_ADMIN角色） ------------------------------
        request = approvalService.approve(
                request.getId(),
                3L,
                "SECURITY_ADMIN",
                true,
                "安全合规，同意执行"
        );

        // 验证二级审批结果
        assertEquals(ApprovalRequest.RequestStatus.APPROVED, request.getStatus());
        assertEquals(2, request.getCurrentNodeIndex()); // 所有节点完成
        RequestApprovalNode secondNode = request.getApprovalNodes().get(1);
        assertEquals(RequestApprovalNode.NodeStatus.APPROVED, secondNode.getStatus());

        // ------------------------------ 步骤5：执行审批通过的操作 ------------------------------
        request = approvalService.executeRequest(request.getId());
        assertEquals(ApprovalRequest.RequestStatus.EXECUTED, request.getStatus());

        // 验证审计服务被调用了5次（创建/提交/一级审批/二级审批/执行）
        verify(auditFeignClient, times(5)).recordPermissionChange(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    /**
     * 测试：审批拒绝流程
     */
    @Test

    public void testApprovalReject_流程终止() {
        // 1. 创建并提交审批
        ApprovalRequest request = approvalService.createRequest(
                1L,
                "张三",
                "技术部",
                "MODIFY_PERMISSION",
                "越权修改权限",
                "测试拒绝",
                ApprovalProcess.RiskLevel.HIGH
        );
        request = approvalService.submitRequest(request.getId());

        // 2. 一级审批拒绝
        request = approvalService.approve(
                request.getId(),
                2L,
                "MANAGER",
                false,
                "权限调整不合理，拒绝申请"
        );

        // 验证结果：流程终止，状态为REJECTED
        assertEquals(ApprovalRequest.RequestStatus.REJECTED, request.getStatus());
        RequestApprovalNode node = request.getApprovalNodes().get(0);
        assertEquals(RequestApprovalNode.NodeStatus.REJECTED, node.getStatus());
        assertEquals("权限调整不合理，拒绝申请", node.getApprovalComment());
    }

    // ------------------------------ 状态机校验测试 ------------------------------

    /**
     * 测试：状态机校验 - 非待提交状态不能提交
     */
    @Test

    public void testStateMachineValidation_非待提交状态不能提交() {
        // 1. 创建并提交审批
        ApprovalRequest request = approvalService.createRequest(
                1L,
                "张三",
                "技术部",
                "MODIFY_PERMISSION",
                "测试",
                "测试",
                ApprovalProcess.RiskLevel.HIGH
        );
        request = approvalService.submitRequest(request.getId());

        // 2. 再次提交，应该抛出异常
        final var requestFinel = request;
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            approvalService.submitRequest(requestFinel.getId());
        });
        assertEquals("当前状态不允许提交审批", exception.getMessage());
    }

    /**
     * 测试：状态机校验 - 已执行的申请不能撤销
     */
    @Test

    public void testStateMachineValidation_已执行申请不能撤销() {
        // 1. 完整流程执行
        ApprovalRequest request = approvalService.createRequest(
                1L,
                "张三",
                "技术部",
                "MODIFY_PERMISSION",
                "测试",
                "测试",
                ApprovalProcess.RiskLevel.HIGH
        );
        request = approvalService.submitRequest(request.getId());
        request = approvalService.approve(request.getId(), 2L, "MANAGER", true, "同意");
        request = approvalService.approve(request.getId(), 3L, "SECURITY_ADMIN", true, "同意");
        request = approvalService.executeRequest(request.getId());

        // 2. 尝试撤销，应该抛出异常
        final Long finalRequestId = request.getId();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            approvalService.cancelRequest(finalRequestId, 1L);
        });
        assertEquals("已执行的申请无法撤销", exception.getMessage());
    }

    // ------------------------------ 权限校验测试 ------------------------------

    /**
     * 测试：权限校验 - 非审批人不能审批
     */
    @Test

    public void testPermissionValidation_非审批人不能审批() {
        // 1. 创建并提交审批
        ApprovalRequest request = approvalService.createRequest(
                1L,
                "张三",
                "技术部",
                "MODIFY_PERMISSION",
                "测试",
                "测试",
                ApprovalProcess.RiskLevel.HIGH
        );
        request = approvalService.submitRequest(request.getId());

        // 2. 用错误的角色审批，应该抛出异常
        final Long finalRequestId = request.getId();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            approvalService.approve(finalRequestId, 999L, "WRONG_ROLE", true, "越权审批");
        });
        assertEquals("您没有权限审批此申请", exception.getMessage());
    }

    /**
     * 测试：权限校验 - 只有申请人可以撤销
     */
    @Test

    public void testPermissionValidation_只有申请人可以撤销() {
        // 1. 创建审批
        ApprovalRequest request = approvalService.createRequest(
                1L,
                "张三",
                "技术部",
                "MODIFY_PERMISSION",
                "测试",
                "测试",
                ApprovalProcess.RiskLevel.HIGH
        );

        // 2. 用非申请人ID撤销，应该抛出异常
        final Long finalRequestId = request.getId();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            approvalService.cancelRequest(finalRequestId, 999L);
        });
        assertEquals("只有申请人可以撤销申请", exception.getMessage());
    }

    // ------------------------------ 查询功能测试 ------------------------------

    /**
     * 测试：获取待我审批的申请列表
     */
    @Test

    public void testGetPendingApprovals_待审批列表查询() {
        // 1. 创建2个审批申请并提交
        List<ApprovalRequest> pendingList_origin = approvalService.getPendingApprovals(2L, "MANAGER");

        ApprovalRequest request1 = approvalService.createRequest(
                1L, "张三", "技术部", "MODIFY_PERMISSION", "测试1", "测试", ApprovalProcess.RiskLevel.HIGH
        );
        ApprovalRequest request2 = approvalService.createRequest(
                2L, "李四", "技术部", "MODIFY_PERMISSION", "测试2", "测试", ApprovalProcess.RiskLevel.HIGH
        );
        approvalService.submitRequest(request1.getId());
        approvalService.submitRequest(request2.getId());

        // 2. 查询MANAGER角色的待审批列表
        List<ApprovalRequest> pendingList = approvalService.getPendingApprovals(2L, "MANAGER");

        // 3. 验证结果
        assertEquals(pendingList_origin.size()+2, pendingList.size());
        assertTrue(pendingList.stream().anyMatch(r -> r.getId().equals(request1.getId())));
        assertTrue(pendingList.stream().anyMatch(r -> r.getId().equals(request2.getId())));
    }

    /**
     * 测试：获取我的申请列表
     */
    @Test

    public void testGetMyRequests_我的申请列表查询() {
        List<ApprovalRequest> myRequests_origin = approvalService.getMyRequests(1L);
        // 1. 张三创建2个申请
        approvalService.createRequest(1L, "张三", "技术部", "MODIFY_PERMISSION", "测试1", "测试", ApprovalProcess.RiskLevel.HIGH);
        approvalService.createRequest(1L, "张三", "技术部", "MODIFY_CONFIG", "测试2", "测试", ApprovalProcess.RiskLevel.HIGH);
        // 李四创建1个申请
        approvalService.createRequest(2L, "李四", "技术部", "EXPORT_DATA", "测试3", "测试", ApprovalProcess.RiskLevel.MEDIUM);

        // 2. 查询张三的申请列表
        List<ApprovalRequest> myRequests = approvalService.getMyRequests(1L);

        // 3. 验证结果
        assertEquals(myRequests_origin.size()+2, myRequests.size());
        assertTrue(myRequests.stream().allMatch(r -> r.getApplicantId().equals(1L)));
    }
}