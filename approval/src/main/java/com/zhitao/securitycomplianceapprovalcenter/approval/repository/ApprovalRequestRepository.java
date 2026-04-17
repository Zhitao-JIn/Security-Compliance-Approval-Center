package com.zhitao.securitycomplianceapprovalcenter.approval.repository;

import com.zhitao.securitycomplianceapprovalcenter.approval.entity.ApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 审批申请Repository
 * 类名与原文完全一致：ApprovalRequestRepository
 */
@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    // 根据申请人ID查找申请列表
    List<ApprovalRequest> findByApplicantId(Long applicantId);

    // 根据申请人ID和状态查找申请列表
    List<ApprovalRequest> findByApplicantIdAndStatus(Long applicantId, ApprovalRequest.RequestStatus status);

    // 查找待我审批的申请列表（与原文一致）
    @Query("SELECT DISTINCT ar FROM ApprovalRequest ar " +
            "JOIN ar.approvalNodes an " +
            "WHERE (an.approverId = :approverId OR an.approverRole = :approverRole) " +
            "AND an.status = 'PENDING' " +
            "AND ar.status IN ('SUBMITTED', 'APPROVING')")
    List<ApprovalRequest> findPendingApprovals(@Param("approverId") Long approverId,
                                               @Param("approverRole") String approverRole);

    // 根据申请编号查找申请
    ApprovalRequest findByRequestNo(String requestNo);

    // 根据状态查找申请列表
    List<ApprovalRequest> findByStatus(ApprovalRequest.RequestStatus status);
}
