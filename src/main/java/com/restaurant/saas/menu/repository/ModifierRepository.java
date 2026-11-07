package com.restaurant.saas.menu.repository;

import com.restaurant.saas.menu.entity.Modifier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModifierRepository extends JpaRepository<Modifier, Long> {

    List<Modifier> findByModifierGroupIdOrderBySortOrderAsc(Long modifierGroupId);
}
