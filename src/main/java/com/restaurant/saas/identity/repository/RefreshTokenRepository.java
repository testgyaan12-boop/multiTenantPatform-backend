package com.restaurant.saas.identity.repository;

import com.restaurant.saas.identity.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Query("""
        SELECT t FROM RefreshToken t
        WHERE t.userId = :userId AND t.revokedAt IS NULL AND t.expiresAt > :now
        ORDER BY t.createdAt DESC
        """)
    List<RefreshToken> findActiveForUser(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now, t.isActive = false WHERE t.id = :id AND t.userId = :userId AND t.revokedAt IS NULL")
    int revokeByIdForUser(@Param("id") Long id, @Param("userId") Long userId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now, t.isActive = false WHERE t.userId = :userId AND t.revokedAt IS NULL AND t.tokenHash <> :keepTokenHash")
    int revokeAllForUserExcept(@Param("userId") Long userId, @Param("keepTokenHash") String keepTokenHash,
                               @Param("now") Instant now);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now, t.isActive = false WHERE t.userId = :userId AND t.revokedAt IS NULL")
    int revokeAllForUser(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :cutoff")
    int deleteExpired(@Param("cutoff") Instant cutoff);
}
