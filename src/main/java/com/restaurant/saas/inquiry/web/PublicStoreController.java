package com.restaurant.saas.inquiry.web;

import com.restaurant.saas.inquiry.dto.PublicStoreProductsResponse;
import com.restaurant.saas.inquiry.dto.StoreView;
import com.restaurant.saas.inquiry.service.InquiryService;
import com.restaurant.saas.inquiry.service.InquiryService.InquiryCreatedView;
import com.restaurant.saas.inquiry.service.InquiryService.InquiryRequest;
import com.restaurant.saas.inquiry.service.PublicStoreService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/stores")
public class PublicStoreController {

    private final PublicStoreService publicStoreService;
    private final InquiryService inquiryService;

    public PublicStoreController(PublicStoreService publicStoreService, InquiryService inquiryService) {
        this.publicStoreService = publicStoreService;
        this.inquiryService = inquiryService;
    }

    @GetMapping("/{slug}")
    public StoreView store(@PathVariable String slug) {
        return publicStoreService.store(slug);
    }

    @GetMapping("/{slug}/products")
    public PublicStoreProductsResponse products(@PathVariable String slug) {
        return publicStoreService.products(slug);
    }

    @GetMapping("/{slug}/products/{itemId}")
    public PublicStoreProductsResponse.ProductView product(
        @PathVariable String slug,
        @PathVariable Long itemId) {
        return publicStoreService.product(slug, itemId);
    }

    @PostMapping("/{slug}/inquiries")
    public InquiryCreatedView inquire(
        @PathVariable String slug,
        @Valid @RequestBody InquiryRequest request) {
        return inquiryService.createFromStore(slug, request);
    }
}
