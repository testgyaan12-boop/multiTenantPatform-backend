package com.restaurant.saas.authorization.security;

import com.restaurant.saas.common.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("tenantGuard")
public class TenantGuard {

    public boolean canAccessTenant(Long tenantId) {
        if (TenantContext.isSuperAdmin()) {
            return true;
        }
        Long current = TenantContext.getTenantOrNull();
        return current != null && current.equals(tenantId);
    }

    public boolean isSuperAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedPrincipal p)) {
            return false;
        }
        return p.isSuperAdmin();
    }
}
