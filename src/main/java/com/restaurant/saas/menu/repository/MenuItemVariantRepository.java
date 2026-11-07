package com.restaurant.saas.menu.repository;

import com.restaurant.saas.menu.entity.MenuItemVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuItemVariantRepository extends JpaRepository<MenuItemVariant, Long> {

    List<MenuItemVariant> findByMenuItemIdOrderBySortOrderAsc(Long menuItemId);

    Optional<MenuItemVariant> findByIdAndTenantId(Long id, Long tenantId);
}
