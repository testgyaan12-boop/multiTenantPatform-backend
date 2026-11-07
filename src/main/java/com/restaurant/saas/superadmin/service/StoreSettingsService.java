package com.restaurant.saas.superadmin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.saas.common.exception.ApiException;
import com.restaurant.saas.common.exception.NotFoundException;
import com.restaurant.saas.superadmin.entity.PlatformSetting;
import com.restaurant.saas.superadmin.repository.PlatformSettingRepository;
import com.restaurant.saas.tenant.entity.Tenant;
import com.restaurant.saas.tenant.repository.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Tenant-wise storefront settings stored in platform_settings under
 * {@code tenant.{id}.booking_payment} as JSON: {"enabled":true,"amount":50}.
 *
 * <p>Defaults when no row exists: booking payment enabled for VEHICLE and
 * REAL_ESTATE tenants (amount 50), disabled for every other vertical.</p>
 */
@Service
public class StoreSettingsService {

    public static final BigDecimal DEFAULT_BOOKING_AMOUNT = new BigDecimal("50");
    private static final Set<String> DEFAULT_ENABLED_TYPES = Set.of("VEHICLE", "REAL_ESTATE");

    private final TenantRepository tenantRepository;
    private final PlatformSettingRepository settingRepository;
    private final ObjectMapper objectMapper;

    public StoreSettingsService(
        TenantRepository tenantRepository,
        PlatformSettingRepository settingRepository,
        ObjectMapper objectMapper) {
        this.tenantRepository = tenantRepository;
        this.settingRepository = settingRepository;
        this.objectMapper = objectMapper;
    }

    public record StoreSettingsRequest(Boolean bookingPaymentEnabled, BigDecimal bookingPaymentAmount) {
    }

    public record StoreSettingsView(
        Long tenantId,
        String businessType,
        String storeName,
        String slug,
        boolean bookingPaymentEnabled,
        BigDecimal bookingPaymentAmount
    ) {
    }

    @Transactional(readOnly = true)
    public StoreSettingsView view(Long tenantId) {
        Tenant tenant = requireTenant(tenantId);
        Booking booking = readBooking(tenant);
        return new StoreSettingsView(
            tenant.getId(),
            tenant.getBusinessType(),
            tenant.getName(),
            tenant.getSlug(),
            booking.enabled(),
            booking.amount());
    }

    @Transactional
    public StoreSettingsView update(Long tenantId, StoreSettingsRequest request) {
        Tenant tenant = requireTenant(tenantId);
        Booking current = readBooking(tenant);

        boolean enabled = request.bookingPaymentEnabled() != null
            ? request.bookingPaymentEnabled()
            : current.enabled();
        BigDecimal amount = request.bookingPaymentAmount() != null
            ? request.bookingPaymentAmount()
            : current.amount();
        if (amount.compareTo(BigDecimal.ZERO) < 0 || amount.compareTo(new BigDecimal("1000000")) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_booking_amount",
                "Booking amount must be between 0 and 1,000,000");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("enabled", enabled);
        payload.put("amount", amount);
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize booking payment settings", ex);
        }

        PlatformSetting setting = setting(tenantId).orElseGet(() -> {
            PlatformSetting created = new PlatformSetting();
            created.setSettingKey(bookingKey(tenantId));
            created.setCategory("TENANT");
            created.setDescription("Storefront booking payment for tenant " + tenantId);
            return created;
        });
        setting.setSettingValue(json);
        settingRepository.save(setting);

        return new StoreSettingsView(
            tenant.getId(),
            tenant.getBusinessType(),
            tenant.getName(),
            tenant.getSlug(),
            enabled,
            amount);
    }

    /** Effective booking-payment config for a tenant (used by storefront code later). */
    @Transactional(readOnly = true)
    public Booking effectiveBooking(Long tenantId) {
        return readBooking(requireTenant(tenantId));
    }

    public record Booking(boolean enabled, BigDecimal amount) {
    }

    private Booking readBooking(Tenant tenant) {
        boolean defaultEnabled = DEFAULT_ENABLED_TYPES.contains(tenant.getBusinessType());
        Optional<PlatformSetting> setting = setting(tenant.getId());
        if (setting.isEmpty() || setting.get().getSettingValue() == null
            || setting.get().getSettingValue().isBlank()) {
            return new Booking(defaultEnabled, DEFAULT_BOOKING_AMOUNT);
        }
        try {
            JsonNode node = objectMapper.readTree(setting.get().getSettingValue());
            boolean enabled = node.path("enabled").asBoolean(defaultEnabled);
            BigDecimal amount = new BigDecimal(node.path("amount").asText(DEFAULT_BOOKING_AMOUNT.toPlainString()));
            return new Booking(enabled, amount);
        } catch (JsonProcessingException | NumberFormatException ex) {
            return new Booking(defaultEnabled, DEFAULT_BOOKING_AMOUNT);
        }
    }

    private Tenant requireTenant(Long tenantId) {
        return tenantRepository.findById(tenantId)
            .orElseThrow(() -> new NotFoundException("Tenant", tenantId));
    }

    private Optional<PlatformSetting> setting(Long tenantId) {
        return settingRepository.findBySettingKey(bookingKey(tenantId));
    }

    private static String bookingKey(Long tenantId) {
        return "tenant." + tenantId + ".booking_payment";
    }
}
