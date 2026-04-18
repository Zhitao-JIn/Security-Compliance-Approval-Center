package com.zhitao.securitycomplianceapprovalcenter.approval.entity;

import com.zhitao.securitycomplianceapprovalcenter.common.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import com.zhitao.securitycomplianceapprovalcenter.approval.entity.ApprovalNode;
import lombok.ToString;

@Data
@Entity
@Table(name = "approval_process")
public class ApprovalProcess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 流程名称
    private String processName;

    // 操作类型
    private String operationType;

    // 风险等级
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    // 审批级别
    private Integer approvalLevel;

    // 审批节点列表
    @ToString.Exclude
    @OneToMany(mappedBy = "approvalProcess", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ApprovalNode> approvalNodes;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;

    // 是否启用
    private Boolean enabled;

}
