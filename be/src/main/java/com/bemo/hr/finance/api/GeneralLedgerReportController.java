package com.bemo.hr.finance.api;

import com.bemo.hr.finance.application.GeneralLedgerReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/reports/general-ledger")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE_MANAGER','ACCOUNTANT','AUDITOR','VIEWER')")
public class GeneralLedgerReportController {
    private final GeneralLedgerReportService service;

    public GeneralLedgerReportController(GeneralLedgerReportService s) {
        service = s;
    }

    @GetMapping
    public List<GeneralLedgerReportService.Row> detail(@RequestParam LocalDate from,
                                                       @RequestParam LocalDate to,
                                                       @RequestParam(required = false) String accountId,
                                                       @RequestParam(required = false) String projectId,
                                                       @RequestParam(required = false) String costCodeId) {
        return service.detail(from, to, accountId, projectId, costCodeId);
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> export(@RequestParam LocalDate from,
                                         @RequestParam LocalDate to,
                                         @RequestParam(required = false) String accountId,
                                         @RequestParam(required = false) String projectId,
                                         @RequestParam(required = false) String costCodeId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=general-ledger.csv")
                .body(service.exportCsv(from, to, accountId, projectId, costCodeId));
    }
}
