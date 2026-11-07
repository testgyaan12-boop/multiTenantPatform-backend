package com.restaurant.saas.restaurant.repository;

import com.restaurant.saas.restaurant.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Optional<Restaurant> findByTenantId(Long tenantId);

    boolean existsByTenantIdAndCode(Long tenantId, String code);
}
