package com.bemo.hr.finance.application;

import com.bemo.hr.finance.api.AccountingApi;
import com.bemo.hr.finance.domain.Account;
import com.bemo.hr.finance.domain.FiscalPeriod;
import com.bemo.hr.finance.domain.FiscalPeriodGuard;
import com.bemo.hr.finance.domain.JournalEntry;
import com.bemo.hr.finance.domain.JournalEntryLine;
import com.bemo.hr.finance.infrastructure.AccountRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryLineRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.idempotency.application.IdempotencyService;
import com.bemo.hr.shared.numbering.DocumentNumberService;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.approval.SegregationOfDutiesService;
import com.bemo.hr.audit.application.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

@Service
public class JournalEntryService {
    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;
    private final AccountRepository accountRepository;
    private final FiscalPeriodGuard fiscalPeriodGuard;
    private final IdempotencyService idempotencyService;
    private final DocumentNumberService documentNumberService;
    private final TenantApplicationRepository tenantApplicationRepository;
    private final SegregationOfDutiesService segregationOfDutiesService;
    private final AuditService auditService;

    public JournalEntryService(JournalEntryRepository journalEntryRepository,
                               JournalEntryLineRepository journalEntryLineRepository,
                               AccountRepository accountRepository,
                               FiscalPeriodGuard fiscalPeriodGuard,
                               IdempotencyService idempotencyService,
                               DocumentNumberService documentNumberService,
                               TenantApplicationRepository tenantApplicationRepository,
                               SegregationOfDutiesService segregationOfDutiesService,
                               AuditService auditService) {
        this.journalEntryRepository = journalEntryRepository;
        this.journalEntryLineRepository = journalEntryLineRepository;
        this.accountRepository = accountRepository;
        this.fiscalPeriodGuard = fiscalPeriodGuard;
        this.idempotencyService = idempotencyService;
        this.documentNumberService = documentNumberService;
        this.tenantApplicationRepository = tenantApplicationRepository;
        this.segregationOfDutiesService = segregationOfDutiesService;
        this.auditService = auditService;
    }

    @Transactional
    public AccountingApi.JournalEntryResponse create(AccountingApi.JournalEntryPayload payload, String username) {
        String appId = TenantContext.require();
        LocalDate entryDate = Instant.ofEpochMilli(payload.entryDate()).atZone(ZoneOffset.UTC).toLocalDate();
        fiscalPeriodGuard.requireAdjustment(entryDate);
        String entryNumber = resolveEntryNumber(appId, entryDate, payload.entryNumber());
        validateStructure(payload, appId, entryNumber);

        JournalEntry entry = new JournalEntry(entryNumber, entryDate, payload.description(),
                payload.reference(), payload.fiscalPeriodId());
        entry.assignCreator(username);
        entry.setCurrency(normalizeCurrency(payload.currency()));
        entry = journalEntryRepository.save(entry);

        for (var linePayload : payload.lines()) {
            JournalEntryLine line = new JournalEntryLine(entry.getId(), linePayload.accountId(),
                    linePayload.partyId(), linePayload.debit(), linePayload.credit(), linePayload.memo());
            journalEntryLineRepository.save(line);
        }
        return toResponse(entry);
    }

    public AccountingApi.NumberingSettings numberingSettings() {
        return new AccountingApi.NumberingSettings(automaticNumbering());
    }

    private String resolveEntryNumber(String appId, LocalDate entryDate, String requested) {
        if (automaticNumbering()) {
            return documentNumberService.next("JOURNAL_ENTRY", "JV", entryDate);
        }
        String value = requireManualEntryNumber(requested);
        if (journalEntryRepository.existsByAppIdAndEntryNumber(appId, value)) {
            throw new BusinessRuleException("رقم القيد مستخدم بالفعل في هذه الشركة.", "JOURNAL_NUMBER_DUPLICATE", HttpStatus.CONFLICT);
        }
        return value;
    }

