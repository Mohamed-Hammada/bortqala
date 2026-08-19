package com.bemo.hr.product.subscription;

import com.bemo.hr.shared.security.Roles;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/platform/subscription")
@RequiredArgsConstructor
@PreAuthorize(Roles.SUPER_ADMIN_ONLY)
public class SubscriptionController {
    private final SubscriptionService service;

    @GetMapping("/plans")
    List<SubscriptionApi.PlanResponse> plans() {
        return service.plans();
    }

    @PutMapping("/plans/{code}")
    SubscriptionApi.PlanResponse savePlan(@PathVariable String code, @Valid @RequestBody SubscriptionApi.PlanUpsertRequest request, Authentication auth) {
        return service.savePlan(code, request, auth.getName());
    }

    @GetMapping
    SubscriptionApi.SubscriptionResponse current() {
        return service.current();
    }

    @PostMapping("/change")
    SubscriptionApi.ChangeResponse change(@Valid @RequestBody SubscriptionApi.ChangeRequest request, Authentication auth) {
        return service.change(request, auth.getName());
    }

    @GetMapping("/history")
    List<SubscriptionApi.HistoryResponse> history() {
        return service.history();
    }

    @GetMapping("/usage")
    SubscriptionApi.UsageResponse usage() {
        return service.usage();
    }
}
