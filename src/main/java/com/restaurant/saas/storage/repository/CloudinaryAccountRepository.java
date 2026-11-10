package com.restaurant.saas.storage.repository;

import com.restaurant.saas.storage.entity.CloudinaryAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CloudinaryAccountRepository extends JpaRepository<CloudinaryAccount, Long> {

    Optional<CloudinaryAccount> findByCloudName(String cloudName);

    List<CloudinaryAccount> findAllByOrderByPriorityAscIdAsc();

    List<CloudinaryAccount> findByIsActiveTrueAndIsDeleteFalseOrderByPriorityAscIdAsc();

    @Query("UPDATE CloudinaryAccount c SET c.creditsUsed = :creditsUsed, c.usedPercent = :usedPercent, "
        + "c.usageCheckedAt = :usageCheckedAt WHERE c.id = :id")
    @Modifying
    int updateUsage(@Param("id") Long id, @Param("creditsUsed") java.math.BigDecimal creditsUsed,
                    @Param("usedPercent") java.math.BigDecimal usedPercent,
                    @Param("usageCheckedAt") java.time.Instant usageCheckedAt);
}