    private String requireManualEntryNumber(String requested) {
        if (requested == null || requested.isBlank()) {
            throw new BusinessRuleException("رقم القيد مطلوب عند اختيار الترقيم اليدوي.", "JOURNAL_NUMBER_REQUIRED", HttpStatus.BAD_REQUEST);
        }
        return requested.strip();
    }

    private boolean automaticNumbering() {
        return tenantApplicationRepository.findById(TenantContext.require())
                .orElseThrow(() -> new BusinessRuleException("Application settings were not found.", "APP_SETTINGS_NOT_FOUND", HttpStatus.CONFLICT))
                .isAutomaticDocumentNumbering();
    }

    @Transactional
    public AccountingApi.JournalEntryResponse post(String id, AccountingApi.JournalActionRequest request, String username) {
        String requestHash = IdempotencyService.hash(id + "|POST|" + request.expectedVersion());
        return idempotencyService.execute("JOURNAL_POST", request.operationId(), requestHash,
                () -> postTransaction(id, request, username),
                entry -> entry.id(),
                this::replayEntry);
    }

    @Transactional
    public AccountingApi.JournalEntryResponse approve(String id, AccountingApi.JournalActionRequest request, String username) {
        JournalEntry entry = requireEntry(TenantContext.require(), id);
        requireVersion(entry, request.expectedVersion());
        segregationOfDutiesService.validateRequesterNotApprover(entry.getCreatedBy(), username, false);
        entry.approve(username);
        journalEntryRepository.save(entry);
        auditService.record("JOURNAL_APPROVED", "JOURNAL_ENTRY", entry.getId(), username,
                "{\"createdBy\":\"" + entry.getCreatedBy() + "\"}", null);
        return toResponse(entry);
    }

    @Transactional
    public AccountingApi.JournalEntryResponse reject(String id, AccountingApi.JournalActionRequest request, String username) {
        JournalEntry entry = requireEntry(TenantContext.require(), id);
        requireVersion(entry, request.expectedVersion());
        segregationOfDutiesService.validateRequesterNotApprover(entry.getCreatedBy(), username, false);
        if (request.reason() == null || request.reason().isBlank()) throw new BusinessRuleException(
                "A rejection reason is required.", "JOURNAL_REJECTION_REASON_REQUIRED", HttpStatus.BAD_REQUEST);
        entry.reject(username, request.reason());
        journalEntryRepository.save(entry);
        auditService.record("JOURNAL_REJECTED", "JOURNAL_ENTRY", entry.getId(), username,
                "{\"createdBy\":\"" + entry.getCreatedBy() + "\",\"reason\":\"" + request.reason().strip() + "\"}", null);
        return toResponse(entry);
    }

    private AccountingApi.JournalEntryResponse postTransaction(String id, AccountingApi.JournalActionRequest request, String username) {
        String appId = TenantContext.require();
        JournalEntry entry = requireEntry(appId, id);
        requireVersion(entry, request.expectedVersion());
        segregationOfDutiesService.validateCreatorNotPoster(entry.getCreatedBy(), username, "journal posting");
        segregationOfDutiesService.validateCreatorNotPoster(entry.getApprovedBy(), username, "journal posting");
        FiscalPeriod period = fiscalPeriodGuard.requireOpen(entry.getEntryDate());
        entry.attachFiscalPeriod(period.getId());
        entry.post(username);
        entry.setOperationId(request.operationId());
        journalEntryRepository.save(entry);
        auditService.record("JOURNAL_POSTED", "JOURNAL_ENTRY", entry.getId(), username,
                "{\"createdBy\":\"" + entry.getCreatedBy() + "\",\"postedBy\":\"" + username + "\"}", null);
        return toResponse(entry);
    }

