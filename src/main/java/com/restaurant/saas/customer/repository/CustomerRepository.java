package com.restaurant.saas.customer.repository;

import com.restaurant.saas.customer.entity.Customer;
import com.restaurant.saas.customer.entity.GuestSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByTenantIdAndEmail(Long tenantId, String email);
}
