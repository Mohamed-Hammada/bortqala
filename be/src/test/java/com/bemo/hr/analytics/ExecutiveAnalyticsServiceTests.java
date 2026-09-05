package com.bemo.hr.analytics;

import com.bemo.hr.analytics.api.ExecutiveAnalyticsApi.*;
import com.bemo.hr.analytics.application.ExecutiveAnalyticsService;
import com.bemo.hr.analytics.domain.*;
import com.bemo.hr.analytics.infrastructure.ExecutiveCockpitTargetRepository;
import com.bemo.hr.analytics.infrastructure.ExecutiveKpiSnapshotRepository;
import com.bemo.hr.compliance.eta.infrastructure.EtaInvoiceSubmissionRepository;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.expenses.infrastructure.ExpenseClaimRepository;
import com.bemo.hr.finance.infrastructure.BankAccountRepository;
import com.bemo.hr.finance.infrastructure.CashboxRepository;
import com.bemo.hr.manufacturing.production.infrastructure.ProductionOrderRepository;
import com.bemo.hr.operations.InventoryItem;
import com.bemo.hr.operations.InventoryItemRepository;
import com.bemo.hr.organization.infrastructure.BranchRepository;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.payroll.infrastructure.SalaryPaymentRepository;
import com.bemo.hr.project.domain.Project;
import com.bemo.hr.project.domain.ProjectStatus;
import com.bemo.hr.project.infrastructure.ProjectCostLedgerEntryRepository;
import com.bemo.hr.project.infrastructure.ProjectRepository;
import com.bemo.hr.access.application.SecurityAuthorizationEvaluator;
import com.bemo.hr.trade.pos.domain.PosTransaction;
import com.bemo.hr.trade.pos.infrastructure.PosTransactionRepository;
import com.bemo.hr.trade.procurement.infrastructure.SupplierInvoiceRepository;
import com.bemo.hr.trade.sales.domain.SalesQuotation;
import com.bemo.hr.trade.sales.infrastructure.CustomerInvoiceRepository;
import com.bemo.hr.trade.sales.infrastructure.CustomerReceiptRepository;
import com.bemo.hr.trade.sales.infrastructure.SalesQuotationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutiveAnalyticsServiceTests {

    @Mock
    private ExecutiveKpiSnapshotRepository snapshotRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private InventoryItemRepository inventoryItemRepository;
    @Mock
    private SalesQuotationRepository salesQuotationRepository;
    @Mock
    private PosTransactionRepository posTransactionRepository;
    @Mock
    private EtaInvoiceSubmissionRepository etaSubmissionRepository;
    @Mock
    private CustomerInvoiceRepository customerInvoiceRepository;
    @Mock
    private CustomerReceiptRepository customerReceiptRepository;
    @Mock
    private SupplierInvoiceRepository supplierInvoiceRepository;
    @Mock
    private CashboxRepository cashboxRepository;
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private ProductionOrderRepository productionOrderRepository;
    @Mock
    private ProjectCostLedgerEntryRepository costLedgerRepository;
    @Mock
    private ExpenseClaimRepository expenseClaimRepository;
    @Mock
    private SalaryPaymentRepository salaryPaymentRepository;
    @Mock
    private ExecutiveCockpitTargetRepository cockpitTargetRepository;
    @Mock
    private BusinessPartyRepository businessPartyRepository;
    @Mock
    private SecurityAuthorizationEvaluator authEvaluator;

    private ExecutiveAnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new ExecutiveAnalyticsService(
                snapshotRepository,
                projectRepository,
                employeeRepository,
                inventoryItemRepository,
                salesQuotationRepository,
                posTransactionRepository,
                etaSubmissionRepository,
                customerInvoiceRepository,
                customerReceiptRepository,
                supplierInvoiceRepository,
                cashboxRepository,
                bankAccountRepository,
                branchRepository,
                productionOrderRepository,
                costLedgerRepository,
                expenseClaimRepository,
                salaryPaymentRepository,
                cockpitTargetRepository,
                businessPartyRepository,
                authEvaluator
        );
    }


    @Test
    void getKpiRegistryReturnsAllSemanticDefinitions() {
        List<KpiDefinitionResponse> registry = analyticsService.getKpiRegistry();

        assertThat(registry).isNotEmpty();
        assertThat(registry).hasSizeGreaterThanOrEqualTo(8);
        assertThat(registry).anyMatch(k -> k.key().equals("NET_PROFIT_MARGIN") && k.unit() == KpiUnit.PERCENT);
        assertThat(registry).anyMatch(k -> k.key().equals("OPERATING_CASH_FLOW") && k.category() == KpiCategory.FINANCIAL);
        assertThat(registry).anyMatch(k -> k.key().equals("INVENTORY_VALUATION") && k.sourceModule().contains("Inventory"));
        assertThat(registry).anyMatch(k -> k.key().equals("PROJECT_PORTFOLIO_VALUE") && k.category() == KpiCategory.PROJECTS);
    }

    @Test
    void getExecutiveOverviewAggregatesMultiModuleMetrics() {
        Project mockProject = mock(Project.class);
        when(mockProject.getStatus()).thenReturn(ProjectStatus.ACTIVE);
        when(mockProject.getContractValue()).thenReturn(BigDecimal.valueOf(1_000_000));
        when(projectRepository.findAll()).thenReturn(List.of(mockProject));

        InventoryItem mockItem = mock(InventoryItem.class);
        when(mockItem.getReorderQuantity()).thenReturn(BigDecimal.valueOf(100));
        when(inventoryItemRepository.findAll()).thenReturn(List.of(mockItem));

        PosTransaction mockPos = mock(PosTransaction.class);
        when(mockPos.getTotalAmount()).thenReturn(BigDecimal.valueOf(15_000));
        when(posTransactionRepository.findAll()).thenReturn(List.of(mockPos));

        SalesQuotation mockQuote = mock(SalesQuotation.class);
        when(mockQuote.getTotalAmount()).thenReturn(BigDecimal.valueOf(60_000));
        when(salesQuotationRepository.findAll()).thenReturn(List.of(mockQuote));

        Employee mockEmp = mock(Employee.class);
        when(mockEmp.isActive()).thenReturn(true);
        when(employeeRepository.findAll()).thenReturn(List.of(mockEmp));

        when(etaSubmissionRepository.findAll()).thenReturn(List.of());

        ExecutiveOverviewResponse response = analyticsService.getExecutiveOverview("2026-08", null, null, null);

        assertThat(response).isNotNull();
        assertThat(response.period()).isEqualTo("2026-08");
        assertThat(response.projectPortfolioValue()).isEqualByComparingTo(BigDecimal.valueOf(1_000_000));
        assertThat(response.inventoryValuation()).isEqualByComparingTo(BigDecimal.valueOf(15_000));
        assertThat(response.posGross()).isEqualByComparingTo(BigDecimal.valueOf(15_000));
        assertThat(response.salesBookings()).isEqualByComparingTo(BigDecimal.valueOf(60_000));
        assertThat(response.activeHeadcount()).isEqualTo(1);
        assertThat(response.moduleSummaries()).hasSize(6);
    }

    @Test
    void getComparativeTrendsBoundsMonthsAndReturnsSeries() {
        ComparativeTrendsResponse response = analyticsService.getComparativeTrends(6, KpiCategory.FINANCIAL);

        assertThat(response.months()).isEqualTo(6);
        assertThat(response.trendPoints()).hasSize(6);
        assertThat(response.trendPoints().get(0).revenue()).isPositive();
        assertThat(response.trendPoints().get(0).netProfit()).isPositive();

        // Bounds test: 1 should clamp to 3
        ComparativeTrendsResponse bounded3 = analyticsService.getComparativeTrends(1, null);
        assertThat(bounded3.months()).isEqualTo(3);
        assertThat(bounded3.trendPoints()).hasSize(3);

        // Bounds test: 50 should clamp to 24
        ComparativeTrendsResponse bounded24 = analyticsService.getComparativeTrends(50, null);
        assertThat(bounded24.months()).isEqualTo(24);
        assertThat(bounded24.trendPoints()).hasSize(24);
    }

    @Test
    void recordSnapshotSavesAndReturnsResponse() {
        CreateSnapshotPayload payload = new CreateSnapshotPayload(
                "2026-Q3",
                KpiCategory.FINANCIAL,
                "NET_PROFIT_MARGIN",
                BigDecimal.valueOf(25.0),
                BigDecimal.valueOf(28.5),
                BigDecimal.valueOf(3.5),
                BigDecimal.valueOf(14.0),
                TrendDirection.UP,
                ReconciliationStatus.RECONCILED,
                "/finance/accounts",
                "{\"audit\":\"verified\"}"
        );

        when(snapshotRepository.save(any(ExecutiveKpiSnapshot.class))).thenAnswer(inv -> inv.getArgument(0));

        ExecutiveKpiSnapshotResponse response = analyticsService.recordSnapshot(payload);

        assertThat(response).isNotNull();
        assertThat(response.periodKey()).isEqualTo("2026-Q3");
        assertThat(response.category()).isEqualTo(KpiCategory.FINANCIAL);
        assertThat(response.kpiKey()).isEqualTo("NET_PROFIT_MARGIN");
        assertThat(response.actualValue()).isEqualByComparingTo(BigDecimal.valueOf(28.5));
        assertThat(response.reconciliationStatus()).isEqualTo(ReconciliationStatus.RECONCILED);
        verify(snapshotRepository).save(any(ExecutiveKpiSnapshot.class));
    }

    @Test
    void getTargetsReturnsDefaultWhenNotFound() {
        when(cockpitTargetRepository.findByPeriodKey("2026-Q3")).thenReturn(Optional.empty());

        CockpitTargetResponse response = analyticsService.getTargets("2026-Q3");

        assertThat(response).isNotNull();
        assertThat(response.periodKey()).isEqualTo("2026-Q3");
        assertThat(response.targetRevenue()).isEqualByComparingTo(BigDecimal.valueOf(1_500_000.00));
    }

    @Test
    void saveTargetsPersistsAndReturnsResponse() {
        SaveCockpitTargetRequest request = new SaveCockpitTargetRequest(
                "2026-Q3",
                BigDecimal.valueOf(1_000_000),
                BigDecimal.valueOf(30.0),
                BigDecimal.valueOf(200_000),
                BigDecimal.valueOf(500_000),
                BigDecimal.valueOf(100_000),
                "Q3 Targets"
        );
        ExecutiveCockpitTarget savedTarget = new ExecutiveCockpitTarget(
                "2026-Q3",
                BigDecimal.valueOf(1_000_000),
                BigDecimal.valueOf(30.0),
                BigDecimal.valueOf(200_000),
                BigDecimal.valueOf(500_000),
                BigDecimal.valueOf(100_000),
                "Q3 Targets"
        );
        when(cockpitTargetRepository.findByPeriodKey("2026-Q3")).thenReturn(Optional.empty());
        when(cockpitTargetRepository.save(any(ExecutiveCockpitTarget.class))).thenReturn(savedTarget);

        CockpitTargetResponse response = analyticsService.saveTargets(request);

        assertThat(response).isNotNull();
        assertThat(response.periodKey()).isEqualTo("2026-Q3");
        assertThat(response.targetRevenue()).isEqualByComparingTo(BigDecimal.valueOf(1_000_000));
        verify(cockpitTargetRepository).save(any(ExecutiveCockpitTarget.class));
    }

    @Test
    void getOwnerCockpitAggregatesAllKpis() {
        lenient().when(customerInvoiceRepository.findAll()).thenReturn(List.of());
        lenient().when(customerReceiptRepository.findAll()).thenReturn(List.of());
        lenient().when(supplierInvoiceRepository.findAll()).thenReturn(List.of());
        lenient().when(cashboxRepository.findAll()).thenReturn(List.of());
        lenient().when(bankAccountRepository.findAll()).thenReturn(List.of());
        lenient().when(branchRepository.findAll()).thenReturn(List.of());
        lenient().when(inventoryItemRepository.findAll()).thenReturn(List.of());
        lenient().when(posTransactionRepository.findAll()).thenReturn(List.of());
        lenient().when(productionOrderRepository.findAll()).thenReturn(List.of());
        lenient().when(costLedgerRepository.findAll()).thenReturn(List.of());
        lenient().when(expenseClaimRepository.findAll()).thenReturn(List.of());
        lenient().when(salaryPaymentRepository.findAll()).thenReturn(List.of());
        lenient().when(projectRepository.findAll()).thenReturn(List.of());
        lenient().when(cockpitTargetRepository.findByPeriodKey(any())).thenReturn(Optional.empty());

        OwnerCockpitResponse response = analyticsService.getOwnerCockpit("2026-09", null, null);

        assertThat(response).isNotNull();
        assertThat(response.period()).isEqualTo("2026-09");
        assertThat(response.kpiSummary()).isNotNull();
        assertThat(response.arAging()).isNotNull();
        assertThat(response.apAging()).isNotNull();
    }

    @Test
    void exportExecutiveCockpitExcelGeneratesValidWorkbook() {
        lenient().when(customerInvoiceRepository.findAll()).thenReturn(List.of());
        lenient().when(customerReceiptRepository.findAll()).thenReturn(List.of());
        lenient().when(supplierInvoiceRepository.findAll()).thenReturn(List.of());
        lenient().when(cashboxRepository.findAll()).thenReturn(List.of());
        lenient().when(bankAccountRepository.findAll()).thenReturn(List.of());
        lenient().when(branchRepository.findAll()).thenReturn(List.of());
        lenient().when(inventoryItemRepository.findAll()).thenReturn(List.of());
        lenient().when(posTransactionRepository.findAll()).thenReturn(List.of());
        lenient().when(productionOrderRepository.findAll()).thenReturn(List.of());
        lenient().when(costLedgerRepository.findAll()).thenReturn(List.of());
        lenient().when(expenseClaimRepository.findAll()).thenReturn(List.of());
        lenient().when(salaryPaymentRepository.findAll()).thenReturn(List.of());
        lenient().when(projectRepository.findAll()).thenReturn(List.of());
        lenient().when(cockpitTargetRepository.findByPeriodKey(any())).thenReturn(Optional.empty());

        byte[] bytes = analyticsService.exportExecutiveCockpitExcel("2026-09", null, null);

        assertThat(bytes).isNotNull();
        assertThat(bytes.length).isGreaterThan(100);
    }
}

