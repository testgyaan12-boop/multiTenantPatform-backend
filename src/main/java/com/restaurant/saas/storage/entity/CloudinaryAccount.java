package com.restaurant.saas.storage.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cloudinary_accounts")
@SQLRestriction("is_delete = 0")
@Getter
@Setter
public class CloudinaryAccount extends BaseEntity {

    @Column(name = "label", nullable = false, length = 120)
    private String label;

    @Column(name = "cloud_name", nullable = false, length = 120, unique = true)
    private String cloudName;

    @Column(name = "api_key", nullable = false, length = 120)
    private String apiKey;

    @Column(name = "api_secret_enc", nullable = false, columnDefinition = "text")
    private String apiSecretEnc;

    @Column(name = "upload_preset", length = 120)
    private String uploadPreset;

    @Column(name = "folder_prefix", length = 160)
    private String folderPrefix;

    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    @Column(name = "monthly_credit_limit", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyCreditLimit = new BigDecimal("25.00");

    @Column(name = "credits_used", nullable = false, precision = 10, scale = 2)
    private BigDecimal creditsUsed = BigDecimal.ZERO;

    @Column(name = "used_percent", nullable = false, precision = 6, scale = 2)
    private BigDecimal usedPercent = BigDecimal.ZERO;

    @Column(name = "local_upload_count", nullable = false)
    private Long localUploadCount = 0L;

    @Column(name = "local_bytes_used", nullable = false)
    private Long localBytesUsed = 0L;

    @Column(name = "usage_checked_at")
    private Instant usageCheckedAt;

    @Column(name = "last_upload_at")
    private Instant lastUploadAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "last_error_at")
    private Instant lastErrorAt;
}
