package com.restaurant.saas.cart.repository;

import com.restaurant.saas.cart.entity.Cart;
import com.restaurant.saas.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByGuestSessionIdAndStatus(Long guestSessionId, String status);

    Optional<Cart> findByIdAndTenantId(Long id, Long tenantId);
}
