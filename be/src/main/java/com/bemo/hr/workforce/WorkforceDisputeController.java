package com.bemo.hr.workforce;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workforce")
public class WorkforceDisputeController {

    private final WorkforceDisputeService disputeService;

    public WorkforceDisputeController(WorkforceDisputeService disputeService) {
        this.disputeService = disputeService;
    }

    public record CreateDisputePayload(String contractorId, BigDecimal disputedAmount, String reason) {}
    public record ResolveDisputePayload(String resolutionNotes, String resolvedBy) {}
    public record RejectDisputePayload(String reason, String rejectedBy) {}

    @PostMapping("/settlements/{periodId}/disputes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'FINANCE_MANAGER')")
    public WorkforceDispute openDispute(@PathVariable String periodId, @RequestBody CreateDisputePayload payload) {
        return disputeService.createDispute(periodId, payload.contractorId(), payload.disputedAmount(), payload.reason());
    }

    @PostMapping("/disputes/{id}/resolve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public WorkforceDispute resolveDispute(@PathVariable String id, @RequestBody ResolveDisputePayload payload) {
        return disputeService.resolveDispute(id, payload.resolutionNotes(), payload.resolvedBy());
    }

    @PostMapping("/disputes/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public WorkforceDispute rejectDispute(@PathVariable String id, @RequestBody RejectDisputePayload payload) {
        return disputeService.rejectDispute(id, payload.reason(), payload.rejectedBy());
    }

    @GetMapping("/settlements/{periodId}/disputes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'FINANCE_MANAGER', 'VIEWER')")
    public List<WorkforceDispute> getDisputesForPeriod(@PathVariable String periodId) {
        return disputeService.getDisputesByPeriod(periodId);
    }
}
