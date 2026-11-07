package com.restaurant.saas.order.web;

import com.restaurant.saas.order.dto.CheckoutRequest;
import com.restaurant.saas.order.dto.OrderResponse;
import com.restaurant.saas.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
public class PublicOrderController {

    private static final String SESSION_HEADER = "X-Guest-Session";

    private final OrderService orderService;

    public PublicOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> checkout(
        @RequestHeader(SESSION_HEADER) String sessionToken,
        @Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(orderService.checkout(sessionToken, request));
    }

    @GetMapping("/orders/{publicToken}")
    public OrderResponse track(@PathVariable String publicToken) {
        return orderService.track(publicToken);
    }
}
