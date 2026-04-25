package com.zhitao.securitycomplianceapprovalcenter.auth.service;

import com.zhitao.securitycomplianceapprovalcenter.auth.entity.User;
import com.zhitao.securitycomplianceapprovalcenter.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * UserService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("rawPassword123");
        testUser.setEmail("test@example.com");
        testUser.setPhone("13800138000");
        testUser.setStatus(1);
        testUser.setCreateTime(LocalDateTime.now());
        testUser.setUpdateTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("创建用户成功 - 密码加密 + 时间填充")
    void createUser_Success_EncryptsPasswordAndFillsTimestamps() {
        // Given
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setPassword("rawPassword");
        newUser.setEmail("newuser@example.com");

        String encodedPassword = "bcryptEncodedPassword123";
        when(passwordEncoder.encode(newUser.getPassword())).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        // When
        User result = userService.createUser(newUser);

        // Then
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(encodedPassword, result.getPassword());
        assertNotNull(result.getCreateTime());
        assertNotNull(result.getUpdateTime());
        verify(passwordEncoder).encode("rawPassword");
        verify(userRepository).save(newUser);
    }

    @Test
    @DisplayName("根据 ID 查询用户成功")
    void getUserById_Success() {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        User result = userService.getUserById(userId);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("根据 ID 查询用户 - 不存在返回 null")
    void getUserById_NotFound_ReturnsNull() {
        // Given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When
        User result = userService.getUserById(userId);

        // Then
        assertNull(result);
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("根据用户名查询用户成功")
    void getUserByUsername_Success() {
        // Given
        String username = "testuser";
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        // When
        User result = userService.getUserByUsername(username);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository).findByUsername(username);
    }

    @Test
    @DisplayName("根据用户名查询用户 - 不存在返回 null")
    void getUserByUsername_NotFound_ReturnsNull() {
        // Given
        String username = "nonexistent";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // When
        User result = userService.getUserByUsername(username);

        // Then
        assertNull(result);
        verify(userRepository).findByUsername(username);
    }
}
