package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.approval.SegregationOfDutiesService;
import com.bemo.hr.approval.SegregationOfDutiesViolationException;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.procurement.api.ProcurementApi;
import com.bemo.hr.trade.procurement.domain.SupplierInvoice;
import com.bemo.hr.trade.procurement.domain.VendorPaymentProposal;
import com.bemo.hr.trade.procurement.domain.VendorPaymentProposalAllocation;
import com.bemo.hr.trade.procurement.infrastructure.SupplierInvoiceRepository;
import com.bemo.hr.trade.procurement.infrastructure.SupplierPaymentRepository;
import com.bemo.hr.trade.procurement.infrastructure.VendorPaymentProposalAllocationRepository;
import com.bemo.hr.trade.procurement.infrastructure.VendorPaymentProposalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VendorPaymentProposalServiceTests {

    private VendorPaymentProposalRepository proposalRepository;
    private VendorPaymentProposalAllocationRepository allocationRepository;
    private SupplierInvoiceRepository supplierInvoiceRepository;
    private SupplierPaymentRepository supplierPaymentRepository;
    private ProcurementService procurementService;
    private VendorPaymentProposalService service;
    private AtomicReference<VendorPaymentProposal> proposal;
    private List<VendorPaymentProposalAllocation> allocations;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(VendorPaymentProposalRepository.class);
        allocationRepository = mock(VendorPaymentProposalAllocationRepository.class);
        supplierInvoiceRepository = mock(SupplierInvoiceRepository.class);
        supplierPaymentRepository = mock(SupplierPaymentRepository.class);
        procurementService = mock(ProcurementService.class);
        proposal = new AtomicReference<>();
        allocations = new ArrayList<>();
        when(proposalRepository.save(any())).thenAnswer(invocation -> {
            VendorPaymentProposal saved = invocation.getArgument(0);
            proposal.set(saved);
            return saved;
        });
        when(proposalRepository.findByIdForUpdate(any())).thenAnswer(invocation -> Optional.ofNullable(proposal.get()));
        when(allocationRepository.save(any())).thenAnswer(invocation -> {
            VendorPaymentProposalAllocation saved = invocation.getArgument(0);
            if (!allocations.contains(saved)) allocations.add(saved);
            return saved;
        });
        when(allocationRepository.findByProposalIdOrderByLineNoAsc(any())).thenAnswer(invocation -> List.copyOf(allocations));
        when(supplierPaymentRepository.sumPostedAmountBySupplierInvoiceId(any())).thenReturn(BigDecimal.ZERO);
        service = new VendorPaymentProposalService(proposalRepository, allocationRepository,
                supplierInvoiceRepository, supplierPaymentRepository, procurementService,
                new SegregationOfDutiesService(), mock(AuditService.class));
    }

    @Test
    void proposalApprovalAndExecutionCreateTheRealSupplierPaymentExactlyOnce() {
        SupplierInvoice invoice = invoice("supp-12", "12500.00");
        stubInvoice(invoice);

        var created = create(List.of(allocation(invoice, "12500.00")));
        assertThat(created.proposedAmount()).isEqualByComparingTo("12500.00");
        assertThat(created.allocations()).hasSize(1);

        var approved = service.approveProposal(created.id(), "checker");
        assertThat(approved.status()).isEqualTo(VendorPaymentProposal.Status.APPROVED);

        when(procurementService.createSupplierPaymentsForProposal(any())).thenReturn(List.of(
                payment("payment-1", invoice.getId(), "12500.00", "op-1:1")));
        var executed = service.executeProposal(created.id(), "op-1", "BANK", "disburser");
        assertThat(executed.status()).isEqualTo(VendorPaymentProposal.Status.EXECUTED);
        assertThat(executed.allocations().get(0).supplierPaymentId()).isEqualTo("payment-1");

        var replay = service.executeProposal(created.id(), "op-1", "BANK", "disburser");
        assertThat(replay.id()).isEqualTo(executed.id());
        verify(procurementService, times(1)).createSupplierPaymentsForProposal(any());
    }

    @Test
    void multiInvoiceProposalDerivesTotalAndCreatesOnePaymentPerAllocation() {
        SupplierInvoice first = invoice("supp-12", "100.00");
        SupplierInvoice second = invoice("supp-12", "80.00");
        stubInvoice(first);
        stubInvoice(second);
        var created = create(List.of(allocation(first, "60.00"), allocation(second, "40.00")));
        service.approveProposal(created.id(), "checker");
        when(procurementService.createSupplierPaymentsForProposal(any())).thenReturn(List.of(
                payment("payment-1", first.getId(), "60.00", "batch-op:1"),
                payment("payment-2", second.getId(), "40.00", "batch-op:2")));

        var executed = service.executeProposal(created.id(), "batch-op", "BANK_TRANSFER", "disburser");

        assertThat(created.proposedAmount()).isEqualByComparingTo("100.00");
        assertThat(executed.allocations()).extracting(VendorPaymentProposalService.AllocationResult::supplierPaymentId)
                .containsExactly("payment-1", "payment-2");
        verify(procurementService).createSupplierPaymentsForProposal(argThat(payloads -> payloads.size() == 2
                && payloads.get(0).supplierInvoiceId().equals(first.getId())
                && payloads.get(0).amount().compareTo(new BigDecimal("60.00")) == 0
                && payloads.get(0).operationId().equals("batch-op:1")
                && payloads.get(1).supplierInvoiceId().equals(second.getId())
                && payloads.get(1).amount().compareTo(new BigDecimal("40.00")) == 0
                && payloads.get(1).operationId().equals("batch-op:2")));
    }

    @Test
    void partialAllocationIsAllowedButOverpaymentAndDuplicateInvoicesAreRejected() {
        SupplierInvoice invoice = invoice("supp-12", "100.00");
        stubInvoice(invoice);
        when(supplierPaymentRepository.sumPostedAmountBySupplierInvoiceId(invoice.getId())).thenReturn(new BigDecimal("20.00"));

        assertThat(create(List.of(allocation(invoice, "50.00"))).proposedAmount()).isEqualByComparingTo("50.00");

        assertThatThrownBy(() -> create(List.of(allocation(invoice, "80.01"))))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(error -> ((BusinessRuleException) error).getCode())
                .isEqualTo("PAYMENT_PROPOSAL_OVERPAYMENT");
        assertThatThrownBy(() -> create(List.of(allocation(invoice, "20.00"), allocation(invoice, "10.00"))))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(error -> ((BusinessRuleException) error).getCode())
                .isEqualTo("PAYMENT_PROPOSAL_INVOICE_DUPLICATE");
    }

    @Test
    void rejectsCrossSupplierAndMixedCurrencyAllocations() {
        SupplierInvoice egp = invoice("supp-12", "100.00", "EGP");
        SupplierInvoice usd = invoice("supp-12", "100.00", "USD");
        SupplierInvoice otherSupplier = invoice("supp-99", "100.00", "EGP");
        stubInvoice(egp);
        stubInvoice(usd);
        stubInvoice(otherSupplier);

        assertThatThrownBy(() -> create(List.of(allocation(egp, "10.00"), allocation(usd, "10.00"))))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(error -> ((BusinessRuleException) error).getCode())
                .isEqualTo("PAYMENT_PROPOSAL_CURRENCY_MISMATCH");
        assertThatThrownBy(() -> create(List.of(allocation(otherSupplier, "10.00"))))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(error -> ((BusinessRuleException) error).getCode())
                .isEqualTo("PROC_INVOICE_SUPPLIER_MISMATCH");
    }

    @Test
    void enforcesMakerCheckerAndDifferentDisburser() {
        SupplierInvoice invoice = invoice("supp-12", "100.00");
        stubInvoice(invoice);
        var created = create(List.of(allocation(invoice, "50.00")));

        assertThatThrownBy(() -> service.approveProposal(created.id(), "maker"))
                .isInstanceOf(SegregationOfDutiesViolationException.class);
        service.approveProposal(created.id(), "checker");
        assertThatThrownBy(() -> service.executeProposal(created.id(), "op", "BANK", "checker"))
                .isInstanceOf(SegregationOfDutiesViolationException.class);
        assertThatThrownBy(() -> service.executeProposal(created.id(), "op", "BANK", "maker"))
                .isInstanceOf(SegregationOfDutiesViolationException.class);
        verifyNoInteractions(procurementService);
    }

    @Test
    void downstreamFailureDoesNotMarkProposalExecuted() {
        SupplierInvoice first = invoice("supp-12", "100.00");
        SupplierInvoice second = invoice("supp-12", "80.00");
        stubInvoice(first);
        stubInvoice(second);
        var created = create(List.of(allocation(first, "60.00"), allocation(second, "40.00")));
        service.approveProposal(created.id(), "checker");
        when(procurementService.createSupplierPaymentsForProposal(any()))
                .thenThrow(new BusinessRuleException("Posting failed"));

        assertThatThrownBy(() -> service.executeProposal(created.id(), "batch-op", "BANK", "disburser"))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(proposal.get().getStatus()).isEqualTo(VendorPaymentProposal.Status.APPROVED);
        verify(proposalRepository, never()).save(argThat(saved -> saved.getStatus() == VendorPaymentProposal.Status.EXECUTED));
    }

    @Test
    void supplierBankValidationFailurePropagatesWithoutExecutingProposal() {
        SupplierInvoice invoice = invoice("supp-12", "100.00");
        stubInvoice(invoice);
        var created = create(List.of(allocation(invoice, "50.00")));
        service.approveProposal(created.id(), "checker");
        when(procurementService.createSupplierPaymentsForProposal(any())).thenThrow(new BusinessRuleException(
                "Verified supplier bank account required", "PROC_SUPPLIER_BANK_VERIFICATION_REQUIRED",
                org.springframework.http.HttpStatus.CONFLICT));

        assertThatThrownBy(() -> service.executeProposal(created.id(), "bank-op", "BANK", "disburser"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(error -> ((BusinessRuleException) error).getCode())
                .isEqualTo("PROC_SUPPLIER_BANK_VERIFICATION_REQUIRED");
        assertThat(proposal.get().getStatus()).isEqualTo(VendorPaymentProposal.Status.APPROVED);
    }

    private VendorPaymentProposalService.ProposalResult create(List<VendorPaymentProposalService.AllocationInput> requested) {
        return service.createProposal("supp-12", requested, LocalDate.now().plusDays(15), "maker");
    }

    private VendorPaymentProposalService.AllocationInput allocation(SupplierInvoice invoice, String amount) {
        return new VendorPaymentProposalService.AllocationInput(invoice.getId(), new BigDecimal(amount));
    }

    private void stubInvoice(SupplierInvoice invoice) {
        when(supplierInvoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
    }

    private SupplierInvoice invoice(String supplierId, String amount) {
        return invoice(supplierId, amount, "EGP");
    }

    private SupplierInvoice invoice(String supplierId, String amount, String currency) {
        return new SupplierInvoice("INV-" + System.nanoTime(), "REF-" + System.nanoTime(), null, currency, supplierId,
                null, null, null, LocalDate.of(2026, 8, 1), new BigDecimal(amount),
                BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.of(2026, 8, 31), null);
    }

    private ProcurementApi.SupplierPaymentResponse payment(String id, String invoiceId, String amount, String operationId) {
        return new ProcurementApi.SupplierPaymentResponse(id, "PMT-1", System.currentTimeMillis(), "supp-12", "Supplier",
                invoiceId, new BigDecimal(amount), BigDecimal.ZERO, null, "EGP", "BANK", null, operationId, "POSTED", System.currentTimeMillis());
    }
}
