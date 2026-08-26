package com.bemo.hr.expenses.api;

import com.bemo.hr.expenses.application.ExpenseClaimService;
import com.bemo.hr.expenses.domain.ExpenseClaim;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.shared.i18n.TranslationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
public class ExpenseClaimController {
    private final ExpenseClaimService expenseClaimService;
    private final EmployeeRepository employeeRepository;
    private final TranslationService translationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ExpenseClaimApi.ClaimResponse>> listMine(Authentication authentication) {
        List<ExpenseClaim> claims = expenseClaimService.listMine(authentication.getName());
        return ResponseEntity.ok(claims.stream().map(this::toResponse).toList());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('HR_MANAGER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<ExpenseClaimApi.ClaimResponse>> listPending() {
        List<ExpenseClaim> claims = expenseClaimService.listPending();
        return ResponseEntity.ok(claims.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExpenseClaimApi.ClaimResponse> getById(@PathVariable String id) {
        ExpenseClaim claim = expenseClaimService.getById(id);
        return ResponseEntity.ok(toResponse(claim));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExpenseClaimApi.ClaimResponse> create(
            Authentication authentication,
            @Valid @RequestBody ExpenseClaimApi.CreateClaimRequest request) {
        ExpenseClaim claim = expenseClaimService.create(authentication.getName(), request);
        return ResponseEntity.ok(toResponse(claim));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExpenseClaimApi.ClaimResponse> update(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody ExpenseClaimApi.UpdateClaimRequest request) {
        ExpenseClaim claim = expenseClaimService.update(authentication.getName(), id, request);
        return ResponseEntity.ok(toResponse(claim));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExpenseClaimApi.ClaimResponse> submit(
            Authentication authentication,
            @PathVariable String id) {
        ExpenseClaim claim = expenseClaimService.submit(authentication.getName(), id);
        return ResponseEntity.ok(toResponse(claim));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('HR_MANAGER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ExpenseClaimApi.ClaimResponse> approve(
            @PathVariable String id,
            @RequestBody(required = false) ExpenseClaimApi.DecisionRequest request) {
        ExpenseClaim claim = expenseClaimService.approve(id, request != null ? request.note() : null);
        return ResponseEntity.ok(toResponse(claim));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('HR_MANAGER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ExpenseClaimApi.ClaimResponse> reject(
            @PathVariable String id,
            @RequestBody(required = false) ExpenseClaimApi.DecisionRequest request) {
        ExpenseClaim claim = expenseClaimService.reject(id, request != null ? request.note() : null);
        return ResponseEntity.ok(toResponse(claim));
    }

    @PostMapping("/{id}/reimburse")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','ACCOUNTANT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ExpenseClaimApi.ClaimResponse> reimburse(
            @PathVariable String id,
            @Valid @RequestBody ExpenseClaimApi.ReimburseRequest request) {
        ExpenseClaim claim = expenseClaimService.reimburse(id, request.reference());
        return ResponseEntity.ok(toResponse(claim));
    }

    private ExpenseClaimApi.ClaimResponse toResponse(ExpenseClaim c) {
        String employeeName = employeeRepository.findById(c.getEmployeeId())
                .map(e -> e.getFullName())
                .orElse("");
        return new ExpenseClaimApi.ClaimResponse(
                c.getId(), c.getEmployeeId(), employeeName,
                c.getCategory(),
                c.getSpentOn() != null ? c.getSpentOn().toString() : null,
                c.getAmount(), c.getCurrency(),
                c.getDescription(),
                c.getReceiptName(), c.getReceiptContentType(), c.getReceiptSize(),
                c.getStatus(),
                c.getApproverId(),
                c.getDecidedAt() != null ? c.getDecidedAt().toEpochMilli() : null,
                c.getDecisionNote(),
                c.getReimbursementReference(),
                c.getCreatedAt() != null ? c.getCreatedAt().toEpochMilli() : 0,
                c.getUpdatedAt() != null ? c.getUpdatedAt().toEpochMilli() : 0);
    }
}
