package com.restaurant.saas.employee.service;

import com.restaurant.saas.audit.service.AuditService;
import com.restaurant.saas.branch.repository.BranchRepository;
import com.restaurant.saas.common.exception.ConflictException;
import com.restaurant.saas.common.exception.ForbiddenException;
import com.restaurant.saas.common.exception.NotFoundException;
import com.restaurant.saas.employee.dto.StaffTaskRequest;
import com.restaurant.saas.employee.dto.StaffTaskResponse;
import com.restaurant.saas.employee.entity.Employee;
import com.restaurant.saas.employee.entity.StaffTask;
import com.restaurant.saas.employee.repository.EmployeeRepository;
import com.restaurant.saas.employee.repository.StaffTaskRepository;
import com.restaurant.saas.identity.entity.User;
import com.restaurant.saas.identity.repository.UserRepository;
import com.restaurant.saas.order.repository.OrderRepository;
import com.restaurant.saas.diningtable.repository.DiningTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class StaffTaskService {

    public static final Set<String> TASK_TYPES = Set.of(
        "SERVE_TABLE", "CLEAN_TABLE", "PREPARE_ORDER", "CONFIRM_ORDER", "RESTOCK_SUPPLIES",
        "KITCHEN_PREPARATION", "CUSTOMER_ASSISTANCE", "OPENING_CHECKLIST", "CLOSING_CHECKLIST", "CUSTOM_TASK");
    public static final Set<String> PRIORITIES = Set.of("LOW", "NORMAL", "HIGH", "URGENT");
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
        "ASSIGNED", Set.of("ACCEPTED", "IN_PROGRESS", "BLOCKED", "CANCELLED"),
        "ACCEPTED", Set.of("IN_PROGRESS", "BLOCKED", "CANCELLED"),
        "IN_PROGRESS", Set.of("BLOCKED", "COMPLETED", "CANCELLED"),
        "BLOCKED", Set.of("IN_PROGRESS", "COMPLETED", "CANCELLED"),
        "COMPLETED", Set.of(),
        "CANCELLED", Set.of());
    private static final int MAX_CONCURRENT_ACTIVE = 8;
    private static final List<String> ACTIVE_STATUSES = List.of("ASSIGNED", "ACCEPTED", "IN_PROGRESS", "BLOCKED");

    private final StaffTaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final DiningTableRepository tableRepository;
    private final OrderRepository orderRepository;
    private final AuditService auditService;

    public StaffTaskService(
        StaffTaskRepository taskRepository,
        EmployeeRepository employeeRepository,
        UserRepository userRepository,
        BranchRepository branchRepository,
        DiningTableRepository tableRepository,
        OrderRepository orderRepository,
        AuditService auditService) {
        this.taskRepository = taskRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.tableRepository = tableRepository;
        this.orderRepository = orderRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<StaffTaskResponse> list(Long tenantId, Long branchId, Long employeeId, String status,
                                        String priority, String taskType, Long tableId,
                                        Instant from, Instant to) {
        List<StaffTask> tasks = taskRepository.findAll((root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            if (branchId != null) predicates.add(cb.equal(root.get("branchId"), branchId));
            if (employeeId != null) predicates.add(cb.equal(root.get("assignedToEmployeeId"), employeeId));
            if (status != null && !status.isBlank()) {
                List<String> statuses = Arrays.asList(status.split(","));
                predicates.add(root.get("status").in(statuses));
            }
            if (priority != null && !priority.isBlank()) predicates.add(cb.equal(root.get("priority"), priority));
            if (taskType != null && !taskType.isBlank()) predicates.add(cb.equal(root.get("taskType"), taskType));
            if (tableId != null) predicates.add(cb.equal(root.get("tableId"), tableId));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null) predicates.add(cb.lessThan(root.get("createdAt"), to));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });

        Map<Long, Employee> employees = new HashMap<>();
        Map<Long, String> employeeNames = new HashMap<>();
        Map<Long, String> employeeRoles = new HashMap<>();
        List<Long> employeeIds = tasks.stream().map(StaffTask::getAssignedToEmployeeId)
            .filter(Objects::nonNull).distinct().toList();
        if (!employeeIds.isEmpty()) {
            employeeRepository.findAllById(employeeIds).forEach(e -> employees.put(e.getId(), e));
            List<User> users = userRepository.findAllByIdWithAuthorities(
                employeeIds.stream().map(id -> employees.get(id).getUserId()).filter(Objects::nonNull).toList());
            Map<Long, User> userMap = new HashMap<>();
            users.forEach(u -> userMap.put(u.getId(), u));
            employees.forEach((id, employee) -> {
                User user = employee.getUserId() == null ? null : userMap.get(employee.getUserId());
                if (user != null) {
                    employeeNames.put(id, user.fullName());
                    employeeRoles.put(id, user.getRoles().stream()
                        .map(r -> r.getName()).sorted().findFirst().orElse(""));
                }
            });
        }

        Map<Long, String> tableNumbers = new HashMap<>();
        List<Long> tableIds = tasks.stream().map(StaffTask::getTableId).filter(Objects::nonNull).distinct().toList();
        if (!tableIds.isEmpty()) {
            tableRepository.findAllById(tableIds).forEach(t -> tableNumbers.put(t.getId(), t.getTableNumber()));
        }
        Map<Long, String> orderNumbers = new HashMap<>();
        List<Long> orderIds = tasks.stream().map(StaffTask::getOrderId).filter(Objects::nonNull).distinct().toList();
        if (!orderIds.isEmpty()) {
            orderRepository.findAllById(orderIds).forEach(o -> orderNumbers.put(o.getId(), o.getOrderNumber()));
        }
        Map<Long, String> branchNames = new HashMap<>();
        List<Long> branchIds = tasks.stream().map(StaffTask::getBranchId).distinct().toList();
        branchRepository.findAllById(branchIds).forEach(b -> branchNames.put(b.getId(), b.getName()));

        Instant now = Instant.now();
        return tasks.stream()
            .sorted(Comparator
                .comparing((StaffTask t) -> t.getDueAt() == null ? Instant.MAX : t.getDueAt())
                .thenComparing(t -> t.getCreatedAt() == null ? Instant.MIN : t.getCreatedAt()))
            .map(task -> toResponse(task, employeeNames, employeeRoles, tableNumbers, orderNumbers, branchNames, now))
            .toList();
    }

    @Transactional
    public StaffTaskResponse create(Long tenantId, Long actorUserId, String actorEmail, StaffTaskRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new ForbiddenException("Task title is required.");
        }
        String type = request.taskType() == null || request.taskType().isBlank()
            ? "CUSTOM_TASK" : request.taskType().trim();
        if (!TASK_TYPES.contains(type)) {
            throw new ForbiddenException("Unsupported task type: " + type);
        }
        String priority = request.priority() == null || request.priority().isBlank()
            ? "NORMAL" : request.priority().trim().toUpperCase();
        if (!PRIORITIES.contains(priority)) {
            throw new ForbiddenException("Unsupported priority: " + priority);
        }
        Long branchId = request.branchId();
        if (branchId == null) {
            throw new ForbiddenException("Branch is required for a task.");
        }
        branchRepository.findByIdAndTenantId(branchId, tenantId)
            .orElseThrow(() -> new NotFoundException("Branch", branchId));

        Long employeeId = request.assignedToEmployeeId();
        if (employeeId == null) {
            throw new ForbiddenException("An employee must be assigned to the task.");
        }
        Employee employee = employeeRepository.findByIdAndTenantId(employeeId, tenantId)
            .orElseThrow(() -> new NotFoundException("Employee", employeeId));
        if (!employee.getBranchId().equals(branchId)) {
            throw new ForbiddenException("The assigned employee does not work in this branch.");
        }
        if (!"ACTIVE".equals(employee.getStatus())) {
            throw new ForbiddenException("Cannot assign tasks to an inactive employee.");
        }
        List<StaffTask> active = taskRepository.findActiveForEmployee(tenantId, branchId, employeeId, -1L);
        if (active.size() >= MAX_CONCURRENT_ACTIVE) {
            throw new ConflictException("too_many_tasks",
                "This employee already has " + active.size() + " active tasks. Reassign or complete some first.");
        }

        if (request.tableId() != null) {
            var table = tableRepository.findById(request.tableId())
                .orElseThrow(() -> new NotFoundException("Table", request.tableId()));
            if (!tenantId.equals(table.getTenantId()) || !branchId.equals(table.getBranchId())) {
                throw new ForbiddenException("Table does not belong to this branch.");
            }
        }
        if (request.orderId() != null) {
            var order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new NotFoundException("Order", request.orderId()));
            if (!tenantId.equals(order.getTenantId())) {
                throw new ForbiddenException("Order does not belong to your restaurant.");
            }
        }

        StaffTask task = new StaffTask();
        task.setTenantId(tenantId);
        task.setBranchId(branchId);
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setTaskType(type);
        task.setPriority(priority);
        task.setStatus("ASSIGNED");
        task.setAssignedToEmployeeId(employeeId);
        task.setCreatedByUserId(actorUserId);
        task.setTableId(request.tableId());
        task.setOrderId(request.orderId());
        task.setDueAt(request.dueAt());
        task.setInstructions(request.instructions());
        task = taskRepository.save(task);

        auditService.record(tenantId, actorUserId, actorEmail, "TASK_ASSIGNED", "Task",
            String.valueOf(task.getId()), null,
            "{\"employeeId\":\"" + employeeId + "\",\"title\":\"" + task.getTitle() + "\"}");

        return toResponse(task, Map.of(employeeId, safeName(employee)), Map.of(employeeId, safeRole(employee)),
            tableNumbersOf(task), orderNumbersOf(task), branchNamesOf(task), Instant.now());
    }

    @Transactional
    public StaffTaskResponse update(Long tenantId, Long id, Long actorUserId, String actorEmail,
                                    StaffTaskRequest request) {
        StaffTask task = taskRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new NotFoundException("Task", id));
        if (COMPLETED.equals(task.getStatus()) || CANCELLED.equals(task.getStatus())) {
            throw new ConflictException("task_closed", "Completed or cancelled tasks cannot be edited.");
        }
        Map<String, Object> before = new LinkedHashMap<>();
        Map<String, Object> after = new LinkedHashMap<>();

        if (request.title() != null && !request.title().isBlank()) {
            before.put("title", task.getTitle());
            task.setTitle(request.title().trim());
            after.put("title", task.getTitle());
        }
        if (request.description() != null) task.setDescription(request.description());
        if (request.priority() != null && !request.priority().isBlank()) {
            String priority = request.priority().toUpperCase();
            if (!PRIORITIES.contains(priority)) throw new ForbiddenException("Unsupported priority.");
            before.put("priority", task.getPriority());
            task.setPriority(priority);
            after.put("priority", priority);
        }
        if (request.dueAt() != null) {
            before.put("dueAt", task.getDueAt());
            task.setDueAt(request.dueAt());
            after.put("dueAt", task.getDueAt());
        }
        if (request.instructions() != null) task.setInstructions(request.instructions());
        if (request.tableId() != null) {
            var table = tableRepository.findById(request.tableId())
                .orElseThrow(() -> new NotFoundException("Table", request.tableId()));
            if (!tenantId.equals(table.getTenantId())) throw new ForbiddenException("Table not in your restaurant.");
            task.setTableId(request.tableId());
        }
        if (request.assignedToEmployeeId() != null
            && !request.assignedToEmployeeId().equals(task.getAssignedToEmployeeId())) {
            Employee employee = employeeRepository.findByIdAndTenantId(request.assignedToEmployeeId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Employee", request.assignedToEmployeeId()));
            if (!employee.getBranchId().equals(task.getBranchId())) {
                throw new ForbiddenException("The employee does not work in this branch.");
            }
            before.put("assignedToEmployeeId", task.getAssignedToEmployeeId());
            task.setAssignedToEmployeeId(employee.getId());
            after.put("assignedToEmployeeId", employee.getId());
            auditService.record(tenantId, actorUserId, actorEmail, "TASK_REASSIGNED", "Task",
                String.valueOf(task.getId()), json(before), json(after));
        }
        taskRepository.save(task);
        if (!after.isEmpty() && !before.containsKey("assignedToEmployeeId")) {
            auditService.record(tenantId, actorUserId, actorEmail, "TASK_UPDATED", "Task",
                String.valueOf(task.getId()), json(before), json(after));
        }
        return reloadResponse(tenantId, task);
    }

    private static final String COMPLETED = "COMPLETED";
    private static final String CANCELLED = "CANCELLED";

    @Transactional
    public StaffTaskResponse updateStatus(Long tenantId, Long id, String newStatus, String note,
                                          Long actorUserId, String actorEmail, boolean canManage,
                                          String actorEmailLogin) {
        StaffTask task = taskRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new NotFoundException("Task", id));
        String status = newStatus == null ? "" : newStatus.trim().toUpperCase();
        if (!TRANSITIONS.containsKey(status)) {
            throw new ForbiddenException("Unsupported task status: " + status);
        }
        if (!canManage) {
            Employee self = employeeRepository.findByUserId(actorUserId)
                .orElseThrow(() -> new ForbiddenException("Only assigned employees can update this task."));
            if (!self.getId().equals(task.getAssignedToEmployeeId())) {
                throw new ForbiddenException("You can only update tasks assigned to you.");
            }
        }
        Set<String> allowed = TRANSITIONS.getOrDefault(task.getStatus(), Set.of());
        if (!allowed.contains(status)) {
            throw new ConflictException("invalid_transition",
                "A task cannot move from " + task.getStatus() + " to " + status + ".");
        }
        String before = task.getStatus();
        task.setStatus(status);
        if (note != null && !note.isBlank()) task.setStatusNote(note.trim());
        Instant now = Instant.now();
        switch (status) {
            case "ACCEPTED" -> task.setAcceptedAt(now);
            case "IN_PROGRESS" -> { if (task.getStartedAt() == null) task.setStartedAt(now); }
            case "COMPLETED" -> task.setCompletedAt(now);
            case "CANCELLED" -> task.setCancelledAt(now);
            default -> { }
        }
        taskRepository.save(task);
        auditService.record(tenantId, actorUserId, actorEmail, "TASK_STATUS_CHANGED", "Task",
            String.valueOf(task.getId()), "{\"status\":\"" + before + "\"}", "{\"status\":\"" + status + "\"}");
        return reloadResponse(tenantId, task);
    }

    @Transactional(readOnly = true)
    public List<StaffTaskResponse> overdue(Long tenantId) {
        return taskRepository.findOverdue(tenantId, Instant.now()).stream()
            .map(task -> reloadResponse(tenantId, task))
            .toList();
    }

    // ---------------------------------------------------------------- helpers

    private StaffTaskResponse reloadResponse(Long tenantId, StaffTask task) {
        Map<Long, String> names = new HashMap<>();
        Map<Long, String> roles = new HashMap<>();
        if (task.getAssignedToEmployeeId() != null) {
            employeeRepository.findById(task.getAssignedToEmployeeId()).ifPresent(employee -> {
                names.put(employee.getId(), safeName(employee));
                roles.put(employee.getId(), safeRole(employee));
            });
        }
        return toResponse(task, names, roles, tableNumbersOf(task), orderNumbersOf(task),
            branchNamesOf(task), Instant.now());
    }

    private String safeName(Employee employee) {
        if (employee.getUserId() == null) return employee.getEmployeeCode();
        return userRepository.findById(employee.getUserId())
            .map(User::fullName).orElse(employee.getEmployeeCode());
    }

    private String safeRole(Employee employee) {
        if (employee.getUserId() == null) return "";
        return userRepository.findActiveByIdWithAuthorities(employee.getUserId())
            .map(u -> u.getRoles().stream().map(r -> r.getName()).sorted().findFirst().orElse(""))
            .orElse("");
    }

    private Map<Long, String> tableNumbersOf(StaffTask task) {
        if (task.getTableId() == null) return Map.of();
        return tableRepository.findById(task.getTableId())
            .map(t -> Map.of(t.getId(), t.getTableNumber())).orElse(Map.of());
    }

    private Map<Long, String> orderNumbersOf(StaffTask task) {
        if (task.getOrderId() == null) return Map.of();
        return orderRepository.findById(task.getOrderId())
            .map(o -> Map.of(o.getId(), o.getOrderNumber())).orElse(Map.of());
    }

    private Map<Long, String> branchNamesOf(StaffTask task) {
        return branchRepository.findById(task.getBranchId())
            .map(b -> Map.of(b.getId(), b.getName())).orElse(Map.of());
    }

    private StaffTaskResponse toResponse(StaffTask task, Map<Long, String> employeeNames,
                                         Map<Long, String> employeeRoles, Map<Long, String> tableNumbers,
                                         Map<Long, String> orderNumbers, Map<Long, String> branchNames,
                                         Instant now) {
        boolean overdue = task.getDueAt() != null
            && ACTIVE_STATUSES.contains(task.getStatus())
            && task.getDueAt().isBefore(now);
        return new StaffTaskResponse(
            task.getId(),
            task.getBranchId(),
            branchNames.get(task.getBranchId()),
            task.getTitle(),
            task.getDescription(),
            task.getTaskType(),
            task.getPriority(),
            task.getStatus(),
            task.getAssignedToEmployeeId(),
            task.getAssignedToEmployeeId() == null ? null : employeeNames.get(task.getAssignedToEmployeeId()),
            task.getAssignedToEmployeeId() == null ? null : employeeRoles.get(task.getAssignedToEmployeeId()),
            task.getTableId(),
            task.getTableId() == null ? null : tableNumbers.get(task.getTableId()),
            task.getOrderId(),
            task.getOrderId() == null ? null : orderNumbers.get(task.getOrderId()),
            task.getDueAt(),
            task.getAcceptedAt(),
            task.getStartedAt(),
            task.getCompletedAt(),
            task.getCancelledAt(),
            task.getInstructions(),
            task.getStatusNote(),
            task.getCreatedAt(),
            task.getUpdatedAt(),
            overdue
        );
    }

    private static String json(Map<String, Object> map) {
        if (map.isEmpty()) return null;
        return map.entrySet().stream()
            .map(e -> "\"" + e.getKey() + "\":\"" + e.getValue() + "\"")
            .collect(java.util.stream.Collectors.joining(",", "{", "}"));
    }
}
