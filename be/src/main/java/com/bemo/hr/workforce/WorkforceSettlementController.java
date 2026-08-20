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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE', 'FINANCE_MANAGER')")
    public List<WorkforceApi.SettlementPeriodResponse> listPeriods() {
        return settlementService.listPeriods();
    }

    @GetMapping("/periods/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE', 'FINANCE_MANAGER')")
    public WorkforceApi.SettlementPeriodResponse getPeriod(@PathVariable String id) {
        return settlementService.getPeriod(id);
    }

    @GetMapping("/periods/{id}/issues")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE', 'FINANCE_MANAGER')")
    public List<WorkforceApi.SettlementIssueResponse> listIssues(@PathVariable String id) {
        return settlementService.listIssues(id);
    }

    @GetMapping("/periods/{id}/contractor-settlements")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE', 'FINANCE_MANAGER')")
    public List<WorkforceApi.ContractorSettlementDetailResponse> listContractorSettlementsForPeriod(@PathVariable String id) {
        return settlementService.listContractorSettlementsForPeriod(id);
    }

    @GetMapping("/contractor-settlements/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE', 'FINANCE_MANAGER')")
    public WorkforceApi.ContractorSettlementDetailResponse getContractorSettlement(@PathVariable String id) {
        return settlementService.getContractorSettlement(id);
    }

    @PostMapping("/periods")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkforceApi.SettlementPeriodResponse createPeriod(@Valid @RequestBody WorkforceApi.SettlementPeriodRequest request) {
        return settlementService.createPeriod(request);
    }

    @PostMapping("/periods/{id}/calculate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER')")
    public WorkforceApi.SettlementCalculationSummary calculatePeriod(@PathVariable String id) {
        return settlementService.calculatePeriod(id);
    }

    @PostMapping("/periods/{id}/review")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER')")
    public TransitionResponse reviewPeriod(@PathVariable String id) {
        return settlementService.reviewPeriod(id);
    }

    @PostMapping("/periods/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_FINANCE', 'FINANCE_MANAGER')")
    public TransitionResponse approvePeriod(@PathVariable String id) {
        return settlementService.approvePeriod(id);
    }

    @PostMapping("/periods/{id}/lock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public TransitionResponse lockPeriod(@PathVariable String id) {
        return settlementService.lockPeriod(id);
    }

    @PostMapping("/contractor-settlements/{id}/post")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_FINANCE', 'FINANCE_MANAGER')")
    public WorkforceApi.ContractorSettlementDetailResponse postSettlement(@PathVariable String id,
                                                                          @Valid @RequestBody WorkforceApi.SettlementPostingRequest request) {
        return settlementService.postSettlement(id, request);
    }

    @PostMapping("/contractor-settlements/{id}/link-invoice")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_FINANCE', 'FINANCE_MANAGER')")
    public WorkforceApi.ContractorSettlementDetailResponse linkInvoice(@PathVariable String id,
                                                                       @Valid @RequestBody WorkforceApi.LinkInvoiceRequest request) {
        return settlementService.linkInvoice(id, request);
    }

    @PostMapping("/contractor-settlements/{id}/mark-paid")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_FINANCE', 'FINANCE_MANAGER')")
    public WorkforceApi.ContractorSettlementDetailResponse markPaid(@PathVariable String id,
                                                                    @Valid @RequestBody WorkforceApi.RecordSettlementPaymentRequest request) {
        return settlementService.recordPayment(id, request);
    }

    @GetMapping("/projects/{projectId}/labor-cost-report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WORKFORCE_FINANCE', 'WORKFORCE_MANAGER')")
    public WorkforceApi.ProjectLaborCostReportResponse getProjectLaborCostReport(
            @PathVariable String projectId,
            @RequestParam(required = false) String periodId
    ) {
        return settlementService.getProjectLaborCostReport(projectId, periodId);
    }

    @GetMapping(value = "/periods/{id}/export-excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE', 'FINANCE_MANAGER')")
    public org.springframework.http.ResponseEntity<byte[]> exportPeriodExcel(@PathVariable String id) {
        byte[] excelBytes = settlementService.exportPeriodExcel(id);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Workforce_Settlement_Period_" + id + ".xlsx\"")
                .body(excelBytes);
    }
}
