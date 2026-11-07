package com.restaurant.saas.restaurant.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Table(name = "restaurants")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class Restaurant extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "legal_name")
    private String legalName;

    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    private String gallery;

    @Column(name = "brand_colors")
    private String brandColors;

    @Column(nullable = false)
    private String timezone = "UTC";

    @Column(nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency = "USD";

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "service_charge_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal serviceChargeRate = BigDecimal.ZERO;

    private String cuisine;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    private String website;

    @Column(name = "address_line")
    private String addressLine;

    private String city;

    private String state;

    private String country;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "business_registration_number")
    private String businessRegistrationNumber;

    @Column(name = "tax_number")
    private String taxNumber;

    @Column(name = "default_language", nullable = false)
    private String defaultLanguage = "en";

    @Column(name = "social_links")
    private String socialLinks;

    @Column(name = "operating_hours")
    private String operatingHours;
}
