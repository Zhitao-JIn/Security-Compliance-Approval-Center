package com.zhitao.securitycomplianceapprovalcenter.audit.service;

import com.zhitao.securitycomplianceapprovalcenter.audit.entity.PermissionAuditLog;
import com.zhitao.securitycomplianceapprovalcenter.audit.repository.PermissionAuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
/**
 * 审计服务完整单元测试
 * @Transactional + @Rollback：测试后自动回滚，不污染数据库
 */
@SpringBootTest
public class AuditLogServiceTest {

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private PermissionAuditLogRepository auditLogRepository;

    // ------------------------------ 正常场景测试 ------------------------------

    /**
     * 测试：正常权限变更日志写入
     */
    @Test
    @Transactional
    @Rollback
    public void testRecordAuditLog_success() {
        // 1. 构建测试数据
        PermissionAuditLog testLog = new PermissionAuditLog();
        testLog.setOperatorId(1L);
        testLog.setOperatorName("系统管理员");
        testLog.setOperationType("UPDATE_ROLE_PERMISSIONS");
        testLog.setPermissionObjectType("ROLE");
        testLog.setPermissionObjectId("1001");
        testLog.setPermissionObjectName("系统管理员角色");
        testLog.setBeforeChange("{\"permissions\":[\"user:list\"]}");
        testLog.setAfterChange("{\"permissions\":[\"user:list\",\"user:add\",\"user:delete\"]}");
        testLog.setOperationReason("新员工入职权限调整");
        testLog.setAuditLevel("HIGH");
        testLog.setOperationResult("SUCCESS");
        testLog.setClientIp("127.0.0.1");
        testLog.setServiceName("auth-service");

        // 2. 执行写入
        auditLogService.recordAuditLog(testLog);

        // 3. 验证结果
        assertNotNull(testLog.getId(), "日志写入后ID不能为空");
        Optional<PermissionAuditLog> savedLog = auditLogRepository.findById(testLog.getId());
        assertTrue(savedLog.isPresent(), "数据库中必须能查到写入的日志");

        // 4. 字段一致性校验
        PermissionAuditLog result = savedLog.get();
        assertEquals(testLog.getOperatorName(), result.getOperatorName());
        assertEquals(testLog.getBeforeChange(), result.getBeforeChange());
        assertEquals(testLog.getAfterChange(), result.getAfterChange());
        assertEquals("SUCCESS", result.getOperationResult());
        assertNotNull(result.getOperationTime(), "操作时间不能为空");
    }

    /**
     * 测试：操作失败场景日志写入
     */
    @Test
    @Transactional
    @Rollback
    public void testRecordAuditLog_fail() {
        // 1. 构建测试数据
        PermissionAuditLog testLog = new PermissionAuditLog();
        testLog.setOperatorId(2L);
        testLog.setOperatorName("普通用户");
        testLog.setOperationType("DELETE_ROLE");
        testLog.setPermissionObjectType("ROLE");
        testLog.setPermissionObjectId("1002");
        testLog.setPermissionObjectName("测试角色");
        testLog.setBeforeChange("{\"id\":1002,\"name\":\"测试角色\"}");
        testLog.setAfterChange("{}");
        testLog.setOperationReason("非法删除角色");
        testLog.setAuditLevel("CRITICAL");
        testLog.setOperationResult("FAIL");
        testLog.setErrorMessage("权限不足，无角色删除权限");
        testLog.setClientIp("192.168.1.100");
        testLog.setServiceName("approval-service");

        // 2. 执行写入
        auditLogService.recordAuditLog(testLog);

        // 3. 验证结果
        Optional<PermissionAuditLog> savedLog = auditLogRepository.findById(testLog.getId());
        assertTrue(savedLog.isPresent());
        assertEquals("FAIL", savedLog.get().getOperationResult());
        assertNotNull(savedLog.get().getErrorMessage());
        assertEquals("权限不足，无角色删除权限", savedLog.get().getErrorMessage());
    }

    // ------------------------------ 边界场景测试 ------------------------------

    /**
     * 测试：超长文本写入（TEXT字段）
     */
    @Test
    @Transactional
    @Rollback
    public void testRecordAuditLog_long() {
        // 构建10000字的超长JSON
        StringBuilder longBeforeChange = new StringBuilder("{\"permissions\":[");
        for (int i = 0; i < 1000; i++) {
            longBeforeChange.append("\"permission_").append(i).append("\",");
        }
        longBeforeChange.deleteCharAt(longBeforeChange.length() - 1);
        longBeforeChange.append("]}");

        PermissionAuditLog testLog = new PermissionAuditLog();
        testLog.setOperatorId(1L);
        testLog.setOperatorName("测试用户");
        testLog.setOperationType("PRESS_TEST");
        testLog.setPermissionObjectType("TEST");
        testLog.setPermissionObjectId("test_001");
        testLog.setPermissionObjectName("超长文本测试");
        testLog.setBeforeChange(longBeforeChange.toString());
        testLog.setAfterChange("{}");
        testLog.setOperationReason("超长文本测试");
        testLog.setAuditLevel("LOW");
        testLog.setOperationResult("SUCCESS");

        // 执行写入
        auditLogService.recordAuditLog(testLog);

        // 验证结果：文本无截断
        Optional<PermissionAuditLog> savedLog = auditLogRepository.findById(testLog.getId());
        assertTrue(savedLog.isPresent());
        assertEquals(longBeforeChange.length(), savedLog.get().getBeforeChange().length());
    }

    /**
     * 测试：特殊字符写入
     */
    @Test
    @Transactional
    @Rollback
    public void testRecordAuditLog_special() {
        PermissionAuditLog testLog = new PermissionAuditLog();
        testLog.setOperatorId(1L);
        testLog.setOperatorName("测试用户\n换行");
        testLog.setOperationType("SPECIAL_CHAR_TEST");
        testLog.setPermissionObjectType("TEST");
        testLog.setPermissionObjectId("test_002");
        testLog.setPermissionObjectName("特殊字符测试\"引号");
        testLog.setBeforeChange("{\"data\":\"测试emoji😊\"}");
        testLog.setAfterChange("{}");
        testLog.setOperationReason("特殊字符测试");
        testLog.setAuditLevel("LOW");
        testLog.setOperationResult("SUCCESS");

        // 执行写入
        auditLogService.recordAuditLog(testLog);

        // 验证结果：特殊字符完整保存
        Optional<PermissionAuditLog> savedLog = auditLogRepository.findById(testLog.getId());
        assertTrue(savedLog.isPresent());
        assertTrue(savedLog.get().getOperatorName().contains("\n"));
        assertTrue(savedLog.get().getPermissionObjectName().contains("\""));
        assertTrue(savedLog.get().getBeforeChange().contains("😊"));
    }
}