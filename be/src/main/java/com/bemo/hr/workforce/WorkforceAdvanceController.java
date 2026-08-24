package com.bemo.hr.workforce;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workforce/advances")
@RequiredArgsConstructor
public class WorkforceAdvanceController {
    private final WorkforceAdvanceService advanceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE')")
    public List<WorkforceApi.AdvanceResponse> list() {
        return advanceService.list();
    }

    @GetMapping("/policies")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE')")
    public List<WorkforceApi.AdvancePolicyResponse> policies() {
        return advanceService.listPolicies();
    }

    @GetMapping("/policies/effective")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE')")
    public WorkforceApi.AdvancePolicyResponse effectivePolicy(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String recipientType,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String workerId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String employeeId,
            @org.springframework.web.bind.annotation.RequestParam String date) {
        return advanceService.effectivePolicy(recipientType, workerId, employeeId, date);
    }

    @GetMapping("/resolved-policy")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE')")
    public WorkforceApi.ResolvedDeductionPolicyResponse resolvedPolicy(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String employeeId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String categoryId) {
        return advanceService.resolveDeductionPolicy(employeeId, categoryId, java.time.LocalDate.now());
    }

    @PostMapping("/apply-deduction")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_FINANCE')")
    public WorkforceApi.ManualDeductionResult applyManualDeduction(@Valid @RequestBody WorkforceApi.ManualDeductionRequest request, Authentication auth) {
        return advanceService.applyManualDeduction(request, auth != null ? auth.getName() : "system");
    }

    @PutMapping("/policies")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER')")
    public WorkforceApi.AdvancePolicyResponse savePolicy(@Valid @RequestBody WorkforceApi.AdvancePolicyRequest request, Authentication auth) {
        return advanceService.savePolicy(request, auth != null ? auth.getName() : "system");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkforceApi.AdvanceResponse create(@Valid @RequestBody WorkforceApi.AdvanceCreateRequest request, Authentication auth) {
        String username = auth != null ? auth.getName() : "system";
        return advanceService.create(request, username);
    }

    @PostMapping("/{id}/pause")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER')")
    public WorkforceApi.AdvanceResponse pause(@org.springframework.web.bind.annotation.PathVariable String id, Authentication auth) {
        String username = auth != null ? auth.getName() : "system";
        return advanceService.pause(id, username);
    }

    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER')")
    public WorkforceApi.AdvanceResponse resume(@org.springframework.web.bind.annotation.PathVariable String id, Authentication auth) {
        String username = auth != null ? auth.getName() : "system";
        return advanceService.resume(id, username);
    }

    @PostMapping("/{id}/repay")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_FINANCE')")
    public WorkforceApi.AdvanceResponse repay(@org.springframework.web.bind.annotation.PathVariable String id, @Valid @RequestBody WorkforceApi.AdvanceRepayRequest request, Authentication auth) {
        String username = auth != null ? auth.getName() : "system";
        return advanceService.repay(id, request, username);
    }
}
