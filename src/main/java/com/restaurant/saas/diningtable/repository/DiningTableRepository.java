package com.restaurant.saas.diningtable.repository;

import com.restaurant.saas.diningtable.entity.DiningSection;
import com.restaurant.saas.diningtable.entity.DiningTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {

    List<DiningTable> findByTenantIdAndBranchIdOrderByTableNumberAsc(Long tenantId, Long branchId);

    Optional<DiningTable> findByIdAndTenantId(Long id, Long tenantId);

    Optional<DiningTable> findByQrToken(String qrToken);

    boolean existsByBranchIdAndTableNumber(Long branchId, String tableNumber);
}
