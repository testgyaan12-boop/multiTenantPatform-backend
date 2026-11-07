package com.restaurant.saas.employee.web;

import com.restaurant.saas.authorization.security.AuthenticatedPrincipal;
import com.restaurant.saas.common.tenant.TenantContext;
import com.restaurant.saas.employee.dto.StaffTaskRequest;
import com.restaurant.saas.employee.dto.StaffTaskResponse;
import com.restaurant.saas.employee.dto.StaffTaskStatusRequest;
import com.restaurant.saas.employee.service.StaffTaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
public class StaffTaskController {

    private final StaffTaskService taskService;

    public StaffTaskController(StaffTaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('task.read', 'employee.read')")
    public List<StaffTaskResponse> list(
        @RequestParam(required = false) Long branchId,
        @RequestParam(required = false) Long employeeId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String priority,
        @RequestParam(required = false) String taskType,
        @RequestParam(required = false) Long tableId,
        @RequestParam(required = false) String from,
        @RequestParam(required = false) String to) {
        return taskService.list(
            TenantContext.requireTenant(), branchId, employeeId, status, priority, taskType, tableId,
            from == null || from.isBlank() ? null : Instant.parse(from),
            to == null || to.isBlank() ? null : Instant.parse(to));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyAuthority('task.read', 'employee.read')")
    public List<StaffTaskResponse> overdue() {
        return taskService.overdue(TenantContext.requireTenant());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('task.manage', 'employee.manage')")
    public ResponseEntity<StaffTaskResponse> create(
        @Valid @RequestBody StaffTaskRequest request,
        @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            taskService.create(TenantContext.requireTenant(), principal.userId(), principal.email(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('task.manage', 'employee.manage')")
    public StaffTaskResponse update(
        @PathVariable Long id,
        @Valid @RequestBody StaffTaskRequest request,
        @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return taskService.update(TenantContext.requireTenant(), id, principal.userId(), principal.email(), request);
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('task.read', 'task.manage', 'employee.manage')")
    public StaffTaskResponse updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody StaffTaskStatusRequest request,
        @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        boolean canManage = principal.permissions().contains("task.manage")
            || principal.permissions().contains("employee.manage");
        return taskService.updateStatus(
            TenantContext.requireTenant(), id, request.status(), request.note(),
            principal.userId(), principal.email(), canManage, principal.email());
    }
}
