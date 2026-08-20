package com.bemo.hr.workforce;

import com.bemo.hr.shared.api.TransitionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workforce/settlements")
@RequiredArgsConstructor
public class WorkforceSettlementController {
    private final WorkforceSettlementService settlementService;

    @GetMapping("/periods")
    @PreAuthorize("@auth.hasPermission('settlements.read')")
    public List<WorkforceApi.SettlementPeriodResponse> listPeriods() {
        return settlementService.listPeriods();
    }

    @GetMapping("/periods/{id}")
    @PreAuthorize("@auth.hasPermission('settlements.read')")
    public WorkforceApi.SettlementPeriodResponse getPeriod(@PathVariable String id) {
        return settlementService.getPeriod(id);
    }

    @GetMapping("/periods/{id}/issues")
    @PreAuthorize("@auth.hasPermission('settlements.read')")
    public List<WorkforceApi.SettlementIssueResponse> listIssues(@PathVariable String id) {
        return settlementService.listIssues(id);
    }

    @GetMapping("/periods/{id}/contractor-settlements")
    @PreAuthorize("@auth.hasPermission('settlements.read')")
    public List<WorkforceApi.ContractorSettlementDetailResponse> listContractorSettlementsForPeriod(@PathVariable String id) {
        return settlementService.listContractorSettlementsForPeriod(id);
    }

    @GetMapping("/contractor-settlements/{id}")
    @PreAuthorize("@auth.hasPermission('settlements.read')")
    public WorkforceApi.ContractorSettlementDetailResponse getContractorSettlement(@PathVariable String id) {
        return settlementService.getContractorSettlement(id);
    }

    @PostMapping("/periods")
    @PreAuthorize("@auth.hasPermission('settlements.prepare')")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkforceApi.SettlementPeriodResponse createPeriod(@Valid @RequestBody WorkforceApi.SettlementPeriodRequest request) {
        return settlementService.createPeriod(request);
    }

    @PostMapping("/periods/{id}/calculate")
    @PreAuthorize("@auth.hasPermission('settlements.prepare')")
    public WorkforceApi.SettlementCalculationSummary calculatePeriod(@PathVariable String id) {
        return settlementService.calculatePeriod(id);
    }

    @PostMapping("/periods/{id}/review")
    @PreAuthorize("@auth.hasPermission('settlements.prepare')")
    public TransitionResponse reviewPeriod(@PathVariable String id) {
        return settlementService.reviewPeriod(id);
    }

    @PostMapping("/periods/{id}/approve")
    @PreAuthorize("@auth.hasPermission('settlements.finalize')")
    public TransitionResponse approvePeriod(@PathVariable String id) {
        return settlementService.approvePeriod(id);
    }

    @PostMapping("/periods/{id}/lock")
    @PreAuthorize("@auth.hasPermission('settlements.finalize')")
    public TransitionResponse lockPeriod(@PathVariable String id) {
        return settlementService.lockPeriod(id);
    }

    @PostMapping("/contractor-settlements/{id}/post")
    @PreAuthorize("@auth.hasPermission('settlements.finalize')")
    public WorkforceApi.ContractorSettlementDetailResponse postSettlement(@PathVariable String id,
                                                                          @Valid @RequestBody WorkforceApi.SettlementPostingRequest request) {
        return settlementService.postSettlement(id, request);
    }

    @PostMapping("/contractor-settlements/{id}/link-invoice")
    @PreAuthorize("@auth.hasPermission('settlements.finalize')")
    public WorkforceApi.ContractorSettlementDetailResponse linkInvoice(@PathVariable String id,
                                                                       @Valid @RequestBody WorkforceApi.LinkInvoiceRequest request) {
        return settlementService.linkInvoice(id, request);
    }

    @PostMapping("/contractor-settlements/{id}/mark-paid")
    @PreAuthorize("@auth.hasPermission('settlements.finalize')")
    public WorkforceApi.ContractorSettlementDetailResponse markPaid(@PathVariable String id,
                                                                    @Valid @RequestBody WorkforceApi.RecordSettlementPaymentRequest request) {
        return settlementService.recordPayment(id, request);
    }

    @GetMapping("/projects/{projectId}/labor-cost-report")
    @PreAuthorize("@auth.hasPermission('settlements.read')")
    public WorkforceApi.ProjectLaborCostReportResponse getProjectLaborCostReport(
            @PathVariable String projectId,
            @RequestParam(required = false) String periodId
    ) {
        return settlementService.getProjectLaborCostReport(projectId, periodId);
    }

    @GetMapping(value = "/periods/{id}/export-excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("@auth.hasPermission('settlements.read')")
    public org.springframework.http.ResponseEntity<byte[]> exportPeriodExcel(@PathVariable String id) {
        byte[] excelBytes = settlementService.exportPeriodExcel(id);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Workforce_Settlement_Period_" + id + ".xlsx\"")
                .body(excelBytes);
    }
}
