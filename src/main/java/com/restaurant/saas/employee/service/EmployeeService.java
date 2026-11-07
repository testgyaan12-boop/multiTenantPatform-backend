package com.restaurant.saas.employee.service;

import com.restaurant.saas.audit.entity.AuditLog;
import com.restaurant.saas.audit.repository.AuditLogRepository;
import com.restaurant.saas.audit.service.AuditService;
import com.restaurant.saas.branch.entity.Branch;
import com.restaurant.saas.branch.repository.BranchRepository;
import com.restaurant.saas.common.exception.ConflictException;
import com.restaurant.saas.common.exception.ForbiddenException;
import com.restaurant.saas.common.exception.NotFoundException;
import com.restaurant.saas.employee.dto.*;
import com.restaurant.saas.employee.entity.Employee;
import com.restaurant.saas.employee.entity.StaffTask;
import com.restaurant.saas.employee.repository.EmployeeRepository;
import com.restaurant.saas.employee.repository.StaffTaskRepository;
import com.restaurant.saas.identity.entity.Role;
import com.restaurant.saas.identity.entity.User;
import com.restaurant.saas.identity.repository.RoleRepository;
import com.restaurant.saas.identity.repository.UserRepository;
import com.restaurant.saas.order.entity.OrderStatusHistory;
import com.restaurant.saas.order.repository.OrderStatusHistoryRepository;
import com.restaurant.saas.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private static final Set<String> ASSIGNABLE_ROLE_EXCLUDE = Set.of("SUPER_ADMIN", "CUSTOMER");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final EmployeeRepository employeeRepository;
    private final StaffTaskRepository taskRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final AuditService auditService;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final String frontendBaseUrl;
    private final Duration inviteTtl;

    public EmployeeService(
        EmployeeRepository employeeRepository,
        StaffTaskRepository taskRepository,
        UserRepository userRepository,
        RoleRepository roleRepository,
        BranchRepository branchRepository,
        OrderRepository orderRepository,
        OrderStatusHistoryRepository historyRepository,
        AuditService auditService,
        AuditLogRepository auditLogRepository,
        PasswordEncoder passwordEncoder,
        @Value("${app.frontend-base-url:http://localhost:5174}") String frontendBaseUrl,
        @Value("${app.invite-ttl-days:7}") long inviteTtlDays) {
        this.employeeRepository = employeeRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.branchRepository = branchRepository;
        this.orderRepository = orderRepository;
        this.historyRepository = historyRepository;
        this.auditService = auditService;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.frontendBaseUrl = frontendBaseUrl;
        this.inviteTtl = Duration.ofDays(inviteTtlDays);
    }

    public record PagedEmployees(List<EmployeeResponse> content, long totalElements) {
    }

    // ------------------------------------------------------------------ list

    @Transactional(readOnly = true)
    public PagedEmployees list(Long tenantId, Long branchId, String search, String status,
                                String role, Boolean onDuty, Pageable pageable) {
        Specification<Employee> spec = (root, query, cb) -> cb.and(
            cb.equal(root.get("tenantId"), tenantId),
            branchId == null ? cb.isNotNull(root.get("id")) : cb.equal(root.get("branchId"), branchId),
            status == null || status.isBlank() ? cb.isNotNull(root.get("id")) : cb.equal(root.get("status"), status),
            onDuty == null ? cb.isNotNull(root.get("id")) : cb.equal(root.get("onDuty"), onDuty)
        );
        Page<Employee> page = employeeRepository.findAll(spec, pageable);
        Map<Long, User> users = loadUsers(page.getContent());
        Map<Long, Branch> branches = loadBranches(page.getContent());
        List<EmployeeResponse> content = page.getContent().stream()
            .map(employee -> toResponse(employee, users, branches, role, search, false))
            .filter(Objects::nonNull)
            .toList();
        return new PagedEmployees(content, page.getTotalElements());
    }

    // ----------------------------------------------------------------- stats

    @Transactional(readOnly = true)
    public EmployeeStatsResponse stats(Long tenantId, Long branchId) {
        List<Employee> employees = branchId == null
            ? employeeRepository.findByTenantId(tenantId)
            : employeeRepository.findByTenantIdAndBranchId(tenantId, branchId);
        Map<Long, User> users = loadUsers(employees);
        long total = 0;
        long onDuty = 0;
        Map<String, Long> byRole = new TreeMap<>();
        for (Employee employee : employees) {
            if (!"ACTIVE".equals(employee.getStatus())) continue;
            total++;
            if (Boolean.TRUE.equals(employee.getOnDuty())) onDuty++;
            User user = employee.getUserId() == null ? null : users.get(employee.getUserId());
            if (user != null) {
                for (Role role : user.getRoles()) {
                    byRole.merge(role.getCode(), 1L, Long::sum);
                }
            }
        }
        return new EmployeeStatsResponse(total, total, onDuty, byRole);
    }

    // ----------------------------------------------------------------- roles

    @Transactional(readOnly = true)
    public List<AssignableRoleResponse> assignableRoles() {
        return roleRepository.findAll().stream()
            .filter(Role::getSystemRole)
            .filter(role -> role.getTenantId() == null)
            .filter(role -> !ASSIGNABLE_ROLE_EXCLUDE.contains(role.getCode()))
            .sorted(Comparator.comparing(Role::getCode))
            .map(role -> new AssignableRoleResponse(role.getCode(), role.getName(), role.getDescription()))
            .toList();
    }

    // ---------------------------------------------------------------- create

    @Transactional
    public EmployeeResponse create(Long tenantId, Long actorId, String actorEmail, EmployeeCreateRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("email_taken", "An account with this email already exists.");
        }
        Role role = roleRepository.findSystemRoleWithPermissions(request.roleCode().trim())
            .orElseThrow(() -> new NotFoundException("Role", request.roleCode()));
        if (ASSIGNABLE_ROLE_EXCLUDE.contains(role.getCode())) {
            throw new ForbiddenException("This role cannot be assigned to employees.");
        }
        Long branchId = resolveBranch(tenantId, request.branchId());

        User user = new User();
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        if (request.phone() != null && !request.phone().isBlank()) {
            user.setPhone(request.phone().trim());
        }
        user.setPasswordHash(passwordEncoder.encode(randomToken()));
        user.setStatus("INVITED");
        user.setEmailVerified(Boolean.FALSE);
        String inviteToken = randomToken();
        user.setInviteToken(inviteToken);
        user.setInviteExpiresAt(Instant.now().plus(inviteTtl));
        user.getRoles().add(role);
        userRepository.save(user);

        Employee employee = new Employee();
        employee.setTenantId(tenantId);
        employee.setBranchId(branchId);
        employee.setUserId(user.getId());
        employee.setEmployeeCode(uniqueCode(tenantId, request.employeeCode()));
        employee.setJobTitle(request.jobTitle() != null && !request.jobTitle().isBlank()
            ? request.jobTitle().trim() : role.getName());
        employee.setHiredAt(request.hiredAt() != null ? request.hiredAt() : LocalDate.now(ZoneOffset.UTC));
        employee.setShiftName(request.shiftName());
        employee.setStatus("ACTIVE");
        employee.setOnDuty(Boolean.FALSE);
        employee = employeeRepository.save(employee);

        auditService.record(tenantId, actorId, actorEmail, "EMPLOYEE_INVITED", "Employee",
            String.valueOf(employee.getId()), null,
            "{\"email\":\"" + email + "\",\"role\":\"" + role.getCode() + "\"}");

        return toResponse(employee, Map.of(user.getId(), user), loadBranches(List.of(employee)), null, null, true);
    }

    // ---------------------------------------------------------------- update

    @Transactional
    public EmployeeResponse update(Long tenantId, Long id, Long actorId, String actorEmail,
                                   EmployeeUpdateRequest request) {
        Employee employee = employeeRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new NotFoundException("Employee", id));
        User user = userRepository.findActiveByIdWithAuthorities(employee.getUserId())
            .orElseThrow(() -> new NotFoundException("Employee account", employee.getUserId()));

        Map<String, Object> before = new LinkedHashMap<>();
        Map<String, Object> after = new LinkedHashMap<>();

        if (request.firstName() != null && !request.firstName().isBlank()) {
            before.put("firstName", user.getFirstName());
            user.setFirstName(request.firstName().trim());
            after.put("firstName", user.getFirstName());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            before.put("lastName", user.getLastName());
            user.setLastName(request.lastName().trim());
            after.put("lastName", user.getLastName());
        }
        if (request.phone() != null) {
            before.put("phone", user.getPhone());
            user.setPhone(request.phone().isBlank() ? null : request.phone().trim());
            after.put("phone", user.getPhone());
        }
        if (request.roleCode() != null && !request.roleCode().isBlank()) {
            String current = user.getRoles().stream().map(Role::getCode).sorted().collect(Collectors.joining(","));
            if (!current.equals(request.roleCode())) {
                Role role = roleRepository.findSystemRoleWithPermissions(request.roleCode().trim())
                    .orElseThrow(() -> new NotFoundException("Role", request.roleCode()));
                if (ASSIGNABLE_ROLE_EXCLUDE.contains(role.getCode())) {
                    throw new ForbiddenException("This role cannot be assigned to employees.");
                }
                before.put("role", current);
                user.setRoles(new HashSet<>(Set.of(role)));
                after.put("role", role.getCode());
            }
        }
        if (request.branchId() != null) {
            Long branchId = resolveBranch(tenantId, request.branchId());
            if (!branchId.equals(employee.getBranchId())) {
                before.put("branchId", employee.getBranchId());
                employee.setBranchId(branchId);
                after.put("branchId", branchId);
            }
        }
        if (request.jobTitle() != null) {
            before.put("jobTitle", employee.getJobTitle());
            employee.setJobTitle(request.jobTitle().isBlank() ? null : request.jobTitle().trim());
            after.put("jobTitle", employee.getJobTitle());
        }
        if (request.hiredAt() != null) {
            before.put("hiredAt", employee.getHiredAt());
            employee.setHiredAt(request.hiredAt());
            after.put("hiredAt", employee.getHiredAt());
        }
        if (request.shiftName() != null) {
            before.put("shiftName", employee.getShiftName());
            employee.setShiftName(request.shiftName().isBlank() ? null : request.shiftName().trim());
            after.put("shiftName", employee.getShiftName());
        }
        if (request.status() != null && !request.status().isBlank()) {
            String status = request.status().trim().toUpperCase();
            if (!status.equals("ACTIVE") && !status.equals("INACTIVE")) {
                throw new ForbiddenException("Employee status must be ACTIVE or INACTIVE.");
            }
            if (!status.equals(employee.getStatus())) {
                before.put("status", employee.getStatus());
                employee.setStatus(status);
                after.put("status", status);
                if ("INACTIVE".equals(status)) {
                    user.setStatus("DISABLED");
                    employee.setOnDuty(Boolean.FALSE);
                    employee.setOnDutySince(null);
                } else if (!"INVITED".equals(user.getStatus())) {
                    user.setStatus("ACTIVE");
                }
            }
        }

        if (!after.isEmpty()) {
            auditService.record(tenantId, actorId, actorEmail, "EMPLOYEE_UPDATED", "Employee",
                String.valueOf(employee.getId()), json(before), json(after));
        }
        userRepository.save(user);
        employeeRepository.save(employee);

        return toResponse(employee, Map.of(user.getId(), user), loadBranches(List.of(employee)), null, null, false);
    }

    // ------------------------------------------------------------------ duty

    @Transactional
    public EmployeeResponse setDuty(Long tenantId, Long id, Long actorId, String actorEmail, boolean onDuty) {
        Employee employee = employeeRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new NotFoundException("Employee", id));
        if (!"ACTIVE".equals(employee.getStatus()) && onDuty) {
            throw new ForbiddenException("Inactive employees cannot clock in.");
        }
        employee.setOnDuty(onDuty);
        employee.setOnDutySince(onDuty ? Instant.now() : null);
        employeeRepository.save(employee);
        auditService.record(tenantId, actorId, actorEmail, onDuty ? "DUTY_START" : "DUTY_END", "Employee",
            String.valueOf(employee.getId()), null, onDuty ? "{\"onDuty\":true}" : "{\"onDuty\":false}");
        return toResponse(employee, loadUsers(List.of(employee)), loadBranches(List.of(employee)), null, null, false);
    }

    // ----------------------------------------------------------- performance

    @Transactional(readOnly = true)
    public EmployeePerformanceResponse performance(Long tenantId, Long id, LocalDate from, LocalDate to) {
        Employee employee = employeeRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new NotFoundException("Employee", id));
        Instant fromInstant = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toInstant = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<StaffTask> tasks = taskRepository.findByTenantIdAndBranchIdAndAssignedToEmployeeIdAndStatusIn(
            tenantId, employee.getBranchId(), employee.getId(),
            List.of("ASSIGNED", "ACCEPTED", "IN_PROGRESS", "BLOCKED", "COMPLETED", "CANCELLED"));
        long assigned = tasks.stream()
            .filter(t -> inRange(t.getCreatedAt(), fromInstant, toInstant))
            .count();
        List<StaffTask> completedInRange = tasks.stream()
            .filter(t -> t.getCompletedAt() != null && inRange(t.getCompletedAt(), fromInstant, toInstant))
            .toList();
        long completed = completedInRange.size();
        Double rate = (assigned == 0 && completed == 0)
            ? null
            : Math.round((completed * 100.0) / Math.max(1, assigned + completed)) / 10.0;
        Double avgMinutes = completedInRange.stream()
            .filter(t -> t.getCreatedAt() != null && t.getCompletedAt() != null)
            .map(t -> Duration.between(t.getCreatedAt(), t.getCompletedAt()).toMinutes())
            .reduce(Long::sum)
            .map(sum -> sum / (double) Math.max(1, completedInRange.size()))
            .orElse(null);

        long tablesServed = completedInRange.stream()
            .filter(t -> "SERVE_TABLE".equals(t.getTaskType()) && t.getTableId() != null)
            .map(StaffTask::getTableId)
            .distinct()
            .count();

        Long ordersHandled = null;
        Double confirmationMinutes = null;
        User user = employee.getUserId() == null ? null
            : userRepository.findById(employee.getUserId()).orElse(null);
        if (user != null) {
            List<OrderStatusHistory> events = historyRepository.findAll().stream()
                .filter(h -> tenantId.equals(h.getTenantId()))
                .filter(h -> user.getEmail().equals(h.getChangedBy()))
                .filter(h -> inRange(h.getCreatedAt(), fromInstant, toInstant))
                .toList();
            ordersHandled = events.stream().map(OrderStatusHistory::getOrderId).distinct().count();
            List<Long> confirmedOrderIds = events.stream()
                .filter(h -> "CONFIRMED".equals(h.getToStatus()))
                .map(OrderStatusHistory::getOrderId)
                .distinct()
                .toList();
            if (!confirmedOrderIds.isEmpty()) {
                List<Long> deltas = orderRepository.findAllById(confirmedOrderIds).stream()
                    .filter(o -> o.getPlacedAt() != null && o.getAcceptedAt() != null)
                    .map(o -> Duration.between(o.getPlacedAt(), o.getAcceptedAt()).toMinutes())
                    .toList();
                if (!deltas.isEmpty()) {
                    confirmationMinutes = deltas.stream().mapToLong(Long::longValue).average().orElse(0);
                }
            }
        }

        Map<String, long[]> daily = new TreeMap<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            daily.put(day.format(ISO_DATE), new long[]{0, 0});
        }
        for (StaffTask task : tasks) {
            if (task.getCreatedAt() != null && inRange(task.getCreatedAt(), fromInstant, toInstant)) {
                String key = LocalDate.ofInstant(task.getCreatedAt(), ZoneOffset.UTC).format(ISO_DATE);
                long[] bucket = daily.get(key);
                if (bucket != null) bucket[0]++;
            }
            if (task.getCompletedAt() != null && inRange(task.getCompletedAt(), fromInstant, toInstant)) {
                String key = LocalDate.ofInstant(task.getCompletedAt(), ZoneOffset.UTC).format(ISO_DATE);
                long[] bucket = daily.get(key);
                if (bucket != null) bucket[1]++;
            }
        }
        List<EmployeePerformanceResponse.DailyPoint> trend = daily.entrySet().stream()
            .map(entry -> new EmployeePerformanceResponse.DailyPoint(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
            .toList();

        return new EmployeePerformanceResponse(
            from.format(ISO_DATE), to.format(ISO_DATE),
            assigned, completed, rate, avgMinutes,
            ordersHandled, confirmationMinutes,
            tablesServed, false, trend);
    }

    // ---------------------------------------------------------------- history

    @Transactional(readOnly = true)
    public List<AuditHistoryResponse> history(Long tenantId, Long id) {
        Employee employee = employeeRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new NotFoundException("Employee", id));
        return auditLogRepository.findByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
                tenantId, "Employee", String.valueOf(employee.getId()))
            .stream()
            .limit(50)
            .map(log -> new AuditHistoryResponse(log.getId(), log.getActorEmail(), log.getAction(),
                log.getEntityType(), log.getEntityId(), log.getCreatedAt()))
            .toList();
    }

    // ---------------------------------------------------------------- get

    @Transactional(readOnly = true)
    public EmployeeResponse get(Long tenantId, Long id) {
        Employee employee = employeeRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new NotFoundException("Employee", id));
        return toResponse(employee, loadUsers(List.of(employee)), loadBranches(List.of(employee)), null, null, false);
    }

    // ---------------------------------------------------------------- helpers

    private EmployeeResponse toResponse(Employee employee, Map<Long, User> users, Map<Long, Branch> branches,
                                        String roleFilter, String search, boolean includeInvite) {
        User user = employee.getUserId() == null ? null : users.get(employee.getUserId());
        if (user == null) return null;
        List<String> roles = user.getRoles().stream().map(Role::getCode).sorted().toList();
        if (roleFilter != null && !roleFilter.isBlank()
            && roles.stream().noneMatch(r -> r.equalsIgnoreCase(roleFilter))) {
            return null;
        }
        if (search != null && !search.isBlank()) {
            String term = search.trim().toLowerCase();
            boolean match = user.getEmail().toLowerCase().contains(term)
                || user.fullName().toLowerCase().contains(term)
                || employee.getEmployeeCode().toLowerCase().contains(term);
            if (!match) return null;
        }
        Branch branch = branches.get(employee.getBranchId());
        String inviteUrl = null;
        if (includeInvite && user.getInviteToken() != null) {
            inviteUrl = frontendBaseUrl + "/accept-invite?token=" + user.getInviteToken();
        }
        return new EmployeeResponse(
            employee.getId(),
            employee.getUserId(),
            employee.getEmployeeCode(),
            user.getFirstName(),
            user.getLastName(),
            user.fullName(),
            user.getEmail(),
            user.getPhone(),
            employee.getBranchId(),
            branch == null ? null : branch.getName(),
            roles,
            employee.getJobTitle(),
            employee.getStatus(),
            user.getStatus(),
            employee.getShiftName(),
            Boolean.TRUE.equals(employee.getOnDuty()),
            employee.getOnDutySince(),
            employee.getHiredAt(),
            user.getLastLoginAt(),
            inviteUrl
        );
    }

    private Map<Long, User> loadUsers(List<Employee> employees) {
        List<Long> userIds = employees.stream()
            .map(Employee::getUserId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (userIds.isEmpty()) return Map.of();
        return userRepository.findAllByIdWithAuthorities(userIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
    }

    private Map<Long, Branch> loadBranches(List<Employee> employees) {
        List<Long> branchIds = employees.stream().map(Employee::getBranchId).distinct().toList();
        if (branchIds.isEmpty()) return Map.of();
        return branchRepository.findAllById(branchIds).stream()
            .collect(Collectors.toMap(Branch::getId, b -> b, (a, b) -> a));
    }

    private Long resolveBranch(Long tenantId, Long branchId) {
        Long resolved = branchId != null
            ? branchId
            : branchRepository.findByTenantIdOrderByNameAsc(tenantId).stream()
                .findFirst().map(Branch::getId)
                .orElseThrow(() -> new NotFoundException("No branches found for this restaurant"));
        Branch branch = branchRepository.findById(resolved)
            .orElseThrow(() -> new NotFoundException("Branch", resolved));
        if (!tenantId.equals(branch.getTenantId())) {
            throw new ForbiddenException("Branch does not belong to your restaurant.");
        }
        return branch.getId();
    }

    private String uniqueCode(Long tenantId, String requested) {
        String base = (requested == null || requested.isBlank())
            ? "EMP" : requested.trim().toUpperCase().replaceAll("[^A-Z0-9-]", "");
        String candidate = base;
        int suffix = 1;
        while (employeeRepository.findByTenantIdAndEmployeeCode(tenantId, candidate).isPresent()) {
            suffix++;
            candidate = base + "-" + suffix;
        }
        return candidate;
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static boolean inRange(Instant value, Instant from, Instant to) {
        return value != null && !value.isBefore(from) && value.isBefore(to);
    }

    private static String json(Map<String, Object> map) {
        if (map.isEmpty()) return null;
        return map.entrySet().stream()
            .map(e -> "\"" + e.getKey() + "\":\"" + e.getValue() + "\"")
            .collect(Collectors.joining(",", "{", "}"));
    }
}
