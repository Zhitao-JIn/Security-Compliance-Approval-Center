package com.zhitao.securitycomplianceapprovalcenter.approval.entity;

import com.zhitao.securitycomplianceapprovalcenter.common.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批申请实体
 */
@Data
@Entity
@Table(name = "approval_request")
public class ApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 申请编号
    private String requestNo;

    // 关联的审批流程
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "process_id")
    private ApprovalProcess approvalProcess;

    // 申请人ID
    private Long applicantId;

    // 申请人名称
    private String applicantName;

    // 申请部门
    private String applicantDepartment;

    // 操作类型
    private String operationType;

    // 操作内容
    @Column(columnDefinition = "TEXT")
    private String operationContent;

    // 操作原因
    private String operationReason;

    // 风险等级
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    // 审批级别
    private Integer approvalLevel;

    // 审批节点列表
    @ToString.Exclude
    @OneToMany(mappedBy = "approvalRequest", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<RequestApprovalNode> approvalNodes;

    // 当前审批节点索引
    private Integer currentNodeIndex;

    // 申请状态
    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;

    // 申请状态枚举
    public enum RequestStatus {
        PENDING, SUBMITTED, APPROVING, APPROVED, REJECTED, EXECUTED, CANCELLED
    }
}
