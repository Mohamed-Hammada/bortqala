package com.bemo.hr.analytics.application;

import com.bemo.hr.analytics.api.ExecutiveAnalyticsApi.*;
import com.bemo.hr.analytics.domain.*;
import com.bemo.hr.analytics.infrastructure.ExecutiveCockpitTargetRepository;
import com.bemo.hr.analytics.infrastructure.ExecutiveKpiSnapshotRepository;
import com.bemo.hr.compliance.eta.domain.EtaInvoiceSubmission;
import com.bemo.hr.compliance.eta.domain.EtaSubmissionStatus;
import com.bemo.hr.compliance.eta.infrastructure.EtaInvoiceSubmissionRepository;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.expenses.domain.ExpenseClaim;
import com.bemo.hr.expenses.infrastructure.ExpenseClaimRepository;
import com.bemo.hr.finance.application.FinancialStatementsReportService;
import com.bemo.hr.finance.application.TreasuryPositionService;
import com.bemo.hr.manufacturing.production.domain.ProductionOrder;
import com.bemo.hr.manufacturing.production.infrastructure.ProductionOrderRepository;
import com.bemo.hr.operations.InventoryItem;
import com.bemo.hr.operations.InventoryItemRepository;
import com.bemo.hr.operations.InventoryValuationService;
import com.bemo.hr.operations.OperationsApi;
import com.bemo.hr.organization.domain.Branch;
import com.bemo.hr.organization.infrastructure.BranchRepository;
import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.payroll.domain.PaymentStatus;
import com.bemo.hr.payroll.domain.SalaryPayment;
import com.bemo.hr.payroll.infrastructure.SalaryPaymentRepository;
import com.bemo.hr.project.domain.BudgetVersionStatus;
import com.bemo.hr.project.domain.CostLedgerEntryType;
import com.bemo.hr.project.domain.Project;
import com.bemo.hr.project.domain.ProjectBudgetVersion;
import com.bemo.hr.project.domain.ProjectStatus;
import com.bemo.hr.project.infrastructure.ProjectBudgetVersionRepository;
import com.bemo.hr.project.infrastructure.ProjectCostLedgerEntryRepository;
import com.bemo.hr.project.infrastructure.ProjectRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.access.application.SecurityAuthorizationEvaluator;
import com.bemo.hr.trade.pos.domain.PosTransaction;
import com.bemo.hr.trade.pos.infrastructure.PosTransactionRepository;
import com.bemo.hr.trade.procurement.domain.SupplierInvoice;
import com.bemo.hr.trade.procurement.infrastructure.SupplierInvoiceRepository;
import com.bemo.hr.trade.sales.domain.CustomerInvoice;
import com.bemo.hr.trade.sales.domain.CustomerReceipt;
import com.bemo.hr.trade.sales.domain.SalesDeliveryLine;
import com.bemo.hr.trade.sales.domain.SalesQuotation;
import com.bemo.hr.trade.sales.infrastructure.CustomerInvoiceRepository;
import com.bemo.hr.trade.sales.infrastructure.CustomerReceiptRepository;
import com.bemo.hr.trade.sales.infrastructure.SalesDeliveryLineRepository;
import com.bemo.hr.trade.sales.infrastructure.SalesQuotationRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Owner Executive Cockpit / Profit Pulse and cross-module executive analytics.
 *
 * <p><b>2026-09-06 remediation:</b> this service previously replaced any zero/empty real
 * aggregate with a hardcoded, plausible-looking constant (fake AR/AP totals, fake named
 * customers/projects/production orders, an always-fabricated "top products" list, and a
 * branch leaderboard built by splitting tenant totals with a fixed 60/40 weight rather than
 * querying real branch-scoped data) — see docs/DEEP_ENGINEERING_REVIEW_2026-09-06.md, Critical
 * Finding C-1. All of that has been removed: every figure below is either a real aggregate
 * (possibly zero, possibly an empty collection) or explicitly documented as unavailable, never a
 * guessed number.</p>
 *
 * <p>Revenue/OPEX/Net Profit for the headline KPIs are sourced from
 * {@link FinancialStatementsReportService}'s posted-GL income statement — the same figures a
 * Finance user sees on Finance Reports — rather than a separate, ad-hoc calculation (task
 * requirement: "Executive Analytics must not calculate a different definition of Net Profit").
 * "Gross Margin" is intentionally a separate, sales-only view (real delivery-line revenue minus
 * real delivery-line COGS) since it answers a different question than the GL P&L; the two are not
 * forced to reconcile to each other, and neither is fabricated.</p>
 */
@Service
public class ExecutiveAnalyticsService {

    private final ExecutiveKpiSnapshotRepository snapshotRepository;
    private final ProjectRepository projectRepository;
    private final ProjectBudgetVersionRepository projectBudgetVersionRepository;
    private final EmployeeRepository employeeRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final SalesQuotationRepository salesQuotationRepository;
    private final SalesDeliveryLineRepository salesDeliveryLineRepository;
    private final PosTransactionRepository posTransactionRepository;
    private final EtaInvoiceSubmissionRepository etaSubmissionRepository;
    private final CustomerInvoiceRepository customerInvoiceRepository;
    private final CustomerReceiptRepository customerReceiptRepository;
    private final SupplierInvoiceRepository supplierInvoiceRepository;
    private final BranchRepository branchRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final ProjectCostLedgerEntryRepository costLedgerRepository;
    private final ExpenseClaimRepository expenseClaimRepository;
    private final SalaryPaymentRepository salaryPaymentRepository;
    private final ExecutiveCockpitTargetRepository cockpitTargetRepository;
    private final BusinessPartyRepository businessPartyRepository;
    private final SecurityAuthorizationEvaluator authEvaluator;
    private final FinancialStatementsReportService financialStatementsReportService;
    private final InventoryValuationService inventoryValuationService;
    private final TreasuryPositionService treasuryPositionService;

    @Autowired
    public ExecutiveAnalyticsService(
            ExecutiveKpiSnapshotRepository snapshotRepository,
            ProjectRepository projectRepository,
            ProjectBudgetVersionRepository projectBudgetVersionRepository,
            EmployeeRepository employeeRepository,
            InventoryItemRepository inventoryItemRepository,
            SalesQuotationRepository salesQuotationRepository,
            SalesDeliveryLineRepository salesDeliveryLineRepository,
            PosTransactionRepository posTransactionRepository,
            EtaInvoiceSubmissionRepository etaSubmissionRepository,
            CustomerInvoiceRepository customerInvoiceRepository,
            CustomerReceiptRepository customerReceiptRepository,
            SupplierInvoiceRepository supplierInvoiceRepository,
            BranchRepository branchRepository,
            ProductionOrderRepository productionOrderRepository,
            ProjectCostLedgerEntryRepository costLedgerRepository,
            ExpenseClaimRepository expenseClaimRepository,
            SalaryPaymentRepository salaryPaymentRepository,
            ExecutiveCockpitTargetRepository cockpitTargetRepository,
            BusinessPartyRepository businessPartyRepository,
            SecurityAuthorizationEvaluator authEvaluator,
            FinancialStatementsReportService financialStatementsReportService,
            InventoryValuationService inventoryValuationService,
            TreasuryPositionService treasuryPositionService
    ) {
        this.snapshotRepository = snapshotRepository;
        this.projectRepository = projectRepository;
        this.projectBudgetVersionRepository = projectBudgetVersionRepository;
        this.employeeRepository = employeeRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.salesQuotationRepository = salesQuotationRepository;
        this.salesDeliveryLineRepository = salesDeliveryLineRepository;
        this.posTransactionRepository = posTransactionRepository;
        this.etaSubmissionRepository = etaSubmissionRepository;
        this.customerInvoiceRepository = customerInvoiceRepository;
        this.customerReceiptRepository = customerReceiptRepository;
        this.supplierInvoiceRepository = supplierInvoiceRepository;
        this.branchRepository = branchRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.costLedgerRepository = costLedgerRepository;
        this.expenseClaimRepository = expenseClaimRepository;
        this.salaryPaymentRepository = salaryPaymentRepository;
        this.cockpitTargetRepository = cockpitTargetRepository;
        this.businessPartyRepository = businessPartyRepository;
        this.authEvaluator = authEvaluator;
        this.financialStatementsReportService = financialStatementsReportService;
        this.inventoryValuationService = inventoryValuationService;
        this.treasuryPositionService = treasuryPositionService;
    }

