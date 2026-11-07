package com.restaurant.saas.subscription.repository;

import com.restaurant.saas.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByTenantId(Long tenantId);
}
