package com.restaurant.saas.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;

/**
 * Base class for every persisted aggregate.
 *
 * <p>Soft-delete semantics: a row is considered deleted only when
 * {@code is_delete = 1}; deleting always also sets {@code is_active = 0}.
 * Normal reads must only see rows with {@code is_active = 1 AND is_delete = 0}.</p>
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "is_delete", nullable = false)
    private Boolean isDelete = Boolean.FALSE;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", length = 100, updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    public boolean isDeleted() {
        return Boolean.TRUE.equals(isDelete);
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(isActive) && !isDeleted();
    }

    public void softDelete() {
        this.isDelete = Boolean.TRUE;
        this.isActive = Boolean.FALSE;
    }

    public void restore(boolean active) {
        this.isDelete = Boolean.FALSE;
        this.isActive = active;
    }

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || !getClass().equals(other.getClass())) {
            return false;
        }
        BaseEntity that = (BaseEntity) other;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public final int hashCode() {
        return getClass().hashCode();
    }
}
