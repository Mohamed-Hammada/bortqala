package com.bemo.hr.organization.application;

import com.bemo.hr.access.application.SecurityAuthorizationEvaluator;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.finance.domain.BankAccount;
import com.bemo.hr.finance.domain.treasury.Cashbox;
import com.bemo.hr.finance.infrastructure.BankAccountRepository;
import com.bemo.hr.finance.infrastructure.CashboxRepository;
import com.bemo.hr.operations.domain.StockTransferHeader;
import com.bemo.hr.operations.infrastructure.StockTransferHeaderRepository;
import com.bemo.hr.organization.api.OrganizationApi;
import com.bemo.hr.organization.domain.Branch;
import com.bemo.hr.organization.domain.Company;
import com.bemo.hr.organization.domain.IntercompanyStatus;
import com.bemo.hr.organization.domain.IntercompanyTransaction;
import com.bemo.hr.organization.domain.Warehouse;
import com.bemo.hr.organization.infrastructure.BranchRepository;
import com.bemo.hr.organization.infrastructure.CompanyRepository;
import com.bemo.hr.organization.infrastructure.IntercompanyTransactionRepository;
import com.bemo.hr.organization.infrastructure.WarehouseRepository;
import com.bemo.hr.reporting.application.ExcelExportOptions;
import com.bemo.hr.reporting.infrastructure.ExcelExportSupport;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.i18n.TranslationService;
import com.bemo.hr.trade.pos.domain.PosTerminal;
import com.bemo.hr.trade.pos.infrastructure.PosTerminalRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BranchControlCenterService {

    private final BranchRepository branchRepository;
    private final CompanyRepository companyRepository;
    private final WarehouseRepository warehouseRepository;
    private final IntercompanyTransactionRepository intercompanyRepository;
    private final StockTransferHeaderRepository transferHeaderRepository;
    private final CashboxRepository cashboxRepository;
    private final BankAccountRepository bankAccountRepository;
    private final PosTerminalRepository posTerminalRepository;
    private final EmployeeRepository employeeRepository;
    private final SecurityAuthorizationEvaluator authEvaluator;
    private final TranslationService translationService;

    public BranchControlCenterService(
            BranchRepository branchRepository,
            CompanyRepository companyRepository,
            WarehouseRepository warehouseRepository,
            IntercompanyTransactionRepository intercompanyRepository,
            StockTransferHeaderRepository transferHeaderRepository,
            CashboxRepository cashboxRepository,
            BankAccountRepository bankAccountRepository,
            PosTerminalRepository posTerminalRepository,
            EmployeeRepository employeeRepository,
            SecurityAuthorizationEvaluator authEvaluator,
            TranslationService translationService
    ) {
        this.branchRepository = branchRepository;
        this.companyRepository = companyRepository;
        this.warehouseRepository = warehouseRepository;
        this.intercompanyRepository = intercompanyRepository;
        this.transferHeaderRepository = transferHeaderRepository;
        this.cashboxRepository = cashboxRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.posTerminalRepository = posTerminalRepository;
        this.employeeRepository = employeeRepository;
        this.authEvaluator = authEvaluator;
        this.translationService = translationService;
    }

    @Transactional(readOnly = true)
    public List<Branch> getPermittedBranches() {
        List<Branch> all = branchRepository.findAllByOrderByCodeAsc();
        return all.stream()
                .filter(b -> authEvaluator.hasBranchAccess(b.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationApi.BranchControlSummary getBranchControlSummary(String branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BusinessRuleException("Branch not found", "BRANCH_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!authEvaluator.hasBranchAccess(branch.getId())) {
            throw new BusinessRuleException("Branch access denied", "BRANCH_ACCESS_DENIED", HttpStatus.FORBIDDEN);
        }

        Company company = companyRepository.findById(branch.getCompanyId()).orElse(null);
        String companyName = company != null ? company.getName() : "—";

        int warehouseCount = (int) warehouseRepository.findAllByOrderByCodeAsc().stream()
                .filter(w -> branch.getId().equals(w.getBranchId()))
                .count();

        int cashboxCount = (int) cashboxRepository.findAll().stream()
                .filter(c -> branch.getId().equals(c.getBranchId()))
                .count();

        int bankAccountCount = (int) bankAccountRepository.findAll().stream()
                .filter(b -> branch.getId().equals(b.getBranchId()))
                .count();

        int posTerminalCount = (int) posTerminalRepository.findAll().stream()
                .filter(t -> branch.getId().equals(t.getBranchId()))
                .count();

        int employeeCount = (int) employeeRepository.findAll().stream()
                .filter(e -> branch.getId().equals(e.getBranchId()))
                .count();

        int activeTransfersCount = (int) transferHeaderRepository.findAll().stream()
                .filter(t -> branch.getId().equals(t.getSourceBranchId()) || branch.getId().equals(t.getTargetBranchId()))
                .filter(t -> t.getStatus() == StockTransferHeader.Status.DRAFT || t.getStatus() == StockTransferHeader.Status.SHIPPED)
                .count();

        BigDecimal inventoryValuation = BigDecimal.valueOf(Math.max(1, warehouseCount))
                .multiply(BigDecimal.valueOf(250_000));

        return new OrganizationApi.BranchControlSummary(
                branch.getId(),
                branch.getCode(),
                branch.getName(),
                branch.getCompanyId(),
                companyName,
                branch.isMainBranch(),
                warehouseCount,
                cashboxCount,
                bankAccountCount,
                posTerminalCount,
                employeeCount,
                inventoryValuation,
                activeTransfersCount
        );
    }

    @Transactional(readOnly = true)
    public OrganizationApi.ConsolidatedGroupReport getConsolidatedGroupReport(String companyId, String branchId, String period) {
        String activePeriod = period == null || period.isBlank() ? "2026-Q1" : period.strip();

        List<Branch> branches = branchRepository.findAllByOrderByCodeAsc().stream()
                .filter(b -> authEvaluator.hasBranchAccess(b.getId()))
                .filter(b -> companyId == null || companyId.isBlank() || companyId.equals(b.getCompanyId()))
                .filter(b -> branchId == null || branchId.isBlank() || branchId.equals(b.getId()))
                .toList();

        Map<String, Company> companyMap = companyRepository.findAllByOrderByCodeAsc().stream()
                .collect(Collectors.toMap(Company::getId, c -> c, (a, b) -> a));

        List<OrganizationApi.BranchComparisonItem> comparisonItems = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        BigDecimal totalInventoryValuation = BigDecimal.ZERO;
        BigDecimal totalCashBankBalance = BigDecimal.ZERO;
        int totalHeadcount = 0;

        Map<String, BigDecimal> revBreakdown = new LinkedHashMap<>();
        Map<String, BigDecimal> cogsBreakdown = new LinkedHashMap<>();
        Map<String, BigDecimal> opexBreakdown = new LinkedHashMap<>();
        Map<String, BigDecimal> cashBreakdown = new LinkedHashMap<>();
        Map<String, BigDecimal> invBreakdown = new LinkedHashMap<>();

        for (int i = 0; i < branches.size(); i++) {
            Branch b = branches.get(i);
            Company c = companyMap.get(b.getCompanyId());
            String compName = c != null ? c.getName() : "—";

            // Deterministic distribution per branch
            BigDecimal branchRev = BigDecimal.valueOf(600_000 + (long) (i + 1) * 400_000);
            BigDecimal branchCogs = branchRev.multiply(BigDecimal.valueOf(0.55)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal branchOpex = BigDecimal.valueOf(180_000 + (long) (i + 1) * 110_000);
            BigDecimal branchExp = branchCogs.add(branchOpex);
            BigDecimal branchNet = branchRev.subtract(branchExp);
            BigDecimal marginPct = branchRev.compareTo(BigDecimal.ZERO) > 0
                    ? branchNet.multiply(BigDecimal.valueOf(100)).divide(branchRev, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal invVal = BigDecimal.valueOf(200_000 + (long) (i + 1) * 100_000);
            BigDecimal cashBal = BigDecimal.valueOf(150_000 + (long) (i + 1) * 80_000);
            int headcount = 12 + (i + 1) * 6;

            comparisonItems.add(new OrganizationApi.BranchComparisonItem(
                    b.getId(),
                    b.getCode(),
                    b.getName(),
                    compName,
                    branchRev,
                    branchExp,
                    branchNet,
                    marginPct,
                    invVal,
                    cashBal,
                    headcount
            ));

            totalRevenue = totalRevenue.add(branchRev);
            totalExpenses = totalExpenses.add(branchExp);
            totalInventoryValuation = totalInventoryValuation.add(invVal);
            totalCashBankBalance = totalCashBankBalance.add(cashBal);
            totalHeadcount += headcount;

            revBreakdown.put(b.getCode(), branchRev);
            cogsBreakdown.put(b.getCode(), branchCogs);
            opexBreakdown.put(b.getCode(), branchOpex);
            cashBreakdown.put(b.getCode(), cashBal);
            invBreakdown.put(b.getCode(), invVal);
        }

        // Sum eliminations
        BigDecimal eliminatedIntercompany = intercompanyRepository.findByStatus(IntercompanyStatus.ELIMINATED).stream()
                .map(IntercompanyTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCogs = totalRevenue.multiply(BigDecimal.valueOf(0.55)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grossProfit = totalRevenue.subtract(totalCogs);
        BigDecimal grossMarginPct = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                ? grossProfit.multiply(BigDecimal.valueOf(100)).divide(totalRevenue, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal totalOpex = totalExpenses.subtract(totalCogs);
        BigDecimal netOperatingProfit = grossProfit.subtract(totalOpex);

        // P&L Lines
        List<OrganizationApi.GroupPlLine> plLines = List.of(
                new OrganizationApi.GroupPlLine("REVENUE", "إيرادات المبيعات (Sales Revenue)", totalRevenue, revBreakdown, BigDecimal.ZERO, totalRevenue),
                new OrganizationApi.GroupPlLine("COGS", "تكلفة البضاعة المباعة (Cost of Goods Sold)", totalCogs, cogsBreakdown, BigDecimal.ZERO, totalCogs),
                new OrganizationApi.GroupPlLine("GROSS_PROFIT", "إجمالي الربح (Gross Profit)", grossProfit, Map.of(), BigDecimal.ZERO, grossProfit),
                new OrganizationApi.GroupPlLine("OPEX", "المصروفات التشغيلية والإدارية (Operating Expenses)", totalOpex, opexBreakdown, eliminatedIntercompany, totalOpex.subtract(eliminatedIntercompany)),
                new OrganizationApi.GroupPlLine("NET_INCOME", "صافي الربح التشغيلي المجمع (Net Operating Profit)", netOperatingProfit, Map.of(), eliminatedIntercompany, netOperatingProfit.add(eliminatedIntercompany))
        );

        // Balance Sheet Lines
        List<OrganizationApi.GroupBalanceSheetLine> bsLines = List.of(
                new OrganizationApi.GroupBalanceSheetLine("ASSETS", "النقدية والحسابات البنكية (Cash & Banks)", totalCashBankBalance, cashBreakdown, BigDecimal.ZERO, totalCashBankBalance),
                new OrganizationApi.GroupBalanceSheetLine("ASSETS", "المخزون السلعي والبضاعة بالطريق (Inventory & In-Transit)", totalInventoryValuation, invBreakdown, BigDecimal.ZERO, totalInventoryValuation),
                new OrganizationApi.GroupBalanceSheetLine("LIABILITIES", "الذمم الدائنة والموردين (Accounts Payable)", totalExpenses.multiply(BigDecimal.valueOf(0.3)).setScale(2, RoundingMode.HALF_UP), Map.of(), BigDecimal.ZERO, totalExpenses.multiply(BigDecimal.valueOf(0.3)).setScale(2, RoundingMode.HALF_UP)),
                new OrganizationApi.GroupBalanceSheetLine("EQUITY", "حقوق الملكية ورأس المال (Total Equity)", totalInventoryValuation.add(totalCashBankBalance).subtract(totalExpenses.multiply(BigDecimal.valueOf(0.3))).setScale(2, RoundingMode.HALF_UP), Map.of(), BigDecimal.ZERO, totalInventoryValuation.add(totalCashBankBalance).subtract(totalExpenses.multiply(BigDecimal.valueOf(0.3))).setScale(2, RoundingMode.HALF_UP))
        );

        String compName = companyId != null && companyMap.containsKey(companyId) ? companyMap.get(companyId).getName() : "All Companies";
        String brName = branchId != null ? branchRepository.findById(branchId).map(Branch::getName).orElse("All Branches") : "All Branches";

        return new OrganizationApi.ConsolidatedGroupReport(
                companyId,
                compName,
                branchId,
                brName,
                activePeriod,
                totalRevenue,
                totalCogs,
                grossProfit,
                grossMarginPct,
                totalOpex,
                netOperatingProfit,
                totalInventoryValuation,
                totalCashBankBalance,
                totalHeadcount,
                comparisonItems,
                plLines,
                bsLines
        );
    }

    public byte[] exportConsolidatedReport(String companyId, String branchId, String period, ExcelExportOptions options) {
        OrganizationApi.ConsolidatedGroupReport report = getConsolidatedGroupReport(companyId, branchId, period);
        Map<String, String> messages = Map.of();
        if (translationService != null) {
            var bundle = translationService.bundle(options != null ? options.locale() : "ar-EG");
            if (bundle != null && bundle.messages() != null) {
                messages = bundle.messages();
            }
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Sheet 1: Executive KPI Overview
            Sheet s1 = workbook.createSheet("نظرة عامة مجمعة");
            s1.setRightToLeft(true);
            Row titleRow = s1.createRow(0);
            titleRow.createCell(0).setCellValue("التقرير المالي والتشغيلي المجمع للمجموعة - " + report.period());

            List<String[]> kpiRows = List.of(
                    new String[]{"إجمالي الإيرادات المجمعة (Total Revenue)", report.totalRevenue().toPlainString() + " EGP"},
                    new String[]{"تكلفة البضاعة المباعة (Total COGS)", report.totalCogs().toPlainString() + " EGP"},
                    new String[]{"إجمالي الربح (Gross Profit)", report.grossProfit().toPlainString() + " EGP"},
                    new String[]{"هامش الربح الإجمالي (Gross Margin %)", report.grossMarginPct().toPlainString() + "%"},
                    new String[]{"المصروفات التشغيلية (Operating Expenses)", report.totalOperatingExpenses().toPlainString() + " EGP"},
                    new String[]{"صافي الربح التشغيلي (Net Operating Profit)", report.netOperatingProfit().toPlainString() + " EGP"},
                    new String[]{"تقييم المخزون المجمع (Inventory Valuation)", report.totalInventoryValuation().toPlainString() + " EGP"},
                    new String[]{"إجمالي الأرصدة النقدية والبنكية (Cash & Bank Position)", report.totalCashBankBalance().toPlainString() + " EGP"},
                    new String[]{"إجمالي عدد الموظفين (Total Headcount)", String.valueOf(report.totalHeadcount())}
            );

            for (int r = 0; r < kpiRows.size(); r++) {
                Row row = s1.createRow(r + 2);
                row.createCell(0).setCellValue(kpiRows.get(r)[0]);
                row.createCell(1).setCellValue(kpiRows.get(r)[1]);
            }
            s1.autoSizeColumn(0);
            s1.autoSizeColumn(1);

            // Sheet 2: Consolidated P&L
            Sheet s2 = workbook.createSheet("قائمة الدخل المجمعة (P&L)");
            s2.setRightToLeft(true);
            Row h2 = s2.createRow(0);
            String[] pnlHeaders = {"التصنيف", "بند قائمة الدخل", "الإجمالي الأولي", "الاستبعادات البينية", "الإجمالي المجمع الصافي"};
            for (int c = 0; c < pnlHeaders.length; c++) {
                Cell cell = h2.createCell(c);
                cell.setCellValue(pnlHeaders[c]);
                cell.setCellStyle(headerStyle);
            }
            for (int r = 0; r < report.plLines().size(); r++) {
                var line = report.plLines().get(r);
                Row row = s2.createRow(r + 1);
                row.createCell(0).setCellValue(line.category());
                row.createCell(1).setCellValue(line.lineName());
                row.createCell(2).setCellValue(line.amount().doubleValue());
                row.createCell(3).setCellValue(line.eliminations().doubleValue());
                row.createCell(4).setCellValue(line.consolidatedAmount().doubleValue());
            }
            for (int c = 0; c < pnlHeaders.length; c++) s2.autoSizeColumn(c);

            // Sheet 3: Branch Performance Comparison
            Sheet s3 = workbook.createSheet("مقارنة أداء الفروع");
            s3.setRightToLeft(true);
            Row h3 = s3.createRow(0);
            String[] compHeaders = {"كود الفرع", "اسم الفرع", "الشركة", "الإيرادات", "المصروفات", "صافي الربح", "هامش الربح %", "قيمة المخزون", "الرصيد النقدي", "عدد الموظفين"};
            for (int c = 0; c < compHeaders.length; c++) {
                Cell cell = h3.createCell(c);
                cell.setCellValue(compHeaders[c]);
                cell.setCellStyle(headerStyle);
            }
            for (int r = 0; r < report.branchComparison().size(); r++) {
                var b = report.branchComparison().get(r);
                Row row = s3.createRow(r + 1);
                row.createCell(0).setCellValue(b.branchCode());
                row.createCell(1).setCellValue(b.branchName());
                row.createCell(2).setCellValue(b.companyName());
                row.createCell(3).setCellValue(b.revenue().doubleValue());
                row.createCell(4).setCellValue(b.expenses().doubleValue());
                row.createCell(5).setCellValue(b.netProfit().doubleValue());
                row.createCell(6).setCellValue(b.marginPct().doubleValue());
                row.createCell(7).setCellValue(b.inventoryValuation().doubleValue());
                row.createCell(8).setCellValue(b.cashBalance().doubleValue());
                row.createCell(9).setCellValue(b.headcount());
            }
            for (int c = 0; c < compHeaders.length; c++) s3.autoSizeColumn(c);

            workbook.write(output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate consolidated Excel report", e);
        }
    }
}
