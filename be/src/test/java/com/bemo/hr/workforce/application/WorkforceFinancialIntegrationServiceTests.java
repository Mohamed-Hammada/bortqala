package com.bemo.hr.workforce.application;

import com.bemo.hr.workforce.domain.WorkforceGlPosting;
import com.bemo.hr.workforce.domain.WorkforceInvoiceMatch;
import com.bemo.hr.workforce.domain.WorkforceRequestBudget;
import com.bemo.hr.workforce.domain.WorkforceTreasuryMatch;
import com.bemo.hr.workforce.infrastructure.WorkforceGlPostingRepository;
import com.bemo.hr.workforce.infrastructure.WorkforceInvoiceMatchRepository;
import com.bemo.hr.workforce.infrastructure.WorkforceRequestBudgetRepository;
import com.bemo.hr.workforce.infrastructure.WorkforceTreasuryMatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkforceFinancialIntegrationServiceTests {

    private WorkforceRequestBudgetRepository budgetRepository;
    private WorkforceInvoiceMatchRepository invoiceMatchRepository;
    private WorkforceGlPostingRepository glPostingRepository;
    private WorkforceTreasuryMatchRepository treasuryMatchRepository;
    private WorkforceFinancialIntegrationService service;

    @BeforeEach
    void setUp() {
        budgetRepository = mock(WorkforceRequestBudgetRepository.class);
        invoiceMatchRepository = mock(WorkforceInvoiceMatchRepository.class);
        glPostingRepository = mock(WorkforceGlPostingRepository.class);
        treasuryMatchRepository = mock(WorkforceTreasuryMatchRepository.class);

        service = new WorkforceFinancialIntegrationService(budgetRepository, invoiceMatchRepository, glPostingRepository, treasuryMatchRepository);
    }

    @Test
    void verifiesWorkforceFinancialLifecycleIntegrationSuccessfully() {
        when(budgetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceMatchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(glPostingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(treasuryMatchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkforceRequestBudget budget = service.allocateRequestBudget("req-1", "dept-10", "b-2026", new BigDecimal("50000.00"));
        assertThat(budget.getAllocatedAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));

        WorkforceInvoiceMatch match = service.matchInvoice("set-100", "inv-200", new BigDecimal("48000.00"), BigDecimal.ZERO);
        assertThat(match.getStatus()).isEqualTo(WorkforceInvoiceMatch.Status.MATCHED);

        WorkforceGlPosting posting = service.recordGlPosting("set-100", "jrnl-300", new BigDecimal("48000.00"));
        assertThat(posting.getStatus()).isEqualTo(WorkforceGlPosting.Status.POSTED);

        WorkforceTreasuryMatch treasury = service.matchTreasuryPayment("pay-400", "btx-500", new BigDecimal("48000.00"));
        assertThat(treasury.getStatus()).isEqualTo(WorkforceTreasuryMatch.Status.MATCHED);

        when(budgetRepository.findByRequestId("req-1")).thenReturn(Optional.of(budget));
        assertThat(service.getRequestBudget("req-1")).isNotNull();
    }
}
