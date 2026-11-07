package com.restaurant.saas.diningtable.repository;

import com.restaurant.saas.diningtable.entity.DiningSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiningSectionRepository extends JpaRepository<DiningSection, Long> {

    List<DiningSection> findByTenantIdAndBranchIdOrderBySortOrderAsc(Long tenantId, Long branchId);
}
