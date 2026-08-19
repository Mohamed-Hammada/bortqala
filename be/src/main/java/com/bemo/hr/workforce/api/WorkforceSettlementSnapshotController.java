package com.bemo.hr.workforce.api;

import com.bemo.hr.shared.security.Roles;
import com.bemo.hr.workforce.application.WorkforceSettlementSnapshotService;
import com.bemo.hr.workforce.domain.WorkforceSettlementSnapshot;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/workforce/settlements/snapshots")
public class WorkforceSettlementSnapshotController {

    private final WorkforceSettlementSnapshotService snapshotService;

    public WorkforceSettlementSnapshotController(WorkforceSettlementSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @PostMapping("/freeze")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.WORKFORCE_MANAGER)
    public WorkforceSettlementSnapshot createFrozenSnapshot(@RequestBody FreezeSnapshotPayload payload) {
        return snapshotService.createFrozenSnapshot(payload.contractorId(), payload.periodId(), payload.totalHours(), payload.grossAmount(), payload.netAmount());
    }

    @GetMapping("/{contractorId}/{periodId}")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.VIEWER + " or " + Roles.WORKFORCE_MANAGER)
    public WorkforceSettlementSnapshot getSnapshot(@PathVariable String contractorId, @PathVariable String periodId) {
        return snapshotService.getSnapshot(contractorId, periodId);
    }

    public record FreezeSnapshotPayload(String contractorId, String periodId, BigDecimal totalHours,
                                        BigDecimal grossAmount, BigDecimal netAmount) {
    }
}
