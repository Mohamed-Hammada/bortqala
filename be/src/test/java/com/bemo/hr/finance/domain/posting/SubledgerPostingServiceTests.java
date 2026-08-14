package com.bemo.hr.finance.domain.posting;

import com.bemo.hr.finance.domain.JournalEntry;
import com.bemo.hr.finance.domain.JournalEntryLine;
import com.bemo.hr.finance.infrastructure.JournalEntryLineRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.finance.domain.FiscalPeriod;
import com.bemo.hr.finance.domain.FiscalPeriodGuard;
import com.bemo.hr.audit.application.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SubledgerPostingServiceTests {

    private PostingProfileRepository postingProfileRepository;
    private PostingProfileLineRepository postingProfileLineRepository;
    private JournalEntryRepository journalEntryRepository;
    private JournalEntryLineRepository journalEntryLineRepository;
    private SubledgerPostingService subledgerPostingService;
    private FiscalPeriodGuard fiscalPeriodGuard;
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        postingProfileRepository = mock(PostingProfileRepository.class);
        postingProfileLineRepository = mock(PostingProfileLineRepository.class);
        journalEntryRepository = mock(JournalEntryRepository.class);
        journalEntryLineRepository = mock(JournalEntryLineRepository.class);
        fiscalPeriodGuard = mock(FiscalPeriodGuard.class);
        auditService = mock(AuditService.class);
        FiscalPeriod period = new FiscalPeriod(2026, 2, "Feb", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), FiscalPeriod.Status.OPEN);
        when(fiscalPeriodGuard.requireOpen(any())).thenReturn(period);
        subledgerPostingService = new SubledgerPostingService(postingProfileRepository, postingProfileLineRepository,
                journalEntryRepository, journalEntryLineRepository,
                fiscalPeriodGuard, auditService, mock(com.bemo.hr.finance.infrastructure.JournalSourceMetadataRepository.class));
    }

    @Test
    void postsBalancedSubledgerEventSuccessfully() {
        String opId = UUID.randomUUID().toString();
        PostingProfile profile = new PostingProfile("P01", "CONTRACTOR_SETTLEMENT_POST", LocalDate.of(2026, 1, 1), null);
        when(postingProfileRepository.findByBusinessEventAndActiveTrueOrderByEffectiveFromDesc("CONTRACTOR_SETTLEMENT_POST"))
                .thenReturn(List.of(profile));
        when(postingProfileLineRepository.findByProfileIdOrderByLineNoAsc(profile.getId())).thenReturn(List.of(
                new PostingProfileLine(profile.getId(), 1, "DEBIT", "FIXED", "DR", "AMOUNT"),
                new PostingProfileLine(profile.getId(), 2, "CREDIT", "FIXED", "CR", "AMOUNT")));

        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        JournalEntry entry = subledgerPostingService.postSubledgerEvent(
                "WORKFORCE",
                "SETTLEMENT",
                "SETTLE-100",
                "CONTRACTOR_SETTLEMENT_POST",
                opId,
                LocalDate.of(2026, 2, 1),
                "Contractor Settlement Posting",
                new BigDecimal("5000.00"),
                new BigDecimal("5000.00"),
                null
        );

        assertThat(entry).isNotNull();
        assertThat(entry.getStatus()).isEqualTo(JournalEntry.Status.POSTED);
        assertThat(entry.getOperationId()).isEqualTo(opId);
        assertThat(entry.getReference()).isEqualTo("WORKFORCE:SETTLEMENT:SETTLE-100");
        verify(journalEntryLineRepository, times(2)).save(any(JournalEntryLine.class));
        verify(fiscalPeriodGuard).requireOpen(LocalDate.of(2026, 2, 1));
        verify(auditService).record(eq("SUBLEDGER_POSTED"), eq("JOURNAL_ENTRY"), eq(entry.getId()), eq("SYSTEM"), anyString(), isNull());
    }

    @Test
    void rejectsUnbalancedSubledgerPosting() {
        String opId = UUID.randomUUID().toString();

        assertThatThrownBy(() -> subledgerPostingService.postSubledgerEvent(
                "WORKFORCE",
                "SETTLEMENT",
                "SETTLE-100",
                "CONTRACTOR_SETTLEMENT_POST",
                opId,
                LocalDate.of(2026, 2, 1),
                "Contractor Settlement Posting",
                new BigDecimal("5000.00"),
                new BigDecimal("4500.00"), // Unbalanced
                "fp-1"
        )).isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("Debit and credit amounts must be equal");
    }

    @Test
    void missingOrIncompletePostingProfileFailsClosed() {
        when(postingProfileRepository.findByBusinessEventAndActiveTrueOrderByEffectiveFromDesc("CUSTOMER_INVOICE_ISSUED"))
                .thenReturn(List.of());
        assertThatThrownBy(() -> subledgerPostingService.postSubledgerEvent(
                "SALES", "CUSTOMER_INVOICE", "invoice-1", "CUSTOMER_INVOICE_ISSUED", "op-1",
                LocalDate.of(2026, 2, 1), "Invoice", new BigDecimal("100"), new BigDecimal("100"), null))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        error -> assertThat(error.getCode()).isEqualTo("SUBLEDGER_POSTING_PROFILE_REQUIRED"));
    }

    @Test
    void profileEventPostsGrossAgainstNetAndDeductions() {
        PostingProfile profile = new PostingProfile("PAYROLL", "PAYROLL_ACCRUAL", LocalDate.of(2026, 1, 1), null);
        when(postingProfileRepository.findByBusinessEventAndActiveTrueOrderByEffectiveFromDesc("PAYROLL_ACCRUAL"))
                .thenReturn(List.of(profile));
        when(postingProfileLineRepository.findByProfileIdOrderByLineNoAsc(profile.getId())).thenReturn(List.of(
                new PostingProfileLine(profile.getId(), 1, "DEBIT", "FIXED", "EXPENSE", "TOTAL_GROSS"),
                new PostingProfileLine(profile.getId(), 2, "CREDIT", "FIXED", "PAYABLE", "TOTAL_NET"),
                new PostingProfileLine(profile.getId(), 3, "CREDIT", "FIXED", "DEDUCTIONS", "TOTAL_DEDUCTIONS")));
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        JournalEntry entry = subledgerPostingService.postProfileEvent("PAYROLL", "PAYROLL_RUN", "run-1",
                "PAYROLL_ACCRUAL", "PAYROLL-ACCRUAL:run-1", LocalDate.of(2026, 2, 28), "Payroll accrual",
                java.util.Map.of("TOTAL_GROSS", new BigDecimal("120"), "TOTAL_NET", new BigDecimal("100"),
                        "TOTAL_DEDUCTIONS", new BigDecimal("20")), null, null, null, "payroll-admin");

        var lineCaptor = org.mockito.ArgumentCaptor.forClass(JournalEntryLine.class);
        verify(journalEntryLineRepository, times(3)).save(lineCaptor.capture());
        assertThat(lineCaptor.getAllValues().stream().map(JournalEntryLine::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("120");
        assertThat(lineCaptor.getAllValues().stream().map(JournalEntryLine::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("120");
        assertThat(entry.getStatus()).isEqualTo(JournalEntry.Status.POSTED);
        assertThat(entry.getPostedBy()).isEqualTo("payroll-admin");
    }

    @Test
    void replaysOperationAndCreatesLinkedBalancedReversal() {
        String opId = UUID.randomUUID().toString();
        JournalEntry original = new JournalEntry("POST-1", LocalDate.of(2026, 2, 1), "Posting", "AP:INVOICE:I-1", null);
        original.setCurrency("USD"); original.approve("approver"); original.post("SYSTEM");
        when(journalEntryRepository.findById(original.getId())).thenReturn(java.util.Optional.of(original));
        when(journalEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(journalEntryLineRepository.findByJournalEntryId(original.getId())).thenReturn(List.of(
                new JournalEntryLine(original.getId(), "DR", null, new BigDecimal("100"), BigDecimal.ZERO, "DR"),
                new JournalEntryLine(original.getId(), "CR", null, BigDecimal.ZERO, new BigDecimal("100"), "CR")));

        JournalEntry reversal = subledgerPostingService.reverse(original.getId(), opId, LocalDate.of(2026, 2, 2), "Correction", "checker");
        when(journalEntryRepository.findByOperationId(opId)).thenReturn(java.util.Optional.of(reversal));
        JournalEntry replay = subledgerPostingService.reverse(original.getId(), opId, LocalDate.of(2026, 2, 2), "Correction", "checker");

        assertThat(reversal.getReversedEntryId()).isEqualTo(original.getId());
        assertThat(reversal.getCreatedBy()).isEqualTo("checker");
        assertThat(reversal.getApprovedBy()).isEqualTo("checker");
        assertThat(reversal.getPostedBy()).isEqualTo("checker");
        assertThat(reversal.getPostedAt()).isNotNull();
        assertThat(original.getReversalEntryId()).isEqualTo(reversal.getId());
        assertThat(replay).isSameAs(reversal);
        verify(journalEntryLineRepository, times(2)).save(any(JournalEntryLine.class));
    }
}
