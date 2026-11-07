package com.restaurant.saas.identity.service;

import com.restaurant.saas.authorization.security.AuthenticatedPrincipal;
import com.restaurant.saas.authorization.security.JwtTokenService;
import com.restaurant.saas.branch.entity.Branch;
import com.restaurant.saas.branch.repository.BranchRepository;
import com.restaurant.saas.common.exception.ApiException;
import com.restaurant.saas.common.exception.ConflictException;
import com.restaurant.saas.common.exception.NotFoundException;
import com.restaurant.saas.common.exception.UnauthorizedException;
import com.restaurant.saas.common.tenant.TenantContext;
import com.restaurant.saas.identity.dto.AuthResponse;
import com.restaurant.saas.identity.dto.AuthUserResponse;
import com.restaurant.saas.identity.dto.RegistrationResponse;
import com.restaurant.saas.identity.dto.RestaurantRegistrationRequest;
import com.restaurant.saas.identity.entity.Permission;
import com.restaurant.saas.identity.entity.RefreshToken;
import com.restaurant.saas.identity.entity.Role;
import com.restaurant.saas.identity.entity.User;
import com.restaurant.saas.identity.repository.RefreshTokenRepository;
import com.restaurant.saas.identity.repository.RoleRepository;
import com.restaurant.saas.identity.repository.UserRepository;
import com.restaurant.saas.restaurant.entity.Restaurant;
import com.restaurant.saas.restaurant.repository.RestaurantRepository;
import com.restaurant.saas.tenant.entity.Tenant;
import com.restaurant.saas.tenant.repository.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final long refreshTtlDays;
    private final TenantRepository tenantRepository;
    private final RestaurantRepository restaurantRepository;
    private final BranchRepository branchRepository;
    private final RoleRepository roleRepository;
    private final com.restaurant.saas.audit.service.AuditService auditService;

    public AuthService(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenService jwtTokenService,
        com.restaurant.saas.authorization.security.SecurityProperties properties,
        TenantRepository tenantRepository,
        RestaurantRepository restaurantRepository,
        BranchRepository branchRepository,
        RoleRepository roleRepository,
        com.restaurant.saas.audit.service.AuditService auditService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTtlDays = properties.jwt().refreshTtlDays();
        this.tenantRepository = tenantRepository;
        this.restaurantRepository = restaurantRepository;
        this.branchRepository = branchRepository;
        this.roleRepository = roleRepository;
        this.auditService = auditService;
    }

    public record LoginResult(AuthResponse response, String refreshToken) {
    }

    @Transactional
    public LoginResult login(String email, String rawPassword, String userAgent, String ipAddress) {
        User user = userRepository.findActiveByEmailWithAuthorities(email.trim().toLowerCase())
            .orElseThrow(() -> new UnauthorizedException("invalid_credentials", "Invalid email or password"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new UnauthorizedException("invalid_credentials", "Invalid email or password");
        }

        if ("PENDING".equals(user.getStatus())) {
            throw new ApiException(
                HttpStatus.FORBIDDEN,
                "account_pending_verification",
                "Your restaurant account is awaiting verification by our team. Please try again once it is approved.");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new UnauthorizedException("invalid_credentials", "Invalid email or password");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        AuthenticatedPrincipal principal = toPrincipal(user);
        String accessToken = jwtTokenService.createAccessToken(principal);
        String refreshToken = issueRefreshToken(user.getId(), userAgent, ipAddress);

        auditService.record(user.getTenantId(), user.getId(), user.getEmail(), "LOGIN", "User",
            String.valueOf(user.getId()), null, null);

        return new LoginResult(
            new AuthResponse(accessToken, jwtTokenService.accessTtlSeconds(), toResponse(user, principal)),
            refreshToken
        );
    }

    @Transactional
    public RegistrationResponse registerRestaurant(RestaurantRegistrationRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("email_taken", "An account with this email already exists");
        }

        String slug = uniqueSlug(request.name().trim());

        Tenant tenant = new Tenant();
        tenant.setName(request.name().trim());
        tenant.setSlug(slug);
        tenant.setStatus("PENDING_VERIFICATION");
        tenant.setContactEmail(email);
        tenant.setTimezone(request.timezone() == null || request.timezone().isBlank() ? "UTC" : request.timezone());
        tenant.setCurrency(request.currency() == null || request.currency().isBlank()
            ? "USD" : request.currency().toUpperCase());
        tenant = tenantRepository.save(tenant);

        Restaurant restaurant = new Restaurant();
        restaurant.setTenantId(tenant.getId());
        restaurant.setCode(slug.toUpperCase().replace('-', '_'));
        restaurant.setName(tenant.getName());
        restaurant.setTimezone(tenant.getTimezone());
        restaurant.setCurrency(tenant.getCurrency());
        restaurantRepository.save(restaurant);

        Branch branch = new Branch();
        branch.setTenantId(tenant.getId());
        branch.setRestaurantId(restaurant.getId());
        branch.setCode("MAIN");
        branch.setName("Main Location");
        branch.setAddressLine1("Update your address");
        branch.setCity("Update your city");
        branch.setCountry("US");
        branch.setIsDefault(Boolean.TRUE);
        branchRepository.save(branch);

        Role ownerRole = roleRepository.findSystemRoleWithPermissions("TENANT_OWNER")
            .orElseThrow(() -> new NotFoundException("TENANT_OWNER role is not configured"));

        User owner = new User();
        owner.setTenantId(tenant.getId());
        owner.setEmail(email);
        owner.setFirstName(request.ownerFirstName().trim());
        owner.setLastName(request.ownerLastName().trim());
        owner.setPasswordHash(passwordEncoder.encode(request.password()));
        owner.setStatus("PENDING");
        owner.getRoles().add(ownerRole);
        userRepository.save(owner);

        return new RegistrationResponse(
            "PENDING_VERIFICATION",
            "Your restaurant has been submitted for verification. Once our team approves it, you can sign in.");
    }

    private String uniqueSlug(String name) {
        String base = name.toLowerCase()
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-+|-+$)", "");
        if (base.isBlank()) {
            base = "restaurant";
        }
        String candidate = base;
        int suffix = 2;
        while (tenantRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix;
            suffix += 1;
        }
        return candidate;
    }

    @Transactional
    public LoginResult refresh(String presentedToken, String userAgent, String ipAddress) {
        if (presentedToken == null || presentedToken.isBlank()) {
            throw new UnauthorizedException("missing_refresh_token", "Refresh token is required");
        }
        String hash = sha256(presentedToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
            .orElseThrow(() -> new UnauthorizedException("invalid_refresh_token", "Refresh token is invalid"));

        if (!stored.isUsable()) {
            refreshTokenRepository.revokeAllForUser(stored.getUserId(), Instant.now());
            throw new UnauthorizedException("refresh_token_reused", "Refresh token is no longer valid");
        }

        User user = userRepository.findActiveByIdWithAuthorities(stored.getUserId())
            .orElseThrow(() -> new UnauthorizedException("invalid_refresh_token", "User is no longer active"));

        stored.setRevokedAt(Instant.now());
        String newRawToken = generateRawToken();
        RefreshToken replacement = createRefreshToken(user.getId(), newRawToken, userAgent, ipAddress);
        stored.setReplacedById(replacement.getId());
        refreshTokenRepository.save(stored);
        refreshTokenRepository.save(replacement);

        AuthenticatedPrincipal principal = toPrincipal(user);
        String accessToken = jwtTokenService.createAccessToken(principal);
        return new LoginResult(
            new AuthResponse(accessToken, jwtTokenService.accessTtlSeconds(), toResponse(user, principal)),
            newRawToken
        );
    }

    @Transactional
    public void logout(String presentedToken) {
        if (presentedToken == null || presentedToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(sha256(presentedToken)).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
            userRepository.findById(token.getUserId()).ifPresent(user ->
                auditService.record(user.getTenantId(), user.getId(), user.getEmail(), "LOGOUT", "User",
                    String.valueOf(user.getId()), null, null));
        });
    }

    @Transactional(readOnly = true)
    public AuthUserResponse currentUser() {
        AuthenticatedPrincipal principal = currentPrincipal();
        User user = userRepository.findActiveByIdWithAuthorities(principal.userId())
            .orElseThrow(() -> new UnauthorizedException("invalid_token", "User is no longer active"));
        return toResponse(user, principal);
    }

    @Transactional
    public void acceptInvite(String token, String rawPassword) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("invalid_invite", "This invitation link is not valid.");
        }
        User user = userRepository.findByInviteToken(token.trim())
            .orElseThrow(() -> new UnauthorizedException("invalid_invite", "This invitation link is not valid."));
        if (user.getInviteExpiresAt() == null || user.getInviteExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(
                HttpStatus.GONE,
                "invite_expired",
                "This invitation has expired. Ask your manager to send you a new invitation.");
        }
        if (!"INVITED".equals(user.getStatus())) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                "invite_used",
                "This invitation has already been accepted. Please sign in instead.");
        }
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setStatus("ACTIVE");
        user.setEmailVerified(Boolean.TRUE);
        user.setInviteToken(null);
        user.setInviteExpiresAt(null);
        userRepository.save(user);
    }

    private AuthenticatedPrincipal currentPrincipal() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedPrincipal p)) {
            throw new UnauthorizedException("unauthorized", "Authentication is required");
        }
        return p;
    }

    private String issueRefreshToken(Long userId, String userAgent, String ipAddress) {
        String rawToken = generateRawToken();
        createRefreshToken(userId, rawToken, userAgent, ipAddress);
        return rawToken;
    }

    private RefreshToken createRefreshToken(Long userId, String rawToken, String userAgent, String ipAddress) {
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(sha256(rawToken));
        token.setExpiresAt(Instant.now().plus(refreshTtlDays, ChronoUnit.DAYS));
        token.setUserAgent(trim(userAgent, 255));
        token.setIpAddress(trim(ipAddress, 45));
        return refreshTokenRepository.save(token);
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static AuthenticatedPrincipal toPrincipal(User user) {
        Set<String> roles = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        Set<String> permissions = user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .map(Permission::getCode)
            .collect(Collectors.toSet());
        return new AuthenticatedPrincipal(
            user.getId(), user.getTenantId(), user.getEmail(), roles, permissions);
    }

    private AuthUserResponse toResponse(User user, AuthenticatedPrincipal principal) {
        String tenantName = principal.tenantId() == null
            ? null
            : tenantRepository.findById(principal.tenantId()).map(Tenant::getName).orElse(null);
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
}
