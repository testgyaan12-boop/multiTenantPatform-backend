package com.restaurant.saas.identity.web;

import com.restaurant.saas.common.web.PageResponse;
import com.restaurant.saas.identity.dto.ActivityItemResponse;
import com.restaurant.saas.identity.dto.AuthUserResponse;
import com.restaurant.saas.identity.dto.ChangePasswordRequest;
import com.restaurant.saas.identity.dto.UpdateProfileRequest;
import com.restaurant.saas.identity.dto.UserPreferencesRequest;
import com.restaurant.saas.identity.dto.UserPreferencesResponse;
import com.restaurant.saas.identity.dto.UserSessionResponse;
import com.restaurant.saas.identity.service.UserProfileService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth/me")
public class UserProfileController {

    private static final String REFRESH_COOKIE = "resto_refresh";

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @PutMapping
    public AuthUserResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return userProfileService.updateProfile(request);
    }
    @PostMapping("/change-password")
    public void changePassword(
        @Valid @RequestBody ChangePasswordRequest request,
        HttpServletRequest httpRequest) {
        userProfileService.changePassword(request, readRefreshCookie(httpRequest));
    }

    @GetMapping("/sessions")
    public List<UserSessionResponse> sessions(HttpServletRequest httpRequest) {
        return userProfileService.listSessions(readRefreshCookie(httpRequest));
    }

    @DeleteMapping("/sessions/{id}")
    public void revokeSession(@PathVariable Long id, HttpServletRequest httpRequest) {
        userProfileService.revokeSession(id, readRefreshCookie(httpRequest));
    }

    @PostMapping("/sessions/revoke-others")
    public void revokeOtherSessions(HttpServletRequest httpRequest) {
        userProfileService.revokeOtherSessions(readRefreshCookie(httpRequest));
    }

    @GetMapping("/preferences")
    public UserPreferencesResponse preferences() {
        return userProfileService.getPreferences();
    }

    @PutMapping("/preferences")
    public UserPreferencesResponse savePreferences(@Valid @RequestBody UserPreferencesRequest request) {
        return userProfileService.savePreferences(request);
    }

    @GetMapping("/activity")
    public PageResponse<ActivityItemResponse> activity(
        @RequestParam(required = false) String from,
        @RequestParam(required = false) String to,
        @RequestParam(required = false) String action,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return userProfileService.activity(from, to, action, page, size);
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
}
