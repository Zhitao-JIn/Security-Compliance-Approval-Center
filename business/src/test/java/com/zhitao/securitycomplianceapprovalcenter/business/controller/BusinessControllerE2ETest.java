package com.zhitao.securitycomplianceapprovalcenter.business.controller;

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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * BusinessController 端到端集成测试
 *
 * 测试前提：需要先启动 auth、approval、audit 服务
 * - auth:     http://localhost:8081
 * - audit:    http://localhost:8082
 * - approval: http://localhost:8083
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BusinessControllerE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("发起权限变更审批申请 - 端到端测试")
    void applyPermissionChange_E2E() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("X-User-Id", "1");
        headers.set("X-User-Name", "admin");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("targetUserId", "2");
        body.add("targetRoleId", "1");
        body.add("operationReason", "工作需要");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/business/permission/apply",
                request,
                String.class
        );

        // Then
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).contains("审批申请提交成功");
        assertThat(response.getBody()).contains("APR");
    }

    @Test
    @DisplayName("查询我的申请列表 - 端到端测试")
    void getMyApplyList_E2E() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "1");
        headers.set("X-User-Name", "admin");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        // When
        ResponseEntity<List> response = restTemplate.exchange(
                "/api/business/permission/my-apply",
                HttpMethod.GET,
                request,
                List.class
        );

        // Then
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("查询待我审批列表 - 端到端测试")
    void getPendingApprovalList_E2E() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "1");
        headers.set("X-User-Name", "admin");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        // When
        ResponseEntity<List> response = restTemplate.exchange(
                "/api/business/approval/pending?approverRole=MANAGER",
                HttpMethod.GET,
                request,
                List.class
        );

        // Then
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
    }
}
