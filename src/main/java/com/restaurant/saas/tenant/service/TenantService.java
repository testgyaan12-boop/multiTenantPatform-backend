package com.restaurant.saas.tenant.service;

import com.restaurant.saas.common.exception.ConflictException;
import com.restaurant.saas.common.exception.NotFoundException;
import com.restaurant.saas.common.web.PageResponse;
import com.restaurant.saas.branch.entity.Branch;
import com.restaurant.saas.branch.repository.BranchRepository;
import com.restaurant.saas.identity.entity.Role;
import com.restaurant.saas.identity.entity.User;
import com.restaurant.saas.identity.repository.RoleRepository;
import com.restaurant.saas.identity.repository.UserRepository;
import com.restaurant.saas.restaurant.entity.Restaurant;
import com.restaurant.saas.restaurant.repository.RestaurantRepository;
import com.restaurant.saas.subscription.entity.Subscription;
import com.restaurant.saas.subscription.entity.SubscriptionPlan;
import com.restaurant.saas.subscription.repository.SubscriptionPlanRepository;
import com.restaurant.saas.subscription.repository.SubscriptionRepository;
import com.restaurant.saas.tenant.dto.TenantCreateRequest;
import com.restaurant.saas.tenant.dto.TenantResponse;
import com.restaurant.saas.tenant.entity.Tenant;
import com.restaurant.saas.tenant.repository.TenantRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final RestaurantRepository restaurantRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantService(
        TenantRepository tenantRepository,
        UserRepository userRepository,
        RoleRepository roleRepository,
        SubscriptionPlanRepository planRepository,
        SubscriptionRepository subscriptionRepository,
        RestaurantRepository restaurantRepository,
        BranchRepository branchRepository,
        PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.restaurantRepository = restaurantRepository;
        this.branchRepository = branchRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<TenantResponse> list(String search, Pageable pageable) {
        String normalized = (search == null || search.isBlank()) ? null : search.trim();
        return PageResponse.of(tenantRepository.search(normalized, pageable).map(TenantResponse::from));
    }

    @Transactional(readOnly = true)
    public TenantResponse get(Long id) {
        return TenantResponse.from(require(id));
    }

    @Transactional
    public TenantResponse create(TenantCreateRequest request) {
        String slug = request.slug().trim().toLowerCase();
        if (tenantRepository.existsBySlug(slug)) {
            throw new ConflictException("tenant_slug_taken", "Tenant slug is already in use");
        }
        String email = request.ownerEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("email_taken", "An account with this email already exists");
        }

        Tenant tenant = new Tenant();
        tenant.setName(request.name().trim());
        tenant.setSlug(slug);
        tenant.setStatus("TRIAL");
        tenant.setContactEmail(email);
        tenant.setTimezone(request.timezone() == null ? "UTC" : request.timezone());
        tenant.setCurrency(request.currency() == null ? "USD" : request.currency().toUpperCase());
        tenant.setBusinessType(
            request.businessType() == null || request.businessType().isBlank()
                ? "RESTAURANT"
                : request.businessType());
        tenant.setTrialEndsAt(Instant.now().plus(14, ChronoUnit.DAYS));
        tenant = tenantRepository.save(tenant);

        Restaurant restaurant = new Restaurant();
        restaurant.setTenantId(tenant.getId());
        restaurant.setCode(slug.toUpperCase().replace('-', '_'));
        restaurant.setName(tenant.getName());
        restaurant.setTimezone(tenant.getTimezone());
        restaurant.setCurrency(tenant.getCurrency());
        restaurant = restaurantRepository.save(restaurant);

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
        owner.setPasswordHash(passwordEncoder.encode(generateTemporaryPassword()));
        owner.setStatus("ACTIVE");
        owner.getRoles().add(ownerRole);
        userRepository.save(owner);

        SubscriptionPlan plan = planRepository.findByCode(request.planCode())
            .orElseThrow(() -> new NotFoundException(
                "Subscription plan " + request.planCode() + " is not configured"));

        Subscription subscription = new Subscription();
        subscription.setTenantId(tenant.getId());
        subscription.setPlanId(plan.getId());
        subscription.setStatus("TRIALING");
        subscription.setTrialEndsAt(tenant.getTrialEndsAt());
        subscription.setCurrentPeriodStart(Instant.now());
        subscription.setCurrentPeriodEnd(tenant.getTrialEndsAt());
        subscriptionRepository.save(subscription);

        return TenantResponse.from(tenant);
    }

    @Transactional
    public TenantResponse suspend(Long id) {
        Tenant tenant = require(id);
        if ("CANCELLED".equals(tenant.getStatus())) {
            throw new ConflictException("tenant_cancelled", "A cancelled tenant cannot be suspended");
        }
        tenant.setStatus("SUSPENDED");
        return TenantResponse.from(tenantRepository.save(tenant));
    }

    @Transactional
    public TenantResponse activate(Long id) {
        Tenant tenant = require(id);
        if ("CANCELLED".equals(tenant.getStatus())) {
            throw new ConflictException("tenant_cancelled", "A cancelled tenant cannot be activated");
        }
        tenant.setStatus("ACTIVE");
        return TenantResponse.from(tenantRepository.save(tenant));
    }

    @Transactional
    public TenantResponse verify(Long id) {
        Tenant tenant = require(id);
        if (!"PENDING_VERIFICATION".equals(tenant.getStatus())) {
            throw new ConflictException("tenant_not_pending", "Only pending restaurants can be verified");
        }

        Instant trialEndsAt = Instant.now().plus(14, ChronoUnit.DAYS);
        tenant.setStatus("TRIAL");
        tenant.setTrialEndsAt(trialEndsAt);
        tenant = tenantRepository.save(tenant);

        List<User> tenantUsers = userRepository.findByTenantId(id);
        for (User user : tenantUsers) {
            if ("PENDING".equals(user.getStatus())) {
                user.setStatus("ACTIVE");
            }
        }
        userRepository.saveAll(tenantUsers);

        if (subscriptionRepository.findByTenantId(id).isEmpty()) {
            SubscriptionPlan plan = planRepository.findByCode("STARTER")
                .orElseThrow(() -> new NotFoundException("STARTER subscription plan is not configured"));
            Subscription subscription = new Subscription();
            subscription.setTenantId(id);
            subscription.setPlanId(plan.getId());
            subscription.setStatus("TRIALING");
            subscription.setTrialEndsAt(trialEndsAt);
            subscription.setCurrentPeriodStart(Instant.now());
            subscription.setCurrentPeriodEnd(trialEndsAt);
            subscriptionRepository.save(subscription);
        }

        return TenantResponse.from(tenant);
    }

    @Transactional
    public TenantResponse reject(Long id) {
        Tenant tenant = require(id);
        if ("CANCELLED".equals(tenant.getStatus())) {
            throw new ConflictException("tenant_cancelled", "A cancelled tenant cannot be rejected");
        }
        tenant.setStatus("SUSPENDED");
        return TenantResponse.from(tenantRepository.save(tenant));
    }

    private Tenant require(Long id) {
        return tenantRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Tenant", id));
    }

    private static String generateTemporaryPassword() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
