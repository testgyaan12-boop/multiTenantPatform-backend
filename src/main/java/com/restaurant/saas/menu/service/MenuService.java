package com.restaurant.saas.menu.service;

import com.restaurant.saas.common.exception.ConflictException;
import com.restaurant.saas.common.exception.NotFoundException;
import com.restaurant.saas.common.tenant.TenantContext;
import com.restaurant.saas.menu.dto.MenuCategoryRequest;
import com.restaurant.saas.menu.dto.MenuCategoryResponse;
import com.restaurant.saas.menu.dto.MenuItemRequest;
import com.restaurant.saas.menu.dto.MenuItemResponse;
import com.restaurant.saas.menu.entity.ItemAttribute;
import com.restaurant.saas.menu.entity.MenuCategory;
import com.restaurant.saas.menu.entity.MenuItem;
import com.restaurant.saas.menu.entity.MenuItemImage;
import com.restaurant.saas.menu.repository.ItemAttributeRepository;
import com.restaurant.saas.menu.repository.MenuCategoryRepository;
import com.restaurant.saas.menu.repository.MenuItemImageRepository;
import com.restaurant.saas.menu.repository.MenuItemRepository;
import com.restaurant.saas.restaurant.entity.Restaurant;
import com.restaurant.saas.restaurant.repository.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MenuService {

    private final MenuCategoryRepository categoryRepository;
    private final MenuItemRepository itemRepository;
    private final RestaurantRepository restaurantRepository;
    private final ItemAttributeRepository attributeRepository;
    private final MenuItemImageRepository imageRepository;

    public MenuService(
        MenuCategoryRepository categoryRepository,
        MenuItemRepository itemRepository,
        RestaurantRepository restaurantRepository,
        ItemAttributeRepository attributeRepository,
        MenuItemImageRepository imageRepository) {
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
        this.restaurantRepository = restaurantRepository;
        this.attributeRepository = attributeRepository;
        this.imageRepository = imageRepository;
    }

    @Transactional(readOnly = true)
    public List<MenuCategoryResponse> listCategories() {
        return categoryRepository.findByTenantIdOrderBySortOrderAscNameAsc(TenantContext.requireTenant())
            .stream().map(MenuCategoryResponse::from).toList();
    }

    @Transactional
    public MenuCategoryResponse createCategory(MenuCategoryRequest request) {
        Long tenantId = TenantContext.requireTenant();
        Restaurant restaurant = restaurantRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new NotFoundException("Restaurant profile is not configured"));
        if (categoryRepository.existsByTenantIdAndRestaurantIdAndName(tenantId, restaurant.getId(), request.name())) {
            throw new ConflictException("category_exists", "A category with this name already exists");
        }
        MenuCategory category = new MenuCategory();
        category.setTenantId(tenantId);
        category.setRestaurantId(restaurant.getId());
        category.setName(request.name());
        category.setDescription(request.description());
        category.setImageUrl(request.imageUrl());
        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }
        return MenuCategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> listItems(Long categoryId, String search) {
        Long tenantId = TenantContext.requireTenant();
        List<MenuItem> items;
        if (search != null && !search.isBlank()) {
            items = itemRepository.searchByTenant(tenantId, categoryId, search.trim());
        } else {
            items = categoryId == null
                ? itemRepository.findByTenantIdOrderBySortOrderAscNameAsc(tenantId)
                : itemRepository.findByTenantIdAndCategoryIdOrderBySortOrderAscNameAsc(tenantId, categoryId);
        }
        if (items.isEmpty()) {
            return List.of();
        }
        List<Long> itemIds = items.stream().map(MenuItem::getId).toList();
        Map<Long, List<ItemAttribute>> attributesByItem = attributeRepository
            .findByTenantIdAndMenuItemIdIn(tenantId, itemIds)
            .stream()
            .collect(Collectors.groupingBy(ItemAttribute::getMenuItemId));
        Map<Long, List<MenuItemImage>> imagesByItem = imageRepository
            .findByTenantIdAndMenuItemIdIn(tenantId, itemIds)
            .stream()
            .collect(Collectors.groupingBy(MenuItemImage::getMenuItemId));
        return items.stream()
            .map(item -> MenuItemResponse.from(
                item,
                attributesByItem.getOrDefault(item.getId(), List.of()),
                imagesByItem.getOrDefault(item.getId(), List.of())))
            .toList();
    }

    @Transactional
    public MenuCategoryResponse updateCategory(Long id, MenuCategoryRequest request) {
        Long tenantId = TenantContext.requireTenant();
        MenuCategory category = categoryRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new NotFoundException("Menu category", id));
        if (categoryRepository.existsByTenantIdAndRestaurantIdAndNameAndIdNot(
                tenantId, category.getRestaurantId(), request.name(), id)) {
            throw new ConflictException("category_exists", "A category with this name already exists");
        }
        category.setName(request.name());
        category.setDescription(request.description());
        category.setImageUrl(request.imageUrl());
        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }
        return MenuCategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        Long tenantId = TenantContext.requireTenant();
        MenuCategory category = categoryRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new NotFoundException("Menu category", id));
        if (itemRepository.countByTenantIdAndCategoryId(tenantId, id) > 0) {
            throw new ConflictException(
                "category_in_use", "Move or delete the items in this category first");
        }
        category.softDelete();
        categoryRepository.save(category);
    }

    @Transactional
    public MenuItemResponse createItem(MenuItemRequest request) {
        Long tenantId = TenantContext.requireTenant();
        Restaurant restaurant = restaurantRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new NotFoundException("Restaurant profile is not configured"));
        MenuCategory category = categoryRepository.findByIdAndTenantId(request.categoryId(), tenantId)
            .orElseThrow(() -> new NotFoundException("Menu category", request.categoryId()));
        if (itemRepository.existsByTenantIdAndSku(tenantId, request.sku())) {
            throw new ConflictException("sku_taken", "An item with this SKU already exists");
        }
        MenuItem item = new MenuItem();
        item.setTenantId(tenantId);
        item.setRestaurantId(restaurant.getId());
        item.setCategoryId(category.getId());
        item.setCurrency(restaurant.getCurrency());
        apply(item, request);
        MenuItem saved = itemRepository.save(item);
        replaceAttributes(tenantId, saved, request.attributes());
        replaceImages(tenantId, saved, request.images());
        return MenuItemResponse.from(
            saved,
            attributeRepository.findByTenantIdAndMenuItemId(tenantId, saved.getId()),
            imageRepository.findByTenantIdAndMenuItemId(tenantId, saved.getId()));
    }

    @Transactional
    public MenuItemResponse updateItem(Long id, MenuItemRequest request) {
        Long tenantId = TenantContext.requireTenant();
        MenuItem item = itemRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new NotFoundException("Menu item", id));
        categoryRepository.findByIdAndTenantId(request.categoryId(), tenantId)
            .orElseThrow(() -> new NotFoundException("Menu category", request.categoryId()));
        item.setCategoryId(request.categoryId());
        apply(item, request);
        MenuItem saved = itemRepository.save(item);
        replaceAttributes(tenantId, saved, request.attributes());
        replaceImages(tenantId, saved, request.images());
        return MenuItemResponse.from(
            saved,
            attributeRepository.findByTenantIdAndMenuItemId(tenantId, saved.getId()),
            imageRepository.findByTenantIdAndMenuItemId(tenantId, saved.getId()));
    }

    @Transactional
    public void deleteItem(Long id) {
        MenuItem item = itemRepository.findByIdAndTenantId(id, TenantContext.requireTenant())
            .orElseThrow(() -> new NotFoundException("Menu item", id));
        imageRepository.deleteByMenuItemId(item.getId());
        item.softDelete();
        itemRepository.save(item);
    }

    private static void apply(MenuItem item, MenuItemRequest request) {
        item.setSku(request.sku());
        item.setName(request.name());
        item.setDescription(request.description());
        item.setPrice(request.price());
        item.setCost(request.cost());
        item.setImageUrl(request.imageUrl());
        if (request.isAvailable() != null) {
            item.setIsAvailable(request.isAvailable());
        }
        if (request.isFeatured() != null) {
            item.setIsFeatured(request.isFeatured());
        }
        item.setPrepTimeMinutes(request.prepTimeMinutes());
        item.setCalories(request.calories());
        item.setStockQty(request.stockQty());
        if (request.sortOrder() != null) {
            item.setSortOrder(request.sortOrder());
        }
    }

    private void replaceAttributes(
        Long tenantId, MenuItem item, List<MenuItemRequest.AttributeInput> attributes) {
        attributeRepository.deleteByMenuItemId(item.getId());
        if (attributes == null || attributes.isEmpty()) {
            return;
        }
        int sortOrder = 0;
        for (MenuItemRequest.AttributeInput attribute : attributes) {
            if (attribute.key() == null || attribute.key().isBlank()
                || attribute.value() == null || attribute.value().isBlank()) {
                continue;
            }
            ItemAttribute entity = new ItemAttribute();
            entity.setTenantId(tenantId);
            entity.setMenuItemId(item.getId());
            entity.setAttrKey(attribute.key().trim());
            entity.setAttrValue(attribute.value().trim());
            entity.setSortOrder(sortOrder++);
            attributeRepository.save(entity);
        }
    }

    private void replaceImages(
        Long tenantId, MenuItem item, List<MenuItemRequest.ImageInput> images) {
        if (images == null) {
            return;
        }
        imageRepository.deleteByMenuItemId(item.getId());
        if (images.isEmpty()) {
            item.setImageUrl(null);
            itemRepository.save(item);
            return;
        }
        int sortOrder = 0;
        String coverUrl = null;
        for (MenuItemRequest.ImageInput image : images) {
            if (image.url() == null || image.url().isBlank()
                || image.label() == null || image.label().isBlank()) {
                continue;
            }
            MenuItemImage entity = new MenuItemImage();
            entity.setTenantId(tenantId);
            entity.setMenuItemId(item.getId());
            entity.setImageUrl(image.url().trim());
            entity.setLabel(image.label().trim());
            entity.setSortOrder(sortOrder++);
            imageRepository.save(entity);
            if (coverUrl == null) {
                coverUrl = entity.getImageUrl();
            }
        }
        item.setImageUrl(coverUrl);
        itemRepository.save(item);
    }
}
