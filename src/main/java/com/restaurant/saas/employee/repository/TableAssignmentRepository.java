package com.restaurant.saas.employee.repository;

import com.restaurant.saas.employee.entity.TableAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TableAssignmentRepository extends JpaRepository<TableAssignment, Long> {

    List<TableAssignment> findByTenantIdAndBranchIdAndIsActiveTrue(Long tenantId, Long branchId);

    List<TableAssignment> findByTenantIdAndIsActiveTrue(Long tenantId);

    Optional<TableAssignment> findByTenantIdAndTableIdAndIsActiveTrue(Long tenantId, Long tableId);

    @Query("""
        SELECT a FROM TableAssignment a
        WHERE a.tenantId = :tenantId AND a.isActive = true AND a.employeeId = :employeeId
        """)
    List<TableAssignment> findActiveByEmployee(@Param("tenantId") Long tenantId, @Param("employeeId") Long employeeId);

    long countByTenantIdAndBranchIdAndIsActiveTrue(Long tenantId, Long branchId);
}
