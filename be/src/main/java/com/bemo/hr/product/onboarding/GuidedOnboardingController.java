package com.bemo.hr.product.onboarding;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/platform/onboarding")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
public class GuidedOnboardingController {
    private final GuidedOnboardingService service;

    @GetMapping("/{code}")
    GuidedOnboardingApi.OverviewResponse overview(@PathVariable String code) {
        return service.overview(code);
    }

    @PostMapping("/{code}/assess")
    GuidedOnboardingApi.OverviewResponse assess(@PathVariable String code, @Valid @RequestBody GuidedOnboardingApi.AssessRequest request, Authentication auth) {
        return service.assess(code, request, auth.getName());
    }
}
