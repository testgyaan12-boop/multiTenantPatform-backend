package com.restaurant.saas.menu.repository;

import com.restaurant.saas.menu.entity.MenuCategory;
import com.restaurant.saas.menu.entity.MenuItem;
import com.restaurant.saas.menu.entity.MenuItemVariant;
import com.restaurant.saas.menu.entity.Modifier;
import com.restaurant.saas.menu.entity.ModifierGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

    List<MenuCategory> findByTenantIdOrderBySortOrderAscNameAsc(Long tenantId);

    Optional<MenuCategory> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndRestaurantIdAndName(Long tenantId, Long restaurantId, String name);

    boolean existsByTenantIdAndRestaurantIdAndNameAndIdNot(
        Long tenantId, Long restaurantId, String name, Long id);
}
