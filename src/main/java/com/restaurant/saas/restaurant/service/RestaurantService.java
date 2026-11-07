package com.restaurant.saas.restaurant.service;

import com.restaurant.saas.common.exception.NotFoundException;
import com.restaurant.saas.common.tenant.TenantContext;
import com.restaurant.saas.branch.repository.BranchRepository;
import com.restaurant.saas.restaurant.dto.RestaurantResponse;
import com.restaurant.saas.restaurant.dto.RestaurantSubscriptionResponse;
import com.restaurant.saas.restaurant.dto.RestaurantUpdateRequest;
import com.restaurant.saas.restaurant.entity.Restaurant;
import com.restaurant.saas.restaurant.repository.RestaurantRepository;
import com.restaurant.saas.subscription.entity.Subscription;
import com.restaurant.saas.subscription.entity.SubscriptionPlan;
import com.restaurant.saas.subscription.repository.SubscriptionPlanRepository;
import com.restaurant.saas.subscription.repository.SubscriptionRepository;
import com.restaurant.saas.tenant.entity.Tenant;
import com.restaurant.saas.tenant.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final TenantRepository tenantRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final BranchRepository branchRepository;
    private final String customerBaseUrl;

    public RestaurantService(
        RestaurantRepository restaurantRepository,
        TenantRepository tenantRepository,
        SubscriptionRepository subscriptionRepository,
        SubscriptionPlanRepository planRepository,
        BranchRepository branchRepository,
        @Value("${app.frontend.customer-base-url:http://localhost:5175}") String customerBaseUrl) {
        this.restaurantRepository = restaurantRepository;
        this.tenantRepository = tenantRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.branchRepository = branchRepository;
        this.customerBaseUrl = customerBaseUrl;
    }

    @Transactional(readOnly = true)
    public RestaurantResponse current() {
        return toResponse(requireRestaurant());
    }

    @Transactional
    public RestaurantResponse update(RestaurantUpdateRequest request) {
        Restaurant restaurant = requireRestaurant();
        restaurant.setName(request.name());
        restaurant.setLegalName(request.legalName());
        restaurant.setDescription(request.description());
        if (request.logoUrl() != null) {
            restaurant.setLogoUrl(blankToNull(request.logoUrl()));
        }
        if (request.coverImageUrl() != null) {
            restaurant.setCoverImageUrl(blankToNull(request.coverImageUrl()));
        }
        if (request.gallery() != null) {
            restaurant.setGallery(blankToNull(request.gallery()));
        }
        if (request.brandColors() != null) {
            restaurant.setBrandColors(blankToNull(request.brandColors()));
        }
        if (request.timezone() != null && !request.timezone().isBlank()) {
            restaurant.setTimezone(request.timezone());
        }
        if (request.currency() != null && !request.currency().isBlank()) {
            restaurant.setCurrency(request.currency());
        }
        if (request.taxRate() != null) {
            restaurant.setTaxRate(request.taxRate());
        }
        if (request.serviceChargeRate() != null) {
            restaurant.setServiceChargeRate(request.serviceChargeRate());
        }
        restaurant.setCuisine(request.cuisine());
        restaurant.setContactEmail(request.contactEmail());
        if (request.contactPhone() != null) {
            restaurant.setContactPhone(blankToNull(request.contactPhone()));
        }
        if (request.website() != null) {
            restaurant.setWebsite(blankToNull(request.website()));
        }
        if (request.addressLine() != null) {
            restaurant.setAddressLine(blankToNull(request.addressLine()));
        }
        if (request.city() != null) {
            restaurant.setCity(blankToNull(request.city()));
        }
        if (request.state() != null) {
            restaurant.setState(blankToNull(request.state()));
        }
        if (request.country() != null) {
            restaurant.setCountry(blankToNull(request.country()));
        }
        if (request.postalCode() != null) {
            restaurant.setPostalCode(blankToNull(request.postalCode()));
        }
        if (request.businessRegistrationNumber() != null) {
            restaurant.setBusinessRegistrationNumber(blankToNull(request.businessRegistrationNumber()));
        }
        if (request.taxNumber() != null) {
            restaurant.setTaxNumber(blankToNull(request.taxNumber()));
        }
        if (request.defaultLanguage() != null && !request.defaultLanguage().isBlank()) {
            restaurant.setDefaultLanguage(request.defaultLanguage().trim());
        }
        if (request.socialLinks() != null) {
            restaurant.setSocialLinks(blankToNull(request.socialLinks()));
        }
        if (request.operatingHours() != null) {
            restaurant.setOperatingHours(blankToNull(request.operatingHours()));
        }
        return toResponse(restaurantRepository.save(restaurant));
    }

    @Transactional(readOnly = true)
    public RestaurantSubscriptionResponse subscription() {
        Long tenantId = TenantContext.requireTenant();
        Optional<Subscription> subscription = subscriptionRepository.findByTenantId(tenantId);
        int branchUsage = branchRepository.findByTenantIdOrderByNameAsc(tenantId).size();
        if (subscription.isEmpty()) {
            return new RestaurantSubscriptionResponse(null, null, null, null, null, null,
                null, null, null, null, null, branchUsage, null);
        }
        Subscription sub = subscription.get();
        SubscriptionPlan plan = planRepository.findById(sub.getPlanId()).orElse(null);
        return new RestaurantSubscriptionResponse(
            plan == null ? null : plan.getCode(),
            plan == null ? null : plan.getName(),
            sub.getStatus(),
            plan == null ? null : plan.getPriceMonthly(),
            plan == null ? null : plan.getCurrency(),
            plan == null ? null : "MONTHLY",
            sub.getTrialEndsAt(),
            sub.getCurrentPeriodStart(),
            sub.getCurrentPeriodEnd(),
            sub.getCancelAtPeriodEnd(),
            plan == null ? null : plan.getMaxBranches(),
            branchUsage,
            plan == null ? null : plan.getFeatures()
        );
    }

    private RestaurantResponse toResponse(Restaurant restaurant) {
        String publicUrl = null;
        String businessType = "RESTAURANT";
        String tenantStatus = null;
        var tenant = tenantRepository.findById(restaurant.getTenantId());
        if (tenant.isPresent()) {
            String slug = tenant.get().getSlug();
            publicUrl = customerBaseUrl.replaceAll("/+$", "") + "/r/" + slug;
            businessType = tenant.get().getBusinessType();
            tenantStatus = tenant.get().getStatus();
        }
        return RestaurantResponse.from(restaurant, publicUrl, businessType, tenantStatus);
    }

    private Restaurant requireRestaurant() {
        Long tenantId = TenantContext.requireTenant();
        return restaurantRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new NotFoundException("Restaurant profile is not configured"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
