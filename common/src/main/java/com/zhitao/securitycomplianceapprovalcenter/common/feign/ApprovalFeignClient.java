package com.zhitao.securitycomplianceapprovalcenter.common.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "approval-service", url = "${service.approval.url:http://localhost:8083}")
public interface ApprovalFeignClient {

    @PostMapping("/api/approval/request")
    Map<String, Object> createApprovalRequest(@RequestBody Map<String, Object> requestDTO);

    @GetMapping("/api/approval/my-requests")
    List<Map<String, Object>> getMyRequests(@RequestParam Long applicantId);

    @GetMapping("/api/approval/pending")
    List<Map<String, Object>> getPendingApprovals(@RequestParam Long approverId,
                                                  @RequestParam String approverRole);
}
