package com.restaurant.saas.identity.repository;

import com.restaurant.saas.identity.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    @Query("""
        SELECT DISTINCT r FROM Role r
        LEFT JOIN FETCH r.permissions
        WHERE r.code = :code AND r.tenantId IS NULL
        """)
    Optional<Role> findSystemRoleWithPermissions(@Param("code") String code);

    Optional<Role> findByCodeAndTenantIdIsNull(String code);
}
