package com.bemo.hr.budget.api;

import com.bemo.hr.budget.application.BudgetService;
import com.bemo.hr.shared.security.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/budget")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'AUDITOR')")
public class BudgetController {

    private final BudgetService budgetService;
    private final AuthService authService;

    public BudgetController(BudgetService budgetService, AuthService authService) {
        this.budgetService = budgetService;
        this.authService = authService;
    }

    @GetMapping(value = "/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> export(Authentication authentication) {
        var preference = authService.currentPreferences(authentication.getName());
        String filename = preference.locale().startsWith("ar") ? "الميزانيات" : "budgets";
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(
                filename + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".xlsx",
                StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(budgetService.export(preference.locale()));
    }

    @GetMapping("/budgets")
    public List<BudgetApi.BudgetResponse> listBudgets() {
        return budgetService.listBudgets();
    }

    @PostMapping("/budgets")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public BudgetApi.BudgetResponse createBudget(@Valid @RequestBody BudgetApi.BudgetPayload payload) {
        return budgetService.createBudget(payload);
    }

    @PutMapping("/budgets/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public BudgetApi.BudgetResponse updateBudget(@PathVariable String id,
                                                 @Valid @RequestBody BudgetApi.BudgetPayload payload) {
        return budgetService.updateBudget(id, payload);
    }

    @DeleteMapping("/budgets/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public void deleteBudget(@PathVariable String id) {
        budgetService.deleteBudget(id);
    }

    @GetMapping("/status")
    public List<BudgetApi.BudgetStatusResponse> status(@RequestParam(required = false) Integer year) {
        return budgetService.status(year);
    }

    @GetMapping("/encumbrances")
    public List<BudgetApi.EncumbranceResponse> encumbrances() {
        return budgetService.listEncumbrances();
    }

    @PostMapping("/budgets/{id}/revisions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public com.bemo.hr.budget.BudgetRevision reviseBudget(@PathVariable String id, @Valid @RequestBody ReviseBudgetPayload payload, Authentication auth) {
        return budgetService.reviseBudget(id, payload.newAmount(), payload.reason(), auth.getName());
    }

    @GetMapping("/budgets/{id}/revisions")
    public List<com.bemo.hr.budget.BudgetRevision> listRevisions(@PathVariable String id) {
        return budgetService.listRevisions(id);
    }

    @PostMapping("/budgets/{id}/revisions/{revisionId}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public com.bemo.hr.budget.BudgetRevision approveRevision(@PathVariable String id, @PathVariable String revisionId, Authentication auth) {
        return budgetService.approveRevision(id, revisionId, auth.getName());
    }

    @PostMapping("/budgets/{id}/revisions/{revisionId}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public com.bemo.hr.budget.BudgetRevision rejectRevision(@PathVariable String id, @PathVariable String revisionId, Authentication auth) {
        return budgetService.rejectRevision(id, revisionId, auth.getName());
    }

    @PostMapping("/transfers")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public com.bemo.hr.budget.BudgetTransfer createTransfer(@RequestBody CreateTransferPayload payload) {
        return budgetService.createTransfer(payload.transferNumber(), payload.sourceBudgetId(), payload.targetBudgetId(), payload.transferAmount(), payload.reason());
    }

    @PostMapping("/transfers/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public com.bemo.hr.budget.BudgetTransfer approveTransfer(@PathVariable String id) {
        return budgetService.approveTransfer(id);
    }

    public record ReviseBudgetPayload(
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.DecimalMin("0") java.math.BigDecimal newAmount,
            @jakarta.validation.constraints.NotBlank String reason) {
    }

    public record CreateTransferPayload(String transferNumber, String sourceBudgetId, String targetBudgetId,
                                        java.math.BigDecimal transferAmount, String reason) {
    }
}
