package com.zhitao.securitycomplianceapprovalcenter.business.controller;

import com.zhitao.securitycomplianceapprovalcenter.common.feign.ApprovalFeignClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 审批查询控制器
 * 提供通用的审批申请查询功能
 */
@RestController
@RequestMapping("/api/business/approval")
@RequiredArgsConstructor
public class ApprovalQueryController {

    private final ApprovalFeignClient approvalFeignClient;

    /**
     * 查询我的申请列表（所有类型）
     */
    @GetMapping("/my-apply")
    public List<Map<String, Object>> getMyApplyList(HttpServletRequest request) {
        Long userId = Long.valueOf(request.getHeader("X-User-Id"));
        Result<List<Map<String, Object>>> response = approvalFeignClient.getMyRequests(userId);
        return response.getData();
    }

    /**
     * 查询待我审批列表
     */
    @GetMapping("/pending")
    public List<Map<String, Object>> getPendingApprovalList(HttpServletRequest request,
                                                            @RequestParam String approverRole) {
        Long userId = Long.valueOf(request.getHeader("X-User-Id"));
        Result<List<Map<String, Object>>> response = approvalFeignClient.getPendingApprovals(userId, approverRole);
        return response.getData();
    }
}
