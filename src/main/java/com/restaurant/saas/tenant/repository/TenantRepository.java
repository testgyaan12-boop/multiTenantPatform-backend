package com.restaurant.saas.tenant.repository;

import com.restaurant.saas.tenant.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findBySlug(String slug);

    @Query(value = "SELECT * FROM tenants WHERE slug = :slug", nativeQuery = true)
    Optional<Tenant> findBySlugIncludingDeleted(@Param("slug") String slug);

    boolean existsBySlug(String slug);

    @Query("""
        SELECT t FROM Tenant t
        WHERE (:search IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(t.slug) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<Tenant> search(@Param("search") String search, Pageable pageable);
}
