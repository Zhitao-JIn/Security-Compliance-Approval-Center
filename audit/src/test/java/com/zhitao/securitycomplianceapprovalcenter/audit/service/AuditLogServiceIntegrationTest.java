package com.zhitao.securitycomplianceapprovalcenter.audit.service;

import com.zhitao.securitycomplianceapprovalcenter.audit.entity.PermissionAuditLog;
import com.zhitao.securitycomplianceapprovalcenter.audit.repository.PermissionAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AuditLogService 集成测试（使用真实 H2 数据库）
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(AuditLogService.class)
class AuditLogServiceIntegrationTest {

    @Autowired
    private PermissionAuditLogRepository auditLogRepository;

    @Autowired
    private AuditLogService auditLogService;

    private PermissionAuditLog testLog;

    @BeforeEach
    void setUp() {
        testLog = new PermissionAuditLog();
        testLog.setOperatorId(1L);
        testLog.setOperatorName("张三");
        testLog.setOperationType("CREATE_PERMISSION");
        testLog.setPermissionObjectType("PERMISSION");
        testLog.setPermissionObjectId("1");
        testLog.setPermissionObjectName("用户管理");
        testLog.setBeforeChange("{}");
        testLog.setAfterChange("{\"name\":\"用户管理\"}");
        testLog.setOperationReason("创建新权限");
        testLog.setAuditLevel("MEDIUM");
        testLog.setOperationResult("SUCCESS");
        testLog.setClientIp("192.168.1.100");
        testLog.setServiceName("auth-service");
    }

    @Test
    @DisplayName("记录审计日志 - 真实数据库保存和查询")
    void recordAuditLog_Integration_SavesToDatabase() {
        // When
        auditLogService.recordAuditLog(testLog);

        // Then - 验证数据库中有记录
        List<PermissionAuditLog> logs = auditLogRepository.findAll();
        assertEquals(1, logs.size());

        PermissionAuditLog saved = logs.get(0);
        assertNotNull(saved.getId());
        assertNotNull(saved.getOperationTime());
        assertEquals(1L, saved.getOperatorId());
        assertEquals("张三", saved.getOperatorName());
        assertEquals("CREATE_PERMISSION", saved.getOperationType());
        assertEquals("MEDIUM", saved.getAuditLevel());
        assertEquals("SUCCESS", saved.getOperationResult());
    }

    @Test
    @DisplayName("记录多条审计日志 - 验证查询")
    void recordMultipleAuditLogs_Integration_VerifyQuery() {
        // Given
        PermissionAuditLog log2 = new PermissionAuditLog();
        log2.setOperatorId(2L);
        log2.setOperatorName("李四");
        log2.setOperationType("UPDATE_PERMISSION");
        log2.setAuditLevel("HIGH");
        log2.setOperationResult("SUCCESS");

        PermissionAuditLog log3 = new PermissionAuditLog();
        log3.setOperatorId(1L);
        log3.setOperatorName("张三");
        log3.setOperationType("DELETE_PERMISSION");
        log3.setAuditLevel("HIGH");
        log3.setOperationResult("SUCCESS");

        // When
        auditLogService.recordAuditLog(testLog);
        auditLogService.recordAuditLog(log2);
        auditLogService.recordAuditLog(log3);

        // Then
        List<PermissionAuditLog> allLogs = auditLogRepository.findAll();
        assertEquals(3, allLogs.size());

        // 按操作人查询
        List<PermissionAuditLog> operator1Logs = auditLogRepository.findAll();
        long operator1Count = operator1Logs.stream()
            .filter(log -> log.getOperatorId().equals(1L))
            .count();
        assertEquals(2, operator1Count);

        // 按审计级别查询
        long highLevelCount = auditLogRepository.findAll().stream()
            .filter(log -> "HIGH".equals(log.getAuditLevel()))
            .count();
        assertEquals(2, highLevelCount);
    }

    @Test
    @DisplayName("审计日志 - 验证字段完整性")
    void recordAuditLog_Integration_VerifyAllFields() {
        // When
        auditLogService.recordAuditLog(testLog);

        // Then
        List<PermissionAuditLog> logs = auditLogRepository.findAll();
        assertEquals(1, logs.size());

        PermissionAuditLog saved = logs.get(0);
        assertEquals("{}", saved.getBeforeChange());
        assertEquals("{\"name\":\"用户管理\"}", saved.getAfterChange());
        assertEquals("创建新权限", saved.getOperationReason());
        assertEquals("192.168.1.100", saved.getClientIp());
        assertEquals("auth-service", saved.getServiceName());
        assertEquals("PERMISSION", saved.getPermissionObjectType());
        assertEquals("1", saved.getPermissionObjectId());
        assertEquals("用户管理", saved.getPermissionObjectName());
    }

    @Test
    @DisplayName("审计日志 - 失败场景记录")
    void recordAuditLog_Integration_FailedOperation() {
        // Given
        testLog.setOperationResult("FAILED");
        testLog.setErrorMessage("数据库连接超时，无法完成操作");
        testLog.setAuditLevel("CRITICAL");

        // When
        auditLogService.recordAuditLog(testLog);

        // Then
        List<PermissionAuditLog> logs = auditLogRepository.findAll();
        assertEquals(1, logs.size());

        PermissionAuditLog saved = logs.get(0);
        assertEquals("FAILED", saved.getOperationResult());
        assertEquals("数据库连接超时，无法完成操作", saved.getErrorMessage());
        assertEquals("CRITICAL", saved.getAuditLevel());
    }

    @Test
    @DisplayName("审计日志 - 时间字段自动填充")
    void recordAuditLog_Integration_TimestampAutoFilled() {
        // Given - 不设置 operationTime
        testLog.setOperationTime(null);

        // When
        LocalDateTime beforeSave = LocalDateTime.now();
        auditLogService.recordAuditLog(testLog);
        LocalDateTime afterSave = LocalDateTime.now();

        // Then
        List<PermissionAuditLog> logs = auditLogRepository.findAll();
        assertEquals(1, logs.size());

        PermissionAuditLog saved = logs.get(0);
        assertNotNull(saved.getOperationTime());
        assertTrue(saved.getOperationTime().isAfter(beforeSave.minusSeconds(1)));
        assertTrue(saved.getOperationTime().isBefore(afterSave.plusSeconds(1)));
    }

    @Test
    @DisplayName("审计日志 - 大数据量字段存储")
    void recordAuditLog_Integration_LargeFields() {
        // Given
        String largeJsonBefore = generateLargeJson(100);
        String largeJsonAfter = generateLargeJson(200);
        testLog.setBeforeChange(largeJsonBefore);
        testLog.setAfterChange(largeJsonAfter);

        // When
        auditLogService.recordAuditLog(testLog);

        // Then
        List<PermissionAuditLog> logs = auditLogRepository.findAll();
        assertEquals(1, logs.size());

        PermissionAuditLog saved = logs.get(0);
        assertEquals(largeJsonBefore.length(), saved.getBeforeChange().length());
        assertEquals(largeJsonAfter.length(), saved.getAfterChange().length());
    }

    /**
     * 生成大型 JSON 字符串用于测试
     */
    private String generateLargeJson(int size) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < size; i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"id\":").append(i).append(",\"name\":\"permission_").append(i).append("\"}");
        }
        sb.append("]");
        return sb.toString();
    }
}
