package com.zhitao.securitycomplianceapprovalcenter.common.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "auth-service", url = "${service.auth.url:http://localhost:8081}")
public interface AuthFeignClient {

    @PostMapping("/api/role/assign")
    void assignRoleToUser(@RequestParam Long userId,
                          @RequestParam Long roleId,
                          @RequestParam Long operatorId,
                          @RequestParam String operatorName);

    @PostMapping("/api/role/remove")
    void removeUserRole(@RequestParam Long userId,
                        @RequestParam Long roleId,
                        @RequestParam Long operatorId,
                        @RequestParam String operatorName);

    @GetMapping("/api/permission/check")
    boolean checkPermission(@RequestParam Long userId, @RequestParam String permissionCode);
}
