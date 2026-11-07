package com.restaurant.saas.customer.service;

import com.restaurant.saas.branch.entity.Branch;
import com.restaurant.saas.branch.repository.BranchRepository;
import com.restaurant.saas.common.exception.ApiException;
import com.restaurant.saas.common.exception.NotFoundException;
import com.restaurant.saas.customer.dto.GuestSessionResponse;
import com.restaurant.saas.customer.entity.GuestSession;
import com.restaurant.saas.customer.repository.GuestSessionRepository;
import com.restaurant.saas.diningtable.entity.DiningTable;
import com.restaurant.saas.diningtable.repository.DiningTableRepository;
import com.restaurant.saas.restaurant.entity.Restaurant;
import com.restaurant.saas.restaurant.repository.RestaurantRepository;
import com.restaurant.saas.tenant.entity.Tenant;
import com.restaurant.saas.tenant.repository.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class GuestSessionService {

    private static final int SESSION_HOURS = 8;

    private final DiningTableRepository tableRepository;
    private final BranchRepository branchRepository;
    private final GuestSessionRepository guestSessionRepository;
    private final TenantRepository tenantRepository;
    private final RestaurantRepository restaurantRepository;

    public GuestSessionService(
        DiningTableRepository tableRepository,
        BranchRepository branchRepository,
        GuestSessionRepository guestSessionRepository,
        TenantRepository tenantRepository,
        RestaurantRepository restaurantRepository) {
        this.tableRepository = tableRepository;
        this.branchRepository = branchRepository;
        this.guestSessionRepository = guestSessionRepository;
        this.tenantRepository = tenantRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @Transactional
    public GuestSessionResponse start(String tableCode, String restaurantSlug) {
        boolean hasTable = tableCode != null && !tableCode.isBlank();
        boolean hasSlug = restaurantSlug != null && !restaurantSlug.isBlank();
        if (!hasTable && !hasSlug) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_session_request",
                "Provide a tableCode or a restaurantSlug");
        }
        if (hasTable) {
            return startForTable(tableCode.trim());
        }
        return startForRestaurant(restaurantSlug.trim());
    }

    @Transactional
    public GuestSessionResponse startForTable(String tableCode) {
        DiningTable table = tableRepository.findByQrToken(tableCode)
            .orElseThrow(() -> new NotFoundException("Table QR code is not valid"));
        Branch branch = branchRepository.findById(table.getBranchId())
            .orElseThrow(() -> new NotFoundException("Branch", table.getBranchId()));
        Tenant tenant = tenantRepository.findById(table.getTenantId())
            .orElseThrow(() -> new NotFoundException("Tenant", table.getTenantId()));
        Restaurant restaurant = restaurantRepository.findByTenantId(tenant.getId()).orElse(null);

        GuestSession session = new GuestSession();
        session.setTenantId(table.getTenantId());
        session.setBranchId(table.getBranchId());
        session.setTableId(table.getId());
        session.setSessionToken(UUID.randomUUID().toString());
        session.setExpiresAt(Instant.now().plus(SESSION_HOURS, ChronoUnit.HOURS));
        session.setLastSeenAt(Instant.now());
        session = guestSessionRepository.save(session);

        return GuestSessionResponse.of(
            session.getSessionToken(), session.getExpiresAt(), table, branch, tenant, restaurant);
    }

    @Transactional
    public GuestSessionResponse startForRestaurant(String restaurantSlug) {
        Tenant tenant = tenantRepository.findBySlug(restaurantSlug)
            .orElseThrow(() -> new NotFoundException("Restaurant not found"));
        Restaurant restaurant = restaurantRepository.findByTenantId(tenant.getId())
            .orElseThrow(() -> new NotFoundException("Restaurant profile is not configured"));
        Branch branch = branchRepository.findByTenantIdOrderByNameAsc(tenant.getId()).stream()
            .filter(Branch::getIsDefault)
            .findFirst()
            .or(() -> branchRepository.findByTenantIdOrderByNameAsc(tenant.getId()).stream().findFirst())
            .orElseThrow(() -> new NotFoundException("No branch is configured for this restaurant"));

        GuestSession session = new GuestSession();
        session.setTenantId(tenant.getId());
        session.setBranchId(branch.getId());
        session.setTableId(null);
        session.setSessionToken(UUID.randomUUID().toString());
        session.setExpiresAt(Instant.now().plus(SESSION_HOURS, ChronoUnit.HOURS));
        session.setLastSeenAt(Instant.now());
        session = guestSessionRepository.save(session);

        return GuestSessionResponse.pickup(
            session.getSessionToken(), session.getExpiresAt(), branch, tenant, restaurant);
    }

    @Transactional(readOnly = true)
    public GuestSession requireActive(String token) {
        GuestSession session = guestSessionRepository.findBySessionToken(token)
            .orElseThrow(() -> new NotFoundException("Guest session not found"));
        if (session.isExpired()) {
            throw new NotFoundException("Guest session has expired");
        }
        return session;
    }
}