    @Transactional
    public AccountingApi.JournalEntryResponse reverse(String id, AccountingApi.JournalActionRequest request, String username) {
        String requestHash = IdempotencyService.hash(id + "|REVERSE|" + request.expectedVersion() + "|" + request.reason());
        return idempotencyService.execute("JOURNAL_REVERSE", request.operationId(), requestHash,
                () -> reverseTransaction(id, request, username),
                entry -> entry.id(),
                this::replayEntry);
    }

    private AccountingApi.JournalEntryResponse reverseTransaction(String id, AccountingApi.JournalActionRequest request, String username) {
        String appId = TenantContext.require();
        JournalEntry entry = requireEntry(appId, id);
        requireVersion(entry, request.expectedVersion());
        if (entry.getStatus() != JournalEntry.Status.POSTED) {
            throw new BusinessRuleException(
                    "لا يمكن عكس قيد في حالة " + entry.getStatus() + ". العكس مسموح فقط للقيد المُرحَّل.",
                    "JOURNAL_STATE_INVALID", HttpStatus.CONFLICT);
        }
        fiscalPeriodGuard.requireOpen(entry.getEntryDate());

        List<JournalEntryLine> originalLines = journalEntryLineRepository.findByJournalEntryId(entry.getId());
        String reversalNumber = entry.getEntryNumber() + "-R";
        JournalEntry reversal = new JournalEntry(reversalNumber, entry.getEntryDate(),
                "عكس القيد " + entry.getEntryNumber()
                        + (request.reason() == null || request.reason().isBlank() ? "" : " — " + request.reason().strip()),
                entry.getReference(), entry.getFiscalPeriodId());
        reversal.setCurrency(entry.getCurrency());
        reversal.linkReversalOf(entry.getId(), request.operationId());
        reversal = journalEntryRepository.save(reversal);

        for (var originalLine : originalLines) {
            JournalEntryLine reversedLine = new JournalEntryLine(
                    reversal.getId(), originalLine.getAccountId(), originalLine.getPartyId(),
                    originalLine.getCredit(), originalLine.getDebit(), originalLine.getMemo());
            journalEntryLineRepository.save(reversedLine);
        }

        entry.markReversed(reversal.getId(), request.reason(), username, request.operationId());
        journalEntryRepository.save(entry);
        return toResponse(reversal);
    }

