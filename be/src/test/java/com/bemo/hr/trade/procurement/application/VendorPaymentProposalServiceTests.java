package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.trade.procurement.domain.VendorPaymentProposal;
import com.bemo.hr.trade.procurement.infrastructure.VendorPaymentProposalRepository;
import com.bemo.hr.approval.SegregationOfDutiesService;
import com.bemo.hr.trade.procurement.api.ProcurementApi;
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
    private ProcurementService procurementService;

    @BeforeEach
    void setUp() {
        repository = mock(VendorPaymentProposalRepository.class);
        procurementService = mock(ProcurementService.class);
        service = new VendorPaymentProposalService(repository, procurementService, new SegregationOfDutiesService());
    }

    @Test
    void createsApprovesAndExecutesVendorPaymentProposalSuccessfully() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VendorPaymentProposal proposal = service.createProposal("supp-12", "inv-88", new BigDecimal("12500.00"), LocalDate.now().plusDays(15), "maker");
        assertThat(proposal).isNotNull();
        assertThat(proposal.getProposedAmount()).isEqualByComparingTo(new BigDecimal("12500.00"));
        assertThat(proposal.getStatus()).isEqualTo(VendorPaymentProposal.Status.PROPOSED);

        when(repository.findByIdForUpdate(proposal.getId())).thenReturn(Optional.of(proposal));

        VendorPaymentProposal approved = service.approveProposal(proposal.getId(), "checker");
        assertThat(approved.getStatus()).isEqualTo(VendorPaymentProposal.Status.APPROVED);

        when(procurementService.createSupplierPayment(any())).thenReturn(new ProcurementApi.SupplierPaymentResponse(
                "payment-1", "PMT-1", System.currentTimeMillis(), "supp-12", "Supplier", "inv-88",
                new BigDecimal("12500"), "EGP", "BANK", null, "op-1", "POSTED", System.currentTimeMillis()));
        VendorPaymentProposal executed = service.executeProposal(proposal.getId(), "op-1", "BANK", "disburser");
        assertThat(executed.getStatus()).isEqualTo(VendorPaymentProposal.Status.EXECUTED);
        assertThat(executed.getSupplierPaymentId()).isEqualTo("payment-1");
        VendorPaymentProposal replay = service.executeProposal(proposal.getId(), "op-1", "BANK", "disburser");
        assertThat(replay).isSameAs(executed);
        verify(procurementService, times(1)).createSupplierPayment(any());
    }
}
