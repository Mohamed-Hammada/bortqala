package com.bemo.hr.finance.application;

import com.bemo.hr.approval.SegregationOfDutiesService;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.finance.api.AccountingApi;
import com.bemo.hr.finance.domain.Account;
import com.bemo.hr.finance.domain.FiscalPeriodGuard;
import com.bemo.hr.finance.domain.JournalEntry;
import com.bemo.hr.finance.infrastructure.AccountRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryLineRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.idempotency.application.IdempotencyService;
import com.bemo.hr.shared.numbering.DocumentNumberService;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JournalEntryServiceTests {

    private static final String APP_ID = "app-1";

    @Mock
    private JournalEntryRepository journalEntryRepository;
    @Mock
    private JournalEntryLineRepository journalEntryLineRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private FiscalPeriodGuard fiscalPeriodGuard;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private DocumentNumberService documentNumberService;
    @Mock
    private TenantApplicationRepository tenantApplicationRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private JournalApprovalService journalApprovalService;

    private TenantApplication app;
    private JournalEntryService service;
    private Account debitAccount;
    private Account creditAccount;

    @BeforeEach
    void setUp() {
        app = new TenantApplication("TEST", "Test App");
        service = new JournalEntryService(journalEntryRepository, journalEntryLineRepository,
                accountRepository, fiscalPeriodGuard, idempotencyService, documentNumberService,
                tenantApplicationRepository, new SegregationOfDutiesService(), auditService,
                mock(com.bemo.hr.finance.infrastructure.JournalDimensionRepository.class), journalApprovalService);
        TenantContext.set(APP_ID);
        lenient().when(tenantApplicationRepository.findById(APP_ID)).thenReturn(Optional.of(app));

        debitAccount = mock(Account.class);
        lenient().when(debitAccount.getId()).thenReturn("acc-dr");
        lenient().when(debitAccount.getCode()).thenReturn("1000");
        lenient().when(debitAccount.isHeader()).thenReturn(false);
        lenient().when(debitAccount.isActive()).thenReturn(true);
        creditAccount = mock(Account.class);
        lenient().when(creditAccount.getId()).thenReturn("acc-cr");
        lenient().when(creditAccount.getCode()).thenReturn("2000");
        lenient().when(creditAccount.isHeader()).thenReturn(false);
        lenient().when(creditAccount.isActive()).thenReturn(true);
        lenient().when(accountRepository.findAllById(anySet())).thenReturn(List.of(debitAccount, creditAccount));
        lenient().when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(journalApprovalService.isApprovalRequired(any(java.util.Map.class))).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private AccountingApi.JournalEntryPayload payload(String entryNumber) {
        long entryDate = LocalDate.of(2026, 8, 6).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        return new AccountingApi.JournalEntryPayload(entryNumber, entryDate, "Test entry", null, null, "EGP",
                List.of(
                        new AccountingApi.JournalEntryLinePayload("acc-dr", null, new java.math.BigDecimal("100.00"),
                                new java.math.BigDecimal("0.00"), null, null, null, null),
                        new AccountingApi.JournalEntryLinePayload("acc-cr", null, new java.math.BigDecimal("0.00"),
                                new java.math.BigDecimal("100.00"), null, null, null, null)));
    }

    @Test
    void autoGeneratesEntryNumberWhenAutomaticNumberingEnabled() {
        app.updateDocumentNumbering(true);
        when(documentNumberService.next("JOURNAL_ENTRY", "JV", LocalDate.of(2026, 8, 6)))
                .thenReturn("JV-2026-00001");

        AccountingApi.JournalEntryResponse response = service.create(payload(null), "admin");

        assertThat(response.entryNumber()).isEqualTo("JV-2026-00001");
        verify(documentNumberService).next("JOURNAL_ENTRY", "JV", LocalDate.of(2026, 8, 6));
    }

    @Test
    void manualModeUsesProvidedEntryNumber() {
        app.updateDocumentNumbering(false);
        when(journalEntryRepository.existsByAppIdAndEntryNumber(APP_ID, "JV-1001")).thenReturn(false);

        AccountingApi.JournalEntryResponse response = service.create(payload("JV-1001"), "admin");

        assertThat(response.entryNumber()).isEqualTo("JV-1001");
        verify(documentNumberService, never()).next(any(), any(), any());
    }

    @Test
    void manualModeRequiresEntryNumberWhenOmitted() {
        app.updateDocumentNumbering(false);

        assertThatThrownBy(() -> service.create(payload(null), "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("رقم القيد مطلوب");
    }

    @Test
    void manualModeRejectsDuplicateEntryNumber() {
        app.updateDocumentNumbering(false);
        when(journalEntryRepository.existsByAppIdAndEntryNumber(APP_ID, "JV-1001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(payload("JV-1001"), "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("مستخدم بالفعل");
    }

    @Test
    void configuredRuleCanAutoApproveAtCreationWhileMissingRulesRemainManual() {
        app.updateDocumentNumbering(false);
        when(journalEntryRepository.existsByAppIdAndEntryNumber(APP_ID, "JV-1002")).thenReturn(false);
        when(journalApprovalService.isApprovalRequired(any(java.util.Map.class))).thenReturn(false);

        AccountingApi.JournalEntryResponse response = service.create(payload("JV-1002"), "maker");

        assertThat(response.status()).isEqualTo("APPROVED");
        verify(auditService).record(org.mockito.ArgumentMatchers.eq("JOURNAL_AUTO_APPROVED"),
                org.mockito.ArgumentMatchers.eq("JOURNAL_ENTRY"), any(),
                org.mockito.ArgumentMatchers.eq("maker"), any(), org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    void manualJournalCreatorCannotPostThroughDirectCommandPath() {
        JournalEntry entry = new JournalEntry("JV-1", LocalDate.of(2026, 8, 6), "Manual", null, null);
        entry.assignCreator("maker");
        org.springframework.test.util.ReflectionTestUtils.setField(entry, "appId", APP_ID);
        when(journalEntryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));
        when(idempotencyService.execute(any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
            java.util.function.Supplier<?> supplier = inv.getArgument(3);
            return supplier.get();
        });
        var request = new AccountingApi.JournalActionRequest(java.util.UUID.randomUUID().toString(), 0L, null);

        assertThatThrownBy(() -> service.post(entry.getId(), request, "maker"))
                .isInstanceOf(com.bemo.hr.approval.SegregationOfDutiesViolationException.class);
        verify(fiscalPeriodGuard, never()).requireOpen(any());
    }

    @Test
    void approvalRequiresDifferentCheckerAndPostingRequiresThirdActor() {
        JournalEntry entry = new JournalEntry("JV-2", LocalDate.of(2026, 8, 6), "Manual", null, null);
        entry.assignCreator("maker");
        org.springframework.test.util.ReflectionTestUtils.setField(entry, "appId", APP_ID);
        when(journalEntryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));
        var approve = new AccountingApi.JournalActionRequest(java.util.UUID.randomUUID().toString(), 0L, null);

        service.approve(entry.getId(), approve, "checker");
        assertThat(entry.getStatus()).isEqualTo(JournalEntry.Status.APPROVED);
        when(idempotencyService.execute(any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
            java.util.function.Supplier<?> supplier = inv.getArgument(3);
            return supplier.get();
        });
        assertThatThrownBy(() -> service.post(entry.getId(), approve, "checker"))
                .isInstanceOf(com.bemo.hr.approval.SegregationOfDutiesViolationException.class);
    }
}
