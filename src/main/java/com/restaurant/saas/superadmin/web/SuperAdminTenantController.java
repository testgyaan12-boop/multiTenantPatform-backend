package com.restaurant.saas.superadmin.web;

import com.restaurant.saas.common.web.PageResponse;
import com.restaurant.saas.common.web.PageableFactory;
import com.restaurant.saas.superadmin.service.StoreSettingsService;
import com.restaurant.saas.superadmin.service.StoreSettingsService.StoreSettingsRequest;
import com.restaurant.saas.superadmin.service.StoreSettingsService.StoreSettingsView;
import com.restaurant.saas.tenant.dto.TenantCreateRequest;
import com.restaurant.saas.tenant.dto.TenantResponse;
import com.restaurant.saas.tenant.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/superadmin/tenants")
public class SuperAdminTenantController {

    private final TenantService tenantService;
    private final StoreSettingsService storeSettingsService;

    public SuperAdminTenantController(TenantService tenantService, StoreSettingsService storeSettingsService) {
        this.tenantService = tenantService;
        this.storeSettingsService = storeSettingsService;
    }

    @GetMapping
    public PageResponse<TenantResponse> list(
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt") String sort,
        @RequestParam(defaultValue = "desc") String direction) {
        return tenantService.list(search, PageableFactory.of(page, size, sort, direction));
    }

    @GetMapping("/{id}")
    public TenantResponse get(@PathVariable Long id) {
        return tenantService.get(id);
    }

    @PostMapping
    public ResponseEntity<TenantResponse> create(@Valid @RequestBody TenantCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tenantService.create(request));
    }

    @PostMapping("/{id}/suspend")
    public TenantResponse suspend(@PathVariable Long id) {
        return tenantService.suspend(id);
    }

    @PostMapping("/{id}/activate")
    public TenantResponse activate(@PathVariable Long id) {
        return tenantService.activate(id);
    }

    @PostMapping("/{id}/verify")
    public TenantResponse verify(@PathVariable Long id) {
        return tenantService.verify(id);
    }

    @PostMapping("/{id}/reject")
    public TenantResponse reject(@PathVariable Long id) {
        return tenantService.reject(id);
    }

    @GetMapping("/{id}/store-settings")
    public StoreSettingsView storeSettings(@PathVariable Long id) {
        return storeSettingsService.view(id);
    }

    @PutMapping("/{id}/store-settings")
    public StoreSettingsView updateStoreSettings(
        @PathVariable Long id,
        @Valid @RequestBody StoreSettingsRequest request) {
        return storeSettingsService.update(id, request);
    }
}
