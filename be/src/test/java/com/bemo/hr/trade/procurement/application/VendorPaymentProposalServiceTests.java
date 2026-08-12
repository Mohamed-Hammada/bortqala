package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.trade.procurement.domain.VendorPaymentProposal;
import com.bemo.hr.trade.procurement.infrastructure.VendorPaymentProposalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VendorPaymentProposalServiceTests {

    private VendorPaymentProposalRepository repository;
    private VendorPaymentProposalService service;

    @BeforeEach
    void setUp() {
        repository = mock(VendorPaymentProposalRepository.class);
        service = new VendorPaymentProposalService(repository);
    }

    @Test
    void createsApprovesAndExecutesVendorPaymentProposalSuccessfully() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VendorPaymentProposal proposal = service.createProposal("supp-12", "inv-88", new BigDecimal("12500.00"), LocalDate.now().plusDays(15));
        assertThat(proposal).isNotNull();
        assertThat(proposal.getProposedAmount()).isEqualByComparingTo(new BigDecimal("12500.00"));
        assertThat(proposal.getStatus()).isEqualTo(VendorPaymentProposal.Status.PROPOSED);

        when(repository.findById(proposal.getId())).thenReturn(Optional.of(proposal));

        VendorPaymentProposal approved = service.approveProposal(proposal.getId());
        assertThat(approved.getStatus()).isEqualTo(VendorPaymentProposal.Status.APPROVED);

        VendorPaymentProposal executed = service.executeProposal(proposal.getId());
        assertThat(executed.getStatus()).isEqualTo(VendorPaymentProposal.Status.EXECUTED);
    }
}
