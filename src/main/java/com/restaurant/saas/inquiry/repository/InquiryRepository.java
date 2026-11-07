package com.restaurant.saas.inquiry.repository;

import com.restaurant.saas.inquiry.entity.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    @Query("""
        SELECT i FROM Inquiry i
        WHERE i.tenantId = :tenantId
          AND (:status IS NULL OR i.status = :status)
          AND (:search IS NULL
               OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(i.phone) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(i.email, '')) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY i.createdAt DESC
        """)
    Page<Inquiry> search(
        @Param("tenantId") Long tenantId,
        @Param("status") String status,
        @Param("search") String search,
        Pageable pageable);

    Inquiry findByIdAndTenantId(Long id, Long tenantId);
}
