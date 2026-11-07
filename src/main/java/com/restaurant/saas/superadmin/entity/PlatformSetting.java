package com.restaurant.saas.superadmin.entity;

import com.restaurant.saas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "platform_settings")
@SQLRestriction("is_active = 1 AND is_delete = 0")
@Getter
@Setter
public class PlatformSetting extends BaseEntity {

    @Column(name = "setting_key", nullable = false, length = 80)
    private String settingKey;

    @Column(name = "setting_value", columnDefinition = "text")
    private String settingValue;

    private String description;

    @Column(nullable = false, length = 40)
    private String category = "GENERAL";
}
