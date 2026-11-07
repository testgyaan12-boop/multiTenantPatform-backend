package com.restaurant.saas.employee.web;

import com.restaurant.saas.authorization.security.AuthenticatedPrincipal;
import com.restaurant.saas.common.tenant.TenantContext;
import com.restaurant.saas.employee.dto.TableAssignmentRequest;
import com.restaurant.saas.employee.dto.TableAssignmentResponse;
import com.restaurant.saas.employee.service.TableAssignmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/table-assignments")
public class TableAssignmentController {

    private final TableAssignmentService assignmentService;

    public TableAssignmentController(TableAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('table.read', 'task.read', 'employee.read')")
    public List<TableAssignmentResponse> list(@RequestParam(required = false) Long branchId) {
        return assignmentService.list(TenantContext.requireTenant(), branchId);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('table.manage', 'task.manage')")
    public ResponseEntity<TableAssignmentResponse> assign(
        @Valid @RequestBody TableAssignmentRequest request,
        @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        TableAssignmentResponse response = assignmentService.assign(
            TenantContext.requireTenant(), principal.userId(), principal.email(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{tableId}")
    @PreAuthorize("hasAnyAuthority('table.manage', 'task.manage')")
    public ResponseEntity<Void> release(
        @PathVariable Long tableId,
        @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        assignmentService.release(TenantContext.requireTenant(), tableId, principal.userId(), principal.email());
        return ResponseEntity.noContent().build();
    }
}
