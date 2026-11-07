package com.restaurant.saas.payment.repository;

import com.restaurant.saas.payment.entity.PaymentWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {

    Optional<PaymentWebhookEvent> findByProviderAndEventId(String provider, String eventId);
}
