package com.bemo.hr.workforce;

import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.i18n.TranslationService;
import com.bemo.hr.trade.sales.application.SalesReceivablesService;
import com.bemo.hr.trade.sales.domain.CustomerInvoice;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ClientBillingServiceTests {

    private static final String CLIENT = "party-c1";
    private static final String CATEGORY = "cat-1";

    private ClientWorkerRateRepository rateRepository;
    private ClientBillingPeriodRepository periodRepository;
    private ClientBillingDraftLineRepository lineRepository;
    private WorkerRepository workerRepository;
    private WorkerCategoryRepository categoryRepository;
    private ManualAttendanceEntryRepository attendanceRepository;
    private WorkforceSettlementPeriodRepository settlementPeriodRepository;
    private ContractorSettlementRepository contractorSettlementRepository;
    private ContractorSettlementLineRepository settlementLineRepository;
    private BusinessPartyRepository partyRepository;
    private SalesReceivablesService salesReceivablesService;
    private TranslationService translationService;
    private ClientBillingService service;

    @BeforeEach
    void setUp() {
        rateRepository = mock(ClientWorkerRateRepository.class);
        periodRepository = mock(ClientBillingPeriodRepository.class);
        lineRepository = mock(ClientBillingDraftLineRepository.class);
        workerRepository = mock(WorkerRepository.class);
        categoryRepository = mock(WorkerCategoryRepository.class);
        attendanceRepository = mock(ManualAttendanceEntryRepository.class);
        settlementPeriodRepository = mock(WorkforceSettlementPeriodRepository.class);
        contractorSettlementRepository = mock(ContractorSettlementRepository.class);
        settlementLineRepository = mock(ContractorSettlementLineRepository.class);
        partyRepository = mock(BusinessPartyRepository.class);
        salesReceivablesService = mock(SalesReceivablesService.class);
        translationService = mock(TranslationService.class);
        service = new ClientBillingService(rateRepository, periodRepository, lineRepository, workerRepository,
                categoryRepository, attendanceRepository, settlementPeriodRepository, contractorSettlementRepository,
                settlementLineRepository, partyRepository, salesReceivablesService, translationService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("tester", "n/a", List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private BusinessParty activeClient() {
        return new BusinessParty("C-100", "Acme Construction", "Acme Construction", "CLIENT",
                "owner", "010011122", "x@acme.test", "Giza", "notes", true,
                "DIRECT", null, "2026-01-01", null, "EGP", "CASH", "30", "999-000", null);
    }

    private WorkforceSettlementPeriod approvedPeriod(String start, String end) {
        WorkforceSettlementPeriod period = new WorkforceSettlementPeriod("P-" + start, start, end, "HALF_MONTH", "APPROVED");
        period.setStatus("APPROVED");
        return period;
    }

    private void stubRates(String clientPartyId, ClientWorkerRate... rates) {
        when(rateRepository.findByClientPartyIdAndWorkerCategoryId(eq(clientPartyId), anyString()))
                .thenReturn(List.of(rates));
        when(rateRepository.save(any(ClientWorkerRate.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private BillingContext billableContext() {
        BillingContext ctx = new BillingContext();
        ctx.period = approvedPeriod("2026-08-01", "2026-08-31");
        ctx.worker = new Worker("WRK-1", "Ahmed Hassan", "cont-1", CATEGORY,
                new BigDecimal("150"), new BigDecimal("8"), null, "MANUAL", "ACTIVE", null, "301", null);
        ctx.rate = new ClientWorkerRate("rate-1", CLIENT, CATEGORY, new BigDecimal("220.00"),
                "2026-01-01", null, "admin");
        when(partyRepository.findById(CLIENT)).thenReturn(Optional.of(activeClient()));
        when(categoryRepository.findById(CATEGORY)).thenReturn(
                Optional.of(new WorkerCategory("cat-code", "Laborer", "desc", new BigDecimal("100"), new BigDecimal("8"), "HALF_MONTH", "ACTIVE")));
        when(categoryRepository.existsById(CATEGORY)).thenReturn(true);
        when(workerRepository.findAll()).thenReturn(List.of(ctx.worker));
        when(workerRepository.findById(ctx.worker.getId())).thenReturn(Optional.of(ctx.worker));
        when(settlementPeriodRepository.findAll()).thenReturn(List.of(ctx.period));
        when(periodRepository.findByClientPartyIdAndPeriod(CLIENT, "2026-08")).thenReturn(Optional.empty());
        when(periodRepository.save(any(ClientBillingPeriod.class))).thenAnswer(inv -> inv.getArgument(0));
        when(lineRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        stubRates(CLIENT, ctx.rate);
        return ctx;
    }

    private static final class BillingContext {
        WorkforceSettlementPeriod period;
        Worker worker;
        ClientWorkerRate rate;
    }

    // ------------------------------------------------------------------
    // AC-1: approved attendance only (DRAFT period days are never billed)
    // ------------------------------------------------------------------

    @Test
    void billableDaysOnlyFromApprovedOrLockedPeriods() {
        BillingContext ctx = billableContext();
        Worker worker = ctx.worker;
        // Work happens 2026-08-05; settlement period is APPROVED 01..31.
        when(attendanceRepository.findByWorkerIdAndWorkDateBetween(worker.getId(), "2026-08-01", "2026-08-31"))
                .thenReturn(List.of(attendance(worker.getId(), "2026-08-05", "1.00"),
                        attendance(worker.getId(), "2026-08-10", "0.50")));
        when(attendanceRepository.findByWorkerIdAndWorkDateBetween(eq(worker.getId()), startsWith("2026-07-0"), anyString()))
                .thenReturn(List.of());

        ClientBillingApi.BillingReviewResponse result = service.generate(
                new ClientBillingApi.GenerateBillingRequest(CLIENT, "2026-08"));

        assertThat(result.lines()).hasSize(1);
        ClientBillingApi.BillingLineResponse line = result.lines().get(0);
        assertThat(line.approvedDays()).isEqualByComparingTo("1.50");
        assertThat(line.amount()).isEqualByComparingTo("330.00"); // 1.00*220 + 0.50*220
        assertThat(line.lineStatus()).isEqualTo(ClientBillingDraftLine.LINE_BILLABLE);
    }

    @Test
    void draftPeriodDaysAreExcludedFromBilling() {
        BillingContext ctx = billableContext();
        Worker worker = ctx.worker;
        WorkforceSettlementPeriod draft = new WorkforceSettlementPeriod("P-DRAFT", "2026-08-01", "2026-08-31", "HALF_MONTH", "DRAFT");
        when(settlementPeriodRepository.findAll()).thenReturn(List.of(draft));
        // Attendance exists but belongs to a DRAFT (not approved) period window, so nothing is billed.
        when(attendanceRepository.findByWorkerIdAndWorkDateBetween(worker.getId(), "2026-08-01", "2026-08-31")).thenReturn(List.of());
        when(attendanceRepository.findByWorkerIdAndWorkDateBetween(eq(worker.getId()), startsWith("2026-07"), anyString())).thenReturn(List.of());

        ClientBillingApi.BillingReviewResponse result = service.generate(
                new ClientBillingApi.GenerateBillingRequest(CLIENT, "2026-08"));

        assertThat(result.lines()).isEmpty();
        assertThat(result.totalBilledAmount()).isEqualByComparingTo("0.00");
    }

    // ------------------------------------------------------------------
    // AC-2: mid-month effective rate change prices later days with the new rate
    // ------------------------------------------------------------------

    @Test
    void midMonthRateChangeAppliesOldThenNewRatePerDay() {
        BillingContext ctx = billableContext();
        Worker worker = ctx.worker;
        ClientWorkerRate oldRate = new ClientWorkerRate("rate-old", CLIENT, CATEGORY, new BigDecimal("200.00"),
                "2026-01-01", "2026-08-15", "admin");
        ClientWorkerRate newRate = new ClientWorkerRate("rate-new", CLIENT, CATEGORY, new BigDecimal("250.00"),
                "2026-08-16", null, "admin");
        stubRates(CLIENT, oldRate, newRate);
        when(attendanceRepository.findByWorkerIdAndWorkDateBetween(worker.getId(), "2026-08-01", "2026-08-31"))
                .thenReturn(List.of(attendance(worker.getId(), "2026-08-15", "1.00"),
                        attendance(worker.getId(), "2026-08-17", "1.00")));
        when(attendanceRepository.findByWorkerIdAndWorkDateBetween(eq(worker.getId()), startsWith("2026-07"), anyString()))
                .thenReturn(List.of());

        ClientBillingApi.BillingReviewResponse result = service.generate(
                new ClientBillingApi.GenerateBillingRequest(CLIENT, "2026-08"));

        assertThat(result.lines()).hasSize(1);
        ClientBillingApi.BillingLineResponse line = result.lines().get(0);
        assertThat(line.amount()).isEqualByComparingTo("450.00"); // 1*200 + 1*250
        assertThat(line.dayRate()).isEqualByComparingTo("250.00"); // effective as of month end
    }

    @Test
    void overlappingRatesForSameClientAndCategoryAreRejected() {
        when(partyRepository.findById(CLIENT)).thenReturn(Optional.of(activeClient()));
        when(categoryRepository.existsById(CATEGORY)).thenReturn(true);
        ClientWorkerRate existing = new ClientWorkerRate("rate-a", CLIENT, CATEGORY, new BigDecimal("200.00"),
                "2026-01-01", null, "admin");
        when(rateRepository.findByClientPartyIdAndWorkerCategoryId(CLIENT, CATEGORY)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.addRate(new ClientBillingApi.CreateRateRequest(
                CLIENT, CATEGORY, new BigDecimal("300.00"), "2026-06-01", null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "CLIENT_RATE_OVERLAP");
    }

    // ------------------------------------------------------------------
    // AC-3: missing client rate → blocked confirmation with a clear reason
    // ------------------------------------------------------------------

    @Test
    void missingRateWorkerGetsUnresolvedLineBlockingConfirmation() {
        BillingContext ctx = billableContext();
        Worker worker = ctx.worker;
        when(attendanceRepository.findByWorkerIdAndWorkDateBetween(worker.getId(), "2026-08-01", "2026-08-31"))
                .thenReturn(List.of(attendance(worker.getId(), "2026-08-05", "1.00")));
        when(attendanceRepository.findByWorkerIdAndWorkDateBetween(eq(worker.getId()), startsWith("2026-07"), anyString()))
                .thenReturn(List.of());
        // The client simply has no rate for the category.
        stubRates(CLIENT);

        ClientBillingApi.BillingReviewResponse result = service.generate(
                new ClientBillingApi.GenerateBillingRequest(CLIENT, "2026-08"));

        assertThat(result.lines()).hasSize(1);
        ClientBillingApi.BillingLineResponse line = result.lines().get(0);
        assertThat(line.lineStatus()).isEqualTo(ClientBillingDraftLine.LINE_MISSING_RATE);
        assertThat(line.reason()).contains("No effective client rate");
        assertThat(line.amount()).isEqualByComparingTo("0.00");

        ClientBillingPeriod open = new ClientBillingPeriod("bp-1", CLIENT, "2026-08", "tester");
        when(periodRepository.findByClientPartyIdAndPeriod(CLIENT, "2026-08")).thenReturn(Optional.of(open));
        when(lineRepository.findByBillingPeriodId("bp-1"))
                .thenReturn(result.lines().stream().map(this::toDraftLine).toList());

        assertThatThrownBy(() -> service.confirm(CLIENT, "2026-08"))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "CLIENT_BILLING_UNRESOLVED_LINES");
    }

    // ------------------------------------------------------------------
    // AC-4: confirmation creates exactly one delivery invoice per period
    // ------------------------------------------------------------------

    @Test
    void confirmCreatesSingleInvoiceMeasuredFromApprovedDayRates() {
        BillingContext ctx = billableContext();
        Worker worker = ctx.worker;
        when(attendanceRepository.findByWorkerIdAndWorkDateBetween(worker.getId(), "2026-08-01", "2026-08-31"))
                .thenReturn(List.of(attendance(worker.getId(), "2026-08-05", "1.00"),
                        attendance(worker.getId(), "2026-08-13", "1.00")));
        when(attendanceRepository.findByWorkerIdAndWorkDateBetween(eq(worker.getId()), startsWith("2026-07"), anyString()))
                .thenReturn(List.of());
        when(periodRepository.findByClientPartyIdAndPeriod(CLIENT, "2026-08")).thenReturn(Optional.empty());
        when(periodRepository.save(any(ClientBillingPeriod.class))).thenAnswer(inv -> inv.getArgument(0));
        CustomerInvoice invoice = new CustomerInvoice("INV-100", CLIENT, "so-1", java.time.LocalDate.of(2026, 8, 31),
                java.time.LocalDate.of(2026, 9, 30), "EGP", new BigDecimal("440.00"));
        when(salesReceivablesService.createAndIssueDeliveryInvoice(eq("CLB-202608-PARTY-"), eq(CLIENT), isNull(),
                eq(java.time.LocalDate.of(2026, 8, 31)), eq("EGP"), eq(new BigDecimal("440.00")), anyString()))
                .thenReturn(invoice);
        when(lineRepository.findByBillingPeriodId("bp-1")).thenReturn(List.of());

        service.generate(new ClientBillingApi.GenerateBillingRequest(CLIENT, "2026-08"));
        // After generation the lines are persisted; re-bind the review.
        ClientBillingPeriod counted = new ClientBillingPeriod("bp-1", CLIENT, "2026-08", "tester");
        when(periodRepository.findByClientPartyIdAndPeriod(CLIENT, "2026-08")).thenReturn(Optional.of(counted));

        // Re-run generation to populate the persisted lines (mocked repo stores nothing), then confirm.
        when(attendanceRepository.findByWorkerIdAndWorkDateBetween(worker.getId(), "2026-08-01", "2026-08-31"))
                .thenReturn(List.of(attendance(worker.getId(), "2026-08-05", "1.00"),
                        attendance(worker.getId(), "2026-08-13", "1.00")));
        ClientBillingApi.BillingReviewResponse generated = service.generate(
                new ClientBillingApi.GenerateBillingRequest(CLIENT, "2026-08"));
        when(lineRepository.findByBillingPeriodId(counted.getId())).thenReturn(generated.lines().stream()
                .map(this::toDraftLine)
                .toList());

        ClientBillingApi.ConfirmResponse confirmed = service.confirm(CLIENT, "2026-08");

        assertThat(confirmed.status()).isEqualTo("INVOICED");
        assertThat(confirmed.invoiceNumber()).isEqualTo("INV-100");
        assertThat(confirmed.totalAmount()).isEqualByComparingTo("440.00");
        verify(salesReceivablesService, times(1)).createAndIssueDeliveryInvoice(anyString(), eq(CLIENT), isNull(),
                any(java.time.LocalDate.class), anyString(), any(BigDecimal.class), anyString());
        verify(periodRepository, times(2)).save(any(ClientBillingPeriod.class));
    }

    @Test
    void regenerateAfterInvoicingIsRejected() {
        ClientBillingPeriod invoiced = new ClientBillingPeriod("bp-1", CLIENT, "2026-08", "tester");
        invoiced.markInvoiced("inv-1", "INV-1", new BigDecimal("100.00"));
        when(partyRepository.findById(CLIENT)).thenReturn(Optional.of(activeClient()));
        when(periodRepository.findByClientPartyIdAndPeriod(CLIENT, "2026-08")).thenReturn(Optional.of(invoiced));

        assertThatThrownBy(() -> service.generate(new ClientBillingApi.GenerateBillingRequest(CLIENT, "2026-08")))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "CLIENT_BILLING_PERIOD_EXISTS");
    }

    @Test
    void confirmOnClosedPeriodIsRejected() {
        ClientBillingPeriod invoiced = new ClientBillingPeriod("bp-1", CLIENT, "2026-08", "tester");
        invoiced.markInvoiced("inv-1", "INV-1", new BigDecimal("100.00"));
        when(periodRepository.findByClientPartyIdAndPeriod(CLIENT, "2026-08")).thenReturn(Optional.of(invoiced));

        assertThatThrownBy(() -> service.confirm(CLIENT, "2026-08"))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "CLIENT_BILLING_PERIOD_NOT_OPEN");
    }

    // ------------------------------------------------------------------
    // AC-5: margin = billed minus wage cost of the same approved periods
    // ------------------------------------------------------------------

    @Test
    void marginReportSubtractsSettledWageCostFromBilledAmount() {
        BillingContext ctx = billableContext();
        Worker worker = ctx.worker;
        when(attendanceRepository.findByWorkerIdAndWorkDateBetween(worker.getId(), "2026-08-01", "2026-08-31"))
                .thenReturn(List.of(attendance(worker.getId(), "2026-08-05", "1.00"),
                        attendance(worker.getId(), "2026-08-13", "1.00")));
        when(attendanceRepository.findByWorkerIdAndWorkDateBetween(eq(worker.getId()), startsWith("2026-07"), anyString()))
                .thenReturn(List.of());
        ContractorSettlement settlement = new ContractorSettlement(ctx.period.getId(), "cont-1", "RATE",
                new BigDecimal("300.00"), new BigDecimal("20.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("320.00"), new BigDecimal("320.00"), BigDecimal.ZERO, "APPROVED");
        ContractorSettlementLine line = new ContractorSettlementLine(settlement.getId(), worker.getId(),
                new BigDecimal("2.00"), new BigDecimal("180.00"), new BigDecimal("360.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("360.00"), null);
        when(contractorSettlementRepository.findByPeriodId(ctx.period.getId())).thenReturn(List.of(settlement));
        when(settlementLineRepository.findBySettlementId(settlement.getId())).thenReturn(List.of(line));
        when(periodRepository.findByClientPartyIdAndPeriod(CLIENT, "2026-08"))
                .thenReturn(Optional.of(new ClientBillingPeriod("bp-1", CLIENT, "2026-08", "tester")));
        when(lineRepository.findByBillingPeriodId("bp-1"))
                .thenReturn(List.of(new ClientBillingDraftLine("dl-1", "bp-1", worker.getId(), "WRK-1",
                        "Ahmed Hassan", CATEGORY, "Laborer", new BigDecimal("2.00"), new BigDecimal("220.00"),
                        new BigDecimal("440.00"), new BigDecimal("360.00"), new BigDecimal("50.00"),
                        ClientBillingDraftLine.LINE_BILLABLE, null)));

        ClientBillingApi.MarginReportResponse report = service.marginReport(CLIENT, "2026-08");

        assertThat(report.totalBilled()).isEqualByComparingTo("440.00");
        assertThat(report.totalWageCost()).isEqualByComparingTo("360.00");
        assertThat(report.totalMargin()).isEqualByComparingTo("80.00");
        assertThat(report.rows()).singleElement().satisfies(row -> {
            assertThat(row.marginAmount()).isEqualByComparingTo("80.00");
            assertThat(row.workerCode()).isEqualTo("WRK-1");
        });
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private ManualAttendanceEntry attendance(String workerId, String date, String value) {
        return new ManualAttendanceEntry(workerId, date, new BigDecimal(value),
                "09:00", "17:00", new BigDecimal("8.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("100.00"), "MANUAL", null);
    }

    private ClientBillingDraftLine toDraftLine(ClientBillingApi.BillingLineResponse line) {
        return new ClientBillingDraftLine("dl-" + line.workerCode(), "bp-1", line.workerId(),
                line.workerCode(), line.fullName(), line.categoryId(), line.categoryName(),
                line.approvedDays(), line.dayRate(), line.amount(), line.wageCost(), line.varianceAmount(),
                line.lineStatus(), line.reason());
    }
}