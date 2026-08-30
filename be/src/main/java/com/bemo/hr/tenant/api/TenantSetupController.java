package com.bemo.hr.tenant.api;

import com.bemo.hr.tenant.api.TenantSetupApi.ConfigureVerticalRequest;
import com.bemo.hr.tenant.api.TenantSetupApi.TenantVerticalResponse;
import com.bemo.hr.tenant.application.TenantSetupService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/tenant/vertical-setup")
public class TenantSetupController {

    private final TenantSetupService tenantSetupService;

    public TenantSetupController(TenantSetupService tenantSetupService) {
        this.tenantSetupService = tenantSetupService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public TenantVerticalResponse getVerticalSetup() {
        return tenantSetupService.getVerticalSetup();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or @auth.hasPermission('access:setup:manage')")
    public TenantVerticalResponse configureVertical(@Valid @RequestBody ConfigureVerticalRequest request,
                                                    @AuthenticationPrincipal Jwt jwt) {
        String actor = jwt != null ? jwt.getSubject() : "system";
        return tenantSetupService.configureTenantVertical(request.vertical(), actor);
    }
}
