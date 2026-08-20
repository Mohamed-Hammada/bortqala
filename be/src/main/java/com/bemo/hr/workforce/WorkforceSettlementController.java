package com.bemo.hr.workforce;

import com.bemo.hr.shared.api.TransitionResponse;
import com.bemo.hr.shared.security.Roles;
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
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.WORKFORCE_FINANCE + " or " + Roles.WORKFORCE_MANAGER + " or " + Roles.WORKFORCE_REVIEWER)
    public List<WorkforceApi.SettlementPeriodResponse> listPeriods() {
        return settlementService.listPeriods();
    }

    @GetMapping("/periods/{id}")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.WORKFORCE_FINANCE + " or " + Roles.WORKFORCE_MANAGER + " or " + Roles.WORKFORCE_REVIEWER)
    public WorkforceApi.SettlementPeriodResponse getPeriod(@PathVariable String id) {
        return settlementService.getPeriod(id);
    }

    @GetMapping("/periods/{id}/issues")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.WORKFORCE_FINANCE + " or " + Roles.WORKFORCE_MANAGER + " or " + Roles.WORKFORCE_REVIEWER)
    public List<WorkforceApi.SettlementIssueResponse> listIssues(@PathVariable String id) {
        return settlementService.listIssues(id);
    }

    @GetMapping("/periods/{id}/contractor-settlements")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.WORKFORCE_FINANCE + " or " + Roles.WORKFORCE_MANAGER + " or " + Roles.WORKFORCE_REVIEWER)
    public List<WorkforceApi.ContractorSettlementDetailResponse> listContractorSettlementsForPeriod(@PathVariable String id) {
        return settlementService.listContractorSettlementsForPeriod(id);
    }

    @GetMapping("/contractor-settlements/{id}")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.WORKFORCE_FINANCE + " or " + Roles.WORKFORCE_MANAGER + " or " + Roles.WORKFORCE_REVIEWER)
    public WorkforceApi.ContractorSettlementDetailResponse getContractorSettlement(@PathVariable String id) {
        return settlementService.getContractorSettlement(id);
    }

    @PostMapping("/periods")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.WORKFORCE_MANAGER)
    @ResponseStatus(HttpStatus.CREATED)
    public WorkforceApi.SettlementPeriodResponse createPeriod(@Valid @RequestBody WorkforceApi.SettlementPeriodRequest request) {
        return settlementService.createPeriod(request);
    }

    @PostMapping("/periods/{id}/calculate")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.WORKFORCE_MANAGER)
    public WorkforceApi.SettlementCalculationSummary calculatePeriod(@PathVariable String id) {
        return settlementService.calculatePeriod(id);
    }

    @PostMapping("/periods/{id}/review")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.WORKFORCE_MANAGER)
    public TransitionResponse reviewPeriod(@PathVariable String id) {
        return settlementService.reviewPeriod(id);
    }

    @PostMapping("/periods/{id}/approve")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.WORKFORCE_FINANCE)
    public TransitionResponse approvePeriod(@PathVariable String id) {
        return settlementService.approvePeriod(id);
    }

    @PostMapping("/periods/{id}/lock")
    @PreAuthorize(Roles.ADMIN_ONLY)
    public TransitionResponse lockPeriod(@PathVariable String id) {
        return settlementService.lockPeriod(id);
    }

    @PostMapping("/contractor-settlements/{id}/post")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.WORKFORCE_FINANCE)
    public WorkforceApi.ContractorSettlementDetailResponse postSettlement(@PathVariable String id,
                                                                          @Valid @RequestBody WorkforceApi.SettlementPostingRequest request) {
        return settlementService.postSettlement(id, request);
    }

    @PostMapping("/contractor-settlements/{id}/link-invoice")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.WORKFORCE_FINANCE)
    public WorkforceApi.ContractorSettlementDetailResponse linkInvoice(@PathVariable String id,
                                                                       @Valid @RequestBody WorkforceApi.LinkInvoiceRequest request) {
        return settlementService.linkInvoice(id, request);
    }

    @PostMapping("/contractor-settlements/{id}/mark-paid")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.WORKFORCE_FINANCE)
    public WorkforceApi.ContractorSettlementDetailResponse markPaid(@PathVariable String id,
                                                                    @Valid @RequestBody WorkforceApi.RecordSettlementPaymentRequest request) {
        return settlementService.recordPayment(id, request);
    }

    @GetMapping("/projects/{projectId}/labor-cost-report")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.PROJECT_MANAGER + " or " + Roles.WORKFORCE_FINANCE + " or " + Roles.WORKFORCE_MANAGER)
    public WorkforceApi.ProjectLaborCostReportResponse getProjectLaborCostReport(
            @PathVariable String projectId,
            @RequestParam(required = false) String periodId
    ) {
        return settlementService.getProjectLaborCostReport(projectId, periodId);
    }

    @GetMapping(value = "/periods/{id}/export-excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.WORKFORCE_FINANCE + " or " + Roles.WORKFORCE_MANAGER + " or " + Roles.WORKFORCE_REVIEWER)
    public org.springframework.http.ResponseEntity<byte[]> exportPeriodExcel(@PathVariable String id) {
        byte[] excelBytes = settlementService.exportPeriodExcel(id);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Workforce_Settlement_Period_" + id + ".xlsx\"")
                .body(excelBytes);
    }
}
