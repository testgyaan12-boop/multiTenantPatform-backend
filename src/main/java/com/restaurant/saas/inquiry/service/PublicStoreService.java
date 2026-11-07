package com.restaurant.saas.inquiry.service;

import com.restaurant.saas.common.exception.NotFoundException;
import com.restaurant.saas.inquiry.dto.StoreView;
import com.restaurant.saas.inquiry.dto.PublicStoreProductsResponse;
import com.restaurant.saas.menu.entity.ItemAttribute;
import com.restaurant.saas.menu.entity.MenuCategory;
import com.restaurant.saas.menu.entity.MenuItem;
import com.restaurant.saas.menu.entity.MenuItemVariant;
import com.restaurant.saas.menu.repository.ItemAttributeRepository;
import com.restaurant.saas.menu.repository.MenuCategoryRepository;
import com.restaurant.saas.menu.repository.MenuItemRepository;
import com.restaurant.saas.menu.repository.MenuItemVariantRepository;
import com.restaurant.saas.restaurant.entity.Restaurant;
import com.restaurant.saas.restaurant.repository.RestaurantRepository;
import com.restaurant.saas.superadmin.service.StoreSettingsService;
import com.restaurant.saas.tenant.entity.Tenant;
import com.restaurant.saas.tenant.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PublicStoreService {

    private final TenantRepository tenantRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuCategoryRepository categoryRepository;
    private final MenuItemRepository itemRepository;
    private final MenuItemVariantRepository variantRepository;
    private final ItemAttributeRepository attributeRepository;
    private final StoreSettingsService storeSettingsService;
    private final String customerBaseUrl;

    public PublicStoreService(
        TenantRepository tenantRepository,
        RestaurantRepository restaurantRepository,
        MenuCategoryRepository categoryRepository,
        MenuItemRepository itemRepository,
        MenuItemVariantRepository variantRepository,
        ItemAttributeRepository attributeRepository,
        StoreSettingsService storeSettingsService,
        @Value("${app.frontend.customer-base-url:http://localhost:5175}") String customerBaseUrl) {
        this.tenantRepository = tenantRepository;
        this.restaurantRepository = restaurantRepository;
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
        this.variantRepository = variantRepository;
        this.attributeRepository = attributeRepository;
        this.storeSettingsService = storeSettingsService;
        this.customerBaseUrl = customerBaseUrl;
    }

    @Transactional(readOnly = true)
    public StoreView store(String slug) {
        Tenant tenant = requireTenant(slug);
        Restaurant restaurant = restaurantRepository.findByTenantId(tenant.getId())
            .orElseThrow(() -> new NotFoundException("Store profile is not configured"));
        StoreSettingsService.Booking booking = storeSettingsService.effectiveBooking(tenant.getId());
        String publicUrl = customerBaseUrl.replaceAll("/+$", "") + "/s/" + tenant.getSlug();
        return new StoreView(
            tenant.getSlug(),
            restaurant.getName(),
            restaurant.getDescription(),
            restaurant.getLogoUrl(),
            restaurant.getCuisine(),
            restaurant.getContactEmail(),
            tenant.getContactPhone(),
            tenant.getCurrency(),
            tenant.getTimezone(),
            tenant.getBusinessType(),
            restaurant.getSocialLinks(),
            formatOperatingHours(restaurant.getOperatingHours()),
            publicUrl,
            new StoreView.BookingConfig(booking.enabled(), booking.amount()));
    }

    @Transactional(readOnly = true)
    public PublicStoreProductsResponse products(String slug) {
        Tenant tenant = requireTenant(slug);
        Restaurant restaurant = restaurantRepository.findByTenantId(tenant.getId())
            .orElseThrow(() -> new NotFoundException("Store profile is not configured"));

        List<MenuCategory> categories = categoryRepository.findByTenantIdOrderBySortOrderAscNameAsc(tenant.getId());
        List<MenuItem> available = itemRepository.searchPublic(tenant.getId(), null, null);

        Map<Long, List<ItemAttribute>> attributesByItem = attributeRepository
            .findByTenantIdAndMenuItemIdIn(
                tenant.getId(), available.stream().map(MenuItem::getId).toList())
            .stream()
            .collect(Collectors.groupingBy(ItemAttribute::getMenuItemId));

        List<PublicStoreProductsResponse.CategoryView> categoryViews = categories.stream()
            .map(category -> new PublicStoreProductsResponse.CategoryView(
                category.getId(),
                category.getName(),
                available.stream()
                    .filter(item -> category.getId().equals(item.getCategoryId()))
                    .map(item -> toProduct(item, attributesByItem.getOrDefault(item.getId(), List.of())))
                    .toList()))
            .filter(category -> !category.items().isEmpty())
            .toList();

        return new PublicStoreProductsResponse(
            tenant.getSlug(),
            restaurant.getName(),
            tenant.getBusinessType(),
            tenant.getCurrency(),
            restaurant.getLogoUrl(),
            categoryViews);
    }

    @Transactional(readOnly = true)
    public PublicStoreProductsResponse.ProductView product(String slug, Long itemId) {
        Tenant tenant = requireTenant(slug);
        MenuItem item = itemRepository.findByIdAndTenantId(itemId, tenant.getId())
            .filter(MenuItem::getIsAvailable)
            .orElseThrow(() -> new NotFoundException("Product not found"));
        List<ItemAttribute> attributes =
            attributeRepository.findByTenantIdAndMenuItemId(tenant.getId(), itemId);
        return toProduct(item, attributes);
    }

    private PublicStoreProductsResponse.ProductView toProduct(MenuItem item, List<ItemAttribute> attributes) {
        List<MenuItemVariant> variants = variantRepository.findByMenuItemIdOrderBySortOrderAsc(item.getId());
        return new PublicStoreProductsResponse.ProductView(
            item.getId(),
            item.getCategoryId(),
            item.getSku(),
            item.getName(),
            item.getDescription(),
            item.getPrice(),
            item.getCurrency(),
            item.getImageUrl(),
            item.getIsAvailable(),
            item.getIsFeatured(),
            item.getStockQty(),
            attributes.stream()
                .map(attribute -> new PublicStoreProductsResponse.AttributeView(
                    attribute.getAttrKey(), attribute.getAttrValue()))
                .toList(),
            variants.stream()
                .map(variant -> new PublicStoreProductsResponse.VariantView(
                    variant.getId(), variant.getName(), variant.getPriceDelta(), variant.getIsDefault()))
                .toList());
    }

    private Tenant requireTenant(String slug) {
        return tenantRepository.findBySlug(slug)
            .orElseThrow(() -> new NotFoundException("Store not found"));
    }

    static String formatOperatingHours(String operatingHours) {
        if (operatingHours == null || operatingHours.isBlank()) {
            return null;
        }
        String trimmed = operatingHours.trim();
        if (!trimmed.startsWith("[")) {
            return trimmed;
        }
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var days = mapper.readTree(trimmed);
            if (!days.isArray() || days.isEmpty()) {
                return trimmed;
            }
            java.util.List<String> parts = new java.util.ArrayList<>();
            for (var day : days) {
                String code = day.path("day").asText("");
                if (code.isBlank()) {
                    continue;
                }
                String label = switch (code.toUpperCase(java.util.Locale.ROOT)) {
                    case "MON" -> "Mon";
                    case "TUE" -> "Tue";
                    case "WED" -> "Wed";
                    case "THU" -> "Thu";
                    case "FRI" -> "Fri";
                    case "SAT" -> "Sat";
                    case "SUN" -> "Sun";
                    default -> code;
                };
                if (day.path("closed").asBoolean(false)) {
                    parts.add(label + " Closed");
                } else {
                    parts.add(label + " " + day.path("open").asText("?") + "-"
                        + day.path("close").asText("?"));
                }
            }
            return parts.isEmpty() ? trimmed : String.join(" · ", parts);
        } catch (Exception ex) {
            return trimmed;
        }
    }
}
