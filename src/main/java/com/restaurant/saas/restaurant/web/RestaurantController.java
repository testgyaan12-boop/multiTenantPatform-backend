package com.restaurant.saas.restaurant.web;

import com.restaurant.saas.restaurant.dto.RestaurantResponse;
import com.restaurant.saas.restaurant.dto.RestaurantSubscriptionResponse;
import com.restaurant.saas.restaurant.dto.RestaurantUpdateRequest;
import com.restaurant.saas.restaurant.service.RestaurantService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/restaurant")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('restaurant.read')")
    public RestaurantResponse current() {
        return restaurantService.current();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('restaurant.update')")
    public RestaurantResponse update(@Valid @RequestBody RestaurantUpdateRequest request) {
        return restaurantService.update(request);
    }

    @GetMapping("/subscription")
    @PreAuthorize("hasAuthority('restaurant.read')")
    public RestaurantSubscriptionResponse subscription() {
        return restaurantService.subscription();
    }
}
