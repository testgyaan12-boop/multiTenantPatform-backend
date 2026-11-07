package com.restaurant.saas.payment.repository;

import com.restaurant.saas.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdAndTenantId(Long id, Long tenantId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByOrderIdAndStatus(Long orderId, String status);
}
