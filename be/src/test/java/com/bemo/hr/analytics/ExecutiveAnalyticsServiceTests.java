package com.bemo.hr.analytics;

import com.bemo.hr.analytics.api.ExecutiveAnalyticsApi.*;
import com.bemo.hr.analytics.application.ExecutiveAnalyticsService;
import com.bemo.hr.analytics.domain.*;
import com.bemo.hr.analytics.infrastructure.ExecutiveCockpitTargetRepository;
import com.bemo.hr.analytics.infrastructure.ExecutiveKpiSnapshotRepository;
import com.bemo.hr.compliance.eta.domain.EtaInvoiceSubmission;
import com.bemo.hr.compliance.eta.infrastructure.EtaInvoiceSubmissionRepository;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.expenses.domain.ExpenseClaim;
import com.bemo.hr.expenses.infrastructure.ExpenseClaimRepository;
import com.bemo.hr.finance.application.FinancialStatementsReportService;
import com.bemo.hr.finance.application.TreasuryPositionService;
import com.bemo.hr.manufacturing.production.infrastructure.ProductionOrderRepository;
import com.bemo.hr.operations.InventoryItem;
import com.bemo.hr.operations.InventoryItemRepository;
import com.bemo.hr.operations.InventoryValuationService;
import com.bemo.hr.operations.OperationsApi;
import com.bemo.hr.organization.domain.Branch;
import com.bemo.hr.organization.infrastructure.BranchRepository;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.payroll.domain.SalaryPayment;
import com.bemo.hr.payroll.infrastructure.SalaryPaymentRepository;
import com.bemo.hr.project.domain.Project;
import com.bemo.hr.project.domain.ProjectStatus;
import com.bemo.hr.project.infrastructure.ProjectBudgetVersionRepository;
import com.bemo.hr.project.infrastructure.ProjectCostLedgerEntryRepository;
import com.bemo.hr.project.infrastructure.ProjectRepository;
import com.bemo.hr.access.application.SecurityAuthorizationEvaluator;
import com.bemo.hr.trade.pos.infrastructure.PosTransactionRepository;
import com.bemo.hr.trade.procurement.infrastructure.SupplierInvoiceRepository;
import com.bemo.hr.trade.sales.domain.CustomerInvoice;
import com.bemo.hr.trade.sales.domain.SalesDeliveryLine;
import com.bemo.hr.trade.sales.domain.SalesQuotation;
import com.bemo.hr.trade.sales.infrastructure.CustomerInvoiceRepository;
import com.bemo.hr.trade.sales.infrastructure.CustomerReceiptRepository;
import com.bemo.hr.trade.sales.infrastructure.SalesDeliveryLineRepository;
import com.bemo.hr.trade.sales.infrastructure.SalesQuotationRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 2026-09-06 remediation: replaces the previous test suite, whose one test exercising the
 * "all real data empty" path ({@code getOwnerCockpitAggregatesAllKpis}) asserted only
 * {@code isNotNull()} on the response — it would have passed identically whether the service
 * returned real zeros or the hardcoded fallback constants it used to fabricate (42,850 / 220,000 /
 * fake customers / fake products / ...). See docs/DEEP_ENGINEERING_REVIEW_2026-09-06.md, High
 * Finding H-5. Every test below that exercises the empty-data path now asserts the actual numeric
 * values are zero/empty, not merely non-null — these tests would have FAILED against the
 * pre-remediation implementation.
 */
@ExtendWith(MockitoExtension.class)
class ExecutiveAnalyticsServiceTests {

    @Mock private ExecutiveKpiSnapshotRepository snapshotRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectBudgetVersionRepository projectBudgetVersionRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private InventoryItemRepository inventoryItemRepository;
    @Mock private SalesQuotationRepository salesQuotationRepository;
    @Mock private SalesDeliveryLineRepository salesDeliveryLineRepository;
    @Mock private PosTransactionRepository posTransactionRepository;
    @Mock private EtaInvoiceSubmissionRepository etaSubmissionRepository;
    @Mock private CustomerInvoiceRepository customerInvoiceRepository;
    @Mock private CustomerReceiptRepository customerReceiptRepository;
    @Mock private SupplierInvoiceRepository supplierInvoiceRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private ProductionOrderRepository productionOrderRepository;
    @Mock private ProjectCostLedgerEntryRepository costLedgerRepository;
    @Mock private ExpenseClaimRepository expenseClaimRepository;
    @Mock private SalaryPaymentRepository salaryPaymentRepository;
    @Mock private ExecutiveCockpitTargetRepository cockpitTargetRepository;
    @Mock private BusinessPartyRepository businessPartyRepository;
    @Mock private SecurityAuthorizationEvaluator authEvaluator;
    @Mock private FinancialStatementsReportService financialStatementsReportService;
    @Mock private InventoryValuationService inventoryValuationService;
    @Mock private TreasuryPositionService treasuryPositionService;
    @Mock private com.bemo.hr.finance.infrastructure.FiscalPeriodRepository fiscalPeriodRepository;
    @Mock private com.bemo.hr.trade.pos.infrastructure.PosTerminalRepository posTerminalRepository;
    @Mock private com.bemo.hr.organization.infrastructure.WarehouseRepository warehouseRepository;

    private ExecutiveAnalyticsService service;
    private final String currentPeriod = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

    @BeforeEach
    void setUp() {
        service = new ExecutiveAnalyticsService(
                snapshotRepository, projectRepository, projectBudgetVersionRepository, employeeRepository,
                inventoryItemRepository, salesQuotationRepository, salesDeliveryLineRepository,
                posTransactionRepository, etaSubmissionRepository, customerInvoiceRepository,
                customerReceiptRepository, supplierInvoiceRepository, branchRepository,
                productionOrderRepository, costLedgerRepository, expenseClaimRepository,
                salaryPaymentRepository, cockpitTargetRepository, businessPartyRepository, authEvaluator,
                financialStatementsReportService, inventoryValuationService, treasuryPositionService,
                fiscalPeriodRepository, posTerminalRepository, warehouseRepository
        );
    }

