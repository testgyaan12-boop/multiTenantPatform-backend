package com.restaurant.saas.employee.repository;

import com.restaurant.saas.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository
    extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    List<Employee> findByTenantIdAndBranchId(Long tenantId, Long branchId);

    List<Employee> findByTenantId(Long tenantId);

    Optional<Employee> findByIdAndTenantId(Long id, Long tenantId);

    Optional<Employee> findByUserId(Long userId);

    Optional<Employee> findByTenantIdAndEmployeeCode(Long tenantId, String employeeCode);

    boolean existsByTenantIdAndUserId(Long tenantId, Long userId);

    List<Employee> findByTenantIdAndBranchIdAndOnDutyTrue(Long tenantId, Long branchId);

    @Query(value = """
        SELECT r.code AS role_code, COUNT(DISTINCT e.id) AS cnt
        FROM employees e
        JOIN user_roles ur ON ur.user_id = e.user_id
        JOIN roles r ON r.id = ur.role_id
        WHERE e.tenant_id = :tenantId
          AND e.branch_id = :branchId
          AND e.status = 'ACTIVE'
          AND e.is_delete = 0
          AND e.is_active = 1
        GROUP BY r.code
        """, nativeQuery = true)
    List<Object[]> countActiveByRole(@Param("tenantId") Long tenantId, @Param("branchId") Long branchId);
}
