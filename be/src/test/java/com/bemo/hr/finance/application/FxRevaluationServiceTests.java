package com.bemo.hr.finance.application;

import com.bemo.hr.finance.api.AccountingApi;
import com.bemo.hr.finance.domain.*;
import com.bemo.hr.finance.infrastructure.*;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FxRevaluationServiceTests {

    private static final String APP_ID = "app-1";
    private static final String USERNAME = "admin";

    @Mock
    private FxRevaluationPostRepository fxRevaluationPostRepository;
    @Mock
    private JournalEntryRepository journalEntryRepository;
    @Mock
    private JournalEntryLineRepository journalEntryLineRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private JournalEntryService journalEntryService;
    @Mock
    private FiscalPeriodGuard fiscalPeriodGuard;

    private FxRevaluationService service;

    @BeforeEach
    void setUp() {
        TenantContext.set(APP_ID);
        service = new FxRevaluationService(
                fxRevaluationPostRepository, journalEntryRepository,
                journalEntryLineRepository, accountRepository, currencyRepository,
                journalEntryService, fiscalPeriodGuard);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void idempotency_sameMonthSkipsDoublePost() {
        Account egpCash = mockAccount("acc-10101", "10101", "EGP Cash", Account.Type.ASSET, "EGP");
        lenient().when(accountRepository.findAllByOrderByCodeAsc()).thenReturn(List.of(egpCash));
        lenient().when(journalEntryRepository.findByStatusInOrderByEntryDateDesc(any())).thenReturn(List.of());
        lenient().when(currencyRepository.findAllByOrderByCodeAsc()).thenReturn(List.of());

        var result = service.runRevaluation(LocalDate.of(2026, 8, 31), USERNAME);

        assertThat(result.currenciesProcessed()).isEqualTo(0);
        assertThat(result.journalsPosted()).isEqualTo(0);
        verify(journalEntryService, never()).create(any(), anyString());
    }

    @Test
    void zeroBalanceCurrency_producesNoJournal() {
        Account usdBank = mockAccount("acc-usd", "1020", "USD Bank", Account.Type.ASSET, "USD");
        Account egpCash = mockAccount("acc-10101", "10101", "EGP Cash", Account.Type.ASSET, "EGP");
        lenient().when(accountRepository.findAllByOrderByCodeAsc()).thenReturn(List.of(usdBank, egpCash));
        lenient().when(currencyRepository.findAllByOrderByCodeAsc()).thenReturn(List.of());

        JournalEntry entry = mock(JournalEntry.class);
        when(entry.getId()).thenReturn("je-1");
        when(entry.getCurrency()).thenReturn("USD");

        JournalEntryLine line = mock(JournalEntryLine.class);
        when(line.getJournalEntryId()).thenReturn("je-1");
        when(line.getAccountId()).thenReturn("acc-usd");
        when(line.getDebit()).thenReturn(new BigDecimal("500"));
        when(line.getCredit()).thenReturn(new BigDecimal("500"));

        when(journalEntryRepository.findByStatusInOrderByEntryDateDesc(any())).thenReturn(List.of(entry));
        when(journalEntryLineRepository.findByJournalEntryIdIn(anySet())).thenReturn(List.of(line));

        var result = service.runRevaluation(LocalDate.of(2026, 8, 31), USERNAME);

        assertThat(result.currenciesProcessed()).isEqualTo(0);
        verify(journalEntryService, never()).create(any(), anyString());
    }

    @Test
    void gainScenario_rateMovedInFavor() {
        Account usdBank = mockAccount("acc-usd", "1020", "USD Bank", Account.Type.ASSET, "USD");
        Account egpCash = mockAccount("acc-10101", "10101", "EGP Cash", Account.Type.ASSET, "EGP");
        Account fxGainLoss = mockAccount("acc-4200", "4200", "FX Gain/Loss", Account.Type.REVENUE, "EGP");
        lenient().when(accountRepository.findAllByOrderByCodeAsc()).thenReturn(List.of(usdBank, egpCash, fxGainLoss));

        Currency usd = mock(Currency.class);
        when(usd.getCode()).thenReturn("USD");
        when(usd.getExchangeRate()).thenReturn(new BigDecimal("50.00"));
        lenient().when(currencyRepository.findAllByOrderByCodeAsc()).thenReturn(List.of(usd));

        JournalEntry entry = mock(JournalEntry.class);
        when(entry.getId()).thenReturn("je-1");
        when(entry.getCurrency()).thenReturn("USD");

        JournalEntryLine line = mock(JournalEntryLine.class);
        when(line.getJournalEntryId()).thenReturn("je-1");
        when(line.getAccountId()).thenReturn("acc-usd");
        when(line.getDebit()).thenReturn(new BigDecimal("10000"));
        when(line.getCredit()).thenReturn(new BigDecimal("0"));

        when(journalEntryRepository.findByStatusInOrderByEntryDateDesc(any())).thenReturn(List.of(entry));
        when(journalEntryLineRepository.findByJournalEntryIdIn(anySet())).thenReturn(List.of(line));
        when(fxRevaluationPostRepository.existsByCurrencyCodeAndYearMonth("USD", "2026-08")).thenReturn(false);

        AccountingApi.JournalEntryResponse journalResponse = mockJournalResponse("je-rev-1", "FX-REV-2026-08-USD");
        when(journalEntryService.create(any(), eq(USERNAME))).thenReturn(journalResponse);
        when(journalEntryService.post(eq("je-rev-1"), any(), eq(USERNAME))).thenReturn(journalResponse);

        var result = service.runRevaluation(LocalDate.of(2026, 8, 31), USERNAME);

        assertThat(result.currenciesProcessed()).isEqualTo(1);
        assertThat(result.journalsPosted()).isEqualTo(1);
        assertThat(result.results().get(0).currencyCode()).isEqualTo("USD");
        assertThat(result.results().get(0).unrealizedGainLoss()).isEqualByComparingTo(new BigDecimal("500000.00"));
        verify(fxRevaluationPostRepository).save(any());
    }

    @Test
    void lossScenario_rateMovedAgainst() {
        Account usdBank = mockAccount("acc-usd", "1020", "USD Bank", Account.Type.ASSET, "USD");
        Account egpCash = mockAccount("acc-10101", "10101", "EGP Cash", Account.Type.ASSET, "EGP");
        Account fxGainLoss = mockAccount("acc-4200", "4200", "FX Gain/Loss", Account.Type.REVENUE, "EGP");
        lenient().when(accountRepository.findAllByOrderByCodeAsc()).thenReturn(List.of(usdBank, egpCash, fxGainLoss));

        Currency usd = mock(Currency.class);
        when(usd.getCode()).thenReturn("USD");
        when(usd.getExchangeRate()).thenReturn(new BigDecimal("45.00"));
        lenient().when(currencyRepository.findAllByOrderByCodeAsc()).thenReturn(List.of(usd));

        JournalEntry entry = mock(JournalEntry.class);
        when(entry.getId()).thenReturn("je-1");
        when(entry.getCurrency()).thenReturn("USD");

        JournalEntryLine line = mock(JournalEntryLine.class);
        when(line.getJournalEntryId()).thenReturn("je-1");
        when(line.getAccountId()).thenReturn("acc-usd");
        when(line.getDebit()).thenReturn(new BigDecimal("0"));
        when(line.getCredit()).thenReturn(new BigDecimal("10000"));

        when(journalEntryRepository.findByStatusInOrderByEntryDateDesc(any())).thenReturn(List.of(entry));
        when(journalEntryLineRepository.findByJournalEntryIdIn(anySet())).thenReturn(List.of(line));
        when(fxRevaluationPostRepository.existsByCurrencyCodeAndYearMonth("USD", "2026-08")).thenReturn(false);

        AccountingApi.JournalEntryResponse journalResponse = mockJournalResponse("je-rev-2", "FX-REV-2026-08-USD");
        when(journalEntryService.create(any(), eq(USERNAME))).thenReturn(journalResponse);
        when(journalEntryService.post(eq("je-rev-2"), any(), eq(USERNAME))).thenReturn(journalResponse);

        var result = service.runRevaluation(LocalDate.of(2026, 8, 31), USERNAME);

        assertThat(result.currenciesProcessed()).isEqualTo(1);
        assertThat(result.journalsPosted()).isEqualTo(1);
        assertThat(result.results().get(0).unrealizedGainLoss()).isEqualByComparingTo(new BigDecimal("-450000.00"));
        verify(fxRevaluationPostRepository).save(any());
    }

    @Test
    void historyReturnsPosts() {
        FxRevaluationPost post = new FxRevaluationPost("USD", "2026-08",
                new BigDecimal("50000"), BigDecimal.ZERO, "je-1", USERNAME);
        when(fxRevaluationPostRepository.findAllByOrderByPostedAtDesc()).thenReturn(List.of(post));

        var history = service.getHistory();

        assertThat(history).hasSize(1);
        assertThat(history.get(0).currencyCode()).isEqualTo("USD");
        assertThat(history.get(0).yearMonth()).isEqualTo("2026-08");
    }

    private Account mockAccount(String id, String code, String name, Account.Type type, String currency) {
        Account account = mock(Account.class);
        lenient().when(account.getId()).thenReturn(id);
        lenient().when(account.getCode()).thenReturn(code);
        lenient().when(account.getName()).thenReturn(name);
        lenient().when(account.getType()).thenReturn(type);
        lenient().when(account.isHeader()).thenReturn(false);
        lenient().when(account.isActive()).thenReturn(true);
        lenient().when(account.getCurrency()).thenReturn(currency);
        return account;
    }

    private AccountingApi.JournalEntryResponse mockJournalResponse(String id, String entryNumber) {
        AccountingApi.JournalEntryResponse resp = mock(AccountingApi.JournalEntryResponse.class);
        lenient().when(resp.id()).thenReturn(id);
        lenient().when(resp.entryNumber()).thenReturn(entryNumber);
        return resp;
    }
}
