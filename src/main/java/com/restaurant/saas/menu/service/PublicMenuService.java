package com.restaurant.saas.menu.service;

import com.restaurant.saas.branch.entity.Branch;
import com.restaurant.saas.branch.repository.BranchRepository;
import com.restaurant.saas.common.exception.NotFoundException;
import com.restaurant.saas.diningtable.entity.DiningTable;
import com.restaurant.saas.diningtable.repository.DiningTableRepository;
import com.restaurant.saas.menu.dto.PublicItemDetailResponse;
import com.restaurant.saas.menu.dto.PublicMenuResponse;
import com.restaurant.saas.menu.entity.MenuCategory;
import com.restaurant.saas.menu.entity.MenuItem;
import com.restaurant.saas.menu.entity.MenuItemVariant;
import com.restaurant.saas.menu.entity.Modifier;
import com.restaurant.saas.menu.entity.ModifierGroup;
import com.restaurant.saas.menu.repository.MenuCategoryRepository;
import com.restaurant.saas.menu.repository.MenuItemRepository;
import com.restaurant.saas.menu.repository.MenuItemVariantRepository;
import com.restaurant.saas.menu.repository.ModifierGroupRepository;
import com.restaurant.saas.menu.repository.ModifierRepository;
import com.restaurant.saas.restaurant.entity.Restaurant;
import com.restaurant.saas.restaurant.repository.RestaurantRepository;
import com.restaurant.saas.tenant.entity.Tenant;
import com.restaurant.saas.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PublicMenuService {

    private final DiningTableRepository tableRepository;
    private final BranchRepository branchRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuCategoryRepository categoryRepository;
    private final MenuItemRepository itemRepository;
    private final MenuItemVariantRepository variantRepository;
    private final ModifierGroupRepository modifierGroupRepository;
    private final ModifierRepository modifierRepository;
    private final TenantRepository tenantRepository;

    public PublicMenuService(
        DiningTableRepository tableRepository,
        BranchRepository branchRepository,
        RestaurantRepository restaurantRepository,
        MenuCategoryRepository categoryRepository,
        MenuItemRepository itemRepository,
        MenuItemVariantRepository variantRepository,
        ModifierGroupRepository modifierGroupRepository,
        ModifierRepository modifierRepository,
        TenantRepository tenantRepository) {
        this.tableRepository = tableRepository;
        this.branchRepository = branchRepository;
        this.restaurantRepository = restaurantRepository;
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
        this.variantRepository = variantRepository;
        this.modifierGroupRepository = modifierGroupRepository;
        this.modifierRepository = modifierRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public PublicMenuResponse menuForTable(String tableCode) {
        DiningTable table = tableRepository.findByQrToken(tableCode)
            .orElseThrow(() -> new NotFoundException("Table QR code is not valid"));
        Restaurant restaurant = restaurantRepository.findByTenantId(table.getTenantId())
            .orElseThrow(() -> new NotFoundException("Restaurant profile is not configured"));
        Tenant tenant = tenantRepository.findById(table.getTenantId())
            .orElseThrow(() -> new NotFoundException("Restaurant not found"));
        return buildMenu(tenant, restaurant);
    }

    @Transactional(readOnly = true)
    public PublicMenuResponse menuForSlug(String slug) {
        Tenant tenant = tenantRepository.findBySlug(slug)
            .orElseThrow(() -> new NotFoundException("Restaurant not found"));
        Restaurant restaurant = restaurantRepository.findByTenantId(tenant.getId())
            .orElseThrow(() -> new NotFoundException("Restaurant profile is not configured"));
        return buildMenu(tenant, restaurant);
    }

    @Transactional(readOnly = true)
    public PublicItemDetailResponse itemDetail(String slug, Long itemId) {
        Tenant tenant = tenantRepository.findBySlug(slug)
            .orElseThrow(() -> new NotFoundException("Restaurant not found"));
        MenuItem item = itemRepository.findByIdAndTenantId(itemId, tenant.getId())
            .orElseThrow(() -> new NotFoundException("Menu item", itemId));

        List<PublicItemDetailResponse.VariantView> variants = variantRepository
            .findByMenuItemIdOrderBySortOrderAsc(item.getId()).stream()
            .map(v -> new PublicItemDetailResponse.VariantView(
                v.getId(), v.getName(), v.getPriceDelta(), v.getIsDefault()))
            .toList();

        List<PublicItemDetailResponse.ModifierGroupView> groups = modifierGroupRepository
            .findGroupIdsForMenuItem(item.getId()).stream()
            .map(groupId -> modifierGroupRepository.findByIdAndTenantId(groupId, tenant.getId()).orElse(null))
            .filter(group -> group != null)
            .map(group -> new PublicItemDetailResponse.ModifierGroupView(
                group.getId(),
                group.getName(),
                group.getSelectionType(),
                group.getIsRequired(),
                group.getMinSelections(),
                group.getMaxSelections(),
                modifierRepository.findByModifierGroupIdOrderBySortOrderAsc(group.getId()).stream()
                    .map(m -> new PublicItemDetailResponse.ModifierView(
                        m.getId(), m.getName(), m.getPriceDelta()))
                    .toList()))
            .toList();

        return new PublicItemDetailResponse(
            item.getId(),
            item.getName(),
            item.getDescription(),
            item.getPrice(),
            item.getImageUrl(),
            item.getIsAvailable(),
            item.getIsFeatured(),
            item.getIsVegetarian(),
            item.getCategoryId(),
            item.getPrepTimeMinutes(),
            item.getCalories(),
            variants,
            groups);
    }

    private PublicMenuResponse buildMenu(Tenant tenant, Restaurant restaurant) {
        List<MenuCategory> categories = categoryRepository
            .findByTenantIdOrderBySortOrderAscNameAsc(tenant.getId());
        List<MenuItem> items = itemRepository.searchPublic(tenant.getId(), null, null);
        Map<Long, List<MenuItem>> byCategory = items.stream()
            .collect(Collectors.groupingBy(MenuItem::getCategoryId));

        List<PublicMenuResponse.CategoryGroup> groups = categories.stream()
            .map(category -> new PublicMenuResponse.CategoryGroup(
                category.getId(),
                category.getName(),
                category.getDescription(),
                byCategory.getOrDefault(category.getId(), List.of()).stream()
                    .map(PublicMenuResponse.ItemView::from)
                    .toList()))
            .filter(group -> !group.items().isEmpty())
            .toList();

        return new PublicMenuResponse(
            restaurant.getName(), tenant.getSlug(), restaurant.getCurrency(), groups);
    }
}
