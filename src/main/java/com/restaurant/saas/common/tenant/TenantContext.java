package com.restaurant.saas.common.tenant;

import com.restaurant.saas.common.exception.ForbiddenException;

/**
 * Holds the tenant resolved from the authenticated principal for the current
 * request. Never trust a tenant id supplied by the client.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SUPER_ADMIN = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private TenantContext() {
    }

    public static void setTenant(Long tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static Long getTenantOrNull() {
        return CURRENT_TENANT.get();
    }

    public static Long requireTenant() {
        Long tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new ForbiddenException("No tenant context for the current request");
        }
        return tenantId;
    }

    public static void setSuperAdmin(boolean value) {
        SUPER_ADMIN.set(value);
    }

    public static boolean isSuperAdmin() {
        return Boolean.TRUE.equals(SUPER_ADMIN.get());
    }

    public static void clear() {
        CURRENT_TENANT.remove();
        SUPER_ADMIN.remove();
    }
}
