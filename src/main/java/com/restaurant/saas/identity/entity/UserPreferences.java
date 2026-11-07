package com.restaurant.saas.identity.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "user_preferences")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class UserPreferences extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private String theme = "SYSTEM";

    @Column(name = "time_zone", nullable = false)
    private String timeZone = "Asia/Kolkata";

    @Column(name = "date_format", nullable = false)
    private String dateFormat = "DD/MM/YYYY";

    @Column(nullable = false)
    private String language = "en";

    @Column(name = "email_notifications", nullable = false)
    private Boolean emailNotifications = Boolean.TRUE;

    @Column(name = "in_app_notifications", nullable = false)
    private Boolean inAppNotifications = Boolean.TRUE;
}
