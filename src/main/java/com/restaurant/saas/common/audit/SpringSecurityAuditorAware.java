package com.restaurant.saas.common.audit;

import com.restaurant.saas.authorization.security.AuthenticatedPrincipal;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorAware")
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    private static final String SYSTEM = "system";

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of(SYSTEM);
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedPrincipal authenticated) {
            return Optional.of(authenticated.userId().toString());
        }
        return Optional.of(SYSTEM);
    }
}
