package com.restaurant.saas.menu.repository;

import com.restaurant.saas.menu.entity.ItemAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface ItemAttributeRepository extends JpaRepository<ItemAttribute, Long> {

    @Query("""
        SELECT a FROM ItemAttribute a
        WHERE a.tenantId = :tenantId AND a.menuItemId = :menuItemId
        ORDER BY a.sortOrder ASC
        """)
    List<ItemAttribute> findByTenantIdAndMenuItemId(
        @Param("tenantId") Long tenantId, @Param("menuItemId") Long menuItemId);

    @Query("""
        SELECT a FROM ItemAttribute a
        WHERE a.tenantId = :tenantId AND a.menuItemId IN :itemIds
        ORDER BY a.menuItemId ASC, a.sortOrder ASC
        """)
    List<ItemAttribute> findByTenantIdAndMenuItemIdIn(
        @Param("tenantId") Long tenantId, @Param("itemIds") Collection<Long> itemIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM ItemAttribute a WHERE a.menuItemId = :menuItemId")
    void deleteByMenuItemId(@Param("menuItemId") Long menuItemId);
}
