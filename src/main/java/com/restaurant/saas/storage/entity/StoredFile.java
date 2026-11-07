package com.restaurant.saas.storage.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "files")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class StoredFile extends BaseEntity {

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "public_url")
    private String publicUrl;

    @Column(name = "uploaded_by")
    private Long uploadedBy;
}
