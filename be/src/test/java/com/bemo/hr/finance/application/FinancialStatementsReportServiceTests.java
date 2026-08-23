package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.Account;
import com.bemo.hr.finance.domain.JournalEntry;
import com.bemo.hr.finance.domain.JournalEntryLine;
import com.bemo.hr.finance.infrastructure.AccountRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryLineRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

class FinancialStatementsReportServiceTests {

    private AccountRepository accountRepository;
    private JournalEntryRepository journalEntryRepository;
    private JournalEntryLineRepository journalEntryLineRepository;
    private FinancialStatementsReportService statementsService;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        journalEntryRepository = mock(JournalEntryRepository.class);
        journalEntryLineRepository = mock(JournalEntryLineRepository.class);
        statementsService = new FinancialStatementsReportService(accountRepository, journalEntryRepository, journalEntryLineRepository);
    }

    @Test
    @DisplayName("Generates Balance Sheet and Income Statement successfully with balanced equation")
    void generatesBalanceSheetAndIncomeStatementSuccessfully() {
        Account cash = new Account("1010", "Cash", Account.Type.ASSET, null, false, "EGP", true);
        Account revenue = new Account("4010", "Sales Revenue", Account.Type.REVENUE, null, false, "EGP", true);
        Account expense = new Account("5010", "Rent Expense", Account.Type.EXPENSE, null, false, "EGP", true);

        when(accountRepository.findAll()).thenReturn(List.of(cash, revenue, expense));

        JournalEntry je = new JournalEntry("JE-2026-001", LocalDate.of(2026, 3, 1), "Sale and Rent", "REF-01", "FP-01");
        je.approve("manager");
        je.post("admin");
        when(journalEntryRepository.findByStatusInOrderByEntryDateDesc(anyList())).thenReturn(List.of(je));

        JournalEntryLine l1 = new JournalEntryLine(je.getId(), cash.getId(), null, new BigDecimal("1000.00"), BigDecimal.ZERO, "Cash in");
        JournalEntryLine l2 = new JournalEntryLine(je.getId(), revenue.getId(), null, BigDecimal.ZERO, new BigDecimal("1000.00"), "Revenue");
        when(journalEntryLineRepository.findByJournalEntryIdIn(anySet())).thenReturn(List.of(l1, l2));

        FinancialStatementsReportService.BalanceSheetReport bs = statementsService.getBalanceSheet(LocalDate.of(2026, 3, 31));
        assertThat(bs.totalAssets()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(bs.netIncome()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(bs.balanced()).isTrue();

        FinancialStatementsReportService.IncomeStatementReport inc = statementsService.getIncomeStatement(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        assertThat(inc.totalRevenue()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(inc.netIncome()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("Direct-method cash flow classifies operating/investing/financing and reconciles exactly")
    void generatesCashFlowStatementSuccessfully() {
        Account cash = new Account("1010", "Main Cash", Account.Type.ASSET, null, false, "EGP", true);
        Account fixedAsset = new Account("1210", "Equipment", Account.Type.ASSET, null, false, "EGP", true);
        Account loanPayable = new Account("2201", "Bank Loan", Account.Type.LIABILITY, null, false, "EGP", true);
        Account equity = new Account("3100", "Owner Capital", Account.Type.EQUITY, null, false, "EGP", true);
        Account revenue = new Account("4010", "Sales Revenue", Account.Type.REVENUE, null, false, "EGP", true);
        Account expense = new Account("5010", "Salaries Expense", Account.Type.EXPENSE, null, false, "EGP", true);
        Account receivable = new Account("1400", "Trade Receivable", Account.Type.ASSET, null, false, "EGP", true);

        when(accountRepository.findAll()).thenReturn(List.of(cash, fixedAsset, loanPayable, equity, revenue, expense, receivable));

        // E1: cash sale 5000 -> operating +5000
        JournalEntry sale = new JournalEntry("JE-CF-1", LocalDate.of(2026, 3, 10), "cash sale", "R1", "FP");
        sale.approve("m");
        sale.post("a");
        // E2: equipment bought with cash 2000 -> investing -2000
        JournalEntry equipmentBuy = new JournalEntry("JE-CF-2", LocalDate.of(2026, 3, 12), "equipment", "R2", "FP");
        equipmentBuy.approve("m");
        equipmentBuy.post("a");
        // E3: owner invests cash 10000 via equity -> financing +10000
        JournalEntry capital = new JournalEntry("JE-CF-3", LocalDate.of(2026, 3, 13), "capital", "R3", "FP");
        capital.approve("m");
        capital.post("a");
        // E4: accrual expense on credit, no cash moved -> must be ignored
        JournalEntry accrual = new JournalEntry("JE-CF-4", LocalDate.of(2026, 3, 14), "accrual", "R4", "FP");
        accrual.approve("m");
        accrual.post("a");

        when(journalEntryRepository.findByStatusInOrderByEntryDateDesc(anyList())).thenReturn(
                List.of(sale, equipmentBuy, capital, accrual));

        java.util.List<JournalEntryLine> lines = List.of(
                new JournalEntryLine(sale.getId(), cash.getId(), null, bd("5000"), BigDecimal.ZERO, "in"),
                new JournalEntryLine(sale.getId(), revenue.getId(), null, BigDecimal.ZERO, bd("5000"), "rev"),
                new JournalEntryLine(equipmentBuy.getId(), fixedAsset.getId(), null, bd("2000"), BigDecimal.ZERO, "asset"),
                new JournalEntryLine(equipmentBuy.getId(), cash.getId(), null, BigDecimal.ZERO, bd("2000"), "out"),
                new JournalEntryLine(capital.getId(), cash.getId(), null, bd("10000"), BigDecimal.ZERO, "in"),
                new JournalEntryLine(capital.getId(), equity.getId(), null, BigDecimal.ZERO, bd("10000"), "equity"),
                new JournalEntryLine(accrual.getId(), expense.getId(), null, bd("800"), BigDecimal.ZERO, "exp"),
                new JournalEntryLine(accrual.getId(), receivable.getId(), null, BigDecimal.ZERO, bd("800"), "ap")
        );
        when(journalEntryLineRepository.findByJournalEntryIdIn(anySet()))
                .thenAnswer(linesByEntry(lines));

        FinancialStatementsReportService.CashFlowReport cf = statementsService.getCashFlowStatement(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertThat(cf.operatingCashFlow()).isEqualByComparingTo(bd("5000"));
        assertThat(cf.investingCashFlow()).isEqualByComparingTo(bd("-2000"));
        assertThat(cf.financingCashFlow()).isEqualByComparingTo(bd("10000"));
        assertThat(cf.netCashFlow()).isEqualByComparingTo(bd("13000"));
        assertThat(cf.closingCashBalance()).isEqualByComparingTo(bd("13000"));
        assertThat(cf.reconciled()).isTrue();
    }

    @Test
    @DisplayName("Financing movements keep the correct sign: loans received increase financing cash")
    void financingLoanReceivedIsPositiveNotNegative() {
        Account cash = new Account("1010", "Main Cash", Account.Type.ASSET, null, false, "EGP", true);
        Account loanPayable = new Account("2201", "Bank Loan", Account.Type.LIABILITY, null, false, "EGP", true);

        when(accountRepository.findAll()).thenReturn(List.of(cash, loanPayable));

        // Loan drawn: cash +5000 / loan credit 5000 -> financing +5000
        JournalEntry drawDown = new JournalEntry("JE-CF-L1", LocalDate.of(2026, 4, 2), "loan drawdown", "R10", "FP");
        drawDown.approve("m");
        drawDown.post("a");
        // Repayment instalment: loan debit 1000 / cash -1000 -> financing -1000
        JournalEntry repayment = new JournalEntry("JE-CF-L2", LocalDate.of(2026, 4, 20), "loan instalment", "R11", "FP");
        repayment.approve("m");
        repayment.post("a");

        when(journalEntryRepository.findByStatusInOrderByEntryDateDesc(anyList()))
                .thenReturn(List.of(drawDown, repayment));
        when(journalEntryLineRepository.findByJournalEntryIdIn(anySet())).thenAnswer(linesByEntry(List.of(
                new JournalEntryLine(drawDown.getId(), cash.getId(), null, bd("5000"), BigDecimal.ZERO, "in"),
                new JournalEntryLine(drawDown.getId(), loanPayable.getId(), null, BigDecimal.ZERO, bd("5000"), "loan"),
                new JournalEntryLine(repayment.getId(), loanPayable.getId(), null, bd("1000"), BigDecimal.ZERO, "repay"),
                new JournalEntryLine(repayment.getId(), cash.getId(), null, BigDecimal.ZERO, bd("1000"), "out")
        )));

        FinancialStatementsReportService.CashFlowReport cf = statementsService.getCashFlowStatement(
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));

        assertThat(cf.financingCashFlow()).isEqualByComparingTo(bd("4000"));
        assertThat(cf.operatingCashFlow()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cf.netCashFlow()).isEqualByComparingTo(bd("4000"));
        assertThat(cf.reconciled()).isTrue();
    }

    @Test
    @DisplayName("Internal transfers between own cash/bank accounts net to zero and reconciliation holds with opening balance")
    void internalTransfersNetToZeroAndReconciliationHolds() {
        Account cash = new Account("1010", "Main Cash", Account.Type.ASSET, null, false, "EGP", true);
        Account bank = new Account("1100", "CIB Bank", Account.Type.ASSET, null, false, "EGP", true);
        Account supplier = new Account("2101", "Trade Payable", Account.Type.LIABILITY, null, false, "EGP", true);
        Account expense = new Account("5010", "Rent Expense", Account.Type.EXPENSE, null, false, "EGP", true);

        when(accountRepository.findAll()).thenReturn(List.of(cash, bank, supplier, expense));

        JournalEntry transfer = new JournalEntry("JE-CF-T1", LocalDate.of(2026, 5, 3), "cash to bank", "R20", "FP");
        transfer.approve("m");
        transfer.post("a");
        // Prior-period seed: opening cash 700 (entry itself belongs to April)
        JournalEntry seed = new JournalEntry("JE-CF-T0", LocalDate.of(2026, 4, 30), "seed", "R19", "FP");
        seed.approve("m");
        seed.post("a");
        // Supplier settled from bank: operating -500
        JournalEntry paidSupplier = new JournalEntry("JE-CF-T2", LocalDate.of(2026, 5, 8), "pay supplier", "R21", "FP");
        paidSupplier.approve("m");
        paidSupplier.post("a");

        when(journalEntryRepository.findByStatusInOrderByEntryDateDesc(anyList()))
                .thenReturn(List.of(seed, transfer, paidSupplier));
        when(journalEntryLineRepository.findByJournalEntryIdIn(anySet())).thenAnswer(linesByEntry(List.of(
                new JournalEntryLine(seed.getId(), cash.getId(), null, bd("700"), BigDecimal.ZERO, "seed"),
                new JournalEntryLine(seed.getId(), expense.getId(), null, BigDecimal.ZERO, bd("700"), "seed offset"),
                new JournalEntryLine(transfer.getId(), bank.getId(), null, bd("3000"), BigDecimal.ZERO, "to bank"),
                new JournalEntryLine(transfer.getId(), cash.getId(), null, BigDecimal.ZERO, bd("3000"), "from cash"),
                new JournalEntryLine(paidSupplier.getId(), supplier.getId(), null, bd("500"), BigDecimal.ZERO, "settle"),
                new JournalEntryLine(paidSupplier.getId(), bank.getId(), null, BigDecimal.ZERO, bd("500"), "out")
        )));

        FinancialStatementsReportService.CashFlowReport cf = statementsService.getCashFlowStatement(
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        // Opening = seed cash movement only (+700); internal transfer contributes nothing
        assertThat(cf.openingCashBalance()).isEqualByComparingTo(bd("700"));
        assertThat(cf.operatingCashFlow()).isEqualByComparingTo(bd("-500"));
        assertThat(cf.investingCashFlow()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cf.financingCashFlow()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cf.closingCashBalance()).isEqualByComparingTo(bd("200"));
        assertThat(cf.reconciled()).isTrue();
    }

    @Test
    @DisplayName("Provides an equal-length comparative previous period alongside the selected one")
    void providesEqualLengthComparativePeriod() {
        Account cash = new Account("1010", "Main Cash", Account.Type.ASSET, null, false, "EGP", true);
        Account revenue = new Account("4010", "Sales Revenue", Account.Type.REVENUE, null, false, "EGP", true);

        when(accountRepository.findAll()).thenReturn(List.of(cash, revenue));

        JournalEntry febSale = new JournalEntry("JE-CF-F", LocalDate.of(2026, 2, 10), "feb sale", "R30", "FP");
        febSale.approve("m");
        febSale.post("a");
        JournalEntry marSale = new JournalEntry("JE-CF-M", LocalDate.of(2026, 3, 15), "mar sale", "R31", "FP");
        marSale.approve("m");
        marSale.post("a");

        when(journalEntryRepository.findByStatusInOrderByEntryDateDesc(anyList()))
                .thenReturn(List.of(febSale, marSale));
        when(journalEntryLineRepository.findByJournalEntryIdIn(anySet())).thenAnswer(linesByEntry(List.of(
                new JournalEntryLine(febSale.getId(), cash.getId(), null, bd("900"), BigDecimal.ZERO, "in"),
                new JournalEntryLine(febSale.getId(), revenue.getId(), null, BigDecimal.ZERO, bd("900"), "rev"),
                new JournalEntryLine(marSale.getId(), cash.getId(), null, bd("1500"), BigDecimal.ZERO, "in"),
                new JournalEntryLine(marSale.getId(), revenue.getId(), null, BigDecimal.ZERO, bd("1500"), "rev")
        )));

        FinancialStatementsReportService.CashFlowReport cf = statementsService.getCashFlowStatement(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertThat(cf.comparative()).isNotNull();
        assertThat(cf.comparative().startDate()).isEqualTo(LocalDate.of(2026, 1, 29));
        assertThat(cf.comparative().endDate()).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(cf.comparative().operatingCashFlow()).isEqualByComparingTo(bd("900"));
        assertThat(cf.operatingCashFlow()).isEqualByComparingTo(bd("1500"));
    }

    @Test
    @DisplayName("Rejects inverted ranges instead of producing a fabricated statement")
    void rejectsInvertedRanges() {
        assertThatThrownBy(() -> statementsService.getCashFlowStatement(
                LocalDate.of(2026, 3, 31), LocalDate.of(2026, 3, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    /** Repository stub that honors the requested journal-entry-id set, like JPA would. */
    private static org.mockito.stubbing.Answer<List<JournalEntryLine>> linesByEntry(List<JournalEntryLine> lines) {
        Map<String, List<JournalEntryLine>> ledger = lines.stream()
                .collect(Collectors.groupingBy(JournalEntryLine::getJournalEntryId));
        return inv -> {
            Set<String> requested = inv.getArgument(0);
            return ledger.entrySet().stream()
                    .filter(e -> requested.contains(e.getKey()))
                    .flatMap(e -> e.getValue().stream())
                    .toList();
        };
    }
}
