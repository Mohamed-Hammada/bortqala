package com.bemo.hr.product.analytics;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/product-analytics")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ProductAnalyticsController {
    private final ProductAnalyticsService service;

    @PostMapping("/events")
    ProductAnalyticsApi.EventResponse event(@Valid @RequestBody ProductAnalyticsApi.EventRequest request, Authentication auth) {
        return service.record(request, auth.getName());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    ProductAnalyticsApi.TenantSummary summary() {
        return service.summary();
    }

    @PostMapping("/retention")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    ProductAnalyticsApi.RetentionResponse retain(@Valid @RequestBody ProductAnalyticsApi.RetentionRequest request, Authentication auth) {
        return service.retain(request, auth.getName());
    }

    @GetMapping("/platform")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    List<ProductAnalyticsApi.PlatformTenantSummary> platform() {
        return service.platform();
    }
}
