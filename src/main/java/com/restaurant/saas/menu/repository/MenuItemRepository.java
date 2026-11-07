package com.restaurant.saas.menu.repository;

import com.restaurant.saas.menu.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByTenantIdOrderBySortOrderAscNameAsc(Long tenantId);

    List<MenuItem> findByTenantIdAndCategoryIdOrderBySortOrderAscNameAsc(Long tenantId, Long categoryId);

    Optional<MenuItem> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndSku(Long tenantId, String sku);

    long countByTenantId(Long tenantId);

    long countByTenantIdAndCategoryId(Long tenantId, Long categoryId);

    @Query("""
        SELECT i FROM MenuItem i
        WHERE i.tenantId = :tenantId
          AND (:categoryId IS NULL OR i.categoryId = :categoryId)
          AND (LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(i.sku) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY i.sortOrder ASC, i.name ASC
        """)
    List<MenuItem> searchByTenant(
        @Param("tenantId") Long tenantId,
        @Param("categoryId") Long categoryId,
        @Param("search") String search);

    @Query("""
        SELECT i FROM MenuItem i
        WHERE i.tenantId = :tenantId AND i.isAvailable = true
          AND (:categoryId IS NULL OR i.categoryId = :categoryId)
          AND (:search IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY i.sortOrder ASC, i.name ASC
        """)
    List<MenuItem> searchPublic(
        @Param("tenantId") Long tenantId,
        @Param("categoryId") Long categoryId,
        @Param("search") String search);
}
