package com.restaurant.saas.employee.web;

import com.restaurant.saas.authorization.security.AuthenticatedPrincipal;
import com.restaurant.saas.common.web.PageResponse;
import com.restaurant.saas.employee.dto.*;
import com.restaurant.saas.employee.service.EmployeeService;
import com.restaurant.saas.employee.service.StaffTaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private static final List<String> SORTABLE = List.of("createdAt", "employeeCode", "status", "hiredAt");

    private final EmployeeService employeeService;
    private final StaffTaskService taskService;

    public EmployeeController(EmployeeService employeeService, StaffTaskService taskService) {
        this.employeeService = employeeService;
        this.taskService = taskService;
    }

    public record PagedContent(List<EmployeeResponse> content, long totalElements) {
    }

    @GetMapping
    @PreAuthorize("hasAuthority('employee.read')")
    public PageResponse<EmployeeResponse> list(
        @RequestParam(required = false) Long branchId,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) Boolean onDuty,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        @RequestParam(defaultValue = "employeeCode") String sort,
        @RequestParam(defaultValue = "asc") String direction) {
        String safeSort = SORTABLE.contains(sort) ? sort : "employeeCode";
        org.springframework.data.domain.Sort.Order order = new org.springframework.data.domain.Sort.Order(
            "desc".equalsIgnoreCase(direction)
                ? org.springframework.data.domain.Sort.Direction.DESC
                : org.springframework.data.domain.Sort.Direction.ASC,
            safeSort);
        EmployeeService.PagedEmployees result = employeeService.list(
            com.restaurant.saas.common.tenant.TenantContext.requireTenant(),
            branchId, search, status, role, onDuty,
            org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by(order)));
        return new PageResponse<>(result.content(),
            new PageResponse.PageMeta(page, size, result.totalElements(),
                (int) Math.ceil(result.totalElements() / (double) Math.max(1, size))));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('employee.read')")
    public EmployeeStatsResponse stats(@RequestParam(required = false) Long branchId) {
        return employeeService.stats(com.restaurant.saas.common.tenant.TenantContext.requireTenant(), branchId);
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('employee.read')")
    public List<AssignableRoleResponse> roles() {
        return employeeService.assignableRoles();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('employee.manage')")
    public ResponseEntity<EmployeeResponse> create(
        @Valid @RequestBody EmployeeCreateRequest request,
        @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        EmployeeResponse response = employeeService.create(
            com.restaurant.saas.common.tenant.TenantContext.requireTenant(),
            principal.userId(), principal.email(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('employee.read')")
    public EmployeeResponse get(@PathVariable Long id) {
        return employeeService.get(com.restaurant.saas.common.tenant.TenantContext.requireTenant(), id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('employee.manage')")
    public EmployeeResponse update(
        @PathVariable Long id,
        @Valid @RequestBody EmployeeUpdateRequest request,
        @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return employeeService.update(
            com.restaurant.saas.common.tenant.TenantContext.requireTenant(),
            id, principal.userId(), principal.email(), request);
    }

    @PostMapping("/{id}/duty")
    @PreAuthorize("hasAnyAuthority('employee.manage', 'order.manage')")
    public EmployeeResponse duty(
        @PathVariable Long id,
        @Valid @RequestBody DutyRequest request,
        @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return employeeService.setDuty(
            com.restaurant.saas.common.tenant.TenantContext.requireTenant(),
            id, principal.userId(), principal.email(), Boolean.TRUE.equals(request.onDuty()));
    }

    @GetMapping("/{id}/performance")
    @PreAuthorize("hasAnyAuthority('employee.read', 'report.read')")
    public EmployeePerformanceResponse performance(
        @PathVariable Long id,
        @RequestParam String from,
        @RequestParam String to) {
        return employeeService.performance(
            com.restaurant.saas.common.tenant.TenantContext.requireTenant(),
            id, LocalDate.parse(from), LocalDate.parse(to));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('audit.read')")
    public List<AuditHistoryResponse> history(@PathVariable Long id) {
        return employeeService.history(com.restaurant.saas.common.tenant.TenantContext.requireTenant(), id);
    }

    @GetMapping("/tasks/summary")
    @PreAuthorize("hasAnyAuthority('task.read', 'employee.read')")
    public Map<String, Long> taskSummary(
        @RequestParam(required = false) Long branchId,
        @RequestParam String from,
        @RequestParam String to,
        @RequestParam(required = false) Long employeeId) {
        long pending = 0;
        long completed = 0;
        var tasks = taskService.list(
            com.restaurant.saas.common.tenant.TenantContext.requireTenant(),
            branchId, employeeId, null, null, null, null,
            java.time.Instant.parse(from), java.time.Instant.parse(to));
        for (var task : tasks) {
            if ("COMPLETED".equals(task.status())) completed++;
            else if (!"CANCELLED".equals(task.status())) pending++;
        }
        return Map.of("pending", pending, "completed", completed, "total", (long) tasks.size());
    }
}
