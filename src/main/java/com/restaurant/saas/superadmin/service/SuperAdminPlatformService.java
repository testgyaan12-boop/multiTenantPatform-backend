package com.restaurant.saas.superadmin.service;

import com.restaurant.saas.audit.entity.AuditLog;
import com.restaurant.saas.audit.repository.AuditLogRepository;
import com.restaurant.saas.audit.service.AuditService;
import com.restaurant.saas.branch.entity.Branch;
import com.restaurant.saas.branch.repository.BranchRepository;
import com.restaurant.saas.common.exception.ApiException;
import com.restaurant.saas.common.exception.ConflictException;
import com.restaurant.saas.common.exception.NotFoundException;
import com.restaurant.saas.authorization.security.AuthenticatedPrincipal;
import com.restaurant.saas.common.web.PageResponse;
import com.restaurant.saas.identity.entity.Permission;
import com.restaurant.saas.identity.entity.Role;
import com.restaurant.saas.identity.entity.User;
import com.restaurant.saas.identity.repository.PermissionRepository;
import com.restaurant.saas.identity.repository.RoleRepository;
import com.restaurant.saas.identity.repository.UserRepository;
import com.restaurant.saas.notification.entity.Notification;
import com.restaurant.saas.notification.repository.NotificationRepository;
import com.restaurant.saas.superadmin.entity.Coupon;
import com.restaurant.saas.superadmin.entity.FeatureFlag;
import com.restaurant.saas.superadmin.entity.PlatformSetting;
import com.restaurant.saas.superadmin.repository.CouponRepository;
import com.restaurant.saas.superadmin.repository.FeatureFlagRepository;
import com.restaurant.saas.superadmin.repository.PlatformSettingRepository;
import com.restaurant.saas.subscription.entity.Subscription;
import com.restaurant.saas.subscription.entity.SubscriptionInvoice;
import com.restaurant.saas.subscription.entity.SubscriptionPlan;
import com.restaurant.saas.subscription.entity.PlanPriceHistory;
import com.restaurant.saas.subscription.repository.PlanPriceHistoryRepository;
import com.restaurant.saas.subscription.repository.SubscriptionInvoiceRepository;
import com.restaurant.saas.subscription.repository.SubscriptionPlanRepository;
import com.restaurant.saas.subscription.repository.SubscriptionRepository;
import com.restaurant.saas.tenant.entity.Tenant;
import com.restaurant.saas.tenant.repository.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.restaurant.saas.superadmin.service.SuperAdminStatsService.toPage;

@Service
public class SuperAdminPlatformService {

    private static final Set<String> BUSINESS_TYPES =
        Set.of("RESTAURANT", "ELECTRONICS", "VEHICLE", "REAL_ESTATE");
    private static final Set<String> PLAN_STATUSES =
        Set.of("ACTIVE", "INACTIVE", "DRAFT", "ARCHIVED");

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final PlanPriceHistoryRepository priceHistoryRepository;
    private final SubscriptionInvoiceRepository invoiceRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final NotificationRepository notificationRepository;
    private final CouponRepository couponRepository;
    private final PlatformSettingRepository settingRepository;
    private final FeatureFlagRepository flagRepository;
    private final PermissionRepository permissionRepository;

