package com.bemo.hr.finance.api;

import com.bemo.hr.finance.application.BankReconciliationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/bank-reconciliation")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'AUDITOR')")
public class BankReconciliationController {
    private final BankReconciliationService bankReconciliationService;

    @GetMapping("/statements")
    public List<BankReconciliationApi.StatementResponse> statements() {
        return bankReconciliationService.listStatements();
    }

    @PostMapping(value = "/statements/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER')")
    public BankReconciliationApi.WorkbenchResponse importStatement(
            @RequestParam String bankAccountId, @RequestParam String statementReference,
            @RequestParam BigDecimal openingBalance, @RequestParam BigDecimal closingBalance,
            @RequestPart("file") MultipartFile file) {
        return bankReconciliationService.importCsv(bankAccountId, statementReference, openingBalance, closingBalance, file);
    }

    @GetMapping("/statements/{id}")
    public BankReconciliationApi.WorkbenchResponse workbench(@PathVariable String id) {
        return bankReconciliationService.workbench(id);
    }

    @PostMapping("/statements/{id}/auto-match")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER')")
    public BankReconciliationApi.WorkbenchResponse autoMatch(@PathVariable String id,
                                                             @Valid @RequestBody BankReconciliationApi.OperationRequest request) {
        return bankReconciliationService.autoMatch(id, request);
    }

    @PostMapping("/statements/{id}/lines/{lineId}/match")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER')")
    public BankReconciliationApi.WorkbenchResponse match(@PathVariable String id, @PathVariable String lineId,
                                                         @Valid @RequestBody BankReconciliationApi.MatchRequest request) {
        return bankReconciliationService.match(id, lineId, request);
    }

    @PostMapping("/statements/{id}/matches/{matchId}/reverse")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public BankReconciliationApi.WorkbenchResponse reverse(@PathVariable String id, @PathVariable String matchId,
                                                           @Valid @RequestBody BankReconciliationApi.ReverseRequest request) {
        return bankReconciliationService.reverse(id, matchId, request);
    }

    @GetMapping("/cash-position")
    public BankReconciliationApi.CashPositionResponse cashPosition() {
        return bankReconciliationService.cashPosition();
    }
}
