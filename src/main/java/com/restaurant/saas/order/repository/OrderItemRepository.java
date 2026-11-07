package com.restaurant.saas.order.repository;

import com.restaurant.saas.order.entity.OrderItem;
import com.restaurant.saas.order.entity.OrderItemModifier;
import com.restaurant.saas.order.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId);
}
