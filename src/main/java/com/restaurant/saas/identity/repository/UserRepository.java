package com.restaurant.saas.identity.repository;

import com.restaurant.saas.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.roles r
        LEFT JOIN FETCH r.permissions
        WHERE u.email = :email
        """)
    Optional<User> findActiveByEmailWithAuthorities(@Param("email") String email);

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.roles r
        LEFT JOIN FETCH r.permissions
        WHERE u.id = :id
        """)
    Optional<User> findActiveByIdWithAuthorities(@Param("id") Long id);

    boolean existsByEmail(String email);

    List<User> findByTenantId(Long tenantId);

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.roles r
        LEFT JOIN FETCH r.permissions
        WHERE u.id IN :ids
        """)
    List<User> findAllByIdWithAuthorities(@Param("ids") Collection<Long> ids);

    Optional<User> findByInviteToken(String inviteToken);
}
