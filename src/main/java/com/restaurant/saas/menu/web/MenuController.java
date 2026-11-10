package com.restaurant.saas.menu.web;

import com.restaurant.saas.menu.dto.MenuCategoryRequest;
import com.restaurant.saas.menu.dto.MenuCategoryResponse;
import com.restaurant.saas.menu.dto.MenuItemRequest;
import com.restaurant.saas.menu.dto.MenuItemResponse;
import com.restaurant.saas.menu.service.MenuService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyAuthority('menu.read', 'product.read')")
    public List<MenuCategoryResponse> categories() {
        return menuService.listCategories();
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAnyAuthority('menu.manage', 'product.manage')")
    public ResponseEntity<MenuCategoryResponse> createCategory(
        @Valid @RequestBody MenuCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(menuService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAnyAuthority('menu.manage', 'product.manage')")
    public MenuCategoryResponse updateCategory(
        @PathVariable Long id,
        @Valid @RequestBody MenuCategoryRequest request) {
        return menuService.updateCategory(id, request);
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasAnyAuthority('menu.manage', 'product.manage')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        menuService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/items")
    @PreAuthorize("hasAnyAuthority('menu.read', 'product.read')")
    public List<MenuItemResponse> items(
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String search) {
        return menuService.listItems(categoryId, search);
    }

    @PostMapping("/items")
    @PreAuthorize("hasAnyAuthority('menu.manage', 'product.manage')")
    public ResponseEntity<MenuItemResponse> createItem(@Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(menuService.createItem(request));
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasAnyAuthority('menu.manage', 'product.manage')")
    public MenuItemResponse updateItem(@PathVariable Long id, @Valid @RequestBody MenuItemRequest request) {
        return menuService.updateItem(id, request);
    }

    @DeleteMapping("/items/{id}")
    @PreAuthorize("hasAnyAuthority('menu.manage', 'product.manage')")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        menuService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
