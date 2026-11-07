package com.restaurant.saas.superadmin.service;

import com.restaurant.saas.branch.entity.Branch;
import com.restaurant.saas.branch.repository.BranchRepository;
import com.restaurant.saas.common.web.PageResponse;
import com.restaurant.saas.customer.entity.Customer;
import com.restaurant.saas.customer.repository.CustomerRepository;
import com.restaurant.saas.employee.repository.EmployeeRepository;
import com.restaurant.saas.identity.entity.User;
import com.restaurant.saas.identity.repository.UserRepository;
import com.restaurant.saas.menu.repository.MenuItemRepository;
import com.restaurant.saas.order.entity.Order;
import com.restaurant.saas.order.repository.OrderRepository;
import com.restaurant.saas.subscription.entity.Subscription;
import com.restaurant.saas.subscription.entity.SubscriptionInvoice;
import com.restaurant.saas.subscription.entity.SubscriptionPlan;
import com.restaurant.saas.subscription.repository.SubscriptionInvoiceRepository;
import com.restaurant.saas.subscription.repository.SubscriptionPlanRepository;
import com.restaurant.saas.subscription.repository.SubscriptionRepository;
import com.restaurant.saas.tenant.entity.Tenant;
import com.restaurant.saas.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SuperAdminStatsService {

    private final TenantRepository tenantRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionInvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final MenuItemRepository menuItemRepository;

    public SuperAdminStatsService(
        TenantRepository tenantRepository,
        BranchRepository branchRepository,
        UserRepository userRepository,
        SubscriptionRepository subscriptionRepository,
        SubscriptionPlanRepository planRepository,
        SubscriptionInvoiceRepository invoiceRepository,
        OrderRepository orderRepository,
        CustomerRepository customerRepository,
        EmployeeRepository employeeRepository,
        MenuItemRepository menuItemRepository) {
        this.tenantRepository = tenantRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.invoiceRepository = invoiceRepository;
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.menuItemRepository = menuItemRepository;
    }

    public record CountBucket(int total, int active, int paused, int inactive, int trial, int suspended,
                              int pending, int cancelled, int trialing, int expiringSoon) {
    }

    public record PlanDistributionItem(String code, String name, int subscribers, BigDecimal mrr) {
    }

    public record MonthAmount(String month, BigDecimal amount) {
    }

    public record StatsResponse(
        CountBucket tenants,
        CountBucket branches,
        CountBucket users,
        CountBucket subscriptions,
        BigDecimal mrr,
        int invoiceOpen,
        BigDecimal outstandingAmount,
        BigDecimal paidThisMonth,
        List<PlanDistributionItem> planDistribution,
        List<MonthAmount> revenueLast12Months) {
    }

    @Transactional(readOnly = true)
    public StatsResponse stats() {
        Instant now = Instant.now();
        List<Tenant> tenants = tenantRepository.findAll();
        List<Branch> branches = branchRepository.findAll();
        List<User> users = userRepository.findAll();
        List<Subscription> subs = subscriptionRepository.findAll();
        List<SubscriptionPlan> plans = planRepository.findAll();
        List<SubscriptionInvoice> invoices = invoiceRepository.findAll();
        Map<Long, SubscriptionPlan> planById = plans.stream()
            .collect(Collectors.toMap(SubscriptionPlan::getId, plan -> plan, (a, b) -> a));

        CountBucket tenantBucket = new CountBucket(
            tenants.size(),
            count(tenants, Tenant::getStatus, "ACTIVE"),
            0, 0,
            count(tenants, Tenant::getStatus, "TRIAL"),
            count(tenants, Tenant::getStatus, "SUSPENDED"),
            count(tenants, Tenant::getStatus, "PENDING_VERIFICATION"),
            count(tenants, Tenant::getStatus, "CANCELLED"),
            0, 0);

        CountBucket branchBucket = new CountBucket(
            branches.size(),
            count(branches, Branch::getStatus, "ACTIVE"),
            count(branches, Branch::getStatus, "PAUSED"),
            count(branches, Branch::getStatus, "INACTIVE"),
            0, 0, 0, 0, 0, 0);

        CountBucket userBucket = new CountBucket(
            users.size(),
            count(users, User::getStatus, "ACTIVE"),
            0, 0, 0, 0,
            count(users, User::getStatus, "PENDING"),
            count(users, User::getStatus, "SUSPENDED"),
            0, 0);

        int active = 0;
        int trialing = 0;
        int cancelled = 0;
        int expiringSoon = 0;
        BigDecimal mrr = BigDecimal.ZERO;
        Map<Long, Integer> subscribersPerPlan = new HashMap<>();
        Map<Long, BigDecimal> mrrPerPlan = new HashMap<>();
        for (Subscription sub : subs) {
            String status = sub.getStatus();
            if ("CANCELLED".equals(status)) {
                cancelled++;
            }
            if ("ACTIVE".equals(status) || "TRIALING".equals(status)) {
                active++;
                SubscriptionPlan plan = planById.get(sub.getPlanId());
                if (plan != null) {
                    mrr = mrr.add(plan.getPriceMonthly());
                    subscribersPerPlan.merge(sub.getPlanId(), 1, Integer::sum);
                    mrrPerPlan.merge(sub.getPlanId(), plan.getPriceMonthly(), BigDecimal::add);
                }
            }
            if ("TRIALING".equals(status)) trialing++;
            if (("ACTIVE".equals(status) || "TRIALING".equals(status)) && sub.getCurrentPeriodEnd() != null
                && !sub.getCurrentPeriodEnd().isAfter(now.plus(Duration.ofDays(14)))) {
                expiringSoon++;
            }
        }
        CountBucket subscriptionBucket = new CountBucket(
            subs.size(), active, 0, 0, 0, 0, 0, cancelled, trialing, expiringSoon);

        int open = 0;
        BigDecimal outstanding = BigDecimal.ZERO;
        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
        BigDecimal paidThisMonth = BigDecimal.ZERO;
        Map<String, BigDecimal> revenueByMonth = new java.util.LinkedHashMap<>();
        for (int i = 11; i >= 0; i--) {
            revenueByMonth.put(currentMonth.minusMonths(i).toString(), BigDecimal.ZERO);
        }
        for (SubscriptionInvoice invoice : invoices) {
            if ("OPEN".equals(invoice.getStatus())) {
                open++;
                outstanding = outstanding.add(invoice.getAmount());
            }
            if ("PAID".equals(invoice.getStatus()) && invoice.getPaidAt() != null) {
                String month = YearMonth.from(invoice.getPaidAt()).toString();
                revenueByMonth.merge(month, invoice.getAmount(), BigDecimal::add);
                if (YearMonth.from(invoice.getPaidAt()).equals(currentMonth)) {
                    paidThisMonth = paidThisMonth.add(invoice.getAmount());
                }
            }
        }

        List<PlanDistributionItem> distribution = plans.stream()
            .map(plan -> new PlanDistributionItem(
                plan.getCode(),
                plan.getName(),
                subscribersPerPlan.getOrDefault(plan.getId(), 0),
                mrrPerPlan.getOrDefault(plan.getId(), BigDecimal.ZERO)))
            .sorted(Comparator.comparingInt(PlanDistributionItem::subscribers).reversed())
            .toList();

        List<MonthAmount> revenue = revenueByMonth.entrySet().stream()
            .map(entry -> new MonthAmount(entry.getKey(), entry.getValue()))
            .toList();

        return new StatsResponse(
            tenantBucket, branchBucket, userBucket, subscriptionBucket,
            mrr.setScale(2, RoundingMode.HALF_UP),
            open, outstanding.setScale(2, RoundingMode.HALF_UP),
            paidThisMonth.setScale(2, RoundingMode.HALF_UP),
            distribution, revenue);
    }

    @Transactional(readOnly = true)
    public List<MonthAmount> revenue(int months) {
        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
        Map<String, BigDecimal> byMonth = new java.util.LinkedHashMap<>();
        for (int i = Math.max(1, months) - 1; i >= 0; i--) {
            byMonth.put(currentMonth.minusMonths(i).toString(), BigDecimal.ZERO);
        }
        for (SubscriptionInvoice invoice : invoiceRepository.findAll()) {
            if ("PAID".equals(invoice.getStatus()) && invoice.getPaidAt() != null) {
                String month = YearMonth.from(invoice.getPaidAt()).toString();
                byMonth.merge(month, invoice.getAmount(), BigDecimal::add);
            }
        }
        return byMonth.entrySet().stream()
            .map(entry -> new MonthAmount(entry.getKey(), entry.getValue().setScale(2, RoundingMode.HALF_UP)))
            .toList();
    }

    public record SalesDay(String date, BigDecimal gross, int orders) {
    }

    public record SalesReportResponse(int days, BigDecimal gross, int orders, int cancelled,
                                      BigDecimal avgTicket, List<SalesDay> daily) {
    }

    @Transactional(readOnly = true)
    public SalesReportResponse sales(int days) {
        int range = Math.max(1, Math.min(days, 365));
        Instant from = Instant.now().minus(Duration.ofDays(range));
        List<Order> orders = orderRepository.findAll().stream()
            .filter(order -> {
                Instant at = order.getCompletedAt() != null ? order.getCompletedAt()
                    : order.getPlacedAt() != null ? order.getPlacedAt() : order.getCreatedAt();
                return at != null && !at.isBefore(from);
            })
            .toList();
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        Map<String, BigDecimal> grossByDay = new java.util.LinkedHashMap<>();
        Map<String, Integer> countByDay = new java.util.LinkedHashMap<>();
        BigDecimal gross = BigDecimal.ZERO;
        int completed = 0;
        int cancelled = 0;
        for (int i = range - 1; i >= 0; i--) {
            String day = LocalDate.now(ZoneOffset.UTC).minusDays(i).format(fmt);
            grossByDay.put(day, BigDecimal.ZERO);
            countByDay.put(day, 0);
        }
        for (Order order : orders) {
            if ("CANCELLED".equals(order.getStatus())) {
                cancelled++;
                continue;
            }
            if (!"COMPLETED".equals(order.getStatus())) continue;
            Instant at = order.getCompletedAt() != null ? order.getCompletedAt()
                : order.getPlacedAt() != null ? order.getPlacedAt() : order.getCreatedAt();
            String day = LocalDate.ofInstant(at, ZoneOffset.UTC).format(fmt);
            grossByDay.merge(day, order.getGrandTotal(), BigDecimal::add);
            countByDay.merge(day, 1, Integer::sum);
            gross = gross.add(order.getGrandTotal());
            completed++;
        }
        List<SalesDay> daily = grossByDay.entrySet().stream()
            .map(entry -> new SalesDay(entry.getKey(), entry.getValue().setScale(2, RoundingMode.HALF_UP),
                countByDay.getOrDefault(entry.getKey(), 0)))
            .toList();
        BigDecimal avg = completed == 0 ? BigDecimal.ZERO
            : gross.divide(BigDecimal.valueOf(completed), 2, RoundingMode.HALF_UP);
        return new SalesReportResponse(range, gross.setScale(2, RoundingMode.HALF_UP), completed, cancelled, avg, daily);
    }

    public record CustomersReportResponse(int total, int createdInRange, int rangeDays) {
    }

    @Transactional(readOnly = true)
    public CustomersReportResponse customers(int days) {
        int range = Math.max(1, Math.min(days, 365));
        Instant from = Instant.now().minus(Duration.ofDays(range));
        List<Customer> all = customerRepository.findAll();
        int created = (int) all.stream()
            .filter(customer -> customer.getCreatedAt() != null && !customer.getCreatedAt().isBefore(from))
            .count();
        return new CustomersReportResponse(all.size(), created, range);
    }

    public record UsageLimit(int used, int limit) {
    }

    public record UsageResponse(Long tenantId, String tenantName, UsageLimit branches, UsageLimit employees,
                                UsageLimit menuItems, int orders, int customers, String planCode,
                                String subscriptionStatus) {
    }

    @Transactional(readOnly = true)
    public UsageResponse usage(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new com.restaurant.saas.common.exception.NotFoundException("Tenant " + tenantId + " not found"));
        SubscriptionPlan plan = subscriptionRepository.findByTenantId(tenantId)
            .flatMap(sub -> planRepository.findById(sub.getPlanId()))
            .orElse(null);
        String status = subscriptionRepository.findByTenantId(tenantId)
            .map(Subscription::getStatus).orElse(null);
        long branches = branchRepository.findAll().stream().filter(b -> tenantId.equals(b.getTenantId())).count();
        long employees = employeeRepository.findAll().stream()
            .filter(e -> tenantId.equals(e.getTenantId())).count();
        long menuItems = menuItemRepository.findAll().stream()
            .filter(item -> tenantId.equals(item.getTenantId())).count();
        long orders = orderRepository.findAll().stream().filter(o -> tenantId.equals(o.getTenantId())).count();
        long customers = customerRepository.findAll().stream()
            .filter(c -> tenantId.equals(c.getTenantId())).count();
        return new UsageResponse(
            tenantId, tenant.getName(),
            new UsageLimit((int) branches, plan == null ? null : plan.getMaxBranches()),
            new UsageLimit((int) employees, plan == null ? null : plan.getMaxEmployees()),
            new UsageLimit((int) menuItems, plan == null ? null : plan.getMaxMenuItems()),
            (int) orders, (int) customers,
            plan == null ? null : plan.getCode(), status);
    }

    static <T> PageResponse<T> toPage(List<T> all, int page, int size) {
        int total = all.size();
        int safeSize = Math.max(1, size);
        int from = Math.max(0, page) * safeSize;
        List<T> slice = from >= total ? List.of()
            : new ArrayList<>(all.subList(from, Math.min(from + safeSize, total)));
        int totalPages = (int) Math.ceil(total / (double) safeSize);
        return new PageResponse<>(slice, new PageResponse.PageMeta(Math.max(0, page), safeSize, total, totalPages));
    }

    private static <T> int count(List<T> items, java.util.function.Function<T, String> extractor, String value) {
        return (int) items.stream().filter(item -> value.equals(extractor.apply(item))).count();
    }
}