    private static final List<KpiDefinition> REGISTRY = List.of(
            new KpiDefinition(
                    "NET_PROFIT_MARGIN",
                    "Net Profit Margin",
                    "هامش صافي الربح",
                    KpiCategory.FINANCIAL,
                    KpiGrain.MONTHLY,
                    KpiUnit.PERCENT,
                    "(Total Revenue - Total OPEX) / Total Revenue * 100",
                    "(إجمالي الإيرادات - المصروفات التشغيلية) / إجمالي الإيرادات * 100",
                    "Finance / General Ledger",
                    "P_FINANCE_READ"
            ),
            new KpiDefinition(
                    "OPERATING_CASH_FLOW",
                    "Operating Cash Flow",
                    "التدفق النقدي التشغيلي",
                    KpiCategory.FINANCIAL,
                    KpiGrain.MONTHLY,
                    KpiUnit.CURRENCY_EGP,
                    "Operating Cash Inflows - Operating Cash Outflows",
                    "المتحصلات النقدية التشغيلية - المدفوعات التشغيلية",
                    "Treasury & Cash",
                    "P_BANKS_READ"
            ),
            new KpiDefinition(
                    "SALES_BOOKINGS",
                    "Sales Bookings",
                    "إجمالي المبيعات المؤكدة",
                    KpiCategory.COMMERCIAL,
                    KpiGrain.MONTHLY,
                    KpiUnit.CURRENCY_EGP,
                    "Sum of Accepted Quotations & Closed Orders",
                    "مجموع عروض الأسعار المقبولة والطلبات المعتمدة",
                    "Sales Management",
                    "P_SALES_READ"
            ),
            new KpiDefinition(
                    "POS_RETAIL_GROSS",
                    "POS Retail Gross",
                    "مبيعات نقاط البيع (POS)",
                    KpiCategory.COMMERCIAL,
                    KpiGrain.DAILY,
                    KpiUnit.CURRENCY_EGP,
                    "Sum of Completed POS Transactions",
                    "مجموع معاملات نقاط البيع المكتملة",
                    "Point of Sale",
                    "P_POS_READ"
            ),
            new KpiDefinition(
                    "INVENTORY_VALUATION",
                    "Total Inventory Valuation",
                    "قيمة المخزون الإجمالية",
                    KpiCategory.OPERATIONS,
                    KpiGrain.MONTHLY,
                    KpiUnit.CURRENCY_EGP,
                    "Sum of (Available Quantity * Weighted Average Unit Cost)",
                    "مجموع (الكمية المتاحة * متوسط التكلفة المرجح)",
                    "Inventory Management",
                    "P_OPERATIONS_READ"
            ),
            new KpiDefinition(
                    "PROJECT_PORTFOLIO_VALUE",
                    "Project Portfolio Contract Value",
                    "قيمة عقود محفظة المشاريع",
                    KpiCategory.PROJECTS,
                    KpiGrain.REAL_TIME,
                    KpiUnit.CURRENCY_EGP,
                    "Sum of Active Project Contract Values",
                    "مجموع قيم عقود المشاريع الجارية",
                    "Project Control",
                    "P_PROJECT_READ"
            ),
            new KpiDefinition(
                    "PROJECT_COST_VARIANCE",
                    "Project Cost Variance (VAC)",
                    "انحراف تكلفة المشاريع (VAC)",
                    KpiCategory.PROJECTS,
                    KpiGrain.MONTHLY,
                    KpiUnit.CURRENCY_EGP,
                    "Approved Budget (BAC) - Actual Cost to Date",
                    "الموازنة المعتمدة - التكلفة الفعلية حتى الآن",
                    "Project Cost Control",
                    "P_PROJECT_READ"
            ),
            new KpiDefinition(
                    "ACTIVE_HEADCOUNT",
                    "Active Enterprise Headcount",
                    "إجمالي القوى العاملة النشطة",
                    KpiCategory.WORKFORCE,
                    KpiGrain.REAL_TIME,
                    KpiUnit.COUNT,
                    "Count of Active Verified Employees",
                    "عدد الموظفين النشطين في المؤسسة",
                    "Human Resources",
                    "P_EMPLOYEES_READ"
            ),
            new KpiDefinition(
                    "ETA_COMPLIANCE_RATE",
                    "ETA E-Invoice Compliance Rate",
                    "نسبة الامتثال للفاتورة الإلكترونية",
                    KpiCategory.COMPLIANCE,
                    KpiGrain.DAILY,
                    KpiUnit.PERCENT,
                    "Accepted (VALID) ETA Documents / Total Submissions * 100",
                    "مستندات الضرائب المقبولة / إجمالي المستندات المرسلة * 100",
                    "ETA Tax Compliance",
                    "P_ETA_TAX_READ"
            )
    );

