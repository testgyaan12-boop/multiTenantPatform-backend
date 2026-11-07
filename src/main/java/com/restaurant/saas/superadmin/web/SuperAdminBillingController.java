package com.restaurant.saas.superadmin.web;

import com.restaurant.saas.common.web.PageResponse;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService.InvoiceView;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService.PlanPriceHistoryView;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService.PlanRequest;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService.PlanStatusRequest;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService.PlanView;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService.SubscriptionRequest;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService.SubscriptionView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/superadmin")
public class SuperAdminBillingController {

    private final SuperAdminPlatformService platformService;

    public SuperAdminBillingController(SuperAdminPlatformService platformService) {
        this.platformService = platformService;
    }

    @GetMapping("/subscriptions")
    public PageResponse<SubscriptionView> subscriptions(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String planCode,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Long tenantId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return platformService.listSubscriptions(status, planCode, search, tenantId, page, size);
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<SubscriptionView> assign(@Valid @RequestBody SubscriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(platformService.assignSubscription(request));
    }

    @PostMapping("/subscriptions/{id}/cancel")
    public SubscriptionView cancel(@PathVariable Long id) {
        return platformService.changeSubscriptionStatus(id, "CANCELLED");
    }

    @PostMapping("/subscriptions/{id}/activate")
    public SubscriptionView activate(@PathVariable Long id) {
        return platformService.changeSubscriptionStatus(id, "ACTIVE");
    }

    @GetMapping("/plans")
    public List<PlanView> plans() {
        return platformService.listPlans();
    }

    @GetMapping("/plans/{code}")
    public PlanView plan(@PathVariable String code) {
        return platformService.getPlan(code);
    }

    @PostMapping("/plans/{code}/status")
    public PlanView changePlanStatus(@PathVariable String code,
                                     @RequestBody PlanStatusRequest request) {
        return platformService.changePlanStatus(code, request.status());
    }

    @GetMapping("/plans/{code}/price-history")
    public List<PlanPriceHistoryView> planPriceHistory(@PathVariable String code) {
        return platformService.priceHistory(code);
    }

    @PostMapping("/plans")
    public ResponseEntity<PlanView> createPlan(@Valid @RequestBody PlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(platformService.createPlan(request));
    }

    @PutMapping("/plans/{code}")
    public PlanView updatePlan(@PathVariable String code, @Valid @RequestBody PlanRequest request) {
        return platformService.updatePlan(code, request);
    }

    @DeleteMapping("/plans/{code}")
    public ResponseEntity<Void> deletePlan(@PathVariable String code) {
        platformService.deletePlan(code);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/invoices")
    public PageResponse<InvoiceView> invoices(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Long tenantId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return platformService.listInvoices(status, search, tenantId, page, size);
    }

    @PostMapping("/invoices/{id}/mark-paid")
    public InvoiceView markPaid(@PathVariable Long id) {
        return platformService.updateInvoiceStatus(id, "PAID");
    }

    @PostMapping("/invoices/{id}/void")
    public InvoiceView voidInvoice(@PathVariable Long id) {
        return platformService.updateInvoiceStatus(id, "VOID");
    }
}
