package com.restaurant.saas.menu.repository;

import com.restaurant.saas.menu.entity.Modifier;
import com.restaurant.saas.menu.entity.ModifierGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ModifierGroupRepository extends JpaRepository<ModifierGroup, Long> {

    List<ModifierGroup> findByTenantIdOrderBySortOrderAscNameAsc(Long tenantId);

    Optional<ModifierGroup> findByIdAndTenantId(Long id, Long tenantId);

    @Query(value = "SELECT modifier_group_id FROM menu_item_modifier_groups WHERE menu_item_id = :menuItemId",
        nativeQuery = true)
    List<Long> findGroupIdsForMenuItem(@Param("menuItemId") Long menuItemId);
}
