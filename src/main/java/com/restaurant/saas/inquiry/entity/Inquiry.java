package com.restaurant.saas.inquiry.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Table(name = "inquiries")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class Inquiry extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "menu_item_id")
    private Long itemId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 32)
    private String phone;

    private String email;

    @Column(length = 2000)
    private String message;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(nullable = false, length = 20)
    private String status = "NEW";

    @Column(name = "booking_payment_status")
    private String bookingPaymentStatus;

    @Column(name = "booking_amount", precision = 10, scale = 2)
    private BigDecimal bookingAmount;

    @Column(name = "order_id")
    private Long orderId;

    @Column(length = 1000)
    private String notes;
}
