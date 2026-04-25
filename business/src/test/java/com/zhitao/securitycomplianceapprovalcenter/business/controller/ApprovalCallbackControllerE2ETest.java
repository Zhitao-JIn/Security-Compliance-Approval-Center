package com.zhitao.securitycomplianceapprovalcenter.business.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.*;

/**
 * ApprovalCallbackController 端到端集成测试
 *
 * 测试前提：需要先启动 auth、approval、audit 服务
 * - auth:     http://localhost:8081
 * - audit:    http://localhost:8082
 * - approval: http://localhost:8083
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApprovalCallbackControllerE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        createUser("admin", "password");
        createUser("testuser", "password");

        // 创建测试角色
        createRole("管理员", "系统管理员角色");
        createRole("普通用户", "普通用户角色");
    }

    /**
     * 创建测试用户
     */
    private void createUser(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = String.format("{\"username\":\"%s\",\"password\":\"%s\",\"status\":1}", username, password);
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity("http://localhost:8081/api/auth/user", request, Void.class);
        } catch (Exception e) {
            // 用户可能已存在，忽略
        }
    }

    /**
     * 创建测试角色
     */
    private void createRole(String name, String description) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = String.format("{\"name\":\"%s\",\"description\":\"%s\"}", name, description);
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity("http://localhost:8081/api/role", request, Void.class);
        } catch (Exception e) {
            // 角色可能已存在，忽略
        }
    }

    @Test
    @DisplayName("权限变更审批通过回调 - 端到端测试")
    void execPermissionChange_E2E() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("targetUserId", "2");
        body.add("targetRoleId", "1");
        body.add("operatorId", "1");
        body.add("operatorName", "admin");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/business/callback/permission/exec",
                HttpMethod.POST,
                request,
                String.class
        );

        // Then
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).contains("权限变更执行成功");
    }

    @Test
    @DisplayName("权限变更回调 - 缺少参数返回 500")
    void execPermissionChange_MissingParam_E2E() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("targetRoleId", "1");
        body.add("operatorId", "1");
        body.add("operatorName", "admin");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/business/callback/permission/exec",
                HttpMethod.POST,
                request,
                String.class
        );

        // Then - 缺少 targetUserId 参数，Controller 会抛异常返回 500
        assertThat(response.getStatusCodeValue()).isGreaterThanOrEqualTo(400);
    }
}
