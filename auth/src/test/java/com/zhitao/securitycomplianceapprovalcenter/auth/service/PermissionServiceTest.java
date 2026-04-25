package com.zhitao.securitycomplianceapprovalcenter.auth.service;

import com.zhitao.securitycomplianceapprovalcenter.auth.entity.Permission;
import com.zhitao.securitycomplianceapprovalcenter.auth.repository.PermissionRepository;
import com.zhitao.securitycomplianceapprovalcenter.common.feign.AuditFeignClient;
import com.zhitao.securitycomplianceapprovalcenter.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PermissionService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @InjectMocks
    private PermissionService permissionService;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private AuditFeignClient auditFeignClient;

    @Mock
    private HttpServletRequest request;

    private Permission testPermission;

    @BeforeEach
    void setUp() {
        testPermission = new Permission();
        testPermission.setId(1L);
        testPermission.setName("用户管理");
        testPermission.setCode("user:manage");
        testPermission.setDescription("管理用户信息");
        testPermission.setResourcePath("/api/user/**");
        testPermission.setOperationType("CRUD");
        testPermission.setCreateTime(LocalDateTime.now());
        testPermission.setUpdateTime(LocalDateTime.now());
    }

    private void mockRequest() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
    }

    @Test
    @DisplayName("创建权限成功 - 记录审计日志")
    void createPermission_Success_RecordsAuditLog() {
        // Given
        Permission newPermission = new Permission();
        newPermission.setName("新权限");
        newPermission.setCode("new:permission");
        newPermission.setDescription("测试权限");

        Long operatorId = 999L;
        String operatorName = "张三";
        String operationReason = "创建新权限";

        mockRequest();
        when(permissionRepository.findByCode(newPermission.getCode())).thenReturn(Optional.empty());
        when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> {
            Permission saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });
        when(auditFeignClient.recordPermissionChange(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Result.success(null));

        // When
        Permission result = permissionService.createPermission(request, newPermission, operatorId, operatorName, operationReason);

        // Then
        assertNotNull(result);
        assertEquals(100L, result.getId());
        verify(permissionRepository).findByCode(newPermission.getCode());
        verify(permissionRepository).save(newPermission);
        verify(auditFeignClient).recordPermissionChange(
                eq(operatorId),
                eq(operatorName),
                eq("CREATE_PERMISSION"),
                eq("PERMISSION"),
                eq("100"),
                eq("新权限"),
                eq("{}"),
                anyString(),
                eq(operationReason),
                eq("MEDIUM"),
                eq("SUCCESS"),
                isNull()
        );
    }

    @Test
    @DisplayName("创建权限失败 - 权限编码已存在")
    void createPermission_CodeExists_ThrowsException() {
        // Given
        Permission newPermission = new Permission();
        newPermission.setName("重复权限");
        newPermission.setCode("existing:code");

        when(permissionRepository.findByCode(newPermission.getCode())).thenReturn(Optional.of(testPermission));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            permissionService.createPermission(request, newPermission, 1L, "admin", "test");
        });
        assertEquals("权限编码已存在：" + newPermission.getCode(), exception.getMessage());
        verify(permissionRepository, never()).save(any());
        verify(auditFeignClient, never()).recordPermissionChange(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("更新权限成功 - 记录审计日志")
    void updatePermission_Success_RecordsAuditLog() {
        // Given
        Long permissionId = 1L;
        Permission updatePermission = new Permission();
        updatePermission.setName("更新后的名称");
        updatePermission.setDescription("更新后的描述");

        Long operatorId = 999L;
        String operatorName = "李四";
        String operationReason = "更新权限信息";

        mockRequest();
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(testPermission));
        when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditFeignClient.recordPermissionChange(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Result.success(null));

        // When
        Permission result = permissionService.updatePermission(request, permissionId, updatePermission, operatorId, operatorName, operationReason);

        // Then
        assertEquals("更新后的名称", result.getName());
        assertEquals("更新后的描述", result.getDescription());
        verify(auditFeignClient).recordPermissionChange(
                eq(operatorId),
                eq(operatorName),
                eq("UPDATE_PERMISSION"),
                eq("PERMISSION"),
                eq(String.valueOf(permissionId)),
                eq("更新后的名称"),
                anyString(), // beforeChange
                anyString(), // afterChange
                eq(operationReason),
                eq("MEDIUM"),
                eq("SUCCESS"),
                isNull()
        );
    }

    @Test
    @DisplayName("更新权限 - 权限不存在")
    void updatePermission_NotFound_ThrowsException() {
        // Given
        Long permissionId = 999L;
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            permissionService.updatePermission(request, permissionId, new Permission(), 1L, "admin", "test");
        });
        assertEquals("权限点不存在", exception.getMessage());
    }

    @Test
    @DisplayName("更新权限成功 - 只更新非空字段")
    void updatePermission_Success_OnlyUpdatesNonNullFields() {
        // Given
        Long permissionId = 1L;
        Permission updatePermission = new Permission();
        updatePermission.setName("新名称");
        // description, resourcePath, operationType 都为 null

        Long operatorId = 1L;
        String operatorName = "admin";
        String operationReason = "test";

        mockRequest();
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(testPermission));
        when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditFeignClient.recordPermissionChange(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Result.success(null));

        // When
        Permission result = permissionService.updatePermission(request, permissionId, updatePermission, 1L, "admin", "test");

        // Then
        assertEquals("新名称", result.getName());
        assertEquals("管理用户信息", result.getDescription()); // 原值保持不变
        assertEquals("/api/user/**", result.getResourcePath()); // 原值保持不变
        assertEquals("CRUD", result.getOperationType()); // 原值保持不变
    }

    @Test
    @DisplayName("删除权限成功 - 记录审计日志")
    void deletePermission_Success_RecordsAuditLog() {
        // Given
        Long permissionId = 1L;
        Long operatorId = 999L;
        String operatorName = "王五";
        String operationReason = "删除无用权限";

        mockRequest();
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(testPermission));
        doNothing().when(permissionRepository).delete(testPermission);
        when(auditFeignClient.recordPermissionChange(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Result.success(null));

        // When
        permissionService.deletePermission(request, permissionId, operatorId, operatorName, operationReason);

        // Then
        verify(permissionRepository).delete(testPermission);
        verify(auditFeignClient).recordPermissionChange(
                eq(operatorId),
                eq(operatorName),
                eq("DELETE_PERMISSION"),
                eq("PERMISSION"),
                eq(String.valueOf(permissionId)),
                eq(testPermission.getName()),
                anyString(), // beforeChange
                eq("{}"),
                eq(operationReason),
                eq("HIGH"),
                eq("SUCCESS"),
                isNull()
        );
    }

    @Test
    @DisplayName("删除权限 - 权限不存在")
    void deletePermission_NotFound_ThrowsException() {
        // Given
        Long permissionId = 999L;
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            permissionService.deletePermission(request, permissionId, 1L, "admin", "test");
        });
        assertEquals("权限点不存在", exception.getMessage());
    }

    @Test
    @DisplayName("查询所有权限列表")
    void getAllPermissions_Success() {
        // Given
        when(permissionRepository.findAll()).thenReturn(List.of(testPermission));

        // When
        List<Permission> result = permissionService.getAllPermissions();

        // Then
        assertEquals(1, result.size());
        assertEquals("user:manage", result.get(0).getCode());
    }

    @Test
    @DisplayName("根据 ID 查询权限成功")
    void getPermissionById_Success() {
        // Given
        Long permissionId = 1L;
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(testPermission));

        // When
        Permission result = permissionService.getPermissionById(permissionId);

        // Then
        assertNotNull(result);
        assertEquals("user:manage", result.getCode());
    }

    @Test
    @DisplayName("根据 ID 查询权限 - 不存在")
    void getPermissionById_NotFound_ThrowsException() {
        // Given
        Long permissionId = 999L;
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            permissionService.getPermissionById(permissionId);
        });
        assertEquals("权限点不存在", exception.getMessage());
    }

    @Test
    @DisplayName("根据编码查询权限成功")
    void getPermissionByCode_Success() {
        // Given
        String code = "user:manage";
        when(permissionRepository.findByCode(code)).thenReturn(Optional.of(testPermission));

        // When
        Permission result = permissionService.getPermissionByCode(code);

        // Then
        assertNotNull(result);
        assertEquals(code, result.getCode());
    }

    @Test
    @DisplayName("根据编码查询权限 - 不存在")
    void getPermissionByCode_NotFound_ThrowsException() {
        // Given
        String code = "nonexistent:code";
        when(permissionRepository.findByCode(code)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            permissionService.getPermissionByCode(code);
        });
        assertEquals("权限编码不存在", exception.getMessage());
    }
}
