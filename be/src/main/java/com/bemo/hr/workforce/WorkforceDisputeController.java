package com.bemo.hr.workforce;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workforce")
public class WorkforceDisputeController {

    private final WorkforceDisputeService workforceDisputeService;

    public WorkforceDisputeController(WorkforceDisputeService workforceDisputeService) {
        this.workforceDisputeService = workforceDisputeService;
    }

    public record CreateDisputePayload(String contractorId, BigDecimal disputedAmount, String reason) {}
    public record ResolveDisputePayload(String resolutionNotes) {}
    public record RejectDisputePayload(String reason) {}

    @PostMapping("/settlements/{periodId}/disputes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_FINANCE', 'FINANCE_MANAGER')")
    public WorkforceDispute openDispute(@PathVariable String periodId, @RequestBody CreateDisputePayload payload, Authentication authentication) {
        return workforceDisputeService.createDispute(periodId, payload.contractorId(), payload.disputedAmount(), payload.reason(), authentication.getName());
    }

    @PostMapping("/disputes/{id}/submit")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_FINANCE', 'FINANCE_MANAGER')")
    public WorkforceDispute submitDispute(@PathVariable String id, Authentication authentication) {
        return workforceDisputeService.submitForReview(id, authentication.getName());
    }

    @PostMapping("/disputes/{id}/resolve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public WorkforceDispute resolveDispute(@PathVariable String id, @RequestBody ResolveDisputePayload payload, Authentication authentication) {
        return workforceDisputeService.resolveDispute(id, payload.resolutionNotes(), authentication.getName());
    }

    @PostMapping("/disputes/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public WorkforceDispute rejectDispute(@PathVariable String id, @RequestBody RejectDisputePayload payload, Authentication authentication) {
        return workforceDisputeService.rejectDispute(id, payload.reason(), authentication.getName());
    }

    @GetMapping("/settlements/{periodId}/disputes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE', 'FINANCE_MANAGER', 'VIEWER')")
    public List<WorkforceDispute> getDisputesForPeriod(@PathVariable String periodId) {
        return workforceDisputeService.getDisputesByPeriod(periodId);
    }
}
