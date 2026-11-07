package com.restaurant.saas.customer.repository;

import com.restaurant.saas.customer.entity.GuestSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuestSessionRepository extends JpaRepository<GuestSession, Long> {

    Optional<GuestSession> findBySessionToken(String sessionToken);
}
