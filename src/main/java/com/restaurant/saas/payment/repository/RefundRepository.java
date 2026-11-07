package com.restaurant.saas.payment.repository;

import com.restaurant.saas.payment.entity.PaymentWebhookEvent;
import com.restaurant.saas.payment.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    Optional<Refund> findByIdAndTenantId(Long id, Long tenantId);
}
