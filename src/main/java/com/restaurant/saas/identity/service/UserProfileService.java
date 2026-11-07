package com.restaurant.saas.identity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.saas.audit.entity.AuditLog;
import com.restaurant.saas.audit.repository.AuditLogRepository;
import com.restaurant.saas.audit.service.AuditService;
import com.restaurant.saas.authorization.security.AuthenticatedPrincipal;
import com.restaurant.saas.common.exception.ApiException;
import com.restaurant.saas.common.exception.NotFoundException;
import com.restaurant.saas.common.web.PageResponse;
import com.restaurant.saas.identity.dto.ActivityItemResponse;
import com.restaurant.saas.identity.dto.AuthUserResponse;
import com.restaurant.saas.identity.dto.ChangePasswordRequest;
import com.restaurant.saas.identity.dto.UpdateProfileRequest;
import com.restaurant.saas.identity.dto.UserPreferencesRequest;
import com.restaurant.saas.identity.dto.UserPreferencesResponse;
import com.restaurant.saas.identity.dto.UserSessionResponse;
import com.restaurant.saas.identity.entity.RefreshToken;
import com.restaurant.saas.identity.entity.User;
import com.restaurant.saas.identity.entity.UserPreferences;
import com.restaurant.saas.identity.repository.RefreshTokenRepository;
import com.restaurant.saas.identity.repository.UserPreferencesRepository;
import com.restaurant.saas.identity.repository.UserRepository;
import com.restaurant.saas.tenant.repository.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final AuditLogRepository auditLogRepository;
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;

    public UserProfileService(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        UserPreferencesRepository preferencesRepository,
        PasswordEncoder passwordEncoder,
        AuditService auditService,
        AuditLogRepository auditLogRepository,
        TenantRepository tenantRepository,
        ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.preferencesRepository = preferencesRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.auditLogRepository = auditLogRepository;
        this.tenantRepository = tenantRepository;
        this.objectMapper = objectMapper;
    }

    // ---------- profile ----------

    @Transactional
    public AuthUserResponse updateProfile(UpdateProfileRequest request) {
        User user = currentUser();
        if (request.firstName() != null && !request.firstName().isBlank()) {
            user.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            user.setLastName(request.lastName().trim());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone().isBlank() ? null : request.phone().trim());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl().isBlank() ? null : request.avatarUrl().trim());
        }
        if (request.displayName() != null) {
            user.setDisplayName(request.displayName().isBlank() ? null : request.displayName().trim());
        }
        if (request.bio() != null) {
            user.setBio(request.bio().isBlank() ? null : request.bio().trim());
        }
        User saved = userRepository.save(user);
        auditService.record(saved.getTenantId(), saved.getId(), saved.getEmail(), "PROFILE_UPDATED", "User",
            String.valueOf(saved.getId()), null, toJson(request));
        AuthenticatedPrincipal principal = toPrincipal(saved);
        return toResponse(saved, principal);
    }

    // ---------- password ----------

    @Transactional
    public void changePassword(ChangePasswordRequest request, String currentRefreshToken) {
        User user = currentUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_current_password",
                "Your current password is not correct");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "password_unchanged",
                "Your new password must be different from the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        Instant now = Instant.now();
        int revoked;
        if (currentRefreshToken != null && !currentRefreshToken.isBlank()) {
            revoked = refreshTokenRepository.revokeAllForUserExcept(user.getId(), sha256(currentRefreshToken), now);
        } else {
            revoked = refreshTokenRepository.revokeAllForUser(user.getId(), now);
        }
        auditService.record(user.getTenantId(), user.getId(), user.getEmail(), "PASSWORD_CHANGED", "User",
            String.valueOf(user.getId()), null, "{\"otherSessionsRevoked\":" + revoked + "}");
    }

    // ---------- sessions ----------

    @Transactional(readOnly = true)
    public List<UserSessionResponse> listSessions(String currentRefreshToken) {
        User user = currentUser();
        String currentHash = currentRefreshToken == null || currentRefreshToken.isBlank()
            ? null
            : sha256(currentRefreshToken);
        return refreshTokenRepository.findActiveForUser(user.getId(), Instant.now()).stream()
            .map(token -> new UserSessionResponse(
                token.getId(),
                describeDevice(token.getUserAgent()),
                token.getUserAgent(),
                token.getIpAddress(),
                token.getCreatedAt(),
                token.getExpiresAt(),
                currentHash != null && currentHash.equals(token.getTokenHash())
            ))
            .collect(Collectors.toList());
    }

    @Transactional
    public void revokeSession(Long sessionId, String currentRefreshToken) {
        User user = currentUser();
        RefreshToken token = refreshTokenRepository.findById(sessionId)
            .filter(t -> user.getId().equals(t.getUserId()))
            .filter(t -> t.getRevokedAt() == null)
            .orElseThrow(() -> new NotFoundException("Session not found"));
        refreshTokenRepository.revokeByIdForUser(token.getId(), user.getId(), Instant.now());
        auditService.record(user.getTenantId(), user.getId(), user.getEmail(), "SESSION_REVOKED", "RefreshToken",
            String.valueOf(token.getId()), null, null);
    }

    @Transactional
    public int revokeOtherSessions(String currentRefreshToken) {
        User user = currentUser();
        if (currentRefreshToken == null || currentRefreshToken.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "missing_session", "No active session was presented");
        }
        int revoked = refreshTokenRepository.revokeAllForUserExcept(user.getId(), sha256(currentRefreshToken),
            Instant.now());
        auditService.record(user.getTenantId(), user.getId(), user.getEmail(), "OTHER_SESSIONS_REVOKED", "User",
            String.valueOf(user.getId()), null, "{\"revoked\":" + revoked + "}");
        return revoked;
    }

    // ---------- preferences ----------

    @Transactional(readOnly = true)
    public UserPreferencesResponse getPreferences() {
        User user = currentUser();
        return preferencesRepository.findByUserId(user.getId())
            .map(UserProfileService::toPreferencesResponse)
            .orElseGet(UserProfileService::defaultPreferences);
    }

    @Transactional
    public UserPreferencesResponse savePreferences(UserPreferencesRequest request) {
        User user = currentUser();
        UserPreferences preferences = preferencesRepository.findByUserId(user.getId())
            .orElseGet(() -> {
                UserPreferences created = new UserPreferences();
                created.setUserId(user.getId());
                return created;
            });
        preferences.setTheme(request.theme());
        preferences.setTimeZone(request.timeZone());
        preferences.setDateFormat(request.dateFormat());
        preferences.setLanguage(request.language());
        if (request.emailNotifications() != null) {
            preferences.setEmailNotifications(request.emailNotifications());
        }
        if (request.inAppNotifications() != null) {
            preferences.setInAppNotifications(request.inAppNotifications());
        }
        UserPreferences saved = preferencesRepository.save(preferences);
        auditService.record(user.getTenantId(), user.getId(), user.getEmail(), "PREFERENCES_UPDATED",
            "UserPreferences", String.valueOf(saved.getId()), null, toJson(request));
        return toPreferencesResponse(saved);
    }

    // ---------- activity ----------

    @Transactional(readOnly = true)
    public PageResponse<ActivityItemResponse> activity(String from, String to, String action, int page, int size) {
        User user = currentUser();
        Instant fromInstant = null;
        Instant toInstant = null;
        try {
            if (from != null && !from.isBlank()) {
                fromInstant = LocalDate.parse(from).atStartOfDay(ZoneId.systemDefault()).toInstant();
            }
            if (to != null && !to.isBlank()) {
                toInstant = LocalDate.parse(to).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            }
        } catch (DateTimeParseException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_date",
                "Dates must be provided as YYYY-MM-DD");
        }
        String needle = action == null || action.isBlank() ? null : action.trim().toLowerCase(Locale.ROOT);
        final Instant rangeStart = fromInstant;
        final Instant rangeEnd = toInstant;

        List<ActivityItemResponse> items = auditLogRepository.findAll().stream()
            .filter(log -> user.getId().equals(log.getActorId())
                || (log.getActorEmail() != null && log.getActorEmail().equalsIgnoreCase(user.getEmail())))
            .filter(log -> rangeStart == null || !log.getCreatedAt().isBefore(rangeStart))
            .filter(log -> rangeEnd == null || log.getCreatedAt().isBefore(rangeEnd))
            .map(UserProfileService::toActivityItem)
            .filter(item -> needle == null || item.action().toLowerCase(Locale.ROOT).contains(needle))
            .sorted(Comparator.comparing(ActivityItemResponse::timestamp,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());

        int safeSize = Math.max(1, Math.min(size, 100));
        int safePage = Math.max(0, page);
        int total = items.size();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        int fromIndex = Math.min(safePage * safeSize, total);
        int toIndex = Math.min(fromIndex + safeSize, total);
        List<ActivityItemResponse> slice = items.subList(fromIndex, toIndex);
        return new PageResponse<>(slice,
            new PageResponse.PageMeta(safePage, safeSize, total, totalPages));
    }

    // ---------- helpers ----------

    private User currentUser() {
        AuthenticatedPrincipal principal = currentPrincipal();
        return userRepository.findActiveByIdWithAuthorities(principal.userId())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "invalid_token",
                "User is no longer active"));
    }

    private AuthenticatedPrincipal currentPrincipal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
            || !(authentication.getPrincipal() instanceof AuthenticatedPrincipal p)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "Authentication is required");
        }
        return p;
    }

    private AuthUserResponse toResponse(User user, AuthenticatedPrincipal principal) {
        String tenantName = principal.tenantId() == null
            ? null
            : tenantRepository.findById(principal.tenantId()).map(t -> t.getName()).orElse(null);
        return new AuthUserResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            principal.roles(),
            principal.permissions(),
            principal.tenantId(),
            tenantName,
            user.getPhone(),
            user.getAvatarUrl(),
            user.getDisplayName(),
            user.getBio(),
            user.getEmailVerified(),
            user.getStatus(),
            user.getLastLoginAt(),
            user.getCreatedAt()
        );
    }

    private AuthenticatedPrincipal toPrincipal(User user) {
        java.util.Set<String> roles = user.getRoles().stream()
            .map(com.restaurant.saas.identity.entity.Role::getCode)
            .collect(Collectors.toSet());
        java.util.Set<String> permissions = user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .map(com.restaurant.saas.identity.entity.Permission::getCode)
            .collect(Collectors.toSet());
        return new AuthenticatedPrincipal(user.getId(), user.getTenantId(), user.getEmail(), roles, permissions);
    }

    private static UserPreferencesResponse toPreferencesResponse(UserPreferences preferences) {
        return new UserPreferencesResponse(
            preferences.getTheme(),
            preferences.getTimeZone(),
            preferences.getDateFormat(),
            preferences.getLanguage(),
            Boolean.TRUE.equals(preferences.getEmailNotifications()),
            Boolean.TRUE.equals(preferences.getInAppNotifications())
        );
    }

    private static UserPreferencesResponse defaultPreferences() {
        return new UserPreferencesResponse("SYSTEM", "Asia/Kolkata", "DD/MM/YYYY", "en", true, true);
    }

    private static ActivityItemResponse toActivityItem(AuditLog log) {
        String details = null;
        if (log.getEntityType() != null) {
            details = log.getEntityId() == null
                ? log.getEntityType()
                : log.getEntityType() + " #" + log.getEntityId();
        }
        String result = log.getAction() != null && log.getAction().toUpperCase(Locale.ROOT).endsWith("FAILED")
            ? "FAILED"
            : "SUCCESS";
        String actor = log.getActorEmail() != null
            ? log.getActorEmail()
            : (log.getActorId() != null ? "user#" + log.getActorId() : "system");
        return new ActivityItemResponse(
            log.getId(),
            log.getCreatedAt(),
            log.getAction(),
            log.getEntityType(),
            actor,
            details,
            result
        );
    }

    private static String describeDevice(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown device";
        }
        String browser = "Browser";
        if (userAgent.contains("Edg/")) {
            browser = "Edge";
        } else if (userAgent.contains("OPR/") || userAgent.contains("Opera")) {
            browser = "Opera";
        } else if (userAgent.contains("Firefox/")) {
            browser = "Firefox";
        } else if (userAgent.contains("Chrome/")) {
            browser = "Chrome";
        } else if (userAgent.contains("Safari/")) {
            browser = "Safari";
        }
        String os = "Unknown OS";
        if (userAgent.contains("Windows")) {
            os = "Windows";
        } else if (userAgent.contains("Android")) {
            os = "Android";
        } else if (userAgent.contains("iPhone") || userAgent.contains("iPad") || userAgent.contains("iOS")) {
            os = "iOS";
        } else if (userAgent.contains("Mac OS") || userAgent.contains("Macintosh")) {
            os = "macOS";
        } else if (userAgent.contains("Linux")) {
            os = "Linux";
        }
        return browser + " on " + os;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String sha256(String value) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
