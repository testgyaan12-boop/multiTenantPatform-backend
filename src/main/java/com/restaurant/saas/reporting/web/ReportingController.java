package com.restaurant.saas.reporting.web;

import com.restaurant.saas.common.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportingController {

    @PersistenceContext
    private EntityManager entityManager;

    public record SalesSummary(
        long orderCount,
        BigDecimal grossRevenue,
        BigDecimal averageTicket,
        List<TopItem> topItems
    ) {
    }

    public record TopItem(String name, long quantity, BigDecimal revenue) {
    }

    public record CustomerSummary(
        long customers,
        long previousCustomers,
        long registeredCustomers
    ) {
    }

    @GetMapping("/customers")
    @PreAuthorize("hasAuthority('report.read')")
    public CustomerSummary customers(
        @RequestParam(defaultValue = "30") int days,
        @RequestParam(required = false) Long branchId) {
        Long tenantId = TenantContext.requireTenant();
        int window = Math.min(Math.max(days, 1), 365);
        Instant since = Instant.now().minus(window, ChronoUnit.DAYS);
        Instant previousSince = Instant.now().minus(2L * window, ChronoUnit.DAYS);

        Object[] row = (Object[]) entityManager.createNativeQuery("""
                SELECT
                    COUNT(DISTINCT CASE
                        WHEN created_at >= :since AND (
                            customer_id IS NOT NULL OR customer_phone IS NOT NULL OR customer_email IS NOT NULL
                        ) THEN CASE
                            WHEN customer_id IS NOT NULL THEN CONCAT('c:', customer_id)
                            WHEN customer_phone IS NOT NULL THEN CONCAT('p:', LOWER(customer_phone))
                            WHEN customer_email IS NOT NULL THEN CONCAT('e:', LOWER(customer_email))
                        END
                    END) AS current_customers,
                    COUNT(DISTINCT CASE
                        WHEN created_at < :since AND (
                            customer_id IS NOT NULL OR customer_phone IS NOT NULL OR customer_email IS NOT NULL
                        ) THEN CASE
                            WHEN customer_id IS NOT NULL THEN CONCAT('c:', customer_id)
                            WHEN customer_phone IS NOT NULL THEN CONCAT('p:', LOWER(customer_phone))
                            WHEN customer_email IS NOT NULL THEN CONCAT('e:', LOWER(customer_email))
                        END
                    END) AS previous_customers
                FROM orders
                WHERE tenant_id = :tenantId
                  AND is_active = 1 AND is_delete = 0
                  AND status NOT IN ('CANCELLED', 'DRAFT')
                  AND created_at >= :previousSince
                  AND (:branchId IS NULL OR branch_id = :branchId)
                """)
            .setParameter("tenantId", tenantId)
            .setParameter("since", since)
            .setParameter("previousSince", previousSince)
            .setParameter("branchId", branchId)
            .getSingleResult();

        Number registered = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM customers
                WHERE tenant_id = :tenantId AND is_active = 1 AND is_delete = 0
                """)
            .setParameter("tenantId", tenantId)
            .getSingleResult();

        return new CustomerSummary(
            ((Number) row[0]).longValue(),
            ((Number) row[1]).longValue(),
            registered.longValue()
        );
    }

    @GetMapping("/sales")
    @PreAuthorize("hasAuthority('report.read')")
    public SalesSummary sales(
        @RequestParam(defaultValue = "30") int days,
        @RequestParam(required = false) Long branchId) {
        Long tenantId = TenantContext.requireTenant();
        Instant since = Instant.now().minus(Math.min(Math.max(days, 1), 365), ChronoUnit.DAYS);

        Object[] summary = (Object[]) entityManager.createNativeQuery("""
                SELECT COUNT(*) AS order_count,
                       COALESCE(SUM(grand_total), 0) AS gross,
                       COALESCE(AVG(grand_total), 0) AS avg_ticket
                FROM orders
                WHERE tenant_id = :tenantId
                  AND is_active = 1 AND is_delete = 0
                  AND status NOT IN ('CANCELLED', 'DRAFT')
                  AND created_at >= :since
                  AND (:branchId IS NULL OR branch_id = :branchId)
                """)
            .setParameter("tenantId", tenantId)
            .setParameter("since", since)
            .setParameter("branchId", branchId)
            .getSingleResult();

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT oi.name_snapshot, SUM(oi.quantity) AS qty, SUM(oi.line_total) AS revenue
                FROM order_items oi
                JOIN orders o ON o.id = oi.order_id
                WHERE o.tenant_id = :tenantId
                  AND oi.is_active = 1 AND oi.is_delete = 0
                  AND o.is_active = 1 AND o.is_delete = 0
                  AND o.status NOT IN ('CANCELLED', 'DRAFT')
                  AND o.created_at >= :since
                  AND (:branchId IS NULL OR o.branch_id = :branchId)
                GROUP BY oi.name_snapshot
                ORDER BY revenue DESC
                LIMIT 10
                """)
            .setParameter("tenantId", tenantId)
            .setParameter("since", since)
            .setParameter("branchId", branchId)
            .getResultList();

        List<TopItem> topItems = rows.stream()
            .map(row -> new TopItem(
                (String) row[0],
                ((Number) row[1]).longValue(),
                new BigDecimal(row[2].toString())))
            .toList();

        return new SalesSummary(
            ((Number) summary[0]).longValue(),
            new BigDecimal(summary[1].toString()),
            new BigDecimal(summary[2].toString()),
            topItems
        );
    }
}
