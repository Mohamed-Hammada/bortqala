package com.bemo.hr.party.api;

import com.bemo.hr.party.api.PartyFinancialPositionApi.*;
import com.bemo.hr.party.application.PartyFinancialPositionService;
import com.bemo.hr.shared.security.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/parties")
@RequiredArgsConstructor
@PreAuthorize(Roles.ADMIN_ACCOUNTANT_FINANCE_MANAGER_PROCUREMENT_MANAGER_SALES_MANAGER)
public class PartyFinancialPositionController {

    private final PartyFinancialPositionService service;

    @GetMapping("/{id}/financial-position")
    public PartyFinancialPositionSummary getFinancialPosition(@PathVariable String id) {
        return service.getFinancialPosition(id);
    }

    @GetMapping("/{id}/statement")
    public PartyStatementResponse getStatement(@PathVariable String id,
                                               @RequestParam(required = false) Long fromDate,
                                               @RequestParam(required = false) Long toDate,
                                               @RequestParam(required = false) String projectId) {
        return service.getStatement(id, fromDate, toDate, projectId);
    }

    @GetMapping("/reports/aging")
    public AgingReportResponse getAgingReport(@RequestParam(required = false) String partyType,
                                              @RequestParam(required = false) Long asOfDate) {
        return service.getAgingReport(partyType, asOfDate);
    }

    @GetMapping("/reports/gl-reconciliation")
    public ArApGlReconciliationResponse getGlReconciliation(@RequestParam(required = false) String partyType) {
        return service.getArApGlReconciliation(partyType);
    }

    @GetMapping("/{id}/statement/export.csv")
    public ResponseEntity<byte[]> exportStatementCsv(@PathVariable String id,
                                                     @RequestParam(required = false) Long fromDate,
                                                     @RequestParam(required = false) Long toDate) {
        byte[] csv = service.exportStatementCsv(id, fromDate, toDate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"party-statement-" + id + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
