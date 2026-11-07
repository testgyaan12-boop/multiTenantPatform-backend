package com.restaurant.saas.customer.web;

import com.restaurant.saas.customer.dto.GuestSessionRequest;
import com.restaurant.saas.customer.dto.GuestSessionResponse;
import com.restaurant.saas.customer.service.GuestSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/guest-sessions")
public class PublicGuestSessionController {

    private final GuestSessionService guestSessionService;

    public PublicGuestSessionController(GuestSessionService guestSessionService) {
        this.guestSessionService = guestSessionService;
    }

    @PostMapping
    public ResponseEntity<GuestSessionResponse> start(@RequestBody GuestSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(guestSessionService.start(request.tableCode(), request.restaurantSlug()));
    }
}
