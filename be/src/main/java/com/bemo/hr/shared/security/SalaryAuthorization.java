package com.bemo.hr.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Reusable authorization component that enforces the per-user
 * {@code canViewSalary} permission on the backend. Frontend menu hiding is not a
 * security boundary; every endpoint exposing salary data must be guarded by
 * {@code @PreAuthorize("@salaryAuthorization.canView(authentication)")}.
 */
@Component("salaryAuthorization")
public class SalaryAuthorization {

    private final AppUserRepository appUserRepository;

    public SalaryAuthorization(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public boolean canView(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String appId = null;
        String username = authentication.getName();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            appId = jwtAuthenticationToken.getToken().getClaimAsString("appId");
        }
        if (appId == null || appId.isBlank()) {
            String current = TenantContext.currentOrSystem();
            appId = "SYSTEM".equals(current) ? null : current;
        }
        if (appId == null || username == null || username.isBlank()) {
            return false;
        }
        return appUserRepository.findByAppIdAndUsernameIgnoreCase(appId, username)
                .map(user -> user.isCanViewSalary()
                        || user.getRoles().stream().anyMatch(role -> role.getCode() == RoleCode.SUPER_ADMIN))
                .orElse(false);
    }
}
