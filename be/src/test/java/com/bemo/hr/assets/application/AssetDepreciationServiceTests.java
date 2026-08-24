package com.bemo.hr.assets.application;

import com.bemo.hr.assets.api.AssetsApi;
import com.bemo.hr.assets.domain.FixedAsset;
import com.bemo.hr.assets.infrastructure.FixedAssetDepreciationPostRepository;
import com.bemo.hr.assets.infrastructure.FixedAssetRepository;
import com.bemo.hr.finance.api.AccountingApi;
import com.bemo.hr.finance.application.JournalEntryService;
import com.bemo.hr.finance.domain.Account;
import com.bemo.hr.finance.domain.FiscalPeriodGuard;
import com.bemo.hr.finance.domain.JournalEntry;
import com.bemo.hr.finance.infrastructure.AccountRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssetDepreciationServiceTests {

    private static final String APP_MONTH = "2026-02";

    private FixedAssetRepository assetRepository;
    private FixedAssetDepreciationPostRepository postRepository;
    private JournalEntryService journalEntryService;
    private JournalEntryRepository journalEntryRepository;
    private AccountRepository accountRepository;
    private FiscalPeriodGuard fiscalPeriodGuard;
    private AuditService auditService;
    private AssetDepreciationService service;

    @BeforeEach
    void setUp() {
        assetRepository = mock(FixedAssetRepository.class);
        postRepository = mock(FixedAssetDepreciationPostRepository.class);
        journalEntryService = mock(JournalEntryService.class);
        journalEntryRepository = mock(JournalEntryRepository.class);
        accountRepository = mock(AccountRepository.class);
        fiscalPeriodGuard = mock(FiscalPeriodGuard.class);
        auditService = mock(AuditService.class);
        service = new AssetDepreciationService(assetRepository, postRepository, journalEntryService,
                journalEntryRepository, accountRepository, fiscalPeriodGuard, auditService,
                "5300", "1280", "1300", "10101", "5310", "4100");
    }

    private static Account account(String id, String code) {
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(id);
        when(account.getCode()).thenReturn(code);
        return account;
    }

    private static FixedAsset twelveMonthVan() {
        long acquisition = LocalDate.of(2026, 1, 15).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        return new FixedAsset("Delivery van", FixedAsset.Category.VEHICLE, acquisition,
                new BigDecimal("12000.00"), BigDecimal.ZERO, 12, null, null);
    }

    private void givenPostedJournal(String entryId, String entryNumber) {
        when(journalEntryService.create(any(), anyString())).thenReturn(new AccountingApi.JournalEntryResponse(
                entryId, entryNumber, 0L, "desc", "ref", "DRAFT", null, null, null, null,
                null, null, null, null, null, null, 0L, List.of(), BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L));
        when(journalEntryRepository.findById(entryId)).thenReturn(Optional.of(
                new JournalEntry(entryNumber, LocalDate.of(2026, 2, 28), "desc", "ref", null)));
    }

    private List<AccountingApi.JournalEntryLinePayload> capturedLines() {
        ArgumentCaptor<AccountingApi.JournalEntryPayload> captor =
                ArgumentCaptor.forClass(AccountingApi.JournalEntryPayload.class);
        verify(journalEntryService).create(captor.capture(), eq("finance1"));
        return captor.getValue().lines();
    }

    @Test
    void straightLineRunPostsOneBalancedApprovedJournalPerAsset() {
        FixedAsset van = twelveMonthVan();
        when(assetRepository.findByStatusInOrderByAcquisitionDateAsc(List.of("ACTIVE"))).thenReturn(List.of(van));
        Account expense = account("acc-exp", "5300");
        Account accumulated = account("acc-acc", "1280");
        when(accountRepository.findAll()).thenReturn(List.of(expense, accumulated));
        when(postRepository.existsByAssetIdAndYearMonth(any(), anyString())).thenReturn(false);
        givenPostedJournal("je-1", "JV-100");

        AssetsApi.DepreciationRunResponse response = service.runDepreciation(APP_MONTH, "finance1");

        assertThat(response.postedCount()).isEqualTo(1);
        assertThat(response.yearMonth()).isEqualTo(APP_MONTH);
        assertThat(response.totalCharge()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(response.results().get(0).outcome()).isEqualTo("POSTED");
        assertThat(response.results().get(0).entryNumber()).isEqualTo("JV-100");

        List<AccountingApi.JournalEntryLinePayload> lines = capturedLines();
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).accountId()).isEqualTo("acc-exp");
        assertThat(lines.get(0).debit()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(lines.get(1).accountId()).isEqualTo("acc-acc");
        assertThat(lines.get(1).credit()).isEqualByComparingTo(new BigDecimal("1000.00"));
        BigDecimal debits = lines.stream().map(line -> line.debit() == null ? BigDecimal.ZERO : line.debit())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credits = lines.stream().map(line -> line.credit() == null ? BigDecimal.ZERO : line.credit())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(debits).isEqualByComparingTo(credits);

        ArgumentCaptor<AccountingApi.JournalEntryPayload> payloadCaptor =
                ArgumentCaptor.forClass(AccountingApi.JournalEntryPayload.class);
        verify(journalEntryService).create(payloadCaptor.capture(), eq("finance1"));
        assertThat(payloadCaptor.getValue().reference()).isEqualTo("ASSET-DEP-2026-02");
        assertThat(payloadCaptor.getValue().lines()).hasSize(2);

        assertThat(van.getAccumulatedDepreciation()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(van.getLastPostedYearMonth()).isEqualTo(APP_MONTH);
        verify(auditService).record(eq("CREATE"), eq("FIXED_ASSET_DEPRECIATION"), eq(van.getId()),
                eq("finance1"), any(), any());
    }

    @Test
    void rerunningTheSameMonthIsExactlyOnce() {
        FixedAsset van = twelveMonthVan();
        when(assetRepository.findByStatusInOrderByAcquisitionDateAsc(List.of("ACTIVE"))).thenReturn(List.of(van));
        Account expense = account("acc-exp", "5300");
        Account accumulated = account("acc-acc", "1280");
        when(accountRepository.findAll()).thenReturn(List.of(expense, accumulated));
        when(postRepository.existsByAssetIdAndYearMonth(any(), anyString())).thenReturn(true);

        AssetsApi.DepreciationRunResponse response = service.runDepreciation(APP_MONTH, "finance1");

        assertThat(response.postedCount()).isZero();
        assertThat(response.results().get(0).outcome()).isEqualTo("ALREADY_POSTED");
        verify(journalEntryService, never()).create(any(), any());
        assertThat(van.getAccumulatedDepreciation()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void lockedFiscalPeriodSkipsTheAssetWithoutFailingTheRun() {
        FixedAsset van = twelveMonthVan();
        when(assetRepository.findByStatusInOrderByAcquisitionDateAsc(List.of("ACTIVE"))).thenReturn(List.of(van));
        Account expense = account("acc-exp", "5300");
        Account accumulated = account("acc-acc", "1280");
        when(accountRepository.findAll()).thenReturn(List.of(expense, accumulated));
        when(fiscalPeriodGuard.requireOpen(any())).thenThrow(new BusinessRuleException(
                "Fiscal period is closed.", "FISCAL_PERIOD_CLOSED",
                org.springframework.http.HttpStatus.CONFLICT));

        AssetsApi.DepreciationRunResponse response = service.runDepreciation(APP_MONTH, "finance1");

        assertThat(response.postedCount()).isZero();
        assertThat(response.results().get(0).outcome()).isEqualTo("DEPRECIATION_PERIOD_LOCKED");
        verify(journalEntryService, never()).create(any(), any());
    }

    @Test
    void missingChartOfAccountsEntriesSkipWithoutFailing() {
        FixedAsset van = twelveMonthVan();
        when(assetRepository.findByStatusInOrderByAcquisitionDateAsc(List.of("ACTIVE"))).thenReturn(List.of(van));
        Account expenseOnly = account("acc-exp", "5300");
        when(accountRepository.findAll()).thenReturn(List.of(expenseOnly));
        AssetsApi.DepreciationRunResponse response = service.runDepreciation(APP_MONTH, "finance1");

        assertThat(response.results().get(0).outcome()).isEqualTo("SKIPPED_MISSING_ACCOUNT");
        verify(journalEntryService, never()).create(any(), any());
    }

    @Test
    void assetsOutsideTheirLifeAreSilentlyIgnored() {
        FixedAsset van = twelveMonthVan();
        when(assetRepository.findByStatusInOrderByAcquisitionDateAsc(List.of("ACTIVE"))).thenReturn(List.of(van));

        AssetsApi.DepreciationRunResponse response = service.runDepreciation("2026-01", "finance1");

        assertThat(response.postedCount()).isZero();
        assertThat(response.results()).isEmpty();
        verify(journalEntryService, never()).create(any(), any());
    }

    @Test
    void disposalWithGainBooksBalancedGainJournal() {
        FixedAsset van = twelveMonthVan();
        van.registerPostedCharge(java.time.YearMonth.of(2026, 2), new BigDecimal("1000.00"));
        van.registerPostedCharge(java.time.YearMonth.of(2026, 3), new BigDecimal("1000.00"));
        van.registerPostedCharge(java.time.YearMonth.of(2026, 4), new BigDecimal("1000.00"));
        van.registerPostedCharge(java.time.YearMonth.of(2026, 5), new BigDecimal("1000.00"));
        // accumulated 4000, NBV 8000; sold for 9000 → gain 1000
        Account accumulatedAcc = account("acc-acc", "1280");
        Account cashAcc = account("acc-cash", "10101");
        Account costAcc = account("acc-cost", "1300");
        Account gainAcc = account("acc-gain", "4100");
        when(accountRepository.findAll()).thenReturn(List.of(accumulatedAcc, cashAcc, costAcc, gainAcc));
        givenPostedJournal("je-9", "JV-900");
        long disposalDate = LocalDate.of(2026, 6, 30).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

        AssetsApi.DisposalJournalSummary summary =
                service.disposeWithJournal(van, disposalDate, new BigDecimal("9000.00"), "finance1");

        assertThat(summary.netBookValue()).isEqualByComparingTo(new BigDecimal("8000.00"));
        assertThat(summary.gainOrLoss()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(summary.entryNumber()).isEqualTo("JV-900");

        List<AccountingApi.JournalEntryLinePayload> lines = capturedLines();
        BigDecimal debits = sum(lines, true);
        BigDecimal credits = sum(lines, false);
        assertThat(debits).isEqualByComparingTo(credits);
        assertThat(debits).isEqualByComparingTo(new BigDecimal("13000.00"));

        AccountingApi.JournalEntryLinePayload costLine = lines.stream()
                .filter(line -> line.accountId().equals("acc-cost")).findFirst().orElseThrow();
        assertThat(costLine.credit()).isEqualByComparingTo(new BigDecimal("12000.00"));
        AccountingApi.JournalEntryLinePayload gainLine = lines.stream()
                .filter(line -> line.accountId().equals("acc-gain")).findFirst().orElseThrow();
        assertThat(gainLine.credit()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(lines.stream().filter(line -> "acc-acc".equals(line.accountId()))
                .findFirst().orElseThrow().debit()).isEqualByComparingTo(new BigDecimal("4000.00"));
        assertThat(lines.stream().filter(line -> "acc-cash".equals(line.accountId()))
                .findFirst().orElseThrow().debit()).isEqualByComparingTo(new BigDecimal("9000.00"));
        verify(auditService).record(eq("DISPOSE"), eq("FIXED_ASSET"), eq(van.getId()),
                eq("finance1"), any(), any());
    }

    @Test
    void disposalAtALossBooksTheLossPlug() {
        FixedAsset van = twelveMonthVan();
        van.registerPostedCharge(java.time.YearMonth.of(2026, 2), new BigDecimal("1000.00"));
        van.registerPostedCharge(java.time.YearMonth.of(2026, 3), new BigDecimal("1000.00"));
        van.registerPostedCharge(java.time.YearMonth.of(2026, 4), new BigDecimal("1000.00"));
        van.registerPostedCharge(java.time.YearMonth.of(2026, 5), new BigDecimal("1000.00"));
        Account accumulatedAcc = account("acc-acc", "1280");
        Account cashAcc = account("acc-cash", "10101");
        Account costAcc = account("acc-cost", "1300");
        Account lossAcc = account("acc-loss", "5310");
        when(accountRepository.findAll()).thenReturn(List.of(accumulatedAcc, cashAcc, costAcc, lossAcc));
        givenPostedJournal("je-8", "JV-800");
        long disposalDate = LocalDate.of(2026, 6, 30).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

        AssetsApi.DisposalJournalSummary summary =
                service.disposeWithJournal(van, disposalDate, new BigDecimal("7500.00"), "finance1");

        assertThat(summary.gainOrLoss()).isEqualByComparingTo(new BigDecimal("-500.00"));
        List<AccountingApi.JournalEntryLinePayload> lines = capturedLines();
        assertThat(sum(lines, true)).isEqualByComparingTo(sum(lines, false));
        assertThat(lines.stream().filter(line -> "acc-loss".equals(line.accountId()))
                .findFirst().orElseThrow().debit()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void disposalRefusesWhenDisposalAccountsAreMissing() {
        FixedAsset van = twelveMonthVan();
        Account accumulatedOnly = account("acc-acc", "1280");
        when(accountRepository.findAll()).thenReturn(List.of(accumulatedOnly));

        assertThatThrownBy(() -> service.disposeWithJournal(van,
                LocalDate.of(2026, 6, 30).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                new BigDecimal("9000.00"), "finance1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "ASSET_DISPOSAL_INVALID");
        verify(journalEntryService, never()).create(any(), any());
    }

    private static BigDecimal sum(List<AccountingApi.JournalEntryLinePayload> lines, boolean debit) {
        return lines.stream()
                .map(line -> debit ? line.debit() : line.credit())
                .map(amount -> amount == null ? BigDecimal.ZERO : amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
