package com.restaurant.saas.cart.web;

import com.restaurant.saas.cart.dto.AddCartItemRequest;
import com.restaurant.saas.cart.dto.CartResponse;
import com.restaurant.saas.cart.dto.UpdateCartItemRequest;
import com.restaurant.saas.cart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/cart")
public class PublicCartController {

    private static final String SESSION_HEADER = "X-Guest-Session";

    private final CartService cartService;

    public PublicCartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse get(@RequestHeader(SESSION_HEADER) String sessionToken) {
        return cartService.getOrCreate(sessionToken);
    }

    @PostMapping("/items")
    public CartResponse addItem(
        @RequestHeader(SESSION_HEADER) String sessionToken,
        @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(sessionToken, request);
    }

    @PutMapping("/items/{itemId}")
    public CartResponse updateItem(
        @RequestHeader(SESSION_HEADER) String sessionToken,
        @PathVariable Long itemId,
        @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItem(sessionToken, itemId, request.quantity());
    }

    @DeleteMapping
    public CartResponse clear(@RequestHeader(SESSION_HEADER) String sessionToken) {
        return cartService.clear(sessionToken);
    }

    @DeleteMapping("/items/{itemId}")
    public CartResponse removeItem(
        @RequestHeader(SESSION_HEADER) String sessionToken,
        @PathVariable Long itemId) {
        return cartService.removeItem(sessionToken, itemId);
    }
}
