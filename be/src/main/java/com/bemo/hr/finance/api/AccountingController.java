package com.bemo.hr.finance.api;

import com.bemo.hr.finance.domain.Account;
import com.bemo.hr.finance.domain.JournalEntry;
import com.bemo.hr.finance.domain.JournalEntryLine;
import com.bemo.hr.finance.infrastructure.AccountRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryLineRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/v1/finance")
public class AccountingController {

    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;

    public AccountingController(AccountRepository accountRepository,
                                JournalEntryRepository journalEntryRepository,
                                JournalEntryLineRepository journalEntryLineRepository) {
        this.accountRepository = accountRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.journalEntryLineRepository = journalEntryLineRepository;
    }

    // --- Chart of Accounts ---
    @GetMapping("/accounts")
    public List<AccountingApi.AccountResponse> listAccounts() {
        return accountRepository.findAllByOrderByCodeAsc().stream().map(this::toResponse).toList();
    }

    @PostMapping("/accounts")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    public AccountingApi.AccountResponse createAccount(@Valid @RequestBody AccountingApi.AccountPayload payload) {
        Account.Type type = Account.Type.valueOf(payload.type().toUpperCase());
        Account account = new Account(payload.code(), payload.name(), type, payload.parentId(), payload.isHeader(), payload.currency(), payload.active());
        return toResponse(accountRepository.save(account));
    }

    @PutMapping("/accounts/{id}")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    public AccountingApi.AccountResponse updateAccount(@PathVariable String id, @Valid @RequestBody AccountingApi.AccountPayload payload) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("الحساب غير موجود في دليل الحسابات"));
        Account.Type type = Account.Type.valueOf(payload.type().toUpperCase());
        account.update(payload.code(), payload.name(), type, payload.parentId(), payload.isHeader(), payload.currency(), payload.active());
        return toResponse(accountRepository.save(account));
    }

    // --- Journal Entries ---
    @GetMapping("/journal-entries")
    public AccountingApi.JournalEntryPageResponse listJournalEntries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = PageRequest.of(page, size);
        var result = journalEntryRepository.findAllByOrderByEntryDateDescCreatedAtDesc(pageable);
        var content = result.getContent().stream().map(this::toResponse).toList();
        return new AccountingApi.JournalEntryPageResponse(content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @PostMapping("/journal-entries")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    public AccountingApi.JournalEntryResponse createJournalEntry(@Valid @RequestBody AccountingApi.JournalEntryPayload payload) {
        // Enforce debit == credit invariant
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (var line : payload.lines()) {
            totalDebit = totalDebit.add(line.debit() == null ? BigDecimal.ZERO : line.debit());
            totalCredit = totalCredit.add(line.credit() == null ? BigDecimal.ZERO : line.credit());
        }

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BusinessRuleException("القيد غير متوازن! مجموع المدين (" + totalDebit + ") يجب أن يساوي مجموع الدائن (" + totalCredit + ")");
        }

        LocalDate entryDate = Instant.ofEpochMilli(payload.entryDate()).atZone(ZoneOffset.UTC).toLocalDate();
        JournalEntry entry = new JournalEntry(payload.entryNumber(), entryDate, payload.description(), payload.reference(), payload.fiscalPeriodId());
        entry = journalEntryRepository.save(entry);

        for (var linePayload : payload.lines()) {
            JournalEntryLine line = new JournalEntryLine(entry.getId(), linePayload.accountId(), linePayload.partyId(), linePayload.debit(), linePayload.credit(), linePayload.memo());
            journalEntryLineRepository.save(line);
        }

        return toResponse(entry);
    }

    @PostMapping("/journal-entries/{id}/post")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    public AccountingApi.JournalEntryResponse postJournalEntry(@PathVariable String id, Authentication authentication) {
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("قيد اليومية غير موجود"));
        entry.post(authentication.getName());
        return toResponse(journalEntryRepository.save(entry));
    }

    @PostMapping("/journal-entries/{id}/reverse")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    public AccountingApi.JournalEntryResponse reverseJournalEntry(@PathVariable String id) {
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("قيد اليومية غير موجود"));
        entry.reverse();
        return toResponse(journalEntryRepository.save(entry));
    }

    private AccountingApi.AccountResponse toResponse(Account a) {
        return new AccountingApi.AccountResponse(
                a.getId(), a.getCode(), a.getName(), a.getType().name(), a.getParentId(),
                a.isHeader(), a.getCurrency(), a.isActive(), a.getCreatedAt(), a.getUpdatedAt()
        );
    }

    private AccountingApi.JournalEntryResponse toResponse(JournalEntry e) {
        var lines = journalEntryLineRepository.findByJournalEntryId(e.getId()).stream()
                .map(l -> new AccountingApi.JournalEntryLineResponse(l.getId(), l.getJournalEntryId(), l.getAccountId(), l.getPartyId(), l.getDebit(), l.getCredit(), l.getMemo()))
                .toList();

        BigDecimal totalDebit = lines.stream().map(AccountingApi.JournalEntryLineResponse::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = lines.stream().map(AccountingApi.JournalEntryLineResponse::credit).reduce(BigDecimal.ZERO, BigDecimal::add);

        long entryDateMs = e.getEntryDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        return new AccountingApi.JournalEntryResponse(
                e.getId(), e.getEntryNumber(), entryDateMs, e.getDescription(), e.getReference(),
                e.getStatus().name(), e.getFiscalPeriodId(), e.getPostedBy(),
                e.getPostedAt(),
                lines, totalDebit, totalCredit, e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
