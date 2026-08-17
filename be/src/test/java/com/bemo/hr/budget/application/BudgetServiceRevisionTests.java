package com.bemo.hr.budget.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.budget.BudgetRevision;
import com.bemo.hr.budget.BudgetRevisionRepository;
import com.bemo.hr.budget.BudgetTransfer;
import com.bemo.hr.budget.BudgetTransferRepository;
import com.bemo.hr.budget.domain.Budget;
import com.bemo.hr.budget.domain.BudgetPeriodType;
import com.bemo.hr.budget.domain.BudgetRepository;
import com.bemo.hr.budget.domain.EncumbranceRepository;
import com.bemo.hr.organization.infrastructure.DepartmentRepository;
import com.bemo.hr.shared.i18n.TranslationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BudgetServiceRevisionTests {

    private BudgetRepository budgetRepository;
    private EncumbranceRepository encumbranceRepository;
    private DepartmentRepository departmentRepository;
    private AuditService auditService;
    private TranslationService translationService;
    private BudgetRevisionRepository budgetRevisionRepository;
    private BudgetTransferRepository budgetTransferRepository;
    private BudgetService budgetService;

    @BeforeEach
    void setUp() {
        budgetRepository = mock(BudgetRepository.class);
        encumbranceRepository = mock(EncumbranceRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        auditService = mock(AuditService.class);
        translationService = mock(TranslationService.class);
        budgetRevisionRepository = mock(BudgetRevisionRepository.class);
        budgetTransferRepository = mock(BudgetTransferRepository.class);
        budgetService = new BudgetService(budgetRepository, encumbranceRepository, departmentRepository, auditService, translationService, budgetRevisionRepository, budgetTransferRepository);
    }

    @Test
    void revisionRemainsImmutableAndDoesNotChangeEffectiveBudgetUntilApproved() {
        Budget b1 = new Budget(2026, BudgetPeriodType.ANNUAL, null, "dept-1", new BigDecimal("100000.00"), "EGP", true, true);
        Budget b2 = new Budget(2026, BudgetPeriodType.ANNUAL, null, "dept-2", new BigDecimal("50000.00"), "EGP", true, true);

        when(budgetRepository.findByIdForUpdate("b1")).thenReturn(Optional.of(b1));
        when(budgetRepository.findById("b1")).thenReturn(Optional.of(b1));
        when(budgetRevisionRepository.existsByBudgetIdAndStatus("b1", BudgetRevision.Status.PENDING)).thenReturn(false);
        when(budgetRevisionRepository.findByBudgetIdOrderByRevisionNumberDesc("b1")).thenReturn(List.of());
        when(budgetRepository.findById("b2")).thenReturn(Optional.of(b2));
        when(budgetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(budgetRevisionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(budgetTransferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BudgetRevision rev = budgetService.reviseBudget("b1", new BigDecimal("120000.00"), "Increased sales target", "manager1");
        assertThat(rev.getStatus()).isEqualTo(BudgetRevision.Status.PENDING);
        assertThat(rev.getPreviousAmount()).isEqualByComparingTo("100000.00");
        assertThat(rev.getNewAmount()).isEqualByComparingTo("120000.00");
        assertThat(b1.getPlannedAmount()).isEqualByComparingTo(new BigDecimal("100000.00"));

        when(budgetRevisionRepository.findById(rev.getId())).thenReturn(Optional.of(rev));
        BudgetRevision approved = budgetService.approveRevision("b1", rev.getId(), "manager2");
        assertThat(approved.getStatus()).isEqualTo(BudgetRevision.Status.APPROVED);
        assertThat(b1.getPlannedAmount()).isEqualByComparingTo(new BigDecimal("120000.00"));
        assertThat(b1.getCurrentRevisionNumber()).isEqualTo(1);
        assertThat(rev.getPreviousAmount()).isEqualByComparingTo("100000.00");

        BudgetTransfer transfer = new BudgetTransfer("TRF-001", "b1", "b2", new BigDecimal("10000.00"), "Reallocate marketing funds");
        when(budgetTransferRepository.findById("trf-1")).thenReturn(Optional.of(transfer));

        BudgetTransfer approvedTransfer = budgetService.approveTransfer("trf-1");
        assertThat(approvedTransfer.getStatus()).isEqualTo(BudgetTransfer.Status.APPROVED);
        assertThat(b1.getPlannedAmount()).isEqualByComparingTo(new BigDecimal("110000.00"));
        assertThat(b2.getPlannedAmount()).isEqualByComparingTo(new BigDecimal("60000.00"));
    }

    @Test
    void revisionRequiresReasonAndIndependentApprover() {
        Budget budget = new Budget(2026, BudgetPeriodType.ANNUAL, null, "dept-1", new BigDecimal("100.00"), "EGP", true, true);
        when(budgetRepository.findByIdForUpdate("b1")).thenReturn(Optional.of(budget));
        when(budgetRevisionRepository.findByBudgetIdOrderByRevisionNumberDesc("b1")).thenReturn(List.of());
        when(budgetRevisionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> budgetService.reviseBudget("b1", new BigDecimal("120.00"), " ", "maker"))
                .hasMessageContaining("reason");
        BudgetRevision revision = budgetService.reviseBudget("b1", new BigDecimal("120.00"), "Approved forecast", "maker");
        when(budgetRevisionRepository.findById(revision.getId())).thenReturn(Optional.of(revision));
        assertThatThrownBy(() -> budgetService.approveRevision("b1", revision.getId(), "maker"))
                .hasMessageContaining("Requester");
        assertThat(budget.getPlannedAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void historyIsReturnedNewestFirstWithoutRewritingPastEvidence() {
        Budget budget = new Budget(2026, BudgetPeriodType.ANNUAL, null, "dept-1", new BigDecimal("100.00"), "EGP", true, true);
        BudgetRevision older = new BudgetRevision("b1", 1, new BigDecimal("80.00"), new BigDecimal("100.00"), "Baseline", "maker", false);
        BudgetRevision newer = new BudgetRevision("b1", 2, new BigDecimal("100.00"), new BigDecimal("120.00"), "Forecast", "maker", true);
        when(budgetRepository.findById("b1")).thenReturn(Optional.of(budget));
        when(budgetRevisionRepository.findByBudgetIdOrderByRevisionNumberDesc("b1")).thenReturn(List.of(newer, older));

        assertThat(budgetService.listRevisions("b1")).containsExactly(newer, older);
        assertThat(older.getPreviousAmount()).isEqualByComparingTo("80.00");
        assertThat(older.getNewAmount()).isEqualByComparingTo("100.00");
    }
}