    public SuperAdminPlatformService(
        TenantRepository tenantRepository,
        UserRepository userRepository,
        RoleRepository roleRepository,
        BranchRepository branchRepository,
        SubscriptionRepository subscriptionRepository,
        SubscriptionPlanRepository planRepository,
        PlanPriceHistoryRepository priceHistoryRepository,
        SubscriptionInvoiceRepository invoiceRepository,
        AuditLogRepository auditLogRepository,
        AuditService auditService,
        NotificationRepository notificationRepository,
        CouponRepository couponRepository,
        PlatformSettingRepository settingRepository,
        FeatureFlagRepository flagRepository,
        PermissionRepository permissionRepository) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.branchRepository = branchRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.invoiceRepository = invoiceRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
        this.notificationRepository = notificationRepository;
        this.couponRepository = couponRepository;
        this.settingRepository = settingRepository;
        this.flagRepository = flagRepository;
        this.permissionRepository = permissionRepository;
    }

    // ---------- subscriptions ----------

    public record SubscriptionView(Long id, Long tenantId, String tenantName, Long planId, String planCode,
                                   String planName, String status, Instant trialEndsAt, Instant currentPeriodStart,
                                   Instant currentPeriodEnd, Boolean cancelAtPeriodEnd, BigDecimal priceMonthly,
                                   Instant createdAt) {
    }

    public record SubscriptionRequest(Long tenantId, String planCode) {
    }

    @Transactional(readOnly = true)
    public PageResponse<SubscriptionView> listSubscriptions(String status, String planCode, String search,
                                                            Long tenantId, int page, int size) {
        Map<Long, String> tenantNames = tenantNames();
        Map<Long, SubscriptionPlan> plans = plansById();
        String needle = normalize(search);
        List<SubscriptionView> views = subscriptionRepository.findAll().stream()
            .filter(sub -> tenantId == null || tenantId.equals(sub.getTenantId()))
            .map(sub -> view(sub, tenantNames, plans))
            .filter(view -> status == null || status.isBlank() || status.equalsIgnoreCase(view.status()))
            .filter(view -> planCode == null || planCode.isBlank() || planCode.equalsIgnoreCase(view.planCode()))
            .filter(view -> needle == null || (view.tenantName() != null
                && view.tenantName().toLowerCase(Locale.ROOT).contains(needle))
                || String.valueOf(view.tenantId()).contains(needle))
            .sorted(Comparator.comparing(SubscriptionView::createdAt,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());
        return toPage(views, page, size);
    }

    @Transactional
    public SubscriptionView assignSubscription(SubscriptionRequest request) {
        Tenant tenant = tenantRepository.findById(request.tenantId())
            .orElseThrow(() -> new NotFoundException("Tenant " + request.tenantId() + " not found"));
        SubscriptionPlan plan = planRepository.findByCode(request.planCode())
            .orElseThrow(() -> new NotFoundException("Plan " + request.planCode() + " not found"));
        if (!"ACTIVE".equals(plan.getStatus())) {
            throw new ConflictException("plan_not_active",
                "Plan " + request.planCode() + " is not active and cannot be assigned");
        }
        Instant now = Instant.now();
        Instant periodEnd = now.plus(30, ChronoUnit.DAYS);
        Subscription subscription = subscriptionRepository.findByTenantId(tenant.getId())
            .orElseGet(Subscription::new);
        boolean isNew = subscription.getId() == null;
        subscription.setTenantId(tenant.getId());
        subscription.setPlanId(plan.getId());
        subscription.setStatus("ACTIVE");
        subscription.setCancelAtPeriodEnd(Boolean.FALSE);
        subscription.setTrialEndsAt(null);
        if (isNew) {
            subscription.setCurrentPeriodStart(now);
            subscription.setCurrentPeriodEnd(periodEnd);
            subscriptionRepository.save(subscription);
        } else {
            subscription.setCurrentPeriodStart(now);
            subscription.setCurrentPeriodEnd(periodEnd);
            subscriptionRepository.save(subscription);
        }
        SubscriptionInvoice invoice = new SubscriptionInvoice();
        invoice.setTenantId(tenant.getId());
        invoice.setSubscriptionId(subscription.getId());
        invoice.setInvoiceNumber("INV-" + now.toEpochMilli() + "-" + tenant.getId());
        invoice.setAmount(plan.getPriceMonthly());
        invoice.setCurrency(plan.getCurrency());
        invoice.setStatus("OPEN");
        invoice.setDueAt(now.plus(7, ChronoUnit.DAYS));
        invoiceRepository.save(invoice);
        return view(subscription, tenantNames(), plansById());
    }

    @Transactional
    public SubscriptionView changeSubscriptionStatus(Long id, String status) {
        Subscription subscription = subscriptionRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Subscription " + id + " not found"));
        subscription.setStatus(status);
        if ("CANCELLED".equals(status)) {
            subscription.setCancelAtPeriodEnd(Boolean.TRUE);
        }
        if ("ACTIVE".equals(status)) {
            subscription.setCancelAtPeriodEnd(Boolean.FALSE);
            if (subscription.getCurrentPeriodEnd() == null || subscription.getCurrentPeriodEnd().isBefore(Instant.now())) {
                subscription.setCurrentPeriodStart(Instant.now());
                subscription.setCurrentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS));
            }
        }
        subscriptionRepository.save(subscription);
        return view(subscription, tenantNames(), plansById());
    }

    // ---------- plans ----------

    public record PlanView(Long id, String code, String name, String description, String businessType,
                           BigDecimal priceMonthly, BigDecimal priceQuarterly, BigDecimal priceYearly,
                           String currency, String status, Integer trialDays, BigDecimal setupFee,
                           BigDecimal taxPercent, BigDecimal yearlyDiscountPercent, Integer maxBranches,
                           Integer maxEmployees, Integer maxMenuItems, Integer maxOrdersPerMonth,
                           Integer maxStorageGb, Integer maxAdminUsers, Integer maxCustomers,
                           String features, Boolean isPublic, Integer sortOrder, int subscriberCount,
                           Instant createdAt, Instant updatedAt) {
    }

    public record PlanRequest(String code, String name, String description, String businessType,
                              BigDecimal priceMonthly, BigDecimal priceQuarterly, BigDecimal priceYearly,
                              String currency, String status, Integer trialDays, BigDecimal setupFee,
                              BigDecimal taxPercent, BigDecimal yearlyDiscountPercent, Integer maxBranches,
                              Integer maxEmployees, Integer maxMenuItems, Integer maxOrdersPerMonth,
                              Integer maxStorageGb, Integer maxAdminUsers, Integer maxCustomers,
                              String features, Boolean isPublic, Integer sortOrder) {
    }

    public record PlanStatusRequest(String status) {
    }

    public record PlanPriceHistoryView(Long id, BigDecimal oldPriceMonthly, BigDecimal newPriceMonthly,
                                       BigDecimal oldPriceQuarterly, BigDecimal newPriceQuarterly,
                                       BigDecimal oldPriceYearly, BigDecimal newPriceYearly,
                                       String currency, String changedBy, Instant createdAt) {
    }

    @Transactional(readOnly = true)
    public List<PlanView> listPlans() {
        Map<Long, Long> subscribers = activeSubscribersByPlan();
        return planRepository.findAll().stream()
            .sorted(Comparator.comparing(SubscriptionPlan::getSortOrder))
            .map(plan -> planView(plan, subscribers.getOrDefault(plan.getId(), 0L).intValue()))
            .toList();
    }

    @Transactional(readOnly = true)
    public PlanView getPlan(String code) {
        SubscriptionPlan plan = requirePlan(code);
        return planView(plan, (int) activeSubscriberCount(plan.getId()));
    }

    @Transactional
    public PlanView createPlan(PlanRequest request) {
        planRepository.findByCode(request.code() == null ? "" : request.code().trim()).ifPresent(existing -> {
            throw new ConflictException("plan_exists", "A plan with code " + request.code() + " already exists");
        });
        SubscriptionPlan plan = new SubscriptionPlan();
        apply(plan, request);
        planRepository.save(plan);
        recordPriceHistory(plan, null, null, null, null);
        audit("PLAN_CREATED", plan, null, planSnapshot(plan));
        return planView(plan, 0);
    }

    @Transactional
    public PlanView updatePlan(String code, PlanRequest request) {
        SubscriptionPlan plan = requirePlan(code);
        BigDecimal oldMonthly = plan.getPriceMonthly();
        BigDecimal oldQuarterly = plan.getPriceQuarterly();
        BigDecimal oldYearly = plan.getPriceYearly();
        String oldCurrency = plan.getCurrency();
        String before = planSnapshot(plan);
        apply(plan, request);
        planRepository.save(plan);
        if (priceChanged(oldMonthly, plan.getPriceMonthly())
            || priceChanged(oldQuarterly, plan.getPriceQuarterly())
            || priceChanged(oldYearly, plan.getPriceYearly())
            || !oldCurrency.equals(plan.getCurrency())) {
            recordPriceHistory(plan, oldMonthly, oldQuarterly, oldYearly, oldCurrency);
        }
        audit("PLAN_UPDATED", plan, before, planSnapshot(plan));
        return planView(plan, (int) activeSubscriberCount(plan.getId()));
    }

    @Transactional
    public PlanView changePlanStatus(String code, String requestedStatus) {
        SubscriptionPlan plan = requirePlan(code);
        String status = requestedStatus == null ? "" : requestedStatus.toUpperCase(Locale.ROOT);
        if (!PLAN_STATUSES.contains(status)) {
            throw new ConflictException("invalid_plan_status", "status must be one of " + PLAN_STATUSES);
        }
        String previous = plan.getStatus();
        if ("ARCHIVED".equals(previous) && !"ARCHIVED".equals(status)) {
            throw new ConflictException("plan_archived", "Archived plans cannot be reactivated");
        }
        plan.setStatus(status);
        plan.setArchivedAt("ARCHIVED".equals(status) ? Instant.now() : null);
        planRepository.save(plan);
        audit("PLAN_STATUS_CHANGED", plan, previous, status);
        return planView(plan, (int) activeSubscriberCount(plan.getId()));
    }

    @Transactional(readOnly = true)
    public List<PlanPriceHistoryView> priceHistory(String code) {
        SubscriptionPlan plan = requirePlan(code);
        return priceHistoryRepository.findByPlanIdOrderByCreatedAtDesc(plan.getId()).stream()
            .map(history -> new PlanPriceHistoryView(history.getId(), history.getOldPriceMonthly(),
                history.getNewPriceMonthly(), history.getOldPriceQuarterly(), history.getNewPriceQuarterly(),
                history.getOldPriceYearly(), history.getNewPriceYearly(), history.getCurrency(),
                history.getChangedBy(), history.getCreatedAt()))
            .toList();
    }

    @Transactional
    public void deletePlan(String code) {
        SubscriptionPlan plan = requirePlan(code);
        long subscribers = activeSubscriberCount(plan.getId());
        if (subscribers > 0) {
            throw new ConflictException("plan_in_use",
                "Plan " + code + " still has " + subscribers + " active subscription(s)");
        }
        String before = planSnapshot(plan);
        plan.setIsDelete(Boolean.TRUE);
        plan.setIsActive(Boolean.FALSE);
        planRepository.save(plan);
        audit("PLAN_DELETED", plan, before, null);
    }

    private SubscriptionPlan requirePlan(String code) {
        return planRepository.findByCode(code)
            .orElseThrow(() -> new NotFoundException("Plan " + code + " not found"));
    }

    private Map<Long, Long> activeSubscribersByPlan() {
        return subscriptionRepository.findAll().stream()
            .filter(sub -> "ACTIVE".equals(sub.getStatus()) || "TRIALING".equals(sub.getStatus()))
            .collect(Collectors.groupingBy(Subscription::getPlanId, Collectors.counting()));
    }

    private long activeSubscriberCount(Long planId) {
        return subscriptionRepository.findAll().stream()
            .filter(sub -> planId.equals(sub.getPlanId()))
            .filter(sub -> "ACTIVE".equals(sub.getStatus()) || "TRIALING".equals(sub.getStatus()))
            .count();
    }

    private void recordPriceHistory(SubscriptionPlan plan, BigDecimal oldMonthly, BigDecimal oldQuarterly,
                                    BigDecimal oldYearly, String oldCurrency) {
        PlanPriceHistory history = new PlanPriceHistory();
        history.setPlanId(plan.getId());
        history.setOldPriceMonthly(oldMonthly);
        history.setNewPriceMonthly(plan.getPriceMonthly());
        history.setOldPriceQuarterly(oldQuarterly);
        history.setNewPriceQuarterly(plan.getPriceQuarterly());
        history.setOldPriceYearly(oldYearly);
        history.setNewPriceYearly(plan.getPriceYearly());
        history.setCurrency(plan.getCurrency());
        history.setChangedBy(currentActor());
        history.setCreatedAt(Instant.now());
        priceHistoryRepository.save(history);
    }

    private void audit(String action, SubscriptionPlan plan, String before, String after) {
        auditService.record(null, null, currentActor(), action, "SUBSCRIPTION_PLAN",
            plan.getCode(), before, after);
    }

    private static String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String actor = authentication != null && authentication.isAuthenticated()
            ? authentication.getName() : "system";
        return actor.length() > 100 ? actor.substring(0, 100) : actor;
    }

    private static boolean priceChanged(BigDecimal before, BigDecimal after) {
        if (before == null || after == null) {
            return before != after;
        }
        return before.compareTo(after) != 0;
    }

    private static String planSnapshot(SubscriptionPlan plan) {
        return "{\"code\":\"" + plan.getCode() + "\",\"status\":\"" + plan.getStatus()
            + "\",\"priceMonthly\":" + plan.getPriceMonthly() + ",\"currency\":\"" + plan.getCurrency() + "\"}";
    }

    // ---------- invoices ----------

    public record InvoiceView(Long id, Long tenantId, String tenantName, Long subscriptionId, String invoiceNumber,
                              BigDecimal amount, String currency, String status, Instant dueAt, Instant paidAt,
                              Instant createdAt) {
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceView> listInvoices(String status, String search, Long tenantId, int page, int size) {
        Map<Long, String> tenantNames = tenantNames();
        String needle = normalize(search);
        List<InvoiceView> views = invoiceRepository.findAll().stream()
            .filter(invoice -> tenantId == null || tenantId.equals(invoice.getTenantId()))
            .map(invoice -> invoiceView(invoice, tenantNames))
            .filter(view -> status == null || status.isBlank() || status.equalsIgnoreCase(view.status()))
            .filter(view -> needle == null || (view.tenantName() != null
                && view.tenantName().toLowerCase(Locale.ROOT).contains(needle))
                || view.invoiceNumber().toLowerCase(Locale.ROOT).contains(needle))
            .sorted(Comparator.comparing(InvoiceView::createdAt,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());
        return toPage(views, page, size);
    }

    @Transactional
    public InvoiceView updateInvoiceStatus(Long id, String status) {
        SubscriptionInvoice invoice = invoiceRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Invoice " + id + " not found"));
        invoice.setStatus(status);
        if ("PAID".equals(status)) {
            invoice.setPaidAt(Instant.now());
        }
        if ("OPEN".equals(status)) {
            invoice.setPaidAt(null);
        }
        invoiceRepository.save(invoice);
        return invoiceView(invoice, tenantNames());
    }

    // ---------- users ----------

    public record UserView(Long id, String email, String firstName, String lastName, String phone, String status,
                           Long tenantId, String tenantName, Instant lastLoginAt, Boolean emailVerified,
                           List<String> roles, Instant createdAt) {
    }

    @Transactional(readOnly = true)
    public PageResponse<UserView> listUsers(String search, String status, Long tenantId, int page, int size) {
        Map<Long, String> tenantNames = tenantNames();
        String needle = normalize(search);
        List<UserView> views = userRepository.findAll().stream()
            .filter(user -> tenantId == null || tenantId.equals(user.getTenantId()))
            .filter(user -> status == null || status.isBlank() || status.equalsIgnoreCase(user.getStatus()))
            .map(user -> userView(user, tenantNames))
            .filter(view -> needle == null || view.email().toLowerCase(Locale.ROOT).contains(needle)
                || (view.firstName() + " " + view.lastName()).toLowerCase(Locale.ROOT).contains(needle))
            .sorted(Comparator.comparing(UserView::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());
        return toPage(views, page, size);
    }

    @Transactional
    public UserView updateUserStatus(Long id, String status) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User " + id + " not found"));
        user.setStatus(status);
        userRepository.save(user);
        return userView(user, tenantNames());
    }

    public record InviteResponse(UserView user, String inviteToken, Instant inviteExpiresAt) {
    }

    @Transactional
    public InviteResponse resendInvite(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User " + id + " not found"));
        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        user.setInviteToken(token.substring(0, 64));
        user.setInviteExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        userRepository.save(user);
        return new InviteResponse(userView(user, tenantNames()), user.getInviteToken(), user.getInviteExpiresAt());
    }

    // ---------- roles ----------

    public record RoleView(Long id, String code, String name, String description, Boolean systemRole,
                           Long tenantId, List<Long> permissionIds, List<String> permissions) {
    }

    public record RolePermissionsRequest(List<Long> permissionIds) {
    }

    @Transactional(readOnly = true)
    public List<RoleView> listRoles() {
        return roleRepository.findAll().stream()
            .sorted(Comparator.comparing(Role::getCode))
            .map(this::roleView)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPermissions() {
        return permissionRepository.findAll().stream()
            .sorted(Comparator.comparing(Permission::getCode))
            .map(permission -> Map.<String, Object>of(
                "id", permission.getId(),
                "code", permission.getCode(),
                "name", permission.getName()))
            .toList();
    }

    @Transactional
    public RoleView updateRolePermissions(Long id, RolePermissionsRequest request) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Role " + id + " not found"));
        if (Boolean.TRUE.equals(role.getSystemRole())) {
            throw new ConflictException("system_role", "Permissions of system roles cannot be edited");
        }
        Set<Permission> permissions = new HashSet<>(permissionRepository.findByIdIn(request.permissionIds()));
        if (permissions.size() != new HashSet<>(request.permissionIds()).size()) {
            throw new NotFoundException("One or more permissions were not found");
        }
        role.setPermissions(permissions);
        roleRepository.save(role);
        auditService.record(role.getTenantId(), currentUserId(), currentUserEmail(), "ROLE_PERMISSIONS_UPDATED", "Role",
            String.valueOf(role.getId()), null, role.getCode());
        return roleView(role);
    }

    public record RoleRequest(String code, String name, String description) {
    }

    @Transactional
    public RoleView createRole(RoleRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (code.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_role_code", "Role code is required");
        }
        if (roleRepository.findByCodeAndTenantIdIsNull(code).isPresent()) {
            throw new ConflictException("role_exists", "A role with this code already exists");
        }
        Role role = new Role();
        role.setCode(code);
        role.setName(request.name().trim());
        role.setDescription(blankToNull(request.description()));
        role.setSystemRole(Boolean.FALSE);
        role.setTenantId(null);
        Role saved = roleRepository.save(role);
        auditService.record(null, currentUserId(), currentUserEmail(), "ROLE_CREATED", "Role",
            String.valueOf(saved.getId()), null, saved.getCode());
        return roleView(saved);
    }

    @Transactional
    public RoleView updateRole(Long id, RoleRequest request) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Role " + id + " not found"));
        if (Boolean.TRUE.equals(role.getSystemRole())) {
            throw new ConflictException("system_role", "System roles cannot be modified");
        }
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (!code.equals(role.getCode()) && roleRepository.findByCodeAndTenantIdIsNull(code).isPresent()) {
            throw new ConflictException("role_exists", "A role with this code already exists");
        }
        role.setCode(code);
        role.setName(request.name().trim());
        role.setDescription(blankToNull(request.description()));
        Role saved = roleRepository.save(role);
        auditService.record(role.getTenantId(), currentUserId(), currentUserEmail(), "ROLE_UPDATED", "Role",
            String.valueOf(saved.getId()), null, saved.getCode());
        return roleView(saved);
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Role " + id + " not found"));
        if (Boolean.TRUE.equals(role.getSystemRole())) {
            throw new ConflictException("system_role", "System roles cannot be deleted");
        }
        long assigned = userRepository.findAll().stream()
            .filter(user -> user.getRoles().stream().anyMatch(r -> r.getId().equals(id)))
            .count();
        if (assigned > 0) {
            throw new ConflictException("role_in_use", "Role is assigned to one or more users");
        }
        roleRepository.delete(role);
        auditService.record(role.getTenantId(), currentUserId(), currentUserEmail(), "ROLE_DELETED", "Role",
            String.valueOf(id), null, role.getCode());
    }

    @Transactional
    public RoleView assignUserRole(Long userId, String roleCode) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
        Role role = roleRepository.findByCodeAndTenantIdIsNull(roleCode.trim().toUpperCase(Locale.ROOT))
            .orElseThrow(() -> new NotFoundException("Role " + roleCode + " not found"));
        if (user.getRoles().contains(role)) {
            throw new ConflictException("role_assigned", "User already has this role");
        }
        user.getRoles().add(role);
        userRepository.save(user);
        auditService.record(user.getTenantId(), currentUserId(), currentUserEmail(), "USER_ROLE_ASSIGNED", "User",
            String.valueOf(user.getId()), null, role.getCode());
        return roleView(role);
    }

    @Transactional
    public void removeUserRole(Long userId, String roleCode) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
        Role role = roleRepository.findByCodeAndTenantIdIsNull(roleCode.trim().toUpperCase(Locale.ROOT))
            .orElseThrow(() -> new NotFoundException("Role " + roleCode + " not found"));
        if (!user.getRoles().remove(role)) {
            throw new ConflictException("role_not_assigned", "User does not have this role");
        }
        userRepository.save(user);
        auditService.record(user.getTenantId(), currentUserId(), currentUserEmail(), "USER_ROLE_REMOVED", "User",
            String.valueOf(user.getId()), null, role.getCode());
    }

    @Transactional(readOnly = true)
    public List<UserView> usersInRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new NotFoundException("Role " + roleId + " not found"));
        Map<Long, String> tenantNames = tenantNames();
        return userRepository.findAll().stream()
            .filter(user -> user.getRoles().stream().anyMatch(r -> r.getId().equals(roleId)))
            .map(user -> userView(user, tenantNames))
            .sorted(Comparator.comparing(UserView::email))
            .toList();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Long currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
            || !(authentication.getPrincipal() instanceof AuthenticatedPrincipal p)) {
            return null;
        }
        return p.userId();
    }

    private String currentUserEmail() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
            || !(authentication.getPrincipal() instanceof AuthenticatedPrincipal p)) {
            return null;
        }
        return p.email();
    }

    // ---------- branches ----------

    public record BranchView(Long id, Long tenantId, String tenantName, Long restaurantId, String code, String name,
                             String city, String country, String status, String currency, Boolean isDefault,
                             Instant createdAt) {
    }

    @Transactional(readOnly = true)
    public PageResponse<BranchView> listBranches(String search, String status, Long tenantId, int page, int size) {
        Map<Long, String> tenantNames = tenantNames();
        String needle = normalize(search);
        List<BranchView> views = branchRepository.findAll().stream()
            .filter(branch -> tenantId == null || tenantId.equals(branch.getTenantId()))
            .filter(branch -> status == null || status.isBlank() || status.equalsIgnoreCase(branch.getStatus()))
            .map(branch -> new BranchView(
                branch.getId(), branch.getTenantId(),
                tenantNames.getOrDefault(branch.getTenantId(), "#" + branch.getTenantId()),
                branch.getRestaurantId(), branch.getCode(), branch.getName(), branch.getCity(),
                branch.getCountry(), branch.getStatus(), branch.getCurrency(), branch.getIsDefault(),
                branch.getCreatedAt()))
            .filter(view -> needle == null || view.name().toLowerCase(Locale.ROOT).contains(needle)
                || view.code().toLowerCase(Locale.ROOT).contains(needle)
                || (view.tenantName() != null && view.tenantName().toLowerCase(Locale.ROOT).contains(needle)))
            .sorted(Comparator.comparing(BranchView::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());
        return toPage(views, page, size);
    }

    @Transactional
    public BranchView updateBranchStatus(Long id, String status) {
        Branch branch = branchRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Branch " + id + " not found"));
        if (!Set.of("ACTIVE", "PAUSED", "INACTIVE").contains(status)) {
            throw new ConflictException("invalid_status", "Status must be ACTIVE, PAUSED or INACTIVE");
        }
        branch.setStatus(status);
        branchRepository.save(branch);
        Map<Long, String> names = tenantNames();
        return new BranchView(
            branch.getId(), branch.getTenantId(),
            names.getOrDefault(branch.getTenantId(), "#" + branch.getTenantId()),
            branch.getRestaurantId(), branch.getCode(), branch.getName(), branch.getCity(),
            branch.getCountry(), branch.getStatus(), branch.getCurrency(), branch.getIsDefault(),
            branch.getCreatedAt());
    }

    // ---------- audit ----------

    public record AuditView(Long id, Long tenantId, Long actorId, String actorEmail, String action,
                            String entityType, String entityId, String ipAddress, String requestId,
                            Instant createdAt) {
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditView> listAudit(String search, String entityType, Long tenantId,
                                             String from, String to, String action, String actor,
                                             int page, int size) {
        String needle = normalize(search);
        String actionNeedle = normalize(action);
        String actorNeedle = normalize(actor);
        java.time.Instant fromInstant = null;
        java.time.Instant toInstant = null;
        if (from != null && !from.isBlank()) {
            try {
                fromInstant = java.time.LocalDate.parse(from)
                    .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
            } catch (java.time.format.DateTimeParseException ex) {
                throw new com.restaurant.saas.common.exception.ApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "invalid_date",
                    "from must be YYYY-MM-DD");
            }
        }
        if (to != null && !to.isBlank()) {
            try {
                toInstant = java.time.LocalDate.parse(to).plusDays(1)
                    .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
            } catch (java.time.format.DateTimeParseException ex) {
                throw new com.restaurant.saas.common.exception.ApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "invalid_date",
                    "to must be YYYY-MM-DD");
            }
        }
        java.time.Instant finalFrom = fromInstant;
        java.time.Instant finalTo = toInstant;
        List<AuditView> views = auditLogRepository.findAll().stream()
            .filter(log -> tenantId == null || tenantId.equals(log.getTenantId()))
            .filter(log -> entityType == null || entityType.isBlank()
                || entityType.equalsIgnoreCase(log.getEntityType()))
            .filter(log -> finalFrom == null || !log.getCreatedAt().isBefore(finalFrom))
            .filter(log -> finalTo == null || log.getCreatedAt().isBefore(finalTo))
            .map(log -> new AuditView(log.getId(), log.getTenantId(), log.getActorId(), log.getActorEmail(),
                log.getAction(), log.getEntityType(), log.getEntityId(), log.getIpAddress(), log.getRequestId(),
                log.getCreatedAt()))
            .filter(view -> needle == null || view.action().toLowerCase(Locale.ROOT).contains(needle)
                || view.entityType().toLowerCase(Locale.ROOT).contains(needle)
                || (view.actorEmail() != null && view.actorEmail().toLowerCase(Locale.ROOT).contains(needle)))
            .filter(view -> actionNeedle == null || view.action().toLowerCase(Locale.ROOT).contains(actionNeedle))
            .filter(view -> actorNeedle == null
                || (view.actorEmail() != null && view.actorEmail().toLowerCase(Locale.ROOT).contains(actorNeedle)))
            .sorted(Comparator.comparing(AuditView::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());
        return toPage(views, page, size);
    }

    // ---------- notifications ----------

    public record NotificationView(Long id, Long tenantId, Long userId, String channel, String templateCode,
                                   String subject, String body, String status, Instant sentAt, Instant readAt,
                                   Instant createdAt) {
    }

    public record BroadcastRequest(String subject, String body) {
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationView> listNotifications(boolean unreadOnly, String search, int page, int size) {
        String needle = normalize(search);
        List<NotificationView> views = notificationRepository.findAll().stream()
            .filter(notification -> !unreadOnly || notification.getReadAt() == null)
            .map(SuperAdminPlatformService::notificationView)
            .filter(view -> needle == null
                || (view.subject() != null && view.subject().toLowerCase(Locale.ROOT).contains(needle))
                || view.templateCode().toLowerCase(Locale.ROOT).contains(needle))
            .sorted(Comparator.comparing(NotificationView::createdAt,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());
        return toPage(views, page, size);
    }

    @Transactional
    public NotificationView markNotificationRead(Long id) {
        Notification notification = notificationRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Notification " + id + " not found"));
        notification.setReadAt(Instant.now());
        notificationRepository.save(notification);
        return notificationView(notification);
    }

    @Transactional
    public Map<String, Object> broadcast(BroadcastRequest request) {
        List<User> users = userRepository.findAll();
        Instant now = Instant.now();
        for (User user : users) {
            Notification notification = new Notification();
            notification.setTenantId(user.getTenantId());
            notification.setUserId(user.getId());
            notification.setChannel("IN_APP");
            notification.setTemplateCode("PLATFORM_BROADCAST");
            notification.setSubject(request.subject());
            notification.setBody(request.body());
            notification.setStatus("SENT");
            notification.setSentAt(now);
            notificationRepository.save(notification);
        }
        return Map.of("recipients", users.size());
    }

    // ---------- coupons ----------

    public record CouponView(Long id, String code, String description, String discountType, BigDecimal discountValue,
                             Instant startsAt, Instant endsAt, Integer maxRedemptions, Integer redeemedCount,
                             Long tenantId, Instant createdAt) {
    }

    public record CouponRequest(String code, String description, String discountType, BigDecimal discountValue,
                                Instant startsAt, Instant endsAt, Integer maxRedemptions, Long tenantId) {
    }

    @Transactional(readOnly = true)
    public List<CouponView> listCoupons() {
        return couponRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(SuperAdminPlatformService::couponView)
            .toList();
    }

    @Transactional
    public CouponView createCoupon(CouponRequest request) {
        couponRepository.findFirstByCode(request.code()).ifPresent(existing -> {
            throw new ConflictException("coupon_exists", "Coupon " + request.code() + " already exists");
        });
        validateCoupon(request);
        Coupon coupon = new Coupon();
        applyCoupon(coupon, request);
        coupon.setRedeemedCount(0);
        couponRepository.save(coupon);
        return couponView(coupon);
    }

    @Transactional
    public CouponView updateCoupon(Long id, CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Coupon " + id + " not found"));
        validateCoupon(request);
        applyCoupon(coupon, request);
        couponRepository.save(coupon);
        return couponView(coupon);
    }

    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Coupon " + id + " not found"));
        coupon.setIsDelete(Boolean.TRUE);
        coupon.setIsActive(Boolean.FALSE);
        couponRepository.save(coupon);
    }

    // ---------- settings / flags / integrations ----------

    public record SettingView(Long id, String key, String value, String description, String category) {
    }

    public record SettingUpsert(String key, String value) {
    }

    public record SettingsRequest(List<SettingUpsert> settings) {
    }

    @Transactional(readOnly = true)
    public List<SettingView> listSettings(String category) {
        return settingRepository.findAllByOrderByCategoryAscSettingKeyAsc().stream()
            .filter(setting -> category == null || category.isBlank()
                || category.equalsIgnoreCase(setting.getCategory()))
            .map(SuperAdminPlatformService::settingView)
            .toList();
    }

    @Transactional
    public List<SettingView> upsertSettings(SettingsRequest request) {
        for (SettingUpsert upsert : request.settings()) {
            PlatformSetting setting = settingRepository.findBySettingKey(upsert.key())
                .orElseGet(() -> {
                    PlatformSetting created = new PlatformSetting();
                    created.setSettingKey(upsert.key());
                    created.setCategory("GENERAL");
                    return created;
                });
            setting.setSettingValue(upsert.value());
            settingRepository.save(setting);
        }
        return listSettings(null);
    }

    public record FlagView(Long id, String key, String description, Boolean enabled, String planCode) {
    }

    public record FlagRequest(String key, Boolean enabled) {
    }

    @Transactional(readOnly = true)
    public List<FlagView> listFlags() {
        return flagRepository.findAllByOrderByFlagKeyAsc().stream()
            .map(flag -> new FlagView(flag.getId(), flag.getFlagKey(), flag.getDescription(),
                flag.getEnabled(), flag.getPlanCode()))
            .toList();
    }

    @Transactional
    public FlagView toggleFlag(FlagRequest request) {
        FeatureFlag flag = flagRepository.findByFlagKey(request.key())
            .orElseThrow(() -> new NotFoundException("Feature flag " + request.key() + " not found"));
        flag.setEnabled(request.enabled());
        flagRepository.save(flag);
        return new FlagView(flag.getId(), flag.getFlagKey(), flag.getDescription(),
            flag.getEnabled(), flag.getPlanCode());
    }

    // ---------- helpers ----------

    private static PlanView planView(SubscriptionPlan plan, int subscriberCount) {
        return new PlanView(
            plan.getId(), plan.getCode(), plan.getName(), plan.getDescription(), plan.getBusinessType(),
            plan.getPriceMonthly(), plan.getPriceQuarterly(), plan.getPriceYearly(), plan.getCurrency(),
            plan.getStatus(), plan.getTrialDays(), plan.getSetupFee(), plan.getTaxPercent(),
            plan.getYearlyDiscountPercent(), plan.getMaxBranches(), plan.getMaxEmployees(),
            plan.getMaxMenuItems(), plan.getMaxOrdersPerMonth(), plan.getMaxStorageGb(),
            plan.getMaxAdminUsers(), plan.getMaxCustomers(), plan.getFeatures(), plan.getIsPublic(),
            plan.getSortOrder(), subscriberCount, plan.getCreatedAt(), plan.getUpdatedAt());
    }

    private SubscriptionView view(Subscription sub, Map<Long, String> tenantNames,
                                  Map<Long, SubscriptionPlan> plans) {
        SubscriptionPlan plan = plans.get(sub.getPlanId());
        return new SubscriptionView(
            sub.getId(), sub.getTenantId(),
            tenantNames.getOrDefault(sub.getTenantId(), "#" + sub.getTenantId()),
            sub.getPlanId(),
            plan == null ? null : plan.getCode(),
            plan == null ? null : plan.getName(),
            sub.getStatus(), sub.getTrialEndsAt(), sub.getCurrentPeriodStart(), sub.getCurrentPeriodEnd(),
            sub.getCancelAtPeriodEnd(),
            plan == null ? null : plan.getPriceMonthly(),
            sub.getCreatedAt());
    }

    private Map<Long, String> tenantNames() {
        return tenantRepository.findAll().stream()
            .collect(Collectors.toMap(Tenant::getId, Tenant::getName, (a, b) -> a));
    }

    private Map<Long, SubscriptionPlan> plansById() {
        return planRepository.findAll().stream()
            .collect(Collectors.toMap(SubscriptionPlan::getId, Function.identity(), (a, b) -> a));
    }

    private InvoiceView invoiceView(SubscriptionInvoice invoice, Map<Long, String> tenantNames) {
        return new InvoiceView(
            invoice.getId(), invoice.getTenantId(),
            tenantNames.getOrDefault(invoice.getTenantId(), "#" + invoice.getTenantId()),
            invoice.getSubscriptionId(), invoice.getInvoiceNumber(), invoice.getAmount(),
            invoice.getCurrency(), invoice.getStatus(), invoice.getDueAt(), invoice.getPaidAt(),
            invoice.getCreatedAt());
    }

    private UserView userView(User user, Map<Long, String> tenantNames) {
        List<String> roles = user.getRoles().stream().map(Role::getCode).sorted().toList();
        return new UserView(
            user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getPhone(),
            user.getStatus(), user.getTenantId(),
            user.getTenantId() == null ? "Platform" : tenantNames.getOrDefault(user.getTenantId(), "#" + user.getTenantId()),
            user.getLastLoginAt(), user.getEmailVerified(), roles, user.getCreatedAt());
    }

    private RoleView roleView(Role role) {
        List<Long> permissionIds = role.getPermissions().stream().map(Permission::getId).sorted().toList();
        List<String> permissions = role.getPermissions().stream().map(Permission::getCode).sorted().toList();
        return new RoleView(role.getId(), role.getCode(), role.getName(), role.getDescription(),
            role.getSystemRole(), role.getTenantId(), permissionIds, permissions);
    }

    private static NotificationView notificationView(Notification notification) {
        return new NotificationView(
            notification.getId(), notification.getTenantId(), notification.getUserId(),
            notification.getChannel(), notification.getTemplateCode(), notification.getSubject(),
            notification.getBody(), notification.getStatus(), notification.getSentAt(),
            notification.getReadAt(), notification.getCreatedAt());
    }

    private static CouponView couponView(Coupon coupon) {
        return new CouponView(
            coupon.getId(), coupon.getCode(), coupon.getDescription(), coupon.getDiscountType(),
            coupon.getDiscountValue(), coupon.getStartsAt(), coupon.getEndsAt(),
            coupon.getMaxRedemptions(), coupon.getRedeemedCount(), coupon.getTenantId(), coupon.getCreatedAt());
    }

    private static SettingView settingView(PlatformSetting setting) {
        return new SettingView(setting.getId(), setting.getSettingKey(), setting.getSettingValue(),
            setting.getDescription(), setting.getCategory());
    }

    private static void validateCoupon(CouponRequest request) {
        if (!"PERCENT".equals(request.discountType()) && !"FIXED".equals(request.discountType())) {
            throw new ConflictException("invalid_discount_type", "discountType must be PERCENT or FIXED");
        }
        if ("PERCENT".equals(request.discountType())
            && request.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ConflictException("invalid_discount_value", "Percentage discounts cannot exceed 100");
        }
    }

    private static void applyCoupon(Coupon coupon, CouponRequest request) {
        coupon.setCode(request.code().toUpperCase(Locale.ROOT).trim());
        coupon.setDescription(request.description());
        coupon.setDiscountType(request.discountType());
        coupon.setDiscountValue(request.discountValue());
        coupon.setStartsAt(request.startsAt());
        coupon.setEndsAt(request.endsAt());
        coupon.setMaxRedemptions(request.maxRedemptions());
        coupon.setTenantId(request.tenantId());
    }

    private static void apply(SubscriptionPlan plan, PlanRequest request) {
        String code = request.code() == null ? "" : request.code().toUpperCase(Locale.ROOT).trim();
        if (code.isBlank()) {
            throw new ConflictException("invalid_plan_code", "Plan code is required");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new ConflictException("invalid_plan_name", "Plan name is required");
        }
        String businessType = request.businessType() == null
            ? "RESTAURANT" : request.businessType().toUpperCase(Locale.ROOT);
        if (!BUSINESS_TYPES.contains(businessType)) {
            throw new ConflictException("invalid_business_type",
                "businessType must be one of " + BUSINESS_TYPES);
        }
        String status = request.status() == null
            ? (plan.getStatus() == null ? "ACTIVE" : plan.getStatus())
            : request.status().toUpperCase(Locale.ROOT);
        if (!PLAN_STATUSES.contains(status)) {
            throw new ConflictException("invalid_plan_status", "status must be one of " + PLAN_STATUSES);
        }
        BigDecimal monthly = request.priceMonthly() == null ? BigDecimal.ZERO : request.priceMonthly();
        validateAmount(monthly, "priceMonthly");
        validateAmount(request.priceQuarterly(), "priceQuarterly");
        validateAmount(request.priceYearly(), "priceYearly");
        validateAmount(request.setupFee(), "setupFee");
        validatePercent(request.taxPercent(), "taxPercent");
        validatePercent(request.yearlyDiscountPercent(), "yearlyDiscountPercent");
        validateLimit(request.trialDays(), "trialDays");
        validateLimit(request.maxBranches(), "maxBranches");
        validateLimit(request.maxEmployees(), "maxEmployees");
        validateLimit(request.maxMenuItems(), "maxMenuItems");
        validateLimit(request.maxOrdersPerMonth(), "maxOrdersPerMonth");
        validateLimit(request.maxStorageGb(), "maxStorageGb");
        validateLimit(request.maxAdminUsers(), "maxAdminUsers");
        validateLimit(request.maxCustomers(), "maxCustomers");

        plan.setCode(code);
        plan.setName(request.name().trim());
        plan.setDescription(request.description());
        plan.setBusinessType(businessType);
        plan.setPriceMonthly(monthly);
        plan.setPriceQuarterly(request.priceQuarterly());
        plan.setPriceYearly(request.priceYearly());
        plan.setStatus(status);
        if ("ARCHIVED".equals(status)) {
            plan.setArchivedAt(plan.getArchivedAt() == null ? Instant.now() : plan.getArchivedAt());
        } else {
            plan.setArchivedAt(null);
        }
        plan.setTrialDays(request.trialDays() == null ? 0 : request.trialDays());
        plan.setSetupFee(request.setupFee());
        plan.setTaxPercent(request.taxPercent());
        plan.setYearlyDiscountPercent(request.yearlyDiscountPercent());
        plan.setCurrency(request.currency() == null ? "USD" : request.currency().toUpperCase(Locale.ROOT));
        plan.setMaxBranches(request.maxBranches());
        plan.setMaxEmployees(request.maxEmployees());
        plan.setMaxMenuItems(request.maxMenuItems());
        plan.setMaxOrdersPerMonth(request.maxOrdersPerMonth());
        plan.setMaxStorageGb(request.maxStorageGb());
        plan.setMaxAdminUsers(request.maxAdminUsers());
        plan.setMaxCustomers(request.maxCustomers());
        plan.setFeatures(request.features());
        plan.setIsPublic(request.isPublic() == null || request.isPublic());
        plan.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    }

    private static void validateAmount(BigDecimal value, String field) {
        if (value != null && value.signum() < 0) {
            throw new ConflictException("invalid_" + field, field + " cannot be negative");
        }
    }

    private static void validatePercent(BigDecimal value, String field) {
        if (value == null) {
            return;
        }
        if (value.signum() < 0 || value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ConflictException("invalid_" + field, field + " must be between 0 and 100");
        }
    }

    private static void validateLimit(Integer value, String field) {
        if (value != null && value < 0) {
            throw new ConflictException("invalid_" + field, field + " cannot be negative");
        }
    }

    private static String normalize(String search) {
        return search == null || search.isBlank() ? null : search.trim().toLowerCase(Locale.ROOT);
    }
}
