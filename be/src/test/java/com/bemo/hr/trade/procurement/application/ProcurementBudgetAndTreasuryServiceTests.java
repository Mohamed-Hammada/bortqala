package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.trade.procurement.domain.ProcurementBudgetApproval;
import com.bemo.hr.trade.procurement.domain.ProcurementTreasuryBankMatch;
import com.bemo.hr.trade.procurement.infrastructure.ProcurementBudgetApprovalRepository;
import com.bemo.hr.trade.procurement.infrastructure.ProcurementTreasuryBankMatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcurementBudgetAndTreasuryServiceTests {

    private ProcurementBudgetApprovalRepository budgetApprovalRepository;
    private ProcurementTreasuryBankMatchRepository treasuryBankMatchRepository;
    private ProcurementBudgetAndTreasuryService service;

    @BeforeEach
    void setUp() {
        budgetApprovalRepository = mock(ProcurementBudgetApprovalRepository.class);
        treasuryBankMatchRepository = mock(ProcurementTreasuryBankMatchRepository.class);
        service = new ProcurementBudgetAndTreasuryService(budgetApprovalRepository, treasuryBankMatchRepository);
    }

    @Test
    void approvesBudgetAndMatchesTreasuryPaymentSuccessfully() {
        when(budgetApprovalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(treasuryBankMatchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProcurementBudgetApproval approval = service.approveBudget("req-88", "bgt-2026", new BigDecimal("75000.00"));
        assertThat(approval).isNotNull();
        assertThat(approval.getAmount()).isEqualByComparingTo(new BigDecimal("75000.00"));
        assertThat(approval.getStatus()).isEqualTo(ProcurementBudgetApproval.Status.APPROVED);

        ProcurementTreasuryBankMatch match = service.matchTreasuryPayment("pay-77", "tx-99", new BigDecimal("75000.00"));
        assertThat(match).isNotNull();
        assertThat(match.getStatus()).isEqualTo(ProcurementTreasuryBankMatch.Status.MATCHED);

        when(budgetApprovalRepository.findByRequisitionId("req-88")).thenReturn(Optional.of(approval));
        assertThat(service.getBudgetApproval("req-88")).isNotNull();
    }
}