    private void validateStructure(AccountingApi.JournalEntryPayload payload, String appId, String entryNumber) {
        if (payload.lines() == null || payload.lines().size() < 2) {
            throw new BusinessRuleException("يجب أن يحتوي القيد على سطرين على الأقل.", "JOURNAL_INVALID", HttpStatus.BAD_REQUEST);
        }
        Set<String> accountIds = payload.lines().stream().map(AccountingApi.JournalEntryLinePayload::accountId).collect(java.util.stream.Collectors.toSet());
        List<Account> accounts = accountRepository.findAllById(accountIds);
        Set<String> found = accounts.stream().map(Account::getId).collect(java.util.stream.Collectors.toSet());
        if (found.size() != accountIds.size()) {
            throw new BusinessRuleException("أحد الحسابات في القيد غير موجود.", "ACCOUNT_NOT_FOUND", HttpStatus.BAD_REQUEST);
        }
        for (var account : accounts) {
            if (account.isHeader() || !account.isActive()) {
                throw new BusinessRuleException("لا يمكن الترحيل على حساب رئيسي أو غير نشط: " + account.getCode(),
                        "ACCOUNT_NOT_POSTING", HttpStatus.BAD_REQUEST);
            }
            String accountCurrency = account.getCurrency();
            String entryCurrency = normalizeCurrency(payload.currency());
            if (accountCurrency != null && !accountCurrency.isBlank()
                    && entryCurrency != null && !entryCurrency.equalsIgnoreCase(accountCurrency)) {
                throw new BusinessRuleException("عملة الحساب " + account.getCode() + " لا تطابق عملة القيد.",
                        "CURRENCY_MISMATCH", HttpStatus.BAD_REQUEST);
            }
        }

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (var line : payload.lines()) {
            BigDecimal debit = line.debit() == null ? BigDecimal.ZERO : line.debit();
            BigDecimal credit = line.credit() == null ? BigDecimal.ZERO : line.credit();
            if (debit.signum() < 0 || credit.signum() < 0) {
                throw new BusinessRuleException("لا يجوز أن تكون قيمة المدين أو الدائن سالبة.", "JOURNAL_INVALID", HttpStatus.BAD_REQUEST);
            }
            if (debit.signum() > 0 && credit.signum() > 0) {
                throw new BusinessRuleException("يجب أن يكون لكل سطر جانب واحد موجب فقط (مدين أو دائن).",
                        "JOURNAL_INVALID", HttpStatus.BAD_REQUEST);
            }
            if (debit.signum() == 0 && credit.signum() == 0) {
                throw new BusinessRuleException("كل سطر يجب أن يحتوي على قيمة مدين أو دائن.", "JOURNAL_INVALID", HttpStatus.BAD_REQUEST);
            }
            totalDebit = totalDebit.add(debit);
            totalCredit = totalCredit.add(credit);
        }
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BusinessRuleException("القيد غير متوازن! مجموع المدين (" + totalDebit + ") يجب أن يساوي مجموع الدائن (" + totalCredit + ")",
                    "JOURNAL_UNBALANCED", HttpStatus.BAD_REQUEST);
        }
    }

    private JournalEntry requireEntry(String appId, String id) {
        return journalEntryRepository.findById(id)
                .filter(entry -> entry.getAppId().equals(appId))
                .orElseThrow(() -> new BusinessRuleException("قيد اليومية غير موجود.", "JOURNAL_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private void requireVersion(JournalEntry entry, Long expectedVersion) {
        if (expectedVersion != null && expectedVersion != entry.getVersion()) {
            throw new BusinessRuleException("تم تعديل السجل بواسطة مستخدم آخر.", "RECORD_ALREADY_MODIFIED", HttpStatus.CONFLICT);
        }
    }

    @Transactional(readOnly = true)
    public Page<AccountingApi.JournalEntryResponse> listPage(Pageable pageable) {
        return journalEntryRepository.findAllByOrderByEntryDateDescCreatedAtDesc(pageable).map(this::toResponse);
    }

    private AccountingApi.JournalEntryResponse replayEntry(String entryId) {
        return toResponse(requireEntry(TenantContext.require(), entryId));
    }

    private String normalizeCurrency(String currency) {
        return currency == null || currency.isBlank() ? null : currency.strip().toUpperCase();
    }

    private AccountingApi.JournalEntryResponse toResponse(JournalEntry e) {
        var lines = journalEntryLineRepository.findByJournalEntryId(e.getId()).stream()
                .map(l -> new AccountingApi.JournalEntryLineResponse(l.getId(), l.getJournalEntryId(), l.getAccountId(),
                        l.getPartyId(), l.getDebit(), l.getCredit(), l.getMemo()))
                .toList();
        BigDecimal totalDebit = lines.stream().map(AccountingApi.JournalEntryLineResponse::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = lines.stream().map(AccountingApi.JournalEntryLineResponse::credit).reduce(BigDecimal.ZERO, BigDecimal::add);
        long entryDateMs = e.getEntryDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        return new AccountingApi.JournalEntryResponse(
                e.getId(), e.getEntryNumber(), entryDateMs, e.getDescription(), e.getReference(),
                e.getStatus().name(), e.getFiscalPeriodId(), e.getCurrency(),
                e.getPostedBy(), e.getPostedAt(),
                e.getReversalEntryId(), e.getReversedEntryId(), e.getReversalReason(), e.getReversedBy(), e.getReversedAt(),
                e.getOperationId(), e.getVersion(),
                lines, totalDebit, totalCredit, e.getCreatedAt(), e.getUpdatedAt());
    }
}
