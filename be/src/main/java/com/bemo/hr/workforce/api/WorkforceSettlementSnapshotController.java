package com.bemo.hr.workforce.api;

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

    public record FreezeSnapshotPayload(String contractorId, String periodId, BigDecimal totalHours, BigDecimal grossAmount, BigDecimal netAmount) {}

    @PostMapping("/freeze")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'FINANCE_MANAGER')")
    public WorkforceSettlementSnapshot createFrozenSnapshot(@RequestBody FreezeSnapshotPayload payload) {
        return snapshotService.createFrozenSnapshot(payload.contractorId(), payload.periodId(), payload.totalHours(), payload.grossAmount(), payload.netAmount());
    }

    @GetMapping("/{contractorId}/{periodId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'FINANCE_MANAGER', 'VIEWER')")
    public WorkforceSettlementSnapshot getSnapshot(@PathVariable String contractorId, @PathVariable String periodId) {
        return snapshotService.getSnapshot(contractorId, periodId);
    }
}
