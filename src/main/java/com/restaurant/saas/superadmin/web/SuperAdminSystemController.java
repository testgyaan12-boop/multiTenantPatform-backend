package com.restaurant.saas.superadmin.web;

import com.restaurant.saas.superadmin.service.SuperAdminPlatformService;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService.CouponRequest;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService.CouponView;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService.FlagRequest;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService.FlagView;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService.SettingView;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService.SettingsRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/superadmin")
public class SuperAdminSystemController {

    private final SuperAdminPlatformService platformService;

    public SuperAdminSystemController(SuperAdminPlatformService platformService) {
        this.platformService = platformService;
    }

    @GetMapping("/coupons")
    public List<CouponView> coupons() {
        return platformService.listCoupons();
    }

    @PostMapping("/coupons")
    public ResponseEntity<CouponView> createCoupon(@Valid @RequestBody CouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(platformService.createCoupon(request));
    }

    @PutMapping("/coupons/{id}")
    public CouponView updateCoupon(@PathVariable Long id, @Valid @RequestBody CouponRequest request) {
        return platformService.updateCoupon(id, request);
    }

    @DeleteMapping("/coupons/{id}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long id) {
        platformService.deleteCoupon(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/settings")
    public List<SettingView> settings(@RequestParam(required = false) String category) {
        return platformService.listSettings(category);
    }

    @PutMapping("/settings")
    public List<SettingView> upsertSettings(@Valid @RequestBody SettingsRequest request) {
        return platformService.upsertSettings(request);
    }

    @GetMapping("/integrations")
    public List<SettingView> integrations() {
        return platformService.listSettings("INTEGRATION");
    }

    @PutMapping("/integrations")
    public List<SettingView> upsertIntegrations(@Valid @RequestBody SettingsRequest request) {
        return platformService.upsertSettings(request);
    }

    @GetMapping("/feature-flags")
    public List<FlagView> featureFlags() {
        return platformService.listFlags();
    }

    @PutMapping("/feature-flags")
    public FlagView toggleFeatureFlag(@Valid @RequestBody FlagRequest request) {
        return platformService.toggleFlag(request);
    }
}
