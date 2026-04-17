package com.zhitao.securitycomplianceapprovalcenter.approval.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审批申请节点实体
 * 类名与原文完全一致：RequestApprovalNode
 */
@Data
@Entity
@Table(name = "request_approval_node")
public class RequestApprovalNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 所属申请
    @ManyToOne
    @JoinColumn(name = "request_id")
    private ApprovalRequest approvalRequest;

    // 节点名称
    private String nodeName;

    // 节点顺序
    private Integer nodeOrder;

    // 审批人ID
    private Long approverId;

    // 审批人角色
    private String approverRole;

    // 节点状态
    @Enumerated(EnumType.STRING)
    private NodeStatus status;

    // 审批意见
    private String approvalComment;

    // 审批时间
    private LocalDateTime approvalTime;

    // 创建时间
    private LocalDateTime createTime;

    // 节点状态枚举（与原文一致）
    public enum NodeStatus {
        PENDING, APPROVED, REJECTED
    }
}
