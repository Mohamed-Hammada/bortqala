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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
