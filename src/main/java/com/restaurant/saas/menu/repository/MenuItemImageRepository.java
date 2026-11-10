package com.restaurant.saas.menu.repository;

import com.restaurant.saas.menu.entity.MenuItemImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface MenuItemImageRepository extends JpaRepository<MenuItemImage, Long> {

    @Query("""
        SELECT i FROM MenuItemImage i
        WHERE i.tenantId = :tenantId AND i.menuItemId = :menuItemId
        ORDER BY i.sortOrder ASC, i.id ASC
        """)
    List<MenuItemImage> findByTenantIdAndMenuItemId(
        @Param("tenantId") Long tenantId, @Param("menuItemId") Long menuItemId);

    @Query("""
        SELECT i FROM MenuItemImage i
        WHERE i.tenantId = :tenantId AND i.menuItemId IN :itemIds
        ORDER BY i.menuItemId ASC, i.sortOrder ASC, i.id ASC
        """)
    List<MenuItemImage> findByTenantIdAndMenuItemIdIn(
        @Param("tenantId") Long tenantId, @Param("itemIds") Collection<Long> itemIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM MenuItemImage i WHERE i.menuItemId = :menuItemId")
    void deleteByMenuItemId(@Param("menuItemId") Long menuItemId);
}
