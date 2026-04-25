package com.zhitao.securitycomplianceapprovalcenter.auth.service;

import com.zhitao.securitycomplianceapprovalcenter.auth.entity.Permission;
import com.zhitao.securitycomplianceapprovalcenter.auth.entity.Role;
import com.zhitao.securitycomplianceapprovalcenter.auth.entity.User;
import com.zhitao.securitycomplianceapprovalcenter.common.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserService userService;

    @Mock
    private RoleService roleService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    private User testUser;
    private Role testRole;
    private Permission testPermission;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword123");
        testUser.setEmail("test@example.com");
        testUser.setStatus(1);
        testUser.setCreateTime(LocalDateTime.now());

        testPermission = new Permission();
        testPermission.setId(1L);
        testPermission.setName("用户管理");
        testPermission.setCode("user:manage");

        testRole = new Role();
        testRole.setId(1L);
        testRole.setName("管理员");
        testRole.setPermissions(List.of(testPermission));
    }

    @Test
    @DisplayName("登录成功 - 返回 JWT Token")
    void login_Success_ReturnsToken() {
        // Given
        String username = "testuser";
        String password = "rawPassword123";
        String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        List<String> permissionCodes = List.of("user:manage");

        when(userService.getUserByUsername(username)).thenReturn(testUser);
        when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(true);
        when(roleService.getUserPermissionCodes(testUser.getId())).thenReturn(permissionCodes);
        when(jwtUtil.createToken(any(Map.class), eq(username))).thenReturn(expectedToken);

        // When
        String token = authService.login(username, password);

        // Then
        assertEquals(expectedToken, token);
        verify(userService).getUserByUsername(username);
        verify(passwordEncoder).matches(password, testUser.getPassword());
        verify(roleService).getUserPermissionCodes(testUser.getId());
        verify(jwtUtil).createToken(any(Map.class), eq(username));
    }

    @Test
    @DisplayName("登录失败 - 用户不存在")
    void login_UserNotFound_ThrowsException() {
        // Given
        String username = "nonexistent";
        String password = "password123";

        when(userService.getUserByUsername(username)).thenReturn(null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(username, password);
        });
        assertEquals("用户名或密码错误", exception.getMessage());
        verify(userService).getUserByUsername(username);
        verifyNoMoreInteractions(passwordEncoder, roleService, jwtUtil);
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void login_WrongPassword_ThrowsException() {
        // Given
        String username = "testuser";
        String password = "wrongPassword";

        when(userService.getUserByUsername(username)).thenReturn(testUser);
        when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(username, password);
        });
        assertEquals("用户名或密码错误", exception.getMessage());
        verify(userService).getUserByUsername(username);
        verify(passwordEncoder).matches(password, testUser.getPassword());
        verifyNoMoreInteractions(roleService, jwtUtil);
    }

    @Test
    @DisplayName("登录失败 - 用户被禁用")
    void login_UserDisabled_ThrowsException() {
        // Given
        testUser.setStatus(0); // 禁用状态
        String username = "testuser";
        String password = "password123";

        when(userService.getUserByUsername(username)).thenReturn(testUser);
        when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(username, password);
        });
        assertEquals("用户已被禁用", exception.getMessage());
        verify(userService).getUserByUsername(username);
        verify(passwordEncoder).matches(password, testUser.getPassword());
        verifyNoInteractions(roleService, jwtUtil);
    }

    @Test
    @DisplayName("登录成功 - JWT 包含正确的用户信息和权限")
    void login_Success_TokenContainsUserInfoAndPermissions() {
        // Given
        String username = "testuser";
        String password = "rawPassword123";
        List<String> permissionCodes = List.of("user:manage", "role:view");

        when(userService.getUserByUsername(username)).thenReturn(testUser);
        when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(true);
        when(roleService.getUserPermissionCodes(testUser.getId())).thenReturn(permissionCodes);
        when(jwtUtil.createToken(any(Map.class), eq(username))).thenAnswer(invocation -> {
            Map<String, Object> claims = invocation.getArgument(0);
            // 验证 claims 中包含正确的信息
            assertEquals(testUser.getId(), claims.get("userId"));
            assertEquals(username, claims.get("username"));
            assertEquals(permissionCodes, claims.get("permissions"));
            return "mockedToken";
        });

        // When
        String token = authService.login(username, password);

        // Then
        assertEquals("mockedToken", token);
    }

    @Test
    @DisplayName("权限校验成功 - 用户拥有指定权限")
    void hasPermission_UserHasPermission_ReturnsTrue() {
        // Given
        Long userId = 1L;
        String permissionCode = "user:manage";

        Role role1 = new Role();
        Permission perm1 = new Permission();
        perm1.setCode("user:manage");
        role1.setPermissions(List.of(perm1));

        Role role2 = new Role();
        Permission perm2 = new Permission();
        perm2.setCode("role:view");
        role2.setPermissions(List.of(perm2));

        when(roleService.getRolesByUserId(userId)).thenReturn(List.of(role1, role2));

        // When
        boolean result = authService.hasPermission(userId, permissionCode);

        // Then
        assertTrue(result);
        verify(roleService).getRolesByUserId(userId);
    }

    @Test
    @DisplayName("权限校验失败 - 用户没有指定权限")
    void hasPermission_UserNoPermission_ReturnsFalse() {
        // Given
        Long userId = 1L;
        String permissionCode = "admin:delete";

        Role role = new Role();
        Permission perm = new Permission();
        perm.setCode("user:view");
        role.setPermissions(List.of(perm));

        when(roleService.getRolesByUserId(userId)).thenReturn(List.of(role));

        // When
        boolean result = authService.hasPermission(userId, permissionCode);

        // Then
        assertFalse(result);
        verify(roleService).getRolesByUserId(userId);
    }

    @Test
    @DisplayName("权限校验成功 - 用户没有任何角色")
    void hasPermission_UserNoRoles_ReturnsFalse() {
        // Given
        Long userId = 1L;
        String permissionCode = "user:manage";

        when(roleService.getRolesByUserId(userId)).thenReturn(new ArrayList<>());

        // When
        boolean result = authService.hasPermission(userId, permissionCode);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("权限校验成功 - 权限码匹配第一个角色")
    void hasPermission_PermissionInFirstRole_ReturnsTrue() {
        // Given
        Long userId = 1L;
        String permissionCode = "dashboard:view";

        Permission perm1 = new Permission();
        perm1.setCode("dashboard:view");
        Role role1 = new Role();
        role1.setPermissions(List.of(perm1));

        Permission perm2 = new Permission();
        perm2.setCode("report:view");
        Role role2 = new Role();
        role2.setPermissions(List.of(perm2));

        when(roleService.getRolesByUserId(userId)).thenReturn(List.of(role1, role2));

        // When
        boolean result = authService.hasPermission(userId, permissionCode);

        // Then
        assertTrue(result);
    }
}
