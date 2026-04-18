package com.zhitao.securitycomplianceapprovalcenter.approval.service;

import com.zhitao.securitycomplianceapprovalcenter.approval.entity.ApprovalNode;
import com.zhitao.securitycomplianceapprovalcenter.approval.entity.ApprovalProcess;
import com.zhitao.securitycomplianceapprovalcenter.approval.repository.ApprovalProcessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 审批流程初始化服务
 */
@Service
@RequiredArgsConstructor
public class ApprovalProcessInitService implements CommandLineRunner {

    private final ApprovalProcessRepository processRepository;

    @Override
    public void run(String... args) {
        // 如果已存在流程，不再初始化
        if (processRepository.count() > 0) {
            return;
        }

        // 初始化所有审批流程
        createDeleteDatabaseProcess();
        createTransferFundProcess();
        createModifyPermissionProcess();
        createModifyConfigProcess();
        createExportDataProcess();
    }

    /**
     * 创建数据库删除审批流程（三级审批）
     */
    private void createDeleteDatabaseProcess() {
        ApprovalProcess process = new ApprovalProcess();
        process.setProcessName("数据库删除审批流程");
        process.setOperationType("DELETE_DATABASE");
        process.setRiskLevel(ApprovalProcess.RiskLevel.CRITICAL);
        process.setApprovalLevel(3);
        process.setEnabled(true);
        process.setCreateTime(LocalDateTime.now());
        process.setUpdateTime(LocalDateTime.now());

        List<ApprovalNode> nodes = new ArrayList<>();
        // 一级审批：直接上级
        ApprovalNode node1 = new ApprovalNode();
        node1.setApprovalProcess(process);
        node1.setNodeName("直接上级审批");
        node1.setNodeOrder(0);
        node1.setApproverRole("MANAGER");
        node1.setStatus(ApprovalNode.ApprovalStatus.PENDING);
        node1.setCreateTime(LocalDateTime.now());
        nodes.add(node1);

        // 二级审批：部门经理
        ApprovalNode node2 = new ApprovalNode();
        node2.setApprovalProcess(process);
        node2.setNodeName("部门经理审批");
        node2.setNodeOrder(1);
        node2.setApproverRole("DIRECTOR");
        node2.setStatus(ApprovalNode.ApprovalStatus.PENDING);
        node2.setCreateTime(LocalDateTime.now());
        nodes.add(node2);

        // 三级审批：安全负责人
        ApprovalNode node3 = new ApprovalNode();
        node3.setApprovalProcess(process);
        node3.setNodeName("安全负责人审批");
        node3.setNodeOrder(2);
        node3.setApproverRole("SECURITY_OFFICER");
        node3.setStatus(ApprovalNode.ApprovalStatus.PENDING);
        node3.setCreateTime(LocalDateTime.now());
        nodes.add(node3);

        process.setApprovalNodes(nodes);
        processRepository.save(process);
    }

    /**
     * 创建资金转账审批流程（三级审批）
     */
    private void createTransferFundProcess() {
        ApprovalProcess process = new ApprovalProcess();
        process.setProcessName("资金转账审批流程");
        process.setOperationType("TRANSFER_FUND");
        process.setRiskLevel(ApprovalProcess.RiskLevel.CRITICAL);
        process.setApprovalLevel(3);
        process.setEnabled(true);
        process.setCreateTime(LocalDateTime.now());
        process.setUpdateTime(LocalDateTime.now());

        List<ApprovalNode> nodes = new ArrayList<>();
        ApprovalNode node1 = new ApprovalNode();
        node1.setApprovalProcess(process);
        node1.setNodeName("财务主管审批");
        node1.setNodeOrder(0);
        node1.setApproverRole("FINANCE_MANAGER");
        node1.setStatus(ApprovalNode.ApprovalStatus.PENDING);
        node1.setCreateTime(LocalDateTime.now());
        nodes.add(node1);

        ApprovalNode node2 = new ApprovalNode();
        node2.setApprovalProcess(process);
        node2.setNodeName("财务总监审批");
        node2.setNodeOrder(1);
        node2.setApproverRole("FINANCE_DIRECTOR");
        node2.setStatus(ApprovalNode.ApprovalStatus.PENDING);
        node2.setCreateTime(LocalDateTime.now());
        nodes.add(node2);

        ApprovalNode node3 = new ApprovalNode();
        node3.setApprovalProcess(process);
        node3.setNodeName("CEO审批");
        node3.setNodeOrder(2);
        node3.setApproverRole("CEO");
        node3.setStatus(ApprovalNode.ApprovalStatus.PENDING);
        node3.setCreateTime(LocalDateTime.now());
        nodes.add(node3);

        process.setApprovalNodes(nodes);
        processRepository.save(process);
    }

