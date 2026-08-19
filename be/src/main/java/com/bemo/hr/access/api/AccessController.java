package com.bemo.hr.access.api;

import com.bemo.hr.access.application.AccessCatalogService;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.AppUser;
import com.bemo.hr.shared.security.AppUserRepository;
import com.bemo.hr.shared.security.Role;
import com.bemo.hr.shared.security.Roles;
import com.bemo.hr.shared.security.TenantContext;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Exposes the role-to-page access catalog, the effective-access preview and the
 * authoritative assignment validation used by the Add/Edit User screens.
 */
@RestController
@RequestMapping("/api/v1")
public class AccessController {

    private final AccessCatalogService accessCatalogService;
    private final AppUserRepository appUserRepository;

    public AccessController(AccessCatalogService accessCatalogService, AppUserRepository appUserRepository) {
        this.accessCatalogService = accessCatalogService;
        this.appUserRepository = appUserRepository;
    }

    /**
     * Metadata only: any authenticated user may read the catalog.
     */
    @GetMapping("/access/catalog")
    @PreAuthorize("isAuthenticated()")
    AccessApi.AccessCatalogResponse catalog() {
        return accessCatalogService.catalog();
    }

    /**
     * Fast local preview for authenticated users; never the final decision.
     */
    @PostMapping("/access/preview")
    @PreAuthorize("isAuthenticated()")
    AccessApi.AccessPreviewResponse preview(@Valid @RequestBody AccessApi.AccessPreviewRequest request) {
        return accessCatalogService.preview(request.roleCodes(), request.menuCodes());
    }

    /**
     * Authoritative validation before a user assignment is saved.
     */
    @PostMapping("/users/access/validate")
    @PreAuthorize(Roles.ADMIN_ONLY)
    AccessApi.AccessValidateResponse validate(@Valid @RequestBody AccessApi.AccessValidateRequest request,
                                              @AuthenticationPrincipal Jwt jwt) {
        return accessCatalogService.validateAssignment(
                rolesOf(jwt), jwt.getClaimAsString("userId"), request.roleCodes(),
                request.menuCodes(), request.targetUserId(),
                currentRolesOf(request.targetUserId()), request.reason());
    }

    private Set<String> rolesOf(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null) {
            return Set.of();
        }
        return roles.stream().collect(Collectors.toUnmodifiableSet());
    }

    private Set<String> currentRolesOf(String targetUserId) {
        if (targetUserId == null || targetUserId.isBlank()) {
            return null;
        }
        AppUser target = appUserRepository.findByAppIdAndId(TenantContext.require(), targetUserId)
                .orElseThrow(() -> new NotFoundException("User not found.", "AUTH_USER_NOT_FOUND"));
        return target.getRoles().stream().map(Role::getCode).map(Enum::name)
                .collect(Collectors.toUnmodifiableSet());
    }
}