    /** Wires every repository/service to return an empty/zero result, exactly the scenario that used to trigger fabrication. */
    private void stubEverythingEmpty() {
        lenient().when(projectRepository.findAll()).thenReturn(List.of());
        lenient().when(inventoryValuationService.report()).thenReturn(
                new OperationsApi.ValuationReport(null, BigDecimal.ZERO, List.of(), List.of(), null, null));
        lenient().when(inventoryValuationService.report(any(), any(), any())).thenReturn(
                new OperationsApi.ValuationReport(null, BigDecimal.ZERO, List.of(), List.of(), null, null));
        lenient().when(posTransactionRepository.sumCompletedInRange(anyLong(), anyLong())).thenReturn(BigDecimal.ZERO);
        lenient().when(fiscalPeriodRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(any(), any())).thenReturn(List.of());
        lenient().when(salesQuotationRepository.findByQuoteDateBetween(any(), any())).thenReturn(List.of());
        lenient().when(salesDeliveryLineRepository.findByCreatedAtBetween(anyLong(), anyLong())).thenReturn(List.of());
        lenient().when(employeeRepository.findAll()).thenReturn(List.of());
        lenient().when(etaSubmissionRepository.findAll()).thenReturn(List.of());
        lenient().when(customerInvoiceRepository.findByInvoiceDateBetween(any(), any())).thenReturn(List.of());
        lenient().when(customerInvoiceRepository.findByOutstandingAmountGreaterThan(any())).thenReturn(List.of());
        lenient().when(customerInvoiceRepository.topCustomersByInvoicedAmount(any())).thenReturn(List.of());
        lenient().when(customerReceiptRepository.findByReceiptDateBetween(any(), any())).thenReturn(List.of());
        lenient().when(supplierInvoiceRepository.findByStatusNot(any())).thenReturn(List.of());
        lenient().when(branchRepository.findAllByOrderByCodeAsc()).thenReturn(List.of());
        lenient().when(productionOrderRepository.findAllByOrderByStartDateDescCreatedAtDesc()).thenReturn(List.of());
        lenient().when(expenseClaimRepository.findBySpentOnBetween(any(), any())).thenReturn(List.of());
        lenient().when(salaryPaymentRepository.findByPeriodYearAndPeriodMonthOrderByCreatedAtDesc(anyInt(), anyInt())).thenReturn(List.of());
        lenient().when(cockpitTargetRepository.findByPeriodKey(any())).thenReturn(Optional.empty());
        lenient().when(inventoryItemRepository.findAll()).thenReturn(List.of());
        lenient().when(financialStatementsReportService.getIncomeStatement(any(), any())).thenReturn(
                new FinancialStatementsReportService.IncomeStatementReport(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        lenient().when(financialStatementsReportService.getCashFlowStatement(any(), any())).thenReturn(
                new FinancialStatementsReportService.CashFlowReport(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true, null));
        lenient().when(treasuryPositionService.totalCashBalance()).thenReturn(BigDecimal.ZERO);
        lenient().when(treasuryPositionService.totalBankBalance()).thenReturn(BigDecimal.ZERO);
        lenient().when(treasuryPositionService.cashBalanceByBranch()).thenReturn(java.util.Map.of());
        lenient().when(treasuryPositionService.bankBalanceByBranch()).thenReturn(java.util.Map.of());
    }

    @Test
    @DisplayName("An empty tenant gets real zeros/empty lists on the Owner Cockpit — never fabricated fallback values")
    void ownerCockpitForAnEmptyTenantReturnsRealZerosNotFakeData() {
        stubEverythingEmpty();

        OwnerCockpitResponse response = service.getOwnerCockpit(currentPeriod, null);

        // These exact values (42850.00, 38200.00, 1450000.00, 185000.00, 250000.00) were the
        // hardcoded fallbacks the pre-remediation code substituted for a real zero.
        assertThat(response.kpiSummary().todaySales()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.kpiSummary().todayCollections()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.kpiSummary().totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.kpiSummary().payrollDisbursed()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.kpiSummary().totalOpex()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.kpiSummary().totalReceivables()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.kpiSummary().totalPayables()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.arAging().current().invoiceCount()).isZero();
        assertThat(response.arAging().total()).isEqualByComparingTo(BigDecimal.ZERO);

        // These fake named entities must never appear.
        assertThat(response.topCustomers()).isEmpty();
        assertThat(response.topProducts()).isEmpty();
        assertThat(response.manufacturingWip()).isEmpty();
        assertThat(response.projectBudgetControl()).isEmpty();
        assertThat(response.branchLeaderboard()).isEmpty();
        assertThat(response.topCustomers()).noneMatch(c -> c.customerName().contains("الأهرام"));
        // No revenue at all means there is nothing for COGS to under-cover — reported as full
        // (100%) coverage, not a misleading 0%.
        assertThat(response.kpiSummary().cogsDataCoveragePercent()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    @DisplayName("cogsDataCoveragePercent flags real revenue that has no matching costed delivery line, instead of silently overstating margin")
    void cogsDataCoveragePercentReflectsIncompleteDeliveryLineData() {
        stubEverythingEmpty();
        CustomerInvoice invoice = mock(CustomerInvoice.class);
        when(invoice.getAmount()).thenReturn(BigDecimal.valueOf(100_000));
        when(customerInvoiceRepository.findByInvoiceDateBetween(any(), any())).thenReturn(List.of(invoice));
        // No SalesDeliveryLine exists for this revenue at all (matches stubEverythingEmpty's default).

        OwnerCockpitResponse noCoverage = service.getOwnerCockpit(currentPeriod, null);

        assertThat(noCoverage.kpiSummary().cogsDataCoveragePercent()).isEqualByComparingTo(BigDecimal.ZERO);
        // COGS silently defaults to 0 (never a guessed ratio) — but that means margin is
        // overstated here (100% of revenue counted as pure profit) whenever coverage is 0.
        assertThat(noCoverage.kpiSummary().totalCogs()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(noCoverage.kpiSummary().grossMarginAmount()).isEqualByComparingTo(BigDecimal.valueOf(100_000));

        // Now the SAME revenue is fully represented by a real, costed delivery line.
        SalesDeliveryLine line = mock(SalesDeliveryLine.class);
        when(line.getQuantity()).thenReturn(BigDecimal.TEN);
        when(line.getUnitPrice()).thenReturn(BigDecimal.valueOf(10_000));
        when(line.getCogsAmount()).thenReturn(BigDecimal.valueOf(60_000));
        when(line.getItemId()).thenReturn(null);
        when(salesDeliveryLineRepository.findByCreatedAtBetween(anyLong(), anyLong())).thenReturn(List.of(line));

        OwnerCockpitResponse fullCoverage = service.getOwnerCockpit(currentPeriod, null);

        assertThat(fullCoverage.kpiSummary().cogsDataCoveragePercent()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(fullCoverage.kpiSummary().totalCogs()).isEqualByComparingTo(BigDecimal.valueOf(60_000));
    }

    @Test
    @DisplayName("Real GL revenue/expenses flow through to the cockpit's headline P&L, unmodified by any ad-hoc formula")
    void ownerCockpitUsesRealGlIncomeStatementForHeadlineFigures() {
        stubEverythingEmpty();
        YearMonth ym = YearMonth.parse(currentPeriod);
        when(financialStatementsReportService.getIncomeStatement(ym.atDay(1), ym.atEndOfMonth()))
                .thenReturn(new FinancialStatementsReportService.IncomeStatementReport(
                        BigDecimal.valueOf(900_000), BigDecimal.valueOf(600_000), BigDecimal.valueOf(300_000)));

        OwnerCockpitResponse response = service.getOwnerCockpit(currentPeriod, null);

        assertThat(response.kpiSummary().totalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(900_000));
        assertThat(response.kpiSummary().totalOpex()).isEqualByComparingTo(BigDecimal.valueOf(600_000));
        assertThat(response.kpiSummary().netProfit()).isEqualByComparingTo(BigDecimal.valueOf(300_000));
        // 300000 / 900000 * 100 = 33.33
        assertThat(response.kpiSummary().netMarginPercent()).isEqualByComparingTo(BigDecimal.valueOf(33.33));
    }

    @Test
    @DisplayName("Real AR invoice aging buckets a specific overdue invoice correctly (no fabricated fallback)")
    void ownerCockpitAgesARInvoicesByRealDueDate() {
        stubEverythingEmpty();
        CustomerInvoice invoice = mock(CustomerInvoice.class);
        when(invoice.getOutstandingAmount()).thenReturn(BigDecimal.valueOf(50_000));
        when(invoice.getDueDate()).thenReturn(LocalDate.now().minusDays(45)); // 45 days overdue -> 31-60 bucket
        when(customerInvoiceRepository.findByOutstandingAmountGreaterThan(any())).thenReturn(List.of(invoice));

        OwnerCockpitResponse response = service.getOwnerCockpit(currentPeriod, null);

        assertThat(response.arAging().days30To60().amount()).isEqualByComparingTo(BigDecimal.valueOf(50_000));
        assertThat(response.arAging().days30To60().invoiceCount()).isEqualTo(1);
        assertThat(response.arAging().current().amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.arAging().total()).isEqualByComparingTo(BigDecimal.valueOf(50_000));
    }

    @Test
    @DisplayName("Branch leaderboard reports real headcount/cash but zero revenue (no per-branch attribution exists) — never a proportional split")
    void branchLeaderboardHasRealHeadcountAndZeroRevenueNotAFabricatedSplit() {
        stubEverythingEmpty();
        Branch branch = mock(Branch.class);
        when(branch.getId()).thenReturn("br-1");
        when(branch.getCode()).thenReturn("MAIN");
        when(branch.getName()).thenReturn("Main Branch");
        when(branch.isMainBranch()).thenReturn(true);
        when(branchRepository.findAllByOrderByCodeAsc()).thenReturn(List.of(branch));
        when(authEvaluator.hasBranchAccess("br-1")).thenReturn(true);

        Employee emp1 = mock(Employee.class);
        when(emp1.getBranchId()).thenReturn("br-1");
        when(emp1.isActive()).thenReturn(true);
        Employee emp2 = mock(Employee.class);
        when(emp2.getBranchId()).thenReturn("br-1");
        when(emp2.isActive()).thenReturn(false); // inactive — must not be counted
        when(employeeRepository.findAll()).thenReturn(List.of(emp1, emp2));

        when(treasuryPositionService.cashBalanceByBranch()).thenReturn(java.util.Map.of("br-1", BigDecimal.valueOf(10_000)));
        when(treasuryPositionService.bankBalanceByBranch()).thenReturn(java.util.Map.of("br-1", BigDecimal.valueOf(5_000)));

        // Revenue is real and non-zero at the tenant level, so a fixed-percentage split (the old
        // *0.6/*0.4 formula) would have produced a non-zero branch revenue. It must not.
        when(financialStatementsReportService.getIncomeStatement(any(), any())).thenReturn(
                new FinancialStatementsReportService.IncomeStatementReport(
                        BigDecimal.valueOf(2_000_000), BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(1_000_000)));

        OwnerCockpitResponse response = service.getOwnerCockpit(currentPeriod, null);

        assertThat(response.branchLeaderboard()).hasSize(1);
        BranchPerformanceItem item = response.branchLeaderboard().get(0);
        assertThat(item.headcount()).isEqualTo(1); // only the active employee
        assertThat(item.cashAndBank()).isEqualByComparingTo(BigDecimal.valueOf(15_000));
        assertThat(item.revenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(item.cogs()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(item.netProfit()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("BRANCH FILTERING: cockpit(branchId=A1) never returns Branch A2's data and vice versa; tenant-wide (no branchId) combines both")
    void branchFilteringGenuinelyIsolatesEachBranchsDataAndNeverLeaksTheOtherBranch() {
        stubEverythingEmpty();

        Branch branchA1 = mock(Branch.class);
        when(branchA1.getId()).thenReturn("branch-a1");
        when(branchA1.getCode()).thenReturn("A1");
        when(branchA1.getName()).thenReturn("Branch A1");
        when(branchA1.isMainBranch()).thenReturn(true);
        Branch branchA2 = mock(Branch.class);
        when(branchA2.getId()).thenReturn("branch-a2");
        when(branchA2.getCode()).thenReturn("A2");
        when(branchA2.getName()).thenReturn("Branch A2");
        when(branchA2.isMainBranch()).thenReturn(false);
        when(authEvaluator.hasBranchAccess("branch-a1")).thenReturn(true);
        when(authEvaluator.hasBranchAccess("branch-a2")).thenReturn(true);
        when(branchRepository.findById("branch-a1")).thenReturn(Optional.of(branchA1));
        when(branchRepository.findById("branch-a2")).thenReturn(Optional.of(branchA2));
        when(branchRepository.findAllByOrderByCodeAsc()).thenReturn(List.of(branchA1, branchA2));

        // Deterministic, materially different headcount per branch: A1 has 2 active + 1 inactive
        // (inactive must not count); A2 has 1 active.
        Employee a1Emp1 = mock(Employee.class);
        when(a1Emp1.isActive()).thenReturn(true);
        when(a1Emp1.getBranchId()).thenReturn("branch-a1");
        Employee a1Emp2 = mock(Employee.class);
        when(a1Emp2.isActive()).thenReturn(true);
        when(a1Emp2.getBranchId()).thenReturn("branch-a1");
        Employee a1Emp3Inactive = mock(Employee.class);
        when(a1Emp3Inactive.isActive()).thenReturn(false);
        when(a1Emp3Inactive.getBranchId()).thenReturn("branch-a1");
        Employee a2Emp1 = mock(Employee.class);
        when(a2Emp1.isActive()).thenReturn(true);
        when(a2Emp1.getBranchId()).thenReturn("branch-a2");
        when(employeeRepository.findByBranchId("branch-a1")).thenReturn(List.of(a1Emp1, a1Emp2, a1Emp3Inactive));
        when(employeeRepository.findByBranchId("branch-a2")).thenReturn(List.of(a2Emp1));
        when(employeeRepository.findAll()).thenReturn(List.of(a1Emp1, a1Emp2, a1Emp3Inactive, a2Emp1));

        // Deterministic, materially different cash/bank per branch.
        when(treasuryPositionService.cashBalanceByBranch()).thenReturn(
                java.util.Map.of("branch-a1", BigDecimal.valueOf(10_000), "branch-a2", BigDecimal.valueOf(500_000)));
        when(treasuryPositionService.bankBalanceByBranch()).thenReturn(
                java.util.Map.of("branch-a1", BigDecimal.valueOf(20_000), "branch-a2", BigDecimal.valueOf(700_000)));

        // Deterministic, materially different POS revenue per branch, via real terminalId -> branchId join.
        com.bemo.hr.trade.pos.domain.PosTerminal a1Terminal = mock(com.bemo.hr.trade.pos.domain.PosTerminal.class);
        when(a1Terminal.getId()).thenReturn("term-a1");
        com.bemo.hr.trade.pos.domain.PosTerminal a2Terminal = mock(com.bemo.hr.trade.pos.domain.PosTerminal.class);
        when(a2Terminal.getId()).thenReturn("term-a2");
        when(posTerminalRepository.findByBranchId("branch-a1")).thenReturn(List.of(a1Terminal));
        when(posTerminalRepository.findByBranchId("branch-a2")).thenReturn(List.of(a2Terminal));
        when(posTransactionRepository.sumCompletedInRangeForTerminals(eq(List.of("term-a1")), anyLong(), anyLong()))
                .thenReturn(BigDecimal.valueOf(1_111));
        when(posTransactionRepository.sumCompletedInRangeForTerminals(eq(List.of("term-a2")), anyLong(), anyLong()))
                .thenReturn(BigDecimal.valueOf(9_999));

        // Deterministic, materially different payroll per branch, via real employeeId -> branchId join.
        SalaryPayment a1Payment = mock(SalaryPayment.class);
        when(a1Payment.getPaymentStatus()).thenReturn(com.bemo.hr.payroll.domain.PaymentStatus.PAID);
        when(a1Payment.getNetAmount()).thenReturn(BigDecimal.valueOf(3_000));
        SalaryPayment a2Payment = mock(SalaryPayment.class);
        when(a2Payment.getPaymentStatus()).thenReturn(com.bemo.hr.payroll.domain.PaymentStatus.PAID);
        when(a2Payment.getNetAmount()).thenReturn(BigDecimal.valueOf(70_000));
        YearMonth ym = YearMonth.parse(currentPeriod);
        // Mockito matches by the ACTUAL list content passed by the service (order of
        // findByBranchId's mocked return), so stub using the exact lists the service will build.
        when(salaryPaymentRepository.findByEmployeeIdInAndPeriodYearAndPeriodMonth(
                argThat(ids -> ids != null && ids.size() == 3), eq(ym.getYear()), eq(ym.getMonthValue())))
                .thenReturn(List.of(a1Payment));
        when(salaryPaymentRepository.findByEmployeeIdInAndPeriodYearAndPeriodMonth(
                argThat(ids -> ids != null && ids.size() == 1), eq(ym.getYear()), eq(ym.getMonthValue())))
                .thenReturn(List.of(a2Payment));

        OwnerCockpitResponse a1Response = service.getOwnerCockpit(currentPeriod, "branch-a1");
        OwnerCockpitResponse a2Response = service.getOwnerCockpit(currentPeriod, "branch-a2");
        OwnerCockpitResponse tenantWide = service.getOwnerCockpit(currentPeriod, null);

        // --- Branch A1: exactly A1's own data, never A2's ---
        assertThat(a1Response.kpiSummary().activeHeadcount()).isEqualTo(2);
        assertThat(a1Response.kpiSummary().cashInHand()).isEqualByComparingTo(BigDecimal.valueOf(10_000));
        assertThat(a1Response.kpiSummary().bankBalances()).isEqualByComparingTo(BigDecimal.valueOf(20_000));
        assertThat(a1Response.kpiSummary().todaySales()).isEqualByComparingTo(BigDecimal.valueOf(1_111));
        assertThat(a1Response.kpiSummary().payrollDisbursed()).isEqualByComparingTo(BigDecimal.valueOf(3_000));
        assertThat(a1Response.branchLeaderboard()).hasSize(1);
        assertThat(a1Response.branchLeaderboard().get(0).branchId()).isEqualTo("branch-a1");

        // --- Branch A2: exactly A2's own data, never A1's ---
        assertThat(a2Response.kpiSummary().activeHeadcount()).isEqualTo(1);
        assertThat(a2Response.kpiSummary().cashInHand()).isEqualByComparingTo(BigDecimal.valueOf(500_000));
        assertThat(a2Response.kpiSummary().bankBalances()).isEqualByComparingTo(BigDecimal.valueOf(700_000));
        assertThat(a2Response.kpiSummary().todaySales()).isEqualByComparingTo(BigDecimal.valueOf(9_999));
        assertThat(a2Response.kpiSummary().payrollDisbursed()).isEqualByComparingTo(BigDecimal.valueOf(70_000));
        assertThat(a2Response.branchLeaderboard()).hasSize(1);
        assertThat(a2Response.branchLeaderboard().get(0).branchId()).isEqualTo("branch-a2");

        // --- Cross-check: neither branch's response ever equals the other's distinctive values ---
        assertThat(a1Response.kpiSummary().cashInHand()).isNotEqualByComparingTo(a2Response.kpiSummary().cashInHand());
        assertThat(a1Response.kpiSummary().todaySales()).isNotEqualByComparingTo(a2Response.kpiSummary().todaySales());
        assertThat(a1Response.kpiSummary().payrollDisbursed()).isNotEqualByComparingTo(a2Response.kpiSummary().payrollDisbursed());

        // --- Tenant-wide (no branchId): a real combined total, not either branch's isolated figure ---
        assertThat(tenantWide.kpiSummary().activeHeadcount()).isEqualTo(3); // 2 (A1) + 1 (A2)
        assertThat(tenantWide.branchLeaderboard()).hasSize(2);
    }

    @Test
    @DisplayName("BRANCH FILTERING: GL-sourced revenue/OPEX/net-profit and AR/AP/top-customers/top-products/manufacturing-WIP have no real branch attribution, so they report real zero/empty when branch-scoped — never the tenant-wide figure and never a fabricated split")
    void branchFilteringReportsHonestZeroForFieldsWithNoRealBranchAttribution() {
        stubEverythingEmpty();
        Branch branch = mock(Branch.class);
        when(branch.getId()).thenReturn("branch-x");
        when(branch.getCode()).thenReturn("X");
        when(branch.getName()).thenReturn("Branch X");
        when(branch.isMainBranch()).thenReturn(true);
        when(authEvaluator.hasBranchAccess("branch-x")).thenReturn(true);
        when(branchRepository.findById("branch-x")).thenReturn(Optional.of(branch));

        // Real, non-zero tenant-wide GL figures exist — a branch-scoped request must NOT leak them.
        // (The service does not even call this when branch-scoped — see getOwnerCockpit step 2 —
        // so this stub is lenient: it documents that real data exists tenant-wide.)
        lenient().when(financialStatementsReportService.getIncomeStatement(any(), any())).thenReturn(
                new FinancialStatementsReportService.IncomeStatementReport(
                        BigDecimal.valueOf(5_000_000), BigDecimal.valueOf(3_000_000), BigDecimal.valueOf(2_000_000)));
        // Real, non-zero tenant-wide open invoices/top-customers exist — a branch-scoped request
        // must not leak them. (The service does not even query this repository when branch-scoped
        // — see getOwnerCockpit step 6 — so this stub is lenient: it documents that real data
        // exists tenant-wide, without asserting the branch-scoped path calls it.)
        CustomerInvoice invoice = mock(CustomerInvoice.class);
        lenient().when(invoice.getOutstandingAmount()).thenReturn(BigDecimal.valueOf(80_000));
        lenient().when(invoice.getDueDate()).thenReturn(LocalDate.now().minusDays(10));
        lenient().when(customerInvoiceRepository.findByOutstandingAmountGreaterThan(any())).thenReturn(List.of(invoice));

        OwnerCockpitResponse response = service.getOwnerCockpit(currentPeriod, "branch-x");

        assertThat(response.kpiSummary().totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.kpiSummary().totalOpex()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.kpiSummary().netProfit()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.kpiSummary().totalReceivables()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.arAging().current().invoiceCount()).isZero();
        assertThat(response.topCustomers()).isEmpty();
        assertThat(response.topProducts()).isEmpty();
        assertThat(response.manufacturingWip()).isEmpty();
        // The branch leaderboard entry itself still reports zero for these same fields, consistent
        // with the tenant-wide leaderboard's existing "no attribution -> zero" convention.
        assertThat(response.branchLeaderboard()).hasSize(1);
        assertThat(response.branchLeaderboard().get(0).revenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("BRANCH FILTERING: project budget/actual (Project.branchId) and expense breakdown (ExpenseClaim.employeeId -> Employee.branchId) are genuinely branch-scoped, not tenant-wide")
    void branchFilteringScopesProjectsAndExpensesViaRealJoins() {
        stubEverythingEmpty();
        Branch branch = mock(Branch.class);
        when(branch.getId()).thenReturn("branch-y");
        when(branch.getCode()).thenReturn("Y");
        when(branch.getName()).thenReturn("Branch Y");
        when(branch.isMainBranch()).thenReturn(true);
        when(authEvaluator.hasBranchAccess("branch-y")).thenReturn(true);
        when(branchRepository.findById("branch-y")).thenReturn(Optional.of(branch));

        // A real project belonging to Branch Y, plus a real tenant-wide project belonging to a
        // DIFFERENT branch that must NOT leak into Branch Y's response.
        Project branchYProject = mock(Project.class);
        when(branchYProject.getId()).thenReturn("proj-y");
        when(branchYProject.getCode()).thenReturn("PRJ-Y");
        when(branchYProject.getName()).thenReturn("Branch Y Project");
        when(branchYProject.getContractValue()).thenReturn(BigDecimal.valueOf(9_000_000));
        when(branchYProject.getStatus()).thenReturn(ProjectStatus.ACTIVE);
        when(projectRepository.findByBranchIdOrderByCreatedAtDesc("branch-y")).thenReturn(List.of(branchYProject));
        // A different branch's project exists tenant-wide (via plain findAll()) — must not appear.
        Project otherBranchProject = mock(Project.class);
        lenient().when(otherBranchProject.getId()).thenReturn("proj-other");
        lenient().when(otherBranchProject.getStatus()).thenReturn(ProjectStatus.ACTIVE);
        lenient().when(projectRepository.findAll()).thenReturn(List.of(otherBranchProject));

        // Real Employee.branchId join for expense claims.
        Employee branchYEmployee = mock(Employee.class);
        when(branchYEmployee.getId()).thenReturn("emp-y");
        when(branchYEmployee.isActive()).thenReturn(true);
        when(employeeRepository.findByBranchId("branch-y")).thenReturn(List.of(branchYEmployee));
        ExpenseClaim branchYExpense = mock(ExpenseClaim.class);
        when(branchYExpense.getCategory()).thenReturn("TRAVEL");
        when(branchYExpense.getAmount()).thenReturn(BigDecimal.valueOf(4_500));
        when(expenseClaimRepository.findByEmployeeIdInAndSpentOnBetween(eq(List.of("emp-y")), any(), any()))
                .thenReturn(List.of(branchYExpense));

        OwnerCockpitResponse response = service.getOwnerCockpit(currentPeriod, "branch-y");

        assertThat(response.projectBudgetControl()).hasSize(1);
        assertThat(response.projectBudgetControl().get(0).projectId()).isEqualTo("proj-y");
        assertThat(response.projectBudgetControl()).noneMatch(p -> "proj-other".equals(p.projectId()));

        assertThat(response.expenseBreakdown()).anyMatch(e -> "TRAVEL".equals(e.category())
                && e.amount().compareTo(BigDecimal.valueOf(4_500)) == 0);
    }

    @Test
    @DisplayName("A branch the caller cannot access is excluded from the leaderboard, and BRANCH_ACCESS_DENIED fires for a direct branch filter")
    void branchAccessDenialIsEnforced() {
        stubEverythingEmpty();
        when(authEvaluator.hasBranchAccess("restricted-branch")).thenReturn(false);

        assertThatThrownBy(() -> service.getOwnerCockpit(currentPeriod, "restricted-branch"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("BRANCH_ACCESS_DENIED");
    }

    @Test
    @DisplayName("Top products are real, from SalesDeliveryLine, never the old qty=150-i*20 index-arithmetic fabrication")
    void topProductsAreRealFromDeliveryLinesNotIndexArithmetic() {
        stubEverythingEmpty();
        // Real inventory items exist — under the old code this alone was enough to trigger fully
        // fabricated "top products" regardless of any real sales data.
        InventoryItem item = mock(InventoryItem.class);
        when(item.getId()).thenReturn("item-1");
        when(inventoryItemRepository.findAll()).thenReturn(List.of(item));

        // No SalesDeliveryLine exists for it (matches stubEverythingEmpty's default).
        OwnerCockpitResponse response = service.getOwnerCockpit(currentPeriod, null);

        assertThat(response.topProducts()).isEmpty();
    }

    @Test
    @DisplayName("getExecutiveOverview scopes POS gross and sales bookings to the requested period, not the tenant's entire history")
    void executiveOverviewScopesPosGrossAndSalesBookingsToThePeriod() {
        stubEverythingEmpty();
        YearMonth ym = YearMonth.parse(currentPeriod);
        long periodStartMs = ym.atDay(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        long periodEndMs = ym.atEndOfMonth().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() - 1;

        // 2026-09-07 remediation (Performance Review P-1, correctness fix found while addressing
        // it): these two figures used to sum the tenant's ENTIRE POS/quotation history regardless
        // of the requested period — a "period" response field that never actually varied by
        // period. A stray, out-of-period value must not leak into this period's response.
        when(posTransactionRepository.sumCompletedInRange(anyLong(), anyLong())).thenReturn(BigDecimal.valueOf(999_999)); // any other range: must not be used
        when(posTransactionRepository.sumCompletedInRange(periodStartMs, periodEndMs)).thenReturn(BigDecimal.valueOf(5_000));

        SalesQuotation quote = mock(SalesQuotation.class);
        when(quote.getTotalAmount()).thenReturn(BigDecimal.valueOf(20_000));
        when(salesQuotationRepository.findByQuoteDateBetween(ym.atDay(1), ym.atEndOfMonth())).thenReturn(List.of(quote));

        ExecutiveOverviewResponse response = service.getExecutiveOverview(currentPeriod);

        assertThat(response.posGross()).isEqualByComparingTo(BigDecimal.valueOf(5_000));
        assertThat(response.salesBookings()).isEqualByComparingTo(BigDecimal.valueOf(20_000));
        verify(salesQuotationRepository, never()).findAll();
    }

    @Test
    @DisplayName("getKpiRegistry returns all semantic definitions")
    void getKpiRegistryReturnsAllSemanticDefinitions() {
        List<KpiDefinitionResponse> registry = service.getKpiRegistry();

        assertThat(registry).isNotEmpty();
        assertThat(registry).hasSizeGreaterThanOrEqualTo(8);
        assertThat(registry).anyMatch(k -> k.key().equals("NET_PROFIT_MARGIN") && k.unit() == KpiUnit.PERCENT);
        assertThat(registry).anyMatch(k -> k.key().equals("ETA_COMPLIANCE_RATE"));
    }

    @Test
    @DisplayName("getExecutiveOverview: an empty tenant gets real zeros, and ETA compliance matches its own documented formula")
    void executiveOverviewComputesEtaComplianceFromRealAcceptedRatio() {
        stubEverythingEmpty();
        EtaInvoiceSubmission valid1 = mock(EtaInvoiceSubmission.class);
        when(valid1.getStatus()).thenReturn(com.bemo.hr.compliance.eta.domain.EtaSubmissionStatus.VALID);
        EtaInvoiceSubmission invalid = mock(EtaInvoiceSubmission.class);
        when(invalid.getStatus()).thenReturn(com.bemo.hr.compliance.eta.domain.EtaSubmissionStatus.INVALID);
        when(etaSubmissionRepository.findAll()).thenReturn(List.of(valid1, invalid));

        ExecutiveOverviewResponse response = service.getExecutiveOverview(currentPeriod);

        // 1 VALID out of 2 total = 50.00%, matching the KPI registry's own documented formula
        // ("Accepted (VALID) ETA Documents / Total Submissions * 100") — previously this was a
        // hardcoded 98.4/100.0 that ignored the real submissions entirely.
        assertThat(response.etaTaxCompliancePercent()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
        assertThat(response.totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.moduleSummaries()).hasSize(6);
        // No FiscalPeriod overlaps this range at all (default empty stub) — honestly reported as
        // NOT_CONFIGURED, not silently ignored (Low Finding L-1).
        assertThat(response.fiscalPeriodStatus()).isEqualTo("NOT_CONFIGURED");
    }

    @Test
    @DisplayName("fiscalPeriodStatus reports OPEN when every overlapping fiscal period is open/soft-closed, and CONTAINS_CLOSED_PERIOD when any is closed or locked")
    void fiscalPeriodStatusReflectsRealFiscalCalendarCoverage() {
        stubEverythingEmpty();
        YearMonth ym = YearMonth.parse(currentPeriod);

        com.bemo.hr.finance.domain.FiscalPeriod openPeriod = new com.bemo.hr.finance.domain.FiscalPeriod(
                ym.getYear(), ym.getMonthValue(), "Test Period", ym.atDay(1), ym.atEndOfMonth(),
                com.bemo.hr.finance.domain.FiscalPeriod.Status.OPEN);
        when(fiscalPeriodRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(ym.atEndOfMonth(), ym.atDay(1)))
                .thenReturn(List.of(openPeriod));

        assertThat(service.getExecutiveOverview(currentPeriod).fiscalPeriodStatus()).isEqualTo("OPEN");

        com.bemo.hr.finance.domain.FiscalPeriod closedPeriod = new com.bemo.hr.finance.domain.FiscalPeriod(
                ym.getYear(), ym.getMonthValue(), "Test Period", ym.atDay(1), ym.atEndOfMonth(),
                com.bemo.hr.finance.domain.FiscalPeriod.Status.CLOSED);
        when(fiscalPeriodRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(ym.atEndOfMonth(), ym.atDay(1)))
                .thenReturn(List.of(closedPeriod));

        assertThat(service.getExecutiveOverview(currentPeriod).fiscalPeriodStatus()).isEqualTo("CONTAINS_CLOSED_PERIOD");
    }

    @Test
    @DisplayName("getComparativeTrends bounds months and returns real (zero, for an empty tenant) series, never the old synthetic growth curve")
    void getComparativeTrendsReturnsRealDataNotASyntheticGrowthCurve() {
        stubEverythingEmpty();

        ComparativeTrendsResponse response = service.getComparativeTrends(6, KpiCategory.FINANCIAL);

        assertThat(response.months()).isEqualTo(6);
        assertThat(response.trendPoints()).hasSize(6);
        // The old fabricated series was ALWAYS positive and ALWAYS growing (rev = 1_200_000 * factor);
        // a real empty tenant must show zero, not a fabricated upward curve.
        assertThat(response.trendPoints()).allMatch(p -> p.revenue().compareTo(BigDecimal.ZERO) == 0);
        assertThat(response.trendPoints()).allMatch(p -> p.netProfit().compareTo(BigDecimal.ZERO) == 0);

        // Bounds test: 1 should clamp to 3
        assertThat(service.getComparativeTrends(1, null).months()).isEqualTo(3);
        // Bounds test: 50 should clamp to 24
        assertThat(service.getComparativeTrends(50, null).months()).isEqualTo(24);
    }

    @Test
    @DisplayName("recordSnapshot upserts an existing period/category/kpiKey row instead of creating a duplicate")
    void recordSnapshotUpsertsRatherThanDuplicating() {
        ExecutiveKpiSnapshot existing = new ExecutiveKpiSnapshot("2026-Q3", KpiCategory.FINANCIAL, "NET_PROFIT_MARGIN",
                BigDecimal.valueOf(25.0), BigDecimal.valueOf(20.0), BigDecimal.valueOf(-5.0), BigDecimal.valueOf(-20.0),
                TrendDirection.DOWN, ReconciliationStatus.RECONCILED, "/finance/accounts", "{}");
        when(snapshotRepository.findByPeriodKeyAndCategoryAndKpiKey("2026-Q3", KpiCategory.FINANCIAL, "NET_PROFIT_MARGIN"))
                .thenReturn(Optional.of(existing));
        when(snapshotRepository.save(any(ExecutiveKpiSnapshot.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateSnapshotPayload payload = new CreateSnapshotPayload("2026-Q3", KpiCategory.FINANCIAL, "NET_PROFIT_MARGIN",
                BigDecimal.valueOf(25.0), BigDecimal.valueOf(28.5), BigDecimal.valueOf(3.5), BigDecimal.valueOf(14.0),
                TrendDirection.UP, ReconciliationStatus.RECONCILED, "/finance/accounts", "{\"audit\":\"verified\"}");

        ExecutiveKpiSnapshotResponse response = service.recordSnapshot(payload);

        assertThat(response.id()).isEqualTo(existing.getId()); // same row, not a new one
        assertThat(response.actualValue()).isEqualByComparingTo(BigDecimal.valueOf(28.5));
        verify(snapshotRepository, never()).save(argThat(s -> !s.getId().equals(existing.getId())));
        verify(snapshotRepository).save(any(ExecutiveKpiSnapshot.class));
    }

    @Test
    @DisplayName("recordSnapshot creates a new row only when none exists for that period/category/kpiKey")
    void recordSnapshotCreatesWhenNoneExists() {
        when(snapshotRepository.findByPeriodKeyAndCategoryAndKpiKey("2026-Q4", KpiCategory.FINANCIAL, "NET_PROFIT_MARGIN"))
                .thenReturn(Optional.empty());
        when(snapshotRepository.save(any(ExecutiveKpiSnapshot.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateSnapshotPayload payload = new CreateSnapshotPayload("2026-Q4", KpiCategory.FINANCIAL, "NET_PROFIT_MARGIN",
                BigDecimal.valueOf(25.0), BigDecimal.valueOf(28.5), BigDecimal.valueOf(3.5), BigDecimal.valueOf(14.0),
                TrendDirection.UP, ReconciliationStatus.RECONCILED, "/finance/accounts", "{}");

        ExecutiveKpiSnapshotResponse response = service.recordSnapshot(payload);

        assertThat(response.periodKey()).isEqualTo("2026-Q4");
        verify(snapshotRepository).save(any(ExecutiveKpiSnapshot.class));
    }

    @Test
    @DisplayName("getTargetsReturnsDefaultWhenNotFound")
    void getTargetsReturnsDefaultWhenNotFound() {
        when(cockpitTargetRepository.findByPeriodKey("2026-Q3")).thenReturn(Optional.empty());

        CockpitTargetResponse response = service.getTargets("2026-Q3");

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("default");
        assertThat(response.periodKey()).isEqualTo("2026-Q3");
        assertThat(response.targetRevenue()).isEqualByComparingTo(BigDecimal.valueOf(1_500_000.00));
    }

    @Test
    @DisplayName("saveTargetsPersistsAndReturnsResponse")
    void saveTargetsPersistsAndReturnsResponse() {
        SaveCockpitTargetRequest request = new SaveCockpitTargetRequest("2026-Q3", BigDecimal.valueOf(1_000_000),
                BigDecimal.valueOf(30.0), BigDecimal.valueOf(200_000), BigDecimal.valueOf(500_000),
                BigDecimal.valueOf(100_000), "Q3 Targets");
        ExecutiveCockpitTarget savedTarget = new ExecutiveCockpitTarget("2026-Q3", BigDecimal.valueOf(1_000_000),
                BigDecimal.valueOf(30.0), BigDecimal.valueOf(200_000), BigDecimal.valueOf(500_000),
                BigDecimal.valueOf(100_000), "Q3 Targets");
        when(cockpitTargetRepository.findByPeriodKey("2026-Q3")).thenReturn(Optional.empty());
        when(cockpitTargetRepository.save(any(ExecutiveCockpitTarget.class))).thenReturn(savedTarget);

        CockpitTargetResponse response = service.saveTargets(request);

        assertThat(response).isNotNull();
        assertThat(response.periodKey()).isEqualTo("2026-Q3");
        assertThat(response.targetRevenue()).isEqualByComparingTo(BigDecimal.valueOf(1_000_000));
        verify(cockpitTargetRepository).save(any(ExecutiveCockpitTarget.class));
    }

    @Test
    @DisplayName("Concurrent target creation for the same not-yet-existing period reports a clean, specific conflict, not a raw 500")
    void concurrentTargetCreationReportsCleanConflict() {
        SaveCockpitTargetRequest request = new SaveCockpitTargetRequest("2026-Q3", BigDecimal.valueOf(1_000_000),
                BigDecimal.valueOf(30.0), BigDecimal.valueOf(200_000), BigDecimal.valueOf(500_000),
                BigDecimal.valueOf(100_000), "Q3 Targets");
        when(cockpitTargetRepository.findByPeriodKey("2026-Q3")).thenReturn(Optional.empty());
        when(cockpitTargetRepository.save(any(ExecutiveCockpitTarget.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> service.saveTargets(request))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("EXECUTIVE_TARGET_CONCURRENT_CREATE");
    }
}
