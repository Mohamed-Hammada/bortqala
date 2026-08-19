package com.bemo.hr.trade.procurement.api;

import com.bemo.hr.shared.security.Roles;
import com.bemo.hr.trade.procurement.application.ProcurementMatchOverrideService;
import com.bemo.hr.trade.procurement.domain.ProcurementMatchOverride;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/procurement/3way-match/overrides")
public class ProcurementMatchOverrideController {

    private final ProcurementMatchOverrideService overrideService;

    public ProcurementMatchOverrideController(ProcurementMatchOverrideService overrideService) {
        this.overrideService = overrideService;
    }

    @PostMapping
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.PROCUREMENT_MANAGER)
    public ProcurementMatchOverride approveOverride(@RequestBody ApproveOverridePayload payload) {
        return overrideService.approveOverride(payload.matchId(), payload.overrideReason(), payload.approvedBy());
    }

    @GetMapping("/{matchId}")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.PROCUREMENT_MANAGER + " or " + Roles.VIEWER)
    public ProcurementMatchOverride getOverride(@PathVariable String matchId) {
        return overrideService.getOverride(matchId);
    }

    public record ApproveOverridePayload(String matchId, String overrideReason, String approvedBy) {
    }
}
