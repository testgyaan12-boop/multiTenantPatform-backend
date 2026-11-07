package com.restaurant.saas.subscription.repository;

import com.restaurant.saas.subscription.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    @Query("select p from SubscriptionPlan p where p.isPublic = true and p.status = :status"
        + " and p.isDelete = false order by p.sortOrder asc")
    List<SubscriptionPlan> findPublicByStatus(@Param("status") String status);

    Optional<SubscriptionPlan> findByCode(String code);
}
