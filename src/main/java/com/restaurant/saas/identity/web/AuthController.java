package com.restaurant.saas.identity.web;

import com.restaurant.saas.identity.dto.AuthResponse;
import com.restaurant.saas.identity.dto.AuthUserResponse;
import com.restaurant.saas.identity.dto.LoginRequest;
import com.restaurant.saas.identity.dto.RegistrationResponse;
import com.restaurant.saas.identity.dto.RestaurantRegistrationRequest;
import com.restaurant.saas.identity.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_COOKIE = "resto_refresh";

    private final AuthService authService;
    private final long refreshTtlSeconds;

    public AuthController(
        AuthService authService,
        com.restaurant.saas.authorization.security.SecurityProperties properties) {
        this.authService = authService;
        this.refreshTtlSeconds = Duration.ofDays(properties.jwt().refreshTtlDays()).toSeconds();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse) {
        AuthService.LoginResult result = authService.login(
            request.email(),
            request.password(),
            httpRequest.getHeader(HttpHeaders.USER_AGENT),
            httpRequest.getRemoteAddr());
        setRefreshCookie(httpResponse, result.refreshToken());
        return ResponseEntity.ok(result.response());
    }

    @PostMapping("/register-restaurant")
    public ResponseEntity<RegistrationResponse> registerRestaurant(
        @Valid @RequestBody RestaurantRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(authService.registerRestaurant(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse) {
        String presented = readRefreshCookie(httpRequest);
        AuthService.LoginResult result = authService.refresh(
            presented,
            httpRequest.getHeader(HttpHeaders.USER_AGENT),
            httpRequest.getRemoteAddr());
        setRefreshCookie(httpResponse, result.refreshToken());
        return ResponseEntity.ok(result.response());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse) {
        authService.logout(readRefreshCookie(httpRequest));
        clearRefreshCookie(httpResponse);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public AuthUserResponse me() {
        return authService.currentUser();
    }

    @PostMapping("/accept-invite")
    public ResponseEntity<Void> acceptInvite(
        @Valid @RequestBody com.restaurant.saas.identity.dto.AcceptInviteRequest request) {
        authService.acceptInvite(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }

    private static String readRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
            .filter(cookie -> REFRESH_COOKIE.equals(cookie.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);
    }

    private void setRefreshCookie(HttpServletResponse response, String value) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge((int) refreshTtlSeconds);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private static void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
