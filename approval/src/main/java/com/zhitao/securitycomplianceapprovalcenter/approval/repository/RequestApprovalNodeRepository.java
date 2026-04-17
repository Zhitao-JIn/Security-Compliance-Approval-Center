package com.zhitao.securitycomplianceapprovalcenter.approval.repository;

import com.zhitao.securitycomplianceapprovalcenter.approval.entity.RequestApprovalNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 审批申请节点Repository
 * 类名与原文完全一致：RequestApprovalNodeRepository
 */
@Repository
public interface RequestApprovalNodeRepository extends JpaRepository<RequestApprovalNode, Long> {

    // 根据申请ID查找审批节点列表
    List<RequestApprovalNode> findByApprovalRequestId(Long requestId);

    // 根据审批人ID和状态查找审批节点列表
    List<RequestApprovalNode> findByApproverIdAndStatus(Long approverId, RequestApprovalNode.NodeStatus status);

    // 根据审批人角色和状态查找审批节点列表
    List<RequestApprovalNode> findByApproverRoleAndStatus(String approverRole, RequestApprovalNode.NodeStatus status);
}
