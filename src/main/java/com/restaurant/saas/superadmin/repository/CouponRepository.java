package com.restaurant.saas.superadmin.repository;

import com.restaurant.saas.superadmin.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    List<Coupon> findAllByOrderByCreatedAtDesc();
    Optional<Coupon> findFirstByCode(String code);
}
