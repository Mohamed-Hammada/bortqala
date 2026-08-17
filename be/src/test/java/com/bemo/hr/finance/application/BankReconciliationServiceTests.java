package com.bemo.hr.finance.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.finance.api.BankReconciliationApi;
import com.bemo.hr.finance.domain.*;
import com.bemo.hr.finance.infrastructure.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.numbering.DocumentNumberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankReconciliationServiceTests {
    @Mock
    BankAccountRepository bankAccountRepository;
    @Mock
    BankStatementRepository statementRepository;
    @Mock
    BankStatementLineRepository lineRepository;
    @Mock
    BankReconciliationMatchRepository matchRepository;
    @Mock
    JournalEntryRepository journalRepository;
    @Mock
    JournalEntryLineRepository journalLineRepository;
    @Mock
    AccountRepository accountRepository;
    @Mock
    FiscalPeriodGuard fiscalPeriodGuard;
    @Mock
    DocumentNumberService documentNumberService;
    @Mock
    AuditService auditService;
    BankReconciliationService service;
    BankAccount bank;
    BankStatement statement;
    BankStatementLine line;

    @BeforeEach
    void setUp() {
        service = new BankReconciliationService(bankAccountRepository, statementRepository, lineRepository,
                matchRepository, journalRepository, journalLineRepository, accountRepository, fiscalPeriodGuard,
                documentNumberService, auditService);
        bank = new BankAccount("Bank", "123", null, null, "gl-bank", "EGP", true);
        statement = new BankStatement(bank.getId(), "ST-1", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
                new BigDecimal("1000"), new BigDecimal("895"), "EGP", "statement.csv", "hash", "tester");
        line = new BankStatementLine(statement.getId(), 1, LocalDate.of(2026, 8, 10), null,
                "Supplier transfer and fee", "PMT-1", new BigDecimal("-105"), new BigDecimal("895"), "fp");
        lenient().when(bankAccountRepository.findById(bank.getId())).thenReturn(Optional.of(bank));
        lenient().when(statementRepository.findById(statement.getId())).thenReturn(Optional.of(statement));
        lenient().when(statementRepository.findByIdForUpdate(statement.getId())).thenReturn(Optional.of(statement));
        lenient().when(lineRepository.findByStatementIdOrderByLineNumberAsc(statement.getId())).thenReturn(List.of(line));
        lenient().when(lineRepository.findByIdForUpdate(line.getId())).thenReturn(Optional.of(line));
        lenient().when(matchRepository.findByStatementLineIdOrderByMatchedAtAsc(line.getId())).thenReturn(List.of());
        lenient().when(matchRepository.findByOperationId(anyString())).thenReturn(List.of());
        lenient().when(matchRepository.findByJournalEntryIdAndStatus(anyString(), eq(BankReconciliationMatch.Status.ACTIVE))).thenReturn(List.of());
        lenient().when(matchRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(journalRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(journalLineRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(fiscalPeriodGuard.requireOpen(any())).thenReturn(openPeriod());
        lenient().when(fiscalPeriodGuard.requireAdjustment(any())).thenReturn(openPeriod());
    }

    @Test
    void exactAutoMatchLinksTheOnlyPostedJournal() {
        line = new BankStatementLine(statement.getId(), 1, LocalDate.of(2026, 8, 10), null,
                "Supplier transfer", "PMT-1", new BigDecimal("-100"), new BigDecimal("900"), "fp2");
        when(lineRepository.findByStatementIdOrderByLineNumberAsc(statement.getId())).thenReturn(List.of(line));
        JournalEntry entry = postedJournal("JV-1", "PMT-1");
        when(journalRepository.findByStatusOrderByEntryDateDesc(JournalEntry.Status.POSTED)).thenReturn(List.of(entry));
        when(journalLineRepository.findByJournalEntryId(entry.getId())).thenReturn(List.of(
                new JournalEntryLine(entry.getId(), "gl-bank", null, BigDecimal.ZERO, new BigDecimal("100"), null)));
        when(lineRepository.countByStatementIdAndStatusIn(eq(statement.getId()), anyList())).thenReturn(0L);

        service.autoMatch(statement.getId(), new BankReconciliationApi.OperationRequest("auto-1"));

        assertThat(line.getStatus()).isEqualTo(BankStatementLine.Status.MATCHED);
        ArgumentCaptor<BankReconciliationMatch> captor = ArgumentCaptor.forClass(BankReconciliationMatch.class);
        verify(matchRepository).save(captor.capture());
        assertThat(captor.getValue().getMatchType()).isEqualTo(BankReconciliationMatch.Type.EXACT);
    }

    @Test
    void partialPaymentPlusFeeCreatesBalancedFeeJournal() {
        JournalEntry payment = postedJournal("JV-PAY", "PMT-1");
        when(journalRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(journalLineRepository.findByJournalEntryId(payment.getId())).thenReturn(List.of(
                new JournalEntryLine(payment.getId(), "gl-bank", null, BigDecimal.ZERO, new BigDecimal("100"), null)));
        Account feeAccount = new Account("6100", "Bank fees", Account.Type.EXPENSE, null, false, "EGP", true);
        when(accountRepository.findById(feeAccount.getId())).thenReturn(Optional.of(feeAccount));
        when(documentNumberService.next("JOURNAL_ENTRY", "JV", line.getTransactionDate())).thenReturn("JV-FEE-1");
        when(lineRepository.countByStatementIdAndStatusIn(eq(statement.getId()), anyList())).thenReturn(0L);

        service.match(statement.getId(), line.getId(), new BankReconciliationApi.MatchRequest("match-1",
                List.of(new BankReconciliationApi.Allocation(payment.getId(), new BigDecimal("100"))),
                new BigDecimal("5"), feeAccount.getId()));

        assertThat(line.getStatus()).isEqualTo(BankStatementLine.Status.MATCHED);
        ArgumentCaptor<JournalEntryLine> lines = ArgumentCaptor.forClass(JournalEntryLine.class);
        verify(journalLineRepository, times(2)).save(lines.capture());
        assertThat(lines.getAllValues()).extracting(JournalEntryLine::getDebit, JournalEntryLine::getCredit)
                .containsExactlyInAnyOrder(tuple(new BigDecimal("5"), BigDecimal.ZERO), tuple(BigDecimal.ZERO, new BigDecimal("5")));
    }

    @Test
    void closedFiscalPeriodBlocksMatching() {
        when(fiscalPeriodGuard.requireOpen(line.getTransactionDate()))
                .thenThrow(new BusinessRuleException("closed", "FISCAL_PERIOD_CLOSED", org.springframework.http.HttpStatus.CONFLICT));

        assertThatThrownBy(() -> service.match(statement.getId(), line.getId(),
                new BankReconciliationApi.MatchRequest("match-closed", List.of(), BigDecimal.ZERO, null)))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode()).isEqualTo("FISCAL_PERIOD_CLOSED");
    }

    @Test
    void reversalReopensMatchedLineWithoutReversingSourceJournal() {
        JournalEntry entry = postedJournal("JV-1", "PMT-1");
        line.addMatch(new BigDecimal("105"));
        BankReconciliationMatch match = new BankReconciliationMatch(line.getId(), entry.getId(), new BigDecimal("105"),
                BankReconciliationMatch.Type.EXACT, "match-1", "tester");
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(lineRepository.countByStatementIdAndStatusIn(eq(statement.getId()), anyList())).thenReturn(1L);

        service.reverse(statement.getId(), match.getId(), new BankReconciliationApi.ReverseRequest("rev-1", "Wrong match"));

        assertThat(match.getStatus()).isEqualTo(BankReconciliationMatch.Status.REVERSED);
        assertThat(line.getStatus()).isEqualTo(BankStatementLine.Status.UNMATCHED);
        verify(journalRepository, never()).save(entry);
    }

    @Test
    void duplicateStatementFileIsRejectedIdempotently() {
        byte[] csv = "date,description,amount\n2026-08-10,Payment,-105\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "statement.csv", "text/csv", csv);
        when(statementRepository.findByBankAccountIdAndFileHash(eq(bank.getId()), anyString())).thenReturn(Optional.of(statement));

        assertThatThrownBy(() -> service.importCsv(bank.getId(), "ST-1", new BigDecimal("1000"), new BigDecimal("895"), file))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode()).isEqualTo("BANK_STATEMENT_DUPLICATE");
    }

    @Test
    void inconsistentClosingBalanceIsRejectedBeforePersistence() {
        byte[] csv = "date,description,reference,amount,balance\n2026-08-10,Payment,PMT-1,-105,895\n"
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "statement.csv", "text/csv", csv);
        when(statementRepository.findByBankAccountIdAndFileHash(eq(bank.getId()), anyString())).thenReturn(Optional.empty());
        when(statementRepository.existsByBankAccountIdAndStatementReference(bank.getId(), "ST-2")).thenReturn(false);

        assertThatThrownBy(() -> service.importCsv(bank.getId(), "ST-2", new BigDecimal("1000"), new BigDecimal("900"), file))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode()).isEqualTo("BANK_STATEMENT_BALANCE_MISMATCH");
        verify(statementRepository, never()).save(any());
    }

    @Test
    void cashPositionKeepsCurrencyTotalsSeparate() {
        BankAccount usdBank = new BankAccount("USD Bank", "456", null, null, "gl-usd", "USD", true);
        BankStatement usdStatement = new BankStatement(usdBank.getId(), "USD-1", LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31), BigDecimal.ZERO, new BigDecimal("100"), "USD", "usd.csv", "usd-hash", "tester");
        when(bankAccountRepository.findAllByOrderByBankNameAsc()).thenReturn(List.of(bank, usdBank));
        when(statementRepository.findFirstByBankAccountIdOrderByPeriodEndDesc(bank.getId())).thenReturn(Optional.of(statement));
        when(statementRepository.findFirstByBankAccountIdOrderByPeriodEndDesc(usdBank.getId())).thenReturn(Optional.of(usdStatement));
        when(lineRepository.countByStatementIdAndStatusIn(anyString(), anyList())).thenReturn(0L);

        var result = service.cashPosition();

        assertThat(result.totalsByCurrency()).containsEntry("EGP", new BigDecimal("895"))
                .containsEntry("USD", new BigDecimal("100"));
    }

    private JournalEntry postedJournal(String number, String reference) {
        JournalEntry entry = new JournalEntry(number, LocalDate.of(2026, 8, 10), "Payment", reference, "period-1");
        entry.approve("approver");
        entry.post("tester");
        return entry;
    }

    private FiscalPeriod openPeriod() {
        return new FiscalPeriod(2026, 8, "August", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), FiscalPeriod.Status.OPEN);
    }
}
