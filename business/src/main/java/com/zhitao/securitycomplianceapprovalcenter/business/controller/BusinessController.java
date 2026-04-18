package com.zhitao.securitycomplianceapprovalcenter.business.controller;


import com.zhitao.securitycomplianceapprovalcenter.common.feign.ApprovalFeignClient;
import com.zhitao.securitycomplianceapprovalcenter.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/business")
public class BusinessController {

    @Autowired
    private ApprovalFeignClient approvalFeignClient;

    /**
     * 发起权限变更审批申请
     */
    @PostMapping("/permission/apply")
    public String applyPermissionChange(HttpServletRequest request,
                                        @RequestParam Long targetUserId,
                                        @RequestParam Long targetRoleId,
                                        @RequestParam String operationReason) {
        // 从请求Header中获取当前登录用户信息（网关透传）
        Long operatorId = Long.valueOf(request.getHeader("X-User-Id"));
        String operatorName = request.getHeader("X-User-Name");

        // 封装审批申请内容（使用 Map 替代实体类，避免跨服务依赖）
        String operationContent = String.format("给用户ID:%s 分配角色ID:%s", targetUserId, targetRoleId);
        Map<String, Object> requestDTO = new HashMap<>();
        requestDTO.put("applicantId", operatorId);
        requestDTO.put("applicantName", operatorName);
        requestDTO.put("applicantDepartment", "技术部");
        requestDTO.put("operationType", "MODIFY_PERMISSION");
        requestDTO.put("operationContent", operationContent);
        requestDTO.put("operationReason", operationReason);
        requestDTO.put("riskLevel", "HIGH");

        // 远程调用审批服务创建申请
        Map<String, Object> approvalRequest = approvalFeignClient.createApprovalRequest(requestDTO);

        return "审批申请提交成功，申请编号：" + approvalRequest.get("requestNo");
    }

    /**
     * 查询我的申请列表
     */
    @GetMapping("/permission/my-apply")
    public List<Map<String, Object>> getMyApplyList(HttpServletRequest request) {
        Long userId = Long.valueOf(request.getHeader("X-User-Id"));
        return approvalFeignClient.getMyRequests(userId);
    }

    /**
     * 查询待我审批列表
     */
    @GetMapping("/approval/pending")
    public List<Map<String, Object>> getPendingApprovalList(HttpServletRequest request,
                                                            @RequestParam String approverRole) {
        Long userId = Long.valueOf(request.getHeader("X-User-Id"));
        return approvalFeignClient.getPendingApprovals(userId, approverRole);
    }
}