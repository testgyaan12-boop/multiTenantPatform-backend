package com.restaurant.saas.inquiry.service;

import com.restaurant.saas.common.exception.NotFoundException;
import com.restaurant.saas.common.tenant.TenantContext;
import com.restaurant.saas.inquiry.entity.Inquiry;
import com.restaurant.saas.inquiry.repository.InquiryRepository;
import com.restaurant.saas.order.entity.Order;
import com.restaurant.saas.order.repository.OrderRepository;
import com.restaurant.saas.branch.entity.Branch;
import com.restaurant.saas.branch.repository.BranchRepository;
import com.restaurant.saas.restaurant.entity.Restaurant;
import com.restaurant.saas.restaurant.repository.RestaurantRepository;
import com.restaurant.saas.superadmin.service.StoreSettingsService;
import com.restaurant.saas.tenant.entity.Tenant;
import com.restaurant.saas.tenant.repository.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restaurant.saas.common.exception.ApiException;
import com.restaurant.saas.common.exception.ConflictException;
import com.restaurant.saas.common.web.PageResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class InquiryService {

    private static final List<String> STATUSES = List.of("NEW", "CONTACTED", "QUALIFIED", "CLOSED", "LOST");

    private final InquiryRepository inquiryRepository;
    private final TenantRepository tenantRepository;
    private final RestaurantRepository restaurantRepository;
    private final BranchRepository branchRepository;
    private final OrderRepository orderRepository;
    private final StoreSettingsService storeSettingsService;
    private final com.restaurant.saas.menu.repository.MenuItemRepository menuItemRepository;

    public InquiryService(
        InquiryRepository inquiryRepository,
        TenantRepository tenantRepository,
        RestaurantRepository restaurantRepository,
        BranchRepository branchRepository,
        OrderRepository orderRepository,
        StoreSettingsService storeSettingsService,
        com.restaurant.saas.menu.repository.MenuItemRepository menuItemRepository) {
        this.inquiryRepository = inquiryRepository;
        this.tenantRepository = tenantRepository;
        this.restaurantRepository = restaurantRepository;
        this.branchRepository = branchRepository;
        this.orderRepository = orderRepository;
        this.storeSettingsService = storeSettingsService;
        this.menuItemRepository = menuItemRepository;
    }

    public record InquiryRequest(
        String name,
        String phone,
        String email,
        String message,
        Long itemId,
        String sourceUrl
    ) {
    }

    public record BookingView(boolean required, BigDecimal amount, String orderPublicToken) {
    }

    public record InquiryCreatedView(Long id, String status, BookingView booking) {
    }

    public record InquiryView(
        Long id,
        Long itemId,
        String itemName,
        String name,
        String phone,
        String email,
        String message,
        String sourceUrl,
        String status,
        String bookingPaymentStatus,
        BigDecimal bookingAmount,
        Long orderId,
        String orderPublicToken,
        String notes,
        Instant createdAt
    ) {
    }

    public record InquiryUpdateRequest(String status, String notes) {
    }

    @Transactional
    public InquiryCreatedView createFromStore(String slug, InquiryRequest request) {
        Tenant tenant = tenantRepository.findBySlug(slug)
            .orElseThrow(() -> new NotFoundException("Store not found"));

        if (request.name() == null || request.name().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "name_required", "Name is required");
        }
        if (request.phone() == null || request.phone().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "phone_required", "Phone number is required");
        }
        StoreSettingsService.Booking booking = storeSettingsService.effectiveBooking(tenant.getId());

        Inquiry inquiry = new Inquiry();
        inquiry.setTenantId(tenant.getId());
        inquiry.setName(request.name().trim());
        inquiry.setPhone(request.phone().trim());
        inquiry.setEmail(request.email() == null || request.email().isBlank() ? null : request.email().trim());
        inquiry.setMessage(request.message() == null ? null : request.message().trim());
        inquiry.setSourceUrl(request.sourceUrl() == null ? null : request.sourceUrl().trim());
        if (request.itemId() != null) {
            menuItemRepository.findByIdAndTenantId(request.itemId(), tenant.getId())
                .orElseThrow(() -> new NotFoundException("Product not found"));
            inquiry.setItemId(request.itemId());
        }
        inquiry.setStatus("NEW");

        String bookingToken = null;
        if (booking.enabled() && booking.amount().compareTo(BigDecimal.ZERO) > 0) {
            Order order = createBookingOrder(tenant, inquiry, booking.amount());
            inquiry.setBookingPaymentStatus("PENDING");
            inquiry.setBookingAmount(booking.amount());
            inquiry.setOrderId(order.getId());
            bookingToken = order.getPublicToken();
        } else {
            inquiry.setBookingPaymentStatus("NONE");
        }

        Inquiry saved = inquiryRepository.save(inquiry);
        return new InquiryCreatedView(
            saved.getId(),
            saved.getStatus(),
            new BookingView(bookingToken != null, booking.amount(), bookingToken));
    }

    private Order createBookingOrder(Tenant tenant, Inquiry inquiry, BigDecimal amount) {
        List<Branch> branches = branchRepository.findByTenantIdOrderByNameAsc(tenant.getId());
        Branch branch = branches.stream()
            .filter(Branch::getIsDefault)
            .findFirst()
            .orElse(branches.isEmpty() ? null : branches.get(0));
        if (branch == null) {
            throw new ConflictException("no_branch", "Store has no branch to attach the booking to");
        }
        Order order = new Order();
        order.setTenantId(tenant.getId());
        order.setBranchId(branch.getId());
        order.setOrderNumber(generateOrderNumber(branch.getCode()));
        order.setStatus("PLACED");
        order.setPaymentStatus("PENDING");
        order.setSource("PICKUP");
        order.setCurrency(tenant.getCurrency());
        order.setSubtotal(amount);
        order.setGrandTotal(amount);
        order.setCustomerName(inquiry.getName());
        order.setCustomerPhone(inquiry.getPhone());
        order.setCustomerEmail(inquiry.getEmail());
        order.setNotes("Enquiry booking");
        order.setPlacedAt(Instant.now());
        return orderRepository.save(order);
    }

    private static String generateOrderNumber(String branchPrefix) {
        String prefix = branchPrefix == null || branchPrefix.isBlank() ? "ORD" : branchPrefix;
        String date = DateTimeFormatter.ofPattern("yyMMdd")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now());
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return prefix + "-" + date + "-" + random;
    }

    @Transactional(readOnly = true)
    public PageResponse<InquiryView> list(String status, String search, int page, int size) {
        Long tenantId = TenantContext.requireTenant();
        String normalizedStatus = status == null || status.isBlank() ? null : status.trim();
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        var pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return PageResponse.of(
            inquiryRepository.search(tenantId, normalizedStatus, normalizedSearch, pageable).map(this::toView));
    }

    @Transactional
    public InquiryView update(Long id, InquiryUpdateRequest request) {
        Long tenantId = TenantContext.requireTenant();
        Inquiry inquiry = inquiryRepository.findByIdAndTenantId(id, tenantId);
        if (inquiry == null) {
            throw new NotFoundException("Inquiry", id);
        }
        if (request.status() != null && !request.status().isBlank()) {
            String status = request.status().trim().toUpperCase();
            if (!STATUSES.contains(status)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_status",
                    "Status must be one of " + String.join(", ", STATUSES));
            }
            inquiry.setStatus(status);
        }
        if (request.notes() != null) {
            inquiry.setNotes(request.notes().isBlank() ? null : request.notes().trim());
        }
        return toView(inquiryRepository.save(inquiry));
    }

    private InquiryView toView(Inquiry inquiry) {
        String itemName = null;
        if (inquiry.getItemId() != null) {
            itemName = menuItemRepository.findByIdAndTenantId(inquiry.getItemId(), inquiry.getTenantId())
                .map(com.restaurant.saas.menu.entity.MenuItem::getName)
                .orElse(null);
        }
        String orderToken = null;
        if (inquiry.getOrderId() != null) {
            orderToken = orderRepository.findById(inquiry.getOrderId())
                .map(Order::getPublicToken)
                .orElse(null);
        }
        return new InquiryView(
            inquiry.getId(),
            inquiry.getItemId(),
            itemName,
            inquiry.getName(),
            inquiry.getPhone(),
            inquiry.getEmail(),
            inquiry.getMessage(),
            inquiry.getSourceUrl(),
            inquiry.getStatus(),
            inquiry.getBookingPaymentStatus(),
            inquiry.getBookingAmount(),
            inquiry.getOrderId(),
            orderToken,
            inquiry.getNotes(),
            inquiry.getCreatedAt());
    }
}
