package com.restaurant.saas.subscription.repository;

import com.restaurant.saas.subscription.entity.PlanPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanPriceHistoryRepository extends JpaRepository<PlanPriceHistory, Long> {

    List<PlanPriceHistory> findByPlanIdOrderByCreatedAtDesc(Long planId);
}
