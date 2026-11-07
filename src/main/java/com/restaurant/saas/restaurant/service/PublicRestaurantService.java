package com.restaurant.saas.restaurant.service;

import com.restaurant.saas.common.exception.NotFoundException;
import com.restaurant.saas.restaurant.dto.PublicRestaurantResponse;
import com.restaurant.saas.restaurant.entity.Restaurant;
import com.restaurant.saas.restaurant.repository.RestaurantRepository;
import com.restaurant.saas.tenant.entity.Tenant;
import com.restaurant.saas.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicRestaurantService {

    private final TenantRepository tenantRepository;
    private final RestaurantRepository restaurantRepository;

    public PublicRestaurantService(TenantRepository tenantRepository, RestaurantRepository restaurantRepository) {
        this.tenantRepository = tenantRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @Transactional(readOnly = true)
    public PublicRestaurantResponse bySlug(String slug) {
        Tenant tenant = tenantRepository.findBySlug(slug)
            .orElseThrow(() -> new NotFoundException("Restaurant not found"));
        Restaurant restaurant = restaurantRepository.findByTenantId(tenant.getId())
            .orElseThrow(() -> new NotFoundException("Restaurant profile is not configured"));
        return new PublicRestaurantResponse(
            tenant.getSlug(),
            restaurant.getName(),
            restaurant.getDescription(),
            restaurant.getLogoUrl(),
            restaurant.getCurrency(),
            restaurant.getTaxRate(),
            restaurant.getServiceChargeRate());
    }
}
