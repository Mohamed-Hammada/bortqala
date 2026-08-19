package com.bemo.hr.product.pack;

import com.bemo.hr.shared.security.Roles;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/platform/industry-packs")
@RequiredArgsConstructor
public class IndustryPackController {
    private final IndustryPackService service;
    private final IndustryRuntimeProfileService runtimeProfileService;

    @GetMapping
    @PreAuthorize(Roles.ADMIN_ONLY)
    public List<IndustryPackApi.PackResponse> catalog() {
        return service.catalog();
    }

    @GetMapping("/runtime-profile")
    @PreAuthorize("isAuthenticated()")
    public IndustryRuntimeProfileService.EffectiveIndustryProfile runtimeProfile() {
        return runtimeProfileService.getEffectiveProfile();
    }

    @GetMapping("/{code}/kpis")
    @PreAuthorize(Roles.ADMIN_ONLY)
    public List<IndustryKpiProvider.KpiResult> kpis(@PathVariable String code) {
        return service.calculateKpis(code);
    }

    @PostMapping("/{code}/install")
    @PreAuthorize(Roles.SUPER_ADMIN_ONLY)
    public IndustryPackApi.PackResponse install(@PathVariable String code, @Valid @RequestBody IndustryPackApi.InstallRequest request, Authentication auth) {
        return service.install(code, request, auth.getName());
    }

    @PostMapping("/{code}/upgrade")
    @PreAuthorize(Roles.SUPER_ADMIN_ONLY)
    public IndustryPackApi.PackResponse upgrade(@PathVariable String code, @Valid @RequestBody IndustryPackApi.UpgradeRequest request, Authentication auth) {
        return service.upgrade(code, request, auth.getName());
    }

    @PostMapping("/{code}/reconcile")
    @PreAuthorize(Roles.SUPER_ADMIN_ONLY)
    public IndustryPackApi.PackResponse reconcile(@PathVariable String code, @RequestBody(required = false) IndustryPackApi.ReconcileRequest request, Authentication auth) {
        return service.reconcile(code, request, auth.getName());
    }

    @PutMapping("/{code}/settings")
    @PreAuthorize(Roles.ADMIN_ONLY)
    public IndustryPackApi.PackResponse settings(@PathVariable String code, @Valid @RequestBody IndustryPackApi.SettingsRequest request, Authentication auth) {
        return service.updateSettings(code, request, auth.getName());
    }

    @PostMapping("/{code}/steps/{stepKey}")
    @PreAuthorize(Roles.ADMIN_ONLY)
    public IndustryPackApi.PackResponse step(@PathVariable String code, @PathVariable String stepKey, @Valid @RequestBody IndustryPackApi.StepRequest request, Authentication auth) {
        return service.completeStep(code, stepKey, request, auth.getName());
    }
}