    /**
     * 创建权限变更审批流程（二级审批）
     */
    private void createModifyPermissionProcess() {
        ApprovalProcess process = new ApprovalProcess();
        process.setProcessName("权限变更审批流程");
        process.setOperationType("MODIFY_PERMISSION");
        process.setRiskLevel(ApprovalProcess.RiskLevel.HIGH);
        process.setApprovalLevel(2);
        process.setEnabled(true);
        process.setCreateTime(LocalDateTime.now());
        process.setUpdateTime(LocalDateTime.now());

        List<ApprovalNode> nodes = new ArrayList<>();
        ApprovalNode node1 = new ApprovalNode();
        node1.setApprovalProcess(process);
        node1.setNodeName("直接上级审批");
        node1.setNodeOrder(0);
        node1.setApproverRole("MANAGER");
        node1.setStatus(ApprovalNode.ApprovalStatus.PENDING);
        node1.setCreateTime(LocalDateTime.now());
        nodes.add(node1);

        ApprovalNode node2 = new ApprovalNode();
        node2.setApprovalProcess(process);
        node2.setNodeName("安全管理员审批");
        node2.setNodeOrder(1);
        node2.setApproverRole("SECURITY_ADMIN");
        node2.setStatus(ApprovalNode.ApprovalStatus.PENDING);
        node2.setCreateTime(LocalDateTime.now());
        nodes.add(node2);

        process.setApprovalNodes(nodes);
        processRepository.save(process);
    }

    /**
     * 创建配置修改审批流程（二级审批）
     */
    private void createModifyConfigProcess() {
        ApprovalProcess process = new ApprovalProcess();
        process.setProcessName("配置修改审批流程");
        process.setOperationType("MODIFY_CONFIG");
        process.setRiskLevel(ApprovalProcess.RiskLevel.HIGH);
        process.setApprovalLevel(2);
        process.setEnabled(true);
        process.setCreateTime(LocalDateTime.now());
        process.setUpdateTime(LocalDateTime.now());

        List<ApprovalNode> nodes = new ArrayList<>();
        ApprovalNode node1 = new ApprovalNode();
        node1.setApprovalProcess(process);
        node1.setNodeName("技术负责人审批");
        node1.setNodeOrder(0);
        node1.setApproverRole("TECH_LEAD");
        node1.setStatus(ApprovalNode.ApprovalStatus.PENDING);
        node1.setCreateTime(LocalDateTime.now());
        nodes.add(node1);

        ApprovalNode node2 = new ApprovalNode();
        node2.setApprovalProcess(process);
        node2.setNodeName("运维负责人审批");
        node2.setNodeOrder(1);
        node2.setApproverRole("OPS_LEAD");
        node2.setStatus(ApprovalNode.ApprovalStatus.PENDING);
        node2.setCreateTime(LocalDateTime.now());
        nodes.add(node2);

        process.setApprovalNodes(nodes);
        processRepository.save(process);
    }

    /**
     * 创建数据导出审批流程（一级审批）
     */
    private void createExportDataProcess() {
        ApprovalProcess process = new ApprovalProcess();
        process.setProcessName("数据导出审批流程");
        process.setOperationType("EXPORT_DATA");
        process.setRiskLevel(ApprovalProcess.RiskLevel.MEDIUM);
        process.setApprovalLevel(1);
        process.setEnabled(true);
        process.setCreateTime(LocalDateTime.now());
        process.setUpdateTime(LocalDateTime.now());

        List<ApprovalNode> nodes = new ArrayList<>();
        ApprovalNode node1 = new ApprovalNode();
        node1.setApprovalProcess(process);
        node1.setNodeName("直接上级审批");
        node1.setNodeOrder(0);
        node1.setApproverRole("MANAGER");
        node1.setStatus(ApprovalNode.ApprovalStatus.PENDING);
        node1.setCreateTime(LocalDateTime.now());
        nodes.add(node1);

        process.setApprovalNodes(nodes);
        processRepository.save(process);
    }
}