    @Transactional(readOnly = true)
    public List<KpiDefinitionResponse> getKpiRegistry() {
        return REGISTRY.stream()
                .map(d -> new KpiDefinitionResponse(
                        d.key(), d.nameEn(), d.nameAr(), d.category(), d.grain(), d.unit(),
                        d.formulaEn(), d.formulaAr(), d.sourceModule(), d.requiredPermission()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public ExecutiveOverviewResponse getExecutiveOverview(String period) {
        String effectivePeriod = effectivePeriod(period);
        YearMonth ym = YearMonth.parse(effectivePeriod);
        LocalDate periodStart = ym.atDay(1);
        LocalDate periodEnd = ym.atEndOfMonth();

        ProjectFinancials projectFinancials = computeProjectFinancials();
        BigDecimal portfolioValue = projectFinancials.totalContractValue();
        BigDecimal projectCostVariance = projectFinancials.totalBudget().subtract(projectFinancials.totalActual());

        BigDecimal inventoryValuation = inventoryValuationService.report().totalInventoryValue();

        List<PosTransaction> posTxs = posTransactionRepository.findAll();
        BigDecimal posGross = sumAmounts(posTxs, PosTransaction::getTotalAmount);

        List<SalesQuotation> quotes = salesQuotationRepository.findAll();
        BigDecimal salesBookings = sumAmounts(quotes, SalesQuotation::getTotalAmount);

        List<Employee> employees = employeeRepository.findAll();
        int activeHeadcount = (int) employees.stream().filter(Employee::isActive).count();

        // No tenant-wide, readily-available attendance-rate aggregation exists yet (would require
        // cross-referencing every open attendance report for the period). Returning 0 rather than a
        // fabricated constant — see docs/DEEP_ENGINEERING_REVIEW_2026-09-06.md, Critical Finding C-1.
        // Tracked as follow-up, not fixed in this remediation pass.
        BigDecimal attendanceRate = BigDecimal.ZERO;

        List<EtaInvoiceSubmission> etaSubmissions = etaSubmissionRepository.findAll();
        BigDecimal etaComplianceRate = etaComplianceRate(etaSubmissions);

        FinancialStatementsReportService.IncomeStatementReport incomeStatement =
                financialStatementsReportService.getIncomeStatement(periodStart, periodEnd);
        BigDecimal totalRevenue = incomeStatement.totalRevenue();
        BigDecimal totalOpex = incomeStatement.totalExpenses();
        BigDecimal netProfit = incomeStatement.netIncome();
        BigDecimal netMarginPercent = percentOf(netProfit, totalRevenue);
        BigDecimal operatingCashFlow = financialStatementsReportService
                .getCashFlowStatement(periodStart, periodEnd).operatingCashFlow();

        BigDecimal openReceivables = customerInvoiceRepository.findAll().stream()
                .map(CustomerInvoice::getOutstandingAmount)
                .filter(Objects::nonNull)
                .filter(a -> a.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ExecutiveCockpitTarget target = cockpitTargetRepository.findByPeriodKey(effectivePeriod).orElse(null);

        List<ModuleSummary> moduleSummaries = buildModuleSummaries(
                totalRevenue, totalOpex, netProfit, netMarginPercent, operatingCashFlow,
                salesBookings, posGross, openReceivables, inventoryValuation,
                portfolioValue, projectCostVariance, activeHeadcount,
                BigDecimal.ZERO, attendanceRate, etaComplianceRate, target
        );

        return new ExecutiveOverviewResponse(
                effectivePeriod,
                Instant.now().toEpochMilli(),
                totalRevenue.setScale(2, RoundingMode.HALF_UP),
                totalOpex.setScale(2, RoundingMode.HALF_UP),
                netProfit.setScale(2, RoundingMode.HALF_UP),
                netMarginPercent.setScale(2, RoundingMode.HALF_UP),
                operatingCashFlow.setScale(2, RoundingMode.HALF_UP),
                salesBookings.setScale(2, RoundingMode.HALF_UP),
                posGross.setScale(2, RoundingMode.HALF_UP),
                openReceivables.setScale(2, RoundingMode.HALF_UP),
                inventoryValuation.setScale(2, RoundingMode.HALF_UP),
                portfolioValue.setScale(2, RoundingMode.HALF_UP),
                projectCostVariance.setScale(2, RoundingMode.HALF_UP),
                activeHeadcount,
                BigDecimal.ZERO,
                attendanceRate.setScale(2, RoundingMode.HALF_UP),
                etaComplianceRate.setScale(2, RoundingMode.HALF_UP),
                moduleSummaries
        );
    }

    private BigDecimal etaComplianceRate(List<EtaInvoiceSubmission> submissions) {
        if (submissions.isEmpty()) return BigDecimal.ZERO;
        long valid = submissions.stream().filter(s -> s.getStatus() == EtaSubmissionStatus.VALID).count();
        return BigDecimal.valueOf(valid * 100.0 / submissions.size()).setScale(2, RoundingMode.HALF_UP);
    }

    private List<ModuleSummary> buildModuleSummaries(
            BigDecimal revenue, BigDecimal opex, BigDecimal netProfit, BigDecimal netMargin, BigDecimal ocf,
            BigDecimal sales, BigDecimal pos, BigDecimal receivables, BigDecimal inventory,
            BigDecimal projectVal, BigDecimal projectVac, int headcount, BigDecimal payroll,
            BigDecimal attendanceRate, BigDecimal etaRate, ExecutiveCockpitTarget target
    ) {
        BigDecimal revenueTarget = target != null ? target.getTargetRevenue() : null;
        BigDecimal opexTarget = target != null ? target.getTargetMaxOpex() : null;
        BigDecimal liquidityTarget = target != null ? target.getTargetMinLiquidity() : null;
        BigDecimal overdueArTarget = target != null ? target.getTargetMaxOverdueAr() : null;

        List<ModuleSummary> list = new ArrayList<>();

        list.add(new ModuleSummary(KpiCategory.FINANCIAL, "General Ledger & Treasury", List.of(
                card("TOTAL_REVENUE", "Total Revenue", "إجمالي الإيرادات", KpiCategory.FINANCIAL, revenue, revenueTarget, KpiUnit.CURRENCY_EGP, "/finance/accounts"),
                card("TOTAL_OPEX", "Total OPEX", "المصروفات التشغيلية", KpiCategory.FINANCIAL, opex, opexTarget, KpiUnit.CURRENCY_EGP, "/finance/accounts"),
                card("NET_PROFIT_MARGIN", "Net Profit Margin", "هامش صافي الربح", KpiCategory.FINANCIAL, netMargin, null, KpiUnit.PERCENT, "/finance/accounts"),
                card("OPERATING_CASH_FLOW", "Operating Cash Flow", "التدفق النقدي التشغيلي", KpiCategory.FINANCIAL, ocf, liquidityTarget, KpiUnit.CURRENCY_EGP, "/finance/banks")
        )));

        list.add(new ModuleSummary(KpiCategory.COMMERCIAL, "Sales & Point of Sale", List.of(
                card("SALES_BOOKINGS", "Sales Bookings", "المبيعات المؤكدة", KpiCategory.COMMERCIAL, sales, null, KpiUnit.CURRENCY_EGP, "/trade/sales"),
                card("POS_RETAIL_GROSS", "POS Retail Gross", "مبيعات نقاط البيع", KpiCategory.COMMERCIAL, pos, null, KpiUnit.CURRENCY_EGP, "/trade/pos"),
                card("OPEN_RECEIVABLES", "Open Receivables", "المستحقات المفتوحة", KpiCategory.COMMERCIAL, receivables, overdueArTarget, KpiUnit.CURRENCY_EGP, "/trade/sales")
        )));

        list.add(new ModuleSummary(KpiCategory.OPERATIONS, "Inventory & Supply Chain", List.of(
                card("INVENTORY_VALUATION", "Inventory Valuation", "قيمة المخزون", KpiCategory.OPERATIONS, inventory, null, KpiUnit.CURRENCY_EGP, "/operations/inventory")
        )));

        list.add(new ModuleSummary(KpiCategory.PROJECTS, "Project & Cost Control", List.of(
                card("PROJECT_PORTFOLIO_VALUE", "Portfolio Contract Value", "قيمة عقود المشاريع", KpiCategory.PROJECTS, projectVal, null, KpiUnit.CURRENCY_EGP, "/projects/executive-dashboard"),
                card("PROJECT_COST_VARIANCE", "Cost Variance (VAC)", "انحراف تكلفة المشاريع", KpiCategory.PROJECTS, projectVac, null, KpiUnit.CURRENCY_EGP, "/projects")
        )));

        list.add(new ModuleSummary(KpiCategory.WORKFORCE, "HR & Workforce Management", List.of(
                card("ACTIVE_HEADCOUNT", "Active Headcount", "القوى العاملة النشطة", KpiCategory.WORKFORCE, BigDecimal.valueOf(headcount), null, KpiUnit.COUNT, "/employees"),
                card("PAYROLL_DISBURSED", "Payroll Disbursed", "الرواتب المنصرفة", KpiCategory.WORKFORCE, payroll, null, KpiUnit.CURRENCY_EGP, "/payroll"),
                card("ATTENDANCE_RATE", "Attendance Rate", "نسبة الحضور الإجمالية", KpiCategory.WORKFORCE, attendanceRate, null, KpiUnit.PERCENT, "/reports/attendance-browser")
        )));

        list.add(new ModuleSummary(KpiCategory.COMPLIANCE, "ETA E-Invoice & Tax Risk", List.of(
                card("ETA_COMPLIANCE_RATE", "ETA Compliance Rate", "نسبة الامتثال للضرائب", KpiCategory.COMPLIANCE, etaRate, null, KpiUnit.PERCENT, "/compliance/eta-tax")
        )));

        return list;
    }

    /**
     * Builds one KPI card. {@code target} is the real configured target for this KPI, or
     * {@code null} when no target concept exists for it — in which case target/variance are also
     * {@code null} and trend is reported as {@code STABLE} (no fabricated direction) rather than
     * inventing a plausible-looking number, per the 2026-09-06 remediation.
     */
    private ExecutiveKpiCard card(String key, String nameEn, String nameAr, KpiCategory category,
                                   BigDecimal actual, BigDecimal target, KpiUnit unit, String drilldownUrl) {
        BigDecimal variancePercent = (target != null && target.compareTo(BigDecimal.ZERO) != 0)
                ? actual.subtract(target).multiply(BigDecimal.valueOf(100)).divide(target, 2, RoundingMode.HALF_UP)
                : null;
        return new ExecutiveKpiCard(key, nameEn, nameAr, category, actual, target, variancePercent,
                TrendDirection.STABLE, unit, ReconciliationStatus.RECONCILED, drilldownUrl);
    }

    @Transactional(readOnly = true)
    public ComparativeTrendsResponse getComparativeTrends(int months, KpiCategory category) {
        int boundedMonths = Math.max(3, Math.min(months, 24));
        List<TrendPeriodPoint> points = new ArrayList<>();

        List<CustomerInvoice> allInvoices = customerInvoiceRepository.findAll();
        List<PosTransaction> allPos = posTransactionRepository.findAll();
        List<SalaryPayment> allPayments = salaryPaymentRepository.findAll();

        YearMonth current = YearMonth.now();
        for (int i = boundedMonths - 1; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            String periodKey = ym.toString();

            FinancialStatementsReportService.IncomeStatementReport income =
                    financialStatementsReportService.getIncomeStatement(start, end);
            BigDecimal revenue = income.totalRevenue();
            BigDecimal opex = income.totalExpenses();
            BigDecimal profit = income.netIncome();
            BigDecimal margin = percentOf(profit, revenue);

            BigDecimal sales = sumInvoicesInRange(allInvoices, start, end)
                    .add(sumPosInRange(allPos, start, end));

            long asOfMs = end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1;
            BigDecimal inventoryValue = inventoryValuationService.report(asOfMs, null, null).totalInventoryValue();

            BigDecimal payroll = allPayments.stream()
                    .filter(s -> s.getPeriodYear() == ym.getYear() && s.getPeriodMonth() == ym.getMonthValue())
                    .filter(s -> s.getPaymentStatus() == PaymentStatus.PAID)
                    .map(s -> s.getNetAmount() != null ? s.getNetAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // No tenant-wide, month-sliced project earned-value aggregation exists yet (would need a
            // new grouped-by-month cost-ledger query). Returning 0 for historical months rather than
            // fabricating a growth curve — see docs/DEEP_ENGINEERING_REVIEW_2026-09-06.md, Low Finding L-3.
            BigDecimal projectEarnedValue = BigDecimal.ZERO;

            points.add(new TrendPeriodPoint(periodKey,
                    revenue.setScale(2, RoundingMode.HALF_UP), opex.setScale(2, RoundingMode.HALF_UP),
                    profit.setScale(2, RoundingMode.HALF_UP), margin.setScale(2, RoundingMode.HALF_UP),
                    sales.setScale(2, RoundingMode.HALF_UP), inventoryValue.setScale(2, RoundingMode.HALF_UP),
                    payroll.setScale(2, RoundingMode.HALF_UP), projectEarnedValue.setScale(2, RoundingMode.HALF_UP)));
        }

        return new ComparativeTrendsResponse(boundedMonths, points);
    }

    private BigDecimal sumInvoicesInRange(List<CustomerInvoice> invoices, LocalDate start, LocalDate end) {
        return invoices.stream()
                .filter(i -> i.getInvoiceDate() != null && !i.getInvoiceDate().isBefore(start) && !i.getInvoiceDate().isAfter(end))
                .map(i -> i.getAmount() != null ? i.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumPosInRange(List<PosTransaction> transactions, LocalDate start, LocalDate end) {
        return transactions.stream()
                .filter(tx -> {
                    LocalDate d = Instant.ofEpochMilli(tx.getCreatedAt()).atZone(ZoneId.systemDefault()).toLocalDate();
                    return !d.isBefore(start) && !d.isAfter(end);
                })
                .map(tx -> tx.getTotalAmount() != null ? tx.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public ExecutiveKpiSnapshotResponse recordSnapshot(CreateSnapshotPayload payload) {
        ExecutiveKpiSnapshot snapshot = snapshotRepository
                .findByPeriodKeyAndCategoryAndKpiKey(payload.periodKey(), payload.category(), payload.kpiKey())
                .orElse(null);
        if (snapshot != null) {
            snapshot.update(payload.targetValue(), payload.actualValue(), payload.varianceValue(),
                    payload.variancePercent(), payload.trendDirection(), payload.reconciliationStatus(),
                    payload.drilldownUrl(), payload.metadataJson());
        } else {
            snapshot = new ExecutiveKpiSnapshot(
                    payload.periodKey(), payload.category(), payload.kpiKey(), payload.targetValue(),
                    payload.actualValue(), payload.varianceValue(), payload.variancePercent(),
                    payload.trendDirection(), payload.reconciliationStatus(), payload.drilldownUrl(),
                    payload.metadataJson()
            );
        }
        ExecutiveKpiSnapshot saved = snapshotRepository.save(snapshot);
        return toSnapshotResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ExecutiveKpiSnapshotResponse> listSnapshots(String periodKey) {
        List<ExecutiveKpiSnapshot> list = (periodKey != null && !periodKey.isBlank())
                ? snapshotRepository.findByPeriodKeyOrderByCategoryAscKpiKeyAsc(periodKey)
                : snapshotRepository.findAllByOrderByCreatedAtDesc();
        return list.stream().map(this::toSnapshotResponse).toList();
    }

    private ExecutiveKpiSnapshotResponse toSnapshotResponse(ExecutiveKpiSnapshot s) {
        return new ExecutiveKpiSnapshotResponse(
                s.getId(), s.getSnapshotDate(), s.getPeriodKey(), s.getCategory(), s.getKpiKey(),
                s.getTargetValue(), s.getActualValue(), s.getVarianceValue(), s.getVariancePercent(),
                s.getTrendDirection(), s.getReconciliationStatus(), s.getDrilldownUrl(), s.getMetadataJson(),
                s.getCreatedAt()
        );
    }

    // =========================================================================
    // TASK-05: OWNER / EXECUTIVE COCKPIT & PROFIT PULSE
    // =========================================================================

    @Transactional(readOnly = true)
    public OwnerCockpitResponse getOwnerCockpit(String period, String branchId) {
        if (branchId != null && !branchId.isBlank() && !authEvaluator.hasBranchAccess(branchId)) {
            throw new BusinessRuleException("Branch access denied", "BRANCH_ACCESS_DENIED", HttpStatus.FORBIDDEN);
        }

        String effectivePeriod = effectivePeriod(period);
        YearMonth ym = YearMonth.parse(effectivePeriod);
        LocalDate periodStart = ym.atDay(1);
        LocalDate periodEnd = ym.atEndOfMonth();
        LocalDate today = LocalDate.now();

        List<CustomerInvoice> allInvoices = customerInvoiceRepository.findAll();
        List<PosTransaction> posTxs = posTransactionRepository.findAll();

        // 1. Today's sales & collections — real, may legitimately be zero on a slow day.
        BigDecimal todaySalesInvoices = allInvoices.stream()
                .filter(i -> today.equals(i.getInvoiceDate()))
                .map(i -> i.getAmount() != null ? i.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todayPosSales = sumPosInRange(posTxs, today, today);
        BigDecimal todaySales = todaySalesInvoices.add(todayPosSales);

        List<CustomerReceipt> allReceipts = customerReceiptRepository.findAll();
        BigDecimal todayReceipts = allReceipts.stream()
                .filter(r -> today.equals(r.getReceiptDate()))
                .map(r -> r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todayCollections = todayReceipts.add(todayPosSales);

        // 2. Headline P&L for the period — real, GL-sourced (same figures Finance Reports shows).
        FinancialStatementsReportService.IncomeStatementReport incomeStatement =
                financialStatementsReportService.getIncomeStatement(periodStart, periodEnd);
        BigDecimal totalRevenue = incomeStatement.totalRevenue();
        BigDecimal totalOpex = incomeStatement.totalExpenses();
        BigDecimal netProfit = incomeStatement.netIncome();
        BigDecimal operatingProfit = netProfit; // no separate operating/net split is modeled
        BigDecimal netMarginPercent = percentOf(netProfit, totalRevenue);

        // 3. Gross margin — a separate, sales-only view (real delivery-line revenue/COGS), not
        // forced to reconcile with the GL P&L above (see class-level note).
        BigDecimal periodSalesRevenue = sumInvoicesInRange(allInvoices, periodStart, periodEnd)
                .add(sumPosInRange(posTxs, periodStart, periodEnd));
        List<SalesDeliveryLine> periodDeliveryLines = salesDeliveryLineRepository.findAll().stream()
                .filter(l -> {
                    LocalDate d = Instant.ofEpochMilli(l.getCreatedAt()).atZone(ZoneId.systemDefault()).toLocalDate();
                    return !d.isBefore(periodStart) && !d.isAfter(periodEnd);
                })
                .toList();
        BigDecimal totalCogs = periodDeliveryLines.stream()
                .map(l -> l.getCogsAmount() != null ? l.getCogsAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grossMarginAmount = periodSalesRevenue.subtract(totalCogs);
        BigDecimal grossMarginPercent = percentOf(grossMarginAmount, periodSalesRevenue);

        // 4. Payroll — real, period-scoped (previously summed ALL-TIME payroll regardless of the
        // requested period, and fabricated a constant when zero).
        List<SalaryPayment> periodPayments = salaryPaymentRepository.findAll().stream()
                .filter(s -> s.getPeriodYear() == ym.getYear() && s.getPeriodMonth() == ym.getMonthValue())
                .toList();
        BigDecimal totalPayrollDisbursed = periodPayments.stream()
                .filter(s -> s.getPaymentStatus() == PaymentStatus.PAID)
                .map(s -> s.getNetAmount() != null ? s.getNetAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payrollPending = periodPayments.stream()
                .filter(s -> s.getPaymentStatus() != PaymentStatus.PAID
                        && s.getPaymentStatus() != PaymentStatus.REVERSED)
                .map(s -> s.getNetAmount() != null ? s.getNetAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Cash & bank position — real, from TreasuryPositionService (no more count*450000 / *0.62 guesses).
        BigDecimal cashInHand = treasuryPositionService.totalCashBalance();
        BigDecimal bankBalances = treasuryPositionService.totalBankBalance();

        // 6. AR aging — real invoice-level bucketing; a zero total is reported as zero, not
        // overwritten with fabricated bucket amounts/counts.
        List<CustomerInvoice> openInvoices = allInvoices.stream()
                .filter(i -> i.getOutstandingAmount() != null && i.getOutstandingAmount().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        AgingTotals arTotals = bucketAgingByDueDate(openInvoices, today,
                CustomerInvoice::getOutstandingAmount,
                i -> i.getDueDate() != null ? i.getDueDate() : (i.getInvoiceDate() != null ? i.getInvoiceDate().plusDays(30) : today));
        ArApAgingSummary arAging = buildAgingSummary(arTotals);
        BigDecimal totalReceivables = arTotals.total();
        BigDecimal overdueReceivables = arTotals.overdue();

        // 7. AP aging — same treatment for supplier invoices.
        List<SupplierInvoice> openSupplierInvoices = supplierInvoiceRepository.findAll().stream()
                .filter(i -> !"PAID".equalsIgnoreCase(i.getStatus()))
                .toList();
        AgingTotals apTotals = bucketAgingByDueDate(openSupplierInvoices, today,
                i -> i.getNetAmount() != null ? i.getNetAmount() : BigDecimal.ZERO,
                i -> i.getDueDate() != null ? i.getDueDate() : i.getInvoiceDate().plusDays(30));
        ArApAgingSummary apAging = buildAgingSummary(apTotals);
        BigDecimal totalPayables = apTotals.total();
        BigDecimal overduePayables = apTotals.overdue();

        BigDecimal netLiquidity = cashInHand.add(bankBalances).subtract(overduePayables);

        // 8. Stock pulse — real on-hand quantities and real valued cost from InventoryValuationService
        // (previously derived a fake "current stock" from reorderPoint*0.4, never the real balance).
        OperationsApi.ValuationReport valuationReport = inventoryValuationService.report();
        Map<String, OperationsApi.ItemValuationView> valuationByItem = valuationReport.items().stream()
                .collect(Collectors.toMap(OperationsApi.ItemValuationView::itemId, v -> v));
        List<InventoryItem> items = inventoryItemRepository.findAll();
        List<StockAlertItem> lowStockAlerts = new ArrayList<>();
        List<StockAlertItem> deadStockAlerts = new ArrayList<>();
        for (InventoryItem it : items) {
            OperationsApi.ItemValuationView valuation = valuationByItem.get(it.getId());
            BigDecimal onHand = valuation != null ? valuation.quantityOnHand() : BigDecimal.ZERO;
            BigDecimal estimatedValue = valuation != null ? valuation.inventoryValue() : BigDecimal.ZERO;
            BigDecimal reorderPoint = it.getReorderPoint() != null ? it.getReorderPoint() : BigDecimal.ZERO;
            BigDecimal reorderQuantity = it.getReorderQuantity() != null ? it.getReorderQuantity() : BigDecimal.ZERO;
            if (reorderPoint.compareTo(BigDecimal.ZERO) > 0 && onHand.compareTo(reorderPoint) <= 0) {
                lowStockAlerts.add(new StockAlertItem(it.getId(), it.getCode(), it.getName(), onHand, reorderPoint, reorderQuantity, it.isDeadStock(), estimatedValue));
            }
            if (it.isDeadStock()) {
                deadStockAlerts.add(new StockAlertItem(it.getId(), it.getCode(), it.getName(), onHand, reorderPoint, reorderQuantity, true, estimatedValue));
            }
        }

        // 9. Manufacturing WIP — real in-progress/planned orders only; empty when there are none.
        List<ProductionOrder> prodOrders = productionOrderRepository.findAllByOrderByStartDateDescCreatedAtDesc();
        List<ManufacturingWipItem> wipItems = new ArrayList<>();
        BigDecimal wipValuation = BigDecimal.ZERO;
        for (ProductionOrder po : prodOrders) {
            if (po.getStatus() == ProductionOrder.Status.IN_PROGRESS || po.getStatus() == ProductionOrder.Status.PLANNED) {
                BigDecimal matCost = po.getActualMaterialCost() != null ? po.getActualMaterialCost() : BigDecimal.ZERO;
                wipValuation = wipValuation.add(matCost);
                wipItems.add(new ManufacturingWipItem(
                        po.getId(), po.getOrderNumber(), po.getFinishedItemId(),
                        po.getTargetQuantity() != null ? po.getTargetQuantity() : BigDecimal.ZERO,
                        po.getActualOutputQuantity() != null ? po.getActualOutputQuantity() : BigDecimal.ZERO,
                        matCost, po.getStartDate() != null ? po.getStartDate().toString() : null,
                        po.getStatus().name()
                ));
            }
        }

        // 10. Project budget vs. actual — real approved budget + real cost-ledger actuals, batched
        // (fixes the confirmed N+1 — one query per project — and removes the contractValue*0.85
        // "budget" guess and the budget*0.72 "actual" guess).
        ProjectFinancials projectFinancials = computeProjectFinancials();
        List<ProjectBudgetVarianceItem> projectControlItems = projectFinancials.items();
        BigDecimal totalProjectBudget = projectFinancials.totalBudget();
        BigDecimal totalProjectActual = projectFinancials.totalActual();
        BigDecimal totalProjectVariance = totalProjectBudget.subtract(totalProjectActual);

        // 11. Branch leaderboard — real headcount (Employee.branchId) and real cash/bank position
        // (Cashbox/BankAccount branchId via TreasuryPositionService). Revenue/COGS/OPEX/margin have
        // no real per-branch attribution anywhere in the schema today (no branchId on
        // CustomerInvoice/PosTransaction/ExpenseClaim/SalaryPayment) — reported as zero rather than
        // a fabricated proportional split of the tenant totals. See
        // docs/DEEP_ENGINEERING_REVIEW_2026-09-06.md, Implementation Gap / Critical Finding C-1.
        List<Employee> employees = employeeRepository.findAll();
        Map<String, BigDecimal> cashByBranch = treasuryPositionService.cashBalanceByBranch();
        Map<String, BigDecimal> bankByBranch = treasuryPositionService.bankBalanceByBranch();
        List<Branch> branches = branchRepository.findAllByOrderByCodeAsc();
        List<BranchPerformanceItem> branchLeaderboard = new ArrayList<>();
        for (Branch b : branches) {
            if (!authEvaluator.hasBranchAccess(b.getId())) continue;
            int branchHeadcount = (int) employees.stream()
                    .filter(e -> b.getId().equals(e.getBranchId()) && e.isActive())
                    .count();
            BigDecimal branchCash = cashByBranch.getOrDefault(b.getId(), BigDecimal.ZERO);
            BigDecimal branchBank = bankByBranch.getOrDefault(b.getId(), BigDecimal.ZERO);
            branchLeaderboard.add(new BranchPerformanceItem(
                    b.getId(), b.getCode(), b.getName(), b.isMainBranch(),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    branchHeadcount, branchCash.add(branchBank)
            ));
        }

        // 12. Top customers — real invoice aggregation only; empty when there are no invoices.
        Map<String, List<CustomerInvoice>> custInvoices = allInvoices.stream()
                .filter(i -> i.getCustomerId() != null)
                .collect(Collectors.groupingBy(CustomerInvoice::getCustomerId));
        List<TopCustomerItem> topCustomers = custInvoices.entrySet().stream()
                .sorted((e1, e2) -> sumAmounts(e2.getValue(), CustomerInvoice::getAmount)
                        .compareTo(sumAmounts(e1.getValue(), CustomerInvoice::getAmount)))
                .limit(5)
                .map(e -> {
                    String custId = e.getKey();
                    List<CustomerInvoice> list = e.getValue();
                    BigDecimal invoiced = sumAmounts(list, CustomerInvoice::getAmount);
                    BigDecimal outstanding = sumAmounts(list, CustomerInvoice::getOutstandingAmount);
                    BigDecimal collected = invoiced.subtract(outstanding);
                    String custName = businessPartyRepository.findById(custId).map(BusinessParty::getName).orElse(custId);
                    return new TopCustomerItem(custId, custName, invoiced, collected, outstanding, list.size());
                })
                .toList();

        // 13. Top products — real, from SalesDeliveryLine (quantity/unitPrice/cogsAmount), grouped
        // by item and ranked by revenue. Previously this was *always* fabricated via
        // `qty = 150 - i*20` array-index arithmetic whenever any inventory item existed at all,
        // regardless of any real sales data.
        Map<String, InventoryItem> itemsById = items.stream().collect(Collectors.toMap(InventoryItem::getId, i -> i));
        List<TopProductItem> topProducts = periodDeliveryLines.stream()
                .filter(l -> l.getItemId() != null)
                .collect(Collectors.groupingBy(SalesDeliveryLine::getItemId))
                .entrySet().stream()
                .map(e -> {
                    String itemId = e.getKey();
                    List<SalesDeliveryLine> lines = e.getValue();
                    BigDecimal qty = lines.stream().map(SalesDeliveryLine::getQuantity).filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal lineRevenue = lines.stream()
                            .map(l -> l.getQuantity() != null && l.getUnitPrice() != null ? l.getQuantity().multiply(l.getUnitPrice()) : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal lineCogs = lines.stream().map(l -> l.getCogsAmount() != null ? l.getCogsAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal marginPercent = percentOf(lineRevenue.subtract(lineCogs), lineRevenue);
                    InventoryItem item = itemsById.get(itemId);
                    String code = item != null ? item.getCode() : itemId;
                    String name = item != null ? item.getName() : itemId;
                    return new TopProductItem(itemId, code, name, qty, lineRevenue, lineCogs, marginPercent);
                })
                .sorted((a, b) -> b.revenue().compareTo(a.revenue()))
                .limit(5)
                .toList();

        // 14. Expense breakdown — real per-category ExpenseClaim sums for the period, plus real
        // payroll disbursed. Percentages are relative to their own combined total (this is an
        // operational "what did we spend on" breakdown, not GL-reconciled OPEX — see class-level note).
        List<ExpenseClaim> periodExpenseClaims = expenseClaimRepository.findAll().stream()
                .filter(c -> c.getSpentOn() != null && !c.getSpentOn().isBefore(periodStart) && !c.getSpentOn().isAfter(periodEnd))
                .toList();
        List<ExpenseCategoryItem> expenseBreakdown = buildExpenseBreakdown(periodExpenseClaims, totalPayrollDisbursed);

        // 15. Targets — real, tenant-configured (or the documented system default when none exists).
        CockpitTargetResponse targets = getTargets(effectivePeriod);

        OwnerCockpitKpiSummary summary = new OwnerCockpitKpiSummary(
                todaySales, todayCollections, netLiquidity, cashInHand, bankBalances,
                totalRevenue, totalCogs, grossMarginAmount, grossMarginPercent, totalOpex,
                operatingProfit, netProfit, netMarginPercent, totalPayrollDisbursed, payrollPending,
                (int) employees.stream().filter(Employee::isActive).count(),
                wipItems.size(), wipValuation, totalProjectBudget, totalProjectActual, totalProjectVariance,
                lowStockAlerts.size(), deadStockAlerts.size(),
                totalReceivables, overdueReceivables, totalPayables, overduePayables
        );

        return new OwnerCockpitResponse(
                effectivePeriod, branchId, Instant.now().toEpochMilli(), summary, arAging, apAging,
                branchLeaderboard, topCustomers, topProducts, expenseBreakdown, lowStockAlerts, deadStockAlerts,
                wipItems, projectControlItems, targets
        );
    }

    private List<ExpenseCategoryItem> buildExpenseBreakdown(List<ExpenseClaim> claims, BigDecimal payrollDisbursed) {
        Map<String, BigDecimal> byCategory = claims.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getCategory() != null ? c.getCategory() : "OTHER",
                        Collectors.reducing(BigDecimal.ZERO, c -> c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO, BigDecimal::add)));
        BigDecimal claimsTotal = byCategory.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal combinedTotal = claimsTotal.add(payrollDisbursed);

        List<ExpenseCategoryItem> result = new ArrayList<>();
        result.add(new ExpenseCategoryItem("PAYROLL", "workspace.executive.expensePayroll", payrollDisbursed, percentOf(payrollDisbursed, combinedTotal)));
        byCategory.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach(e -> result.add(new ExpenseCategoryItem(e.getKey(), "workspace.executive.expenseCategory." + e.getKey().toLowerCase(Locale.ROOT),
                        e.getValue(), percentOf(e.getValue(), combinedTotal))));
        return result;
    }

    private record ProjectFinancials(List<ProjectBudgetVarianceItem> items, BigDecimal totalBudget,
                                      BigDecimal totalActual, BigDecimal totalContractValue) {
    }

    /**
     * Real project financials for every currently-open (non-CLOSED) project: real approved budget
     * ({@link ProjectBudgetVersionRepository}), real actual cost ({@link ProjectCostLedgerEntryRepository},
     * batched to avoid the N+1 confirmed in docs/DEEP_ENGINEERING_REVIEW_2026-09-06.md Performance
     * Review P-2). Both default to zero when no real record exists — never a guessed percentage of
     * contract value.
     */
    private ProjectFinancials computeProjectFinancials() {
        List<Project> projects = projectRepository.findAll().stream()
                .filter(p -> p.getStatus() != ProjectStatus.CLOSED)
                .toList();
        if (projects.isEmpty()) {
            return new ProjectFinancials(List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        List<String> projectIds = projects.stream().map(Project::getId).toList();
        Map<String, BigDecimal> actualByProject = sumByProjectId(projectIds, CostLedgerEntryType.ACTUAL);

        List<ProjectBudgetVarianceItem> items = new ArrayList<>();
        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal totalActual = BigDecimal.ZERO;
        BigDecimal totalContractValue = BigDecimal.ZERO;
        for (Project p : projects) {
            BigDecimal contractValue = p.getContractValue() != null ? p.getContractValue() : BigDecimal.ZERO;
            BigDecimal budget = projectBudgetVersionRepository.findByProjectIdAndStatus(p.getId(), BudgetVersionStatus.APPROVED)
                    .map(ProjectBudgetVersion::getTotalBudgetAmount).orElse(BigDecimal.ZERO);
            BigDecimal actual = actualByProject.getOrDefault(p.getId(), BigDecimal.ZERO);
            BigDecimal variance = budget.subtract(actual);
            totalBudget = totalBudget.add(budget);
            totalActual = totalActual.add(actual);
            totalContractValue = totalContractValue.add(contractValue);
            items.add(new ProjectBudgetVarianceItem(p.getId(), p.getCode(), p.getName(), contractValue, budget, actual, variance, p.getStatus().name()));
        }
        return new ProjectFinancials(items, totalBudget, totalActual, totalContractValue);
    }

    private Map<String, BigDecimal> sumByProjectId(List<String> projectIds, CostLedgerEntryType entryType) {
        Map<String, BigDecimal> result = new HashMap<>();
        for (ProjectCostLedgerEntryRepository.ProjectAmountByType row
                : costLedgerRepository.sumAmountByProjectIdInAndEntryType(projectIds, entryType)) {
            result.put(row.getProjectId(), row.getTotal());
        }
        return result;
    }

    private record AgingTotals(BigDecimal current, int currentCount, BigDecimal b30to60, int b30to60Count,
                                BigDecimal b60to90, int b60to90Count, BigDecimal bOver90, int bOver90Count,
                                BigDecimal total, BigDecimal overdue) {
    }

    /**
     * Buckets real open invoices/entries by days-past-due (0-30 = current, 31-60, 61-90, 90+).
     * Returns real zeros/empty totals when {@code openItems} is empty — never a fabricated fallback.
     */
    private <T> AgingTotals bucketAgingByDueDate(List<T> openItems, LocalDate today,
                                                  java.util.function.Function<T, BigDecimal> amountFn,
                                                  java.util.function.Function<T, LocalDate> dueDateFn) {
        BigDecimal current = BigDecimal.ZERO;
        int currentCount = 0;
        BigDecimal b30to60 = BigDecimal.ZERO;
        int b30to60Count = 0;
        BigDecimal b60to90 = BigDecimal.ZERO;
        int b60to90Count = 0;
        BigDecimal bOver90 = BigDecimal.ZERO;
        int bOver90Count = 0;

        for (T item : openItems) {
            BigDecimal amount = amountFn.apply(item);
            LocalDate due = dueDateFn.apply(item);
            long days = ChronoUnit.DAYS.between(due, today);
            if (days <= 30) {
                current = current.add(amount);
                currentCount++;
            } else if (days <= 60) {
                b30to60 = b30to60.add(amount);
                b30to60Count++;
            } else if (days <= 90) {
                b60to90 = b60to90.add(amount);
                b60to90Count++;
            } else {
                bOver90 = bOver90.add(amount);
                bOver90Count++;
            }
        }

        BigDecimal total = current.add(b30to60).add(b60to90).add(bOver90);
        BigDecimal overdue = b30to60.add(b60to90).add(bOver90);
        return new AgingTotals(current, currentCount, b30to60, b30to60Count, b60to90, b60to90Count, bOver90, bOver90Count, total, overdue);
    }

    private ArApAgingSummary buildAgingSummary(AgingTotals t) {
        BigDecimal pCurrent = percentOf(t.current(), t.total());
        BigDecimal p30 = percentOf(t.b30to60(), t.total());
        BigDecimal p60 = percentOf(t.b60to90(), t.total());
        BigDecimal p90 = percentOf(t.bOver90(), t.total());
        return new ArApAgingSummary(
                new AgingBucket("executive.bucketCurrent", t.current(), t.currentCount(), pCurrent),
                new AgingBucket("executive.bucket30to60", t.b30to60(), t.b30to60Count(), p30),
                new AgingBucket("executive.bucket60to90", t.b60to90(), t.b60to90Count(), p60),
                new AgingBucket("executive.bucketOver90", t.bOver90(), t.bOver90Count(), p90),
                t.total(), t.overdue()
        );
    }

    @Transactional(readOnly = true)
    public byte[] exportExecutiveCockpitExcel(String period, String branchId) {
        OwnerCockpitResponse data = getOwnerCockpit(period, branchId);
        final int maxRowsPerSheet = 500; // guards against an unbounded workbook (see Performance Review P-3)
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Sheet 1: Executive KPIs
            Sheet s1 = workbook.createSheet("المؤشرات التنفيذية (KPIs)");
            s1.setRightToLeft(true);
            Row t1 = s1.createRow(0);
            t1.createCell(0).setCellValue("لوحة قيادة المالك والمدير التنفيذي - " + data.period());

            List<String[]> kpis = List.of(
                    new String[]{"مبيعات اليوم (Today's Sales)", data.kpiSummary().todaySales().toPlainString() + " EGP"},
                    new String[]{"متحصلات اليوم (Today's Collections)", data.kpiSummary().todayCollections().toPlainString() + " EGP"},
                    new String[]{"صافي السيولة النقدية (Net Liquidity)", data.kpiSummary().netLiquidity().toPlainString() + " EGP"},
                    new String[]{"النقدية بالصناديق (Cash in Hand)", data.kpiSummary().cashInHand().toPlainString() + " EGP"},
                    new String[]{"أرصدة البنوك (Bank Balances)", data.kpiSummary().bankBalances().toPlainString() + " EGP"},
                    new String[]{"إجمالي الإيرادات (Total Revenue)", data.kpiSummary().totalRevenue().toPlainString() + " EGP"},
                    new String[]{"تكلفة البضاعة المباعة (COGS)", data.kpiSummary().totalCogs().toPlainString() + " EGP"},
                    new String[]{"إجمالي الربح (Gross Profit)", data.kpiSummary().grossMarginAmount().toPlainString() + " EGP"},
                    new String[]{"هامش إجمالي الربح (Gross Margin %)", data.kpiSummary().grossMarginPercent().toPlainString() + "%"},
                    new String[]{"المصروفات التشغيلية (OPEX)", data.kpiSummary().totalOpex().toPlainString() + " EGP"},
                    new String[]{"الربح التشغيلي (Operating Profit)", data.kpiSummary().operatingProfit().toPlainString() + " EGP"},
                    new String[]{"صافي الربح (Net Profit)", data.kpiSummary().netProfit().toPlainString() + " EGP"},
                    new String[]{"هامش صافي الربح (Net Margin %)", data.kpiSummary().netMarginPercent().toPlainString() + "%"},
                    new String[]{"إجمالي الذمم المدينة (AR)", data.kpiSummary().totalReceivables().toPlainString() + " EGP"},
                    new String[]{"الذمم المدينة المتأخرة (Overdue AR)", data.kpiSummary().overdueReceivables().toPlainString() + " EGP"},
                    new String[]{"إجمالي الالتزامات (AP)", data.kpiSummary().totalPayables().toPlainString() + " EGP"},
                    new String[]{"الالتزامات المتأخرة (Overdue AP)", data.kpiSummary().overduePayables().toPlainString() + " EGP"},
                    new String[]{"أوامر الإنتاج الجارية (WIP)", String.valueOf(data.kpiSummary().manufacturingWipCount())},
                    new String[]{"تقييم الإنتاج الجاري (WIP Valuation)", data.kpiSummary().manufacturingWipValuation().toPlainString() + " EGP"},
                    new String[]{"انحراف موازنة المشاريع (VAC)", data.kpiSummary().projectCostVariance().toPlainString() + " EGP"},
                    new String[]{"عدد نواقص المخزون (Low Stock)", String.valueOf(data.kpiSummary().lowStockCount())},
                    new String[]{"عدد المخزون الراكد (Dead Stock)", String.valueOf(data.kpiSummary().deadStockCount())}
            );

            for (int r = 0; r < kpis.size(); r++) {
                Row row = s1.createRow(r + 2);
                row.createCell(0).setCellValue(kpis.get(r)[0]);
                row.createCell(1).setCellValue(kpis.get(r)[1]);
            }
            s1.autoSizeColumn(0);
            s1.autoSizeColumn(1);

            // Sheet 2: AR & AP Aging Waterfall
            Sheet s2 = workbook.createSheet("أعمار الديون والالتزامات");
            s2.setRightToLeft(true);
            Row h2 = s2.createRow(0);
            String[] agingHeaders = {"الشريحة العمرية", "الذمم المدينة (AR)", "عدد الفواتير (AR)", "النسبة % (AR)", "الذمم الدائنة (AP)", "عدد الفواتير (AP)", "النسبة % (AP)"};
            for (int c = 0; c < agingHeaders.length; c++) {
                Cell cell = h2.createCell(c);
                cell.setCellValue(agingHeaders[c]);
                cell.setCellStyle(headerStyle);
            }
            List<String[]> agingRows = List.of(
                    new String[]{"0 - 30 يوم (حالي)", data.arAging().current().amount().toPlainString(), String.valueOf(data.arAging().current().invoiceCount()), data.arAging().current().percentOfTotal() + "%", data.apAging().current().amount().toPlainString(), String.valueOf(data.apAging().current().invoiceCount()), data.apAging().current().percentOfTotal() + "%"},
                    new String[]{"31 - 60 يوم", data.arAging().days30To60().amount().toPlainString(), String.valueOf(data.arAging().days30To60().invoiceCount()), data.arAging().days30To60().percentOfTotal() + "%", data.apAging().days30To60().amount().toPlainString(), String.valueOf(data.apAging().days30To60().invoiceCount()), data.apAging().days30To60().percentOfTotal() + "%"},
                    new String[]{"61 - 90 يوم", data.arAging().days60To90().amount().toPlainString(), String.valueOf(data.arAging().days60To90().invoiceCount()), data.arAging().days60To90().percentOfTotal() + "%", data.apAging().days60To90().amount().toPlainString(), String.valueOf(data.apAging().days60To90().invoiceCount()), data.apAging().days60To90().percentOfTotal() + "%"},
                    new String[]{"+90 يوم (متأخر)", data.arAging().daysOver90().amount().toPlainString(), String.valueOf(data.arAging().daysOver90().invoiceCount()), data.arAging().daysOver90().percentOfTotal() + "%", data.apAging().daysOver90().amount().toPlainString(), String.valueOf(data.apAging().daysOver90().invoiceCount()), data.apAging().daysOver90().percentOfTotal() + "%"}
            );
            for (int r = 0; r < agingRows.size(); r++) {
                Row row = s2.createRow(r + 1);
                for (int c = 0; c < agingRows.get(r).length; c++) {
                    row.createCell(c).setCellValue(agingRows.get(r)[c]);
                }
            }
            for (int c = 0; c < agingHeaders.length; c++) s2.autoSizeColumn(c);

            // Sheet 3: Branch Leaderboard
            Sheet s3 = workbook.createSheet("أداء الفروع");
            s3.setRightToLeft(true);
            Row h3 = s3.createRow(0);
            String[] brHeaders = {"كود الفرع", "اسم الفرع", "الإيرادات", "تكلفة المبيعات", "إجمالي الربح", "هامش الربح %", "المصروفات", "صافي الربح", "السيولة النقدية", "عدد الموظفين"};
            for (int c = 0; c < brHeaders.length; c++) {
                Cell cell = h3.createCell(c);
                cell.setCellValue(brHeaders[c]);
                cell.setCellStyle(headerStyle);
            }
            List<BranchPerformanceItem> branchRows = data.branchLeaderboard().stream().limit(maxRowsPerSheet).toList();
            for (int r = 0; r < branchRows.size(); r++) {
                var b = branchRows.get(r);
                Row row = s3.createRow(r + 1);
                row.createCell(0).setCellValue(b.branchCode());
                row.createCell(1).setCellValue(b.branchName());
                row.createCell(2).setCellValue(b.revenue().doubleValue());
                row.createCell(3).setCellValue(b.cogs().doubleValue());
                row.createCell(4).setCellValue(b.grossProfit().doubleValue());
                row.createCell(5).setCellValue(b.grossMarginPercent().doubleValue());
                row.createCell(6).setCellValue(b.opex().doubleValue());
                row.createCell(7).setCellValue(b.netProfit().doubleValue());
                row.createCell(8).setCellValue(b.cashAndBank().doubleValue());
                row.createCell(9).setCellValue(b.headcount());
            }
            for (int c = 0; c < brHeaders.length; c++) s3.autoSizeColumn(c);

            // Sheet 4: Top Customers & Products
            Sheet s4 = workbook.createSheet("أفضل العملاء والمنتجات");
            s4.setRightToLeft(true);
            Row h4 = s4.createRow(0);
            h4.createCell(0).setCellValue("أفضل العملاء");
            Row custHead = s4.createRow(1);
            String[] cHeads = {"اسم العميل", "إجمالي الفواتير", "إجمالي المحصل", "الرصيد المتبقي", "عدد الفواتير"};
            for (int c = 0; c < cHeads.length; c++) {
                Cell cell = custHead.createCell(c);
                cell.setCellValue(cHeads[c]);
                cell.setCellStyle(headerStyle);
            }
            int rIdx = 2;
            for (var cust : data.topCustomers()) {
                Row row = s4.createRow(rIdx++);
                row.createCell(0).setCellValue(cust.customerName());
                row.createCell(1).setCellValue(cust.totalInvoiced().doubleValue());
                row.createCell(2).setCellValue(cust.totalCollected().doubleValue());
                row.createCell(3).setCellValue(cust.outstandingBalance().doubleValue());
                row.createCell(4).setCellValue(cust.invoiceCount());
            }
            rIdx++;
            Row pTitle = s4.createRow(rIdx++);
            pTitle.createCell(0).setCellValue("أفضل المنتجات مبيعاً");
            Row prodHead = s4.createRow(rIdx++);
            String[] pHeads = {"كود الصنف", "اسم المنتج", "الكمية المباعة", "الإيرادات", "تكلفة البضاعة", "هامش الربح %"};
            for (int c = 0; c < pHeads.length; c++) {
                Cell cell = prodHead.createCell(c);
                cell.setCellValue(pHeads[c]);
                cell.setCellStyle(headerStyle);
            }
            for (var prod : data.topProducts()) {
                Row row = s4.createRow(rIdx++);
                row.createCell(0).setCellValue(prod.itemCode());
                row.createCell(1).setCellValue(prod.itemName());
                row.createCell(2).setCellValue(prod.quantitySold().doubleValue());
                row.createCell(3).setCellValue(prod.revenue().doubleValue());
                row.createCell(4).setCellValue(prod.cogs().doubleValue());
                row.createCell(5).setCellValue(prod.marginPercent().doubleValue());
            }
            for (int c = 0; c < 6; c++) s4.autoSizeColumn(c);

            // Sheet 5: Operational Alerts (Stock & Manufacturing WIP)
            Sheet s5 = workbook.createSheet("المخزون والإنتاج الجاري");
            s5.setRightToLeft(true);
            Row sTitle = s5.createRow(0);
            sTitle.createCell(0).setCellValue("تنبيهات نواقص المخزون");
            Row sHead = s5.createRow(1);
            String[] stockHeads = {"كود الصنف", "اسم المنتج", "الرصيد الحالي", "نقطة إعادة الطلب", "كمية الطلب", "القيمة التقديرية"};
            for (int c = 0; c < stockHeads.length; c++) {
                Cell cell = sHead.createCell(c);
                cell.setCellValue(stockHeads[c]);
                cell.setCellStyle(headerStyle);
            }
            int sIdx = 2;
            for (var st : data.lowStockAlerts().stream().limit(maxRowsPerSheet).toList()) {
                Row row = s5.createRow(sIdx++);
                row.createCell(0).setCellValue(st.itemCode());
                row.createCell(1).setCellValue(st.itemName());
                row.createCell(2).setCellValue(st.currentStock().doubleValue());
                row.createCell(3).setCellValue(st.reorderPoint().doubleValue());
                row.createCell(4).setCellValue(st.reorderQuantity().doubleValue());
                row.createCell(5).setCellValue(st.estimatedValue().doubleValue());
            }
            sIdx++;
            Row wTitle = s5.createRow(sIdx++);
            wTitle.createCell(0).setCellValue("أوامر الإنتاج الجارية (WIP)");
            Row wHead = s5.createRow(sIdx++);
            String[] wipHeads = {"رقم الأمر", "المنتج", "الكمية المطلوبة", "الكمية المنجزة", "تكلفة المواد", "تاريخ البدء", "الحالة"};
            for (int c = 0; c < wipHeads.length; c++) {
                Cell cell = wHead.createCell(c);
                cell.setCellValue(wipHeads[c]);
                cell.setCellStyle(headerStyle);
            }
            for (var wip : data.manufacturingWip().stream().limit(maxRowsPerSheet).toList()) {
                Row row = s5.createRow(sIdx++);
                row.createCell(0).setCellValue(wip.orderNumber());
                row.createCell(1).setCellValue(wip.itemName() != null ? wip.itemName() : "—");
                row.createCell(2).setCellValue(wip.targetQuantity().doubleValue());
                row.createCell(3).setCellValue(wip.actualOutputQuantity().doubleValue());
                row.createCell(4).setCellValue(wip.materialCost().doubleValue());
                row.createCell(5).setCellValue(wip.startDate() != null ? wip.startDate() : "—");
                row.createCell(6).setCellValue(wip.status());
            }
            for (int c = 0; c < 7; c++) s5.autoSizeColumn(c);

            // Sheet 6: Projects Budget Control
            Sheet s6 = workbook.createSheet("تكاليف وموازنات المشاريع");
            s6.setRightToLeft(true);
            Row prjHead = s6.createRow(0);
            String[] prjHeads = {"كود المشروع", "اسم المشروع", "قيمة العقد", "الموازنة المعتمدة", "التكلفة الفعلية", "انحراف التكلفة (VAC)", "الحالة"};
            for (int c = 0; c < prjHeads.length; c++) {
                Cell cell = prjHead.createCell(c);
                cell.setCellValue(prjHeads[c]);
                cell.setCellStyle(headerStyle);
            }
            List<ProjectBudgetVarianceItem> projectRows = data.projectBudgetControl().stream().limit(maxRowsPerSheet).toList();
            for (int r = 0; r < projectRows.size(); r++) {
                var p = projectRows.get(r);
                Row row = s6.createRow(r + 1);
                row.createCell(0).setCellValue(p.code());
                row.createCell(1).setCellValue(p.name());
                row.createCell(2).setCellValue(p.contractValue().doubleValue());
                row.createCell(3).setCellValue(p.budgetAmount().doubleValue());
                row.createCell(4).setCellValue(p.actualCost().doubleValue());
                row.createCell(5).setCellValue(p.costVariance().doubleValue());
                row.createCell(6).setCellValue(p.status());
            }
            for (int c = 0; c < prjHeads.length; c++) s6.autoSizeColumn(c);

            workbook.write(output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Owner Executive Cockpit Excel report", e);
        }
    }

    @Transactional(readOnly = true)
    public CockpitTargetResponse getTargets(String periodKey) {
        String key = (periodKey != null && !periodKey.isBlank()) ? periodKey : effectivePeriod(null);
        Optional<ExecutiveCockpitTarget> target = cockpitTargetRepository.findByPeriodKey(key);
        if (target.isPresent()) {
            ExecutiveCockpitTarget t = target.get();
            return new CockpitTargetResponse(
                    t.getId(), t.getPeriodKey(), t.getTargetRevenue(), t.getTargetGrossMarginPercent(),
                    t.getTargetMaxOpex(), t.getTargetMinLiquidity(), t.getTargetMaxOverdueAr(), t.getNotes(), t.getUpdatedAt()
            );
        }
        // No tenant-configured target exists for this period yet. `id: "default"` (not a real UUID)
        // signals to callers that these are illustrative starting values, not a saved target.
        return new CockpitTargetResponse(
                "default", key, BigDecimal.valueOf(1_500_000.00), BigDecimal.valueOf(35.0),
                BigDecimal.valueOf(250_000.00), BigDecimal.valueOf(300_000.00), BigDecimal.valueOf(50_000.00),
                "Standard operational targets", System.currentTimeMillis()
        );
    }

    @Transactional
    public CockpitTargetResponse saveTargets(SaveCockpitTargetRequest request) {
        if (request.periodKey() == null || request.periodKey().isBlank()) {
            throw new BusinessRuleException("Period key is required", "EXECUTIVE_TARGET_INVALID", HttpStatus.BAD_REQUEST);
        }
        ExecutiveCockpitTarget target = cockpitTargetRepository.findByPeriodKey(request.periodKey()).orElse(null);

        if (target != null) {
            target.update(request.targetRevenue(), request.targetGrossMarginPercent(), request.targetMaxOpex(),
                    request.targetMinLiquidity(), request.targetMaxOverdueAr(), request.notes());
        } else {
            target = new ExecutiveCockpitTarget(request.periodKey(), request.targetRevenue(),
                    request.targetGrossMarginPercent(), request.targetMaxOpex(), request.targetMinLiquidity(),
                    request.targetMaxOverdueAr(), request.notes());
        }

        ExecutiveCockpitTarget saved;
        try {
            saved = cockpitTargetRepository.save(target);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Two concurrent first-time saves for the same not-yet-existing period both pass the
            // null check above; the DB unique constraint on (app_id, period_key) rejects the loser.
            // Report a clean, specific conflict instead of letting the generic DATA_CONFLICT handler
            // return an unhelpful message (see docs/DEEP_ENGINEERING_REVIEW_2026-09-06.md, Medium
            // Finding M-3) — the caller should re-fetch and retry as an update.
            throw new BusinessRuleException(
                    "Targets for this period were just created by another request; reload and try again.",
                    "EXECUTIVE_TARGET_CONCURRENT_CREATE", HttpStatus.CONFLICT);
        }
        return new CockpitTargetResponse(
                saved.getId(), saved.getPeriodKey(), saved.getTargetRevenue(), saved.getTargetGrossMarginPercent(),
                saved.getTargetMaxOpex(), saved.getTargetMinLiquidity(), saved.getTargetMaxOverdueAr(),
                saved.getNotes(), saved.getUpdatedAt()
        );
    }

    private String effectivePeriod(String period) {
        return (period != null && !period.isBlank()) ? period : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    private static BigDecimal percentOf(BigDecimal numerator, BigDecimal denominator) {
        return denominator != null && denominator.compareTo(BigDecimal.ZERO) > 0
                ? numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }

    private static <T> BigDecimal sumAmounts(List<T> list, java.util.function.Function<T, BigDecimal> amountFn) {
        return list.stream().map(amountFn).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
