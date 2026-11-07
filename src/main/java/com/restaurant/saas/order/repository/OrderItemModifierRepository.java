package com.restaurant.saas.order.repository;

import com.restaurant.saas.order.entity.OrderItemModifier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemModifierRepository extends JpaRepository<OrderItemModifier, Long> {

    List<OrderItemModifier> findByOrderItemIdOrderByIdAsc(Long orderItemId);
}
