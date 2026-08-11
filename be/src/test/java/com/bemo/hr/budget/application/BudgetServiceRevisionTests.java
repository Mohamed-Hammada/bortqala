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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    void revisesBudgetAndApproveTransferSuccessfully() {
        Budget b1 = new Budget(2026, BudgetPeriodType.ANNUAL, null, "dept-1", new BigDecimal("100000.00"), "EGP", true, true);
        Budget b2 = new Budget(2026, BudgetPeriodType.ANNUAL, null, "dept-2", new BigDecimal("50000.00"), "EGP", true, true);

        when(budgetRepository.findById("b1")).thenReturn(Optional.of(b1));
        when(budgetRepository.findById("b2")).thenReturn(Optional.of(b2));
        when(budgetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(budgetRevisionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(budgetTransferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BudgetRevision rev = budgetService.reviseBudget("b1", new BigDecimal("120000.00"), "Increased sales target", "manager1");
        assertThat(rev).isNotNull();
        assertThat(b1.getPlannedAmount()).isEqualByComparingTo(new BigDecimal("120000.00"));

        BudgetTransfer transfer = new BudgetTransfer("TRF-001", "b1", "b2", new BigDecimal("10000.00"), "Reallocate marketing funds");
        when(budgetTransferRepository.findById("trf-1")).thenReturn(Optional.of(transfer));

        BudgetTransfer approved = budgetService.approveTransfer("trf-1");
        assertThat(approved.getStatus()).isEqualTo(BudgetTransfer.Status.APPROVED);
        assertThat(b1.getPlannedAmount()).isEqualByComparingTo(new BigDecimal("110000.00"));
        assertThat(b2.getPlannedAmount()).isEqualByComparingTo(new BigDecimal("60000.00"));
    }
}
