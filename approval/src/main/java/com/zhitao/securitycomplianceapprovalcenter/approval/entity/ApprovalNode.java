package com.zhitao.securitycomplianceapprovalcenter.approval.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "approval_node")
public class ApprovalNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 所属流程
    @ManyToOne
    @JoinColumn(name = "process_id")
    private ApprovalProcess approvalProcess;

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
    private ApprovalStatus status;

    // 创建时间
    private LocalDateTime createTime;

    // 审批状态枚举（与原文一致）
    public enum ApprovalStatus {
        PENDING, APPROVED, REJECTED
    }
}
