package com.bemo.hr.budget.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.budget.api.BudgetApi;
import com.bemo.hr.budget.domain.Budget;
import com.bemo.hr.budget.domain.BudgetPeriodType;
import com.bemo.hr.budget.domain.BudgetRepository;
import com.bemo.hr.budget.domain.Encumbrance;
import com.bemo.hr.budget.domain.EncumbranceRepository;
import com.bemo.hr.budget.domain.EncumbranceStatus;
import com.bemo.hr.organization.domain.Department;
import com.bemo.hr.organization.infrastructure.DepartmentRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.i18n.TranslationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BudgetServiceTests {

    private BudgetRepository budgetRepository;
    private EncumbranceRepository encumbranceRepository;
    private DepartmentRepository departmentRepository;
    private BudgetService budgetService;

    private static final String DEPARTMENT = "dept-1";
    private static final String BUDGET_ID = "budget-1";

    @BeforeEach
    void setUp() {
        budgetRepository = mock(BudgetRepository.class);
        encumbranceRepository = mock(EncumbranceRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        budgetService = new BudgetService(budgetRepository, encumbranceRepository, departmentRepository,
                mock(AuditService.class), mock(TranslationService.class),
                mock(com.bemo.hr.budget.BudgetRevisionRepository.class),
                mock(com.bemo.hr.budget.BudgetTransferRepository.class));
        when(departmentRepository.findById(DEPARTMENT))
                .thenReturn(Optional.of(new Department("company-1", "D1", "Production", null, true)));
    }

    private Budget annualBudget(BigDecimal planned) {
        return new Budget(2026, BudgetPeriodType.ANNUAL, null, DEPARTMENT, planned, "EGP", true, true);
    }

    private Budget monthlyBudget(BigDecimal planned, int month) {
        return new Budget(2026, BudgetPeriodType.MONTHLY, month, DEPARTMENT, planned, "EGP", true, true);
    }

    private Encumbrance activeEncumbrance(BigDecimal committed) {
        Encumbrance encumbrance = new Encumbrance(BUDGET_ID, "po-1", "PO-100", committed, "EGP");
        return encumbrance;
    }

    @Test
    void encumberForOrder_withMatchingAnnualBudget_createsCommitment() {
        Budget budget = annualBudget(new BigDecimal("1000.00"));
        when(budgetRepository.findByDepartmentIdAndActiveTrue(DEPARTMENT)).thenReturn(List.of(budget));
        when(encumbranceRepository.findByBudgetId(BUDGET_ID)).thenReturn(List.of());
        when(encumbranceRepository.save(any(Encumbrance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = budgetService.encumberForOrder("po-1", "PO-100", DEPARTMENT,
                new BigDecimal("400.00"), LocalDate.of(2026, 7, 1), "user-a");

        assertThat(response).isNotNull();
        assertThat(response.budgetId()).isEqualTo(budget.getId());
        assertThat(response.committedAmount()).isEqualByComparingTo("400.00");
        assertThat(response.status()).isEqualTo(EncumbranceStatus.ACTIVE.name());
    }

    @Test
    void encumberForOrder_withMonthlyBudget_matchesDocumentMonth() {
        Budget budget = monthlyBudget(new BigDecimal("500.00"), 7);
        when(budgetRepository.findByDepartmentIdAndActiveTrue(DEPARTMENT)).thenReturn(List.of(budget));
        when(encumbranceRepository.findByBudgetId(BUDGET_ID)).thenReturn(List.of());
        when(encumbranceRepository.save(any(Encumbrance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = budgetService.encumberForOrder("po-1", "PO-100", DEPARTMENT,
                new BigDecimal("100.00"), LocalDate.of(2026, 7, 15), "user-a");

        assertThat(response).isNotNull();
        assertThat(response.committedAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void encumberForOrder_whenMonthlyBudgetIsForAnotherMonth_returnsNull() {
        Budget budget = monthlyBudget(new BigDecimal("500.00"), 7);
        when(budgetRepository.findByDepartmentIdAndActiveTrue(DEPARTMENT)).thenReturn(List.of(budget));

        var response = budgetService.encumberForOrder("po-1", "PO-100", DEPARTMENT,
                new BigDecimal("100.00"), LocalDate.of(2026, 8, 15), "user-a");

        assertThat(response).isNull();
        verify(encumbranceRepository, never()).save(any(Encumbrance.class));
    }

    @Test
    void encumberForOrder_withNoBudget_returnsNull() {
        when(budgetRepository.findByDepartmentIdAndActiveTrue(DEPARTMENT)).thenReturn(List.of());

        var response = budgetService.encumberForOrder("po-1", "PO-100", DEPARTMENT,
                new BigDecimal("100.00"), LocalDate.of(2026, 7, 15), "user-a");

        assertThat(response).isNull();
        verify(encumbranceRepository, never()).save(any(Encumbrance.class));
    }

    @Test
    void encumberForOrder_whenOverBudgetAndBlocking_throws() {
        Budget budget = annualBudget(new BigDecimal("1000.00"));
        when(budgetRepository.findByDepartmentIdAndActiveTrue(DEPARTMENT)).thenReturn(List.of(budget));
        when(encumbranceRepository.findByBudgetId(BUDGET_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> budgetService.encumberForOrder("po-1", "PO-100", DEPARTMENT,
                new BigDecimal("1000.01"), LocalDate.of(2026, 7, 1), "user-a"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("يتجاوز مبلغ أمر الشراء");
    }

    @Test
    void encumberForOrder_whenOverBudgetButNotBlocking_createsCommitment() {
        Budget budget = new Budget(2026, BudgetPeriodType.ANNUAL, null, DEPARTMENT,
                new BigDecimal("1000.00"), "EGP", false, true);
        when(budgetRepository.findByDepartmentIdAndActiveTrue(DEPARTMENT)).thenReturn(List.of(budget));
        when(encumbranceRepository.findByBudgetId(BUDGET_ID)).thenReturn(List.of());
        when(encumbranceRepository.save(any(Encumbrance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = budgetService.encumberForOrder("po-1", "PO-100", DEPARTMENT,
                new BigDecimal("1500.00"), LocalDate.of(2026, 7, 1), "user-a");

        assertThat(response).isNotNull();
        assertThat(response.committedAmount()).isEqualByComparingTo("1500.00");
    }

    @Test
    void liquidateForInvoice_reducesRemainingCommitment() {
        Budget budget = annualBudget(new BigDecimal("1000.00"));
        when(budgetRepository.findByDepartmentIdAndActiveTrue(DEPARTMENT)).thenReturn(List.of(budget));
        Encumbrance encumbrance = activeEncumbrance(new BigDecimal("400.00"));
        when(encumbranceRepository.findByBudgetId(BUDGET_ID)).thenReturn(List.of());
        when(encumbranceRepository.findFirstByPurchaseOrderIdAndStatus("po-1", EncumbranceStatus.ACTIVE))
                .thenReturn(Optional.of(encumbrance));

        budgetService.liquidateForInvoice("po-1", new BigDecimal("150.00"), "user-a");

        assertThat(encumbrance.getLiquidatedAmount()).isEqualByComparingTo("150.00");
        assertThat(encumbrance.getStatus()).isEqualTo(EncumbranceStatus.ACTIVE);
        assertThat(encumbrance.getCommittedAmount().subtract(encumbrance.getLiquidatedAmount()))
                .isEqualByComparingTo("250.00");
    }

    @Test
    void liquidateForInvoice_whenFullyLiquidated_marksEncumbranceReleased() {
        Encumbrance encumbrance = activeEncumbrance(new BigDecimal("200.00"));
        when(encumbranceRepository.findFirstByPurchaseOrderIdAndStatus("po-1", EncumbranceStatus.ACTIVE))
                .thenReturn(Optional.of(encumbrance));

        budgetService.liquidateForInvoice("po-1", new BigDecimal("200.00"), "user-a");

        assertThat(encumbrance.getStatus()).isEqualTo(EncumbranceStatus.RELEASED);
        assertThat(encumbrance.getLiquidatedAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    void releaseForCancel_returnsUnliquidatedCommitmentToBudget() {
        Encumbrance encumbrance = activeEncumbrance(new BigDecimal("500.00"));
        encumbrance.liquidate(new BigDecimal("100.00"));
        when(encumbranceRepository.findFirstByPurchaseOrderIdAndStatus("po-1", EncumbranceStatus.ACTIVE))
                .thenReturn(Optional.of(encumbrance));

        budgetService.releaseForCancel("po-1", "user-a");

        assertThat(encumbrance.getStatus()).isEqualTo(EncumbranceStatus.RELEASED);
        assertThat(encumbrance.getReleasedAmount()).isEqualByComparingTo("400.00");
        assertThat(encumbrance.getReleasedAt()).isNotNull();
    }

    @Test
    void status_reportsCommittedActualAvailableAndUtilization() {
        Budget budget = annualBudget(new BigDecimal("1000.00"));
        when(budgetRepository.findByActiveTrueOrderByFiscalYearDescPeriodMonthAsc()).thenReturn(List.of(budget));
        Encumbrance encumbrance = activeEncumbrance(new BigDecimal("300.00"));
        encumbrance.liquidate(new BigDecimal("100.00"));
        when(encumbranceRepository.findByBudgetId(budget.getId())).thenReturn(List.of(encumbrance));

        var rows = budgetService.status(null);

        assertThat(rows).hasSize(1);
        var row = rows.get(0);
        assertThat(row.committedAmount()).isEqualByComparingTo("300.00");
        assertThat(row.actualAmount()).isEqualByComparingTo("100.00");
        assertThat(row.availableAmount()).isEqualByComparingTo("600.00");
        assertThat(row.utilizationPercent()).isEqualByComparingTo("40.00");
        assertThat(row.departmentName()).isEqualTo("Production");
    }

    @Test
    void createBudget_requiresExistingDepartment() {
        when(departmentRepository.findById(anyString())).thenReturn(Optional.empty());

        BudgetApi.BudgetPayload payload = new BudgetApi.BudgetPayload(2026, BudgetPeriodType.ANNUAL, null,
                "unknown", new BigDecimal("100.00"), "EGP", true, true);

        assertThatThrownBy(() -> budgetService.createBudget(payload))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("غير موجود");
    }

    @Test
    void createBudget_rejectsInvalidMonthlyPeriodMonth() {
        BudgetApi.BudgetPayload payload = new BudgetApi.BudgetPayload(2026, BudgetPeriodType.MONTHLY, 13,
                DEPARTMENT, new BigDecimal("100.00"), "EGP", true, true);

        assertThatThrownBy(() -> budgetService.createBudget(payload))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("بين 1 و12");
    }

    @Test
    void deleteBudget_withActiveEncumbrances_isRejected() {
        Budget budget = annualBudget(new BigDecimal("100.00"));
        when(budgetRepository.findById(BUDGET_ID)).thenReturn(Optional.of(budget));
        when(encumbranceRepository.findByBudgetId(BUDGET_ID))
                .thenReturn(List.of(activeEncumbrance(new BigDecimal("50.00"))));

        assertThatThrownBy(() -> budgetService.deleteBudget(BUDGET_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("التزامات نشطة");
    }

    @Test
    void deleteBudget_withoutEncumbrances_deletes() {
        Budget budget = annualBudget(new BigDecimal("100.00"));
        when(budgetRepository.findById(BUDGET_ID)).thenReturn(Optional.of(budget));
        when(encumbranceRepository.findByBudgetId(BUDGET_ID)).thenReturn(List.of());

        budgetService.deleteBudget(BUDGET_ID);

        verify(budgetRepository).delete(budget);
    }

    @Test
    void requireBudget_whenMissing_throwsNotFound() {
        when(budgetRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.deleteBudget("missing"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateBudget_preservesActiveFlagAndPlannedAmount() {
        Budget budget = annualBudget(new BigDecimal("100.00"));
        when(budgetRepository.findById(BUDGET_ID)).thenReturn(Optional.of(budget));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BudgetApi.BudgetPayload payload = new BudgetApi.BudgetPayload(2026, BudgetPeriodType.ANNUAL, null,
                DEPARTMENT, new BigDecimal("200.00"), "EGP", true, false);

        var response = budgetService.updateBudget(BUDGET_ID, payload);

        assertThat(response.plannedAmount()).isEqualByComparingTo("200.00");
        assertThat(response.active()).isFalse();
    }
}
