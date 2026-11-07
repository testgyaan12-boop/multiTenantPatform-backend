package com.restaurant.saas.employee.service;

import com.restaurant.saas.audit.service.AuditService;
import com.restaurant.saas.common.exception.ForbiddenException;
import com.restaurant.saas.common.exception.NotFoundException;
import com.restaurant.saas.diningtable.repository.DiningTableRepository;
import com.restaurant.saas.employee.dto.TableAssignmentRequest;
import com.restaurant.saas.employee.dto.TableAssignmentResponse;
import com.restaurant.saas.employee.entity.Employee;
import com.restaurant.saas.employee.entity.TableAssignment;
import com.restaurant.saas.employee.repository.EmployeeRepository;
import com.restaurant.saas.employee.repository.TableAssignmentRepository;
import com.restaurant.saas.identity.entity.User;
import com.restaurant.saas.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TableAssignmentService {

    private final TableAssignmentRepository assignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final DiningTableRepository tableRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public TableAssignmentService(
        TableAssignmentRepository assignmentRepository,
        EmployeeRepository employeeRepository,
        DiningTableRepository tableRepository,
        UserRepository userRepository,
        AuditService auditService) {
        this.assignmentRepository = assignmentRepository;
        this.employeeRepository = employeeRepository;
        this.tableRepository = tableRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<TableAssignmentResponse> list(Long tenantId, Long branchId) {
        List<TableAssignment> assignments = branchId == null
            ? assignmentRepository.findByTenantIdAndIsActiveTrue(tenantId)
            : assignmentRepository.findByTenantIdAndBranchIdAndIsActiveTrue(tenantId, branchId);
        Map<Long, Employee> employees = new HashMap<>();
        employeeRepository.findAllById(
            assignments.stream().map(TableAssignment::getEmployeeId).distinct().toList())
            .forEach(e -> employees.put(e.getId(), e));
        Map<Long, String> names = new HashMap<>();
        Map<Long, String> roles = new HashMap<>();
        List<Long> userIds = employees.values().stream().map(Employee::getUserId)
            .filter(Objects::nonNull).toList();
        Map<Long, User> users = new HashMap<>();
        if (!userIds.isEmpty()) {
            userRepository.findAllByIdWithAuthorities(userIds).forEach(u -> users.put(u.getId(), u));
        }
        employees.forEach((id, employee) -> {
            User user = employee.getUserId() == null ? null : users.get(employee.getUserId());
            if (user != null) {
                names.put(id, user.fullName());
                roles.put(id, user.getRoles().stream().map(r -> r.getName()).sorted().findFirst().orElse(""));
            } else {
                names.put(id, employee.getEmployeeCode());
            }
        });

        return assignments.stream()
            .sorted(Comparator.comparing(TableAssignment::getAssignedAt))
            .map(assignment -> {
                var table = tableRepository.findById(assignment.getTableId()).orElse(null);
                return new TableAssignmentResponse(
                    assignment.getId(),
                    assignment.getTableId(),
                    table == null ? null : table.getTableNumber(),
                    assignment.getBranchId(),
                    assignment.getEmployeeId(),
                    names.getOrDefault(assignment.getEmployeeId(), ""),
                    roles.getOrDefault(assignment.getEmployeeId(), ""),
                    assignment.getAssignedAt(),
                    assignment.getReleasedAt());
            })
            .toList();
    }

    @Transactional
    public TableAssignmentResponse assign(Long tenantId, Long actorUserId, String actorEmail,
                                          TableAssignmentRequest request) {
        var table = tableRepository.findById(request.tableId())
            .orElseThrow(() -> new NotFoundException("Table", request.tableId()));
        if (!tenantId.equals(table.getTenantId()) || !request.branchId().equals(table.getBranchId())) {
            throw new ForbiddenException("Table does not belong to this branch.");
        }
        Employee employee = employeeRepository.findByIdAndTenantId(request.employeeId(), tenantId)
            .orElseThrow(() -> new NotFoundException("Employee", request.employeeId()));
        if (!employee.getBranchId().equals(request.branchId())) {
            throw new ForbiddenException("The employee does not work in this branch.");
        }
        if (!"ACTIVE".equals(employee.getStatus())) {
            throw new ForbiddenException("Only active employees can be assigned to tables.");
        }

        TableAssignment existing = assignmentRepository
            .findByTenantIdAndTableIdAndIsActiveTrue(tenantId, request.tableId())
            .orElse(null);
        if (existing != null) {
            if (existing.getEmployeeId().equals(employee.getId())) {
                return toResponse(existing);
            }
            existing.setIsActive(Boolean.FALSE);
            existing.setReleasedAt(Instant.now());
            assignmentRepository.save(existing);
            auditService.record(tenantId, actorUserId, actorEmail, "TABLE_ASSIGNMENT_REPLACED", "TableAssignment",
                String.valueOf(existing.getId()),
                "{\"employeeId\":\"" + existing.getEmployeeId() + "\"}",
                "{\"employeeId\":\"" + employee.getId() + "\"}");
        }

        TableAssignment assignment = new TableAssignment();
        assignment.setTenantId(tenantId);
        assignment.setBranchId(request.branchId());
        assignment.setTableId(request.tableId());
        assignment.setEmployeeId(employee.getId());
        assignment.setAssignedByUserId(actorUserId);
        assignment.setAssignedAt(Instant.now());
        assignment = assignmentRepository.save(assignment);

        auditService.record(tenantId, actorUserId, actorEmail, "TABLE_ASSIGNMENT_CREATED", "TableAssignment",
            String.valueOf(assignment.getId()), null,
            "{\"tableId\":\"" + request.tableId() + "\",\"employeeId\":\"" + employee.getId() + "\"}");
        return toResponse(assignment);
    }

    @Transactional
    public void release(Long tenantId, Long tableId, Long actorUserId, String actorEmail) {
        TableAssignment assignment = assignmentRepository
            .findByTenantIdAndTableIdAndIsActiveTrue(tenantId, tableId)
            .orElseThrow(() -> new NotFoundException("No active assignment for this table"));
        assignment.setIsActive(Boolean.FALSE);
        assignment.setReleasedAt(Instant.now());
        assignmentRepository.save(assignment);
        auditService.record(tenantId, actorUserId, actorEmail, "TABLE_ASSIGNMENT_RELEASED", "TableAssignment",
            String.valueOf(assignment.getId()),
            "{\"employeeId\":\"" + assignment.getEmployeeId() + "\"}", null);
    }

    private TableAssignmentResponse toResponse(TableAssignment assignment) {
        String name = employeeRepository.findById(assignment.getEmployeeId())
            .map(employee -> employee.getUserId() == null
                ? employee.getEmployeeCode()
                : userRepository.findById(employee.getUserId()).map(User::fullName).orElse(employee.getEmployeeCode()))
            .orElse("");
        var table = tableRepository.findById(assignment.getTableId()).orElse(null);
        return new TableAssignmentResponse(
            assignment.getId(),
            assignment.getTableId(),
            table == null ? null : table.getTableNumber(),
            assignment.getBranchId(),
            assignment.getEmployeeId(),
            name,
            null,
            assignment.getAssignedAt(),
            assignment.getReleasedAt());
    }
}
