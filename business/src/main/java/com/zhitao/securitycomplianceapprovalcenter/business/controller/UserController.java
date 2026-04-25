package com.zhitao.securitycomplianceapprovalcenter.business.controller;

import com.zhitao.securitycomplianceapprovalcenter.business.dto.UserCreateRequest;
import com.zhitao.securitycomplianceapprovalcenter.business.dto.UserActionRequest;
import com.zhitao.securitycomplianceapprovalcenter.common.feign.ApprovalFeignClient;
import com.zhitao.securitycomplianceapprovalcenter.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户管理审批控制器
 * 负责发起用户管理相关的审批申请
 */
@RestController
@RequestMapping("/api/business/user")
@RequiredArgsConstructor
public class UserController {

    private final ApprovalFeignClient approvalFeignClient;

    /**
     * 发起创建用户审批申请
     */
    @PostMapping("/create")
    public String applyCreateUser(HttpServletRequest request,
                                  @RequestBody UserCreateRequest createRequest) {
        // 从请求 Header 中获取当前登录用户信息（网关透传）
        Long operatorId = Long.valueOf(request.getHeader("X-User-Id"));
        String operatorName = request.getHeader("X-User-Name");

        // 封装审批申请内容
        String operationContent = String.format("创建用户：%s (%s) - 部门：%s, 职位：%s",
                createRequest.getUsername(), createRequest.getEmail(),
                createRequest.getDepartment(), createRequest.getPosition());

        Map<String, Object> requestDTO = new HashMap<>();
        requestDTO.put("applicantId", operatorId);
        requestDTO.put("applicantName", operatorName);
        requestDTO.put("applicantDepartment", operatorName);
        requestDTO.put("operationType", "CREATE_USER");
        requestDTO.put("operationContent", operationContent);
        requestDTO.put("operationReason", "新增系统用户");
        requestDTO.put("riskLevel", "MEDIUM");

        // 存储额外的用户信息，供回调时使用
        requestDTO.put("userData", createRequest);

        // 远程调用审批服务创建申请
        Result<Map<String, Object>> response = approvalFeignClient.createApprovalRequest(requestDTO);

        return "用户创建审批申请提交成功，申请编号：" + response.getData().get("requestNo");
    }

    /**
     * 发起禁用用户审批申请
     */
    @PostMapping("/disable")
    public String applyDisableUser(HttpServletRequest request,
                                   @RequestBody UserActionRequest actionRequest) {
        Long operatorId = Long.valueOf(request.getHeader("X-User-Id"));
        String operatorName = request.getHeader("X-User-Name");

        String operationContent = String.format("禁用用户：%s (ID:%s)",
                actionRequest.getTargetUsername(), actionRequest.getTargetUserId());

        Map<String, Object> requestDTO = new HashMap<>();
        requestDTO.put("applicantId", operatorId);
        requestDTO.put("applicantName", operatorName);
        requestDTO.put("applicantDepartment", operatorName);
        requestDTO.put("operationType", "DISABLE_USER");
        requestDTO.put("operationContent", operationContent);
        requestDTO.put("operationReason", actionRequest.getOperationReason());
        requestDTO.put("riskLevel", "HIGH");

        // 存储额外的操作信息，供回调时使用
        requestDTO.put("actionData", actionRequest);

        Result<Map<String, Object>> response = approvalFeignClient.createApprovalRequest(requestDTO);

        return "用户禁用审批申请提交成功，申请编号：" + response.getData().get("requestNo");
    }

    /**
     * 发起启用用户审批申请
     */
    @PostMapping("/enable")
    public String applyEnableUser(HttpServletRequest request,
                                  @RequestBody UserActionRequest actionRequest) {
        Long operatorId = Long.valueOf(request.getHeader("X-User-Id"));
        String operatorName = request.getHeader("X-User-Name");

        String operationContent = String.format("启用用户：%s (ID:%s)",
                actionRequest.getTargetUsername(), actionRequest.getTargetUserId());

        Map<String, Object> requestDTO = new HashMap<>();
        requestDTO.put("applicantId", operatorId);
        requestDTO.put("applicantName", operatorName);
        requestDTO.put("applicantDepartment", operatorName);
        requestDTO.put("operationType", "ENABLE_USER");
        requestDTO.put("operationContent", operationContent);
        requestDTO.put("operationReason", actionRequest.getOperationReason());
        requestDTO.put("riskLevel", "MEDIUM");

        requestDTO.put("actionData", actionRequest);

        Result<Map<String, Object>> response = approvalFeignClient.createApprovalRequest(requestDTO);

        return "用户启用审批申请提交成功，申请编号：" + response.getData().get("requestNo");
    }

    /**
     * 发起重置用户密码审批申请
     */
    @PostMapping("/reset-password")
    public String applyResetUserPassword(HttpServletRequest request,
                                         @RequestBody UserActionRequest actionRequest) {
        Long operatorId = Long.valueOf(request.getHeader("X-User-Id"));
        String operatorName = request.getHeader("X-User-Name");

        String operationContent = String.format("重置用户密码：%s (ID:%s)",
                actionRequest.getTargetUsername(), actionRequest.getTargetUserId());

        Map<String, Object> requestDTO = new HashMap<>();
        requestDTO.put("applicantId", operatorId);
        requestDTO.put("applicantName", operatorName);
        requestDTO.put("applicantDepartment", operatorName);
        requestDTO.put("operationType", "RESET_USER_PASSWORD");
        requestDTO.put("operationContent", operationContent);
        requestDTO.put("operationReason", actionRequest.getOperationReason());
        requestDTO.put("riskLevel", "MEDIUM");

        requestDTO.put("actionData", actionRequest);

        Result<Map<String, Object>> response = approvalFeignClient.createApprovalRequest(requestDTO);

        return "用户重置密码审批申请提交成功，申请编号：" + response.getData().get("requestNo");
    }
}
