package com.restaurant.saas.restaurant.web;

import com.restaurant.saas.menu.dto.PublicItemDetailResponse;
import com.restaurant.saas.menu.dto.PublicMenuResponse;
import com.restaurant.saas.menu.service.PublicMenuService;
import com.restaurant.saas.restaurant.dto.PublicRestaurantResponse;
import com.restaurant.saas.restaurant.service.PublicRestaurantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/restaurants")
public class PublicRestaurantController {

    private final PublicRestaurantService publicRestaurantService;
    private final PublicMenuService publicMenuService;

    public PublicRestaurantController(
        PublicRestaurantService publicRestaurantService,
        PublicMenuService publicMenuService) {
        this.publicRestaurantService = publicRestaurantService;
        this.publicMenuService = publicMenuService;
    }

    @GetMapping("/{slug}")
    public PublicRestaurantResponse restaurant(@PathVariable String slug) {
        return publicRestaurantService.bySlug(slug);
    }

    @GetMapping("/{slug}/menu")
    public PublicMenuResponse menu(@PathVariable String slug) {
        return publicMenuService.menuForSlug(slug);
    }

    @GetMapping("/{slug}/items/{itemId}")
    public PublicItemDetailResponse item(@PathVariable String slug, @PathVariable Long itemId) {
        return publicMenuService.itemDetail(slug, itemId);
    }
}
