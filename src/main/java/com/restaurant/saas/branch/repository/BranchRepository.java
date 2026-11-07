package com.restaurant.saas.branch.repository;

import com.restaurant.saas.branch.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    List<Branch> findByTenantIdOrderByNameAsc(Long tenantId);

    Optional<Branch> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndCode(Long tenantId, String code);
}
