package com.zhitao.securitycomplianceapprovalcenter.approval.repository;

import com.zhitao.securitycomplianceapprovalcenter.approval.entity.ApprovalProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 审批流程Repository
 * 类名与原文完全一致：ApprovalProcessRepository
 */
@Repository
public interface ApprovalProcessRepository extends JpaRepository<ApprovalProcess, Long> {

    // 根据操作类型和风险等级查找审批流程（与原文一致）
    ApprovalProcess findByOperationTypeAndRiskLevel(String operationType, ApprovalProcess.RiskLevel riskLevel);

    // 根据操作类型查找审批流程列表
    List<ApprovalProcess> findByOperationType(String operationType);

    // 根据风险等级查找审批流程列表
    List<ApprovalProcess> findByRiskLevel(ApprovalProcess.RiskLevel riskLevel);
}
