package com.restaurant.saas.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.saas.branch.entity.Branch;
import com.restaurant.saas.branch.repository.BranchRepository;
import com.restaurant.saas.cart.dto.ModifierSelection;
import com.restaurant.saas.cart.entity.Cart;
import com.restaurant.saas.cart.entity.CartItem;
import com.restaurant.saas.cart.repository.CartItemRepository;
import com.restaurant.saas.cart.repository.CartRepository;
import com.restaurant.saas.cart.service.CartService;
import com.restaurant.saas.common.exception.ConflictException;
import com.restaurant.saas.common.exception.NotFoundException;
import com.restaurant.saas.customer.entity.GuestSession;
import com.restaurant.saas.customer.service.GuestSessionService;
import com.restaurant.saas.diningtable.entity.DiningTable;
import com.restaurant.saas.diningtable.repository.DiningTableRepository;
import com.restaurant.saas.menu.entity.MenuItem;
import com.restaurant.saas.menu.repository.MenuItemRepository;
import com.restaurant.saas.order.dto.CheckoutRequest;
import com.restaurant.saas.order.dto.OrderResponse;
import com.restaurant.saas.order.entity.Order;
import com.restaurant.saas.order.entity.OrderItem;
import com.restaurant.saas.order.entity.OrderItemModifier;
import com.restaurant.saas.order.entity.OrderStatusHistory;
import com.restaurant.saas.order.repository.OrderItemModifierRepository;
import com.restaurant.saas.order.repository.OrderItemRepository;
import com.restaurant.saas.order.repository.OrderRepository;
import com.restaurant.saas.order.repository.OrderStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderItemModifierRepository orderItemModifierRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartService cartService;
    private final GuestSessionService guestSessionService;
    private final MenuItemRepository menuItemRepository;
    private final BranchRepository branchRepository;
    private final DiningTableRepository diningTableRepository;
    private final ObjectMapper objectMapper;

    public OrderService(
        OrderRepository orderRepository,
        OrderItemRepository orderItemRepository,
        OrderItemModifierRepository orderItemModifierRepository,
        OrderStatusHistoryRepository historyRepository,
        CartRepository cartRepository,
        CartItemRepository cartItemRepository,
        CartService cartService,
        GuestSessionService guestSessionService,
        MenuItemRepository menuItemRepository,
        BranchRepository branchRepository,
        DiningTableRepository diningTableRepository,
        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderItemModifierRepository = orderItemModifierRepository;
        this.historyRepository = historyRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.cartService = cartService;
        this.guestSessionService = guestSessionService;
        this.menuItemRepository = menuItemRepository;
        this.branchRepository = branchRepository;
        this.diningTableRepository = diningTableRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderResponse checkout(String sessionToken, CheckoutRequest request) {
        GuestSession session = guestSessionService.requireActive(sessionToken);
        Cart cart = cartRepository.findByGuestSessionIdAndStatus(session.getId(), "OPEN")
            .orElseThrow(() -> new ConflictException(
                "cart_empty", "Add at least one item before checkout"));
        List<CartItem> cartItems = cartItemRepository.findByCartIdOrderByIdAsc(cart.getId());
        if (cartItems.isEmpty()) {
            throw new ConflictException("cart_empty", "Add at least one item before checkout");
        }

        if (request.tipAmount() != null && request.tipAmount().compareTo(BigDecimal.ZERO) > 0) {
            cart.setTipTotal(request.tipAmount());
            cart = cartService.recalculate(cart);
        }

        Order order = new Order();
        order.setTenantId(session.getTenantId());
        order.setBranchId(session.getBranchId());
        order.setTableId(session.getTableId());
        order.setGuestSessionId(session.getId());
        order.setOrderNumber(generateOrderNumber(session.getBranchId()));
        order.setStatus("PLACED");
        order.setPaymentStatus("PENDING");
        order.setSource(session.getTableId() == null ? "PICKUP" : "QR");
        order.setCurrency(cart.getCurrency());
        order.setSubtotal(cart.getSubtotal());
        order.setTaxTotal(cart.getTaxTotal());
        order.setServiceCharge(cart.getServiceCharge());
        order.setTipTotal(cart.getTipTotal());
        order.setDiscountTotal(cart.getDiscountTotal());
        order.setGrandTotal(cart.getGrandTotal());
        order.setCustomerName(request.customerName());
        order.setCustomerEmail(request.customerEmail());
        order.setCustomerPhone(request.customerPhone());
        order.setNotes(request.notes());
        order.setPlacedAt(Instant.now());
        order = orderRepository.save(order);

        for (CartItem cartItem : cartItems) {
            MenuItem menuItem = menuItemRepository.findById(cartItem.getMenuItemId()).orElse(null);
            OrderItem orderItem = new OrderItem();
            orderItem.setTenantId(session.getTenantId());
            orderItem.setOrderId(order.getId());
            orderItem.setMenuItemId(cartItem.getMenuItemId());
            orderItem.setVariantId(cartItem.getVariantId());
            orderItem.setNameSnapshot(menuItem == null ? "Item" : menuItem.getName());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setModifierTotal(cartItem.getModifierTotal());
            orderItem.setLineTotal(cartItem.getLineTotal());
            orderItem.setNotes(cartItem.getNotes());
            orderItem.setStatus("PLACED");
            orderItemRepository.save(orderItem);

            for (ModifierSelection selection : ModifierSelection.read(objectMapper, cartItem.getModifierSelections())) {
                OrderItemModifier modifier = new OrderItemModifier();
                modifier.setTenantId(session.getTenantId());
                modifier.setOrderItemId(orderItem.getId());
                modifier.setModifierId(selection.modifierId());
                modifier.setNameSnapshot(selection.name());
                modifier.setPriceDelta(selection.priceDelta());
                orderItemModifierRepository.save(modifier);
            }
        }

        recordStatus(order, null, "PLACED", request.customerName());

        cart.setStatus("CHECKED_OUT");
        cartRepository.save(cart);

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse track(String publicToken) {
        Order order = orderRepository.findByPublicToken(publicToken)
            .orElseThrow(() -> new NotFoundException("Order not found"));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getForTenant(Long tenantId, Long orderId) {
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
            .orElseThrow(() -> new NotFoundException("Order", orderId));
        return toResponse(order);
    }

    @Transactional
    public OrderResponse updateStatus(Long tenantId, Long orderId, String newStatus, String actor) {
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
            .orElseThrow(() -> new NotFoundException("Order", orderId));
        String previous = order.getStatus();
        validateTransition(previous, newStatus);

        order.setStatus(newStatus);
        Instant now = Instant.now();
        switch (newStatus) {
            case "CONFIRMED" -> order.setAcceptedAt(now);
            case "READY" -> order.setReadyAt(now);
            case "COMPLETED" -> order.setCompletedAt(now);
            case "CANCELLED" -> order.setCancelledAt(now);
            default -> {
            }
        }
        orderRepository.save(order);
        recordStatus(order, previous, newStatus, actor);
        return toResponse(order);
    }

    private static void validateTransition(String current, String next) {
        boolean valid = switch (current) {
            case "PLACED" -> next.equals("CONFIRMED") || next.equals("CANCELLED");
            case "CONFIRMED" -> next.equals("PREPARING") || next.equals("CANCELLED");
            case "PREPARING" -> next.equals("READY") || next.equals("CANCELLED");
            case "READY" -> next.equals("SERVED") || next.equals("COMPLETED");
            case "SERVED" -> next.equals("COMPLETED");
            default -> false;
        };
        if (!valid) {
            throw new ConflictException(
                "invalid_status_transition", "Cannot change order status from " + current + " to " + next);
        }
    }

    private void recordStatus(Order order, String from, String to, String actor) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setTenantId(order.getTenantId());
        history.setOrderId(order.getId());
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setChangedBy(actor);
        historyRepository.save(history);
    }

    private String generateOrderNumber(Long branchId) {
        Branch branch = branchRepository.findById(branchId).orElse(null);
        String prefix = branch == null ? "ORD" : branch.getCode();
        String date = DateTimeFormatter.ofPattern("yyMMdd")
            .withZone(ZoneOffset.UTC).format(Instant.now());
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return prefix + "-" + date + "-" + random;
    }

    private OrderResponse toResponse(Order order) {
        String tableNumber = null;
        if (order.getTableId() != null) {
            tableNumber = diningTableRepository.findById(order.getTableId())
                .map(DiningTable::getTableNumber)
                .orElse(null);
        }

        List<OrderResponse.OrderItemResponse> items = orderItemRepository
            .findByOrderIdOrderByIdAsc(order.getId()).stream()
            .map(item -> new OrderResponse.OrderItemResponse(
                item.getId(),
                item.getNameSnapshot(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal(),
                item.getStatus(),
                item.getNotes(),
                orderItemModifierRepository.findByOrderItemIdOrderByIdAsc(item.getId()).stream()
                    .map(modifier -> new OrderResponse.ModifierView(
                        modifier.getNameSnapshot(), modifier.getPriceDelta()))
                    .toList()))
            .toList();

        return new OrderResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getPublicToken(),
            order.getStatus(),
            order.getPaymentStatus(),
            order.getSource(),
            order.getTableId(),
            tableNumber,
            order.getCurrency(),
            order.getSubtotal(),
            order.getTaxTotal(),
            order.getServiceCharge(),
            order.getTipTotal(),
            order.getDiscountTotal(),
            order.getGrandTotal(),
            order.getCustomerName(),
            order.getCustomerPhone(),
            order.getNotes(),
            order.getPlacedAt(),
            order.getAcceptedAt(),
            order.getReadyAt(),
            order.getCompletedAt(),
            order.getCancelledAt(),
            order.getCreatedAt(),
            items
        );
    }

    @Transactional(readOnly = true)
    public List<com.restaurant.saas.order.dto.OrderTimelineEntry> timeline(Long tenantId, Long orderId) {
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
            .orElseThrow(() -> new NotFoundException("Order", orderId));
        return historyRepository.findByOrderIdOrderByCreatedAtAsc(order.getId()).stream()
            .map(entry -> new com.restaurant.saas.order.dto.OrderTimelineEntry(
                entry.getFromStatus(),
                entry.getToStatus(),
                entry.getCreatedAt(),
                entry.getChangedBy()))
            .toList();
    }
}
