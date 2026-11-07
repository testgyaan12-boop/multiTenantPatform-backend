package com.restaurant.saas.superadmin.repository;

import com.restaurant.saas.superadmin.entity.PlatformSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, Long> {
    List<PlatformSetting> findAllByOrderByCategoryAscSettingKeyAsc();
    Optional<PlatformSetting> findBySettingKey(String settingKey);
}
