package com.bemo.hr.workforce;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workforce/settlements")
@RequiredArgsConstructor
public class WorkforceSettlementController {
    private final WorkforceSettlementService settlementService;

    @GetMapping("/periods")
    public List<WorkforceApi.SettlementPeriodResponse> listPeriods() {
        return settlementService.listPeriods();
    }

    @GetMapping("/periods/{id}")
    public WorkforceApi.SettlementPeriodResponse getPeriod(@PathVariable String id) {
        return settlementService.getPeriod(id);
    }

    @GetMapping("/periods/{id}/issues")
    public List<WorkforceApi.SettlementIssueResponse> listIssues(@PathVariable String id) {
        return settlementService.listIssues(id);
    }

    @PostMapping("/periods")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkforceApi.SettlementPeriodResponse createPeriod(@Valid @RequestBody WorkforceApi.SettlementPeriodRequest request) {
        return settlementService.createPeriod(request);
    }

    @PostMapping("/periods/{id}/calculate")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public WorkforceApi.SettlementCalculationSummary calculatePeriod(@PathVariable String id) {
        return settlementService.calculatePeriod(id);
    }

    @PostMapping("/periods/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public WorkforceApi.SettlementPeriodResponse reviewPeriod(@PathVariable String id) {
        return settlementService.reviewPeriod(id);
    }

    @PostMapping("/periods/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public WorkforceApi.SettlementPeriodResponse approvePeriod(@PathVariable String id) {
        return settlementService.approvePeriod(id);
    }

    @PostMapping("/periods/{id}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public WorkforceApi.SettlementPeriodResponse lockPeriod(@PathVariable String id) {
        return settlementService.lockPeriod(id);
    }

    @GetMapping(value = "/periods/{id}/export-excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public org.springframework.http.ResponseEntity<byte[]> exportPeriodExcel(@PathVariable String id) {
        byte[] excelBytes = settlementService.exportPeriodExcel(id);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Workforce_Settlement_Period_" + id + ".xlsx\"")
                .body(excelBytes);
    }
}
