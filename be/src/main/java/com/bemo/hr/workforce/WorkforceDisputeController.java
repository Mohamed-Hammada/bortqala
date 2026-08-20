package com.bemo.hr.workforce;

import com.bemo.hr.shared.security.Roles;
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

    @PostMapping("/settlements/{periodId}/disputes")
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER_WORKFORCE_FINANCE_WORKFORCE_MANAGER)
    public WorkforceDispute openDispute(@PathVariable String periodId, @RequestBody CreateDisputePayload payload, Authentication authentication) {
        return workforceDisputeService.createDispute(periodId, payload.contractorId(), payload.disputedAmount(), payload.reason(), authentication.getName());
    }

    @PostMapping("/disputes/{id}/submit")
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER_WORKFORCE_FINANCE_WORKFORCE_MANAGER)
    public WorkforceDispute submitDispute(@PathVariable String id, Authentication authentication) {
        return workforceDisputeService.submitForReview(id, authentication.getName());
    }

    @PostMapping("/disputes/{id}/resolve")
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER)
    public WorkforceDispute resolveDispute(@PathVariable String id, @RequestBody ResolveDisputePayload payload, Authentication authentication) {
        return workforceDisputeService.resolveDispute(id, payload.resolutionNotes(), authentication.getName());
    }

    @PostMapping("/disputes/{id}/reject")
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER)
    public WorkforceDispute rejectDispute(@PathVariable String id, @RequestBody RejectDisputePayload payload, Authentication authentication) {
        return workforceDisputeService.rejectDispute(id, payload.reason(), authentication.getName());
    }

    @GetMapping("/settlements/{periodId}/disputes")
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER_VIEWER_WORKFORCE_FINANCE_WORKFORCE_MANAGER_WORKFORCE_REVIEWER)
    public List<WorkforceDispute> getDisputesForPeriod(@PathVariable String periodId) {
        return workforceDisputeService.getDisputesByPeriod(periodId);
    }

    public record CreateDisputePayload(String contractorId, BigDecimal disputedAmount, String reason) {
    }

    public record ResolveDisputePayload(String resolutionNotes) {
    }

    public record RejectDisputePayload(String reason) {
    }
}
