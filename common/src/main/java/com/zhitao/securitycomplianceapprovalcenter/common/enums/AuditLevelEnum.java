package com.zhitao.securitycomplianceapprovalcenter.common.enums;

import lombok.Getter;

/**
 * 审计级别枚举
 */
@Getter
public enum AuditLevelEnum {
    LOW("LOW", "低风险"),
    MEDIUM("MEDIUM", "中风险"),
    HIGH("HIGH", "高风险"),
    CRITICAL("CRITICAL", "极高风险");

    private final String code;
    private final String desc;

    AuditLevelEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
