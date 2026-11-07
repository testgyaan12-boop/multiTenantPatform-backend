package com.restaurant.saas.identity.repository;

import com.restaurant.saas.identity.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    List<Permission> findByIdIn(Collection<Long> ids);
}
