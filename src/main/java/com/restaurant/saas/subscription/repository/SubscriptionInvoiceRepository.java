package com.restaurant.saas.subscription.repository;

import com.restaurant.saas.subscription.entity.SubscriptionInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionInvoiceRepository extends JpaRepository<SubscriptionInvoice, Long> {

    List<SubscriptionInvoice> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
