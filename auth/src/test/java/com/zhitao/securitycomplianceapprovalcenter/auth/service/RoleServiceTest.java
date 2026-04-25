package com.zhitao.securitycomplianceapprovalcenter.auth.service;

import com.zhitao.securitycomplianceapprovalcenter.auth.entity.Permission;
import com.zhitao.securitycomplianceapprovalcenter.auth.entity.Role;
import com.zhitao.securitycomplianceapprovalcenter.auth.entity.UserRole;
import com.zhitao.securitycomplianceapprovalcenter.auth.repository.PermissionRepository;
import com.zhitao.securitycomplianceapprovalcenter.auth.repository.RoleRepository;
import com.zhitao.securitycomplianceapprovalcenter.auth.repository.UserRoleRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RoleService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @InjectMocks
    private RoleService roleService;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private AuditFeignClient auditFeignClient;

    @Mock
    private HttpServletRequest request;

    @Mock
    private RoleService roleServiceSelf;

    private Role testRole;
    private Permission testPermission1;
    private Permission testPermission2;
    private UserRole testUserRole;

    @BeforeEach
    void setUp() {
        // 准备测试数据 - 角色
        testRole = new Role();
        testRole.setId(1L);
        testRole.setName("管理员");
        testRole.setDescription("系统管理员角色");
        testRole.setCreateTime(LocalDateTime.now());
        testRole.setUpdateTime(LocalDateTime.now());

        // 准备测试数据 - 权限 1
        testPermission1 = new Permission();
        testPermission1.setId(1L);
        testPermission1.setName("用户管理");
        testPermission1.setCode("user:manage");

        // 准备测试数据 - 权限 2
        testPermission2 = new Permission();
        testPermission2.setId(2L);
        testPermission2.setName("角色查看");
        testPermission2.setCode("role:view");

        // 准备测试数据 - 用户角色关联
        testUserRole = new UserRole();
        testUserRole.setUserId(100L);
        testUserRole.setRoleId(1L);
        testUserRole.setCreateTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("创建角色成功 - 自动填充时间字段")
    void createRole_Success_FillsTimestamps() {
        // Given
        Role newRole = new Role();
        newRole.setName("新角色");
        newRole.setDescription("测试角色");

        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role savedRole = invocation.getArgument(0);
            assertNotNull(savedRole.getCreateTime());
            assertNotNull(savedRole.getUpdateTime());
            return savedRole;
        });

        // When
        Role result = roleService.createRole(newRole);

        // Then
        assertNotNull(result.getCreateTime());
        assertNotNull(result.getUpdateTime());
        verify(roleRepository).save(newRole);
    }

    @Test
    @DisplayName("给用户分配角色成功 - 验证用户角色关联")
    void assignRoleToUser_Success_CreatesUserRole() {
        // Given
        Long userId = 100L;
        Long roleId = 1L;
        UserRole capturedUserRole = new UserRole();

        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> {
            capturedUserRole.setUserId(invocation.<UserRole>getArgument(0).getUserId());
            capturedUserRole.setRoleId(invocation.<UserRole>getArgument(0).getRoleId());
            capturedUserRole.setCreateTime(invocation.<UserRole>getArgument(0).getCreateTime());
            return capturedUserRole;
        });

        // When
        roleService.assignRoleToUser(userId, roleId);

        // Then
        assertEquals(userId, capturedUserRole.getUserId(), "用户 ID 应该被正确设置");
        assertEquals(roleId, capturedUserRole.getRoleId(), "角色 ID 应该被正确设置");
        assertNotNull(capturedUserRole.getCreateTime(), "创建时间应该被自动填充");
        verify(userRoleRepository).save(argThat(ur ->
            ur.getUserId().equals(userId) && ur.getRoleId().equals(roleId)
        ));
    }

    @Test
    @DisplayName("获取用户角色列表成功")
    void getRolesByUserId_Success_ReturnsRoles() {
        // Given
        Long userId = 100L;
        UserRole userRole1 = new UserRole();
        userRole1.setUserId(userId);
        userRole1.setRoleId(1L);

        UserRole userRole2 = new UserRole();
        userRole2.setUserId(userId);
        userRole2.setRoleId(2L);

        Role role1 = new Role();
        role1.setId(1L);
        role1.setName("管理员");

        Role role2 = new Role();
        role2.setId(2L);
        role2.setName("普通用户");

        when(userRoleRepository.findByUserId(userId)).thenReturn(List.of(userRole1, userRole2));
        when(roleRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(role1, role2));

        // When
        List<Role> result = roleService.getRolesByUserId(userId);

        // Then
        assertEquals(2, result.size());
        verify(userRoleRepository).findByUserId(userId);
        verify(roleRepository).findAllById(List.of(1L, 2L));
    }

    @Test
    @DisplayName("获取用户角色列表 - 用户没有角色")
    void getRolesByUserId_NoRoles_ReturnsEmpty() {
        // Given
        Long userId = 100L;
        when(userRoleRepository.findByUserId(userId)).thenReturn(new ArrayList<>());
        when(roleRepository.findAllById(new ArrayList<>())).thenReturn(new ArrayList<>());

        // When
        List<Role> result = roleService.getRolesByUserId(userId);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("获取用户权限编码成功 - 去重")
    void getUserPermissionCodes_Success_ReturnsUniqueCodes() {
        // Given
        Long userId = 100L;

        // 权限 1 和权限 3 编码相同，用于测试去重
        Permission perm1 = new Permission();
        perm1.setId(1L);
        perm1.setCode("user:manage");

        Permission perm2 = new Permission();
        perm2.setId(2L);
        perm2.setCode("role:view");

        Permission perm3 = new Permission();
        perm3.setId(3L);
        perm3.setCode("user:manage"); // 重复编码

        Role role1 = new Role();
        role1.setPermissions(List.of(perm1, perm2));

        Role role2 = new Role();
        role2.setPermissions(List.of(perm3));

        when(userRoleRepository.findByUserId(userId)).thenReturn(List.of(testUserRole));
        when(roleRepository.findAllById(List.of(1L))).thenReturn(List.of(role1, role2));

        // When
        List<String> result = roleService.getUserPermissionCodes(userId);

        // Then
        assertTrue(result.contains("user:manage"));
        assertTrue(result.contains("role:view"));
        assertEquals(2, result.size()); // 验证去重
    }

    @Test
    @DisplayName("获取用户权限编码 - 用户没有权限")
    void getUserPermissionCodes_NoPermissions_ReturnsEmpty() {
        // Given
        Long userId = 100L;
        Role role = new Role();
        role.setPermissions(new ArrayList<>());

        when(userRoleRepository.findByUserId(userId)).thenReturn(List.of(testUserRole));
        when(roleRepository.findAllById(List.of(1L))).thenReturn(List.of(role));

        // When
        List<String> result = roleService.getUserPermissionCodes(userId);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("给角色绑定权限成功 - 记录审计日志")
    void bindPermissionsToRole_Success_RecordsAuditLog() {
        // Given
        Long roleId = 1L;
        List<Long> permissionIds = List.of(1L, 2L);
        Long operatorId = 999L;
        String operatorName = "张三";
        String operationReason = "添加权限";

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));
        when(permissionRepository.findAllById(permissionIds)).thenReturn(List.of(testPermission1, testPermission2));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditFeignClient.recordPermissionChange(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Result.success(null));

        // When
        Role result = roleService.bindPermissionsToRole(request, roleId, permissionIds, operatorId, operatorName, operationReason);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getPermissions().size());
        verify(auditFeignClient).recordPermissionChange(
                eq(operatorId),
                eq(operatorName),
                eq("BIND_ROLE_PERMISSION"),
                eq("ROLE_PERMISSION"),
                eq(String.valueOf(roleId)),
                eq(testRole.getName()),
                anyString(), // beforeChange
                anyString(), // afterChange
                eq(operationReason),
                eq("HIGH"),
                eq("SUCCESS"),
                isNull()
        );
    }

    @Test
    @DisplayName("给角色绑定权限 - 角色不存在")
    void bindPermissionsToRole_RoleNotFound_ThrowsException() {
        // Given
        Long roleId = 999L;
        List<Long> permissionIds = List.of(1L);

        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            roleService.bindPermissionsToRole(request, roleId, permissionIds, 1L, "admin", "test");
        });
        assertEquals("角色不存在", exception.getMessage());
    }

    @Test
    @DisplayName("权限校验成功 - 用户有指定权限")
    void hasPermission_UserHasPermission_ReturnsTrue() {
        // Given
        Long userId = 100L;
        String permissionCode = "user:manage";

        Permission perm = new Permission();
        perm.setCode(permissionCode);
        Role role = new Role();
        role.setPermissions(List.of(perm));

        when(roleServiceSelf.getUserPermissionCodes(userId)).thenReturn(List.of(permissionCode, "role:view"));

        // When
        boolean result = roleService.hasPermission(userId, permissionCode);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("权限校验成功 - 用户是超管")
    void hasPermission_UserIsSuperAdmin_ReturnsTrue() {
        // Given
        Long userId = 100L;
        String permissionCode = "admin:delete";

        when(roleServiceSelf.getUserPermissionCodes(userId)).thenReturn(List.of("*:*:*"));

        // When
        boolean result = roleService.hasPermission(userId, permissionCode);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("权限校验失败 - 用户没有指定权限")
    void hasPermission_UserNoPermission_ReturnsFalse() {
        // Given
        Long userId = 100L;
        String permissionCode = "admin:delete";

        when(roleServiceSelf.getUserPermissionCodes(userId)).thenReturn(List.of("user:view", "role:view"));

        // When
        boolean result = roleService.hasPermission(userId, permissionCode);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("权限校验 - 用户没有任何权限")
    void hasPermission_UserNoPermissions_ReturnsFalse() {
        // Given
        Long userId = 100L;
        String permissionCode = "user:manage";

        when(roleServiceSelf.getUserPermissionCodes(userId)).thenReturn(new ArrayList<>());

        // When
        boolean result = roleService.hasPermission(userId, permissionCode);

        // Then
        assertFalse(result);
    }
}
