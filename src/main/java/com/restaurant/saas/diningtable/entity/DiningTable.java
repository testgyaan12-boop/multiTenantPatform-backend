package com.restaurant.saas.diningtable.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dining_tables")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class DiningTable extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "section_id")
    private Long sectionId;

    @Column(name = "table_number", nullable = false)
    private String tableNumber;

    private String name;

    @Column(nullable = false)
    private Integer capacity = 2;

    @Column(nullable = false)
    private String status = "AVAILABLE";

    @Column(name = "qr_token", nullable = false, length = 36, columnDefinition = "char(36)")
    private String qrToken = UUID.randomUUID().toString();

    @Column(name = "qr_rotated_at")
    private Instant qrRotatedAt;
}
