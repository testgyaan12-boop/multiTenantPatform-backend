package com.restaurant.saas.storage.repository;

import com.restaurant.saas.storage.entity.StoredFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {

    Optional<StoredFile> findByStorageKey(String storageKey);

    @Query("select count(f) from StoredFile f where (:tenantId is null or f.tenantId = :tenantId)")
    long countAll(@Param("tenantId") Long tenantId);

    @Query("select coalesce(sum(f.sizeBytes), 0) from StoredFile f where (:tenantId is null or f.tenantId = :tenantId)")
    long sumBytes(@Param("tenantId") Long tenantId);

    @Query("select count(f) from StoredFile f where (:tenantId is null or f.tenantId = :tenantId) "
        + "and f.createdAt >= :from and f.createdAt < :to")
    long countBetween(@Param("tenantId") Long tenantId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("select coalesce(sum(f.sizeBytes), 0) from StoredFile f where (:tenantId is null or f.tenantId = :tenantId) "
        + "and f.createdAt >= :from and f.createdAt < :to")
    long sumBytesBetween(@Param("tenantId") Long tenantId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("select coalesce(sum(f.sizeBytes), 0) from StoredFile f where (:tenantId is null or f.tenantId = :tenantId) "
        + "and f.createdAt < :before")
    long sumBytesBefore(@Param("tenantId") Long tenantId, @Param("before") Instant before);

    @Query("select f from StoredFile f where (:tenantId is null or f.tenantId = :tenantId) "
        + "order by f.createdAt desc")
    List<StoredFile> findRecent(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query(value = "select date(created_at) as d, count(*) as c, coalesce(sum(size_bytes), 0) as b "
        + "from files where is_delete = 0 and is_active = 1 "
        + "and (:tenantId is null or tenant_id = :tenantId) "
        + "and created_at >= :from and created_at < :to "
        + "group by date(created_at) order by d", nativeQuery = true)
    List<Object[]> dailyGrowth(@Param("tenantId") Long tenantId,
                               @Param("from") Instant from, @Param("to") Instant to);

    @Query(value = "select cat, count(*), coalesce(sum(size_bytes), 0) from ("
        + "select case "
        + "  when content_type like 'image/%' then 'IMAGE' "
        + "  when content_type like 'video/%' then 'VIDEO' "
        + "  when content_type like 'application/pdf' "
        + "    or content_type like '%word%' "
        + "    or content_type like '%excel%' "
        + "    or content_type like '%sheet%' "
        + "    or content_type like '%presentation%' "
        + "    or content_type like 'text/%' then 'DOCUMENT' "
        + "  else 'OTHER' end as cat, size_bytes "
        + "from files where is_delete = 0 and is_active = 1 "
        + "and (:tenantId is null or tenant_id = :tenantId)) t group by cat", nativeQuery = true)
    List<Object[]> fileTypeDistribution(@Param("tenantId") Long tenantId);

    @Query(value = "select t.id, t.tenant_id, t.original_name, t.content_type, t.size_bytes, "
        + "t.public_url, t.provider, t.created_at, t.cat from ("
        + "select f.id, f.tenant_id, f.original_name, f.content_type, f.size_bytes, "
        + "f.public_url, f.provider, f.created_at, case "
        + "  when f.content_type like 'image/%' then 'IMAGE' "
        + "  when f.content_type like 'video/%' then 'VIDEO' "
        + "  when f.content_type like 'application/pdf' "
        + "    or f.content_type like '%word%' "
        + "    or f.content_type like '%excel%' "
        + "    or f.content_type like '%sheet%' "
        + "    or f.content_type like '%presentation%' "
        + "    or f.content_type like 'text/%' then 'DOCUMENT' "
        + "  else 'OTHER' end as cat "
        + "from files f where f.is_delete = 0 and f.is_active = 1 "
        + "and (:tenantId is null or f.tenant_id = :tenantId) "
        + "and (:search is null or lower(f.original_name) like lower(concat('%', :search, '%')))) t "
        + "where (:type is null or t.cat = :type) order by t.created_at desc",
        countQuery = "select count(*) from ("
        + "select case "
        + "  when f.content_type like 'image/%' then 'IMAGE' "
        + "  when f.content_type like 'video/%' then 'VIDEO' "
        + "  when f.content_type like 'application/pdf' "
        + "    or f.content_type like '%word%' "
        + "    or f.content_type like '%excel%' "
        + "    or f.content_type like '%sheet%' "
        + "    or f.content_type like '%presentation%' "
        + "    or f.content_type like 'text/%' then 'DOCUMENT' "
        + "  else 'OTHER' end as cat "
        + "from files f where f.is_delete = 0 and f.is_active = 1 "
        + "and (:tenantId is null or f.tenant_id = :tenantId) "
        + "and (:search is null or lower(f.original_name) like lower(concat('%', :search, '%')))) t "
        + "where (:type is null or t.cat = :type)",
        nativeQuery = true)
    Page<Object[]> pageFiles(@Param("tenantId") Long tenantId,
                             @Param("search") String search,
                             @Param("type") String type,
                             Pageable pageable);
}
