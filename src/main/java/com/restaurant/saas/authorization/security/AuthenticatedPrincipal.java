package com.restaurant.saas.authorization.security;

import java.util.Set;

public record AuthenticatedPrincipal(
    Long userId,
    Long tenantId,
    String email,
    Set<String> roles,
    Set<String> permissions
) {

    public boolean isSuperAdmin() {
        return roles.contains("SUPER_ADMIN");
    }
}
