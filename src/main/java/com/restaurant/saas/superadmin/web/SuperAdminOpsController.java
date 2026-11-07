package com.restaurant.saas.superadmin.web;

import com.restaurant.saas.common.web.PageResponse;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService.AuditView;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService.BranchView;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService.BroadcastRequest;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService.NotificationView;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/superadmin")
public class SuperAdminOpsController {

    private final SuperAdminPlatformService platformService;

    public SuperAdminOpsController(SuperAdminPlatformService platformService) {
        this.platformService = platformService;
    }

    @GetMapping("/branches")
    public PageResponse<BranchView> branches(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Long tenantId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return platformService.listBranches(search, status, tenantId, page, size);
    }

    @PutMapping("/branches/{id}/status")
    public BranchView updateBranchStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return platformService.updateBranchStatus(id, body.getOrDefault("status", "ACTIVE"));
    }

    @GetMapping("/audit-logs")
    public PageResponse<AuditView> auditLogs(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String entityType,
        @RequestParam(required = false) Long tenantId,
        @RequestParam(required = false) String from,
        @RequestParam(required = false) String to,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String actor,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return platformService.listAudit(search, entityType, tenantId, from, to, action, actor, page, size);
    }

    @GetMapping("/notifications")
    public PageResponse<NotificationView> notifications(
        @RequestParam(defaultValue = "false") boolean unreadOnly,
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return platformService.listNotifications(unreadOnly, search, page, size);
    }

    @PostMapping("/notifications/{id}/read")
    public NotificationView markRead(@PathVariable Long id) {
        return platformService.markNotificationRead(id);
    }

    @PostMapping("/notifications/broadcast")
    public ResponseEntity<Map<String, Object>> broadcast(@Valid @RequestBody BroadcastRequest request) {
        return ResponseEntity.ok(platformService.broadcast(request));
    }
}
