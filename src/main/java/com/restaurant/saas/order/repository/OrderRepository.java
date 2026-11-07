package com.restaurant.saas.order.repository;

import com.restaurant.saas.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByPublicToken(String publicToken);

    Optional<Order> findByIdAndTenantId(Long id, Long tenantId);

    @Query("""
        SELECT o FROM Order o
        WHERE o.tenantId = :tenantId
          AND (:branchId IS NULL OR o.branchId = :branchId)
          AND (:status IS NULL OR o.status = :status)
          AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus)
          AND (:source IS NULL OR o.source = :source)
          AND o.createdAt >= :since
          AND (:search IS NULL
               OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(o.customerName, '')) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY o.createdAt DESC
        """)
    Page<Order> search(
        @Param("tenantId") Long tenantId,
        @Param("branchId") Long branchId,
        @Param("status") String status,
        @Param("paymentStatus") String paymentStatus,
        @Param("source") String source,
        @Param("search") String search,
        @Param("since") java.time.Instant since,
        Pageable pageable);

    @Query("""
        SELECT o.status, COUNT(o) FROM Order o
        WHERE o.tenantId = :tenantId
          AND (:branchId IS NULL OR o.branchId = :branchId)
          AND o.createdAt >= :since
        GROUP BY o.status
        """)
    java.util.List<Object[]> statusCounts(
        @Param("tenantId") Long tenantId,
        @Param("branchId") Long branchId,
        @Param("since") java.time.Instant since);

    long countByTenantIdAndStatusIn(Long tenantId, java.util.Collection<String> statuses);
}
