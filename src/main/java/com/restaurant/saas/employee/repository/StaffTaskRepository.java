package com.restaurant.saas.employee.repository;

import com.restaurant.saas.employee.entity.StaffTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StaffTaskRepository
    extends JpaRepository<StaffTask, Long>, JpaSpecificationExecutor<StaffTask> {

    Optional<StaffTask> findByIdAndTenantId(Long id, Long tenantId);

    List<StaffTask> findByTenantIdAndBranchIdAndAssignedToEmployeeIdAndStatusIn(
        Long tenantId, Long branchId, Long employeeId, List<String> statuses);

    long countByTenantIdAndBranchIdAndStatusIn(Long tenantId, Long branchId, List<String> statuses);

    @Query("""
        SELECT t FROM StaffTask t
        WHERE t.tenantId = :tenantId
          AND t.branchId = :branchId
          AND t.assignedToEmployeeId = :employeeId
          AND t.status IN ('ASSIGNED', 'ACCEPTED', 'IN_PROGRESS', 'BLOCKED')
          AND t.id <> :excludeId
        """)
    List<StaffTask> findActiveForEmployee(
        @Param("tenantId") Long tenantId,
        @Param("branchId") Long branchId,
        @Param("employeeId") Long employeeId,
        @Param("excludeId") Long excludeId);

    @Query("""
        SELECT t FROM StaffTask t
        WHERE t.tenantId = :tenantId
          AND t.status IN ('ASSIGNED', 'ACCEPTED', 'IN_PROGRESS', 'BLOCKED')
          AND t.dueAt IS NOT NULL AND t.dueAt < :now
        """)
    List<StaffTask> findOverdue(@Param("tenantId") Long tenantId, @Param("now") Instant now);
}
