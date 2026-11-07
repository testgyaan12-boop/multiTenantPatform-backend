package com.restaurant.saas.identity.dto;

public record AuthResponse(String accessToken, long expiresIn, AuthUserResponse user) {
}
