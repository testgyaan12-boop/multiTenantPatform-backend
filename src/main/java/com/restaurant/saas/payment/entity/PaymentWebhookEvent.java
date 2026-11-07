package com.restaurant.saas.payment.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

@Entity
@Table(name = "payment_webhook_events")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class PaymentWebhookEvent extends BaseEntity {

    @Column(nullable = false)
    private String provider;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "longtext")
    private String payload;

    @Column(name = "signature_valid", nullable = false)
    private Boolean signatureValid = Boolean.FALSE;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "error_message")
    private String errorMessage;
}
