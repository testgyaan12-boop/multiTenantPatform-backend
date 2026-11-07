package com.restaurant.saas.order.web;

import com.restaurant.saas.authorization.security.AuthenticatedPrincipal;
import com.restaurant.saas.common.tenant.TenantContext;
import com.restaurant.saas.common.web.PageResponse;
import com.restaurant.saas.common.web.PageableFactory;
import com.restaurant.saas.order.dto.OrderResponse;
import com.restaurant.saas.order.repository.OrderRepository;
import com.restaurant.saas.order.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    public OrderController(OrderService orderService, OrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('order.read')")
    public PageResponse<OrderResponse> list(
        @RequestParam(required = false) Long branchId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String paymentStatus,
        @RequestParam(required = false) String source,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Integer days,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        Long tenantId = TenantContext.requireTenant();
        java.time.Instant since = (days == null || days < 0)
            ? java.time.Instant.EPOCH
            : java.time.Instant.now().minus(days, java.time.temporal.ChronoUnit.DAYS);
        String term = (search == null || search.isBlank()) ? null : search.trim();
        var orders = orderRepository.search(
            tenantId, branchId, blankToNull(status), blankToNull(paymentStatus),
            blankToNull(source), term, since,
            PageableFactory.of(page, size, "createdAt", "desc"));
        return PageResponse.of(orders.map(order -> orderService.getForTenant(tenantId, order.getId())));
    }

    @GetMapping("/counts")
    @PreAuthorize("hasAuthority('order.read')")
    public com.restaurant.saas.order.dto.OrderCountsResponse counts(
        @RequestParam(required = false) Long branchId,
        @RequestParam(required = false) Integer days) {
        Long tenantId = TenantContext.requireTenant();
        java.time.Instant since = (days == null || days < 0)
            ? java.time.Instant.EPOCH
            : java.time.Instant.now().minus(days, java.time.temporal.ChronoUnit.DAYS);
        java.util.Map<String, Long> byStatus = new java.util.LinkedHashMap<>();
        long total = 0;
        for (Object[] row : orderRepository.statusCounts(tenantId, branchId, since)) {
            String status = (String) row[0];
            long count = ((Number) row[1]).longValue();
            byStatus.put(status, count);
            total += count;
        }
        return new com.restaurant.saas.order.dto.OrderCountsResponse(total, byStatus);
    }

    @GetMapping("/{id}/timeline")
    @PreAuthorize("hasAuthority('order.read')")
    public java.util.List<com.restaurant.saas.order.dto.OrderTimelineEntry> timeline(@PathVariable Long id) {
        return orderService.timeline(TenantContext.requireTenant(), id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('order.read')")
    public OrderResponse get(@PathVariable Long id) {
        return orderService.getForTenant(TenantContext.requireTenant(), id);
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('order.manage')")
    public OrderResponse updateStatus(
        @PathVariable Long id,
        @RequestParam String status,
        @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return orderService.updateStatus(
            TenantContext.requireTenant(), id, status,
            principal == null ? "system" : principal.email());
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
