package com.bemo.hr.finance.api;

import com.bemo.hr.finance.application.JournalEntryService;
import com.bemo.hr.finance.domain.Account;
import com.bemo.hr.finance.infrastructure.AccountRepository;
import com.bemo.hr.shared.security.Roles;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance")
@PreAuthorize(Roles.ADMIN_ACCOUNTANT_AUDITOR_FINANCE_MANAGER_HR_MANAGER_TREASURY_USER)
public class AccountingController {

    private final AccountRepository accountRepository;
    private final JournalEntryService journalEntryService;
    private final com.bemo.hr.finance.application.JournalDimensionReportService journalDimensionReportService;

    public AccountingController(AccountRepository accountRepository, JournalEntryService journalEntryService,
                                com.bemo.hr.finance.application.JournalDimensionReportService journalDimensionReportService) {
        this.accountRepository = accountRepository;
        this.journalEntryService = journalEntryService;
        this.journalDimensionReportService = journalDimensionReportService;
    }

    // --- Chart of Accounts ---
    @GetMapping("/accounts")
    public List<AccountingApi.AccountResponse> listAccounts() {
        return accountRepository.findAllByOrderByCodeAsc().stream().map(this::toResponse).toList();
    }

    @PostMapping("/accounts")
    @Transactional
    @PreAuthorize(Roles.ADMIN_ACCOUNTANT_FINANCE_MANAGER)
    public AccountingApi.AccountResponse createAccount(@Valid @RequestBody AccountingApi.AccountPayload payload) {
        Account.Type type = Account.Type.valueOf(payload.type().toUpperCase());
        Account account = new Account(payload.code(), payload.name(), type, payload.parentId(), payload.isHeader(), payload.currency(), payload.active());
        return toResponse(accountRepository.save(account));
    }

    @PutMapping("/accounts/{id}")
    @Transactional
    @PreAuthorize(Roles.ADMIN_ACCOUNTANT_FINANCE_MANAGER)
    public AccountingApi.AccountResponse updateAccount(@PathVariable String id, @Valid @RequestBody AccountingApi.AccountPayload payload) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new com.bemo.hr.shared.domain.BusinessRuleException("الحساب غير موجود في دليل الحسابات"));
        Account.Type type = Account.Type.valueOf(payload.type().toUpperCase());
        account.update(payload.code(), payload.name(), type, payload.parentId(), payload.isHeader(), payload.currency(), payload.active());
        return toResponse(accountRepository.save(account));
    }

    // --- Journal Entries ---
    @GetMapping("/numbering-settings")
    public AccountingApi.NumberingSettings numberingSettings() {
        return journalEntryService.numberingSettings();
    }

    @GetMapping("/journal-entries")
    public AccountingApi.JournalEntryPageResponse listJournalEntries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        var result = journalEntryService.listPage(pageable);
        return new AccountingApi.JournalEntryPageResponse(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @PostMapping("/journal-entries")
    @PreAuthorize(Roles.ADMIN_ACCOUNTANT_FINANCE_MANAGER)
    public AccountingApi.JournalEntryResponse createJournalEntry(@Valid @RequestBody AccountingApi.JournalEntryPayload payload,
                                                                 Authentication authentication) {
        return journalEntryService.create(payload, authentication.getName());
    }

    @PostMapping("/journal-entries/{id}/post")
    @PreAuthorize(Roles.ADMIN_ACCOUNTANT_FINANCE_MANAGER)
    public AccountingApi.JournalEntryResponse postJournalEntry(@PathVariable String id,
                                                               @Valid @RequestBody AccountingApi.JournalActionRequest request,
                                                               Authentication authentication) {
        return journalEntryService.post(id, request, authentication.getName());
    }

    @GetMapping("/reports/dimensions")
    public List<com.bemo.hr.finance.application.JournalDimensionReportService.DimensionSummary> dimensionReport(
            @RequestParam java.time.LocalDate from, @RequestParam java.time.LocalDate to,
            @RequestParam(required = false) String costCenterId, @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String departmentId) {
        return journalDimensionReportService.summarize(from, to, costCenterId, projectId, departmentId);
    }

    @PostMapping("/journal-entries/{id}/approve")
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER)
    public AccountingApi.JournalEntryResponse approveJournalEntry(@PathVariable String id,
                                                                  @Valid @RequestBody AccountingApi.JournalActionRequest request, Authentication authentication) {
        return journalEntryService.approve(id, request, authentication.getName());
    }

    @PostMapping("/journal-entries/{id}/reject")
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER)
    public AccountingApi.JournalEntryResponse rejectJournalEntry(@PathVariable String id,
                                                                 @Valid @RequestBody AccountingApi.JournalActionRequest request, Authentication authentication) {
        return journalEntryService.reject(id, request, authentication.getName());
    }

    @PostMapping("/journal-entries/{id}/reverse")
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER)
    public AccountingApi.JournalEntryResponse reverseJournalEntry(@PathVariable String id,
                                                                  @Valid @RequestBody AccountingApi.JournalActionRequest request,
                                                                  Authentication authentication) {
        return journalEntryService.reverse(id, request, authentication.getName());
    }

    private AccountingApi.AccountResponse toResponse(Account a) {
        return new AccountingApi.AccountResponse(
                a.getId(), a.getCode(), a.getName(), a.getType().name(), a.getParentId(),
                a.isHeader(), a.getCurrency(), a.isActive(), a.getCreatedAt(), a.getUpdatedAt()
        );
    }
}
